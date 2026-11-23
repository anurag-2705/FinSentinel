
package com.finsentinel.account;

import com.finsentinel.account.dto.CreateAccountRequest;
import com.finsentinel.account.entity.Account;
import com.finsentinel.account.entity.AccountStatus;
import com.finsentinel.account.entity.AccountStatusAction;
import com.finsentinel.account.entity.AccountType;
import com.finsentinel.account.repository.AccountRepository;
import com.finsentinel.account.service.AccountService;
import com.finsentinel.user.dto.RegisterRequest;
import com.finsentinel.user.entity.AppUser;
import com.finsentinel.user.entity.UserStatus;
import com.finsentinel.user.repository.UserRepository;
import com.finsentinel.user.service.UserRegistrationService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AccountStatusHistoryIntegrationTests {

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
                "account-test-" + UUID.randomUUID() + "@example.com";

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

    @Test
    void shouldReturnEmptyHistoryForNewAccount() {

        AppUser user = createActiveUser();
        Account account = createAccount(user.getId());

        var history = accountService.getAccountStatusHistory(
                user.getId(),
                account.getId()
        );

        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    void shouldRecordFreezeAndUnfreezeHistory() {

        AppUser user = createActiveUser();
        Account account = createAccount(user.getId());

        accountService.freezeAccount(
                user.getId(),
                account.getId()
        );

        accountService.unfreezeAccount(
                user.getId(),
                account.getId()
        );

        var history = accountService.getAccountStatusHistory(
                user.getId(),
                account.getId()
        );

        assertEquals(2, history.size());

        assertEquals(
                AccountStatusAction.UNFREEZE,
                history.get(0).action()
        );

        assertEquals(
                AccountStatus.FROZEN,
                history.get(0).previousStatus()
        );

        assertEquals(
                AccountStatus.ACTIVE,
                history.get(0).newStatus()
        );

        assertEquals(
                AccountStatusAction.FREEZE,
                history.get(1).action()
        );

        assertEquals(
                AccountStatus.ACTIVE,
                history.get(1).previousStatus()
        );

        assertEquals(
                AccountStatus.FROZEN,
                history.get(1).newStatus()
        );

        assertNotNull(history.get(0).createdAt());
        assertNotNull(history.get(1).createdAt());
    }

    @Test
    void shouldPreventUserFromAccessingAnotherUsersHistory() {

        AppUser firstUser = createActiveUser();
        AppUser secondUser = createActiveUser();

        Account account = createAccount(firstUser.getId());

        accountService.freezeAccount(
                firstUser.getId(),
                account.getId()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> accountService.getAccountStatusHistory(
                        secondUser.getId(),
                        account.getId()
                )
        );
    }
}
