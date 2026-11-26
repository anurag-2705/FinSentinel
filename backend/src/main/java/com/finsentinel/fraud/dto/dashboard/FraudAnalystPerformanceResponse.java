package com.finsentinel.fraud.dto.dashboard;

import java.util.UUID;

public record FraudAnalystPerformanceResponse(
        UUID analystId,
        String analystEmail,
        long totalAssigned,
        long approvedAlerts,
        long rejectedAlerts,
        long confirmedBlockedAlerts,
        long completedInvestigations,
        double resolutionRate
) {
}