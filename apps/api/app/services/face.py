from __future__ import annotations

import json
import math
import uuid
from datetime import datetime

from cryptography.fernet import Fernet, InvalidToken
from sqlalchemy.orm import Session

from app.core.config import settings
from app.models.employee import Employee
from app.models.face import EmployeeFaceTemplate
from app.models.system import AuditLog, SystemSetting

EMBEDDING_SIZE = 128
FACE_CHECK_SETTING = "face_check"
ENROLLMENT_WINDOW_MINUTES = 30
# Embeddings captured in one enrollment session must look like the same person.
ENROLLMENT_CONSISTENCY_THRESHOLD = 0.5


class FaceDataUnavailableError(Exception):
    """FACE_DATA_KEY is missing or cannot decrypt stored templates."""


class InvalidEmbeddingError(ValueError):
    pass


def _cipher() -> Fernet:
    if not settings.face_data_key:
        raise FaceDataUnavailableError("FACE_DATA_KEY is not configured")
    try:
        return Fernet(settings.face_data_key.encode())
    except ValueError as error:
        raise FaceDataUnavailableError("FACE_DATA_KEY is not a valid Fernet key") from error


def normalize_embedding(values: list[float]) -> list[float]:
    if len(values) != EMBEDDING_SIZE or not all(math.isfinite(value) for value in values):
        raise InvalidEmbeddingError(f"Face embeddings must contain {EMBEDDING_SIZE} finite numbers")
    norm = math.sqrt(sum(value * value for value in values))
    if norm < 1e-6:
        raise InvalidEmbeddingError("Face embedding is empty")
    return [value / norm for value in values]


def cosine(left: list[float], right: list[float]) -> float:
    return sum(a * b for a, b in zip(left, right))


def is_face_check_enabled(db: Session) -> bool:
    setting = db.get(SystemSetting, FACE_CHECK_SETTING)
    return setting is not None and setting.value == "on"


def set_face_check_enabled(db: Session, enabled: bool, actor: str) -> None:
    setting = db.get(SystemSetting, FACE_CHECK_SETTING)
    if setting is None:
        setting = SystemSetting(key=FACE_CHECK_SETTING, value="off", description="Kiosk face check on top of PIN")
        db.add(setting)
    setting.value = "on" if enabled else "off"
    audit(db, actor, "face_check_enabled" if enabled else "face_check_disabled")
    db.commit()


def enrollment_state(employee: Employee, now: datetime | None = None) -> str:
    """Effective state; an authorization that was never used expires on its own."""
    if employee.enrollment_state == "pending":
        expires_at = employee.face_enrollment_expires_at
        if expires_at is None or expires_at <= (now or datetime.utcnow()):
            return "not_enrolled"
    return employee.enrollment_state


def kiosk_face_step(db: Session, employee: Employee) -> str:
    """What the kiosk must do after a correct PIN: "verify", "enroll" or "none"."""
    state = enrollment_state(employee)
    if state == "pending":
        return "enroll"
    if state == "enrolled" and is_face_check_enabled(db):
        return "verify"
    return "none"


def store_templates(db: Session, employee: Employee, embeddings: list[list[float]], device_id: str) -> None:
    normalized = [normalize_embedding(values) for values in embeddings]
    for index, first in enumerate(normalized):
        for second in normalized[index + 1:]:
            if cosine(first, second) < ENROLLMENT_CONSISTENCY_THRESHOLD:
                raise InvalidEmbeddingError("Enrollment captures do not match each other")

    cipher = _cipher()
    db.query(EmployeeFaceTemplate).filter(EmployeeFaceTemplate.employee_id == employee.id).delete()
    for values in normalized:
        db.add(EmployeeFaceTemplate(
            id=f"face-{uuid.uuid4().hex}",
            employee_id=employee.id,
            encrypted_embedding=cipher.encrypt(json.dumps(values).encode()).decode(),
            device_id=device_id,
            created_at=datetime.utcnow(),
        ))
    employee.enrollment_state = "enrolled"
    employee.face_enrollment_expires_at = None
    audit(db, f"kiosk:{device_id}", "face_enrolled", f"employee={employee.id} templates={len(normalized)}")
    db.commit()


def match_score(db: Session, employee: Employee, embeddings: list[list[float]]) -> float:
    """Lowest best-match score across the submitted captures; every capture must match the employee."""
    cipher = _cipher()
    templates = db.query(EmployeeFaceTemplate).filter(EmployeeFaceTemplate.employee_id == employee.id).all()
    if not templates:
        return -1.0
    try:
        stored = [json.loads(cipher.decrypt(item.encrypted_embedding.encode())) for item in templates]
    except InvalidToken as error:
        raise FaceDataUnavailableError("Stored face templates cannot be decrypted with FACE_DATA_KEY") from error
    return min(max(cosine(probe, template) for template in stored) for probe in map(normalize_embedding, embeddings))


def delete_face_data(db: Session, employee: Employee, actor: str) -> int:
    deleted = db.query(EmployeeFaceTemplate).filter(EmployeeFaceTemplate.employee_id == employee.id).delete()
    employee.enrollment_state = "not_enrolled"
    employee.face_enrollment_expires_at = None
    audit(db, actor, "face_data_deleted", f"employee={employee.id} templates={deleted}")
    db.commit()
    return deleted


def audit(db: Session, actor: str, action: str, details: str | None = None) -> None:
    db.add(AuditLog(id=f"audit-{uuid.uuid4().hex}", actor=actor, action=action, details=details, created_at=datetime.utcnow()))
