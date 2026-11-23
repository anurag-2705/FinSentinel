package com.finsentinel.fraud.dto.dashboard;

import java.math.BigDecimal;

public record FraudDashboardSummaryResponse(

        long totalAlerts,

        long openAlerts,

        long underReviewAlerts,

        long resolvedAlerts,

        long dismissedAlerts,

        long highRiskAlerts,

        long pendingTransactions,

        BigDecimal totalPendingAmount

) {
}