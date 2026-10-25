from datetime import date
from typing import Optional

from pydantic import BaseModel, Field

PIN_PATTERN = r"^\d{4,8}$"


class EmployeeBase(BaseModel):
    employee_code: str = Field(..., min_length=2, max_length=64)
    full_name: str = Field(..., min_length=2, max_length=255)
    job_function_id: Optional[str] = None
    start_date: date
    active: bool = True


class EmployeeCreate(EmployeeBase):
    pin: Optional[str] = Field(default=None, pattern=PIN_PATTERN)


class EmployeeRead(EmployeeBase):
    id: str
    enrollment_state: str = "not_enrolled"
    has_pin: bool = False


class EmployeePinUpdate(BaseModel):
    pin: str = Field(..., pattern=PIN_PATTERN)
