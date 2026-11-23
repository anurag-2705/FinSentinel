package com.finsentinel.transaction.controller;

import com.finsentinel.transaction.dto.TransactionFilterRequest;
import com.finsentinel.transaction.dto.TransactionPageResponse;

import com.finsentinel.transaction.dto.DepositRequest;
import com.finsentinel.transaction.dto.WithdrawalRequest;
import com.finsentinel.transaction.dto.TransferRequest;
import com.finsentinel.transaction.dto.BeneficiaryTransferRequest;
import com.finsentinel.transaction.dto.TransactionResponse;

import com.finsentinel.transaction.service.DepositService;
import com.finsentinel.transaction.service.WithdrawalService;
import com.finsentinel.transaction.service.TransactionHistoryService;
import com.finsentinel.transaction.service.TransferService;
import com.finsentinel.transaction.service.BeneficiaryTransferService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final DepositService depositService;
    private final WithdrawalService withdrawalService;
    private final TransactionHistoryService transactionHistoryService;
    private final TransferService transferService;
    private final BeneficiaryTransferService beneficiaryTransferService;

    public TransactionController(
            DepositService depositService,
            WithdrawalService withdrawalService,
            TransactionHistoryService transactionHistoryService,
            TransferService transferService,
            BeneficiaryTransferService beneficiaryTransferService) {

        this.depositService = depositService;
        this.withdrawalService = withdrawalService;
        this.transactionHistoryService = transactionHistoryService;
        this.transferService = transferService;
        this.beneficiaryTransferService = beneficiaryTransferService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @AuthenticationPrincipal UUID userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DepositRequest request) {

        TransactionResponse response =
                depositService.deposit(userId, request, idempotencyKey);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @AuthenticationPrincipal UUID userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody WithdrawalRequest request) {

        TransactionResponse response =
                withdrawalService.withdraw(userId, request, idempotencyKey);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @AuthenticationPrincipal UUID userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        TransactionResponse response =
                transferService.transfer(userId, request, idempotencyKey);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/beneficiary-transfer")
    public ResponseEntity<TransactionResponse> beneficiaryTransfer(
            @AuthenticationPrincipal UUID userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody BeneficiaryTransferRequest request) {

        TransactionResponse response =
                beneficiaryTransferService.transfer(
                        userId,
                        request,
                        idempotencyKey
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            @AuthenticationPrincipal UUID userId) {

        List<TransactionResponse> transactions =
                transactionHistoryService.getTransactionHistory(userId);

        return ResponseEntity.ok(transactions);
    }
@GetMapping("/filter")
public ResponseEntity<TransactionPageResponse> getFilteredTransactionHistory(
        @AuthenticationPrincipal UUID userId,
        @RequestParam(required = false) String transactionType,
        @RequestParam(required = false) Instant startDate,
        @RequestParam(required = false) Instant endDate,
        @RequestParam(required = false) UUID accountId,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size) {

    TransactionFilterRequest request = new TransactionFilterRequest(
            transactionType == null || transactionType.isBlank()
                    ? null
                    : com.finsentinel.transaction.entity.TransactionType.valueOf(
                            transactionType.toUpperCase()),
            startDate,
            endDate,
            accountId,
            page,
            size
    );

    return ResponseEntity.ok(
            transactionHistoryService.getFilteredTransactionHistory(
                    userId,
                    request
            )
    );
}
}