package com.finsentinel.fraud.service;

import com.finsentinel.account.dto.AccountBalanceResponse;
import com.finsentinel.account.entity.Account;
import com.finsentinel.account.entity.AccountStatus;
import com.finsentinel.account.repository.AccountRepository;

import com.finsentinel.beneficiary.entity.BeneficiaryStatus;
import com.finsentinel.beneficiary.repository.BeneficiaryRepository;

import com.finsentinel.fraud.dto.FraudBulkAssignmentResponse;
import com.finsentinel.fraud.dto.FraudAlertResponse;
import com.finsentinel.fraud.dto.FraudAlertReviewResponse;
import com.finsentinel.fraud.entity.FraudAlert;
import com.finsentinel.fraud.entity.FraudAlertReview;
import com.finsentinel.fraud.entity.FraudAlertStatus;
import com.finsentinel.fraud.entity.FraudDecision;
import com.finsentinel.fraud.entity.FraudReviewAction;
import com.finsentinel.fraud.repository.FraudAlertRepository;
import com.finsentinel.fraud.repository.FraudAlertReviewRepository;
import com.finsentinel.role.repository.UserRoleRepository;
import com.finsentinel.role.entity.UserRole;

import com.finsentinel.transaction.entity.FinancialTransaction;
import com.finsentinel.transaction.entity.TransactionStatus;
import com.finsentinel.transaction.entity.TransactionType;
import com.finsentinel.transaction.repository.FinancialTransactionRepository;
import com.finsentinel.transaction.repository.LedgerEntryRepository;
import com.finsentinel.transaction.service.AccountBalanceService;
import com.finsentinel.transaction.service.LedgerPostingService;

