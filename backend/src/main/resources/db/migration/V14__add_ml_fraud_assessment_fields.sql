ALTER TABLE fraud_assessments
    ADD COLUMN ml_fraud_probability DOUBLE PRECISION,
    ADD COLUMN ml_risk_level VARCHAR(20),
    ADD COLUMN ml_model_version VARCHAR(100);
