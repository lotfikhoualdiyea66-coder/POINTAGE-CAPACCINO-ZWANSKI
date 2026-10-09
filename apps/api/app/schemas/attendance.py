from datetime import datetime

from pydantic import BaseModel, Field


class EventCreate(BaseModel):
    employee_id: str
    event_type: str = Field(..., pattern=r"^(CLOCK_IN|CLOCK_OUT|BREAK_START|BREAK_END)$")
    event_time: datetime
    source: str = "face"
    device_id: str | None = None
    idempotency_key: str = Field(..., min_length=3, max_length=128)
    reason: str | None = None


class EventRead(EventCreate):
    id: str
    status: str = "confirmed"
