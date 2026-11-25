-- =====================================================
-- FinSentinel
-- V16: User Management Audit Log
-- =====================================================

CREATE TABLE user_management_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    actor_user_id UUID NOT NULL,
    target_user_id UUID NOT NULL,

    action VARCHAR(50) NOT NULL,

    details JSONB NOT NULL DEFAULT '{}'::jsonb,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_management_audit_actor
        FOREIGN KEY (actor_user_id)
        REFERENCES app_users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_user_management_audit_target
        FOREIGN KEY (target_user_id)
        REFERENCES app_users(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_user_management_audit_target
    ON user_management_audit(target_user_id, created_at DESC);

CREATE INDEX idx_user_management_audit_actor
    ON user_management_audit(actor_user_id, created_at DESC);

CREATE INDEX idx_user_management_audit_created_at
    ON user_management_audit(created_at DESC);