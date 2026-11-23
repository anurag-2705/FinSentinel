package com.finsentinel.account.dto;

import com.finsentinel.account.entity.AccountType;

import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(

        @NotNull(message = "Account type is required")
        AccountType accountType

) {
}