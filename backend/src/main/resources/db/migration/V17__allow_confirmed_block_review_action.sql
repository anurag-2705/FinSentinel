ALTER TABLE fraud_alert_reviews
DROP CONSTRAINT IF EXISTS fraud_alert_reviews_action_check;

ALTER TABLE fraud_alert_reviews
ADD CONSTRAINT fraud_alert_reviews_action_check
CHECK (action IN ('ASSIGNED', 'APPROVED', 'REJECTED', 'CONFIRMED_BLOCK'));