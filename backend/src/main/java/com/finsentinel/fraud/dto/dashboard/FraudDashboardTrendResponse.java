package com.finsentinel.fraud.dto.dashboard;

public record FraudDashboardTrendResponse(
        String day,
        long assessed,
        long flagged,
        long blocked
) {
}