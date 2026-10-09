# AGENTS.md

This repository implements the Cappuccino Lac 1 local attendance and shift management system.

## Working rules
- Keep the project LAN-only and offline-first.
- Do not commit secrets, passwords, production tokens, or real staff biometric data.
- Use a safe default configuration that disables live biometric enrollment until a proper model license is validated.
- Prefer typed interfaces, tests, and clear module boundaries.
- Preserve the supplied logo asset and the French/Arabic product language requirements.

## Local workflow
1. Backend: cd apps/api && python -m venv .venv && . .venv/Scripts/Activate.ps1 && pip install -r requirements.txt
2. Run tests: pytest
3. Run API: uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
4. Frontend: cd apps/web && npm install && npm run build
5. Android: project shell only; actual device provisioning remains pending hardware authorization.

## Documentation
- docs/ARCHITECTURE.md describes the system design.
- docs/DECISIONS.md captures implementation choices and assumptions.
- docs/IMPLEMENTATION_STATUS.md records completion status and remaining hardware-dependent tasks.
