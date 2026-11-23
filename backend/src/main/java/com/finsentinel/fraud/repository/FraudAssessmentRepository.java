
package com.finsentinel.fraud.repository;

import com.finsentinel.fraud.entity.FraudAssessment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FraudAssessmentRepository
        extends JpaRepository<FraudAssessment, UUID> {

    Optional<FraudAssessment> findByTransaction_Id(UUID transactionId);

    boolean existsByTransaction_Id(UUID transactionId);
}
