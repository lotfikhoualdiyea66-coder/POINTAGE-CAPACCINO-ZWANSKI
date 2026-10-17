# Security baseline

## Principles
- Keep the application inside the local LAN.
- Prefer server-authoritative attendance decisions and a minimal encrypted queue on clients.
- Store no secrets in the repository.
- Require explicit config for production tokens, TLS, and device credentials.

## Dashboard authentication
- Configure a unique `SECRET_KEY` of at least 32 characters and a strong `INITIAL_ADMIN_PASSWORD` before bootstrapping the first administrator.
- Dashboard access tokens are signed HS256 tokens that expire after 30 minutes.
- Employee, job-function, attendance, and dashboard endpoints require administrator authorization. Kiosk event submission remains unavailable until device credentials are implemented.
- Do not expose the development HTTP service to the internet. Use verified TLS and an authenticated deployment before any production or remote access.

## Supabase Postgres
- Keep the database connection URI in the API server's ignored `.env`; never expose it to the browser or Android app.
- Apply schema changes through Alembic. The Supabase migration enables RLS without `anon` or `authenticated` policies, blocking direct Data API access to application tables.
- The current dashboard login remains the FastAPI administrator login stored in Postgres; it is not Supabase Auth. Do not create public policies to bypass the API's authorization checks.
- Use the Supabase session pooler over TLS for the persistent IPv4-hosted API. A real Supabase connection still requires a project database URI and password supplied locally.

## Data handling
- Do not store raw face images, camera frames, or biometric vectors in the repository.
- Use a provider abstraction for biometric templates and disable live enrollment until authorized.
- Protect any encrypted biometric templates with local key management and restricted access.

## Operational guidance
- Use Docker or systemd with non-root service accounts.
- Rotate local TLS certificates and revoke any compromised device credentials.
- Keep database and backup locations outside the repo root.
- Review audit logs for any administrative deletion or export operation.
