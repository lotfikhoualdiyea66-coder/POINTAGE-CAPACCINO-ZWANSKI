# MASTER BUILD PROMPT — CAPPUCCINO LAC 1 ATTENDANCE SYSTEM

> **Project owner:** Cappuccino Lac 1  
> **Product / implementation brand:** Zwanski Vulzero  
> **Project type:** Private, LAN-only, offline-first employee attendance and shift-management system  
> **Primary devices:** Samsung Galaxy S22 attendance kiosk, Ubuntu local server, Windows 11 manager workstation  
> **Primary interface language:** French  
> **Additional interface language:** Arabic, including right-to-left layout  
> **Status:** Complete the product end to end; do not stop at a plan, prototype, mockup, or generated code that has not been tested.

---

## 0. YOUR ROLE AND NON-NEGOTIABLE WORKING RULES

Act as the senior product engineer, software architect, Android engineer, backend engineer, frontend engineer, database engineer, QA engineer, DevOps engineer, and security reviewer responsible for delivering this application.

Your assignment is to inspect the current workspace, make reasonable engineering decisions, implement the system in coherent phases, run the available tests, repair failures, and leave behind a working, documented, maintainable project.

### Do not ask the owner questions

The owner has explicitly asked you to think independently and take the project to completion without asking a series of questions. Do not stop to ask about ordinary implementation choices, styling, naming, folder structure, libraries, or defaults. Inspect the environment and existing files. Where a choice has not been specified, use the safest practical default, record the decision in `docs/DECISIONS.md`, and continue.

If a required environment detail is unavailable, build a configurable solution with a safe default, provide detection/setup instructions, and continue. Do not fabricate credentials, hardware capabilities, legal approval, biometric-model licenses, test results, or production deployment success.

The only things you must **not** do autonomously are destructive or legally consequential live actions: do not factory-reset the S22, enroll real employees, collect real biometric data, change router/firewall policy outside the application's explicit setup, replace system configurations destructively, or enable live biometric production mode without the relevant operator confirmation and documented licensing/privacy preconditions. Build the software, test it with synthetic fixtures, and provide exact opt-in instructions for those steps. Never block the rest of the project because such an action requires human authorization.

### Implementation rules

1. Inspect the repository and machine before changing anything. Preserve useful existing work; do not overwrite or delete unrelated files.
2. Write the application, not only a specification. Finish each phase, run its checks, fix issues, and then proceed.
3. No vibe coding: use typed interfaces, clear module boundaries, database migrations, error handling, input validation, accessibility, tests, explicit business rules, and secure defaults.
4. Never claim a test passed unless you ran it and saw it pass. Document unrun tests and why they could not run.
5. Never leave a visible primary button inert, use placeholder screens in place of core functionality, or use fake attendance events in production mode.
6. Demo/test mode must be clearly labelled and must never be confused with production. Synthetic people and attendance data must be recognizable as test data.
7. Do not add analytics, telemetry, advertisements, cloud APIs, external font dependencies, remote scripts, hosted databases, or third-party SaaS. All normal application functions must remain inside the restaurant's local network.
8. Ollama is optional. No core attendance function may depend on Ollama, an LLM, or internet access.
9. Use current stable, actively maintained dependencies available at implementation time. Pin versions, record licenses, and choose compatible versions instead of blindly copying outdated version numbers.
10. Provide straightforward local setup and recovery. The application must not require an IDE to run after installation.
11. Do not hard-code passwords, signing keys, secrets, IP addresses, tokens, or production data into source control.
12. Keep a project `AGENTS.md` (or equivalent contributor instructions) consistent with this prompt and update it if the repository has an existing agent-guidance file.
13. Continue through every phase. If a feature is blocked by device access, a paid license, or a human authorization gate, implement its interfaces, tests, disabled-safe state, and documented activation procedure, then proceed with the rest.

---

## 1. PRODUCT GOAL

Build a professional attendance and shift-management application for one restaurant: **Cappuccino Lac 1**. It is for internal use only. It is not a public SaaS product and must not require cloud hosting.

The Samsung Galaxy S22 will be mounted at the staff entrance and act as a dedicated attendance kiosk. Staff should be able to identify themselves through live facial recognition and record attendance actions. The Ubuntu machine will host the authoritative API and PostgreSQL database as background services. The Windows 11 office computer will use a browser-based manager dashboard on the same private local network. When an employee successfully records an event, the dashboard should update in real time and show a visible notification.

The main business outcome is a clear, auditable record that protects both employees and management: the same published rules must be applied to everyone, no attendance entry may be silently erased, corrections must be traceable, and each employee must be able to review their monthly record.

### Success means

- A manager can set up the local system, create employees, configure shifts, and operate the dashboard.
- The S22 can run a dedicated kiosk app and communicate securely with the Ubuntu server.
- The kiosk can capture an event, provide understandable feedback, and submit it exactly once.
- Confirmed events appear on the manager dashboard in real time.
- Temporary network failure is visible and handled safely; events are never falsely presented as server-confirmed when they are not.
- Monthly time calculations are reproducible, explainable, and covered by tests.
- Database backups can be restored through documented procedures.
- No employee photos, facial templates, attendance records, logs, backups, or AI prompts are sent to a cloud service.
- A fresh developer or administrator can follow the documentation to build, install, validate, back up, restore, and troubleshoot the system.

---

## 2. DEVICE AND OPERATING ASSUMPTIONS

Use these defaults unless inspection finds a better compatible arrangement:

- **Kiosk:** Samsung Galaxy S22, native Android application, Kotlin, Jetpack Compose, CameraX, Android Keystore for device-local secrets/queued records, Room for the small local event outbox if needed.
- **Server:** Ubuntu Linux on the local office machine. The backend and PostgreSQL run in the background and restart automatically after reboot or failure.
- **Dashboard:** Windows 11 browser connected over the restaurant's private LAN. A standards-compliant browser is the expected access method; do not require a Windows-native desktop installer.
- **Backend:** Python using FastAPI, Pydantic, SQLAlchemy 2.x and Alembic, with a supported stable Python runtime. Use REST for normal operations and WebSockets (or another robust local real-time transport) for live dashboard notifications.
- **Database:** PostgreSQL using a currently supported stable major version. Prefer PostgreSQL for durable central data. SQLite may be used only for isolated automated tests or an explicitly documented small local device outbox—not as a second authoritative server database.
- **Dashboard frontend:** TypeScript + React + Vite, with an actively maintained component/accessibility strategy. Use responsive layouts and typed API contracts. Choose a restrained styling system; avoid adding a sprawling UI framework without need.
- **Packaging/deployment:** Docker Compose is acceptable for the API, database, reverse proxy, and optional services when suitable for the target Ubuntu environment. Also supply a simple installation/update/start/stop/status/backup/restore workflow. Persistent data must live in explicitly documented volumes/host directories.
- **AI:** Optional Ollama service on the same local machine, accessed through `http://127.0.0.1` or another loopback-only binding. Do not expose the Ollama API to the wider LAN by default.

Keep the application modular. The server should be the source of truth for employee records, shift rules, accepted attendance events, approvals, corrections, and reports.

### Network assumptions

- The S22, Ubuntu server, and Windows 11 computer are on the same trusted private LAN/Wi-Fi network.
- The server should use a stable address, preferably a DHCP reservation or documented static address. Do not invent the address; discover it during setup and make it configurable.
- The service must not be exposed through router port forwarding or publicly reachable internet addresses.
- Provide authenticated, encrypted connections between kiosk and server. Use a local certificate authority/certificate pinning or another secure, documented local TLS provisioning solution. Do not silently fall back to transmitting passwords, tokens, biometric templates, or camera frames over clear-text HTTP in a production configuration.
- The Windows browser's trust setup and Android certificate provisioning/pinning must be documented clearly.
- Allow a development-only local mode, with obvious warnings, only if needed for first-run tests. Do not silently carry insecure development settings into production.

