package com.finsentinel.fraud.dto.dashboard;

public record FraudRiskDistributionResponse(
        String riskLevel,
        long transactionCount
) {
}