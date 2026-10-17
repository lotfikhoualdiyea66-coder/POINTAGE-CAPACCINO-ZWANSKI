from datetime import date, datetime, time, timedelta
from uuid import uuid4

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_db, require_admin
from app.models.employee import Employee
from app.models.shift import EmployeeShiftAssignment, ShiftTemplate
from app.models.user import User
from app.schemas.shift import (
    ShiftAssignmentCreate,
    ShiftAssignmentRead,
    ShiftTemplateCreate,
    ShiftTemplateRead,
)

router = APIRouter(prefix="/api/v1", tags=["shifts"])


def _shift_template_read(shift: ShiftTemplate) -> ShiftTemplateRead:
    return ShiftTemplateRead(
        id=shift.id,
        name=shift.name,
        start_time=shift.start_time,
        end_time=shift.end_time,
        break_minutes=shift.break_minutes,
    )


@router.get("/shift-templates", response_model=list[ShiftTemplateRead])
def list_shift_templates(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    return [_shift_template_read(item) for item in db.query(ShiftTemplate).order_by(ShiftTemplate.start_time).all()]


@router.post("/shift-templates", response_model=ShiftTemplateRead, status_code=status.HTTP_201_CREATED)
def create_shift_template(
    payload: ShiftTemplateCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    shift = ShiftTemplate(
        id=f"shift-{uuid4().hex}",
        name=payload.name,
        start_time=payload.start_time,
        end_time=payload.end_time,
        break_minutes=payload.break_minutes,
    )
    db.add(shift)
    db.commit()
    db.refresh(shift)
    return _shift_template_read(shift)


@router.get(
    "/employees/{employee_id}/shift-assignments",
    response_model=list[ShiftAssignmentRead],
)
def list_shift_assignments(
    employee_id: str,
    work_date: date | None = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    if not db.query(Employee).filter(Employee.id == employee_id).first():
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Employee not found")

    query = (
        db.query(EmployeeShiftAssignment, ShiftTemplate)
        .join(ShiftTemplate, EmployeeShiftAssignment.shift_template_id == ShiftTemplate.id)
        .filter(EmployeeShiftAssignment.employee_id == employee_id)
    )
    if work_date:
        day_start = datetime.combine(work_date, time.min)
        day_end = day_start + timedelta(days=1)
        query = query.filter(
            EmployeeShiftAssignment.valid_from < day_end,
            (EmployeeShiftAssignment.valid_to.is_(None) | (EmployeeShiftAssignment.valid_to >= day_start)),
        )

    return [
        ShiftAssignmentRead(
            id=assignment.id,
            employee_id=assignment.employee_id,
            shift_template_id=shift.id,
            shift_name=shift.name,
            valid_from=assignment.valid_from.date(),
            valid_to=assignment.valid_to.date() if assignment.valid_to else None,
        )
        for assignment, shift in query.order_by(EmployeeShiftAssignment.valid_from, ShiftTemplate.start_time).all()
    ]


@router.post(
    "/employees/{employee_id}/shift-assignments",
    response_model=ShiftAssignmentRead,
    status_code=status.HTTP_201_CREATED,
)
def create_shift_assignment(
    employee_id: str,
    payload: ShiftAssignmentCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    if not db.query(Employee).filter(Employee.id == employee_id).first():
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Employee not found")
    shift = db.query(ShiftTemplate).filter(ShiftTemplate.id == payload.shift_template_id).first()
    if not shift:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Shift template not found")
    if payload.valid_to and payload.valid_to < payload.valid_from:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="valid_to must not precede valid_from")

    assignment = EmployeeShiftAssignment(
        id=f"assignment-{uuid4().hex}",
        employee_id=employee_id,
        shift_template_id=shift.id,
        valid_from=datetime.combine(payload.valid_from, time.min),
        valid_to=datetime.combine(payload.valid_to, time.max) if payload.valid_to else None,
    )
    db.add(assignment)
    db.commit()
    db.refresh(assignment)
    return ShiftAssignmentRead(
        id=assignment.id,
        employee_id=employee_id,
        shift_template_id=shift.id,
        shift_name=shift.name,
        valid_from=payload.valid_from,
        valid_to=payload.valid_to,
    )