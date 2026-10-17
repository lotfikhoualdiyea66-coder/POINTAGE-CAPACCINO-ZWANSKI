from alembic import op

revision = "20261017_enable_rls"
down_revision = "20261009_initial"
branch_labels = None
depends_on = None

tables = (
    "job_functions",
    "employees",
    "shift_templates",
    "attendance_events",
    "users",
    "kiosk_devices",
    "audit_logs",
    "system_settings",
)


def upgrade():
    if op.get_bind().dialect.name != "postgresql":
        return
    for table in tables:
        op.execute(f"ALTER TABLE public.{table} ENABLE ROW LEVEL SECURITY")


def downgrade():
    if op.get_bind().dialect.name != "postgresql":
        return
    for table in tables:
        op.execute(f"ALTER TABLE public.{table} DISABLE ROW LEVEL SECURITY")