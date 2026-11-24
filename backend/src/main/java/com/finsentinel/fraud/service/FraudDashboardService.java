package com.finsentinel.fraud.service;

import com.finsentinel.fraud.dto.dashboard.FraudAnalystPerformanceResponse;
import com.finsentinel.fraud.entity.FraudReviewAction;
import com.finsentinel.fraud.repository.FraudAlertReviewRepository;
import com.finsentinel.fraud.dto.dashboard.FraudRiskDistributionResponse;
import com.finsentinel.fraud.dto.dashboard.FraudRuleFrequencyResponse;
import com.finsentinel.fraud.repository.FraudAssessmentRuleRepository;

import com.finsentinel.fraud.dto.dashboard.FraudDashboardActivityResponse;
import com.finsentinel.transaction.repository.FinancialTransactionRepository;

import com.finsentinel.fraud.dto.dashboard.FraudAnalyticsSummaryResponse;
import com.finsentinel.fraud.entity.FraudDecision;
import com.finsentinel.fraud.repository.FraudAssessmentRepository;
import com.finsentinel.fraud.dto.dashboard.FraudDashboardTrendResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import com.finsentinel.fraud.dto.FraudAlertResponse;
import com.finsentinel.fraud.dto.dashboard.FraudAnalystWorkloadResponse;
import com.finsentinel.fraud.dto.dashboard.FraudDashboardPageResponse;
import com.finsentinel.fraud.dto.dashboard.FraudDashboardSummaryResponse;

import com.finsentinel.fraud.entity.FraudAlert;
import com.finsentinel.fraud.entity.FraudAlertStatus;

import com.finsentinel.fraud.repository.FraudAlertRepository;
import com.finsentinel.fraud.repository.FraudDashboardSpecification;

import com.finsentinel.role.entity.UserRole;
import com.finsentinel.role.repository.UserRoleRepository;

import com.finsentinel.transaction.entity.FinancialTransaction;
import com.finsentinel.transaction.entity.TransactionStatus;
import com.finsentinel.transaction.entity.TransactionType;

