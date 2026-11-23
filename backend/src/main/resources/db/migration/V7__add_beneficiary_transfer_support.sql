ALTER TABLE financial_transactions
ADD COLUMN beneficiary_id UUID;

ALTER TABLE financial_transactions
ADD CONSTRAINT fk_transaction_beneficiary
FOREIGN KEY (beneficiary_id)
REFERENCES beneficiaries(id);

CREATE INDEX idx_transactions_beneficiary
ON financial_transactions(beneficiary_id);

ALTER TABLE financial_transactions
DROP CONSTRAINT chk_transaction_type;

ALTER TABLE financial_transactions
ADD CONSTRAINT chk_transaction_type
CHECK (
    transaction_type IN (
        'DEPOSIT',
        'WITHDRAWAL',
        'TRANSFER',
        'BENEFICIARY_TRANSFER'
    )
);