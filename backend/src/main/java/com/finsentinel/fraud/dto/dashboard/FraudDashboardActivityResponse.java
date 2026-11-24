package com.finsentinel.fraud.dto.dashboard;

public record FraudDashboardActivityResponse(
        String day,
        long transactions,
        long flagged
) {
}