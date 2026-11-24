package com.finsentinel.ml.dto;

import java.math.BigDecimal;

public record MLPredictionRequest(

        BigDecimal amount,

        int transaction_hour,

        int is_weekend,

        int transactions_last_1h,

        int failed_txns_24h,

        int new_device,

        int new_payee,

        double geo_distance_km,

        int account_age_days,

        int is_international,

        BigDecimal average_amount_30d,

        double amount_to_average_ratio

) {
}