package com.finsentinel.account.dto;

import com.finsentinel.account.entity.AccountStatus;
import com.finsentinel.account.entity.AccountStatusAction;
import com.finsentinel.account.entity.AccountStatusHistory;

import java.time.Instant;
import java.util.UUID;

public record AccountStatusHistoryResponse(
        UUID id,
        AccountStatusAction action,
        AccountStatus previousStatus,
        AccountStatus newStatus,
        Instant createdAt
) {

    public static AccountStatusHistoryResponse from(
            AccountStatusHistory history
    ) {
        return new AccountStatusHistoryResponse(
                history.getId(),
                history.getAction(),
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getCreatedAt()
        );
    }
}