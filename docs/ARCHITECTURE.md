# Architecture overview

## Components
- `apps/api`: FastAPI + SQLAlchemy backend, Alembic migrations, tests, and attendance engine.
- `apps/web`: React + TypeScript dashboard for managers.
- `apps/android-kiosk`: Kotlin/Jetpack Compose shell for the Samsung S22 kiosk.
- `infra`: Compose file, backup scripts, systemd examples, and local-config helpers.
- `docs`: design and operational documentation.

## Runtime topology
1. Ubuntu server hosts Postgres and FastAPI.
2. The manager browser connects to the local dashboard through the LAN.
3. The kiosk pairs with the server over TLS and submits attendance events.
4. WebSocket updates propagate accepted events to the dashboard without cloud dependencies.

## Safety model
- The server is authoritative for all attendance decisions.
- The kiosk stores only encrypted pending events until confirmation.
- Biometric recognition is behind an explicit provider boundary and disabled by default.
- All monthly and shift calculations are generated from deterministic policy modules rather than UI logic.
