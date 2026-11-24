package com.finsentinel.fraud.dto.dashboard;

public record FraudRuleFrequencyResponse(
        String ruleCode,
        String ruleName,
        long triggerCount
) {
}