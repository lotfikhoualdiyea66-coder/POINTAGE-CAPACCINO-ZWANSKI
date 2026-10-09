import pytest
from fastapi.testclient import TestClient

from app.db.base import Base
from app.db.session import SessionLocal, engine
from app.core.config import settings
from app.main import app


@pytest.fixture(scope="function")
def client():
    original_password = settings.initial_admin_password
    settings.initial_admin_password = "test-only-password"
    Base.metadata.create_all(bind=engine)
    try:
        yield TestClient(app)
    finally:
        Base.metadata.drop_all(bind=engine)
        settings.initial_admin_password = original_password


@pytest.fixture(scope="function")
def db_session():
    session = SessionLocal()
    try:
        yield session
    finally:
        session.close()
