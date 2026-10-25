import pytest
from fastapi.testclient import TestClient

from app.api.routes.kiosk import pairing_limiter, pin_limiter
from app.core.config import settings


@pytest.fixture
def admin_headers(client: TestClient):
    client.post("/api/v1/auth/bootstrap")
    login = client.post("/api/v1/auth/login", json={"email": "admin@cappuccino.example", "password": "test-only-password"})
    return {"Authorization": f"Bearer {login.json()['access_token']}"}


@pytest.fixture
def kiosk_headers(client: TestClient):
    original = settings.kiosk_pairing_code
    settings.kiosk_pairing_code = "12345678"
    pairing_limiter.clear()
    pin_limiter.clear()
    response = client.post("/api/v1/kiosk/pair", json={"pairing_code": "12345678", "device_name": "Test phone"})
    assert response.status_code == 200
    yield {"X-Kiosk-Token": response.json()["kiosk_token"]}
    settings.kiosk_pairing_code = original


def create_employee(client: TestClient, headers: dict, code: str = "EMP-PIN", pin: str | None = "4321"):
    payload = {"employee_code": code, "full_name": "Kiosk Worker", "start_date": "2025-01-01", "active": True}
    if pin:
        payload["pin"] = pin
    response = client.post("/api/v1/employees", json=payload, headers=headers)
    assert response.status_code == 200
    return response.json()


def test_pairing_rejects_wrong_code_and_locks_out(client: TestClient):
    settings.kiosk_pairing_code = "12345678"
    pairing_limiter.clear()
    try:
        for _ in range(5):
            assert client.post("/api/v1/kiosk/pair", json={"pairing_code": "00000000", "device_name": "x"}).status_code == 401
        locked = client.post("/api/v1/kiosk/pair", json={"pairing_code": "12345678", "device_name": "x"})
        assert locked.status_code == 429
    finally:
        settings.kiosk_pairing_code = ""
        pairing_limiter.clear()


def test_kiosk_routes_require_pairing(client: TestClient):
    body = {"employee_code": "EMP-PIN", "pin": "4321"}
    assert client.post("/api/v1/kiosk/identify", json=body).status_code == 401
    assert client.post("/api/v1/kiosk/identify", json=body, headers={"X-Kiosk-Token": "bogus"}).status_code == 401


def test_employee_clocks_in_and_out_with_pin(client: TestClient, admin_headers, kiosk_headers):
    employee = create_employee(client, admin_headers)
    assert employee["has_pin"] is True
    assert "pin" not in employee

    identified = client.post("/api/v1/kiosk/identify", json={"employee_code": "emp-pin", "pin": "4321"}, headers=kiosk_headers)
    assert identified.status_code == 200
    assert identified.json()["state"] == "clocked_out"
    assert identified.json()["allowed_actions"] == ["CLOCK_IN"]

    clock = {"employee_code": "EMP-PIN", "pin": "4321", "event_type": "CLOCK_IN", "idempotency_key": "tap-0001"}
    first = client.post("/api/v1/kiosk/clock", json=clock, headers=kiosk_headers)
    assert first.status_code == 200
    assert first.json()["state"] == "clocked_in"
    assert first.json()["allowed_actions"] == ["BREAK_START", "CLOCK_OUT"]

    retry = client.post("/api/v1/kiosk/clock", json=clock, headers=kiosk_headers)
    assert retry.status_code == 200
    assert retry.json()["event_time"] == first.json()["event_time"]

    duplicate_in = client.post("/api/v1/kiosk/clock", json={**clock, "idempotency_key": "tap-0002"}, headers=kiosk_headers)
    assert duplicate_in.status_code == 409

    out = client.post("/api/v1/kiosk/clock", json={**clock, "event_type": "CLOCK_OUT", "idempotency_key": "tap-0003"}, headers=kiosk_headers)
    assert out.status_code == 200
    assert out.json()["state"] == "clocked_out"

    events = client.get("/api/v1/attendance", headers=admin_headers).json()
    assert {event["event_type"] for event in events} == {"CLOCK_IN", "CLOCK_OUT"}
    assert all(event["source"] == "kiosk_pin" for event in events)


def test_wrong_pin_is_rejected_and_locked(client: TestClient, admin_headers, kiosk_headers):
    create_employee(client, admin_headers)
    for _ in range(5):
        response = client.post("/api/v1/kiosk/identify", json={"employee_code": "EMP-PIN", "pin": "0000"}, headers=kiosk_headers)
        assert response.status_code == 401
    locked = client.post("/api/v1/kiosk/identify", json={"employee_code": "EMP-PIN", "pin": "4321"}, headers=kiosk_headers)
    assert locked.status_code == 429


def test_employee_without_pin_cannot_clock_until_admin_sets_one(client: TestClient, admin_headers, kiosk_headers):
    employee = create_employee(client, admin_headers, code="EMP-NOPIN", pin=None)
    assert employee["has_pin"] is False
    body = {"employee_code": "EMP-NOPIN", "pin": "1111"}
    assert client.post("/api/v1/kiosk/identify", json=body, headers=kiosk_headers).status_code == 401

    assert client.put(f"/api/v1/employees/{employee['id']}/pin", json={"pin": "12"}, headers=admin_headers).status_code == 422
    assert client.put(f"/api/v1/employees/{employee['id']}/pin", json={"pin": "1111"}, headers=admin_headers).status_code == 204
    pin_limiter.clear()
    assert client.post("/api/v1/kiosk/identify", json=body, headers=kiosk_headers).status_code == 200


def test_revoked_kiosk_is_rejected(client: TestClient, admin_headers, kiosk_headers):
    devices = client.get("/api/v1/kiosk/devices", headers=admin_headers).json()
    assert len(devices) == 1 and devices[0]["active"] is True
    assert client.post(f"/api/v1/kiosk/devices/{devices[0]['id']}/revoke", headers=admin_headers).status_code == 204
    assert client.get("/api/v1/kiosk/me", headers=kiosk_headers).status_code == 401
