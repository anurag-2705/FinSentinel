package com.finsentinel.transaction;

import com.finsentinel.account.dto.AccountBalanceResponse;
import com.finsentinel.account.entity.Account;
import com.finsentinel.account.entity.AccountStatus;
import com.finsentinel.account.repository.AccountRepository;
import com.finsentinel.fraud.dto.FraudAssessmentResponse;
import com.finsentinel.fraud.entity.FraudDecision;
import com.finsentinel.fraud.service.FraudDetectionService;
import com.finsentinel.transaction.dto.TransferRequest;
import com.finsentinel.transaction.entity.FinancialTransaction;
import com.finsentinel.transaction.entity.TransactionStatus;
import com.finsentinel.transaction.service.AccountBalanceService;
import com.finsentinel.transaction.service.LedgerPostingService;
import com.finsentinel.transaction.service.TransactionReferenceGenerator;
import com.finsentinel.transaction.service.TransferService;
import com.finsentinel.transaction.repository.FinancialTransactionRepository;
import com.finsentinel.user.entity.AppUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceFraudTests {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private FinancialTransactionRepository transactionRepository;

    @Mock
    private AccountBalanceService accountBalanceService;

    @Mock
    private LedgerPostingService ledgerPostingService;

    @Mock
    private TransactionReferenceGenerator referenceGenerator;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private Account sourceAccount;

    @Mock
    private Account destinationAccount;

    @Mock
    private AppUser user;

    @InjectMocks
    private TransferService transferService;

    private UUID userId;
    private UUID sourceId;
    private UUID destinationId;

    private TransferRequest request;

    @BeforeEach
    void setUp() {

        userId = UUID.randomUUID();
        sourceId = UUID.randomUUID();
        destinationId = UUID.randomUUID();

        request = new TransferRequest(
                sourceId,
                destinationId,
                new BigDecimal("500.00"),
                "Test transfer"
        );

        when(sourceAccount.getId()).thenReturn(sourceId);
        when(destinationAccount.getId()).thenReturn(destinationId);

        when(sourceAccount.getUser()).thenReturn(user);
        when(sourceAccount.getStatus()).thenReturn(AccountStatus.ACTIVE);
        when(destinationAccount.getStatus()).thenReturn(AccountStatus.ACTIVE);

        when(sourceAccount.getCurrency()).thenReturn("INR");
        when(destinationAccount.getCurrency()).thenReturn("INR");

        when(accountRepository.findByIdAndUser_IdForUpdate(
                any(UUID.class), eq(userId)
        )).thenAnswer(invocation -> {

            UUID accountId = invocation.getArgument(0);

            if (accountId.equals(sourceId)) {
                return Optional.of(sourceAccount);
            }

            if (accountId.equals(destinationId)) {
                return Optional.of(destinationAccount);
            }

            return Optional.empty();
        });

        when(transactionRepository.findByUser_IdAndIdempotencyKey(
                userId, "test-key"
        )).thenReturn(Optional.empty());

        when(accountBalanceService.getBalance(userId, sourceId))
                .thenReturn(new AccountBalanceResponse(
                        sourceId,
                        "FSN123456789",
                        "INR",
                        new BigDecimal("5000.00")
                ));

        when(referenceGenerator.generateReference())
                .thenReturn("FSN-TEST-12345678");

        when(transactionRepository.saveAndFlush(
                any(FinancialTransaction.class)
        )).thenAnswer(invocation -> {

            FinancialTransaction transaction = invocation.getArgument(0);

            ReflectionTestUtils.setField(
                    transaction,
                    "id",
                    UUID.randomUUID()
            );

            return transaction;
        });

        
    }

    private void mockFraudDecision(FraudDecision decision) {

        when(fraudDetectionService.assessTransaction(any(UUID.class)))
                .thenAnswer(invocation -> {

                    UUID transactionId = invocation.getArgument(0);

                    return new FraudAssessmentResponse(
                            UUID.randomUUID(),
                            transactionId,
                            20,
                            decision,
                            "Test fraud assessment",
                            List.of(),
                            Instant.now()
                    );
                });
    }

    @Test
    void shouldCompleteTransferWhenFraudDecisionIsAllow() {

        mockFraudDecision(FraudDecision.ALLOW);
when(transactionRepository.save(
        any(FinancialTransaction.class)
)).thenAnswer(invocation -> invocation.getArgument(0));

        var response = transferService.transfer(
                userId,
                request,
                "test-key"
        );

        assertEquals(
                TransactionStatus.COMPLETED,
                response.status()
        );

        verify(fraudDetectionService, times(1))
                .assessTransaction(any(UUID.class));

        verify(ledgerPostingService, times(1))
                .postTransfer(
                        any(FinancialTransaction.class),
                        eq(sourceAccount),
                        eq(destinationAccount)
                );
    }

    @Test
    void shouldKeepTransferPendingWhenFraudDecisionIsHold() {

        mockFraudDecision(FraudDecision.HOLD);

        var response = transferService.transfer(
                userId,
                request,
                "test-key"
        );

        assertEquals(
                TransactionStatus.PENDING,
                response.status()
        );

        verify(fraudDetectionService, times(1))
                .assessTransaction(any(UUID.class));

        verify(ledgerPostingService, never())
                .postTransfer(
                        any(FinancialTransaction.class),
                        any(Account.class),
                        any(Account.class)
                );
    }

    @Test
    void shouldRejectTransferWhenFraudDecisionIsBlock() {

        mockFraudDecision(FraudDecision.BLOCK);
when(transactionRepository.save(
        any(FinancialTransaction.class)
)).thenAnswer(invocation -> invocation.getArgument(0));

        var response = transferService.transfer(
                userId,
                request,
                "test-key"
        );

        assertEquals(
                TransactionStatus.FAILED,
                response.status()
        );

        verify(fraudDetectionService, times(1))
                .assessTransaction(any(UUID.class));

        verify(ledgerPostingService, never())
                .postTransfer(
                        any(FinancialTransaction.class),
                        any(Account.class),
                        any(Account.class)
                );
    }
}