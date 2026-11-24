package com.finsentinel.fraud.service;

import com.finsentinel.fraud.entity.FraudAlert;
import com.finsentinel.fraud.repository.FraudAlertRepository;
import com.finsentinel.ml.dto.MLExplanationResponse;
import com.finsentinel.ml.dto.MLPredictionRequest;
import com.finsentinel.ml.service.MLFeatureExtractionService;
import com.finsentinel.ml.service.MLPredictionService;
import com.finsentinel.transaction.entity.FinancialTransaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FraudAlertExplanationService {

    private static final List<String> DEFAULT_UNAVAILABLE_FEATURES =
            List.of("new_device", "geo_distance_km", "is_international");

    private final FraudAlertRepository fraudAlertRepository;
    private final MLFeatureExtractionService mlFeatureExtractionService;
    private final MLPredictionService mlPredictionService;

    public FraudAlertExplanationService(
            FraudAlertRepository fraudAlertRepository,
            MLFeatureExtractionService mlFeatureExtractionService,
            MLPredictionService mlPredictionService
    ) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.mlFeatureExtractionService = mlFeatureExtractionService;
        this.mlPredictionService = mlPredictionService;
    }

    @Transactional(readOnly = true)
    public MLExplanationResponse getExplanation(UUID alertId) {

        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Fraud alert not found")
                );

        var assessment = alert.getAssessment();

        Map<String, Object> snapshot =
                assessment.getMlFeatureSnapshot();

        MLPredictionRequest features;
        List<String> unavailableFeatures;

        if (snapshot != null && !snapshot.isEmpty()) {

            // Use the exact features saved during the original assessment.
            features = convertSnapshotToRequest(snapshot);
            unavailableFeatures = getUnavailableFeatures(snapshot);

        } else {

            // Backward compatibility for assessments created before V15.
            FinancialTransaction transaction =
                    assessment.getTransaction();

            features = mlFeatureExtractionService.extractFeatures(transaction);

            // These inputs have historically used neutral defaults.
            unavailableFeatures = DEFAULT_UNAVAILABLE_FEATURES;
        }

        MLExplanationResponse explanation =
                mlPredictionService.explain(features);

        // Add metadata without changing the Python model inputs.
        return new MLExplanationResponse(
                explanation.success(),
                explanation.model(),
                explanation.fraud_probability(),
                explanation.fraud_percentage(),
                explanation.risk_level(),
                explanation.threshold(),
                explanation.top_factors(),
                explanation.risk_increasing_factors(),
                explanation.risk_decreasing_factors(),
                explanation.disclaimer(),
                unavailableFeatures
        );
    }

    private List<String> getUnavailableFeatures(
            Map<String, Object> snapshot
    ) {
        Object metadataObject = snapshot.get("_metadata");

        if (metadataObject instanceof Map<?, ?> metadata) {

            Object unavailableObject =
                    metadata.get("unavailable_features");

            if (unavailableObject instanceof List<?> features) {

                return features.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .toList();
            }
        }

        // Older snapshots predate the metadata field.
        return DEFAULT_UNAVAILABLE_FEATURES;
    }

    private MLPredictionRequest convertSnapshotToRequest(
            Map<String, Object> snapshot
    ) {
        return new MLPredictionRequest(
                getBigDecimal(snapshot, "amount"),
                getInt(snapshot, "transaction_hour"),
                getInt(snapshot, "is_weekend"),
                getInt(snapshot, "transactions_last_1h"),
                getInt(snapshot, "failed_txns_24h"),
                getInt(snapshot, "new_device"),
                getInt(snapshot, "new_payee"),
                getDouble(snapshot, "geo_distance_km"),
                getInt(snapshot, "account_age_days"),
                getInt(snapshot, "is_international"),
                getBigDecimal(snapshot, "average_amount_30d"),
                getDouble(snapshot, "amount_to_average_ratio")
        );
    }

    private Number getNumber(Map<String, Object> snapshot, String key) {
        Object value = snapshot.get(key);

        if (!(value instanceof Number number)) {
            throw new IllegalStateException(
                    "Missing or invalid ML feature in snapshot: " + key
            );
        }

        return number;
    }

    private BigDecimal getBigDecimal(Map<String, Object> snapshot, String key) {
        Number number = getNumber(snapshot, key);

        if (number instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }

        return new BigDecimal(number.toString());
    }

    private int getInt(Map<String, Object> snapshot, String key) {
        return getNumber(snapshot, key).intValue();
    }

    private double getDouble(Map<String, Object> snapshot, String key) {
        return getNumber(snapshot, key).doubleValue();
    }
}