package com.finsentinel.fraud.repository;

import com.finsentinel.fraud.entity.FraudAlertReview;
import com.finsentinel.fraud.entity.FraudReviewAction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FraudAlertReviewRepository
        extends JpaRepository<FraudAlertReview, UUID> {

    List<FraudAlertReview> findByAlert_IdOrderByCreatedAtDesc(UUID alertId);

    List<FraudAlertReview> findByAnalyst_IdOrderByCreatedAtDesc(UUID analystId);

    long countByAnalyst_IdAndAction(
            UUID analystId,
            FraudReviewAction action
    );

    long countByAnalyst_IdAndActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID analystId,
            FraudReviewAction action,
            Instant start,
            Instant end
    );
}
