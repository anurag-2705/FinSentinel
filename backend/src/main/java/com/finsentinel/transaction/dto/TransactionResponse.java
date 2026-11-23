package com.finsentinel.transaction.dto;

import com.finsentinel.transaction.entity.FinancialTransaction;
import com.finsentinel.transaction.entity.TransactionStatus;
import com.finsentinel.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(

        UUID id,

        String referenceNumber,

        TransactionType transactionType,

        BigDecimal amount,

        String currency,

        TransactionStatus status,

        UUID sourceAccountId,

        UUID destinationAccountId,

        String description,

        Instant createdAt,

        Instant completedAt

) {

    public static TransactionResponse from(
            FinancialTransaction transaction
    ) {

        return new TransactionResponse(
                transaction.getId(),
                transaction.getReferenceNumber(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),

                transaction.getSourceAccount() != null
                        ? transaction.getSourceAccount().getId()
                        : null,

                transaction.getDestinationAccount() != null
                        ? transaction.getDestinationAccount().getId()
                        : null,

                transaction.getDescription(),
                transaction.getCreatedAt(),
                transaction.getCompletedAt()
        );
    }
}
