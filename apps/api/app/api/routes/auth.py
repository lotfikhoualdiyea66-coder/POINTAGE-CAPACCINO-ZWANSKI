from fastapi import APIRouter, Depends, HTTPException, status
from passlib.context import CryptContext
from sqlalchemy.orm import Session

from app.api.deps import get_db
from app.core.config import settings
from app.models.user import User
from app.schemas.auth import LoginRequest, TokenResponse

router = APIRouter(prefix="/api/v1/auth", tags=["auth"])
pwd_context = CryptContext(schemes=["pbkdf2_sha256"], deprecated="auto")


@router.post("/login", response_model=TokenResponse)
def login(payload: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.email == payload.email).first()
    if not user or not pwd_context.verify(payload.password, user.password_hash):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")
    return TokenResponse(access_token=user.email)


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
