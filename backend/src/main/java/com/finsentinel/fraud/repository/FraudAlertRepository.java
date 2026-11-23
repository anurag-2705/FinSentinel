package com.finsentinel.fraud.repository;

import com.finsentinel.fraud.entity.FraudAlert;
import com.finsentinel.fraud.entity.FraudAlertStatus;
import com.finsentinel.transaction.entity.TransactionStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FraudAlertRepository extends
        JpaRepository<FraudAlert, UUID>,
        JpaSpecificationExecutor<FraudAlert> {

    Optional<FraudAlert> findByAssessment_Id(UUID assessmentId);

    List<FraudAlert> findByStatusOrderByCreatedAtDesc(
            FraudAlertStatus status
    );

    List<FraudAlert> findAllByOrderByCreatedAtDesc();

    List<FraudAlert> findByAssignedTo_IdOrderByCreatedAtDesc(
            UUID analystId
    );

    boolean existsByAssessment_Id(UUID assessmentId);

    // Dashboard: alert statistics

    long countByStatus(FraudAlertStatus status);

    long countByAssessment_RiskScoreGreaterThanEqual(
            Integer riskScore
    );

    @Query("""
            SELECT COALESCE(SUM(a.assessment.transaction.amount), 0)
            FROM FraudAlert a
            WHERE a.assessment.transaction.status = :transactionStatus
            """)
    BigDecimal sumTransactionAmountByTransactionStatus(
            @Param("transactionStatus") TransactionStatus transactionStatus
    );

    // Dashboard: analyst workload

    long countByAssignedTo_Id(UUID analystId);

    long countByAssignedTo_IdAndStatus(
            UUID analystId,
            FraudAlertStatus status
    );

    // Fraud review: pessimistic locking

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a
            FROM FraudAlert a
            WHERE a.id = :alertId
            """)
    Optional<FraudAlert> findByIdForUpdate(
            @Param("alertId") UUID alertId
    );
    @Query("""
        SELECT COUNT(a)
        FROM FraudAlert a
        WHERE a.assessment.transaction.status = :transactionStatus
        """)
long countByTransactionStatus(
        @Param("transactionStatus") TransactionStatus transactionStatus
);
}