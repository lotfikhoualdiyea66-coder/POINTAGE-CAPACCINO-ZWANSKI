from collections import defaultdict
from datetime import date, datetime, time, timedelta
from zoneinfo import ZoneInfo

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_db, require_admin
from app.models.attendance import AttendanceEvent
from app.models.employee import Employee
from app.models.shift import EmployeeShiftAssignment, ShiftTemplate
from app.models.user import User
from app.core.config import settings
from app.services.attendance_rules import state_from_events
from app.services.timecalc import compute_minutes_from_events

router = APIRouter(prefix="/api/v1", tags=["dashboard"])


@router.get("/dashboard/overview")
def dashboard_overview(db: Session = Depends(get_db), current_user: User = Depends(require_admin)):
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


@router.get("/dashboard/workday")
def dashboard_workday(
    work_date: date | None = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    local_now = datetime.now(ZoneInfo(settings.timezone))
    selected_date = work_date or local_now.date()
    day_start = datetime.combine(selected_date, time.min)
    day_end = day_start + timedelta(days=1)
    employees = db.query(Employee).filter(Employee.active.is_(True)).order_by(Employee.full_name).all()
    employee_ids = [employee.id for employee in employees]
    if not employee_ids:
        return {"work_date": selected_date.isoformat(), "employees": []}

    events_by_employee: dict[str, list[dict]] = defaultdict(list)
    events = (
        db.query(AttendanceEvent)
        .filter(
            AttendanceEvent.employee_id.in_(employee_ids),
            AttendanceEvent.event_time >= day_start,
            AttendanceEvent.event_time < day_end,
        )
        .order_by(AttendanceEvent.event_time)
        .all()
    )
    for event in events:
        events_by_employee[event.employee_id].append(
            {"event_type": event.event_type, "event_time": event.event_time}
        )

    month_start = datetime.combine(selected_date.replace(day=1), time.min)
    if selected_date.month == 12:
        next_month = selected_date.replace(year=selected_date.year + 1, month=1, day=1)
    else:
        next_month = selected_date.replace(month=selected_date.month + 1, day=1)
    month_end = datetime.combine(next_month, time.min)
    month_clock_ins = (
        db.query(AttendanceEvent.employee_id, AttendanceEvent.event_time)
        .filter(
            AttendanceEvent.employee_id.in_(employee_ids),
            AttendanceEvent.event_type == "CLOCK_IN",
            AttendanceEvent.event_time >= month_start,
            AttendanceEvent.event_time < month_end,
        )
        .all()
    )
    worked_days_by_employee: dict[str, set[date]] = defaultdict(set)
    for employee_id, event_time in month_clock_ins:
        worked_days_by_employee[employee_id].add(event_time.date())

    schedule_rows = (
        db.query(EmployeeShiftAssignment, ShiftTemplate)
        .join(ShiftTemplate, EmployeeShiftAssignment.shift_template_id == ShiftTemplate.id)
        .filter(
            EmployeeShiftAssignment.employee_id.in_(employee_ids),
            EmployeeShiftAssignment.valid_from < day_end,
            (EmployeeShiftAssignment.valid_to.is_(None) | (EmployeeShiftAssignment.valid_to >= day_start)),
        )
        .order_by(EmployeeShiftAssignment.valid_from, ShiftTemplate.start_time)
        .all()
    )
    shifts_by_employee: dict[str, list[ShiftTemplate]] = defaultdict(list)
    for assignment, shift in schedule_rows:
        shifts_by_employee[assignment.employee_id].append(shift)

    is_today = selected_date == local_now.date()
    as_of = local_now.replace(tzinfo=None) if is_today else None
    summaries = []
    for employee in employees:
        employee_events = events_by_employee[employee.id]
        shifts = shifts_by_employee[employee.id]
        scheduled_targets = []
        for shift in shifts:
            start_at = datetime.combine(selected_date, shift.start_time)
            end_at = datetime.combine(selected_date, shift.end_time)
            duration_minutes = int((end_at - start_at).total_seconds() / 60)
            if duration_minutes <= 0:
                duration_minutes += 24 * 60
            scheduled_targets.append(max(duration_minutes - shift.break_minutes, 0))

        target_minutes = sum(scheduled_targets) if scheduled_targets else 8 * 60
        totals = compute_minutes_from_events(employee_events, as_of=as_of)
        work_state = state_from_events(employee_events)
        worked_minutes = totals["worked_minutes"]
        if work_state == "on_break":
            status = "on_break"
        elif work_state == "clocked_in":
            status = "target_reached" if worked_minutes >= target_minutes else "working"
        elif not employee_events:
            status = "not_started"
        else:
            status = "complete" if worked_minutes >= target_minutes else "short_day"

        summaries.append({
            "employee_id": employee.id,
            "employee_code": employee.employee_code,
            "full_name": employee.full_name,
            "work_date": selected_date.isoformat(),
            "days_worked_this_month": len(worked_days_by_employee[employee.id]),
            "worked_minutes": worked_minutes,
            "break_minutes": totals["break_minutes"],
            "target_minutes": target_minutes,
            "remaining_minutes": max(target_minutes - worked_minutes, 0),
            "overtime_minutes": max(worked_minutes - target_minutes, 0),
            "shift_count": len(shifts),
            "is_double_shift": len(shifts) > 1,
            "shifts": [{
                "name": shift.name,
                "start_time": shift.start_time.isoformat(),
                "end_time": shift.end_time.isoformat(),
                "break_minutes": shift.break_minutes,
            } for shift in shifts],
            "status": status,
        })

    return {"work_date": selected_date.isoformat(), "employees": summaries}
