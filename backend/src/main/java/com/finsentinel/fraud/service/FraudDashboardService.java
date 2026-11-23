package com.finsentinel.fraud.service;

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

    private final FraudAlertRepository fraudAlertRepository;
    private final UserRoleRepository userRoleRepository;

    public FraudDashboardService(
            FraudAlertRepository fraudAlertRepository,
            UserRoleRepository userRoleRepository
    ) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.userRoleRepository = userRoleRepository;
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