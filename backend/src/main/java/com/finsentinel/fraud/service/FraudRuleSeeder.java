package com.finsentinel.fraud.service;

import com.finsentinel.fraud.entity.FraudRule;
import com.finsentinel.fraud.repository.FraudRuleRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FraudRuleSeeder implements CommandLineRunner {

    private final FraudRuleRepository fraudRuleRepository;

    public FraudRuleSeeder(FraudRuleRepository fraudRuleRepository) {
        this.fraudRuleRepository = fraudRuleRepository;
    }

    @Override
    public void run(String... args) {

        List<FraudRule> defaultRules = List.of(
            createRule(
                "LARGE_TRANSACTION",
                "Large Transaction",
                "Transaction amount exceeds the configured large transaction threshold.",
                30
            ),
            createRule(
                "RAPID_TRANSACTIONS",
                "Rapid Successive Transactions",
                "Multiple transactions occur within a short time period.",
                25
            ),
            createRule(
                "UNUSUAL_AMOUNT",
                "Unusual Transaction Amount",
                "Transaction amount is significantly different from typical activity.",
                20
            ),
            createRule(
                "MULTIPLE_FAILED_TRANSACTIONS",
                "Multiple Failed Transactions",
                "Multiple failed transactions are detected within a short period.",
                35
            ),
            createRule(
                "HIGH_RISK_PATTERN",
                "High-Risk Transaction Pattern",
                "A combination of suspicious transaction indicators is detected.",
                40
            ),
            createRule(
                "HIGH_VELOCITY_SPENDING",
                "High Velocity Spending",
                "Total transaction spending within a rolling 60-minute window exceeds the configured threshold.",
                35
            )
        );

        for (FraudRule rule : defaultRules) {
            if (!fraudRuleRepository.existsByRuleCode(rule.getRuleCode())) {
                fraudRuleRepository.save(rule);
            }
        }

        System.out.println("FinSentinel fraud rules initialized successfully.");
    }

    private FraudRule createRule(
            String code,
            String name,
            String description,
            int riskScore
    ) {
        FraudRule rule = new FraudRule();
        rule.setRuleCode(code);
        rule.setRuleName(name);
        rule.setDescription(description);
        rule.setRiskScore(riskScore);
        rule.setEnabled(true);

        return rule;
    }
}