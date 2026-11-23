package com.finsentinel.account.repository;

import com.finsentinel.account.entity.Account;
import com.finsentinel.account.entity.AccountStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByUser_Id(UUID userId);

    List<Account> findByUser_IdAndStatus(
            UUID userId,
            AccountStatus status
    );

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    Optional<Account> findByIdAndUser_Id(
            UUID accountId,
            UUID userId
    );

    // Lock an account while performing a financial operation.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a
            FROM Account a
            WHERE a.id = :accountId
            AND a.user.id = :userId
            """)
    Optional<Account> findByIdAndUser_IdForUpdate(
            @Param("accountId") UUID accountId,
            @Param("userId") UUID userId
    );
}