package com.finsentinel.transaction.dto;

import com.finsentinel.transaction.entity.TransactionType;

import java.time.Instant;
import java.util.UUID;

public record TransactionFilterRequest(

        TransactionType transactionType,

        Instant startDate,

        Instant endDate,

        UUID accountId,

        Integer page,

        Integer size

) {
}