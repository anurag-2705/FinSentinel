package com.finsentinel.fraud.dto.dashboard;

import java.util.UUID;

public record FraudAnalystWorkloadResponse(

        UUID analystId,

        String analystEmail,

        long totalAssignedAlerts,

        long openAssignedAlerts,

        long underReviewAlerts,

        long resolvedAlerts,

        long workloadScore,

        boolean recommended

) {
}