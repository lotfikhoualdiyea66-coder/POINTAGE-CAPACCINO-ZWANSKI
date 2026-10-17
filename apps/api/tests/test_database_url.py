from app.db.session import normalize_database_url


def test_postgres_url_uses_psycopg_and_ssl_by_default():
    url = normalize_database_url("postgresql://postgres.project:pass@pooler.example:5432/postgres")

    assert url.drivername == "postgresql+psycopg"
    assert url.query["sslmode"] == "require"


def test_postgres_url_preserves_explicit_ssl_mode():
    url = normalize_database_url("postgres://postgres:pass@db.example/postgres?sslmode=verify-full")

    assert url.drivername == "postgresql+psycopg"
    assert url.query["sslmode"] == "verify-full"


def test_sqlite_url_remains_unchanged():
    url = normalize_database_url("sqlite:///./attendance_dev.db")

    assert url.drivername == "sqlite"
    assert "sslmode" not in url.query