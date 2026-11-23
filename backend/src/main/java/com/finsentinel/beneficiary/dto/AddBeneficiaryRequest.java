package com.finsentinel.beneficiary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddBeneficiaryRequest(

        @NotBlank(message = "Beneficiary name is required")
        @Size(max = 100, message = "Beneficiary name cannot exceed 100 characters")
        String beneficiaryName,

        @Size(max = 50, message = "Nickname cannot exceed 50 characters")
        String nickname,

        @NotBlank(message = "Bank name is required")
        @Size(max = 100, message = "Bank name cannot exceed 100 characters")
        String bankName,

        @NotBlank(message = "Account number is required")
        @Size(min = 5, max = 34, message = "Account number must contain 5 to 34 characters")
        @Pattern(regexp = "^[0-9]+$", message = "Account number must contain only digits")
        String accountNumber,

        @NotBlank(message = "IFSC code is required")
        @Pattern(
                regexp = "^[A-Z]{4}0[A-Z0-9]{6}$",
                message = "Invalid IFSC code format"
        )
        String ifscCode

) {
}
