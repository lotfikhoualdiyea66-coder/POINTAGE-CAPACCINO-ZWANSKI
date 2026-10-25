from fastapi import FastAPI
from sqlalchemy import inspect, text
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes.attendance import router as attendance_router
from app.api.routes.auth import router as auth_router
from app.api.routes.dashboard import router as dashboard_router
from app.api.routes.employees import router as employees_router
from app.api.routes.health import router as health_router
from app.api.routes.kiosk import router as kiosk_router
from app.api.routes.shifts import router as shifts_router
from app.core.config import settings
from app.db.base import Base
from app.db.session import engine

app = FastAPI(title=settings.app_name, version="0.1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=[origin.strip() for origin in settings.allowed_origins.split(",") if origin.strip()],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

if engine.dialect.name == "sqlite":
    Base.metadata.create_all(bind=engine)
    # create_all never alters existing tables; add columns introduced after a local DB was created.
    if "pin_hash" not in {column["name"] for column in inspect(engine).get_columns("employees")}:
        with engine.begin() as connection:
            connection.execute(text("ALTER TABLE employees ADD COLUMN pin_hash VARCHAR(255)"))

app.include_router(health_router)
app.include_router(auth_router)
app.include_router(employees_router)
app.include_router(attendance_router)
app.include_router(dashboard_router)
app.include_router(shifts_router)
app.include_router(kiosk_router)


@app.get("/")
def root():
    return {"app": settings.app_name, "status": "running"}
