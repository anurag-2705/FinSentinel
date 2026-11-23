
package com.finsentinel.transaction.service;

import com.finsentinel.account.entity.Account;
import com.finsentinel.account.entity.AccountStatus;
import com.finsentinel.account.repository.AccountRepository;
import com.finsentinel.fraud.dto.FraudAssessmentResponse;
import com.finsentinel.fraud.entity.FraudDecision;
import com.finsentinel.fraud.service.FraudDetectionService;
import com.finsentinel.transaction.dto.DepositRequest;
import com.finsentinel.transaction.dto.TransactionResponse;
import com.finsentinel.transaction.entity.FinancialTransaction;
import com.finsentinel.transaction.entity.TransactionStatus;
import com.finsentinel.transaction.entity.TransactionType;
import com.finsentinel.transaction.repository.FinancialTransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class DepositService {

    private final AccountRepository accountRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final LedgerPostingService ledgerPostingService;
    private final TransactionReferenceGenerator referenceGenerator;
    private final FraudDetectionService fraudDetectionService;

    public DepositService(
            AccountRepository accountRepository,
            FinancialTransactionRepository transactionRepository,
            LedgerPostingService ledgerPostingService,
            TransactionReferenceGenerator referenceGenerator,
            FraudDetectionService fraudDetectionService
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerPostingService = ledgerPostingService;
        this.referenceGenerator = referenceGenerator;
        this.fraudDetectionService = fraudDetectionService;
    }

    @Transactional
    public TransactionResponse deposit(
            UUID userId,
            DepositRequest request,
            String idempotencyKey
    ) {

        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.length() > 100) {

            throw new IllegalArgumentException(
                    "A valid idempotency key is required"
            );
        }

        // Check for an existing request.
        var existingTransaction = transactionRepository
                .findByUser_IdAndIdempotencyKey(
                        userId,
                        idempotencyKey
                );

        if (existingTransaction.isPresent()) {

            FinancialTransaction existing =
                    existingTransaction.get();

            boolean sameRequest =
                    existing.getTransactionType() == TransactionType.DEPOSIT
                    && existing.getAmount().compareTo(request.amount()) == 0
                    && existing.getDestinationAccount() != null
                    && existing.getDestinationAccount().getId()
                            .equals(request.accountId());

            if (!sameRequest) {
                throw new IllegalArgumentException(
                        "Idempotency key was already used for a different request"
                );
            }

            return TransactionResponse.from(existing);
        }

        // Lock the customer's account.
        Account account = accountRepository
                .findByIdAndUser_IdForUpdate(
                        request.accountId(),
                        userId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Account not found"
                        )
                );

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Deposits are not allowed on inactive accounts"
            );
        }

        // Create a pending transaction.
        FinancialTransaction transaction =
                new FinancialTransaction();

        transaction.setUser(account.getUser());
        transaction.setDestinationAccount(account);
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setAmount(request.amount());
        transaction.setCurrency(account.getCurrency());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setReferenceNumber(
                referenceGenerator.generateReference()
        );
        transaction.setDescription(request.description());

        FinancialTransaction savedTransaction =
                transactionRepository.saveAndFlush(transaction);

        // Assess fraud risk BEFORE posting ledger entries.
        FraudAssessmentResponse assessment =
                fraudDetectionService.assessTransaction(
                        savedTransaction.getId()
                );

        // HOLD: keep the transaction pending for analyst review.
        if (assessment.decision() == FraudDecision.HOLD) {
            return TransactionResponse.from(savedTransaction);
        }

        // BLOCK: reject the transaction without ledger posting.
        if (assessment.decision() == FraudDecision.BLOCK) {

            savedTransaction.setStatus(TransactionStatus.FAILED);

            FinancialTransaction failedTransaction =
                    transactionRepository.save(savedTransaction);

            return TransactionResponse.from(failedTransaction);
        }

        // ALLOW: post balanced ledger entries.
        ledgerPostingService.postDeposit(
                savedTransaction,
                account
        );

        // Complete the transaction.
        savedTransaction.setStatus(TransactionStatus.COMPLETED);
        savedTransaction.setCompletedAt(Instant.now());

        FinancialTransaction completedTransaction =
                transactionRepository.save(savedTransaction);

        return TransactionResponse.from(completedTransaction);
    }
}
