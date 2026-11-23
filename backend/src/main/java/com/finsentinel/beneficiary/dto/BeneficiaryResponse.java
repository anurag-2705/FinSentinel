package com.finsentinel.beneficiary.dto;

import com.finsentinel.beneficiary.entity.Beneficiary;
import com.finsentinel.beneficiary.entity.BeneficiaryStatus;

import java.time.Instant;
import java.util.UUID;

public record BeneficiaryResponse(

        UUID id,
        String beneficiaryName,
        String nickname,
        String bankName,
        String accountNumber,
        String ifscCode,
        BeneficiaryStatus status,
        Instant createdAt

) {

    public static BeneficiaryResponse from(Beneficiary beneficiary) {

        return new BeneficiaryResponse(
                beneficiary.getId(),
                beneficiary.getBeneficiaryName(),
                beneficiary.getNickname(),
                beneficiary.getBankName(),
                maskAccountNumber(beneficiary.getAccountNumber()),
                beneficiary.getIfscCode(),
                beneficiary.getStatus(),
                beneficiary.getCreatedAt()
        );
    }

    private static String maskAccountNumber(String accountNumber) {

        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }

        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}