-- ==========================================
-- 1. LEDGER ACCOUNTS
-- ==========================================

CREATE TABLE ledger_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    account_code VARCHAR(50) NOT NULL UNIQUE,

    account_name VARCHAR(100) NOT NULL,

    account_category VARCHAR(20) NOT NULL,

    account_id UUID UNIQUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ledger_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id),

    CONSTRAINT chk_ledger_account_category
        CHECK (
            account_category IN (
                'ASSET',
                'LIABILITY',
                'EQUITY',
                'INCOME',
                'EXPENSE'
            )
        )
);

-- System cash account

INSERT INTO ledger_accounts (
    account_code,
    account_name,
    account_category
)
VALUES (
    'BANK_CASH',
    'Bank Cash Account',
    'ASSET'
);

-- Create ledger accounts for existing customer accounts

INSERT INTO ledger_accounts (
    account_code,
    account_name,
    account_category,
    account_id
)
SELECT
    'CUSTOMER-' || account_number,
    'Customer Deposit Account',
    'LIABILITY',
    id
FROM accounts;


-- ==========================================
-- 2. FINANCIAL TRANSACTIONS
-- ==========================================

CREATE TABLE financial_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL,

    source_account_id UUID,

    destination_account_id UUID,

    transaction_type VARCHAR(20) NOT NULL,

    amount NUMERIC(19, 2) NOT NULL,

    currency VARCHAR(3) NOT NULL DEFAULT 'INR',

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    idempotency_key VARCHAR(100) NOT NULL,

    reference_number VARCHAR(50) NOT NULL UNIQUE,

    description VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    completed_at TIMESTAMPTZ,

    CONSTRAINT fk_transaction_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id),

    CONSTRAINT fk_transaction_source
        FOREIGN KEY (source_account_id)
        REFERENCES accounts(id),

    CONSTRAINT fk_transaction_destination
        FOREIGN KEY (destination_account_id)
        REFERENCES accounts(id),

    CONSTRAINT chk_transaction_type
        CHECK (
            transaction_type IN (
                'DEPOSIT',
                'WITHDRAWAL',
                'TRANSFER'
            )
        ),

    CONSTRAINT chk_transaction_status
        CHECK (
            status IN (
                'PENDING',
                'COMPLETED',
                'FAILED'
            )
        ),

    CONSTRAINT chk_transaction_amount
        CHECK (amount > 0),

    CONSTRAINT chk_transaction_currency
        CHECK (currency ~ '^[A-Z]{3}$'),

    CONSTRAINT uq_transaction_idempotency
        UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_transactions_user
    ON financial_transactions(user_id);

CREATE INDEX idx_transactions_source
    ON financial_transactions(source_account_id);

CREATE INDEX idx_transactions_destination
    ON financial_transactions(destination_account_id);

CREATE INDEX idx_transactions_created_at
    ON financial_transactions(created_at DESC);


-- ==========================================
-- 3. DOUBLE-ENTRY LEDGER
-- ==========================================

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    transaction_id UUID NOT NULL,

    ledger_account_id UUID NOT NULL,

    entry_type VARCHAR(10) NOT NULL,

    amount NUMERIC(19, 2) NOT NULL,

    currency VARCHAR(3) NOT NULL DEFAULT 'INR',

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ledger_entry_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES financial_transactions(id),

    CONSTRAINT fk_ledger_entry_account
        FOREIGN KEY (ledger_account_id)
        REFERENCES ledger_accounts(id),

    CONSTRAINT chk_ledger_entry_type
        CHECK (entry_type IN ('DEBIT', 'CREDIT')),

    CONSTRAINT chk_ledger_entry_amount
        CHECK (amount > 0),

    CONSTRAINT chk_ledger_entry_currency
        CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_ledger_entries_transaction
    ON ledger_entries(transaction_id);

CREATE INDEX idx_ledger_entries_account
    ON ledger_entries(ledger_account_id);

CREATE INDEX idx_ledger_entries_created_at
    ON ledger_entries(created_at DESC);