import com.finsentinel.user.entity.AppUser;
import com.finsentinel.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class FraudAlertReviewService {

    private final FraudAlertRepository fraudAlertRepository;
    private final FraudAlertReviewRepository reviewRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerPostingService ledgerPostingService;
    private final UserRepository userRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountBalanceService accountBalanceService;
private final UserRoleRepository userRoleRepository;

    public FraudAlertReviewService(
        FraudAlertRepository fraudAlertRepository,
        FraudAlertReviewRepository reviewRepository,
        FinancialTransactionRepository transactionRepository,
        AccountRepository accountRepository,
        LedgerEntryRepository ledgerEntryRepository,
        LedgerPostingService ledgerPostingService,
        UserRepository userRepository,
        BeneficiaryRepository beneficiaryRepository,
        AccountBalanceService accountBalanceService,
        UserRoleRepository userRoleRepository
) {
    this.fraudAlertRepository = fraudAlertRepository;
    this.reviewRepository = reviewRepository;
    this.transactionRepository = transactionRepository;
    this.accountRepository = accountRepository;
    this.ledgerEntryRepository = ledgerEntryRepository;
    this.ledgerPostingService = ledgerPostingService;
    this.userRepository = userRepository;
    this.beneficiaryRepository = beneficiaryRepository;
    this.accountBalanceService = accountBalanceService;
    this.userRoleRepository = userRoleRepository;
}

    @Transactional(readOnly = true)
    public List<FraudAlertResponse> getAlerts(FraudAlertStatus status) {

        List<FraudAlert> alerts = status == null
                ? fraudAlertRepository.findAllByOrderByCreatedAtDesc()
                : fraudAlertRepository.findByStatusOrderByCreatedAtDesc(status);

        return alerts.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FraudAlertResponse getAlert(UUID alertId) {

        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Fraud alert not found")
                );

        return toResponse(alert);
    }

    @Transactional(readOnly = true)
    public List<FraudAlertReviewResponse> getReviewHistory(UUID alertId) {

        if (!fraudAlertRepository.existsById(alertId)) {
            throw new IllegalArgumentException("Fraud alert not found");
        }

        List<FraudAlertReview> reviews =
                reviewRepository.findByAlert_IdOrderByCreatedAtDesc(alertId);

        return reviews.stream()
                .map(review -> new FraudAlertReviewResponse(
                        review.getId(),
                        review.getAction(),
                        review.getAnalyst().getId(),
                        review.getAnalyst().getEmail(),
                        review.getNotes(),
                        review.getCreatedAt()
                ))
                .toList();
    }

@Transactional
public FraudAlertResponse autoAssignAlert(UUID alertId) {

    // Serialize concurrent automatic assignment decisions.
    userRoleRepository.acquireAutomaticAssignmentLock();

    FraudAlert alert = fraudAlertRepository.findByIdForUpdate(alertId)
            .orElseThrow(() ->
                    new IllegalArgumentException("Fraud alert not found")
            );

    if (alert.getStatus() != FraudAlertStatus.OPEN) {
        throw new IllegalStateException(
                "Only OPEN alerts can be automatically assigned"
        );
    }

    if (alert.getAssignedTo() != null) {
        throw new IllegalStateException(
                "Alert is already assigned"
        );
    }

    List<UserRole> analystRoles =
            userRoleRepository.findByRole_Name("FRAUD_ANALYST");

    if (analystRoles.isEmpty()) {
        throw new IllegalStateException(
                "No fraud analysts are available"
        );
    }

    AppUser selectedAnalyst = null;
    long lowestWorkload = Long.MAX_VALUE;

    for (UserRole userRole : analystRoles) {

        AppUser analyst = userRole.getUser();

        long openAlerts =
                fraudAlertRepository.countByAssignedTo_IdAndStatus(
                        analyst.getId(),
                        FraudAlertStatus.OPEN
                );

        long underReviewAlerts =
                fraudAlertRepository.countByAssignedTo_IdAndStatus(
                        analyst.getId(),
                        FraudAlertStatus.UNDER_REVIEW
                );

        long workloadScore = openAlerts + (underReviewAlerts * 2);

        if (workloadScore < lowestWorkload) {
            lowestWorkload = workloadScore;
            selectedAnalyst = analyst;
        }
    }

    if (selectedAnalyst == null) {
        throw new IllegalStateException(
                "Unable to select a fraud analyst"
        );
    }

    alert.setAssignedTo(selectedAnalyst);
    alert.setStatus(FraudAlertStatus.UNDER_REVIEW);

    FraudAlert savedAlert = fraudAlertRepository.save(alert);

    recordReview(
            savedAlert,
            selectedAnalyst,
            FraudReviewAction.ASSIGNED,
            "Automatically assigned based on analyst workload balancing. Workload score: "
                    + lowestWorkload
    );

    return toResponse(savedAlert);
}

@Transactional
public FraudBulkAssignmentResponse bulkAutoAssignAlerts() {

    // Serialize bulk and individual automatic assignment decisions.
    userRoleRepository.acquireAutomaticAssignmentLock();

    List<FraudAlert> unassignedAlerts =
            fraudAlertRepository
                    .findByStatusAndAssignedToIsNullOrderByCreatedAtAsc(
                            FraudAlertStatus.OPEN
                    );

    if (unassignedAlerts.isEmpty()) {
        return new FraudBulkAssignmentResponse(
                0,
                0,
                0,
                List.of("No eligible unassigned OPEN alerts found.")
        );
    }

    List<UserRole> analystRoles =
            userRoleRepository.findByRole_Name("FRAUD_ANALYST");

    if (analystRoles.isEmpty()) {
        throw new IllegalStateException(
                "No fraud analysts are available"
        );
    }

    // Track workloads to balance assignments within this batch.
    java.util.Map<UUID, Long> workloads = new java.util.HashMap<>();

    java.util.Map<UUID, AppUser> analysts = new java.util.HashMap<>();

    for (UserRole userRole : analystRoles) {

        AppUser analyst = userRole.getUser();

        long openAlerts =
                fraudAlertRepository.countByAssignedTo_IdAndStatus(
                        analyst.getId(),
                        FraudAlertStatus.OPEN
                );

        long underReviewAlerts =
                fraudAlertRepository.countByAssignedTo_IdAndStatus(
                        analyst.getId(),
                        FraudAlertStatus.UNDER_REVIEW
                );

        workloads.put(
                analyst.getId(),
                openAlerts + (underReviewAlerts * 2)
        );

        analysts.put(
                analyst.getId(),
                analyst
        );
    }

    int successfulAssignments = 0;
    int failedAssignments = 0;

    List<String> messages = new java.util.ArrayList<>();

    for (FraudAlert candidate : unassignedAlerts) {

        try {

            FraudAlert alert =
                    fraudAlertRepository.findByIdForUpdate(candidate.getId())
                            .orElse(null);

            if (alert == null
                    || alert.getStatus() != FraudAlertStatus.OPEN
                    || alert.getAssignedTo() != null) {

                failedAssignments++;

                messages.add(
                        "Skipped alert " + candidate.getId()
                                + ": no longer eligible."
                );

                continue;
            }

            UUID selectedAnalystId = null;
            long lowestWorkload = Long.MAX_VALUE;

            for (var entry : workloads.entrySet()) {

                if (entry.getValue() < lowestWorkload) {

                    lowestWorkload = entry.getValue();
                    selectedAnalystId = entry.getKey();
                }
            }

            if (selectedAnalystId == null) {

                failedAssignments++;

                messages.add(
                        "Failed alert " + alert.getId()
                                + ": unable to select analyst."
                );

                continue;
            }

            AppUser selectedAnalyst =
                    analysts.get(selectedAnalystId);

            alert.setAssignedTo(selectedAnalyst);
            alert.setStatus(FraudAlertStatus.UNDER_REVIEW);

            FraudAlert savedAlert =
                    fraudAlertRepository.save(alert);

            recordReview(
                    savedAlert,
                    selectedAnalyst,
                    FraudReviewAction.ASSIGNED,
                    "Bulk automatically assigned based on analyst workload balancing. "
                            + "Workload score: " + lowestWorkload
            );

            // Increment workload immediately so the next alert
            // considers this assignment.
            workloads.put(
                    selectedAnalystId,
                    lowestWorkload + 2
            );

            successfulAssignments++;

            messages.add(
                    "Assigned alert " + alert.getId()
                            + " to " + selectedAnalyst.getEmail()
                            + " (workload score: " + lowestWorkload + ")."
            );

        } catch (Exception exception) {

            failedAssignments++;

            messages.add(
                    "Failed alert " + candidate.getId()
                            + ": " + exception.getMessage()
            );
        }
    }

    return new FraudBulkAssignmentResponse(
            unassignedAlerts.size(),
            successfulAssignments,
            failedAssignments,
            messages
    );
}

    @Transactional
    public FraudAlertResponse assignAlert(
            UUID alertId,
            UUID analystId
    ) {

        FraudAlert alert = fraudAlertRepository.findByIdForUpdate(alertId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Fraud alert not found")
                );

        AppUser analyst = getAnalyst(analystId);

        if (alert.getStatus() != FraudAlertStatus.OPEN
                && alert.getStatus() != FraudAlertStatus.UNDER_REVIEW) {

            throw new IllegalStateException(
                    "This fraud alert is no longer available for assignment"
            );
        }

        if (alert.getAssignedTo() != null) {

            if (!alert.getAssignedTo().getId().equals(analystId)) {
                throw new IllegalStateException(
                        "This alert is already assigned to another analyst"
                );
            }

            return toResponse(alert);
        }

        alert.setAssignedTo(analyst);
        alert.setStatus(FraudAlertStatus.UNDER_REVIEW);

        FraudAlert savedAlert = fraudAlertRepository.save(alert);

        recordReview(
                savedAlert,
                analyst,
                FraudReviewAction.ASSIGNED,
                "Alert assigned to analyst"
        );

        return toResponse(savedAlert);
    }

    @Transactional
    public FraudAlertResponse approveAlert(
            UUID alertId,
            UUID analystId,
            String notes
    ) {

        FraudAlert alert = fraudAlertRepository.findByIdForUpdate(alertId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Fraud alert not found")
                );

        AppUser analyst = getAnalyst(analystId);

        validateAssignedAnalyst(alert, analystId);

        if (alert.getStatus() != FraudAlertStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "Alert must be under review before approval"
            );
        }

        if (alert.getAssessment().getDecision() != FraudDecision.HOLD) {
            throw new IllegalStateException(
                    "Only HOLD transactions can be approved"
            );
        }

        FinancialTransaction transaction =
                transactionRepository.findByIdForUpdate(
                        alert.getAssessment().getTransaction().getId()
                ).orElseThrow(() ->
                        new IllegalArgumentException("Transaction not found")
                );

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending transactions can be approved"
            );
        }

        if (ledgerEntryRepository.existsByTransaction_Id(transaction.getId())) {
            throw new IllegalStateException(
                    "Ledger entries already exist for this transaction"
            );
        }

        UUID userId = transaction.getUser().getId();

        switch (transaction.getTransactionType()) {

            case DEPOSIT -> {

                if (transaction.getDestinationAccount() == null) {
                    throw new IllegalStateException(
                            "Deposit destination account not found"
                    );
                }

                Account account = accountRepository.findByIdAndUser_IdForUpdate(
                        transaction.getDestinationAccount().getId(),
                        userId
                ).orElseThrow(() ->
                        new IllegalArgumentException("Customer account not found")
                );

                if (account.getStatus() != AccountStatus.ACTIVE) {
                    throw new IllegalStateException(
                            "Cannot approve deposit for an inactive account"
                    );
                }

                ledgerPostingService.postDeposit(transaction, account);
            }

            case WITHDRAWAL -> {

                if (transaction.getSourceAccount() == null) {
                    throw new IllegalStateException(
                            "Withdrawal source account not found"
                    );
                }

                Account account = accountRepository.findByIdAndUser_IdForUpdate(
                        transaction.getSourceAccount().getId(),
                        userId
                ).orElseThrow(() ->
                        new IllegalArgumentException("Customer account not found")
                );

                if (account.getStatus() != AccountStatus.ACTIVE) {
                    throw new IllegalStateException(
                            "Cannot approve withdrawal from an inactive account"
                    );
                }

                validateSufficientBalance(
                        userId,
                        account,
                        transaction
                );

                ledgerPostingService.postWithdrawal(transaction, account);
            }

            case TRANSFER -> {

                if (transaction.getSourceAccount() == null
                        || transaction.getDestinationAccount() == null) {

                    throw new IllegalStateException(
                            "Transfer accounts not found"
                    );
                }

                UUID sourceId = transaction.getSourceAccount().getId();
                UUID destinationId = transaction.getDestinationAccount().getId();

                if (sourceId.equals(destinationId)) {
                    throw new IllegalStateException(
                            "Source and destination accounts must be different"
                    );
                }

                // Lock accounts in consistent UUID order.
                UUID firstId = sourceId.compareTo(destinationId) < 0
                        ? sourceId : destinationId;

                UUID secondId = sourceId.compareTo(destinationId) < 0
                        ? destinationId : sourceId;

                Account firstAccount =
                        accountRepository.findByIdAndUser_IdForUpdate(
                                firstId,
                                userId
                        ).orElseThrow(() ->
                                new IllegalArgumentException("Account not found")
                        );

                Account secondAccount =
                        accountRepository.findByIdAndUser_IdForUpdate(
                                secondId,
                                userId
                        ).orElseThrow(() ->
                                new IllegalArgumentException("Account not found")
                        );

                Account sourceAccount = sourceId.equals(firstId)
                        ? firstAccount : secondAccount;

                Account destinationAccount = destinationId.equals(firstId)
                        ? firstAccount : secondAccount;

                if (sourceAccount.getStatus() != AccountStatus.ACTIVE
                        || destinationAccount.getStatus() != AccountStatus.ACTIVE) {

                    throw new IllegalStateException(
                            "Both transfer accounts must be active"
                    );
                }

                if (!sourceAccount.getCurrency().equals(
                        destinationAccount.getCurrency())) {

                    throw new IllegalStateException(
                            "Transfer account currencies must match"
                    );
                }

                if (!sourceAccount.getCurrency().equals(
                        transaction.getCurrency())) {

                    throw new IllegalStateException(
                            "Transaction currency does not match account currency"
                    );
                }

                validateSufficientBalance(
                        userId,
                        sourceAccount,
                        transaction
                );

                ledgerPostingService.postTransfer(
                        transaction,
                        sourceAccount,
                        destinationAccount
                );
            }

            case BENEFICIARY_TRANSFER -> {

                if (transaction.getSourceAccount() == null
                        || transaction.getBeneficiary() == null) {

                    throw new IllegalStateException(
                            "Beneficiary transfer details are missing"
                    );
                }

                Account sourceAccount =
                        accountRepository.findByIdAndUser_IdForUpdate(
                                transaction.getSourceAccount().getId(),
                                userId
                        ).orElseThrow(() ->
                                new IllegalArgumentException("Source account not found")
                        );

                if (sourceAccount.getStatus() != AccountStatus.ACTIVE) {
                    throw new IllegalStateException(
                            "Source account is inactive"
                    );
                }

                if (!"INR".equals(sourceAccount.getCurrency())) {
                    throw new IllegalStateException(
                            "Only INR beneficiary transfers are supported"
                    );
                }

                beneficiaryRepository.findByIdAndUser_IdAndStatus(
                        transaction.getBeneficiary().getId(),
                        userId,
                        BeneficiaryStatus.ACTIVE
                ).orElseThrow(() ->
                        new IllegalStateException("Active beneficiary not found")
                );

                validateSufficientBalance(
                        userId,
                        sourceAccount,
                        transaction
                );

                ledgerPostingService.postBeneficiaryTransfer(
                        transaction,
                        sourceAccount
                );
            }
        }

        // Complete the transaction after successful ledger posting.
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(Instant.now());

        transactionRepository.save(transaction);

        // Resolve the fraud alert.
        alert.setStatus(FraudAlertStatus.RESOLVED);
        alert.setAnalystNotes(notes);
        alert.setResolvedAt(Instant.now());

        FraudAlert savedAlert = fraudAlertRepository.save(alert);

        recordReview(
                savedAlert,
                analyst,
                FraudReviewAction.APPROVED,
                notes
        );

        return toResponse(savedAlert);
    }

    @Transactional
    public FraudAlertResponse rejectAlert(
            UUID alertId,
            UUID analystId,
            String notes
    ) {

        FraudAlert alert = fraudAlertRepository.findByIdForUpdate(alertId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Fraud alert not found")
                );

        AppUser analyst = getAnalyst(analystId);

        validateAssignedAnalyst(alert, analystId);

        if (alert.getStatus() != FraudAlertStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "Alert must be under review before rejection"
            );
        }

        if (alert.getAssessment().getDecision() != FraudDecision.HOLD) {
            throw new IllegalStateException(
                    "Only HOLD transactions can be rejected through this workflow"
            );
        }

        FinancialTransaction transaction =
                transactionRepository.findByIdForUpdate(
                        alert.getAssessment().getTransaction().getId()
                ).orElseThrow(() ->
                        new IllegalArgumentException("Transaction not found")
                );

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending transactions can be rejected"
            );
        }

        if (ledgerEntryRepository.existsByTransaction_Id(transaction.getId())) {
            throw new IllegalStateException(
                    "Transaction already has ledger entries and cannot be rejected"
            );
        }

        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(transaction);

        alert.setStatus(FraudAlertStatus.RESOLVED);
        alert.setAnalystNotes(notes);
        alert.setResolvedAt(Instant.now());

        FraudAlert savedAlert = fraudAlertRepository.save(alert);

        recordReview(
                savedAlert,
                analyst,
                FraudReviewAction.REJECTED,
                notes
        );

        return toResponse(savedAlert);
    }