---

## 3. BRANDING AND UX DIRECTION

Use the **provided Cappuccino Lac logo** exactly as supplied. Search the repository, workspace, and provided user attachments for the logo. Preserve the original artwork and proportions. Do not redraw, substitute, distort, or generate a different logo. If the logo is not available as a local file, build the application with a deliberate logo asset slot and document precisely where the owner should place it; never use a fake replacement.

Suggested asset path: `apps/web/public/branding/cappuccino-logo.png` (or a clearly documented equivalent). Keep a copy of the source asset in the project assets if permitted. Check its dimensions and background before use. Ensure it remains legible on both dark and light surfaces.

### Brand personality

Use a polished, discreet, premium restaurant-administration style:

- Near-black/charcoal background accents.
- Warm gold/champagne accents inspired by the logo.
- Warm ivory/light surfaces for readable tables and forms.
- Strong contrast and clear status colors that do not depend on color alone.
- Generous spacing, consistent alignment, clean typography, subtle borders, and no unnecessary animations.
- Avoid generic template appearance, excessive gradients, excessive glass effects, decorative cards everywhere, or oversized marketing sections.

Color samples may begin near `#10100F`, `#D6B57A`, and `#F6EBD7`, but check contrast and adjust as needed. Brand colors must not override accessibility requirements.

### Languages and accessibility

- French is the default language throughout the kiosk and dashboard.
- Provide Arabic translation for all important kiosk actions, errors, status messages, and core dashboard elements where practical.
- Support true RTL layout for Arabic; do not merely right-align French-style layouts.
- Provide an in-dashboard language selector; persist per device/user where appropriate.
- Use simple staff-facing French and Arabic labels, large touch targets, large status text, clear icons plus text, and restrained reading requirements.
- Minimum touch target should generally be 48dp on Android and an equivalent comfortable target on the web.
- Support keyboard navigation, visible focus, semantic headings/labels, screen-reader labels, sufficient contrast, and reduced-motion preferences.
- Do not rely exclusively on sound, color, or animation for a success/error state.
- Kiosk should recover to the idle face-capture screen after a short configurable timeout, while ensuring the just-completed person's personal information is not left visible.

### Suggested kiosk labels

French:
- `Présentez votre visage`
- `Reconnaissance en cours…`
- `Identité confirmée`
- `Choisissez une action`
- `Arrivée`
- `Départ`
- `Début de pause`
- `Fin de pause`
- `Pointage enregistré`
- `Connexion au serveur interrompue`
- `Veuillez réessayer ou contacter le responsable`

Arabic equivalents must be carefully reviewed for clear Tunisian workplace usage. Keep translation strings in resource/i18n files, not hard-coded inside view code.

---

## 4. CRITICAL BIOMETRIC, PRIVACY, AND MODEL-LICENSING RULES

Facial recognition is a sensitive subsystem. Build it behind a clear provider interface so the app can be tested without collecting real faces and can change models without rewriting the attendance system.

### Data minimization

- Do not retain original face photos or camera frames after immediate processing by default.
- Process frames in memory and discard them after the recognition/enrollment operation, except for short-lived synthetic test fixtures or a specifically authorized diagnostic capture that is disabled by default and controlled by an administrator.
- Do not print or log image pixels, image byte data, facial embeddings, secrets, authentication tokens, or raw biometric input.
- Store only what is necessary for recognition, using authenticated encryption at rest with a key-management strategy documented in `docs/SECURITY.md`. Do not commit encryption keys to source control or store keys beside encrypted templates without protection.
- If templates are used, treat them as sensitive biometric data even though they are not raw photos. Limit access, retention, export, and backup access.
- Provide authorized biometric-template deletion on employee deactivation/removal. Record the administrative action without copying the deleted biometric content into the audit log.
- Use minimal employee data: employee number, name, function, active/inactive status, employment-relevant dates if needed, and assigned schedule. Do not collect national ID numbers, home addresses, personal phone numbers, or salary data unless a concrete necessary feature requires them.
- Never allow a face recognition score or model output to decide discipline, salary deduction, or dismissal. The software records events and surfaces uncertainty; a responsible manager reviews exceptions.

### Model usage must be legally and commercially valid

The source-code license of a model library does not automatically mean its pretrained weights can be used commercially. Before using or redistributing any face detection, face embedding/recognition, or anti-spoofing model, inspect its exact license and source. Record the model name, version, source URL, checksum, license, usage restrictions, and whether commercial internal workplace use is explicitly permitted in `docs/MODEL_CATALOG.md`.

**Do not silently download or package model weights whose commercial-use rights are uncertain or research-only.** Do not treat InsightFace/ArcFace model weights as automatically commercially licensed merely because code is open source. Avoid sending biometric data to hosted APIs. The model must run on the S22 and/or the Ubuntu local server only.

If an eligible, adequately documented model cannot be selected without a paid license or manual license confirmation:

1. Implement a well-defined `FaceRecognitionProvider` / `FaceEngine` interface and provider configuration.
2. Implement all non-biometric flows, enrollment screens, server APIs, audit handling, synthetic tests, and a clearly marked test/demonstration provider.
3. Make live biometric enrollment and production recognition unavailable by default until a properly licensed provider is configured.
4. Show an explicit administrator configuration status instead of pretending biometric identification works.
5. Document the exact approved model artifact format and setup steps. Continue with every other phase.

Do not create a homemade face-matching algorithm from facial landmarks or weak heuristics and present it as secure identity verification.

### Recognition behavior

- **Default biometric architecture:** perform face detection, embedding generation, matching, and liveness/presentation-attack analysis on the Ubuntu server using a properly licensed local provider. The S22 streams only the minimum required live camera frames to the Ubuntu service over authenticated TLS on the private LAN. Process frames in memory and discard them immediately after the recognition request; never write frames to disk, include them in logs, or send them outside the LAN. Keep biometric templates centrally on the Ubuntu server, not replicated to the S22. This is the default because the owner wants the local server to remain the authoritative place for data.
- If the server is unreachable, live face identification is unavailable; show the offline state and use the defined authorized manager-assisted fallback. Do not silently switch to cloud recognition or claim offline facial recognition works. A different on-device design may be chosen only if it has a concrete security/privacy advantage and is documented in `docs/DECISIONS.md`; central-server recognition remains the default.
- Use live camera input with documented face-quality checks and an appropriate presentation-attack/liveness strategy.
- Do not claim liveness detection is perfect. Document expected failure modes and evaluate photo/video replay attempts during testing.
- Require a single clear face during attendance and enrollment, with usable quality and lighting.
- Use conservative matching thresholds and a second-best-match margin. Thresholds must be configurable only by authorized administrators and validated on a representative opt-in test set before live use.
- If confidence is low, multiple candidates are too close, liveness fails, or no match exists, **fail closed**: do not record attendance as a confirmed biometric match. Offer a documented fallback such as an employee code/PIN or manager-assisted entry, visibly label its method, and log the fallback event.
- Do not reveal employee names or matched candidates before verification has reached the required confidence threshold.
- Enrollment must require an authorized manager session, a unique employee record, multiple good-quality live captures, a confirmation step, and the applicable legal/privacy readiness acknowledgement.
- An enrolled person must have a tested way to request correction/deletion and a non-biometric fallback where required or operationally necessary.

### Legal and organizational readiness

