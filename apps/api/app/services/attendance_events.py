from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy.orm import Session

from app.models.attendance import AttendanceEvent
from app.services.attendance_rules import VALID_TRANSITIONS, state_from_events, validate_transition


class InvalidTransitionError(Exception):
    pass


def current_state(db: Session, employee_id: str) -> str:
    events = db.query(AttendanceEvent.event_type, AttendanceEvent.event_time).filter(
        AttendanceEvent.employee_id == employee_id
    ).all()
    return state_from_events([{"event_type": event_type, "event_time": event_time} for event_type, event_time in events])


def allowed_actions(state: str) -> list[str]:
    return sorted(VALID_TRANSITIONS.get(state, set()))


def record_event(
    db: Session,
    *,
    employee_id: str,
    event_type: str,
    event_time: datetime,
    source: str,
    idempotency_key: str,
    device_id: str | None = None,
    reason: str | None = None,
) -> AttendanceEvent:
    """Store an attendance event, returning the existing one for a repeated idempotency key."""
    existing = db.query(AttendanceEvent).filter(AttendanceEvent.idempotency_key == idempotency_key).first()
    if existing:
        return existing

    valid, message = validate_transition(current_state(db, employee_id), event_type)
    if not valid:
        raise InvalidTransitionError(message)

    now = datetime.utcnow()
    item = AttendanceEvent(
        id=f"event-{uuid.uuid4().hex}",
        employee_id=employee_id,
        event_type=event_type,
        event_time=event_time,
        source=source,
        idempotency_key=idempotency_key,
        device_id=device_id,
        status="confirmed",
        reason=reason,
        created_at=now,
        updated_at=now,
    )
    db.add(item)
    db.commit()
    db.refresh(item)
    return item
