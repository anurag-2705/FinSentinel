package com.finsentinel.ml.dto;

public record MLPredictionResponse(

        boolean success,

        String model,

        double fraud_probability,

        double fraud_percentage,

        double threshold,

        boolean is_fraud,

        String risk_level,

        String message,

        String disclaimer

) {
}