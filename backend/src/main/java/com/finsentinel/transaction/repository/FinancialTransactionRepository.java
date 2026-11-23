package com.finsentinel.transaction.repository;

import com.finsentinel.transaction.entity.FinancialTransaction;
import com.finsentinel.transaction.entity.TransactionStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinancialTransactionRepository
        extends JpaRepository<FinancialTransaction, UUID>,
                JpaSpecificationExecutor<FinancialTransaction> {

    Optional<FinancialTransaction> findByReferenceNumber(
            String referenceNumber
    );

    Optional<FinancialTransaction> findByUser_IdAndIdempotencyKey(
            UUID userId,
            String idempotencyKey
    );

    boolean existsByUser_IdAndIdempotencyKey(
            UUID userId,
            String idempotencyKey
    );

    List<FinancialTransaction> findByUser_IdOrderByCreatedAtDesc(
            UUID userId
    );

    List<FinancialTransaction> findBySourceAccount_IdOrderByCreatedAtDesc(
            UUID accountId
    );

    List<FinancialTransaction> findByDestinationAccount_IdOrderByCreatedAtDesc(
            UUID accountId
    );

    // Count failed transactions within a specified time window.
    long countByUser_IdAndStatusAndCreatedAtAfter(
            UUID userId,
            TransactionStatus status,
            Instant createdAt
    );

    // Count recent transactions for rapid transaction detection.
    long countByUser_IdAndCreatedAtAfter(
            UUID userId,
            Instant createdAt
    );

    // Count completed transactions for customer behaviour analysis.
    long countByUser_IdAndStatus(
            UUID userId,
            TransactionStatus status
    );

    // Calculate the customer's historical average transaction amount.
    @Query("""
            SELECT AVG(t.amount)
            FROM FinancialTransaction t
            WHERE t.user.id = :userId
            AND t.status = :status
            """)
    Optional<BigDecimal> findAverageTransactionAmount(
            @Param("userId") UUID userId,
            @Param("status") TransactionStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT t
            FROM FinancialTransaction t
            WHERE t.id = :transactionId
            """)
    Optional<FinancialTransaction> findByIdForUpdate(
            @Param("transactionId") UUID transactionId
    );
}