from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_db, require_admin
from app.models.employee import Employee, JobFunction
from app.models.user import User
from app.core.security import pwd_context
from app.schemas.employee import EmployeeCreate, EmployeePinUpdate, EmployeeRead

router = APIRouter(prefix="/api/v1", tags=["employees"])


@router.get("/functions")
def list_functions(db: Session = Depends(get_db), current_user: User = Depends(require_admin)):
    return [{"id": item.id, "name": item.name, "active": item.active} for item in db.query(JobFunction).all()]


@router.post("/functions")
def create_function(name: str, db: Session = Depends(get_db), current_user: User = Depends(require_admin)):
    item = JobFunction(id=f"func-{db.query(JobFunction).count() + 1}", name=name, active=True)
    db.add(item)
    db.commit()
    return {"id": item.id, "name": item.name}


@router.get("/employees", response_model=list[EmployeeRead])
def list_employees(db: Session = Depends(get_db), current_user: User = Depends(require_admin)):
    employees = db.query(Employee).all()
    return [EmployeeRead(
        id=e.id,
        employee_code=e.employee_code,
        full_name=e.full_name,
        job_function_id=e.job_function_id,
        start_date=e.start_date,
        active=e.active,
        enrollment_state=e.enrollment_state,
        has_pin=e.pin_hash is not None,
    ) for e in employees]


@router.post("/employees", response_model=EmployeeRead)
def create_employee(
    payload: EmployeeCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    if db.query(Employee).filter(Employee.employee_code == payload.employee_code).first():
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Employee code already exists")
    employee = Employee(
        id=f"emp-{db.query(Employee).count() + 1}",
        employee_code=payload.employee_code,
        full_name=payload.full_name,
        job_function_id=payload.job_function_id,
        start_date=payload.start_date,
        active=payload.active,
        enrollment_state="not_enrolled",
        pin_hash=pwd_context.hash(payload.pin) if payload.pin else None,
    )
    db.add(employee)
    db.commit()
    db.refresh(employee)
    return EmployeeRead(
        id=employee.id,
        employee_code=employee.employee_code,
        full_name=employee.full_name,
        job_function_id=employee.job_function_id,
        start_date=employee.start_date,
        active=employee.active,
        enrollment_state=employee.enrollment_state,
        has_pin=employee.pin_hash is not None,
    )


@router.put("/employees/{employee_id}/pin", status_code=status.HTTP_204_NO_CONTENT)
def set_employee_pin(
    employee_id: str,
    payload: EmployeePinUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    employee = db.query(Employee).filter(Employee.id == employee_id).first()
    if not employee:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Employee not found")
    employee.pin_hash = pwd_context.hash(payload.pin)
    db.commit()