Add a `Privacy & Biometric Readiness` page to the manager dashboard and a checklist in `docs/PRIVACY_AND_BIOMETRICS.md`. It must identify items that require the restaurant's own confirmation, including applicable Tunisian INPDP procedures/authorizations, appropriate employee notice and lawful basis, access controls, retention rules, deletion procedure, a non-biometric fallback, and incident response. Do not claim legal compliance has been achieved merely because the checklist exists.

Use synthetic identities for development and automated testing. Do not enroll real staff in this build session.

---

## 5. SYSTEM ARCHITECTURE

Build a clear repository structure. A reasonable structure is:

```text
/
  AGENTS.md
  README.md
  CHANGELOG.md
  docs/
    ARCHITECTURE.md
    DECISIONS.md
    SECURITY.md
    PRIVACY_AND_BIOMETRICS.md
    MODEL_CATALOG.md
    ATTENDANCE_RULES.md
    INSTALL_UBUNTU.md
    INSTALL_WINDOWS_DASHBOARD.md
    ANDROID_KIOSK_SETUP.md
    OPERATIONS.md
    BACKUP_AND_RESTORE.md
    TROUBLESHOOTING.md
    TEST_PLAN.md
  apps/
    api/                 # FastAPI backend
    web/                 # React/TypeScript manager dashboard
    android-kiosk/       # Kotlin Android application
  infra/
    compose.yaml
    reverse-proxy/
    scripts/
  tests/
    integration/
    e2e/
    fixtures/
```

Adjust this structure where the existing repository or build tooling makes a better arrangement sensible, but keep server, web dashboard, Android app, infrastructure, documentation, and tests organized and discoverable.

### Backend responsibilities

- Authentication, sessions, permissions, device registration, and health endpoints.
- Employee and function management.
- Shift templates, assigned schedules, break policy, and exceptions.
- Enrollment lifecycle and the chosen face-engine provider interface.
- Attendance event validation, deduplication, audit trail, offline/pending event sync, and server acknowledgements.
- Real-time dashboard event publication.
- Time calculations and report generation.
- Configuration, data export, backup status, system health, and readiness checklist.
- Optional local Ollama integration behind a separate service boundary.

### Frontend responsibilities

- Manager dashboard and authenticated settings.
- Real-time arrival/departure notifications.
- Employee management, schedule management, attendance review, correction workflow, reports, and privacy readiness.
- French/Arabic localization and accessible RTL.
- Visible server-connection and kiosk-connection health.

### Android responsibilities

- Full-screen kiosk UI and live camera capture.
- Secure pairing with the specific local server/device record.
- Attendance action selection and clear results.
- Reliable retries with unique request IDs and an encrypted local pending-event outbox.
- Device health/status page available only through the protected admin workflow.
- Kiosk startup/recovery and managed Lock Task Mode integration.
- No ordinary employee access to Android settings, browser, other apps, enrollment, or management functions when correctly provisioned as a dedicated device.

### No split-brain

Only the Ubuntu server database is authoritative. The kiosk may hold only the smallest encrypted pending-event queue and configuration required for temporary disconnection; it must not become a second administrative database. Define how queued events are reconciled and acknowledged. Avoid duplicating editable attendance data across independent clients.

---

## 6. EMPLOYEES AND ORGANIZATION

Build employee management with at least these fields:

- Server-generated immutable employee UUID.
- Human-readable employee number/code, unique and configurable format.
- Full name.
- Job function (editable lookup table, not a source-code enum).
- Active/inactive status.
- Start date and optional end date when operationally required.
- Assigned schedule or shift group.
- Biometric enrollment state: not enrolled, enrolled, pending deletion, deleted, or unavailable because the provider is not configured.
- Created/updated timestamps and responsible administrative user IDs.

Functions must be configurable from the dashboard. Initial demo seeds may include generic restaurant roles such as `Responsable`, `Chef de rang`, `Cuisinier`, `Plonge`, and `Shisha`, but do not create real staff records from prior chat messages. Clearly mark all seeded records `DEMO` and make demo reset safe and explicit.

Features:

- Create, edit, activate/deactivate, and view employee history.
- Search/filter by name, code, function, and status.
- CSV import with preview, validation, duplicate detection, and a rollback/cancel option.
- CSV export restricted by permissions and recorded in the audit trail.
- Deactivation must block future normal attendance recognition but preserve required historical attendance records.
- Employee deletion must distinguish personal/biometric data erasure from retention of legally required attendance records. Implement a controlled data-retention strategy instead of cascading away important timesheet history without warning.
- Never permit deletion or editing to silently erase past events.

---

## 7. SHIFT SCHEDULING

Implement configurable shift templates and employee/date assignments. Do not hard-code the restaurant's actual operating hours or assume which days employees work.

Support:

- One or more shifts per day, split shifts, and shifts crossing midnight.
- Planned start/end, unpaid/paid break rules where configurable, grace periods, and overtime-review thresholds.
- Days off, leave/absence markers, public holiday tags, and exceptional schedule changes.
- Individual and group schedules.
- Schedule changes after publication must be audited and must not rewrite the actual attendance events.
- Clearly distinguish scheduled hours from actual worked hours.
- Show mismatches, missed punches, early departures, late arrivals, unscheduled events, and overnight shifts without automatically deciding fault.
- Use actual event timestamps and explicit rules for time calculations. Avoid hidden rounding. The default should retain minute-level event precision; any rounding or tolerance policy must be configurable, documented, and auditable.

Store timestamps safely as UTC instants in the database, while applying the `Africa/Tunis` timezone for display, work-date boundaries, shift interpretation, and reports. Test daylight-saving/timezone boundary behavior even if local transition rules rarely affect the configured operating period.

---

## 8. ATTENDANCE STATE MACHINE AND EVENT RULES

Implement attendance as an explicit state machine, not a collection of ad hoc button handlers.

### Supported event types

- `CLOCK_IN` / French `Arrivée`
- `CLOCK_OUT` / French `Départ`
- `BREAK_START` / French `Début de pause`
- `BREAK_END` / French `Fin de pause`
- `MANAGER_CORRECTION` or a structured correction record linked to an original event
- `MANUAL_FALLBACK` with explicit method/reason metadata when an authorized fallback is used

The exact set of supported event types may include additional clearly named types if needed, but don't blur event meanings.

### Normal transitions

- Not currently clocked in → `CLOCK_IN`.
- Clocked in and not on break → `BREAK_START` or `CLOCK_OUT`.
- On break → `BREAK_END`.
- Clocked in after a completed break → another allowed break or `CLOCK_OUT`.
- A new shift after clock-out may create a new `CLOCK_IN` according to assigned schedule and the configured double-shift rules.

Use a configurable, explicit policy for overnight shifts, repeated shifts, early/late attendance, and emergency exceptions. Reject impossible transitions with a human-readable explanation and do not silently transform them into a different action. Give managers an exception workflow rather than bypassing validation.

### Kiosk interaction

1. Idle face-capture page shows Cappuccino branding, French/Arabic option, clear face guide, and current local date/time.
2. Capture a live face and run the configured recognition/liveness workflow.
3. When an identity is confidently confirmed, show a privacy-minimized confirmation (for example, first name/short name only where appropriate) and ask the employee to choose `Arrivée`, `Départ`, `Début de pause`, or `Fin de pause` from currently valid actions. The app may suggest the most likely action based on state, but must make the actual recorded action clear before submission.
4. Validate the event state against the server's current authoritative state. If an event is invalid, explain what action is expected and offer a safe retry or manager fallback.
5. Submit using a unique idempotency key/event UUID; retries must not produce duplicate punches.
6. Show success only when the authoritative server accepts the event, unless clearly showing a pending offline event.
7. Return to idle after a short configurable delay, clearing any employee display state.

