import json
import random

import pytest
from cryptography.fernet import Fernet
from fastapi.testclient import TestClient

from app.api.routes.kiosk import face_limiter
from app.core.config import settings
from app.db.session import SessionLocal
from app.models.face import EmployeeFaceTemplate
from app.models.system import AuditLog
from tests.test_kiosk import admin_headers, create_employee, kiosk_headers  # noqa: F401 (fixtures)


def synthetic_face(seed: int) -> list[float]:
    generator = random.Random(seed)
    return [generator.gauss(0, 1) for _ in range(128)]


def variant(face: list[float], seed: int, noise: float = 0.15) -> list[float]:
    generator = random.Random(seed)
    return [value + generator.gauss(0, noise) for value in face]


@pytest.fixture(autouse=True)
def face_key():
    original = settings.face_data_key
    settings.face_data_key = Fernet.generate_key().decode()
    face_limiter.clear()
    yield
    settings.face_data_key = original


@pytest.fixture
def enrolled(client: TestClient, admin_headers, kiosk_headers):
    employee = create_employee(client, admin_headers)
    response = client.post(f"/api/v1/employees/{employee['id']}/face/authorize", json={"consent_confirmed": True}, headers=admin_headers)
    assert response.status_code == 204
    face = synthetic_face(1)
    identify = client.post("/api/v1/kiosk/identify", json={"employee_code": "EMP-PIN", "pin": "4321"}, headers=kiosk_headers)
    assert identify.json()["face"] == "enroll"
    enroll = client.post(
        "/api/v1/kiosk/face/enroll",
        json={"employee_code": "EMP-PIN", "pin": "4321", "embeddings": [variant(face, seed) for seed in (10, 11, 12)]},
        headers=kiosk_headers,
    )
    assert enroll.status_code == 200
    return employee, face


def clock(client, headers, key, embeddings=None):
    body = {"employee_code": "EMP-PIN", "pin": "4321", "event_type": "CLOCK_IN", "idempotency_key": key}
    if embeddings is not None:
        body["face_embeddings"] = embeddings
    return client.post("/api/v1/kiosk/clock", json=body, headers=headers)


def test_authorization_requires_consent_and_pin(client: TestClient, admin_headers):
    with_pin = create_employee(client, admin_headers)
    no_pin = create_employee(client, admin_headers, code="EMP-NOPIN", pin=None)
    url = f"/api/v1/employees/{with_pin['id']}/face/authorize"
    assert client.post(url, json={"consent_confirmed": False}, headers=admin_headers).status_code == 422
    assert client.post(url, json={}, headers=admin_headers).status_code == 422
    assert client.post(f"/api/v1/employees/{no_pin['id']}/face/authorize", json={"consent_confirmed": True}, headers=admin_headers).status_code == 409


def test_enrollment_without_authorization_is_refused(client: TestClient, admin_headers, kiosk_headers):
    create_employee(client, admin_headers)
    face = synthetic_face(1)
    response = client.post(
        "/api/v1/kiosk/face/enroll",
        json={"employee_code": "EMP-PIN", "pin": "4321", "embeddings": [variant(face, s) for s in (1, 2, 3)]},
        headers=kiosk_headers,
    )
    assert response.status_code == 409


def test_enrollment_rejects_captures_of_different_people(client: TestClient, admin_headers, kiosk_headers):
    employee = create_employee(client, admin_headers)
    client.post(f"/api/v1/employees/{employee['id']}/face/authorize", json={"consent_confirmed": True}, headers=admin_headers)
    response = client.post(
        "/api/v1/kiosk/face/enroll",
        json={"employee_code": "EMP-PIN", "pin": "4321", "embeddings": [synthetic_face(1), synthetic_face(2), synthetic_face(3)]},
        headers=kiosk_headers,
    )
    assert response.status_code == 422


def test_templates_are_encrypted_at_rest(client: TestClient, enrolled):
    employee, _ = enrolled
    with SessionLocal() as db:
        templates = db.query(EmployeeFaceTemplate).filter_by(employee_id=employee["id"]).all()
        assert len(templates) == 3
        for template in templates:
            with pytest.raises(json.JSONDecodeError):
                json.loads(template.encrypted_embedding)


def test_face_check_off_keeps_pin_only(client: TestClient, kiosk_headers, enrolled):
    identify = client.post("/api/v1/kiosk/identify", json={"employee_code": "EMP-PIN", "pin": "4321"}, headers=kiosk_headers)
    assert identify.json()["face"] == "none"
    response = clock(client, kiosk_headers, "tap-off-1")
    assert response.status_code == 200


def test_face_check_on_requires_matching_face(client: TestClient, admin_headers, kiosk_headers, enrolled):
    _, face = enrolled
    assert client.put("/api/v1/settings/face-check", json={"enabled": True}, headers=admin_headers).json()["enabled"] is True
    identify = client.post("/api/v1/kiosk/identify", json={"employee_code": "EMP-PIN", "pin": "4321"}, headers=kiosk_headers)
    assert identify.json()["face"] == "verify"

    assert clock(client, kiosk_headers, "tap-face-1").status_code == 403
    assert clock(client, kiosk_headers, "tap-face-2", [synthetic_face(99)]).status_code == 403
    # One matching and one foreign capture: every capture must match.
    assert clock(client, kiosk_headers, "tap-face-3", [variant(face, 50), synthetic_face(98)]).status_code == 403

    accepted = clock(client, kiosk_headers, "tap-face-4", [variant(face, 51), variant(face, 52)])
    assert accepted.status_code == 200
    events = client.get("/api/v1/attendance", headers=admin_headers).json()
    assert events[0]["source"] == "kiosk_pin_face"

    with SessionLocal() as db:
        assert db.query(AuditLog).filter_by(action="face_rejected").count() == 2


def test_repeated_face_mismatch_locks(client: TestClient, admin_headers, kiosk_headers, enrolled):
    _, face = enrolled
    client.put("/api/v1/settings/face-check", json={"enabled": True}, headers=admin_headers)
    for attempt in range(5):
        assert clock(client, kiosk_headers, f"tap-lock-{attempt}", [synthetic_face(200 + attempt)]).status_code == 403
    assert clock(client, kiosk_headers, "tap-lock-final", [variant(face, 60)]).status_code == 429


def test_deleting_face_data_returns_to_pin_only(client: TestClient, admin_headers, kiosk_headers, enrolled):
    employee, _ = enrolled
    client.put("/api/v1/settings/face-check", json={"enabled": True}, headers=admin_headers)
    assert client.delete(f"/api/v1/employees/{employee['id']}/face", headers=admin_headers).status_code == 204
    with SessionLocal() as db:
        assert db.query(EmployeeFaceTemplate).filter_by(employee_id=employee["id"]).count() == 0
    listed = client.get("/api/v1/employees", headers=admin_headers).json()[0]
    assert listed["enrollment_state"] == "not_enrolled"
    assert clock(client, kiosk_headers, "tap-deleted").status_code == 200


def test_face_check_cannot_be_enabled_without_key(client: TestClient, admin_headers):
    settings.face_data_key = ""
    assert client.put("/api/v1/settings/face-check", json={"enabled": True}, headers=admin_headers).status_code == 503
