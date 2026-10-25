from typing import Literal

from pydantic import BaseModel


class FaceCheckSettings(BaseModel):
    enabled: bool


class FaceCheckStatus(FaceCheckSettings):
    encryption_configured: bool
    match_threshold: float


class FaceEnrollmentAuthorization(BaseModel):
    # The administrator confirms the employee gave written consent before any capture.
    consent_confirmed: Literal[True]
