package com.finsentinel.fraud.dto;

import com.finsentinel.fraud.entity.FraudReviewAction;

import java.time.Instant;
import java.util.UUID;

public record FraudAlertReviewResponse(
        UUID reviewId,
        FraudReviewAction action,
        UUID analystId,
        String analystEmail,
        String notes,
        Instant createdAt
) {
}