@Transactional
public FraudAlertResponse confirmBlockedAlert(
        UUID alertId,
        UUID analystId,
        String notes
) {

    FraudAlert alert = fraudAlertRepository.findByIdForUpdate(alertId)
            .orElseThrow(() ->
                    new IllegalArgumentException("Fraud alert not found")
            );

    AppUser analyst = getAnalyst(analystId);

    validateAssignedAnalyst(alert, analystId);

    if (alert.getStatus() != FraudAlertStatus.UNDER_REVIEW) {
        throw new IllegalStateException(
                "Alert must be under review before confirmation"
        );
    }

    if (alert.getAssessment().getDecision() != FraudDecision.BLOCK) {
        throw new IllegalStateException(
                "Only BLOCK transactions can use this workflow"
        );
    }

    FinancialTransaction transaction =
            transactionRepository.findByIdForUpdate(
                    alert.getAssessment().getTransaction().getId()
            ).orElseThrow(() ->
                    new IllegalArgumentException("Transaction not found")
            );

    if (transaction.getStatus() != TransactionStatus.FAILED) {
        throw new IllegalStateException(
                "Blocked transaction must have FAILED status"
        );
    }

    if (ledgerEntryRepository.existsByTransaction_Id(transaction.getId())) {
        throw new IllegalStateException(
                "Blocked transaction unexpectedly has ledger entries"
        );
    }

    alert.setStatus(FraudAlertStatus.RESOLVED);
    alert.setAnalystNotes(notes);
    alert.setResolvedAt(Instant.now());

    FraudAlert savedAlert = fraudAlertRepository.save(alert);

    recordReview(
            savedAlert,
            analyst,
            FraudReviewAction.CONFIRMED_BLOCK,
            notes
    );

    return toResponse(savedAlert);
}

    private void validateSufficientBalance(
            UUID userId,
            Account account,
            FinancialTransaction transaction
    ) {

        AccountBalanceResponse balance =
                accountBalanceService.getBalance(userId, account.getId());

        if (balance.availableBalance().compareTo(transaction.getAmount()) < 0) {
            throw new IllegalStateException(
                    "Insufficient balance to approve this transaction"
            );
        }
    }

    private AppUser getAnalyst(UUID analystId) {

        return userRepository.findById(analystId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Analyst not found")
                );
    }

    private void validateAssignedAnalyst(
            FraudAlert alert,
            UUID analystId
    ) {

        if (alert.getAssignedTo() == null
                || !alert.getAssignedTo().getId().equals(analystId)) {

            throw new IllegalStateException(
                    "This alert is not assigned to the current analyst"
            );
        }
    }

    private void recordReview(
            FraudAlert alert,
            AppUser analyst,
            FraudReviewAction action,
            String notes
    ) {

        FraudAlertReview review = new FraudAlertReview();

        review.setAlert(alert);
        review.setAnalyst(analyst);
        review.setAction(action);
        review.setNotes(notes);

        reviewRepository.save(review);
    }

    private FraudAlertResponse toResponse(FraudAlert alert) {

        FinancialTransaction transaction =
                alert.getAssessment().getTransaction();

        AppUser assignedAnalyst = alert.getAssignedTo();

        return new FraudAlertResponse(
                alert.getId(),
                alert.getAssessment().getId(),
                transaction.getId(),
                transaction.getReferenceNumber(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                alert.getAssessment().getRiskScore(),
                alert.getAssessment().getDecision(),
                alert.getAssessment().getAssessmentReason(),
                alert.getStatus(),
                assignedAnalyst != null ? assignedAnalyst.getId() : null,
                assignedAnalyst != null ? assignedAnalyst.getEmail() : null,
                alert.getAnalystNotes(),
                                alert.getCreatedAt(),
                alert.getUpdatedAt(),
                alert.getResolvedAt(),
                alert.getAssessment().getMlFraudProbability(),
                alert.getAssessment().getMlRiskLevel(),
                alert.getAssessment().getMlModelVersion()
        );
    }
}