package com.finsentinel.ml.dto;

import java.util.List;

public record MLExplanationResponse(
        boolean success,
        String model,
        double fraud_probability,
        double fraud_percentage,
        String risk_level,
        double threshold,
        List<MLFeatureExplanation> top_factors,
        List<MLFeatureExplanation> risk_increasing_factors,
        List<MLFeatureExplanation> risk_decreasing_factors,
        String disclaimer,
        List<String> unavailable_features
) {
}