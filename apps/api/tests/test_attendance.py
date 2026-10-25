from datetime import date, datetime, time

from fastapi.testclient import TestClient

from app.models.attendance import AttendanceEvent
from app.models.shift import EmployeeShiftAssignment, ShiftTemplate


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
    assert client.get("/api/v1/dashboard/workday").status_code == 401


def test_workday_summary_reports_double_shift_and_short_day(client: TestClient):
    client.post("/api/v1/auth/bootstrap")
    login = client.post(
        "/api/v1/auth/login",
        json={"email": "admin@cappuccino.example", "password": "test-only-password"},
    )
    headers = {"Authorization": f"Bearer {login.json()['access_token']}"}
    employee_response = client.post(
        "/api/v1/employees",
        json={
            "employee_code": "EMP-DOUBLE",
            "full_name": "Nadia Kefi",
            "job_function_id": None,
            "start_date": "2025-01-01",
            "active": True,
        },
        headers=headers,
    )
    employee_id = employee_response.json()["id"]
    shift_templates = []
    for name, start_time, end_time, break_minutes in [
        ("Day", "08:00:00", "17:00:00", 60),
        ("Evening", "18:00:00", "22:00:00", 30),
    ]:
        response = client.post(
            "/api/v1/shift-templates",
            json={"name": name, "start_time": start_time, "end_time": end_time, "break_minutes": break_minutes},
            headers=headers,
        )
        assert response.status_code == 201
        shift_templates.append(response.json())

    for shift in shift_templates:
        response = client.post(
            f"/api/v1/employees/{employee_id}/shift-assignments",
            json={"shift_template_id": shift["id"], "valid_from": "2025-01-01"},
            headers=headers,
        )
        assert response.status_code == 201

    for event_type, event_time in [
        ("CLOCK_IN", "2025-01-10T08:00:00"),
        ("BREAK_START", "2025-01-10T12:00:00"),
        ("BREAK_END", "2025-01-10T13:00:00"),
        ("CLOCK_OUT", "2025-01-10T17:00:00"),
    ]:
        response = client.post(
            "/api/v1/attendance/events",
            json={
                "employee_id": employee_id,
                "event_type": event_type,
                "event_time": event_time,
                "source": "admin_test",
                "device_id": "test-kiosk",
                "idempotency_key": f"{employee_id}-{event_type}",
            },
            headers=headers,
        )
        assert response.status_code == 200

    response = client.get(
        "/api/v1/dashboard/workday",
        params={"work_date": date(2025, 1, 10).isoformat()},
        headers=headers,
    )

    assert response.status_code == 200
    summary = response.json()["employees"][0]
    assert summary["worked_minutes"] == 480
    assert summary["target_minutes"] == 690
    assert summary["remaining_minutes"] == 210
    assert summary["days_worked_this_month"] == 1
    assert summary["is_double_shift"] is True
    assert summary["status"] == "short_day"