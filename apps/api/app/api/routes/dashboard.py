from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_db
from app.models.attendance import AttendanceEvent
from app.models.employee import Employee

router = APIRouter(prefix="/api/v1", tags=["dashboard"])


@router.get("/dashboard/overview")
def dashboard_overview(db: Session = Depends(get_db)):
    employees = db.query(Employee).count()
    events = db.query(AttendanceEvent).order_by(AttendanceEvent.event_time.desc()).limit(10).all()
    return {
        "employees": employees,
        "latest_events": [{
            "id": e.id,
            "employee_id": e.employee_id,
            "event_type": e.event_type,
            "event_time": e.event_time.isoformat(),
        } for e in events],
        "status": "ok",
    }
