package com.finsentinel.fraud.service;

import com.finsentinel.account.dto.AccountBalanceResponse;
import com.finsentinel.account.entity.Account;
import com.finsentinel.account.entity.AccountStatus;
import com.finsentinel.account.repository.AccountRepository;

import com.finsentinel.beneficiary.entity.BeneficiaryStatus;
import com.finsentinel.beneficiary.repository.BeneficiaryRepository;

import com.finsentinel.fraud.dto.FraudAlertResponse;
import com.finsentinel.fraud.dto.FraudAlertReviewResponse;
import com.finsentinel.fraud.entity.FraudAlert;
import com.finsentinel.fraud.entity.FraudAlertReview;
import com.finsentinel.fraud.entity.FraudAlertStatus;
import com.finsentinel.fraud.entity.FraudDecision;
import com.finsentinel.fraud.entity.FraudReviewAction;
import com.finsentinel.fraud.repository.FraudAlertRepository;
import com.finsentinel.fraud.repository.FraudAlertReviewRepository;

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

    public FraudAlertReviewService(
            FraudAlertRepository fraudAlertRepository,
            FraudAlertReviewRepository reviewRepository,
            FinancialTransactionRepository transactionRepository,
            AccountRepository accountRepository,
            LedgerEntryRepository ledgerEntryRepository,
            LedgerPostingService ledgerPostingService,
            UserRepository userRepository,
            BeneficiaryRepository beneficiaryRepository,
            AccountBalanceService accountBalanceService
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
                alert.getResolvedAt()
        );
    }
}