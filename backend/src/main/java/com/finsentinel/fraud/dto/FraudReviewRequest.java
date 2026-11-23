package com.finsentinel.fraud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FraudReviewRequest(

        @NotBlank(message = "Review notes are required")
        @Size(max = 2000, message = "Review notes cannot exceed 2000 characters")
        String notes

) {}