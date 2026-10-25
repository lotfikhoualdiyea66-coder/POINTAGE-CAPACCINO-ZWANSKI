from typing import Annotated

from pydantic import BaseModel, Field

from app.schemas.employee import PIN_PATTERN

FaceEmbedding = Annotated[list[float], Field(min_length=128, max_length=128)]


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
    face_embeddings: list[FaceEmbedding] | None = Field(default=None, min_length=1, max_length=3)


class KioskFaceEnrollRequest(KioskCredentials):
    embeddings: list[FaceEmbedding] = Field(..., min_length=3, max_length=5)


class KioskEmployeeStatus(BaseModel):
    employee_code: str
    full_name: str
    state: str
    allowed_actions: list[str]
    # "verify": capture a face before clocking; "enroll": an admin authorized face enrollment; "none": PIN only.
    face: str = "none"


class KioskClockResponse(KioskEmployeeStatus):
    event_type: str
    event_time: str
