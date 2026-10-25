from datetime import datetime, timedelta, timezone

import jwt
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_db
from app.core.config import settings
from app.core.security import pwd_context
from app.models.user import User
from app.schemas.auth import LoginRequest, TokenResponse

router = APIRouter(prefix="/api/v1/auth", tags=["auth"])
token_algorithm = "HS256"


def create_access_token(user: User) -> str:
    if len(settings.secret_key) < 32:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Configure a SECRET_KEY with at least 32 characters before enabling login",
        )
    now = datetime.now(timezone.utc)
    expires_at = now + timedelta(minutes=settings.access_token_expire_minutes)
    return jwt.encode(
        {"sub": user.id, "iat": now, "exp": expires_at},
        settings.secret_key,
        algorithm=token_algorithm,
    )


@router.post("/login", response_model=TokenResponse)
def login(payload: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.email == payload.email).first()
    if not user or not pwd_context.verify(payload.password, user.password_hash):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")
    access_token = create_access_token(user)
    return TokenResponse(
        access_token=access_token,
        expires_in=settings.access_token_expire_minutes * 60,
    )


@router.post("/bootstrap")
def bootstrap(db: Session = Depends(get_db)):
    existing = db.query(User).count()
    if existing > 0:
        return {"status": "already_initialized"}
    if not settings.initial_admin_password:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Configure INITIAL_ADMIN_PASSWORD before initializing the administrator account",
        )
    user = User(
        id="admin-1",
        email=settings.initial_admin_email,
        full_name="Initial Administrator",
        password_hash=pwd_context.hash(settings.initial_admin_password),
        role="administrator",
    )
    db.add(user)
    db.commit()
    return {"status": "initialized", "email": settings.initial_admin_email}
