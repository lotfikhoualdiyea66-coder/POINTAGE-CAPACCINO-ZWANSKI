import hashlib

from passlib.context import CryptContext

pwd_context = CryptContext(schemes=["pbkdf2_sha256"], deprecated="auto")


def hash_kiosk_token(token: str) -> str:
    return hashlib.sha256(token.encode("utf-8")).hexdigest()
