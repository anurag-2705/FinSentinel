
package com.finsentinel.fraud.entity;

import com.finsentinel.transaction.entity.FinancialTransaction;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_assessments")
public class FraudAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "transaction_id",
            nullable = false,
            unique = true
    )
    private FinancialTransaction transaction;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FraudDecision decision;

    @Column(name = "assessment_reason", length = 1000)
    private String assessmentReason;

    @Column(name = "assessed_at", nullable = false)
    private Instant assessedAt;

    @PrePersist
    protected void onCreate() {
        if (assessedAt == null) {
            assessedAt = Instant.now();
        }
    }

    public FraudAssessment() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public FinancialTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(FinancialTransaction transaction) {
        this.transaction = transaction;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public FraudDecision getDecision() {
        return decision;
    }

    public void setDecision(FraudDecision decision) {
        this.decision = decision;
    }

    public String getAssessmentReason() {
        return assessmentReason;
    }

    public void setAssessmentReason(String assessmentReason) {
        this.assessmentReason = assessmentReason;
    }

    public Instant getAssessedAt() {
        return assessedAt;
    }
}
