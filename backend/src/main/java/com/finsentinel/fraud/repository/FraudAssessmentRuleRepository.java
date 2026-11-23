
package com.finsentinel.fraud.repository;

import com.finsentinel.fraud.entity.FraudAssessmentRule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FraudAssessmentRuleRepository
        extends JpaRepository<FraudAssessmentRule, UUID> {

    List<FraudAssessmentRule> findByAssessment_Id(UUID assessmentId);

    boolean existsByAssessment_IdAndFraudRule_Id(
            UUID assessmentId,
            UUID fraudRuleId
    );
}
