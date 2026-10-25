import secrets
import uuid
from datetime import datetime
from zoneinfo import ZoneInfo

from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.api.deps import get_db, require_admin, require_kiosk
from app.core.config import settings
from app.core.security import hash_kiosk_token, pwd_context
from app.models.employee import Employee
from app.models.system import KioskDevice
from app.models.user import User
from app.schemas.kiosk import (
    KioskClockRequest,
    KioskClockResponse,
    KioskCredentials,
    KioskEmployeeStatus,
    KioskFaceEnrollRequest,
    KioskPairRequest,
    KioskPairResponse,
)
from app.services.attendance_events import InvalidTransitionError, allowed_actions, current_state, record_event
from app.services.face import (
    FaceDataUnavailableError,
    InvalidEmbeddingError,
    audit,
    kiosk_face_step,
    match_score,
    store_templates,
)
from app.services.lockout import AttemptLimiter

router = APIRouter(prefix="/api/v1", tags=["kiosk"])

pairing_limiter = AttemptLimiter(max_failures=5, lock_seconds=15 * 60)
pin_limiter = AttemptLimiter(max_failures=5, lock_seconds=5 * 60)
face_limiter = AttemptLimiter(max_failures=5, lock_seconds=5 * 60)


@router.post("/kiosk/pair", response_model=KioskPairResponse)
def pair_kiosk(payload: KioskPairRequest, request: Request, db: Session = Depends(get_db)):
    if not settings.kiosk_pairing_code:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Kiosk pairing is not configured")
    client_key = request.client.host if request.client else "unknown"
    if pairing_limiter.is_locked(client_key):
        raise HTTPException(status_code=status.HTTP_429_TOO_MANY_REQUESTS, detail="Too many attempts, try again later")
    if not secrets.compare_digest(payload.pairing_code.encode(), settings.kiosk_pairing_code.encode()):
        pairing_limiter.fail(client_key)
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid pairing code")
    pairing_limiter.reset(client_key)

    token = secrets.token_urlsafe(32)
    device = KioskDevice(
        id=f"kiosk-{uuid.uuid4().hex[:12]}",
        device_name=payload.device_name.strip(),
        pairing_token=hash_kiosk_token(token),
        active=True,
        last_seen_at=datetime.utcnow(),
        created_at=datetime.utcnow(),
    )
    db.add(device)
    db.commit()
    return KioskPairResponse(device_id=device.id, device_name=device.device_name, kiosk_token=token)


def _authenticate_employee(db: Session, credentials: KioskCredentials) -> Employee:
    code = credentials.employee_code.strip().upper()
    if pin_limiter.is_locked(code):
        raise HTTPException(status_code=status.HTTP_429_TOO_MANY_REQUESTS, detail="Too many attempts, try again later")
    employee = db.query(Employee).filter(func.upper(Employee.employee_code) == code).first()
    if (
        not employee
        or not employee.active
        or not employee.pin_hash
        or not pwd_context.verify(credentials.pin, employee.pin_hash)
    ):
        pin_limiter.fail(code)
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid employee code or PIN")
    pin_limiter.reset(code)
    return employee


def _status(db: Session, employee: Employee) -> dict:
    state = current_state(db, employee.id)
    return {
        "employee_code": employee.employee_code,
        "full_name": employee.full_name,
        "state": state,
        "allowed_actions": allowed_actions(state),
        "face": kiosk_face_step(db, employee),
    }


@router.get("/kiosk/me")
def kiosk_me(device: KioskDevice = Depends(require_kiosk)):
    return {"device_id": device.id, "device_name": device.device_name}


@router.post("/kiosk/identify", response_model=KioskEmployeeStatus)
def kiosk_identify(
    payload: KioskCredentials,
    db: Session = Depends(get_db),
    device: KioskDevice = Depends(require_kiosk),
):
    return _status(db, _authenticate_employee(db, payload))


@router.post("/kiosk/clock", response_model=KioskClockResponse)
def kiosk_clock(
    payload: KioskClockRequest,
    db: Session = Depends(get_db),
    device: KioskDevice = Depends(require_kiosk),
):
    employee = _authenticate_employee(db, payload)
    source, reason = "kiosk_pin", None
    if kiosk_face_step(db, employee) == "verify":
        score = _verify_face(db, employee, device, payload.face_embeddings)
        source, reason = "kiosk_pin_face", f"face_score={score:.3f}"
    event_time = datetime.now(ZoneInfo(settings.timezone)).replace(tzinfo=None, microsecond=0)
    try:
        event = record_event(
            db,
            employee_id=employee.id,
            event_type=payload.event_type,
            event_time=event_time,
            source=source,
            idempotency_key=f"kiosk:{device.id}:{payload.idempotency_key}",
            device_id=device.id,
            reason=reason,
        )
    except InvalidTransitionError as error:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=str(error)) from error
    return {**_status(db, employee), "event_type": event.event_type, "event_time": event.event_time.isoformat()}


def _verify_face(db: Session, employee: Employee, device: KioskDevice, embeddings: list[list[float]] | None) -> float:
    if not embeddings:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Face verification required")
    if face_limiter.is_locked(employee.id):
        raise HTTPException(status_code=status.HTTP_429_TOO_MANY_REQUESTS, detail="Too many attempts, try again later")
    try:
        score = match_score(db, employee, embeddings)
    except InvalidEmbeddingError as error:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(error)) from error
    except FaceDataUnavailableError as error:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Face data unavailable") from error
    if score < settings.face_match_threshold:
        face_limiter.fail(employee.id)
        audit(db, f"kiosk:{device.id}", "face_rejected", f"employee={employee.id} score={score:.3f}")
        db.commit()
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Face not recognized")
    face_limiter.reset(employee.id)
    return score


@router.post("/kiosk/face/enroll", response_model=KioskEmployeeStatus)
def kiosk_face_enroll(
    payload: KioskFaceEnrollRequest,
    db: Session = Depends(get_db),
    device: KioskDevice = Depends(require_kiosk),
):
    employee = _authenticate_employee(db, payload)
    if kiosk_face_step(db, employee) != "enroll":
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Face enrollment is not authorized")
    try:
        store_templates(db, employee, payload.embeddings, device.id)
    except InvalidEmbeddingError as error:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(error)) from error
    except FaceDataUnavailableError as error:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Face data unavailable") from error
    return _status(db, employee)


@router.get("/kiosk/devices")
def list_kiosk_devices(db: Session = Depends(get_db), current_user: User = Depends(require_admin)):
    devices = db.query(KioskDevice).order_by(KioskDevice.created_at.desc()).all()
    return [{
        "id": device.id,
        "device_name": device.device_name,
        "active": device.active,
        "last_seen_at": device.last_seen_at.isoformat() if device.last_seen_at else None,
        "created_at": device.created_at.isoformat(),
    } for device in devices]


@router.post("/kiosk/devices/{device_id}/revoke", status_code=status.HTTP_204_NO_CONTENT)
def revoke_kiosk_device(device_id: str, db: Session = Depends(get_db), current_user: User = Depends(require_admin)):
    device = db.query(KioskDevice).filter(KioskDevice.id == device_id).first()
    if not device:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Kiosk not found")
    device.active = False
    db.commit()
