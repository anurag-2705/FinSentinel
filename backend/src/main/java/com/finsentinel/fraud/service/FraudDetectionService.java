package com.finsentinel.fraud.service;

import com.finsentinel.fraud.dto.FraudAssessmentResponse;
import com.finsentinel.fraud.entity.FraudAlert;
import com.finsentinel.fraud.entity.FraudAssessment;
import com.finsentinel.fraud.entity.FraudAssessmentRule;
import com.finsentinel.fraud.entity.FraudDecision;
import com.finsentinel.fraud.entity.FraudRule;
import com.finsentinel.fraud.repository.FraudAlertRepository;
import com.finsentinel.fraud.repository.FraudAssessmentRepository;
import com.finsentinel.fraud.repository.FraudAssessmentRuleRepository;
import com.finsentinel.fraud.repository.FraudRuleRepository;

import com.finsentinel.transaction.entity.FinancialTransaction;
import com.finsentinel.transaction.entity.TransactionStatus;
import com.finsentinel.transaction.repository.FinancialTransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FraudDetectionService {

    private static final BigDecimal LARGE_TRANSACTION_LIMIT =
            new BigDecimal("50000.00");

    private static final BigDecimal VERY_LARGE_TRANSACTION_LIMIT =
            new BigDecimal("100000.00");

    private static final BigDecimal UNUSUAL_AMOUNT_MULTIPLIER =
            new BigDecimal("3");

    private static final int MINIMUM_COMPLETED_TRANSACTIONS = 5;

    private static final long FAILED_TRANSACTION_THRESHOLD = 3;

    private static final long RAPID_TRANSACTION_THRESHOLD = 3;

    private final FinancialTransactionRepository transactionRepository;
    private final FraudRuleRepository fraudRuleRepository;
    private final FraudAssessmentRepository assessmentRepository;
    private final FraudAssessmentRuleRepository assessmentRuleRepository;
    private final FraudAlertRepository fraudAlertRepository;

    public FraudDetectionService(
            FinancialTransactionRepository transactionRepository,
            FraudRuleRepository fraudRuleRepository,
            FraudAssessmentRepository assessmentRepository,
            FraudAssessmentRuleRepository assessmentRuleRepository,
            FraudAlertRepository fraudAlertRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.fraudRuleRepository = fraudRuleRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentRuleRepository = assessmentRuleRepository;
        this.fraudAlertRepository = fraudAlertRepository;
    }

    @Transactional
    public FraudAssessmentResponse assessTransaction(UUID transactionId) {

        FinancialTransaction transaction = transactionRepository
                .findById(transactionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Transaction not found")
                );

        // Prevent duplicate assessments.
        var existingAssessment =
                assessmentRepository.findByTransaction_Id(transactionId);

        if (existingAssessment.isPresent()) {
            return buildResponse(existingAssessment.get());
        }

        List<FraudRule> triggeredRules = new ArrayList<>();

        BigDecimal amount = transaction.getAmount();
        UUID userId = transaction.getUser().getId();

        Instant assessmentTime = transaction.getCreatedAt();

        int riskScore = 0;

        // Rule 1: Large transaction.
        FraudRule largeTransactionRule =
                getEnabledRule("LARGE_TRANSACTION");

        if (largeTransactionRule != null
                && amount.compareTo(LARGE_TRANSACTION_LIMIT) >= 0) {

            triggeredRules.add(largeTransactionRule);
            riskScore += largeTransactionRule.getRiskScore();
        }

        // Rule 2: Very large transaction / high-risk pattern.
        FraudRule highRiskRule =
                getEnabledRule("HIGH_RISK_PATTERN");

        if (highRiskRule != null
                && amount.compareTo(VERY_LARGE_TRANSACTION_LIMIT) >= 0) {

            triggeredRules.add(highRiskRule);
            riskScore += highRiskRule.getRiskScore();
        }

        // Rule 3: Multiple failed transactions within 24 hours.
        FraudRule failedTransactionsRule =
                getEnabledRule("MULTIPLE_FAILED_TRANSACTIONS");

        if (failedTransactionsRule != null) {

            Instant failedWindowStart =
                    assessmentTime.minus(24, ChronoUnit.HOURS);

            long failedTransactionCount =
                    transactionRepository.countByUser_IdAndStatusAndCreatedAtAfter(
                            userId,
                            TransactionStatus.FAILED,
                            failedWindowStart
                    );

            // Include the current transaction if it is already FAILED.
            if (transaction.getStatus() == TransactionStatus.FAILED) {
                failedTransactionCount++;
            }

            if (failedTransactionCount >= FAILED_TRANSACTION_THRESHOLD) {

                triggeredRules.add(failedTransactionsRule);
                riskScore += failedTransactionsRule.getRiskScore();
            }
        }

        // Rule 4: Rapid successive transactions within 5 minutes.
        FraudRule rapidTransactionsRule =
                getEnabledRule("RAPID_TRANSACTIONS");

        if (rapidTransactionsRule != null) {

            Instant rapidWindowStart =
                    assessmentTime.minus(5, ChronoUnit.MINUTES);

            long recentTransactionCount =
                    transactionRepository.countByUser_IdAndCreatedAtAfter(
                            userId,
                            rapidWindowStart
                    );

            // Include the current transaction if it has not been
            // counted by the database query.
            if (recentTransactionCount == 0) {
                recentTransactionCount = 1;
            }

            if (recentTransactionCount >= RAPID_TRANSACTION_THRESHOLD) {

                triggeredRules.add(rapidTransactionsRule);
                riskScore += rapidTransactionsRule.getRiskScore();
            }
        }

        // Rule 5: Unusual transaction amount.
        FraudRule unusualAmountRule =
                getEnabledRule("UNUSUAL_AMOUNT");

        if (unusualAmountRule != null) {

            long completedTransactionCount =
                    transactionRepository.countByUser_IdAndStatus(
                            userId,
                            TransactionStatus.COMPLETED
                    );

            // Use historical behaviour only when sufficient
            // completed transaction history exists.
            if (completedTransactionCount >= MINIMUM_COMPLETED_TRANSACTIONS) {

                Optional<BigDecimal> averageAmount =
                        transactionRepository.findAverageTransactionAmount(
                                userId,
                                TransactionStatus.COMPLETED
                        );

                if (averageAmount.isPresent()
                        && averageAmount.get().compareTo(BigDecimal.ZERO) > 0) {

                    BigDecimal unusualAmountLimit =
                            averageAmount.get().multiply(
                                    UNUSUAL_AMOUNT_MULTIPLIER
                            );

                    if (amount.compareTo(unusualAmountLimit) >= 0) {

                        triggeredRules.add(unusualAmountRule);
                        riskScore += unusualAmountRule.getRiskScore();
                    }
                }
            }
        }

        // Keep risk score within 0–100.
        riskScore = Math.min(riskScore, 100);

        FraudDecision decision;

        if (riskScore >= 70) {
            decision = FraudDecision.BLOCK;
        } else if (riskScore >= 30) {
            decision = FraudDecision.HOLD;
        } else {
            decision = FraudDecision.ALLOW;
        }

        String reason = triggeredRules.isEmpty()
                ? "No configured fraud rules were triggered."
                : "Triggered fraud rules: "
                    + triggeredRules.stream()
                        .map(FraudRule::getRuleName)
                        .reduce((first, second) -> first + ", " + second)
                        .orElse("");

        // Save fraud assessment.
        FraudAssessment assessment = new FraudAssessment();

        assessment.setTransaction(transaction);
        assessment.setRiskScore(riskScore);
        assessment.setDecision(decision);
        assessment.setAssessmentReason(reason);

        assessment = assessmentRepository.saveAndFlush(assessment);

        // Save every triggered rule.
        for (FraudRule rule : triggeredRules) {

            FraudAssessmentRule assessmentRule =
                    new FraudAssessmentRule();

            assessmentRule.setAssessment(assessment);
            assessmentRule.setFraudRule(rule);

            assessmentRuleRepository.save(assessmentRule);
        }

        // Generate alert for suspicious transactions.
        if (decision == FraudDecision.HOLD
                || decision == FraudDecision.BLOCK) {

            FraudAlert alert = new FraudAlert();

            alert.setAssessment(assessment);

            fraudAlertRepository.save(alert);
        }

        return buildResponse(assessment);
    }

    private FraudRule getEnabledRule(String ruleCode) {

        return fraudRuleRepository.findByRuleCode(ruleCode)
                .filter(rule -> Boolean.TRUE.equals(rule.getEnabled()))
                .orElse(null);
    }

    private FraudAssessmentResponse buildResponse(
            FraudAssessment assessment
    ) {

        List<String> triggeredRuleNames =
                assessmentRuleRepository
                        .findByAssessment_Id(assessment.getId())
                        .stream()
                        .map(item -> item.getFraudRule().getRuleName())
                        .toList();

        return new FraudAssessmentResponse(
                assessment.getId(),
                assessment.getTransaction().getId(),
                assessment.getRiskScore(),
                assessment.getDecision(),
                assessment.getAssessmentReason(),
                triggeredRuleNames,
                assessment.getAssessedAt()
        );
    }
}