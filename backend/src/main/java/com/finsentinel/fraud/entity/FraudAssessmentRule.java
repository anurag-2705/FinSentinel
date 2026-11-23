
package com.finsentinel.fraud.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "fraud_assessment_rules",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_assessment_rule",
            columnNames = {"assessment_id", "fraud_rule_id"}
        )
    }
)
public class FraudAssessmentRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private FraudAssessment assessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fraud_rule_id", nullable = false)
    private FraudRule fraudRule;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @PrePersist
    protected void onCreate() {
        if (triggeredAt == null) {
            triggeredAt = Instant.now();
        }
    }

    public FraudAssessmentRule() {
    }

    public UUID getId() {
        return id;
    }

    public FraudAssessment getAssessment() {
        return assessment;
    }

    public void setAssessment(FraudAssessment assessment) {
        this.assessment = assessment;
    }

    public FraudRule getFraudRule() {
        return fraudRule;
    }

    public void setFraudRule(FraudRule fraudRule) {
        this.fraudRule = fraudRule;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }
}
