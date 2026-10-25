from datetime import datetime, timedelta

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_db, require_admin
from app.core.config import settings
from app.models.employee import Employee
from app.models.user import User
from app.schemas.face import FaceCheckSettings, FaceCheckStatus, FaceEnrollmentAuthorization
from app.services.face import (
    ENROLLMENT_WINDOW_MINUTES,
    audit,
    delete_face_data,
    is_face_check_enabled,
    set_face_check_enabled,
)

router = APIRouter(prefix="/api/v1", tags=["face"])


def _face_status(db: Session) -> FaceCheckStatus:
    return FaceCheckStatus(
        enabled=is_face_check_enabled(db),
        encryption_configured=bool(settings.face_data_key),
        match_threshold=settings.face_match_threshold,
    )


def _get_employee(db: Session, employee_id: str) -> Employee:
    employee = db.query(Employee).filter(Employee.id == employee_id).first()
    if not employee:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Employee not found")
    return employee


@router.get("/settings/face-check", response_model=FaceCheckStatus)
def get_face_check(db: Session = Depends(get_db), current_user: User = Depends(require_admin)):
    return _face_status(db)


@router.put("/settings/face-check", response_model=FaceCheckStatus)
def update_face_check(
    payload: FaceCheckSettings,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    if payload.enabled and not settings.face_data_key:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Configure FACE_DATA_KEY first")
    set_face_check_enabled(db, payload.enabled, current_user.email)
    return _face_status(db)


@router.post("/employees/{employee_id}/face/authorize", status_code=status.HTTP_204_NO_CONTENT)
def authorize_face_enrollment(
    employee_id: str,
    payload: FaceEnrollmentAuthorization,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    if not settings.face_data_key:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Configure FACE_DATA_KEY first")
    employee = _get_employee(db, employee_id)
    if not employee.pin_hash:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Set a PIN before enrolling a face")
    employee.enrollment_state = "pending"
    employee.face_enrollment_expires_at = datetime.utcnow() + timedelta(minutes=ENROLLMENT_WINDOW_MINUTES)
    audit(db, current_user.email, "face_enrollment_authorized", f"employee={employee.id} consent_confirmed=true")
    db.commit()


@router.delete("/employees/{employee_id}/face", status_code=status.HTTP_204_NO_CONTENT)
def remove_face_data(employee_id: str, db: Session = Depends(get_db), current_user: User = Depends(require_admin)):
    delete_face_data(db, _get_employee(db, employee_id), current_user.email)
