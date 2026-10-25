from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_db, require_admin
from app.models.attendance import AttendanceEvent
from app.models.employee import Employee
from app.models.user import User
from app.schemas.attendance import EventCreate, EventRead
from app.services.attendance_events import InvalidTransitionError, record_event

router = APIRouter(prefix="/api/v1", tags=["attendance"])


@router.get("/attendance")
def list_events(db: Session = Depends(get_db), current_user: User = Depends(require_admin)):
    events = db.query(AttendanceEvent).order_by(AttendanceEvent.event_time.desc()).all()
    return [{
        "id": event.id,
        "employee_id": event.employee_id,
        "event_type": event.event_type,
        "event_time": event.event_time.isoformat(),
        "status": event.status,
        "source": event.source,
        "device_id": event.device_id,
    } for event in events]


@router.post("/attendance/events", response_model=EventRead)
def create_event(
    payload: EventCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    employee = db.query(Employee).filter(Employee.id == payload.employee_id).first()
    if not employee:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Employee not found")
    try:
        item = record_event(
            db,
            employee_id=payload.employee_id,
            event_type=payload.event_type,
            event_time=payload.event_time,
            source=payload.source,
            idempotency_key=payload.idempotency_key,
            device_id=payload.device_id,
            reason=payload.reason,
        )
    except InvalidTransitionError as error:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=str(error)) from error
    return EventRead(
        id=item.id,
        employee_id=item.employee_id,
        event_type=item.event_type,
        event_time=item.event_time,
        source=item.source,
        device_id=item.device_id,
        idempotency_key=item.idempotency_key,
        status=item.status,
    )
