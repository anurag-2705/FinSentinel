package com.finsentinel.ml.service;

import com.finsentinel.ml.dto.MLPredictionRequest;
import com.finsentinel.transaction.entity.FinancialTransaction;
import com.finsentinel.transaction.entity.TransactionStatus;
import com.finsentinel.transaction.repository.FinancialTransactionRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class MLFeatureExtractionService {

    private static final ZoneId PLATFORM_ZONE =
            ZoneId.of("Asia/Kolkata");

    private final FinancialTransactionRepository transactionRepository;

    public MLFeatureExtractionService(
            FinancialTransactionRepository transactionRepository
    ) {
        this.transactionRepository = transactionRepository;
    }

    public MLPredictionRequest extractFeatures(
            FinancialTransaction transaction
    ) {

        Instant transactionTime = transaction.getCreatedAt();

        if (transactionTime == null) {
            transactionTime = Instant.now();
        }

        UUID userId = transaction.getUser().getId();

        BigDecimal amount = transaction.getAmount();

        ZonedDateTime localTime =
                transactionTime.atZone(PLATFORM_ZONE);

        int transactionHour = localTime.getHour();

        int isWeekend =
                (localTime.getDayOfWeek().getValue() >= 6) ? 1 : 0;

        // Transactions during the previous hour.
        long transactionCount = transactionRepository
                .countByUser_IdAndCreatedAtAfter(
                        userId,
                        transactionTime.minus(Duration.ofHours(1))
                );

        // Exclude the current transaction if it is already persisted.
        if (transaction.getId() != null) {
            transactionCount = Math.max(0, transactionCount - 1);
        }

        // Failed financial transactions in the previous 24 hours.
        long failedTransactions = transactionRepository
                .countByUser_IdAndStatusAndCreatedAtAfter(
                        userId,
                        TransactionStatus.FAILED,
                        transactionTime.minus(Duration.ofHours(24))
                );

        // Determine whether the beneficiary is newly added.
        int newPayee = 0;

        if (transaction.getBeneficiary() != null
                && transaction.getBeneficiary().getCreatedAt() != null) {

            Instant beneficiaryCreatedAt =
                    transaction.getBeneficiary().getCreatedAt();

            Instant newPayeeWindow =
                    transactionTime.minus(Duration.ofHours(24));

            if (!beneficiaryCreatedAt.isBefore(newPayeeWindow)
                    && !beneficiaryCreatedAt.isAfter(transactionTime)) {
                newPayee = 1;
            }
        }

        // Account age in days.
        long accountAgeDays = 0;

        if (transaction.getUser().getCreatedAt() != null) {
            accountAgeDays = Math.max(
                    0,
                    Duration.between(
                            transaction.getUser().getCreatedAt(),
                            transactionTime
                    ).toDays()
            );
        }

        // Average completed transaction amount over the previous 30 days.
        BigDecimal averageAmount = transactionRepository
                .findAverageTransactionAmountSince(
                        userId,
                        TransactionStatus.COMPLETED,
                        transactionTime.minus(Duration.ofDays(30))
                )
                .orElse(BigDecimal.ZERO);

        double amountToAverageRatio = 0.0;

        if (averageAmount.compareTo(BigDecimal.ZERO) > 0) {
            amountToAverageRatio = amount.divide(
                    averageAmount,
                    MathContext.DECIMAL64
            ).doubleValue();
        }

        /*
         * Device, geographical distance and international status
         * are not currently collected by the platform.
         * Neutral defaults are used instead of invented values.
         */

        int newDevice = 0;
        double geoDistanceKm = 0.0;
        int international = 0;

        return new MLPredictionRequest(
                amount,
                transactionHour,
                isWeekend,
                (int) Math.min(transactionCount, Integer.MAX_VALUE),
                (int) Math.min(failedTransactions, Integer.MAX_VALUE),
                newDevice,
                newPayee,
                geoDistanceKm,
                (int) Math.min(accountAgeDays, Integer.MAX_VALUE),
                international,
                averageAmount,
                amountToAverageRatio
        );
    }
}