# Implementation status

## Phase 0: complete
- Workspace audited.
- Project structure created.
- Architecture and decisions documented.
- Branding asset preserved.

## Phase 1: in progress
- Root repository scaffold installed.
- Admin dashboard login and employee registration are implemented; management API routes require an administrator token.
- Admins can define shift templates and assign multiple shifts; the dashboard reports worked time, breaks, target/shortfall, double shifts, and month-to-date days worked.
- Worked time sums actual clocked intervals and subtracts recorded breaks; open shifts can be calculated live.
- Web production build and backend tests pass locally.

## Phase 2+: planned and partially implemented
- Backend schema, models, and attendance engine are under development.
- Frontend dashboard scaffold is included.
- Android debug APK builds with Gradle 8.7, JDK 17, and Android API 34.
- The APK remembers its LAN API URL, retries health checks every 30 seconds, stores bounded non-sensitive diagnostics, and has an optional on-device YuNet face-count screen; it does not identify employees or submit attendance.
- Hardware-bound biometric provisioning and S22/Ubuntu deployment remain pending explicit device access and authorization.

## Remaining blockers
- Emulator model tests pass; physical S22 camera, thermal, and accessibility testing remains pending.
- APKs are debug-signed ABI splits, not production-signed releases.
- Authenticated kiosk pairing and kiosk event submission are not implemented; attendance events can currently only be submitted through the admin API.
- Face matching/enrollment and liveness are not implemented. Fingerprint attendance requires a compatible external reader and Android SDK.
- This workspace still uses local SQLite; Supabase Postgres requires a private database URI and applied migrations.
- Full Ubuntu systemd deployment requires the target machine and operating-system privileges.

The project remains usable for local development, synthetic tests, and documentation of the real production setup path.
