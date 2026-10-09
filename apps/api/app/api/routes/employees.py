from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_db
from app.models.employee import Employee, JobFunction
from app.schemas.employee import EmployeeCreate, EmployeeRead

router = APIRouter(prefix="/api/v1", tags=["employees"])


@router.get("/functions")
def list_functions(db: Session = Depends(get_db)):
    return [{"id": item.id, "name": item.name, "active": item.active} for item in db.query(JobFunction).all()]


@router.post("/functions")
def create_function(name: str, db: Session = Depends(get_db)):
    item = JobFunction(id=f"func-{db.query(JobFunction).count() + 1}", name=name, active=True)
    db.add(item)
    db.commit()
    return {"id": item.id, "name": item.name}


@router.get("/employees", response_model=list[EmployeeRead])
def list_employees(db: Session = Depends(get_db)):
    employees = db.query(Employee).all()
    return [EmployeeRead(
        id=e.id,
        employee_code=e.employee_code,
        full_name=e.full_name,
        job_function_id=e.job_function_id,
        start_date=e.start_date,
        active=e.active,
        enrollment_state=e.enrollment_state,
    ) for e in employees]


@router.post("/employees", response_model=EmployeeRead)
def create_employee(payload: EmployeeCreate, db: Session = Depends(get_db)):
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
    )
