from datetime import date, time

from pydantic import BaseModel, Field


class ShiftTemplateCreate(BaseModel):
    name: str = Field(..., min_length=2, max_length=255)
    start_time: time
    end_time: time
    break_minutes: int = Field(default=30, ge=0, le=720)


class ShiftTemplateRead(ShiftTemplateCreate):
    id: str


class ShiftAssignmentCreate(BaseModel):
    shift_template_id: str
    valid_from: date
    valid_to: date | None = None


class ShiftAssignmentRead(ShiftAssignmentCreate):
    id: str
    employee_id: str
    shift_name: str