CREATE TABLE IF NOT EXISTS account_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    performed_by_user_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,
    previous_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_account_history_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_account_history_user
        FOREIGN KEY (performed_by_user_id)
        REFERENCES app_users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_account_history_action
        CHECK (action IN ('FREEZE', 'UNFREEZE')),

    CONSTRAINT chk_account_history_previous_status
        CHECK (previous_status IN ('ACTIVE', 'FROZEN', 'CLOSED')),

    CONSTRAINT chk_account_history_new_status
        CHECK (new_status IN ('ACTIVE', 'FROZEN', 'CLOSED'))
);

CREATE INDEX IF NOT EXISTS idx_account_history_account
    ON account_status_history(account_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_account_history_user
    ON account_status_history(performed_by_user_id, created_at DESC);