package com.finsentinel.transaction.repository;

import com.finsentinel.transaction.dto.TransactionFilterRequest;
import com.finsentinel.transaction.entity.FinancialTransaction;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<FinancialTransaction> filtered(
            UUID userId,
            TransactionFilterRequest request) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(
                    criteriaBuilder.equal(
                            root.get("user").get("id"),
                            userId
                    )
            );

            if (request.transactionType() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("transactionType"),
                                request.transactionType()
                        )
                );
            }

            if (request.startDate() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdAt"),
                                request.startDate()
                        )
                );
            }

            if (request.endDate() != null) {
                predicates.add(
                        criteriaBuilder.lessThan(
                                root.get("createdAt"),
                                request.endDate()
                        )
                );
            }

            if (request.accountId() != null) {

                Predicate sourceAccount = criteriaBuilder.equal(
                        root.get("sourceAccount").get("id"),
                        request.accountId()
                );

                Predicate destinationAccount = criteriaBuilder.equal(
                        root.get("destinationAccount").get("id"),
                        request.accountId()
                );

                predicates.add(
                        criteriaBuilder.or(
                                sourceAccount,
                                destinationAccount
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}