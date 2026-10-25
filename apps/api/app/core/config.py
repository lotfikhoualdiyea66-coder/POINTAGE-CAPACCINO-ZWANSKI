from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "Cappuccino Lac 1 Attendance"
    environment: str = "development"
    database_url: str = "sqlite:///./attendance_dev.db"
    secret_key: str = ""
    access_token_expire_minutes: int = 30
    allowed_origins: str = "http://localhost:5173,http://127.0.0.1:5173"
    initial_admin_email: str = "admin@cappuccino.example"
    initial_admin_password: str = ""
    timezone: str = "Africa/Tunis"
    kiosk_pairing_code: str = ""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", case_sensitive=False)


settings = Settings()