Never submit multiple actions from a single recognition. Do not show a list of other employees or let a user choose a different identity when biometric matching fails.

### Event record fields

Each accepted/pending event should support:

- Immutable event UUID and idempotency key.
- Employee UUID.
- Event type.
- Client captured timestamp and server received/accepted timestamp.
- Device/terminal UUID.
- Timezone context and source/method (`face`, `employee_code_pin`, `manager_entry`, etc.) without exposing biometric values.
- Recognition outcome metadata limited to what is necessary; do not log or export facial embeddings or raw images.
- Network/offline state and sync state.
- Validation result and relevant shift association.
- Creation metadata and integrity/audit references.

The backend's accepted server timestamp is authoritative for online events. If an event was created offline, preserve the original device-recorded timestamp and later synchronization timestamp, mark the record as delayed/offline, detect clock skew, and send it for review if it conflicts with an existing event. Never disguise the difference between recorded time and synchronization time.

### Idempotency and concurrency

- Duplicate requests with the same event UUID/idempotency key must return the prior result and must not create a second event.
- Use database transaction boundaries and constraints to prevent racing kiosk submissions from creating impossible sequences.
- Two simultaneous requests for the same employee must be serialized/validated safely.
- Use explicit unique constraints, not only frontend button disabling, for duplicate-prevention guarantees.

---

## 9. AUDITABILITY, CORRECTIONS, AND FAIRNESS

This is a core requirement, not an optional feature.

- Raw attendance events are append-only from the user interface. No normal role may silently update/delete a confirmed event.
- A correction creates a separate record linked to the original and records old/new interpreted values, the reason, acting user, approval if required, and timestamps.
- Keep the distinction between the original recorded event and the corrected interpretation.
- Configure roles such as `Administrator`, `Manager`, and `Read-only/Auditor` with least privilege. The kiosk is a device role, not an administrator.
- Require a reason for correcting records, approving offline/conflicting events, editing schedules after publication, re-opening a finalized month, exporting data, and deleting biometric templates.
- For high-impact changes such as changing a punch or reopening a finalized timesheet, support optional two-person approval through configuration; if implemented, it must work end to end.
- Keep an append-only application audit log for security-sensitive actions, with tamper-evident chaining or an equivalent integrity control and verification utility. Explain the limitations: a privileged machine administrator can still attack the host, so protected backups are necessary.
- Never log credentials, session cookies, tokens, passwords, raw images, face embeddings, or secrets.
- Dashboard must identify edited/corrected records and show a correction history.
- Each employee's monthly statement should be exportable/printable for review; where employee self-login is not implemented, the manager must still be able to print or provide an individual statement securely.
- Give the employee an explicit dispute/acknowledgment state if feasible. Do not interpret “not acknowledged” as acceptance or waive applicable rights.

---

## 10. MANAGER DASHBOARD

Build a functional, responsive, professionally branded dashboard. The primary user is the restaurant manager working on Windows 11, but it should remain usable on a tablet.

### Overview page

Show at a glance:

- Employees currently clocked in.
- Latest arrivals and departures.
- Current breaks.
- Employees expected by schedule but not yet recorded, with configurable logic.
- Late/early/missed-punch exceptions, clearly labelled as items for review, not final judgments.
- Offline/unsynced events and device/server connection health.
- Today's schedule and attendance summary.
- Database backup status and the last successful backup.

### Live notifications

- Publish event updates over authenticated WebSockets or an equivalent robust real-time channel.
- When a clock event is accepted, update the dashboard immediately without manual refresh.
- Display employee name, function, action, time, and status in a compact notification.
- Optional sound must be user-configurable, initially conservative, and accompanied by visible text.
- Support reconnect with automatic resubscription and a fresh authoritative data snapshot. Never assume the client received an event just because the server broadcast it.
- Prevent duplicated notifications when reconnecting or replaying events.
- Show a clear banner if real-time connection is unavailable; ordinary refresh/API access should still provide current records.

### Pages

At minimum implement:

1. Login/logout and session-expiry handling.
2. Overview/live attendance.
3. Employees and functions.
4. Enrollment state and protected enrollment workflow.
5. Today's attendance/events.
6. Shift templates and schedule calendar/list.
7. Exceptions and missing punches.
8. Corrections and approval history.
9. Monthly timesheets and report export.
10. Devices/kiosk health and pairing status.
11. User accounts, roles, and security settings.
12. Privacy/biometric readiness.
13. System status, backups, logs/status summaries, and app version.
14. Language and interface settings.

A page isn't considered implemented merely because its route exists; it must be connected to the backend and functional.

### Reports and exports

Support date ranges, employee/function filters, print-friendly reports, CSV export, and PDF generation or print-to-PDF. Reports should include:

- Scheduled shifts and planned time.
- Accepted recorded events.
- Computed actual hours and break deductions using transparent configured rules.
- Overtime candidates and approvals separately.
- Late/early/missed-punch markers.
- Corrections and exception notes.
- Pending/disputed/conflicting records.
- Report generation time, timezone, calculation policy/version, and finalized status.

Do not calculate actual wages or make legal payroll determinations without a separately approved and well-specified payroll module. The product is first an attendance/time-record system.

### Month finalization

Provide a review workflow for month-end: show outstanding exceptions, require authorized review, generate a snapshot of the timesheet and calculation-policy version, and mark the period finalized. Reopening requires permission, a reason, and an audit entry. Re-finalization must preserve prior finalized snapshots.

---

## 11. ANDROID S22 KIOSK AND LOCKDOWN

Implement a native Kotlin Android application using a single-activity architecture and Jetpack Compose. Use current stable Android tooling that builds on the installed environment. Use CameraX or another maintained camera API. Make target SDK and supported API range explicit. Inspect the actual S22/Android version if accessible but do not assume device access is available.

### Kiosk reliability

- Full-screen portrait layout by default; allow rotation only if product-tested and deliberately configured.
- Designed for wall mounting at the staff entrance and continuous charging.
- Keep screen on during use with sensible thermal/battery safeguards.
- Return to the idle screen after each action; no employee details remain visible for long.
- Handle camera permission errors, camera unavailable, low light, multiple faces, server loss, invalid certificate, recognition timeout, app crash/restart, and backend maintenance with human-readable messages.
- Retry transient network failures with bounded exponential backoff and jitter.
- Keep an encrypted, bounded pending-event outbox with unique event UUIDs. Do not accumulate duplicate events or silently discard them. Provide clear capacity/health handling.
- Report app version, device ID, server connectivity, outbox count, and last successful sync to the authorized dashboard without sharing unnecessary personal/device data.
- Provide a locally configurable kiosk label (for example, `Cappuccino Lac 1 — Entrée du personnel`).

### Kiosk exit prevention

Implement a dedicated-device mode based on Android Enterprise managed-device capabilities and Lock Task Mode. Do not confuse screen pinning with secure kiosk mode.

- Provide a DevicePolicyManager/Device Admin provisioning path appropriate to a fully managed dedicated device, along with an explicit setup guide.
- When properly provisioned, allowlist the kiosk app in Lock Task Mode, set the kiosk/home behavior where supported, and start kiosk mode automatically.
- Support boot/startup recovery, crash recovery, and returning to the kiosk after legitimate app restarts.
- Hide normal app navigation and settings access during kiosk operation as supported by Android's managed-device APIs.
- Provide a protected, deliberate administrator exit flow requiring an authorized manager action and explicit device-management exit policy. Do not place an obvious employee-accessible exit button on the idle screen.
- Implement safe setup states: if the device is not provisioned/managed, label it clearly as `Kiosk restrictions not enabled` and explain the remaining limitations. Do not falsely tell the owner that the phone cannot be exited.
- Disable debugging for the documented production profile and explain physical-security limitations. No app can guarantee protection from someone who has physical control and can power off, factory-reset, or reflash a device.
- **Never trigger a factory reset automatically.** Factory reset may be needed for device-owner provisioning. Provide a separate checklist, data-backup warning, and step-by-step instructions for the owner to perform this manually only after backing up the device and accepting the consequences.
- Add a separate development/test mode that is not locked down. Never lock the developer's daily-use phone by default.

