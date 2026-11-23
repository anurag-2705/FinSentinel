CREATE TABLE fraud_alert_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    alert_id UUID NOT NULL
        REFERENCES fraud_alerts(id),

    analyst_id UUID NOT NULL
        REFERENCES app_users(id),

    action VARCHAR(30) NOT NULL
        CHECK (action IN ('ASSIGNED', 'APPROVED', 'REJECTED')),

    notes VARCHAR(2000),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fraud_alert_reviews_alert_created
    ON fraud_alert_reviews(alert_id, created_at DESC);

CREATE INDEX idx_fraud_alert_reviews_analyst
    ON fraud_alert_reviews(analyst_id);