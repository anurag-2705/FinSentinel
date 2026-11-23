package com.finsentinel.fraud.dto;

import com.finsentinel.fraud.entity.FraudAlertStatus;
import com.finsentinel.fraud.entity.FraudDecision;
import com.finsentinel.transaction.entity.TransactionStatus;
import com.finsentinel.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudAlertResponse(

        UUID alertId,

        UUID assessmentId,

        UUID transactionId,

        String transactionReference,

        TransactionType transactionType,

        BigDecimal amount,

        String currency,

        TransactionStatus transactionStatus,

        Integer riskScore,

        FraudDecision decision,

        String assessmentReason,

        FraudAlertStatus alertStatus,

        UUID assignedAnalystId,

        String assignedAnalystEmail,

        String analystNotes,

        Instant createdAt,

        Instant updatedAt,

        Instant resolvedAt

) {}