package com.finsentinel.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(

        @NotNull(message = "Source account ID is required")
        UUID sourceAccountId,

        @NotNull(message = "Destination account ID is required")
        UUID destinationAccountId,

        @NotNull(message = "Transfer amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Transfer amount must be greater than zero"
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Amount can have at most 2 decimal places"
        )
        BigDecimal amount,

        @Size(
                max = 255,
                message = "Description cannot exceed 255 characters"
        )
        String description

) {
}