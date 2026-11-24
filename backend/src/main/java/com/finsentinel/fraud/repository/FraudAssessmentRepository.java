package com.finsentinel.fraud.repository;

import com.finsentinel.fraud.entity.FraudAssessment;
import com.finsentinel.fraud.entity.FraudDecision;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface FraudAssessmentRepository
        extends JpaRepository<FraudAssessment, UUID> {

    Optional<FraudAssessment> findByTransaction_Id(UUID transactionId);

    boolean existsByTransaction_Id(UUID transactionId);

    long countByDecision(FraudDecision decision);

    long countByAssessedAtGreaterThanEqualAndAssessedAtLessThan(
            Instant start,
            Instant end
    );

    long countByAssessedAtGreaterThanEqualAndAssessedAtLessThanAndDecision(
            Instant start,
            Instant end,
            FraudDecision decision
    );

    long countByRiskScoreBetween(
            Integer minimum,
            Integer maximum
    );

    long countByAssessedAtGreaterThanEqualAndAssessedAtLessThanAndRiskScoreBetween(
            Instant start,
            Instant end,
            Integer minimum,
            Integer maximum
    );

    @Query("""
            SELECT COALESCE(SUM(a.transaction.amount), 0)
            FROM FraudAssessment a
            WHERE a.decision = :decision
            """)
    BigDecimal sumTransactionAmountByDecision(
            @Param("decision") FraudDecision decision
    );

    @Query("""
            SELECT COALESCE(SUM(a.transaction.amount), 0)
            FROM FraudAssessment a
            WHERE a.decision = :decision
            AND a.assessedAt >= :start
            AND a.assessedAt < :end
            """)
    BigDecimal sumTransactionAmountByDecisionAndDateRange(
            @Param("decision") FraudDecision decision,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}