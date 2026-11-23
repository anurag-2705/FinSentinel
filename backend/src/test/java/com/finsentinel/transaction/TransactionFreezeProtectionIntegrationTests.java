package com.finsentinel.transaction;

import com.finsentinel.account.dto.CreateAccountRequest;
import com.finsentinel.account.entity.Account;
import com.finsentinel.account.entity.AccountType;
import com.finsentinel.account.repository.AccountRepository;
import com.finsentinel.account.service.AccountService;

import com.finsentinel.beneficiary.dto.AddBeneficiaryRequest;
import com.finsentinel.beneficiary.dto.BeneficiaryResponse;
import com.finsentinel.beneficiary.service.BeneficiaryService;

import com.finsentinel.transaction.dto.DepositRequest;
import com.finsentinel.transaction.dto.WithdrawalRequest;
import com.finsentinel.transaction.dto.TransferRequest;
import com.finsentinel.transaction.dto.BeneficiaryTransferRequest;

import com.finsentinel.transaction.service.DepositService;
import com.finsentinel.transaction.service.WithdrawalService;
import com.finsentinel.transaction.service.TransferService;
import com.finsentinel.transaction.service.BeneficiaryTransferService;

import com.finsentinel.user.dto.RegisterRequest;
import com.finsentinel.user.entity.AppUser;
import com.finsentinel.user.entity.UserStatus;
import com.finsentinel.user.repository.UserRepository;
import com.finsentinel.user.service.UserRegistrationService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TransactionFreezeProtectionIntegrationTests {

    @Autowired
    private DepositService depositService;

    @Autowired
    private WithdrawalService withdrawalService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private BeneficiaryTransferService beneficiaryTransferService;

    @Autowired
    private BeneficiaryService beneficiaryService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRegistrationService registrationService;

    private AppUser createActiveUser() {

        String email =
                "transaction-test-" + UUID.randomUUID() + "@example.com";

        RegisterRequest request = new RegisterRequest(
                "Test",
                "Customer",
                email,
                null,
                "StrongPassword123!"
        );

        AppUser user = registrationService.register(request);

        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);

        return userRepository.saveAndFlush(user);
    }

    private Account createAccount(UUID userId) {

        accountService.createAccount(
                userId,
                new CreateAccountRequest(AccountType.SAVINGS)
        );

        return accountRepository.findByUser_Id(userId)
                .stream()
                .findFirst()
                .orElseThrow();
    }

    private Account createAnotherAccount(
            UUID userId,
            UUID existingAccountId
    ) {

        accountService.createAccount(
                userId,
                new CreateAccountRequest(AccountType.SAVINGS)
        );

        return accountRepository.findByUser_Id(userId)
                .stream()
                .filter(account ->
                        !account.getId().equals(existingAccountId))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void shouldRejectDepositWhenAccountIsFrozen() {

        AppUser user = createActiveUser();
        Account account = createAccount(user.getId());

        accountService.freezeAccount(
                user.getId(),
                account.getId()
        );

        DepositRequest request = new DepositRequest(
                account.getId(),
                new BigDecimal("1000.00"),
                "Test deposit"
        );

        assertThrows(
                IllegalStateException.class,
                () -> depositService.deposit(
                        user.getId(),
                        request,
                        "frozen-deposit-" + UUID.randomUUID()
                )
        );
    }

    @Test
    void shouldRejectWithdrawalWhenAccountIsFrozen() {

        AppUser user = createActiveUser();
        Account account = createAccount(user.getId());

        accountService.freezeAccount(
                user.getId(),
                account.getId()
        );

        WithdrawalRequest request = new WithdrawalRequest(
                account.getId(),
                new BigDecimal("500.00"),
                "Test withdrawal"
        );

        assertThrows(
                IllegalStateException.class,
                () -> withdrawalService.withdraw(
                        user.getId(),
                        request,
                        "frozen-withdrawal-" + UUID.randomUUID()
                )
        );
    }

    @Test
    void shouldRejectTransferWhenSourceAccountIsFrozen() {

        AppUser user = createActiveUser();

        Account sourceAccount = createAccount(user.getId());

        Account destinationAccount =
                createAnotherAccount(user.getId(), sourceAccount.getId());

        accountService.freezeAccount(
                user.getId(),
                sourceAccount.getId()
        );

        TransferRequest request = new TransferRequest(
                sourceAccount.getId(),
                destinationAccount.getId(),
                new BigDecimal("100.00"),
                "Test transfer"
        );

        assertThrows(
                IllegalStateException.class,
                () -> transferService.transfer(
                        user.getId(),
                        request,
                        "frozen-source-" + UUID.randomUUID()
                )
        );
    }

    @Test
    void shouldRejectTransferWhenDestinationAccountIsFrozen() {

        AppUser user = createActiveUser();

        Account sourceAccount = createAccount(user.getId());

        Account destinationAccount =
                createAnotherAccount(user.getId(), sourceAccount.getId());

        accountService.freezeAccount(
                user.getId(),
                destinationAccount.getId()
        );

        TransferRequest request = new TransferRequest(
                sourceAccount.getId(),
                destinationAccount.getId(),
                new BigDecimal("100.00"),
                "Test transfer"
        );

        assertThrows(
                IllegalStateException.class,
                () -> transferService.transfer(
                        user.getId(),
                        request,
                        "frozen-destination-" + UUID.randomUUID()
                )
        );
    }

    @Test
    void shouldRejectBeneficiaryTransferWhenSourceAccountIsFrozen() {

        AppUser user = createActiveUser();
        Account account = createAccount(user.getId());

        AddBeneficiaryRequest beneficiaryRequest =
                new AddBeneficiaryRequest(
                        "Test Beneficiary",
                        "Test",
                        "HDFC Bank",
                        "123456789012",
                        "HDFC0001234"
                );

        BeneficiaryResponse beneficiary =
                beneficiaryService.addBeneficiary(
                        user.getId(),
                        beneficiaryRequest
                );

        accountService.freezeAccount(
                user.getId(),
                account.getId()
        );

        BeneficiaryTransferRequest request =
                new BeneficiaryTransferRequest(
                        account.getId(),
                        beneficiary.id(),
                        new BigDecimal("100.00"),
                        "Test beneficiary transfer"
                );

        assertThrows(
                IllegalStateException.class,
                () -> beneficiaryTransferService.transfer(
                        user.getId(),
                        request,
                        "frozen-beneficiary-" + UUID.randomUUID()
                )
        );
    }
}