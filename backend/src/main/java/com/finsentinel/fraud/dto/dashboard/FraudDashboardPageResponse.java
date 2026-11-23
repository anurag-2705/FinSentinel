package com.finsentinel.fraud.dto.dashboard;

import com.finsentinel.fraud.dto.FraudAlertResponse;

import java.util.List;

public record FraudDashboardPageResponse(

        List<FraudAlertResponse> alerts,

        int page,

        int size,

        long totalElements,

        int totalPages,

        boolean first,

        boolean last

) {
}