CREATE TABLE beneficiaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL,

    beneficiary_name VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),

    bank_name VARCHAR(100) NOT NULL,
    account_number VARCHAR(34) NOT NULL,
    ifsc_code VARCHAR(11) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_beneficiary_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_beneficiary_status
        CHECK (status IN ('ACTIVE', 'BLOCKED')),

    CONSTRAINT chk_beneficiary_ifsc
        CHECK (ifsc_code ~ '^[A-Z]{4}0[A-Z0-9]{6}$'),

    CONSTRAINT uq_user_beneficiary_account
        UNIQUE (user_id, ifsc_code, account_number)
);

CREATE INDEX idx_beneficiaries_user
    ON beneficiaries(user_id);

CREATE INDEX idx_beneficiaries_user_status
    ON beneficiaries(user_id, status);