If fully managed Lock Task Mode is not configured, provide a limited test option such as screen pinning only if useful, clearly marked as not sufficient for production. Ensure the application remains recoverable by an authorized technician using documented methods.

### Attendance device pairing

- The server should register a kiosk device and issue a revocable device credential during a one-time pairing flow.
- Pairing must verify the server identity, ideally with a short-lived pairing code/QR plus pinned local TLS certificate or another secure bootstrap mechanism.
- Do not embed a reusable global API secret in the APK.
- Administrator can revoke a lost/decommissioned kiosk. The server must reject the revoked device.
- If pairing expires or certificates change, fail safely and provide a manager/technician recovery procedure.

---

## 12. OFFLINE AND NETWORK-FAILURE BEHAVIOR

Treat local network failure as a first-class state.

- Online accepted attendance events are written to PostgreSQL before the kiosk displays a confirmed success.
- Use a durable server acknowledgement so the kiosk knows whether an event was committed.
- If the network breaks before the server accepts an event, never claim the event has been confirmed by the server.
- Store only the minimum event payload needed for retry in an encrypted local outbox and clearly display `En attente de synchronisation` to the employee when policy allows queued punch recording.
- Because one-to-many face identification may depend on the local server, do not claim face recognition works during server outage unless a separately approved and secured on-device recognition provider is actually implemented. Default outage fallback: show a clear offline state and offer an authorized manager-assisted/manual emergency procedure. Do not cache all employee face templates on the kiosk unless a separately documented privacy/security decision explicitly enables it.
- When safe for the configured flow, pending records should preserve the original timestamp, local monotonic/order metadata, server receive time, device ID, and offline marker.
- On reconnect, synchronize in sequence; use idempotency keys; reconcile conflicts and duplicate candidates; never overwrite an accepted server event silently.
- If an event's claimed time is suspicious, clock skew is large, or the event conflicts with an existing shift state, route it to a manager review queue and label it unresolved.
- Dashboard should differentiate `Confirmed`, `Pending sync`, `Needs review`, `Rejected`, and `Corrected`.
- The core dashboard and API should continue to provide local access if internet is down. The system is LAN-only, not dependent on internet availability for routine operations.

---

## 13. DATABASE DESIGN

Design a normalized relational schema with migrations. At a minimum consider these entities (adjust names and relationships as appropriate and document them):

- `users`
- `roles` / `user_roles` or an equivalent explicit RBAC schema
- `employees`
- `job_functions`
- `kiosk_devices`
- `device_credentials` or a secure hash/credential-reference scheme
- `face_enrollments` / `biometric_templates` with restricted access and encryption
- `shift_templates`
- `employee_schedule_assignments`
- `schedule_exceptions` / leave markers as required
- `attendance_events` (append-only accepted event ledger)
- `attendance_event_receipts` or idempotency records
- `attendance_adjustments` / correction workflow
- `break_policies` / calculation policies as needed
- `audit_log`
- `notification_delivery` or durable event cursor if required
- `monthly_timesheet_snapshots` / finalization history
- `system_settings` with typed/validated configuration, excluding secrets
- `backup_runs` and `system_health_events` if useful

Requirements:

- UUID primary keys for identifiers that cross devices.
- Proper foreign keys, indexes, unique constraints, `CHECK` constraints where useful, and transaction-safe business rules.
- Store instants with timezone-aware database types; normalize writes and display in `Africa/Tunis`.
- Use Alembic migrations and document migration/rollback expectations. Backups are the primary rollback protection for destructive schema/data operations; do not claim all migrations can be safely reversed.
- Use database roles with least privilege. The API runtime must not run as a superuser and must not connect with the PostgreSQL administrative account.
- Explicit data-retention/deletion rules, particularly for biometric templates and attendance history.
- Test fresh install and upgrade from the previous schema version.
- Avoid relying on application checks where a database constraint can safely enforce uniqueness/integrity.

---

## 14. BACKEND API AND SECURITY

Use a versioned API prefix such as `/api/v1`. Publish OpenAPI docs for development/admin use; production access to sensitive API docs can be restricted.

Create endpoints for the real workflows: auth/session, health/readiness, initial setup, employees/functions, schedules, attendance transitions, device pairing/health/revocation, biometric enrollment/provider status, exception review, corrections/approvals, reports/finalization, audit history, app settings, backup status, and privacy readiness.

### Security baseline

- Passwords hashed with Argon2id or another currently recommended password hashing scheme; never reversible encryption for passwords.
- Strong random session/CSRF protections, secure cookies, correct SameSite/HttpOnly/Secure settings in production, session expiration/revocation, login rate limiting, and generic login error messages.
- Optional TOTP for administrators/managers if it can be implemented reliably without internet. Recovery codes must be safely stored and displayed once.
- Enforce authorization in backend services/routes, not only by hiding frontend buttons.
- Strict input validation with Pydantic and database constraints.
- Bound upload size, CSV row count, request body size, and export range; protect against CSV formula injection in exports.
- Restrict CORS to the configured local dashboard origin(s); do not use a blanket wildcard with credentials.
- Use secure TLS in production, device authentication, certificate verification/pinning where appropriate, and rotation/revocation procedures.
- Rate-limit login, enrollment, pairing, and recognition requests. Avoid account/employee enumeration through error messages.
- Configure request timeouts and maximum image/frame payload sizes. Do not write camera uploads to permanent disk; if temporary buffers are unavoidable, delete reliably and document the residual limitation.
- Structured logs with secret redaction, rotation, limited retention, and access restrictions.
- Prevent SQL injection, path traversal, SSRF, unsafe deserialization, mass assignment, insecure direct object references, and privilege escalation.
- Use parameterized SQL/ORM, safe file paths, and least-privilege filesystem/database permissions.
- Protect against CSRF where cookie-based authentication is used; validate WebSocket auth and origin; prevent unauthorized subscriptions to employee data.
- Keep all administrative exports and sensitive changes audited.
- Do not expose database ports outside loopback/local container network unless explicitly needed. Do not expose Ollama over the LAN by default.
- No secret/default password may be usable in production. During first-run setup, require creation of the initial administrator account through a one-time setup token or local-only bootstrap process, then invalidate the setup path.
- Never store raw API tokens in logs or show device secrets after their initial provisioning.
- Build a security checklist mapped to the stable OWASP ASVS release available at implementation time, prioritizing applicable controls. Do not claim certification.

### Health endpoints

Expose a low-information liveness endpoint and a protected/limited readiness endpoint that checks database/API status. Do not leak secrets, environment variables, biometric status details, or internal paths to unauthenticated callers.

---

## 15. REAL-TIME EVENT DELIVERY

Use WebSockets or a sound equivalent for low-latency updates to the manager dashboard.

- After a database transaction successfully commits an attendance event, publish an event for the dashboard.
- Avoid sending a notification before the DB commit.
- Use a reliable strategy for reconnects, event IDs/cursors, deduplication, and snapshot refresh. If the process crashes after commit but before broadcast, the dashboard must recover through reconciliation or periodic snapshot refresh.
- Authenticate dashboard subscriptions and filter by authorization.
- Notify for accepted arrival/departure/break events, device online/offline state, offline queue sync, and new review-worthy exceptions.
- Do not send biometric vectors, camera frames, internal tokens, or unnecessary personal data over the event bus.
- Add tests for disconnect/reconnect, duplicate events, multiple browser tabs, unauthorized sockets, and crash/restart recovery.

