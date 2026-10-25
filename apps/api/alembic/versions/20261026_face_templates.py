from alembic import op
import sqlalchemy as sa

revision = "20261026_face_templates"
down_revision = "20261025_employee_pin"
branch_labels = None
depends_on = None


def upgrade():
    op.add_column("employees", sa.Column("face_enrollment_expires_at", sa.DateTime(), nullable=True))
    op.create_table(
        "employee_face_templates",
        sa.Column("id", sa.String(length=64), nullable=False),
        sa.Column("employee_id", sa.String(length=64), nullable=False),
        sa.Column("encrypted_embedding", sa.Text(), nullable=False),
        sa.Column("device_id", sa.String(length=64), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        sa.ForeignKeyConstraint(["employee_id"], ["employees.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_employee_face_templates_employee_id", "employee_face_templates", ["employee_id"])
    if op.get_bind().dialect.name == "postgresql":
        op.execute("ALTER TABLE public.employee_face_templates ENABLE ROW LEVEL SECURITY")


def downgrade():
    op.drop_index("ix_employee_face_templates_employee_id", table_name="employee_face_templates")
    op.drop_table("employee_face_templates")
    op.drop_column("employees", "face_enrollment_expires_at")