import com.finsentinel.user.entity.AppUser;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.data.jpa.domain.Specification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FraudDashboardService {

    private static final int HIGH_RISK_THRESHOLD = 70;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String FRAUD_ANALYST_ROLE = "FRAUD_ANALYST";
private static final ZoneId ANALYTICS_ZONE =
        ZoneId.of("Asia/Kolkata");

private record AnalyticsDateRange(
        Instant start,
        Instant end,
        int days
) {}

private AnalyticsDateRange resolveDateRange(String period) {

    LocalDate today = LocalDate.now(ANALYTICS_ZONE);

    LocalDate startDate;
    int days;

    switch (period == null ? "7D" : period.toUpperCase()) {

        case "7D" -> {
            days = 7;
            startDate = today.minusDays(6);
        }

        case "30D" -> {
            days = 30;
            startDate = today.minusDays(29);
        }

        case "90D" -> {
            days = 90;
            startDate = today.minusDays(89);
        }

        case "ALL" -> {
            startDate = LocalDate.of(1970, 1, 1);
            days = 90;
        }

        default -> throw new IllegalArgumentException(
                "Period must be 7D, 30D, 90D or ALL"
        );
    }

    Instant start = startDate
            .atStartOfDay(ANALYTICS_ZONE)
            .toInstant();

    Instant end = today.plusDays(1)
            .atStartOfDay(ANALYTICS_ZONE)
            .toInstant();

    return new AnalyticsDateRange(start, end, days);
}

    private final FraudAlertRepository fraudAlertRepository;
private final FraudAssessmentRepository fraudAssessmentRepository;
private final UserRoleRepository userRoleRepository;
private final FinancialTransactionRepository financialTransactionRepository;
    private final FraudAssessmentRuleRepository fraudAssessmentRuleRepository;
    private final FraudAlertReviewRepository fraudAlertReviewRepository;


    public FraudDashboardService(
        FraudAlertRepository fraudAlertRepository,
        FraudAssessmentRepository fraudAssessmentRepository,
        FraudAlertReviewRepository fraudAlertReviewRepository,
        FraudAssessmentRuleRepository fraudAssessmentRuleRepository,
        UserRoleRepository userRoleRepository,
        FinancialTransactionRepository financialTransactionRepository
) {
    this.fraudAlertRepository = fraudAlertRepository;
    this.fraudAssessmentRepository = fraudAssessmentRepository;
    this.fraudAlertReviewRepository = fraudAlertReviewRepository;
    this.fraudAssessmentRuleRepository = fraudAssessmentRuleRepository;
    this.userRoleRepository = userRoleRepository;
    this.financialTransactionRepository = financialTransactionRepository;
}
    // Dashboard summary

    public FraudDashboardSummaryResponse getSummary() {

        long totalAlerts = fraudAlertRepository.count();

        long openAlerts = fraudAlertRepository.countByStatus(
                FraudAlertStatus.OPEN
        );

        long underReviewAlerts = fraudAlertRepository.countByStatus(
                FraudAlertStatus.UNDER_REVIEW
        );

        long resolvedAlerts = fraudAlertRepository.countByStatus(
                FraudAlertStatus.RESOLVED
        );

        long dismissedAlerts = fraudAlertRepository.countByStatus(
                FraudAlertStatus.DISMISSED
        );

        long highRiskAlerts =
                fraudAlertRepository.countByAssessment_RiskScoreGreaterThanEqual(
                        HIGH_RISK_THRESHOLD
                );

        long pendingTransactions =
                fraudAlertRepository.countByTransactionStatus(
                        TransactionStatus.PENDING
                );

        BigDecimal totalPendingAmount =
                fraudAlertRepository.sumTransactionAmountByTransactionStatus(
                        TransactionStatus.PENDING
                );

        return new FraudDashboardSummaryResponse(
                totalAlerts,
                openAlerts,
                underReviewAlerts,
                resolvedAlerts,
                dismissedAlerts,
                highRiskAlerts,
                pendingTransactions,
                totalPendingAmount
        );
    }


// Dashboard: last seven days of transaction activity

// Fraud analytics summary using actual database records.
public FraudAnalyticsSummaryResponse getAnalyticsSummary(String period) {

    AnalyticsDateRange range = resolveDateRange(period);

    long assessedTransactions =
            fraudAssessmentRepository
                    .countByAssessedAtGreaterThanEqualAndAssessedAtLessThan(
                            range.start(),
                            range.end()
                    );

    long heldTransactions =
            fraudAssessmentRepository
                    .countByAssessedAtGreaterThanEqualAndAssessedAtLessThanAndDecision(
                            range.start(),
                            range.end(),
                            FraudDecision.HOLD
                    );

    long blockedTransactions =
            fraudAssessmentRepository
                    .countByAssessedAtGreaterThanEqualAndAssessedAtLessThanAndDecision(
                            range.start(),
                            range.end(),
                            FraudDecision.BLOCK
                    );

    long flaggedTransactions =
            heldTransactions + blockedTransactions;

    BigDecimal blockedAmount =
            fraudAssessmentRepository
                    .sumTransactionAmountByDecisionAndDateRange(
                            FraudDecision.BLOCK,
                            range.start(),
                            range.end()
                    );

    double fraudDetectionRate = assessedTransactions == 0
            ? 0.0
            : (flaggedTransactions * 100.0) / assessedTransactions;

    return new FraudAnalyticsSummaryResponse(
            assessedTransactions,
            flaggedTransactions,
            heldTransactions,
            blockedTransactions,
            blockedAmount,
            Math.round(fraudDetectionRate * 100.0) / 100.0
    );
}
// Most frequently triggered fraud detection rules.
public List<FraudRuleFrequencyResponse> getTopFraudRules(String period) {

    AnalyticsDateRange range = resolveDateRange(period);

    List<Object[]> results;

    if ("ALL".equalsIgnoreCase(period)) {
        results = fraudAssessmentRuleRepository.findRuleFrequency();
    } else {
        results = fraudAssessmentRuleRepository.findRuleFrequencyByDateRange(
                range.start(),
                range.end()
        );
    }

    return results.stream()
            .map(row -> new FraudRuleFrequencyResponse(
                    (String) row[0],
                    (String) row[1],
                    ((Number) row[2]).longValue()
            ))
            .toList();
}
// Distribution of assessed transactions by risk level.
public List<FraudRiskDistributionResponse> getRiskDistribution(String period) {

    AnalyticsDateRange range = resolveDateRange(period);

    long lowRisk;
    long mediumRisk;
    long highRisk;

    if ("ALL".equalsIgnoreCase(period)) {

        lowRisk = fraudAssessmentRepository.countByRiskScoreBetween(0, 29);
        mediumRisk = fraudAssessmentRepository.countByRiskScoreBetween(30, 69);
        highRisk = fraudAssessmentRepository.countByRiskScoreBetween(70, 100);

    } else {

        lowRisk = fraudAssessmentRepository
                .countByAssessedAtGreaterThanEqualAndAssessedAtLessThanAndRiskScoreBetween(
                        range.start(), range.end(), 0, 29
                );

        mediumRisk = fraudAssessmentRepository
                .countByAssessedAtGreaterThanEqualAndAssessedAtLessThanAndRiskScoreBetween(
                        range.start(), range.end(), 30, 69
                );

        highRisk = fraudAssessmentRepository
                .countByAssessedAtGreaterThanEqualAndAssessedAtLessThanAndRiskScoreBetween(
                        range.start(), range.end(), 70, 100
                );
    }

    return List.of(
            new FraudRiskDistributionResponse("LOW", lowRisk),
            new FraudRiskDistributionResponse("MEDIUM", mediumRisk),
            new FraudRiskDistributionResponse("HIGH", highRisk)
    );
}
// Daily fraud trends for the last seven days.
public List<FraudDashboardTrendResponse> getDailyFraudTrends(String period) {

    AnalyticsDateRange range = resolveDateRange(period);

    LocalDate today = LocalDate.now(ANALYTICS_ZONE);

    int days = range.days();

    DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd MMM");

    return java.util.stream.IntStream.range(0, days)
            .mapToObj(dayIndex -> {

                LocalDate date = today.minusDays(days - 1L - dayIndex);

                Instant start = date.atStartOfDay(ANALYTICS_ZONE).toInstant();

                Instant end = date.plusDays(1)
                        .atStartOfDay(ANALYTICS_ZONE)
                        .toInstant();

                long assessed =
                        fraudAssessmentRepository
                                .countByAssessedAtGreaterThanEqualAndAssessedAtLessThan(
                                        start, end
                                );

                long held =
                        fraudAssessmentRepository
                                .countByAssessedAtGreaterThanEqualAndAssessedAtLessThanAndDecision(
                                        start, end, FraudDecision.HOLD
                                );

                long blocked =
                        fraudAssessmentRepository
                                .countByAssessedAtGreaterThanEqualAndAssessedAtLessThanAndDecision(
                                        start, end, FraudDecision.BLOCK
                                );

                return new FraudDashboardTrendResponse(
                        date.format(formatter),
                        assessed,
                        held + blocked,
                        blocked
                );
            })
            .toList();
}
public List<FraudDashboardActivityResponse> getActivity() {

    ZoneId zone = ZoneId.of("Asia/Kolkata");

    LocalDate today = LocalDate.now(zone);

    DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("EEE");

    return java.util.stream.IntStream.rangeClosed(0, 6)
        .mapToObj(dayIndex -> {

                LocalDate date = today.minusDays(6 - dayIndex);

                Instant start = date.atStartOfDay(zone).toInstant();

                Instant end = date.plusDays(1)
                        .atStartOfDay(zone)
                        .toInstant();

                long transactions =
                        financialTransactionRepository
                                .countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                        start,
                                        end
                                );

                long flagged =
                        fraudAlertRepository
                                .countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                        start,
                                        end
                                );

                return new FraudDashboardActivityResponse(
                        date.format(formatter),
                        transactions,
                        flagged
                );
            })
            .toList();
}