---

## 16. TIME-CALCULATION ENGINE

Keep time calculation pure and testable in a dedicated module. UI components must not calculate official hours.

Requirements:

- Calculate shift sessions from accepted events using explicit, versioned rules.
- Support overnight and split shifts and multiple breaks.
- Distinguish raw punches, corrected interpretations, and final reported hours.
- Handle missing clock-out, missing break return, duplicate punches, events in unexpected order, unscheduled punches, and offline-synchronized events as reviewable exceptions.
- Never guess a missing event without flagging it. A configurable preview estimate is acceptable only if it is labelled as an estimate and not used as finalized time.
- Separate scheduled hours, recorded presence, break deductions, validated worked hours, approved overtime, and any payroll conversion.
- Do not hide rounding. Default event storage to original precision; if a calculation policy rounds final totals, display the policy and test its exact behavior.
- Make timezone/work-date attribution explicit for shifts crossing midnight.
- Include a calculation policy version on generated and finalized timesheets.
- Do not let Ollama or any other generative model calculate authoritative time totals.

Create extensive deterministic tests for normal shifts, overnight shifts, split shifts, multiple breaks, 23:59/00:00 boundaries, daylight-saving/timezone boundaries, grace periods, missing punches, duplicate events, corrected punches, concurrent punches, offline uploads, and finalized-month reopening.

---

## 17. MONTHLY REPORTS AND EMPLOYEE REVIEW

At month-end, create per-employee and whole-restaurant reports with clear columns, legible print layout, French labels by default, Arabic support where appropriate, and a visible Cappuccino logo.

Each employee's report should show:

- Employee number, name, and function.
- Date-by-date scheduled shifts.
- Recorded check-in/out and break events.
- Actual worked duration according to the active calculation policy.
- Break deductions where applicable.
- Overtime candidates and separately approved overtime.
- Absences/leave/missing punches and unresolved conflicts.
- Corrected records, reasons, and approval metadata.
- Daily and monthly totals.
- Finalization/review status and report generation timestamp.

Rules:

- A missing punch must be obvious.
- Pending, disputed, or conflicting events must not be blended into verified totals without a clear label.
- Provide date filters and employee/function filters.
- CSV exports must resist spreadsheet formula injection and include timezone/policy metadata where useful.
- PDF/export files must be generated locally. Do not use external conversion services.
- Finalized reports should preserve a reproducible snapshot; changes after finalization require an authorized reopen and produce a new version while preserving the previous snapshot.
- Do not equate acknowledgement with legal consent or waiver.

---

## 18. OPTIONAL OLLAMA INTEGRATION

Implement Ollama as a strictly optional local-only module, disabled if not installed or configured.

Allowed examples:

- Summarize already-aggregated monthly attendance totals for an authorized manager.
- Explain why certain records were flagged for manual review, using non-biometric event metadata.
- Help draft an operational weekly summary.

Prohibited uses:

- Face recognition, generation or storage of biometric templates through the LLM.
- Deciding whether an employee was actually present when the authoritative records are ambiguous.
- Making final decisions about penalties, salary deductions, discipline, or termination.
- Altering attendance events, corrections, schedules, or calculated hours.
- Sending data to public cloud model APIs or silently using a remote fallback.
- Giving the LLM broader access to the database or filesystem than necessary.

Use only loopback/local connections. Pass only the minimum aggregated data needed, sanitize the prompt, label all output as AI-generated and non-authoritative, and ensure failure of Ollama has no impact on core product functions. If Ollama isn't installed, hide or disable AI features with an informative status, not a broken page.

---

## 19. INSTALLATION, BACKGROUND SERVICES, AND OPERATIONS

Deliver working setup and operating scripts, not hand-wavy instructions.

### Ubuntu server

Provide scripts or a documented installer to:

- Check operating system, CPU architecture, available disk/memory, Docker/Compose or chosen runtime, and required ports.
- Install or verify prerequisites without unreviewed destructive changes.
- Create non-root data/config directories with correct permissions.
- Generate strong unique secrets locally and store them outside source control with appropriate permissions.
- Initialize PostgreSQL and apply migrations.
- Configure the local TLS setup and provide a clear method for trusting the local certificate on the manager PC and pairing the Android kiosk.
- Create persistent database and encrypted backup locations.
- Start/stop/restart services and show status/logs.
- Run migrations as an explicit step with backup guidance.
- Schedule backups and verify backup completion.
- Perform a restore test into a separate/test database.
- Start automatically after reboot; use restart policies or a suitable `systemd` unit.
- Expose a health/status command.

No passwords or secrets should be printed into the terminal output, world-readable files, or shell history. The installer must be idempotent and report what it will change before sensitive actions.

### Service reliability

- Backend/database should run without an interactive terminal.
- Services should start after reboot and recover from common crash conditions.
- Logs should rotate and not contain sensitive data.
- Backups should be atomic/consistent and encrypted, and restoration must be documented and tested.
- Alert the dashboard when last backup is too old or failed.
- Provide a controlled update process with pre-update backup, version display, migration checks, and rollback/recovery guidance.
- Never auto-update dependencies from the internet in the middle of a shift. Updates should be explicit and testable.

### Network safety

- Do not configure router port forwarding.
- Bind database ports to internal container networks or loopback unless there is a documented reason otherwise.
- Bind public application listeners only to the trusted LAN interface as configured.
- Provide a firewall guidance section; do not replace the user's firewall rules wholesale.
- Make it possible to operate the core system when internet connectivity is unavailable.

### Windows 11 manager workstation

Provide simple instructions to reach the local dashboard by configured LAN hostname/IP and trust the local CA certificate. Avoid requiring the whole API/database to be installed on Windows if Ubuntu is the designated server. Dashboard must be usable in a current stable browser and remain responsive on a standard office monitor.

---

## 20. BACKUP, RESTORE, AND DATA EXPORT

Build a usable backup/restore process early, before live data is entered.

- Daily automated PostgreSQL backup by default; allow configuration of schedule and retention.
- Encrypted backup files, protected file permissions, and no biometric template plaintext in exports.
- Include required application configuration/data and documented secret-recovery requirements without packaging keys carelessly alongside backups.
- Store backups outside the database container/volume and ideally on a separate local disk. Document that a backup on the same physical laptop does not protect against total disk failure.
- Provide backup listing, last-success status, size, checksum, and failure status.
- Test restoration on a non-production target, validate schema version and record counts, and document the recovery steps.
- Provide controlled admin export of employee/attendance records without biometric templates by default.
- Do not offer unrestricted export of biometric templates. If template export/restore is strictly needed for disaster recovery, encrypt it and require a separately protected recovery key plus elevated authorization.
- Audit backup creation, restore operations, and sensitive data exports without putting secrets in the audit log.

---

## 21. TESTING AND QUALITY GATES

Use the testing frameworks appropriate to the chosen stack. Testing is a required implementation phase, not a final suggestion.

### Backend tests

- Unit tests for event state transitions, time calculations, schedule rules, corrections, authorization, idempotency, and offline conflict handling.
- API tests for valid/invalid inputs, permissions, session expiry, device revocation, report filters, and error responses.
- Database integration tests with real PostgreSQL or a reliable test service, including constraints and concurrent submissions.
- Migration tests on a fresh database and a previous-version fixture where feasible.
- Security tests for unauthenticated routes, unauthorized employee access, role escalation, brute-force rate limits, WebSocket authorization, CORS/CSRF configuration, and export injection.

### Frontend tests

