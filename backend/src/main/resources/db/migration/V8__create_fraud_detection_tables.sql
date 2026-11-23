
-- 1. Configurable fraud detection rules
CREATE TABLE fraud_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_code VARCHAR(50) NOT NULL UNIQUE,
    rule_name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    risk_score INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_fraud_rule_risk_score
        CHECK (risk_score BETWEEN 0 AND 100)
);

-- 2. Fraud assessments for financial transactions
CREATE TABLE fraud_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL UNIQUE,
    risk_score INTEGER NOT NULL,
    decision VARCHAR(20) NOT NULL,
    assessment_reason VARCHAR(1000),
    assessed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_fraud_assessment_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES financial_transactions(id),

    CONSTRAINT chk_fraud_assessment_risk_score
        CHECK (risk_score BETWEEN 0 AND 100),

    CONSTRAINT chk_fraud_assessment_decision
        CHECK (decision IN ('ALLOW', 'HOLD', 'BLOCK'))
);

CREATE INDEX idx_fraud_assessments_decision
    ON fraud_assessments(decision);

CREATE INDEX idx_fraud_assessments_assessed_at
    ON fraud_assessments(assessed_at DESC);

-- 3. Rules triggered during an assessment
CREATE TABLE fraud_assessment_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL,
    fraud_rule_id UUID NOT NULL,
    triggered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_assessment_rule_assessment
        FOREIGN KEY (assessment_id)
        REFERENCES fraud_assessments(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_assessment_rule_rule
        FOREIGN KEY (fraud_rule_id)
        REFERENCES fraud_rules(id),

    CONSTRAINT uq_assessment_rule
        UNIQUE (assessment_id, fraud_rule_id)
);

CREATE INDEX idx_assessment_rules_assessment
    ON fraud_assessment_rules(assessment_id);

-- 4. Fraud alerts for analyst investigation
CREATE TABLE fraud_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    assigned_to UUID,
    analyst_notes VARCHAR(2000),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_fraud_alert_assessment
        FOREIGN KEY (assessment_id)
        REFERENCES fraud_assessments(id),

    CONSTRAINT fk_fraud_alert_assignee
        FOREIGN KEY (assigned_to)
        REFERENCES app_users(id),

    CONSTRAINT chk_fraud_alert_status
        CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED'))
);

CREATE INDEX idx_fraud_alerts_status
    ON fraud_alerts(status);

CREATE INDEX idx_fraud_alerts_created_at
    ON fraud_alerts(created_at DESC);
