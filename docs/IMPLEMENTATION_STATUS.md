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
- The APK pairs with the API, lets employees clock in/out and take breaks with their code + PIN, shows server status, stores bounded non-sensitive diagnostics, and has an optional on-device YuNet face-count screen.
- Hardware-bound biometric provisioning and S22/Ubuntu deployment remain pending explicit device access and authorization.

## Remaining blockers
- Emulator model tests pass; physical S22 camera, thermal, and accessibility testing remains pending.
- APKs are debug-signed ABI splits, not production-signed releases.
- Kiosk pairing (shared pairing code → per-device token, revocable from the dashboard) and employee code + PIN clock-in/out are implemented. The in-memory lockout counters reset when the API restarts.
- Face check on top of the PIN is implemented (admin-authorized enrollment on the kiosk, encrypted templates, server-side matching, head-turn liveness) and off by default. It still needs real-world tuning on the S22 (lighting, threshold) and an INPDP/consent review before use with real staff. Fingerprint attendance requires a compatible external USB reader and its Android SDK.
- This workspace still uses local SQLite; Supabase Postgres requires a private database URI and applied migrations.
- Full Ubuntu systemd deployment requires the target machine and operating-system privileges.

The project remains usable for local development, synthetic tests, and documentation of the real production setup path.
