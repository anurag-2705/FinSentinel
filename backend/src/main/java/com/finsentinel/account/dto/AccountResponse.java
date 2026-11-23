package com.finsentinel.account.dto;

import com.finsentinel.account.entity.Account;
import com.finsentinel.account.entity.AccountStatus;
import com.finsentinel.account.entity.AccountType;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        AccountType accountType,
        String currency,
        AccountStatus status,
        Instant createdAt
) {

    public static AccountResponse from(Account account) {

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getCurrency(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}