// Analyst performance metrics based on actual review records.
public List<FraudAnalystPerformanceResponse> getAnalystPerformance(String period) {

    AnalyticsDateRange range = resolveDateRange(period);

    List<UserRole> analysts =
            userRoleRepository.findByRole_Name(FRAUD_ANALYST_ROLE);

    return analysts.stream()
            .map(userRole -> {

                AppUser analyst = userRole.getUser();
                UUID analystId = analyst.getId();

                long totalAssigned;
                long approved;
                long rejected;

                if ("ALL".equalsIgnoreCase(period)) {

                    totalAssigned =
                            fraudAlertReviewRepository.countByAnalyst_IdAndAction(
                                    analystId,
                                    FraudReviewAction.ASSIGNED
                            );

                    approved =
                            fraudAlertReviewRepository.countByAnalyst_IdAndAction(
                                    analystId,
                                    FraudReviewAction.APPROVED
                            );

                    rejected =
                            fraudAlertReviewRepository.countByAnalyst_IdAndAction(
                                    analystId,
                                    FraudReviewAction.REJECTED
                            );

                } else {

                    totalAssigned =
                            fraudAlertReviewRepository
                                    .countByAnalyst_IdAndActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                            analystId,
                                            FraudReviewAction.ASSIGNED,
                                            range.start(),
                                            range.end()
                                    );

                    approved =
                            fraudAlertReviewRepository
                                    .countByAnalyst_IdAndActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                            analystId,
                                            FraudReviewAction.APPROVED,
                                            range.start(),
                                            range.end()
                                    );

                    rejected =
                            fraudAlertReviewRepository
                                    .countByAnalyst_IdAndActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                            analystId,
                                            FraudReviewAction.REJECTED,
                                            range.start(),
                                            range.end()
                                    );
                }

                long completed = approved + rejected;

                double resolutionRate = totalAssigned == 0
                        ? 0.0
                        : (completed * 100.0) / totalAssigned;

                return new FraudAnalystPerformanceResponse(
                        analystId,
                        analyst.getEmail(),
                        totalAssigned,
                        approved,
                        rejected,
                        completed,
                        Math.round(resolutionRate * 100.0) / 100.0
                );
            })
            .toList();
}   

 // Analyst workload

    public List<FraudAnalystWorkloadResponse> getAnalystWorkload() {

        List<UserRole> analysts =
                userRoleRepository.findByRole_Name(FRAUD_ANALYST_ROLE);

        return analysts.stream()
                .map(userRole -> {

                    AppUser analyst = userRole.getUser();

                    UUID analystId = analyst.getId();

                    long totalAssigned =
                            fraudAlertRepository.countByAssignedTo_Id(
                                    analystId
                            );

                    long openAssigned =
                            fraudAlertRepository.countByAssignedTo_IdAndStatus(
                                    analystId,
                                    FraudAlertStatus.OPEN
                            );

                    long underReview =
                            fraudAlertRepository.countByAssignedTo_IdAndStatus(
                                    analystId,
                                    FraudAlertStatus.UNDER_REVIEW
                            );

                    long resolved =
                            fraudAlertRepository.countByAssignedTo_IdAndStatus(
                                    analystId,
                                    FraudAlertStatus.RESOLVED
                            );

                    return new FraudAnalystWorkloadResponse(
                            analystId,
                            analyst.getEmail(),
                            totalAssigned,
                            openAssigned,
                            underReview,
                            resolved
                    );
                })
                .toList();
    }

    // Paginated and filtered fraud alerts

    public FraudDashboardPageResponse getAlerts(
            FraudAlertStatus status,
            Integer minimumRiskScore,
            Integer maximumRiskScore,
            TransactionType transactionType,
            int page,
            int size,
            String sortBy,
            String direction
    ) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }

        if (minimumRiskScore != null && minimumRiskScore < 0) {
            throw new IllegalArgumentException(
                    "Minimum risk score cannot be negative"
            );
        }

        if (maximumRiskScore != null && maximumRiskScore > 100) {
            throw new IllegalArgumentException(
                    "Maximum risk score cannot exceed 100"
            );
        }

        if (minimumRiskScore != null
                && maximumRiskScore != null
                && minimumRiskScore > maximumRiskScore) {

            throw new IllegalArgumentException(
                    "Minimum risk score cannot exceed maximum risk score"
            );
        }

        String sortProperty = switch (
                sortBy == null ? "createdAt" : sortBy
        ) {
            case "createdAt" -> "createdAt";
            case "riskScore" -> "assessment.riskScore";
            case "amount" -> "assessment.transaction.amount";
            default -> throw new IllegalArgumentException(
                    "Unsupported sort field"
            );
        };

        Sort.Direction sortDirection;

        try {
            sortDirection = Sort.Direction.fromString(
                    direction == null ? "DESC" : direction
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Sort direction must be ASC or DESC"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortProperty)
        );

        Specification<FraudAlert> specification =
                Specification.allOf(
                        FraudDashboardSpecification.hasStatus(status),
                        FraudDashboardSpecification.hasMinimumRiskScore(
                                minimumRiskScore
                        ),
                        FraudDashboardSpecification.hasMaximumRiskScore(
                                maximumRiskScore
                        ),
                        FraudDashboardSpecification.hasTransactionType(
                                transactionType
                        )
                );

        Page<FraudAlert> alertPage =
                fraudAlertRepository.findAll(
                        specification,
                        pageable
                );

        List<FraudAlertResponse> alerts = alertPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new FraudDashboardPageResponse(
                alerts,
                alertPage.getNumber(),
                alertPage.getSize(),
                alertPage.getTotalElements(),
                alertPage.getTotalPages(),
                alertPage.isFirst(),
                alertPage.isLast()
        );
    }

    // Entity to response mapping

    private FraudAlertResponse toResponse(FraudAlert alert) {

        var assessment = alert.getAssessment();

        FinancialTransaction transaction =
                assessment.getTransaction();

        AppUser assignedAnalyst = alert.getAssignedTo();

        return new FraudAlertResponse(
                alert.getId(),
                assessment.getId(),
                transaction.getId(),
                transaction.getReferenceNumber(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                assessment.getRiskScore(),
                assessment.getDecision(),
                assessment.getAssessmentReason(),
                alert.getStatus(),
                assignedAnalyst != null ? assignedAnalyst.getId() : null,
                assignedAnalyst != null ? assignedAnalyst.getEmail() : null,
                alert.getAnalystNotes(),
                alert.getCreatedAt(),
                alert.getUpdatedAt(),
                alert.getResolvedAt()
        );
    }
}