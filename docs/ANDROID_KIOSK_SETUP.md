# Android kiosk setup

## Current build status

The native Kotlin/Jetpack Compose project currently produces a **debug APK**. It opens a French/Arabic safe-setup screen, shows the Africa/Tunis date and time, and keeps the display awake. It explicitly reports that the server is not paired and facial recognition is disabled. It does not capture camera frames, identify employees, or submit attendance events.

This APK is suitable for checking the native app launch and bilingual presentation on the S22. It is not an operational attendance kiosk or a release-signed production app.

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

The APK is written to:

```text
apps/android-kiosk/app/build/outputs/apk/debug/app-debug.apk
```

The Gradle wrapper pins Gradle 8.7. No production signing key is included in the repository.

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
   adb install -r apps\android-kiosk\app\build\outputs\apk\debug\app-debug.apk
   ```

To uninstall the development build:

```powershell
adb uninstall com.cappuccino.kiosk
```

## Before operational attendance use

- Pair the kiosk to the Ubuntu server using a one-time, revocable device credential and verified TLS. Neither is implemented in the current APK.
- Implement and test CameraX capture, liveness, and the licensed local recognition provider. Do not enable face collection until licensing, privacy, and organizational authorization are recorded.
- Implement server-side event submission with unique idempotency IDs, acknowledgements, and a bounded encrypted outbox before enabling attendance actions.
- Validate the app on the physical S22, including camera conditions, connectivity loss, restart behavior, thermal behavior, and accessibility.
- Provision Android Enterprise device-owner/Lock Task mode only through the documented administrator process. The current build does not lock the phone down.
- Create a separately protected production signing key and release APK only after the above acceptance checks.
