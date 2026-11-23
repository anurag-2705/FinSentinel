
package com.finsentinel.fraud.controller;

import com.finsentinel.fraud.dto.FraudAssessmentResponse;
import com.finsentinel.fraud.service.FraudDetectionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fraud")
public class FraudDetectionController {

    private final FraudDetectionService fraudDetectionService;

    public FraudDetectionController(
            FraudDetectionService fraudDetectionService
    ) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @PostMapping("/assessments/{transactionId}")
    public ResponseEntity<FraudAssessmentResponse> assessTransaction(
            @PathVariable UUID transactionId
    ) {
        FraudAssessmentResponse response =
                fraudDetectionService.assessTransaction(transactionId);

        return ResponseEntity.ok(response);
    }
}
