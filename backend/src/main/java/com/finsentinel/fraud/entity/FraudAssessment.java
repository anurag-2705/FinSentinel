
package com.finsentinel.fraud.entity;

import com.finsentinel.transaction.entity.FinancialTransaction;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
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

    @Column(name = "ml_fraud_probability")
    private Double mlFraudProbability;

    @Column(name = "ml_risk_level", length = 20)
    private String mlRiskLevel;

    @Column(name = "ml_model_version", length = 100)
    private String mlModelVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ml_feature_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> mlFeatureSnapshot;

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

    public Double getMlFraudProbability() {
        return mlFraudProbability;
    }

    public void setMlFraudProbability(Double mlFraudProbability) {
        this.mlFraudProbability = mlFraudProbability;
    }

    public String getMlRiskLevel() {
        return mlRiskLevel;
    }

    public void setMlRiskLevel(String mlRiskLevel) {
        this.mlRiskLevel = mlRiskLevel;
    }

    public String getMlModelVersion() {
        return mlModelVersion;
    }

    public void setMlModelVersion(String mlModelVersion) {
        this.mlModelVersion = mlModelVersion;
    }

    public Map<String, Object> getMlFeatureSnapshot() {
        return mlFeatureSnapshot;
    }

    public void setMlFeatureSnapshot(Map<String, Object> mlFeatureSnapshot) {
        this.mlFeatureSnapshot = mlFeatureSnapshot;
    }
}
