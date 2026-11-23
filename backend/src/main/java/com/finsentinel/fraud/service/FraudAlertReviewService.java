package com.finsentinel.fraud.service;

import com.finsentinel.account.entity.Account;
import com.finsentinel.account.entity.AccountStatus;
import com.finsentinel.account.repository.AccountRepository;

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

    public FraudAlertReviewService(
            FraudAlertRepository fraudAlertRepository,
            FraudAlertReviewRepository reviewRepository,
            FinancialTransactionRepository transactionRepository,
            AccountRepository accountRepository,
            LedgerEntryRepository ledgerEntryRepository,
            LedgerPostingService ledgerPostingService,
            UserRepository userRepository
    ) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.reviewRepository = reviewRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.ledgerPostingService = ledgerPostingService;
        this.userRepository = userRepository;
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

    // Retrieve the complete review history of a fraud alert.
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
                        new IllegalArgumentException(
                                "Transaction not found"
                        )
                );

        if (transaction.getTransactionType() != TransactionType.DEPOSIT) {
            throw new IllegalStateException(
                    "Only deposit transactions can be approved through this workflow"
            );
        }

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending transactions can be approved"
            );
        }

        if (transaction.getDestinationAccount() == null) {
            throw new IllegalStateException(
                    "Deposit destination account not found"
            );
        }

        Account account = accountRepository
                .findByIdAndUser_IdForUpdate(
                        transaction.getDestinationAccount().getId(),
                        transaction.getUser().getId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer account not found"
                        )
                );

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot approve deposit for an inactive account"
            );
        }

        if (ledgerEntryRepository.existsByTransaction_Id(transaction.getId())) {
            throw new IllegalStateException(
                    "Ledger entries already exist for this transaction"
            );
        }

        // Post balanced ledger entries.
        ledgerPostingService.postDeposit(transaction, account);

        // Complete the financial transaction.
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
                        new IllegalArgumentException(
                                "Transaction not found"
                        )
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

        // Reject without posting any ledger entries.
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