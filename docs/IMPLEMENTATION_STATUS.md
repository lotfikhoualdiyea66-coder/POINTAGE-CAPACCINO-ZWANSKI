# Implementation status

## Phase 0: complete
- Workspace audited.
- Project structure created.
- Architecture and decisions documented.
- Branding asset preserved.

## Phase 1: in progress
- Root repository scaffold installed.
- Web production build and backend tests pass locally.

## Phase 2+: planned and partially implemented
- Backend schema, models, and attendance engine are under development.
- Frontend dashboard scaffold is included.
- Android debug APK builds with Gradle 8.7, JDK 17, and Android API 34.
- The APK currently displays a bilingual safe-setup screen; it does not capture faces or record attendance.
- Hardware-bound biometric provisioning and S22/Ubuntu deployment remain pending explicit device access and authorization.

## Remaining blockers
- No Android device is currently connected to ADB; installation and physical S22 testing remain pending.
- The APK is a debug build, not a production-signed release.
- Secure server pairing, CameraX capture, licensed recognition/liveness, and Lock Task provisioning are not implemented.
- Production biometric model licensing must be manually reviewed before enabling live recognition.
- Full Ubuntu systemd deployment requires the target machine and operating-system privileges.

The project remains usable for local development, synthetic tests, and documentation of the real production setup path.
