package com.finsentinel.account.repository;

import com.finsentinel.account.entity.AccountStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountStatusHistoryRepository
        extends JpaRepository<AccountStatusHistory, UUID> {

    List<AccountStatusHistory> findByAccountIdOrderByCreatedAtDesc(
            UUID accountId
    );

}