package com.finsentinel.transaction.service;

import com.finsentinel.account.dto.AccountBalanceResponse;
import com.finsentinel.account.entity.Account;
import com.finsentinel.account.entity.AccountStatus;
import com.finsentinel.account.repository.AccountRepository;
import com.finsentinel.beneficiary.entity.Beneficiary;
import com.finsentinel.beneficiary.entity.BeneficiaryStatus;
import com.finsentinel.beneficiary.repository.BeneficiaryRepository;
import com.finsentinel.transaction.dto.BeneficiaryTransferRequest;
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
public class BeneficiaryTransferService {

    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final AccountBalanceService accountBalanceService;
    private final LedgerPostingService ledgerPostingService;
    private final TransactionReferenceGenerator referenceGenerator;

    public BeneficiaryTransferService(
            AccountRepository accountRepository,
            BeneficiaryRepository beneficiaryRepository,
            FinancialTransactionRepository transactionRepository,
            AccountBalanceService accountBalanceService,
            LedgerPostingService ledgerPostingService,
            TransactionReferenceGenerator referenceGenerator
    ) {
        this.accountRepository = accountRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.transactionRepository = transactionRepository;
        this.accountBalanceService = accountBalanceService;
        this.ledgerPostingService = ledgerPostingService;
        this.referenceGenerator = referenceGenerator;
    }

    @Transactional
    public TransactionResponse transfer(
            UUID userId,
            BeneficiaryTransferRequest request,
            String idempotencyKey
    ) {

        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.length() > 100) {
            throw new IllegalArgumentException("Valid idempotency key is required");
        }

        Account sourceAccount = accountRepository
                .findByIdAndUser_IdForUpdate(request.sourceAccountId(), userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Source account not found")
                );

        var existingTransaction = transactionRepository
                .findByUser_IdAndIdempotencyKey(userId, idempotencyKey);

        if (existingTransaction.isPresent()) {

            FinancialTransaction existing = existingTransaction.get();

            boolean sameRequest =
                    existing.getTransactionType() == TransactionType.BENEFICIARY_TRANSFER
                    && existing.getAmount().compareTo(request.amount()) == 0
                    && existing.getSourceAccount() != null
                    && existing.getSourceAccount().getId()
                            .equals(request.sourceAccountId())
                    && existing.getBeneficiary() != null
                    && existing.getBeneficiary().getId()
                            .equals(request.beneficiaryId());

            if (!sameRequest) {
                throw new IllegalArgumentException(
                        "Idempotency key has already been used for another request"
                );
            }

            return TransactionResponse.from(existing);
        }

        Beneficiary beneficiary = beneficiaryRepository
                .findByIdAndUser_IdAndStatus(
                        request.beneficiaryId(),
                        userId,
                        BeneficiaryStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Active beneficiary not found"
                        )
                );

        if (sourceAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Source account is not active");
        }

        if (!"INR".equals(sourceAccount.getCurrency())) {
            throw new IllegalArgumentException(
                    "Only INR beneficiary transfers are supported"
            );
        }

        AccountBalanceResponse balanceResponse =
                accountBalanceService.getBalance(
                        userId,
                        sourceAccount.getId()
                );

        if (balanceResponse.availableBalance()
                .compareTo(request.amount()) < 0) {
            throw new IllegalStateException("Insufficient account balance");
        }

        FinancialTransaction transaction = new FinancialTransaction();

        transaction.setUser(sourceAccount.getUser());
        transaction.setSourceAccount(sourceAccount);
        transaction.setBeneficiary(beneficiary);
        transaction.setTransactionType(
                TransactionType.BENEFICIARY_TRANSFER
        );
        transaction.setAmount(request.amount());
        transaction.setCurrency(sourceAccount.getCurrency());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setReferenceNumber(
                referenceGenerator.generateReference()
        );
        transaction.setDescription(request.description());

        FinancialTransaction savedTransaction =
                transactionRepository.saveAndFlush(transaction);

        ledgerPostingService.postBeneficiaryTransfer(
                savedTransaction,
                sourceAccount
        );

        savedTransaction.setStatus(TransactionStatus.COMPLETED);
        savedTransaction.setCompletedAt(Instant.now());

        FinancialTransaction completedTransaction =
                transactionRepository.save(savedTransaction);

        return TransactionResponse.from(completedTransaction);
    }
}