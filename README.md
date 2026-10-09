# Cappuccino Lac 1 Attendance & Shift Management System

This repository contains a local-first attendance and shift management system for Cappuccino Lac 1, with a FastAPI backend, a React dashboard, an Android kiosk shell, and operational docs.

## Stack
- Backend: Python, FastAPI, SQLAlchemy, Alembic, Pydantic
- Dashboard: TypeScript, React, Vite
- Kiosk: Kotlin + Jetpack Compose shell
- Database: PostgreSQL in production; SQLite fallback for local tests and development

## Quick start

### Backend
`powershell
cd apps/api
python -m venv .venv
. .venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env
# Edit .env and set a unique INITIAL_ADMIN_PASSWORD before first bootstrap.
python -m pytest
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
`

### Frontend
`powershell
cd apps/web
npm install
npm run build
npm run dev -- --host 0.0.0.0
`

### Android kiosk
The Android project builds a debug APK with the Gradle wrapper:

```powershell
cd apps/android-kiosk
.\gradlew.bat assembleDebug
```

APK output: `apps/android-kiosk/app/build/outputs/apk/debug/app-debug.apk`.
Install on an authorized USB-connected device with:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

See [docs/ANDROID_KIOSK_SETUP.md](docs/ANDROID_KIOSK_SETUP.md) for the S22 setup steps and current limitations.

### Docker Compose database
Before starting Compose from `infra`, copy `.env.example` to `.env` and set a unique `POSTGRES_PASSWORD`. Compose refuses to start the database if it is not set; the database port is bound to loopback only.

## Documentation
- docs/ARCHITECTURE.md
- docs/DECISIONS.md
- docs/IMPLEMENTATION_STATUS.md
- docs/ANDROID_KIOSK_SETUP.md
- docs/MODEL_CATALOG.md
- docs/SECURITY.md

## Important
The project is intentionally LAN-only and does not enable live biometric recognition without a verified, licensed provider and administrator approval.
