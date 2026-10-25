# Android kiosk setup

## Current build status

The native Kotlin/Jetpack Compose project produces **debug APKs** (fine for LAN-only use; no Play Store signing). It is a working French/Arabic attendance kiosk:

- **Pairing:** on first launch, enter the server address and the pairing code (`KIOSK_PAIRING_CODE` in `apps/api/.env`). The server returns a device token; the phone stores it and every kiosk request is authenticated with it. Paired phones are listed in the dashboard and can be revoked there.
- **Clocking:** an employee types their employee code and PIN, sees their current status, and taps Arrivée / Début de pause / Fin de pause / Départ. The server stamps the time (Africa/Tunis) and enforces valid transitions; retries after a network error cannot record the same tap twice.
- **Lockouts:** 5 wrong PINs lock that employee code for 5 minutes; 5 wrong pairing codes lock the client for 15 minutes.

No camera is needed. The optional face-camera diagnostic (setup screen) processes frames in memory with YuNet and reports only a face count; it does not store frames or identify employees.

## Build on Windows

Prerequisites:

- JDK 17
- Android SDK Platform 34 and Build Tools 34.0.0
- Android SDK license acceptance

From the repository root:

```powershell
cd apps/android-kiosk
.\gradlew.bat testDebugUnitTest assembleDebug
```

The OpenCV model-load smoke test can be run on a connected emulator/device with `.\gradlew.bat connectedDebugAndroidTest`.

ABI-split APKs are written to:

```text
apps/android-kiosk/app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
apps/android-kiosk/app/build/outputs/apk/debug/app-x86_64-debug.apk
```

Use ARM64 for the S22 and x86_64 for the Android emulator. The repository copy at `apps/android-kiosk/Cappuccino-Kiosk-debug.apk` is the ARM64 build.

The Gradle wrapper pins Gradle 8.7. No production signing key is included in the repository.

## Check the local API over Wi-Fi

1. Connect the computer and kiosk to the same trusted private Wi-Fi. Do not use a guest network with client isolation.
2. Reserve a stable LAN address for the computer in the router, then start the API with `uvicorn app.main:app --reload --host 0.0.0.0 --port 8000`.
3. Allow port 8000 through the computer firewall for private networks only.
4. On the kiosk, enter `http://<dashboard-computer-LAN-IPv4>:8000`, the pairing code and a device name, then tap **Jumeler** (or the Arabic equivalent). The pairing is stored on the device and survives restarts; **Configuration de la borne** re-pairs it.
5. In the dashboard, give each employee a 4–8 digit PIN (when adding them, or with **Définir le PIN**).

The app polls `GET /api/v1/health` to show the connection status. The dashboard computer's `127.0.0.1` address will not work from a phone. HTTP is allowed only for private IPv4 addresses in the app; use verified HTTPS and proper device credentials before operational use. The dashboard UI itself is served separately on port 5173.

The kiosk writes non-sensitive connection diagnostics to its private app storage (`kiosk-diagnostics.log`) and rotates one previous file. Tap **Copier les journaux** to copy the logs for support. These logs contain health-check outcomes and timings, not employee records, passwords, or biometric data.

## Install with ADB

1. On the S22, enable Developer options and USB debugging. This is for development installation only; disable debugging for the eventual managed production profile.
2. Connect the unlocked phone to the Windows workstation with a data-capable USB cable.
3. Accept the RSA debugging prompt on the phone.
4. Confirm ADB sees the device:

   ```powershell
   adb devices -l
   ```

   The device must show as `device`, not `unauthorized`.
5. Install the debug APK:

   ```powershell
   adb install -r apps\android-kiosk\app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
   ```

To uninstall the development build:

```powershell
adb uninstall com.cappuccino.kiosk
```

## Before operational attendance use

- Pair the kiosk to the API using a one-time, revocable device credential and verified TLS. This is not implemented yet.
- Implement employee-template enrollment/matching and validated liveness. Bundled YuNet/SFace currently power face-detection diagnostics only.
- A phone's built-in Android biometric prompt authenticates the device owner; it cannot identify different employees. Fingerprint attendance requires a compatible external reader and its Android SDK.
- Implement server-side event submission with unique idempotency IDs, acknowledgements, and a bounded encrypted outbox before enabling attendance actions.
- Validate the app on the physical S22, including camera conditions, connectivity loss, restart behavior, thermal behavior, and accessibility. Emulator testing is not a substitute.
- Provision Android Enterprise device-owner/Lock Task mode only through the documented administrator process. The current build does not lock the phone down.
- Create a separately protected production signing key and release APK only after the above acceptance checks.
