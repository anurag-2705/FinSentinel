package com.finsentinel.fraud.repository;

import com.finsentinel.fraud.entity.FraudAlert;
import com.finsentinel.fraud.entity.FraudAlertStatus;
import com.finsentinel.transaction.entity.TransactionType;

import org.springframework.data.jpa.domain.Specification;

public final class FraudDashboardSpecification {

    private FraudDashboardSpecification() {
    }

    public static Specification<FraudAlert> hasStatus(
            FraudAlertStatus status
    ) {
        return (root, query, criteriaBuilder) -> {

            if (status == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    root.get("status"),
                    status
            );
        };
    }

    public static Specification<FraudAlert> hasMinimumRiskScore(
            Integer minimumRiskScore
    ) {
        return (root, query, criteriaBuilder) -> {

            if (minimumRiskScore == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.greaterThanOrEqualTo(
                    root.join("assessment").get("riskScore"),
                    minimumRiskScore
            );
        };
    }

    public static Specification<FraudAlert> hasMaximumRiskScore(
            Integer maximumRiskScore
    ) {
        return (root, query, criteriaBuilder) -> {

            if (maximumRiskScore == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.lessThanOrEqualTo(
                    root.join("assessment").get("riskScore"),
                    maximumRiskScore
            );
        };
    }

    public static Specification<FraudAlert> hasTransactionType(
            TransactionType transactionType
    ) {
        return (root, query, criteriaBuilder) -> {

            if (transactionType == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    root.join("assessment")
                            .join("transaction")
                            .get("transactionType"),
                    transactionType
            );
        };
    }
}