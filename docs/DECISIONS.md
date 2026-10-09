# Design decisions

## Baseline
- The project is split into a local FastAPI backend, React dashboard, and a Kotlin kiosk shell.
- The source of truth is the Ubuntu server database; kiosk devices keep only queued events and device metadata.
- Production biometric processing remains disabled unless a commercially licensed provider is approved and configured.

## Runtime decisions
- Backend defaults to SQLite for local tooling and automated tests to keep the repository runnable without a local PostgreSQL server.
- Production deployment should use PostgreSQL on Ubuntu with Docker Compose or systemd; these configs are included as examples, not hard-coded secrets.
- Frontend uses Vite + React + TypeScript with a simple French/Arabic ready layout and branded theme.

## Biometric safety
- No face images or templates are stored in the repository.
- The recognition provider is implemented behind an interface and defaults to a demo provider that reports not configured.
- Full live biometric registration is intentionally blocked until the model and licensing path are validated.

## Hardware dependencies
- Android device provisioning, Lock Task Mode, and actual retail kiosk telemetry remain hardware-dependent and are documented as pending without the S22 and managed-device authorization.