- Component and integration tests for login, employees, functions, scheduling, event list, correction flow, report filters, settings, Arabic RTL, connection states, and accessibility-critical behavior.
- End-to-end browser flow: login → create demo employee → assign schedule → create synthetic attendance events → see live dashboard update → resolve an exception → export a report.
- Test stale WebSocket/reconnect behavior and duplicate notifications.

### Android tests

- Unit tests for event payload creation, state transitions shown on the kiosk, retry/idempotency behavior, and offline queue limits.
- UI/instrumentation tests for the attendance flow, timeouts, error states, permissions, and return-to-idle behavior where device/emulator facilities permit.
- Build the debug APK successfully. Provide release-build/signing instructions; do not invent or commit signing secrets.
- Test lost server connection, invalid certificates, rejected event, repeated submission, app restart, and revoked device credentials.
- Device-owner/Lock Task Mode provisioning tests must be documented. If actual hardware cannot be controlled, mark the hardware acceptance test as pending instead of claiming it passed.

### Acceptance criteria

The system is not considered complete unless the following can be demonstrated using synthetic data:

1. Fresh local installation and startup works from documentation.
2. Initial administrator setup works without a default production password.
3. Manager can create an employee and configure a function/shift.
4. A synthetic or properly enabled licensed-provider attendance event validates against the state machine and commits exactly once.
5. Dashboard updates in real time after a committed event.
6. Replayed event requests do not create duplicate punches.
7. Invalid transitions are rejected with an understandable message.
8. Corrections preserve the original record, reason, author, time, and approval state.
9. Overnight shifts, breaks, missing punches, and month-end totals match deterministic tests.
10. CSV and print/PDF reports show reproducible totals and correction history.
11. When the network goes down, the kiosk shows offline state and never falsely reports server confirmation; safe pending/fallback behavior is testable.
12. Revoked device credentials cannot submit new online events.
13. A fresh database backup can be restored in a test environment and validated.
14. Services restart after a simulated process restart and after machine reboot where environment permits.
15. French UI works throughout; Arabic text and RTL layout work on the main kiosk/dashboard flows.
16. Core pages contain no inert controls, broken routes, placeholder lorem ipsum, or silent errors.
17. No image/template/token appears in logs; no cloud requests occur during core workflows.
18. Biometric production mode remains disabled if lawful use/model license/production provider configuration is unverified.

Run all available automated suites and summarize exact outcomes, skipped tests, and reasons. Fix the failures you can reproduce. Do not suppress tests simply to obtain a green result.

---

## 22. DEVELOPMENT PHASES — COMPLETE THEM IN ORDER

### Phase 0 — Workspace and environment audit

- Inspect the repository, git status, active branch, existing app code, available runtimes, installed tooling, and project-specific instructions.
- Inspect the user-provided logo asset and existing visual assets.
- Identify any existing code that should be preserved.
- Create `docs/DECISIONS.md` and `docs/ARCHITECTURE.md` before large architectural changes.
- Record the current status and a concise actionable plan in `docs/IMPLEMENTATION_STATUS.md`.
- Do not ask questions; record reasonable assumptions and proceed.

**Exit gate:** Existing work preserved, architecture selected, environment constraints documented, and the implementation plan is specific enough to execute.

### Phase 1 — Repository foundation and development workflow

- Create clear apps/services directories, `.gitignore`, environment example files with placeholders only, lint/format config, test setup, and development commands.
- Pin/manage dependencies reproducibly.
- Add a root README with one source of truth for commands.
- Create synthetic fixtures and deterministic clock/time helpers.

**Exit gate:** Basic linting/build/test commands are reproducible and no secret or real employee data is in the repository.

### Phase 2 — Database and backend core

- Implement schema and migrations.
- Implement authentication, first-admin bootstrap, roles, employee/function CRUD, device records, configuration, health checks, and audit foundations.
- Implement the attendance state machine and idempotent event ingestion.
- Implement schedule model and time-calculation engine with extensive tests.
- Use database transactions/constraints for integrity.

**Exit gate:** Backend unit/integration tests pass; schema can be initialized from an empty database; core event and calculation workflows are tested.

### Phase 3 — Manager dashboard foundation

- Implement login, layout, navigation, branding, typography, language system, and responsive dashboard shell.
- Implement employee/function management, shift setup, schedule views, and overview.
- Connect all pages to the actual backend.
- Add accessibility and error/loading/empty states.

**Exit gate:** A manager can complete core setup via the browser; no critical page is mock-only.

### Phase 4 — Attendance event review and reporting

- Build event list, current presence, break state, exceptions, correction approval, audit history, report filtering, month finalization, and CSV/PDF/print output.
- Validate that corrected/finalized reports preserve provenance and policy versions.

**Exit gate:** Deterministic synthetic month reports match unit-test expectations; audit trail is visible and usable.

### Phase 5 — Real-time notifications and service health

- Implement authenticated real-time delivery, reconnect snapshots/cursors, notification UX, device status, backup status, and degraded-connection display.
- Ensure publish occurs only after committed transaction and prevent duplicate alerts after reconnect.

**Exit gate:** End-to-end tests demonstrate live updates and reconnection without duplicate records or notifications.

### Phase 6 — Facial recognition integration boundary and enrollment workflow

- Implement enrollment UI, provider contract, recognition request contract, template encryption/storage boundary, error handling, confidence/margin decisions, liveness outcome interface, deletion flow, and synthetic provider tests.
- Verify commercial/model rights for any actual recognition weights and write them to `docs/MODEL_CATALOG.md`.
- Use local-only processing. Prefer minimizing image retention and ensure all image processing/storage decisions are documented.
- If model licensing is not verifiable, keep the provider not configured and all real enrollment blocked; don't download questionable weights or fake successful recognition.
- Add readiness requirements before enabling production biometric use.

**Exit gate:** Enrollment and recognition contracts are tested end to end with a licensed provider if available; otherwise the unconfigured state and non-biometric fallback work honestly and safely.

### Phase 7 — Android S22 kiosk app

- Implement native Kotlin/Compose idle screen, camera capture, action buttons, server API client, TLS/pairing, result screens, encrypted outbox, retry/timeout logic, app diagnostics, French/Arabic resources, and admin-only maintenance workflow.
- Integrate the selected recognition provider without storing raw photos unnecessarily.
- Generate a debug APK and release build instructions.
- Create a dedicated-device/Device Owner/Lock Task Mode component or fully documented compatible implementation. Test whether it builds and specify exact supported enrollment method.
- Do not provision or lock the owner's S22 automatically.

**Exit gate:** APK builds, tests run, event submission/retry is idempotent, error states are visible, and kiosk-lock setup limitations are accurately documented.

### Phase 8 — LAN deployment and background services

- Complete Docker Compose/system service configuration, persistent volumes, local TLS, initial setup/bootstrap, server start/stop/status, local firewall guidance, scheduled backups, health checks, and no-internet test.
- Configure the web dashboard to use the single backend base URL and a production configuration that does not require external assets.
- Supply Windows trust/access instructions and Android pairing instructions.

**Exit gate:** A clean Ubuntu setup can run the backend/database in the background and the dashboard is reachable from a Windows browser on the private LAN.

### Phase 9 — Ollama integration (optional)

- Add a disabled-by-default local adapter for aggregated summaries and operational explanations.
- Ensure it uses only localhost, never controls records, never calculates authoritative hours, and gracefully degrades if unavailable.
- Add test coverage that core attendance still works with Ollama stopped or absent.

**Exit gate:** Core product works with no Ollama model installed; optional integration is isolated and safely disabled by default.

### Phase 10 — Security, privacy, performance, and resilience review

