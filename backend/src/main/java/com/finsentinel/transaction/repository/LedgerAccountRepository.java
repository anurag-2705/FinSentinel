package com.finsentinel.transaction.repository;

import com.finsentinel.transaction.entity.LedgerAccount;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LedgerAccountRepository
        extends JpaRepository<LedgerAccount, UUID> {

    Optional<LedgerAccount> findByAccountCode(String accountCode);

    Optional<LedgerAccount> findByAccount_Id(UUID accountId);

    boolean existsByAccountCode(String accountCode);

    boolean existsByAccount_Id(UUID accountId);
}