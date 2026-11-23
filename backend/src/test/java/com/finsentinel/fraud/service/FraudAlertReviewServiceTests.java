package com.finsentinel.fraud.service;

import com.finsentinel.account.repository.AccountRepository;
import com.finsentinel.beneficiary.repository.BeneficiaryRepository;
import com.finsentinel.fraud.repository.FraudAlertRepository;
import com.finsentinel.fraud.repository.FraudAlertReviewRepository;
import com.finsentinel.transaction.repository.LedgerEntryRepository;
import com.finsentinel.transaction.service.LedgerPostingService;
import com.finsentinel.transaction.repository.FinancialTransactionRepository;
import com.finsentinel.user.repository.UserRepository;
import com.finsentinel.transaction.service.AccountBalanceService;
import com.finsentinel.fraud.entity.FraudAlert;
import com.finsentinel.fraud.entity.FraudAssessment;
import com.finsentinel.fraud.entity.FraudAlertStatus;
import com.finsentinel.fraud.entity.FraudDecision;
import com.finsentinel.transaction.entity.FinancialTransaction;
import com.finsentinel.transaction.entity.TransactionStatus;
import com.finsentinel.transaction.entity.TransactionType;
import com.finsentinel.user.entity.AppUser;
import com.finsentinel.account.entity.Account;
import com.finsentinel.account.entity.AccountStatus;
import com.finsentinel.account.dto.AccountBalanceResponse;
import com.finsentinel.beneficiary.entity.Beneficiary;
import com.finsentinel.beneficiary.entity.BeneficiaryStatus;

