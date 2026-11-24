package com.finsentinel.ml.dto;

public record MLFeatureExplanation(
        String feature,
        String label,
        double value,
        double shap_value,
        String impact
) {
}