
package com.finsentinel.fraud.dto;

import com.finsentinel.fraud.entity.FraudDecision;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FraudAssessmentResponse(
        UUID assessmentId,
        UUID transactionId,
        Integer riskScore,
        FraudDecision decision,
        String assessmentReason,
        List<String> triggeredRules,
        Instant assessedAt
) {}
