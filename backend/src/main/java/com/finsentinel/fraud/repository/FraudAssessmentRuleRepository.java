package com.finsentinel.fraud.repository;

import com.finsentinel.fraud.entity.FraudAssessmentRule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FraudAssessmentRuleRepository
        extends JpaRepository<FraudAssessmentRule, UUID> {

    List<FraudAssessmentRule> findByAssessment_Id(UUID assessmentId);

    boolean existsByAssessment_IdAndFraudRule_Id(
            UUID assessmentId,
            UUID fraudRuleId
    );

    // All-time fraud rule frequency.
    @Query("""
            SELECT
                r.ruleCode,
                r.ruleName,
                COUNT(ar.id)
            FROM FraudAssessmentRule ar
            JOIN ar.fraudRule r
            GROUP BY r.id, r.ruleCode, r.ruleName
            ORDER BY COUNT(ar.id) DESC
            """)
    List<Object[]> findRuleFrequency();

    // Fraud rule frequency within a selected date range.
    @Query("""
            SELECT
                r.ruleCode,
                r.ruleName,
                COUNT(ar.id)
            FROM FraudAssessmentRule ar
            JOIN ar.fraudRule r
            JOIN ar.assessment a
            WHERE a.assessedAt >= :start
              AND a.assessedAt < :end
            GROUP BY r.id, r.ruleCode, r.ruleName
            ORDER BY COUNT(ar.id) DESC
            """)
    List<Object[]> findRuleFrequencyByDateRange(
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}