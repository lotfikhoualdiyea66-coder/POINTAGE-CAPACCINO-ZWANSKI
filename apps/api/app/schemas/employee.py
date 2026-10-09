from datetime import date
from typing import Optional

from pydantic import BaseModel, Field


class EmployeeCreate(BaseModel):
    employee_code: str = Field(..., min_length=2, max_length=64)
    full_name: str = Field(..., min_length=2, max_length=255)
    job_function_id: Optional[str] = None
    start_date: date
    active: bool = True


class EmployeeRead(EmployeeCreate):
    id: str
    enrollment_state: str = "not_enrolled"
