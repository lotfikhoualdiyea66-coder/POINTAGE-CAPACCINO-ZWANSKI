from pydantic import BaseModel, Field

from app.schemas.employee import PIN_PATTERN


class KioskPairRequest(BaseModel):
    pairing_code: str = Field(..., min_length=4, max_length=64)
    device_name: str = Field(..., min_length=1, max_length=255)


class KioskPairResponse(BaseModel):
    device_id: str
    device_name: str
    kiosk_token: str


class KioskCredentials(BaseModel):
    employee_code: str = Field(..., min_length=2, max_length=64)
    pin: str = Field(..., pattern=PIN_PATTERN)


class KioskClockRequest(KioskCredentials):
    event_type: str = Field(..., pattern=r"^(CLOCK_IN|CLOCK_OUT|BREAK_START|BREAK_END)$")
    idempotency_key: str = Field(..., min_length=8, max_length=64)


class KioskEmployeeStatus(BaseModel):
    employee_code: str
    full_name: str
    state: str
    allowed_actions: list[str]


class KioskClockResponse(KioskEmployeeStatus):
    event_type: str
    event_time: str
