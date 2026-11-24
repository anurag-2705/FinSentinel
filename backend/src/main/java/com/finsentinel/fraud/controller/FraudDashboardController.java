package com.finsentinel.fraud.controller;

import com.finsentinel.fraud.dto.dashboard.FraudRuleFrequencyResponse;
import com.finsentinel.fraud.dto.dashboard.FraudAnalystWorkloadResponse;
import com.finsentinel.fraud.dto.dashboard.FraudDashboardPageResponse;
import com.finsentinel.fraud.dto.dashboard.FraudDashboardSummaryResponse;

import com.finsentinel.fraud.dto.dashboard.FraudAnalystPerformanceResponse;
import com.finsentinel.fraud.dto.dashboard.FraudAnalyticsSummaryResponse;
import com.finsentinel.fraud.dto.dashboard.FraudDashboardActivityResponse;
import com.finsentinel.fraud.entity.FraudAlertStatus;
import com.finsentinel.fraud.service.FraudDashboardService;

import com.finsentinel.fraud.dto.dashboard.FraudRiskDistributionResponse;
import com.finsentinel.transaction.entity.TransactionType;
import com.finsentinel.fraud.dto.dashboard.FraudDashboardTrendResponse;

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

@GetMapping("/analytics/daily-trends")
public ResponseEntity<List<FraudDashboardTrendResponse>> getDailyFraudTrends(
        @RequestParam(defaultValue = "7D") String period) {

    return ResponseEntity.ok(
            fraudDashboardService.getDailyFraudTrends(period)
    );
}
@GetMapping("/analytics/summary")
public ResponseEntity<FraudAnalyticsSummaryResponse> getAnalyticsSummary(
        @RequestParam(defaultValue = "7D") String period) {

    return ResponseEntity.ok(
            fraudDashboardService.getAnalyticsSummary(period)
    );
}

    // Dashboard summary

    @GetMapping("/summary")
    public ResponseEntity<FraudDashboardSummaryResponse> getSummary() {

        return ResponseEntity.ok(
                fraudDashboardService.getSummary()
        );
    }

// Daily transaction activity

@GetMapping("/activity")
public ResponseEntity<List<FraudDashboardActivityResponse>> getActivity() {

    return ResponseEntity.ok(
            fraudDashboardService.getActivity()
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

    @GetMapping("/analytics/analyst-performance")
public ResponseEntity<List<FraudAnalystPerformanceResponse>> getAnalystPerformance(
        @RequestParam(defaultValue = "7D") String period) {

    return ResponseEntity.ok(
            fraudDashboardService.getAnalystPerformance(period)
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
    @GetMapping("/analytics/top-rules")
public ResponseEntity<List<FraudRuleFrequencyResponse>> getTopFraudRules(
        @RequestParam(defaultValue = "7D") String period) {

    return ResponseEntity.ok(
            fraudDashboardService.getTopFraudRules(period)
    );
}
@GetMapping("/analytics/risk-distribution")
public ResponseEntity<List<FraudRiskDistributionResponse>> getRiskDistribution(
        @RequestParam(defaultValue = "7D") String period) {

    return ResponseEntity.ok(
            fraudDashboardService.getRiskDistribution(period)
    );
}
}