package com.finsentinel.fraud.repository;

import com.finsentinel.fraud.entity.FraudAlertReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FraudAlertReviewRepository
        extends JpaRepository<FraudAlertReview, UUID> {

    List<FraudAlertReview> findByAlert_IdOrderByCreatedAtDesc(UUID alertId);

    List<FraudAlertReview> findByAnalyst_IdOrderByCreatedAtDesc(UUID analystId);
}