from alembic import op
import sqlalchemy as sa

revision = "20261025_employee_pin"
down_revision = "20261017_shift_assignments"
branch_labels = None
depends_on = None


def upgrade():
    op.add_column("employees", sa.Column("pin_hash", sa.String(length=255), nullable=True))
    op.create_index("ix_kiosk_devices_pairing_token", "kiosk_devices", ["pairing_token"], unique=True)


def downgrade():
    op.drop_index("ix_kiosk_devices_pairing_token", table_name="kiosk_devices")
    op.drop_column("employees", "pin_hash")
