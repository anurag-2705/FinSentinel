package com.finsentinel.transaction.service;

import com.finsentinel.account.dto.AccountBalanceResponse;
import com.finsentinel.account.entity.Account;
import com.finsentinel.account.repository.AccountRepository;
import com.finsentinel.transaction.entity.LedgerAccount;
import com.finsentinel.transaction.entity.LedgerEntry;
import com.finsentinel.transaction.entity.LedgerEntryType;
import com.finsentinel.transaction.repository.LedgerAccountRepository;
import com.finsentinel.transaction.repository.LedgerEntryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AccountBalanceService {

    private final AccountRepository accountRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AccountBalanceService(
            AccountRepository accountRepository,
            LedgerAccountRepository ledgerAccountRepository,
            LedgerEntryRepository ledgerEntryRepository
    ) {
        this.accountRepository = accountRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    public AccountBalanceResponse getBalance(
            UUID userId,
            UUID accountId
    ) {

        Account account = accountRepository
                .findByIdAndUser_Id(accountId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found")
                );

        LedgerAccount ledgerAccount = ledgerAccountRepository
                .findByAccount_Id(accountId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Ledger account not found for this account"
                        )
                );

        List<LedgerEntry> entries =
                ledgerEntryRepository
                        .findByLedgerAccount_IdOrderByCreatedAtDesc(
                                ledgerAccount.getId()
                        );

        BigDecimal credits = BigDecimal.ZERO;
        BigDecimal debits = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {

            if (entry.getEntryType() == LedgerEntryType.CREDIT) {
                credits = credits.add(entry.getAmount());
            } else if (entry.getEntryType() == LedgerEntryType.DEBIT) {
                debits = debits.add(entry.getAmount());
            }
        }

        BigDecimal balance = credits.subtract(debits);

        return new AccountBalanceResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getCurrency(),
                balance
        );
    }
}