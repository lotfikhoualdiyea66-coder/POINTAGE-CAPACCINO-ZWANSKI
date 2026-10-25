from datetime import datetime
from typing import Generator

import jwt
from fastapi import Depends, Header, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.security import hash_kiosk_token
from app.db.session import SessionLocal
from app.models.system import KioskDevice
from app.models.user import User


bearer_scheme = HTTPBearer(auto_error=False)


def get_db() -> Generator[Session, None, None]:
    session = SessionLocal()
    try:
        yield session
    finally:
        session.close()


def require_auth(credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme), db: Session = Depends(get_db)) -> User:
    if credentials is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Authentication required")
    if len(settings.secret_key) < 32:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Authentication is not configured",
        )
    try:
        payload = jwt.decode(credentials.credentials, settings.secret_key, algorithms=["HS256"])
    except jwt.InvalidTokenError as error:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials") from error

    user_id = payload.get("sub")
    user = db.query(User).filter(User.id == user_id).first() if isinstance(user_id, str) else None
    if not user or not user.is_active:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")
    return user


def require_admin(user: User = Depends(require_auth)) -> User:
    if user.role != "administrator":
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Administrator access required")
    return user


def require_kiosk(x_kiosk_token: str | None = Header(default=None), db: Session = Depends(get_db)) -> KioskDevice:
    device = (
        db.query(KioskDevice).filter(KioskDevice.pairing_token == hash_kiosk_token(x_kiosk_token)).first()
        if x_kiosk_token
        else None
    )
    if not device or not device.active:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Kiosk not paired")
    device.last_seen_at = datetime.utcnow()
    db.commit()
    return device
