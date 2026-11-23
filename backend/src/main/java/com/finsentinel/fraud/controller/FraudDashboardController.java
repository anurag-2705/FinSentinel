package com.finsentinel.fraud.controller;

import com.finsentinel.fraud.dto.dashboard.FraudAnalystWorkloadResponse;
import com.finsentinel.fraud.dto.dashboard.FraudDashboardPageResponse;
import com.finsentinel.fraud.dto.dashboard.FraudDashboardSummaryResponse;

import com.finsentinel.fraud.entity.FraudAlertStatus;
import com.finsentinel.fraud.service.FraudDashboardService;

import com.finsentinel.transaction.entity.TransactionType;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fraud/dashboard")
public class FraudDashboardController {

    private final FraudDashboardService fraudDashboardService;

    public FraudDashboardController(
            FraudDashboardService fraudDashboardService
    ) {
        this.fraudDashboardService = fraudDashboardService;
    }

    // Dashboard summary

    @GetMapping("/summary")
    public ResponseEntity<FraudDashboardSummaryResponse> getSummary() {

        return ResponseEntity.ok(
                fraudDashboardService.getSummary()
        );
    }

    // Analyst workload

    @GetMapping("/analyst-workload")
    public ResponseEntity<List<FraudAnalystWorkloadResponse>>
            getAnalystWorkload() {

        return ResponseEntity.ok(
                fraudDashboardService.getAnalystWorkload()
        );
    }

    // Paginated and filtered alerts

    @GetMapping("/alerts")
    public ResponseEntity<FraudDashboardPageResponse> getAlerts(

            @RequestParam(required = false)
            FraudAlertStatus status,

            @RequestParam(required = false)
            Integer minimumRiskScore,

            @RequestParam(required = false)
            Integer maximumRiskScore,

            @RequestParam(required = false)
            TransactionType transactionType,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size,

            @RequestParam(defaultValue = "createdAt")
            String sortBy,

            @RequestParam(defaultValue = "DESC")
            String direction

    ) {

        return ResponseEntity.ok(
                fraudDashboardService.getAlerts(
                        status,
                        minimumRiskScore,
                        maximumRiskScore,
                        transactionType,
                        page,
                        size,
                        sortBy,
                        direction
                )
        );
    }
}