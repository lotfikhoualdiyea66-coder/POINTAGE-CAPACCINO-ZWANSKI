from fastapi.testclient import TestClient


def test_bootstrap_and_employee_flow(client: TestClient):
    bootstrap = client.post("/api/v1/auth/bootstrap")
    assert bootstrap.status_code == 200

    login = client.post("/api/v1/auth/login", json={"email": "admin@cappuccino.example", "password": "test-only-password"})
    assert login.status_code == 200
    token = login.json()["access_token"]
    assert token != "admin@cappuccino.example"
    headers = {"Authorization": f"Bearer {token}"}

    employee = client.post("/api/v1/employees", json={
        "employee_code": "EMP-001",
        "full_name": "Amina Ben Ali",
        "job_function_id": None,
        "start_date": "2025-01-01",
        "active": True,
    }, headers=headers)
    assert employee.status_code == 200
    created = employee.json()
    assert created["employee_code"] == "EMP-001"
    assert client.get("/api/v1/employees", headers=headers).json()[0]["id"] == created["id"]

    event = client.post("/api/v1/attendance/events", json={
        "employee_id": created["id"],
        "event_type": "CLOCK_IN",
        "event_time": "2025-01-10T08:00:00",
        "source": "face",
        "device_id": "kiosk-001",
        "idempotency_key": "evt-123",
    }, headers=headers)
    assert event.status_code == 200
    assert event.json()["status"] == "confirmed"

    duplicate = client.post("/api/v1/attendance/events", json={
        "employee_id": created["id"],
        "event_type": "CLOCK_IN",
        "event_time": "2025-01-10T08:00:00",
        "source": "face",
        "device_id": "kiosk-001",
        "idempotency_key": "evt-123",
    }, headers=headers)
    assert duplicate.status_code == 200
    assert duplicate.json()["id"] == event.json()["id"]

    invalid = client.post("/api/v1/attendance/events", json={
        "employee_id": created["id"],
        "event_type": "CLOCK_IN",
        "event_time": "2025-01-10T09:00:00",
        "source": "face",
        "device_id": "kiosk-001",
        "idempotency_key": "evt-456",
    }, headers=headers)
    assert invalid.status_code == 409


def test_management_routes_require_admin_auth(client: TestClient):
    employee_payload = {
        "employee_code": "EMP-002",
        "full_name": "Sami Trabelsi",
        "job_function_id": None,
        "start_date": "2025-01-01",
        "active": True,
    }

    assert client.get("/api/v1/employees").status_code == 401
    assert client.post("/api/v1/employees", json=employee_payload).status_code == 401
    assert client.get("/api/v1/attendance").status_code == 401
    assert client.get("/api/v1/dashboard/overview").status_code == 401
