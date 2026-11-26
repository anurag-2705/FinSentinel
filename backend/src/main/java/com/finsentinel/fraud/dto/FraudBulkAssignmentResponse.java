package com.finsentinel.fraud.dto;

import java.util.List;

public record FraudBulkAssignmentResponse(
        int totalProcessed,
        int successfulAssignments,
        int failedAssignments,
        List<String> messages
) {
}