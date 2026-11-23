package com.finsentinel.transaction.service;

import com.finsentinel.account.dto.AccountBalanceResponse;
import com.finsentinel.account.entity.Account;
import com.finsentinel.account.entity.AccountStatus;
import com.finsentinel.account.repository.AccountRepository;
import com.finsentinel.transaction.dto.TransactionResponse;
import com.finsentinel.transaction.dto.TransferRequest;
import com.finsentinel.transaction.entity.FinancialTransaction;
import com.finsentinel.transaction.entity.TransactionStatus;
import com.finsentinel.transaction.entity.TransactionType;
import com.finsentinel.transaction.repository.FinancialTransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final AccountBalanceService accountBalanceService;
    private final LedgerPostingService ledgerPostingService;
    private final TransactionReferenceGenerator referenceGenerator;

    public TransferService(
            AccountRepository accountRepository,
            FinancialTransactionRepository transactionRepository,
            AccountBalanceService accountBalanceService,
            LedgerPostingService ledgerPostingService,
            TransactionReferenceGenerator referenceGenerator) {

        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountBalanceService = accountBalanceService;
        this.ledgerPostingService = ledgerPostingService;
        this.referenceGenerator = referenceGenerator;
    }

    @Transactional
    public TransactionResponse transfer(
            UUID userId,
            TransferRequest request,
            String idempotencyKey) {

        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.length() > 100) {
            throw new IllegalArgumentException(
                    "A valid idempotency key is required");
        }

        UUID sourceId = request.sourceAccountId();
        UUID destinationId = request.destinationAccountId();

        if (sourceId.equals(destinationId)) {
            throw new IllegalArgumentException(
                    "Source and destination accounts must be different");
        }

        // Lock accounts in a consistent order to reduce deadlock risk.
        UUID firstId = sourceId.compareTo(destinationId) < 0
                ? sourceId : destinationId;

        UUID secondId = sourceId.compareTo(destinationId) < 0
                ? destinationId : sourceId;

        Account firstAccount = accountRepository
                .findByIdAndUser_IdForUpdate(firstId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found"));

        Account secondAccount = accountRepository
                .findByIdAndUser_IdForUpdate(secondId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found"));

        Account sourceAccount = sourceId.equals(firstId)
                ? firstAccount : secondAccount;

        Account destinationAccount = destinationId.equals(firstId)
                ? firstAccount : secondAccount;

        // Check whether this request was already processed.
        var existingTransaction = transactionRepository
                .findByUser_IdAndIdempotencyKey(userId, idempotencyKey);

        if (existingTransaction.isPresent()) {

            FinancialTransaction existing = existingTransaction.get();

            boolean sameRequest =
                    existing.getTransactionType() == TransactionType.TRANSFER
                    && existing.getAmount().compareTo(request.amount()) == 0
                    && existing.getSourceAccount() != null
                    && existing.getDestinationAccount() != null
                    && existing.getSourceAccount().getId().equals(sourceId)
                    && existing.getDestinationAccount().getId().equals(destinationId);

            if (!sameRequest) {
                throw new IllegalArgumentException(
                        "Idempotency key was already used for a different request");
            }

            return TransactionResponse.from(existing);
        }

        // Both accounts must be active.
        if (sourceAccount.getStatus() != AccountStatus.ACTIVE
                || destinationAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Transfers require both accounts to be active");
        }

        // Both accounts must use the same currency.
        if (!sourceAccount.getCurrency().equals(destinationAccount.getCurrency())) {
            throw new IllegalArgumentException(
                    "Source and destination currencies must match");
        }

        // Check available balance.
        AccountBalanceResponse balanceResponse =
                accountBalanceService.getBalance(userId, sourceId);

        if (balanceResponse.availableBalance().compareTo(request.amount()) < 0) {
            throw new IllegalStateException(
                    "Insufficient account balance");
        }

        // Create the financial transaction.
        FinancialTransaction transaction = new FinancialTransaction();

        transaction.setUser(sourceAccount.getUser());
        transaction.setSourceAccount(sourceAccount);
        transaction.setDestinationAccount(destinationAccount);
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setAmount(request.amount());
        transaction.setCurrency(sourceAccount.getCurrency());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setReferenceNumber(
                referenceGenerator.generateReference());
        transaction.setDescription(request.description());

        FinancialTransaction savedTransaction =
                transactionRepository.saveAndFlush(transaction);

        // Post balanced debit and credit ledger entries.
        ledgerPostingService.postTransfer(
                savedTransaction,
                sourceAccount,
                destinationAccount);

        savedTransaction.setStatus(TransactionStatus.COMPLETED);
        savedTransaction.setCompletedAt(Instant.now());

        FinancialTransaction completedTransaction =
                transactionRepository.save(savedTransaction);

        return TransactionResponse.from(completedTransaction);
    }
}