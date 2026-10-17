from alembic import op
import sqlalchemy as sa

revision = "20261017_shift_assignments"
down_revision = "20261017_enable_rls"
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
        "employee_shift_assignments",
        sa.Column("id", sa.String(length=64), nullable=False),
        sa.Column("employee_id", sa.String(length=64), nullable=False),
        sa.Column("shift_template_id", sa.String(length=64), nullable=False),
        sa.Column("valid_from", sa.DateTime(), nullable=False),
        sa.Column("valid_to", sa.DateTime(), nullable=True),
        sa.CheckConstraint(
            "valid_to IS NULL OR valid_to >= valid_from",
            name="ck_shift_assignment_valid_range",
        ),
        sa.ForeignKeyConstraint(["employee_id"], ["employees.id"]),
        sa.ForeignKeyConstraint(["shift_template_id"], ["shift_templates.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        "ix_shift_assignment_employee_valid_from",
        "employee_shift_assignments",
        ["employee_id", "valid_from"],
    )
    op.create_index(
        "ix_employee_shift_assignments_shift_template_id",
        "employee_shift_assignments",
        ["shift_template_id"],
    )
    if op.get_bind().dialect.name == "postgresql":
        op.execute("ALTER TABLE public.employee_shift_assignments ENABLE ROW LEVEL SECURITY")


def downgrade():
    op.drop_index("ix_employee_shift_assignments_shift_template_id", table_name="employee_shift_assignments")
    op.drop_index("ix_shift_assignment_employee_valid_from", table_name="employee_shift_assignments")
    op.drop_table("employee_shift_assignments")