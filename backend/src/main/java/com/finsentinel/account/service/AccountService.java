package com.finsentinel.account.service;

import com.finsentinel.account.dto.AccountResponse;
import com.finsentinel.account.dto.CreateAccountRequest;
import com.finsentinel.account.entity.Account;
import com.finsentinel.account.entity.AccountStatus;
import com.finsentinel.account.entity.AccountStatusAction;
import com.finsentinel.account.entity.AccountStatusHistory;
import com.finsentinel.account.repository.AccountRepository;
import com.finsentinel.account.repository.AccountStatusHistoryRepository;
import com.finsentinel.user.entity.AppUser;
import com.finsentinel.user.entity.UserStatus;
import com.finsentinel.user.repository.UserRepository;
import com.finsentinel.transaction.entity.LedgerAccount;
import com.finsentinel.transaction.entity.LedgerAccountCategory;
import com.finsentinel.transaction.repository.LedgerAccountRepository;

import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final AccountStatusHistoryRepository accountStatusHistoryRepository;

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository,
            AccountNumberGenerator accountNumberGenerator,
            LedgerAccountRepository ledgerAccountRepository,
            AccountStatusHistoryRepository accountStatusHistoryRepository
    ) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.accountStatusHistoryRepository = accountStatusHistoryRepository;
    }

    @Transactional
    public AccountResponse createAccount(
            UUID userId,
            CreateAccountRequest request
    ) {

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("User account is not active");
        }

        Account account = new Account();

        account.setUser(user);
        account.setAccountNumber(
                accountNumberGenerator.generateAccountNumber()
        );
        account.setAccountType(request.accountType());
        account.setStatus(AccountStatus.ACTIVE);
        account.setCurrency("INR");

        Account savedAccount = accountRepository.saveAndFlush(account);

        LedgerAccount ledgerAccount = new LedgerAccount();

        ledgerAccount.setAccountCode(
                "CUSTOMER-" + savedAccount.getAccountNumber()
        );

        ledgerAccount.setAccountName("Customer Deposit Account");
        ledgerAccount.setAccountCategory(LedgerAccountCategory.LIABILITY);
        ledgerAccount.setAccount(savedAccount);

        ledgerAccountRepository.save(ledgerAccount);

        return AccountResponse.from(savedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(UUID userId) {

        return accountRepository.findByUser_Id(userId)
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(
            UUID userId,
            UUID accountId
    ) {

        Account account = accountRepository
                .findByIdAndUser_Id(accountId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found"));

        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse freezeAccount(
            UUID userId,
            UUID accountId
    ) {

        Account account = accountRepository
                .findByIdAndUser_Id(accountId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found"));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only active accounts can be frozen");
        }

        account.setStatus(AccountStatus.FROZEN);

        Account savedAccount = accountRepository.save(account);

        recordStatusChange(
                accountId,
                userId,
                AccountStatusAction.FREEZE,
                AccountStatus.ACTIVE,
                AccountStatus.FROZEN
        );

        return AccountResponse.from(savedAccount);
    }

    @Transactional
    public AccountResponse unfreezeAccount(
            UUID userId,
            UUID accountId
    ) {

        Account account = accountRepository
                .findByIdAndUser_Id(accountId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found"));

        if (account.getStatus() != AccountStatus.FROZEN) {
            throw new IllegalStateException(
                    "Only frozen accounts can be unfrozen");
        }

        account.setStatus(AccountStatus.ACTIVE);

        Account savedAccount = accountRepository.save(account);

        recordStatusChange(
                accountId,
                userId,
                AccountStatusAction.UNFREEZE,
                AccountStatus.FROZEN,
                AccountStatus.ACTIVE
        );

        return AccountResponse.from(savedAccount);
    }

    private void recordStatusChange(
            UUID accountId,
            UUID userId,
            AccountStatusAction action,
            AccountStatus previousStatus,
            AccountStatus newStatus
    ) {

        AccountStatusHistory history = new AccountStatusHistory();

        history.setAccountId(accountId);
        history.setPerformedByUserId(userId);
        history.setAction(action);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);

        accountStatusHistoryRepository.save(history);
    }
}