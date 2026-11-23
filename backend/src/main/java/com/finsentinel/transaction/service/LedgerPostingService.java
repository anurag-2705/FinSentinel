package com.finsentinel.transaction.service;

import com.finsentinel.account.entity.Account;
import com.finsentinel.transaction.entity.FinancialTransaction;
import com.finsentinel.transaction.entity.LedgerAccount;
import com.finsentinel.transaction.entity.LedgerEntry;
import com.finsentinel.transaction.entity.LedgerEntryType;
import com.finsentinel.transaction.repository.LedgerAccountRepository;
import com.finsentinel.transaction.repository.LedgerEntryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerPostingService {

    private static final String BANK_CASH_ACCOUNT = "BANK_CASH";

    private final LedgerAccountRepository ledgerAccountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerPostingService(
            LedgerAccountRepository ledgerAccountRepository,
            LedgerEntryRepository ledgerEntryRepository
    ) {
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public void postDeposit(
            FinancialTransaction transaction,
            Account customerAccount
    ) {
        LedgerAccount bankCash = getBankCashAccount();
        LedgerAccount customerLedger = getCustomerLedgerAccount(customerAccount);

        createEntry(transaction, bankCash, LedgerEntryType.DEBIT);
        createEntry(transaction, customerLedger, LedgerEntryType.CREDIT);
    }

    @Transactional
    public void postWithdrawal(
            FinancialTransaction transaction,
            Account customerAccount
    ) {
        LedgerAccount bankCash = getBankCashAccount();
        LedgerAccount customerLedger = getCustomerLedgerAccount(customerAccount);

        createEntry(transaction, customerLedger, LedgerEntryType.DEBIT);
        createEntry(transaction, bankCash, LedgerEntryType.CREDIT);
    }

    @Transactional
    public void postTransfer(
            FinancialTransaction transaction,
            Account sourceAccount,
            Account destinationAccount
    ) {
        LedgerAccount sourceLedger = getCustomerLedgerAccount(sourceAccount);
        LedgerAccount destinationLedger = getCustomerLedgerAccount(destinationAccount);

        createEntry(transaction, sourceLedger, LedgerEntryType.DEBIT);
        createEntry(transaction, destinationLedger, LedgerEntryType.CREDIT);
    }

    @Transactional
    public void postBeneficiaryTransfer(
            FinancialTransaction transaction,
            Account sourceAccount
    ) {
        LedgerAccount bankCash = getBankCashAccount();
        LedgerAccount customerLedger = getCustomerLedgerAccount(sourceAccount);

        createEntry(transaction, customerLedger, LedgerEntryType.DEBIT);
        createEntry(transaction, bankCash, LedgerEntryType.CREDIT);
    }

    private LedgerAccount getBankCashAccount() {
        return ledgerAccountRepository
                .findByAccountCode(BANK_CASH_ACCOUNT)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Bank cash ledger account not found"
                        )
                );
    }

    private LedgerAccount getCustomerLedgerAccount(Account account) {
        return ledgerAccountRepository
                .findByAccount_Id(account.getId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Customer ledger account not found"
                        )
                );
    }

    private void createEntry(
            FinancialTransaction transaction,
            LedgerAccount ledgerAccount,
            LedgerEntryType entryType
    ) {
        LedgerEntry entry = new LedgerEntry();

        entry.setTransaction(transaction);
        entry.setLedgerAccount(ledgerAccount);
        entry.setEntryType(entryType);
        entry.setAmount(transaction.getAmount());
        entry.setCurrency(transaction.getCurrency());

        ledgerEntryRepository.save(entry);
    }
}