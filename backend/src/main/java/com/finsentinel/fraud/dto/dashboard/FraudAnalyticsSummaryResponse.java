package com.finsentinel.fraud.dto.dashboard;

import java.math.BigDecimal;

public record FraudAnalyticsSummaryResponse(
        long assessedTransactions,
        long flaggedTransactions,
        long heldTransactions,
        long blockedTransactions,
        BigDecimal blockedTransactionAmount,
        double fraudDetectionRate
) {
}