- Run dependency/license scan, static analysis, linter, test suite, API/security checks, accessibility checks, and a secret scan where tooling is available.
- Verify database least privilege, TLS trust behavior, device revocation, audit integrity checks, backup restoration, and log redaction.
- Test server restart, database restart, browser reconnect, network loss, and outbox reconciliation.
- Review retention/deletion, biometric readiness, export permissions, and optional AI boundary.
- Fix found issues; record remaining limitations honestly.

**Exit gate:** No unresolved critical/high security defect known from executed checks. Anything requiring physical hardware or legal review is clearly identified and safely gated.

### Phase 11 — Final acceptance and handover

- Run all tests and builds feasible in the workspace.
- Perform the complete synthetic end-to-end flow.
- Confirm user documentation matches actual commands and file paths.
- Produce a concise `docs/IMPLEMENTATION_STATUS.md` listing completed features, commands run, exact test results, build artifacts, configuration requirements, and unperformed hardware/legal tests.
- Provide operational documentation, backup/restore, admin usage, first-run setup, S22 pairing, kiosk provisioning, and disaster recovery.
- Give a brief terminal summary when finished, but the source repository and its documentation must stand on their own.

**Exit gate:** A working, tested, documented project—not merely code fragments or a plan—is available.

---

## 23. ERROR HANDLING AND EDGE CASES

Handle these explicitly throughout the application:

- Server not running, database unavailable, browser disconnected, invalid/expired session, revoked kiosk credential, TLS mismatch/certificate expiration, wrong server address, and clock/timezone misconfiguration.
- Phone reboots, app crashes, camera permission revoked, camera busy, thermal/performance degradation, no face, multiple faces, poor image quality, low confidence, close candidate scores, liveness check failure, and model/provider missing.
- Employee inactive or not enrolled; employee scheduled for a different shift; employee attempts a duplicate/incompatible event; a manager deactivates someone with open attendance state.
- Repeated HTTP requests, timeout after commit, retry after reconnect, out-of-order offline sync, duplicate UUID, and competing requests for one employee.
- Missing clock-in/out, break without return, overnight shift, split shift, manual fallback, correction after month-end, reopened finalized month, and event received in a later month.
- Unicode names, accented French, Arabic script, right-to-left punctuation, long names/functions, empty results, and CSV formula-injection values.
- Disk full, backup failure, restore failure, outdated backup, expired certificate, expired setup token, and outbox capacity reached.

Every failure should preserve existing data, show an understandable message to the operator, and log enough redacted diagnostic information for troubleshooting.

---

## 24. REQUIRED DOCUMENTATION DELIVERABLES

Create and keep current:

- `README.md`: overview and quickest safe start.
- `AGENTS.md`: project conventions for future coding agents.
- `docs/ARCHITECTURE.md`: components, trust boundaries, data flows, and failure behavior.
- `docs/DECISIONS.md`: important decisions, assumptions, rejected alternatives, and reasons.
- `docs/ATTENDANCE_RULES.md`: event state machine, shift/break calculation rules, timezone, rounding, offline rules, and examples.
- `docs/SECURITY.md`: authentication, roles, TLS, secrets, device pairing/revocation, encryption, logs, threat model, and incident response.
- `docs/PRIVACY_AND_BIOMETRICS.md`: data inventory, minimization, notice/retention/deletion process, lawful-use readiness checklist, and limitations.
- `docs/MODEL_CATALOG.md`: selected face model/provider versions, URLs, checksums, exact license, usage rights, and configuration procedure. Mark `NOT APPROVED/NOT CONFIGURED` when rights are unresolved.
- `docs/INSTALL_UBUNTU.md`: full fresh installation, updates, services, LAN/TLS, backups, and troubleshooting.
- `docs/INSTALL_WINDOWS_DASHBOARD.md`: certificate trust, browser access, and local connectivity checks.
- `docs/ANDROID_KIOSK_SETUP.md`: debug/release build, pairing, permissions, managed-device provisioning, Lock Task Mode, safe admin exit, recovery, and factory-reset warning.
- `docs/OPERATIONS.md`: daily operations, adding/deactivating employees, schedule changes, exception handling, month-end close, service status, and troubleshooting.
- `docs/BACKUP_AND_RESTORE.md`: backup schedule, retention, verification, restoration, and disaster recovery.
- `docs/TEST_PLAN.md`: automated and manual test suites with executed/pending status.
- `docs/IMPLEMENTATION_STATUS.md`: current build phase, completed items, test outputs, outstanding authorization/hardware gates, and next operational steps.

Keep commands accurate. Do not write a document claiming a feature is complete before the corresponding implementation and test exist.

---

## 25. DEFINITION OF DONE

The work is done when:

- The backend, database, dashboard, and Android source code are implemented in an organized repository.
- The Ubuntu server can run persistently in the background with controlled startup, restart, logs, and backups.
- Windows 11 can access the manager dashboard on the local network through a documented secure connection.
- The S22 kiosk builds and implements its real workflow, pairing, recovery, and managed kiosk mode path; limitations and physical-device tests are clearly reported.
- Employee/function management, shift scheduling, attendance state transitions, live notifications, exceptions, correction audit trail, month-end review, and reports work end to end with synthetic data.
- Core attendance operations do not depend on the internet or Ollama.
- No external cloud is involved in core features and no hidden telemetry is present.
- Face recognition uses a provider with verified commercial usage rights, or production biometric features are explicitly disabled with a clear, working fallback until a valid provider is configured.
- The application never confuses a pending/offline event with a server-confirmed event.
- All available automated tests/builds have been run, failures fixed where possible, and results documented accurately.
- Fresh-install, update, backup, restore, admin operation, and kiosk provisioning instructions are complete and verified against the code.
- The provided Cappuccino logo is used faithfully and the app looks intentional, professional, accessible, and suitable for staff at a restaurant entrance.
- There are no fake green checkmarks, fake test reports, fake employees presented as real, empty core pages, or claims that physical-device setup/legal authorization has occurred when it has not.

---

## 26. TECHNICAL REFERENCES TO VERIFY DURING IMPLEMENTATION

Use official current documentation first and re-check version, licensing, and compatibility at implementation time. These are research starting points, not permission to bypass license review:

- Android dedicated devices: https://developer.android.com/work/dpc/dedicated-devices
- Android Lock Task Mode: https://developer.android.com/work/dpc/dedicated-devices/lock-task-mode
- Android app architecture recommendations: https://developer.android.com/topic/architecture/recommendations
- ONNX Runtime mobile: https://onnxruntime.ai/docs/tutorials/mobile/
- FastAPI WebSockets: https://fastapi.tiangolo.com/advanced/websockets/
- PostgreSQL supported versions: https://www.postgresql.org/support/versioning/
- PostgreSQL backup and restore: https://www.postgresql.org/docs/current/backup.html
- OWASP ASVS: https://owasp.org/www-project-application-security-verification-standard/
- Tunisia INPDP: https://www.inpdp.tn/
- InsightFace license notes: https://github.com/deepinsight/insightface and https://www.insightface.ai/solutions/face-recognition-licensing
- OpenCV Zoo model-specific licenses: https://github.com/opencv/opencv_zoo

The model references above do **not** automatically authorize any model for commercial employee attendance. Check the exact artifact's terms and provenance before use.

---

## FINAL INSTRUCTION

Start now. Inspect the existing workspace, preserve relevant work, document decisions, and execute the phases in order. Make practical engineering decisions without asking routine questions. Build the whole system to the highest reliable standard that the available environment supports, test it, repair failures, and leave the owner with working code, setup scripts, a functioning synthetic demo, clear operational documentation, and a truthful report of any step that still requires physical-device access or human legal/licensing authorization.
