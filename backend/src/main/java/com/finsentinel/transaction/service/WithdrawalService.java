package com.finsentinel.transaction.service;

import com.finsentinel.account.dto.AccountBalanceResponse;
import com.finsentinel.account.entity.Account;
import com.finsentinel.account.entity.AccountStatus;
import com.finsentinel.account.repository.AccountRepository;

import com.finsentinel.fraud.dto.FraudAssessmentResponse;
import com.finsentinel.fraud.entity.FraudDecision;
import com.finsentinel.fraud.service.FraudDetectionService;

import com.finsentinel.transaction.dto.TransactionResponse;
import com.finsentinel.transaction.dto.WithdrawalRequest;
import com.finsentinel.transaction.entity.FinancialTransaction;
import com.finsentinel.transaction.entity.TransactionStatus;
import com.finsentinel.transaction.entity.TransactionType;
import com.finsentinel.transaction.repository.FinancialTransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class WithdrawalService {

    private final AccountRepository accountRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final AccountBalanceService accountBalanceService;
    private final LedgerPostingService ledgerPostingService;
    private final TransactionReferenceGenerator referenceGenerator;
    private final FraudDetectionService fraudDetectionService;

    public WithdrawalService(
            AccountRepository accountRepository,
            FinancialTransactionRepository transactionRepository,
            AccountBalanceService accountBalanceService,
            LedgerPostingService ledgerPostingService,
            TransactionReferenceGenerator referenceGenerator,
            FraudDetectionService fraudDetectionService
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountBalanceService = accountBalanceService;
        this.ledgerPostingService = ledgerPostingService;
        this.referenceGenerator = referenceGenerator;
        this.fraudDetectionService = fraudDetectionService;
    }

    @Transactional
    public TransactionResponse withdraw(
            UUID userId,
            WithdrawalRequest request,
            String idempotencyKey
    ) {

        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.length() > 100) {

            throw new IllegalArgumentException(
                    "A valid idempotency key is required"
            );
        }

        // Lock the account before processing.
        Account account = accountRepository
                .findByIdAndUser_IdForUpdate(
                        request.accountId(),
                        userId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found")
                );

        // Check idempotency.
        var existingTransaction = transactionRepository
                .findByUser_IdAndIdempotencyKey(userId, idempotencyKey);

        if (existingTransaction.isPresent()) {

            FinancialTransaction existing =
                    existingTransaction.get();

            boolean sameRequest =
                    existing.getTransactionType()
                            == TransactionType.WITHDRAWAL
                    && existing.getAmount()
                            .compareTo(request.amount()) == 0
                    && existing.getSourceAccount() != null
                    && existing.getSourceAccount().getId()
                            .equals(request.accountId());

            if (!sameRequest) {
                throw new IllegalArgumentException(
                        "Idempotency key was already used for a different request"
                );
            }

            return TransactionResponse.from(existing);
        }

        // Validate account status.
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Withdrawals are not allowed on inactive accounts"
            );
        }

        // Check available balance.
        AccountBalanceResponse balanceResponse =
                accountBalanceService.getBalance(
                        userId,
                        account.getId()
                );

        var balance = balanceResponse.availableBalance();

        if (balance.compareTo(request.amount()) < 0) {
            throw new IllegalStateException(
                    "Insufficient account balance"
            );
        }

        // Create pending transaction.
        FinancialTransaction transaction =
                new FinancialTransaction();

        transaction.setUser(account.getUser());
        transaction.setSourceAccount(account);
        transaction.setTransactionType(TransactionType.WITHDRAWAL);
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

        // Assess transaction before posting ledger entries.
        FraudAssessmentResponse assessment =
                fraudDetectionService.assessTransaction(
                        savedTransaction.getId()
                );

        // HOLD: Keep transaction pending for analyst review.
        if (assessment.decision() == FraudDecision.HOLD) {
            return TransactionResponse.from(savedTransaction);
        }

        // BLOCK: Mark transaction as failed.
        if (assessment.decision() == FraudDecision.BLOCK) {

            savedTransaction.setStatus(TransactionStatus.FAILED);

            FinancialTransaction failedTransaction =
                    transactionRepository.save(savedTransaction);

            return TransactionResponse.from(failedTransaction);
        }

        // ALLOW: Post double-entry ledger records.
        ledgerPostingService.postWithdrawal(
                savedTransaction,
                account
        );

        // Complete transaction.
        savedTransaction.setStatus(TransactionStatus.COMPLETED);
        savedTransaction.setCompletedAt(Instant.now());

        FinancialTransaction completedTransaction =
                transactionRepository.save(savedTransaction);

        return TransactionResponse.from(completedTransaction);
    }
}