package com.finsentinel.transaction;

import com.finsentinel.account.dto.AccountBalanceResponse;
import com.finsentinel.account.entity.Account;
import com.finsentinel.account.entity.AccountStatus;
import com.finsentinel.account.repository.AccountRepository;
import com.finsentinel.beneficiary.entity.Beneficiary;
import com.finsentinel.beneficiary.entity.BeneficiaryStatus;
import com.finsentinel.beneficiary.repository.BeneficiaryRepository;
import com.finsentinel.fraud.dto.FraudAssessmentResponse;
import com.finsentinel.fraud.entity.FraudDecision;
import com.finsentinel.fraud.service.FraudDetectionService;
import com.finsentinel.transaction.dto.BeneficiaryTransferRequest;
import com.finsentinel.transaction.entity.FinancialTransaction;
import com.finsentinel.transaction.entity.TransactionStatus;
import com.finsentinel.transaction.service.AccountBalanceService;
import com.finsentinel.transaction.service.BeneficiaryTransferService;
import com.finsentinel.transaction.service.LedgerPostingService;
import com.finsentinel.transaction.service.TransactionReferenceGenerator;
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
class BeneficiaryTransferServiceFraudTests {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

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
    private Beneficiary beneficiary;

    @Mock
    private AppUser user;

    @InjectMocks
    private BeneficiaryTransferService transferService;

    private UUID userId;
    private UUID sourceId;
    private UUID beneficiaryId;

    private BeneficiaryTransferRequest request;

    @BeforeEach
    void setUp() {

        userId = UUID.randomUUID();
        sourceId = UUID.randomUUID();
        beneficiaryId = UUID.randomUUID();

        request = new BeneficiaryTransferRequest(
                sourceId,
                beneficiaryId,
                new BigDecimal("500.00"),
                "Test beneficiary transfer"
        );

        when(sourceAccount.getId()).thenReturn(sourceId);
        when(sourceAccount.getUser()).thenReturn(user);
        when(sourceAccount.getStatus()).thenReturn(AccountStatus.ACTIVE);
        when(sourceAccount.getCurrency()).thenReturn("INR");

        when(accountRepository.findByIdAndUser_IdForUpdate(
                sourceId, userId
        )).thenReturn(Optional.of(sourceAccount));

        when(transactionRepository.findByUser_IdAndIdempotencyKey(
                userId, "test-key"
        )).thenReturn(Optional.empty());

        when(beneficiaryRepository.findByIdAndUser_IdAndStatus(
                beneficiaryId,
                userId,
                BeneficiaryStatus.ACTIVE
        )).thenReturn(Optional.of(beneficiary));

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
                .postBeneficiaryTransfer(
                        any(FinancialTransaction.class),
                        eq(sourceAccount)
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
                .postBeneficiaryTransfer(
                        any(FinancialTransaction.class),
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
                .postBeneficiaryTransfer(
                        any(FinancialTransaction.class),
                        any(Account.class)
                );
    }
}