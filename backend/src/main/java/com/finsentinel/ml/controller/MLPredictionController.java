package com.finsentinel.ml.controller;

import com.finsentinel.ml.dto.MLPredictionRequest;
import com.finsentinel.ml.dto.MLPredictionResponse;
import com.finsentinel.ml.service.MLPredictionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ml")
public class MLPredictionController {

    private final MLPredictionService mlPredictionService;

    public MLPredictionController(
            MLPredictionService mlPredictionService
    ) {
        this.mlPredictionService = mlPredictionService;
    }

    @PostMapping("/predict")
    public ResponseEntity<MLPredictionResponse> predictFraud(
            @RequestBody MLPredictionRequest request
    ) {
        MLPredictionResponse response =
                mlPredictionService.predict(request);

        return ResponseEntity.ok(response);
    }
}