import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FraudAlertReviewServiceTests {

    private FraudAlertReviewService service;

    private FraudAlertRepository fraudAlertRepository;
    private FraudAlertReviewRepository reviewRepository;
    private FinancialTransactionRepository transactionRepository;
    private AccountRepository accountRepository;
    private LedgerEntryRepository ledgerEntryRepository;
    private LedgerPostingService ledgerPostingService;
    private UserRepository userRepository;
    private BeneficiaryRepository beneficiaryRepository;
    private AccountBalanceService accountBalanceService;

    @BeforeEach
    void setUp() {

        fraudAlertRepository = mock(FraudAlertRepository.class);
        reviewRepository = mock(FraudAlertReviewRepository.class);
        transactionRepository = mock(FinancialTransactionRepository.class);
        accountRepository = mock(AccountRepository.class);
        ledgerEntryRepository = mock(LedgerEntryRepository.class);
        ledgerPostingService = mock(LedgerPostingService.class);
        userRepository = mock(UserRepository.class);
        beneficiaryRepository = mock(BeneficiaryRepository.class);
        accountBalanceService = mock(AccountBalanceService.class);

        service = new FraudAlertReviewService(
                fraudAlertRepository,
                reviewRepository,
                transactionRepository,
                accountRepository,
                ledgerEntryRepository,
                ledgerPostingService,
                userRepository,
                beneficiaryRepository,
                accountBalanceService
        );
    }

    @Test
    void shouldThrowExceptionWhenAlertDoesNotExist() {

        UUID alertId = UUID.randomUUID();

        when(fraudAlertRepository.findById(alertId))
                .thenReturn(java.util.Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getAlert(alertId)
        );
    }

    @Test
    void shouldThrowExceptionWhenApprovingNonExistingAlert() {

        UUID alertId = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();

        when(fraudAlertRepository.findByIdForUpdate(alertId))
                .thenReturn(java.util.Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.approveAlert(alertId, analystId, "Approved")
        );
    }

    @Test
    void shouldThrowExceptionWhenRejectingNonExistingAlert() {

        UUID alertId = UUID.randomUUID();
        UUID analystId = UUID.randomUUID();

        when(fraudAlertRepository.findByIdForUpdate(alertId))
                .thenReturn(java.util.Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.rejectAlert(alertId, analystId, "Rejected")
        );
    }
@Test
void shouldRejectHeldTransactionSuccessfully() {

    UUID alertId = UUID.randomUUID();
    UUID analystId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();

    AppUser analyst = new AppUser();
    ReflectionTestUtils.setField(analyst, "id", analystId);
    analyst.setEmail("analyst@test.com");

    FinancialTransaction transaction = new FinancialTransaction();
    ReflectionTestUtils.setField(transaction, "id", transactionId);

    transaction.setTransactionType(TransactionType.DEPOSIT);
    transaction.setAmount(new BigDecimal("1000.00"));
    transaction.setCurrency("INR");
    transaction.setStatus(TransactionStatus.PENDING);
    transaction.setReferenceNumber("TEST-001");

    FraudAssessment assessment = new FraudAssessment();
    assessment.setId(UUID.randomUUID());
    assessment.setTransaction(transaction);
    assessment.setRiskScore(80);
    assessment.setDecision(FraudDecision.HOLD);
    assessment.setAssessmentReason("Suspicious transaction");

    FraudAlert alert = new FraudAlert();
    ReflectionTestUtils.setField(alert, "id", alertId);
    alert.setAssessment(assessment);
    alert.setStatus(FraudAlertStatus.UNDER_REVIEW);
    alert.setAssignedTo(analyst);

    when(fraudAlertRepository.findByIdForUpdate(alertId))
            .thenReturn(Optional.of(alert));

    when(userRepository.findById(analystId))
            .thenReturn(Optional.of(analyst));

    when(transactionRepository.findByIdForUpdate(transactionId))
            .thenReturn(Optional.of(transaction));

    when(ledgerEntryRepository.existsByTransaction_Id(transactionId))
            .thenReturn(false);

    when(transactionRepository.save(any(FinancialTransaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    when(fraudAlertRepository.save(any(FraudAlert.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    service.rejectAlert(alertId, analystId, "Fraud confirmed");

    assertEquals(TransactionStatus.FAILED, transaction.getStatus());

    assertEquals(FraudAlertStatus.RESOLVED, alert.getStatus());

    assertEquals("Fraud confirmed", alert.getAnalystNotes());

    verify(ledgerPostingService, never())
            .postDeposit(any(), any());

    verify(reviewRepository).save(any());
}
@Test
void shouldApproveHeldDepositSuccessfully() {

    UUID alertId = UUID.randomUUID();
    UUID analystId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();

    AppUser analyst = new AppUser();
    ReflectionTestUtils.setField(analyst, "id", analystId);
    analyst.setEmail("analyst@test.com");

    AppUser customer = new AppUser();
    ReflectionTestUtils.setField(customer, "id", customerId);
    customer.setEmail("customer@test.com");

    Account account = new Account();
    ReflectionTestUtils.setField(account, "id", accountId);
    account.setStatus(AccountStatus.ACTIVE);

    FinancialTransaction transaction = new FinancialTransaction();
    ReflectionTestUtils.setField(transaction, "id", transactionId);

    transaction.setUser(customer);
    transaction.setDestinationAccount(account);
    transaction.setTransactionType(TransactionType.DEPOSIT);
    transaction.setAmount(new BigDecimal("1000.00"));
    transaction.setCurrency("INR");
    transaction.setStatus(TransactionStatus.PENDING);
    transaction.setReferenceNumber("TEST-002");

    FraudAssessment assessment = new FraudAssessment();
    assessment.setId(UUID.randomUUID());
    assessment.setTransaction(transaction);
    assessment.setRiskScore(80);
    assessment.setDecision(FraudDecision.HOLD);
    assessment.setAssessmentReason("Suspicious deposit");

    FraudAlert alert = new FraudAlert();
    ReflectionTestUtils.setField(alert, "id", alertId);
    alert.setAssessment(assessment);
    alert.setStatus(FraudAlertStatus.UNDER_REVIEW);
    alert.setAssignedTo(analyst);

    when(fraudAlertRepository.findByIdForUpdate(alertId))
            .thenReturn(Optional.of(alert));

    when(userRepository.findById(analystId))
            .thenReturn(Optional.of(analyst));

    when(transactionRepository.findByIdForUpdate(transactionId))
            .thenReturn(Optional.of(transaction));

    when(ledgerEntryRepository.existsByTransaction_Id(transactionId))
            .thenReturn(false);

    when(accountRepository.findByIdAndUser_IdForUpdate(accountId, customerId))
            .thenReturn(Optional.of(account));

    when(transactionRepository.save(any(FinancialTransaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    when(fraudAlertRepository.save(any(FraudAlert.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    service.approveAlert(alertId, analystId, "Deposit verified");

    assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());

    assertEquals(FraudAlertStatus.RESOLVED, alert.getStatus());

    verify(ledgerPostingService).postDeposit(transaction, account);

    verify(reviewRepository).save(any());
}
@Test
void shouldApproveHeldWithdrawalSuccessfully() {

    UUID alertId = UUID.randomUUID();
    UUID analystId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();

    AppUser analyst = new AppUser();
    ReflectionTestUtils.setField(analyst, "id", analystId);
    analyst.setEmail("analyst@test.com");

    AppUser customer = new AppUser();
    ReflectionTestUtils.setField(customer, "id", customerId);
    customer.setEmail("customer@test.com");

    Account account = new Account();
    ReflectionTestUtils.setField(account, "id", accountId);
    account.setStatus(AccountStatus.ACTIVE);

    FinancialTransaction transaction = new FinancialTransaction();
    ReflectionTestUtils.setField(transaction, "id", transactionId);

    transaction.setUser(customer);
    transaction.setSourceAccount(account);
    transaction.setTransactionType(TransactionType.WITHDRAWAL);
    transaction.setAmount(new BigDecimal("1000.00"));
    transaction.setCurrency("INR");
    transaction.setStatus(TransactionStatus.PENDING);
    transaction.setReferenceNumber("TEST-003");

    FraudAssessment assessment = new FraudAssessment();
    assessment.setId(UUID.randomUUID());
    assessment.setTransaction(transaction);
    assessment.setRiskScore(80);
    assessment.setDecision(FraudDecision.HOLD);
    assessment.setAssessmentReason("Suspicious withdrawal");

    FraudAlert alert = new FraudAlert();
    ReflectionTestUtils.setField(alert, "id", alertId);
    alert.setAssessment(assessment);
    alert.setStatus(FraudAlertStatus.UNDER_REVIEW);
    alert.setAssignedTo(analyst);

    when(fraudAlertRepository.findByIdForUpdate(alertId))
            .thenReturn(Optional.of(alert));

    when(userRepository.findById(analystId))
            .thenReturn(Optional.of(analyst));

    when(transactionRepository.findByIdForUpdate(transactionId))
            .thenReturn(Optional.of(transaction));

    when(ledgerEntryRepository.existsByTransaction_Id(transactionId))
            .thenReturn(false);

    when(accountRepository.findByIdAndUser_IdForUpdate(accountId, customerId))
            .thenReturn(Optional.of(account));

    when(accountBalanceService.getBalance(customerId, accountId))
            .thenReturn(new AccountBalanceResponse(
                    accountId,
                    "TEST-ACCOUNT",
                    "INR",
                    new BigDecimal("5000.00")
            ));

    when(transactionRepository.save(any(FinancialTransaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    when(fraudAlertRepository.save(any(FraudAlert.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    service.approveAlert(alertId, analystId, "Withdrawal verified");

    assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());

    assertEquals(FraudAlertStatus.RESOLVED, alert.getStatus());

    verify(ledgerPostingService).postWithdrawal(transaction, account);

    verify(reviewRepository).save(any());
}
@Test
void shouldApproveHeldTransferSuccessfully() {

    UUID alertId = UUID.randomUUID();
    UUID analystId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();

    UUID sourceId = UUID.randomUUID();
    UUID destinationId = UUID.randomUUID();

    AppUser analyst = new AppUser();
    ReflectionTestUtils.setField(analyst, "id", analystId);
    analyst.setEmail("analyst@test.com");

    AppUser customer = new AppUser();
    ReflectionTestUtils.setField(customer, "id", customerId);
    customer.setEmail("customer@test.com");

    Account sourceAccount = new Account();
    ReflectionTestUtils.setField(sourceAccount, "id", sourceId);
    sourceAccount.setStatus(AccountStatus.ACTIVE);
    sourceAccount.setCurrency("INR");

    Account destinationAccount = new Account();
    ReflectionTestUtils.setField(destinationAccount, "id", destinationId);
    destinationAccount.setStatus(AccountStatus.ACTIVE);
    destinationAccount.setCurrency("INR");

    FinancialTransaction transaction = new FinancialTransaction();
    ReflectionTestUtils.setField(transaction, "id", transactionId);

    transaction.setUser(customer);
    transaction.setSourceAccount(sourceAccount);
    transaction.setDestinationAccount(destinationAccount);
    transaction.setTransactionType(TransactionType.TRANSFER);
    transaction.setAmount(new BigDecimal("1000.00"));
    transaction.setCurrency("INR");
    transaction.setStatus(TransactionStatus.PENDING);
    transaction.setReferenceNumber("TEST-004");

    FraudAssessment assessment = new FraudAssessment();
    assessment.setId(UUID.randomUUID());
    assessment.setTransaction(transaction);
    assessment.setRiskScore(80);
    assessment.setDecision(FraudDecision.HOLD);
    assessment.setAssessmentReason("Suspicious transfer");

    FraudAlert alert = new FraudAlert();
    ReflectionTestUtils.setField(alert, "id", alertId);
    alert.setAssessment(assessment);
    alert.setStatus(FraudAlertStatus.UNDER_REVIEW);
    alert.setAssignedTo(analyst);

    when(fraudAlertRepository.findByIdForUpdate(alertId))
            .thenReturn(Optional.of(alert));

    when(userRepository.findById(analystId))
            .thenReturn(Optional.of(analyst));

    when(transactionRepository.findByIdForUpdate(transactionId))
            .thenReturn(Optional.of(transaction));

    when(ledgerEntryRepository.existsByTransaction_Id(transactionId))
            .thenReturn(false);

    UUID firstId = sourceId.compareTo(destinationId) < 0
            ? sourceId : destinationId;

    UUID secondId = sourceId.compareTo(destinationId) < 0
            ? destinationId : sourceId;

    Account firstAccount = firstId.equals(sourceId)
            ? sourceAccount : destinationAccount;

    Account secondAccount = secondId.equals(sourceId)
            ? sourceAccount : destinationAccount;

    when(accountRepository.findByIdAndUser_IdForUpdate(firstId, customerId))
            .thenReturn(Optional.of(firstAccount));

    when(accountRepository.findByIdAndUser_IdForUpdate(secondId, customerId))
            .thenReturn(Optional.of(secondAccount));

    when(accountBalanceService.getBalance(customerId, sourceId))
            .thenReturn(new AccountBalanceResponse(
                    sourceId,
                    "SOURCE-ACCOUNT",
                    "INR",
                    new BigDecimal("5000.00")
            ));

    when(transactionRepository.save(any(FinancialTransaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    when(fraudAlertRepository.save(any(FraudAlert.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    service.approveAlert(alertId, analystId, "Transfer verified");

    assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());

    assertEquals(FraudAlertStatus.RESOLVED, alert.getStatus());

    verify(ledgerPostingService).postTransfer(
            transaction,
            sourceAccount,
            destinationAccount
    );

    verify(reviewRepository).save(any());
}
@Test
void shouldApproveHeldBeneficiaryTransferSuccessfully() {

    UUID alertId = UUID.randomUUID();
    UUID analystId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();
    UUID beneficiaryId = UUID.randomUUID();

    AppUser analyst = new AppUser();
    ReflectionTestUtils.setField(analyst, "id", analystId);
    analyst.setEmail("analyst@test.com");

    AppUser customer = new AppUser();
    ReflectionTestUtils.setField(customer, "id", customerId);
    customer.setEmail("customer@test.com");

    Account sourceAccount = new Account();
    ReflectionTestUtils.setField(sourceAccount, "id", accountId);
    sourceAccount.setStatus(AccountStatus.ACTIVE);
    sourceAccount.setCurrency("INR");

    Beneficiary beneficiary = new Beneficiary();
    ReflectionTestUtils.setField(beneficiary, "id", beneficiaryId);
    beneficiary.setUser(customer);
    beneficiary.setStatus(BeneficiaryStatus.ACTIVE);

    FinancialTransaction transaction = new FinancialTransaction();
    ReflectionTestUtils.setField(transaction, "id", transactionId);

    transaction.setUser(customer);
    transaction.setSourceAccount(sourceAccount);
    transaction.setBeneficiary(beneficiary);
    transaction.setTransactionType(TransactionType.BENEFICIARY_TRANSFER);
    transaction.setAmount(new BigDecimal("1000.00"));
    transaction.setCurrency("INR");
    transaction.setStatus(TransactionStatus.PENDING);
    transaction.setReferenceNumber("TEST-005");

    FraudAssessment assessment = new FraudAssessment();
    assessment.setId(UUID.randomUUID());
    assessment.setTransaction(transaction);
    assessment.setRiskScore(80);
    assessment.setDecision(FraudDecision.HOLD);
    assessment.setAssessmentReason("Suspicious beneficiary transfer");

    FraudAlert alert = new FraudAlert();
    ReflectionTestUtils.setField(alert, "id", alertId);
    alert.setAssessment(assessment);
    alert.setStatus(FraudAlertStatus.UNDER_REVIEW);
    alert.setAssignedTo(analyst);

    when(fraudAlertRepository.findByIdForUpdate(alertId))
            .thenReturn(Optional.of(alert));

    when(userRepository.findById(analystId))
            .thenReturn(Optional.of(analyst));

    when(transactionRepository.findByIdForUpdate(transactionId))
            .thenReturn(Optional.of(transaction));

    when(ledgerEntryRepository.existsByTransaction_Id(transactionId))
            .thenReturn(false);

    when(accountRepository.findByIdAndUser_IdForUpdate(accountId, customerId))
            .thenReturn(Optional.of(sourceAccount));

    when(beneficiaryRepository.findByIdAndUser_IdAndStatus(
            beneficiaryId,
            customerId,
            BeneficiaryStatus.ACTIVE
    )).thenReturn(Optional.of(beneficiary));

    when(accountBalanceService.getBalance(customerId, accountId))
            .thenReturn(new AccountBalanceResponse(
                    accountId,
                    "SOURCE-ACCOUNT",
                    "INR",
                    new BigDecimal("5000.00")
            ));

    when(transactionRepository.save(any(FinancialTransaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    when(fraudAlertRepository.save(any(FraudAlert.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    service.approveAlert(alertId, analystId, "Beneficiary transfer verified");

    assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());

    assertEquals(FraudAlertStatus.RESOLVED, alert.getStatus());

    verify(ledgerPostingService).postBeneficiaryTransfer(
            transaction,
            sourceAccount
    );

    verify(reviewRepository).save(any());
}
@Test
void shouldPreventAnalystFromApprovingAnotherAnalystsAlert() {

    UUID alertId = UUID.randomUUID();
    UUID currentAnalystId = UUID.randomUUID();
    UUID assignedAnalystId = UUID.randomUUID();

    AppUser currentAnalyst = new AppUser();
    ReflectionTestUtils.setField(currentAnalyst, "id", currentAnalystId);
    currentAnalyst.setEmail("current@test.com");

    AppUser assignedAnalyst = new AppUser();
    ReflectionTestUtils.setField(assignedAnalyst, "id", assignedAnalystId);
    assignedAnalyst.setEmail("assigned@test.com");

    FraudAlert alert = new FraudAlert();
    ReflectionTestUtils.setField(alert, "id", alertId);
    alert.setStatus(FraudAlertStatus.UNDER_REVIEW);
    alert.setAssignedTo(assignedAnalyst);

    when(fraudAlertRepository.findByIdForUpdate(alertId))
            .thenReturn(Optional.of(alert));

    when(userRepository.findById(currentAnalystId))
            .thenReturn(Optional.of(currentAnalyst));

    assertThrows(
            IllegalStateException.class,
            () -> service.approveAlert(
                    alertId,
                    currentAnalystId,
                    "Attempted unauthorized approval"
            )
    );


    verify(transactionRepository, never())
            .findByIdForUpdate(any());

    verify(ledgerPostingService, never())
            .postDeposit(any(), any());

    verify(reviewRepository, never()).save(any());
}
@Test
void shouldPreventWithdrawalApprovalWhenBalanceIsInsufficient() {

    UUID alertId = UUID.randomUUID();
    UUID analystId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();

    AppUser analyst = new AppUser();
    ReflectionTestUtils.setField(analyst, "id", analystId);
    analyst.setEmail("analyst@test.com");

    AppUser customer = new AppUser();
    ReflectionTestUtils.setField(customer, "id", customerId);
    customer.setEmail("customer@test.com");

    Account account = new Account();
    ReflectionTestUtils.setField(account, "id", accountId);
    account.setStatus(AccountStatus.ACTIVE);
    account.setCurrency("INR");

    FinancialTransaction transaction = new FinancialTransaction();
    ReflectionTestUtils.setField(transaction, "id", transactionId);

    transaction.setUser(customer);
    transaction.setSourceAccount(account);
    transaction.setTransactionType(TransactionType.WITHDRAWAL);
    transaction.setAmount(new BigDecimal("10000.00"));
    transaction.setCurrency("INR");
    transaction.setStatus(TransactionStatus.PENDING);
    transaction.setReferenceNumber("TEST-006");

    FraudAssessment assessment = new FraudAssessment();
    assessment.setId(UUID.randomUUID());
    assessment.setTransaction(transaction);
    assessment.setRiskScore(80);
    assessment.setDecision(FraudDecision.HOLD);
    assessment.setAssessmentReason("Suspicious withdrawal");

    FraudAlert alert = new FraudAlert();
    ReflectionTestUtils.setField(alert, "id", alertId);
    alert.setAssessment(assessment);
    alert.setStatus(FraudAlertStatus.UNDER_REVIEW);
    alert.setAssignedTo(analyst);

    when(fraudAlertRepository.findByIdForUpdate(alertId))
            .thenReturn(Optional.of(alert));

    when(userRepository.findById(analystId))
            .thenReturn(Optional.of(analyst));

    when(transactionRepository.findByIdForUpdate(transactionId))
            .thenReturn(Optional.of(transaction));

    when(ledgerEntryRepository.existsByTransaction_Id(transactionId))
            .thenReturn(false);

    when(accountRepository.findByIdAndUser_IdForUpdate(accountId, customerId))
            .thenReturn(Optional.of(account));

    when(accountBalanceService.getBalance(customerId, accountId))
            .thenReturn(new AccountBalanceResponse(
                    accountId,
                    "SOURCE-ACCOUNT",
                    "INR",
                    new BigDecimal("500.00")
            ));

    assertThrows(
            IllegalStateException.class,
            () -> service.approveAlert(
                    alertId,
                    analystId,
                    "Attempted withdrawal approval"
            )
    );

    verify(ledgerPostingService, never())
            .postWithdrawal(any(), any());

    verify(reviewRepository, never()).save(any());
}
@Test
void shouldPreventWithdrawalApprovalFromFrozenAccount() {

    UUID alertId = UUID.randomUUID();
    UUID analystId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();

    AppUser analyst = new AppUser();
    ReflectionTestUtils.setField(analyst, "id", analystId);
    analyst.setEmail("analyst@test.com");

    AppUser customer = new AppUser();
    ReflectionTestUtils.setField(customer, "id", customerId);
    customer.setEmail("customer@test.com");

    Account account = new Account();
    ReflectionTestUtils.setField(account, "id", accountId);
    account.setStatus(AccountStatus.FROZEN);
    account.setCurrency("INR");

    FinancialTransaction transaction = new FinancialTransaction();
    ReflectionTestUtils.setField(transaction, "id", transactionId);

    transaction.setUser(customer);
    transaction.setSourceAccount(account);
    transaction.setTransactionType(TransactionType.WITHDRAWAL);
    transaction.setAmount(new BigDecimal("1000.00"));
    transaction.setCurrency("INR");
    transaction.setStatus(TransactionStatus.PENDING);
    transaction.setReferenceNumber("TEST-007");

    FraudAssessment assessment = new FraudAssessment();
    assessment.setId(UUID.randomUUID());
    assessment.setTransaction(transaction);
    assessment.setRiskScore(80);
    assessment.setDecision(FraudDecision.HOLD);
    assessment.setAssessmentReason("Suspicious withdrawal");

    FraudAlert alert = new FraudAlert();
    ReflectionTestUtils.setField(alert, "id", alertId);
    alert.setAssessment(assessment);
    alert.setStatus(FraudAlertStatus.UNDER_REVIEW);
    alert.setAssignedTo(analyst);

    when(fraudAlertRepository.findByIdForUpdate(alertId))
            .thenReturn(Optional.of(alert));

    when(userRepository.findById(analystId))
            .thenReturn(Optional.of(analyst));

    when(transactionRepository.findByIdForUpdate(transactionId))
            .thenReturn(Optional.of(transaction));

    when(ledgerEntryRepository.existsByTransaction_Id(transactionId))
            .thenReturn(false);

    when(accountRepository.findByIdAndUser_IdForUpdate(accountId, customerId))
            .thenReturn(Optional.of(account));

    assertThrows(
            IllegalStateException.class,
            () -> service.approveAlert(
                    alertId,
                    analystId,
                    "Attempted withdrawal from frozen account"
            )
    );

    verify(accountBalanceService, never())
            .getBalance(any(), any());

    verify(ledgerPostingService, never())
            .postWithdrawal(any(), any());

    verify(reviewRepository, never()).save(any());
}
@Test
void shouldPreventApprovalWhenLedgerEntryAlreadyExists() {

    UUID alertId = UUID.randomUUID();
    UUID analystId = UUID.randomUUID();
    UUID transactionId = UUID.randomUUID();

    AppUser analyst = new AppUser();
    ReflectionTestUtils.setField(analyst, "id", analystId);
    analyst.setEmail("analyst@test.com");

    FinancialTransaction transaction = new FinancialTransaction();
    ReflectionTestUtils.setField(transaction, "id", transactionId);

    transaction.setTransactionType(TransactionType.DEPOSIT);
    transaction.setAmount(new BigDecimal("1000.00"));
    transaction.setCurrency("INR");
    transaction.setStatus(TransactionStatus.PENDING);
    transaction.setReferenceNumber("TEST-008");

    FraudAssessment assessment = new FraudAssessment();
    assessment.setId(UUID.randomUUID());
    assessment.setTransaction(transaction);
    assessment.setRiskScore(80);
    assessment.setDecision(FraudDecision.HOLD);

    FraudAlert alert = new FraudAlert();
    ReflectionTestUtils.setField(alert, "id", alertId);
    alert.setAssessment(assessment);
    alert.setStatus(FraudAlertStatus.UNDER_REVIEW);
    alert.setAssignedTo(analyst);

    when(fraudAlertRepository.findByIdForUpdate(alertId))
            .thenReturn(Optional.of(alert));

    when(userRepository.findById(analystId))
            .thenReturn(Optional.of(analyst));

    when(transactionRepository.findByIdForUpdate(transactionId))
            .thenReturn(Optional.of(transaction));

    when(ledgerEntryRepository.existsByTransaction_Id(transactionId))
            .thenReturn(true);

    assertThrows(
            IllegalStateException.class,
            () -> service.approveAlert(
                    alertId,
                    analystId,
                    "Attempted duplicate approval"
            )
    );

    verify(ledgerPostingService, never())
            .postDeposit(any(), any());

    verify(transactionRepository, never())
            .save(any(FinancialTransaction.class));

    verify(reviewRepository, never()).save(any());
}
}
