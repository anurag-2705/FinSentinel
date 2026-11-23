package com.finsentinel.fraud.controller;

import com.finsentinel.fraud.dto.FraudAlertResponse;
import com.finsentinel.fraud.dto.FraudAlertReviewResponse;
import com.finsentinel.fraud.dto.FraudReviewRequest;
import com.finsentinel.fraud.entity.FraudAlertStatus;
import com.finsentinel.fraud.service.FraudAlertReviewService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fraud/alerts")
public class FraudAlertReviewController {

    private final FraudAlertReviewService fraudAlertReviewService;

    public FraudAlertReviewController(
            FraudAlertReviewService fraudAlertReviewService
    ) {
        this.fraudAlertReviewService = fraudAlertReviewService;
    }

    @GetMapping
    public ResponseEntity<List<FraudAlertResponse>> getAlerts(
            @RequestParam(required = false) FraudAlertStatus status
    ) {
        return ResponseEntity.ok(
                fraudAlertReviewService.getAlerts(status)
        );
    }

    @GetMapping("/{alertId}")
    public ResponseEntity<FraudAlertResponse> getAlert(
            @PathVariable UUID alertId
    ) {
        return ResponseEntity.ok(
                fraudAlertReviewService.getAlert(alertId)
        );
    }

    // Get complete review history of a fraud alert.
    @GetMapping("/{alertId}/reviews")
    public ResponseEntity<List<FraudAlertReviewResponse>> getReviewHistory(
            @PathVariable UUID alertId
    ) {
        return ResponseEntity.ok(
                fraudAlertReviewService.getReviewHistory(alertId)
        );
    }

    @PostMapping("/{alertId}/assign")
    public ResponseEntity<FraudAlertResponse> assignAlert(
            @PathVariable UUID alertId,
            Authentication authentication
    ) {
        UUID analystId = (UUID) authentication.getPrincipal();

        return ResponseEntity.ok(
                fraudAlertReviewService.assignAlert(
                        alertId,
                        analystId
                )
        );
    }

    @PostMapping("/{alertId}/approve")
    public ResponseEntity<FraudAlertResponse> approveAlert(
            @PathVariable UUID alertId,
            @Valid @RequestBody FraudReviewRequest request,
            Authentication authentication
    ) {
        UUID analystId = (UUID) authentication.getPrincipal();

        return ResponseEntity.ok(
                fraudAlertReviewService.approveAlert(
                        alertId,
                        analystId,
                        request.notes()
                )
        );
    }

    @PostMapping("/{alertId}/reject")
    public ResponseEntity<FraudAlertResponse> rejectAlert(
            @PathVariable UUID alertId,
            @Valid @RequestBody FraudReviewRequest request,
            Authentication authentication
    ) {
        UUID analystId = (UUID) authentication.getPrincipal();

        return ResponseEntity.ok(
                fraudAlertReviewService.rejectAlert(
                        alertId,
                        analystId,
                        request.notes()
                )
        );
    }
}