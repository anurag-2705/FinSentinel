package com.finsentinel.account.controller;

import com.finsentinel.account.dto.AccountBalanceResponse;
import com.finsentinel.account.dto.AccountResponse;
import com.finsentinel.account.dto.CreateAccountRequest;
import com.finsentinel.account.service.AccountService;
import com.finsentinel.transaction.service.AccountBalanceService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountBalanceService accountBalanceService;

    public AccountController(
            AccountService accountService,
            AccountBalanceService accountBalanceService
    ) {
        this.accountService = accountService;
        this.accountBalanceService = accountBalanceService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateAccountRequest request
    ) {
        AccountResponse response =
                accountService.createAccount(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(
            @AuthenticationPrincipal UUID userId
    ) {
        List<AccountResponse> accounts =
                accountService.getMyAccounts(userId);

        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID accountId
    ) {
        AccountResponse account =
                accountService.getAccountById(userId, accountId);

        return ResponseEntity.ok(account);
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<AccountBalanceResponse> getAccountBalance(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID accountId
    ) {
        AccountBalanceResponse balance =
                accountBalanceService.getBalance(userId, accountId);

        return ResponseEntity.ok(balance);
    }

    @PatchMapping("/{accountId}/freeze")
    public ResponseEntity<AccountResponse> freezeAccount(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID accountId
    ) {
        AccountResponse account =
                accountService.freezeAccount(userId, accountId);

        return ResponseEntity.ok(account);
    }

    @PatchMapping("/{accountId}/unfreeze")
    public ResponseEntity<AccountResponse> unfreezeAccount(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID accountId
    ) {
        AccountResponse account =
                accountService.unfreezeAccount(userId, accountId);

        return ResponseEntity.ok(account);
    }
}