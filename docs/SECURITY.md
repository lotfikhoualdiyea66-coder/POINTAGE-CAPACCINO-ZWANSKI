# Security baseline

## Principles
- Keep the application inside the local LAN.
- Prefer server-authoritative attendance decisions and a minimal encrypted queue on clients.
- Store no secrets in the repository.
- Require explicit config for production tokens, TLS, and device credentials.

## Data handling
- Do not store raw face images, camera frames, or biometric vectors in the repository.
- Use a provider abstraction for biometric templates and disable live enrollment until authorized.
- Protect any encrypted biometric templates with local key management and restricted access.

## Operational guidance
- Use Docker or systemd with non-root service accounts.
- Rotate local TLS certificates and revoke any compromised device credentials.
- Keep database and backup locations outside the repo root.
- Review audit logs for any administrative deletion or export operation.
