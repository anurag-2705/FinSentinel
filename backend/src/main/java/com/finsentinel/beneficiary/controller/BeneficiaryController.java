package com.finsentinel.beneficiary.controller;

import com.finsentinel.beneficiary.dto.AddBeneficiaryRequest;
import com.finsentinel.beneficiary.dto.BeneficiaryResponse;
import com.finsentinel.beneficiary.dto.UpdateBeneficiaryRequest;
import com.finsentinel.beneficiary.service.BeneficiaryService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @PostMapping
    public ResponseEntity<BeneficiaryResponse> addBeneficiary(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody AddBeneficiaryRequest request) {

        BeneficiaryResponse response =
                beneficiaryService.addBeneficiary(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<BeneficiaryResponse>> getBeneficiaries(
            @AuthenticationPrincipal UUID userId) {

        return ResponseEntity.ok(
                beneficiaryService.getBeneficiaries(userId)
        );
    }

    @GetMapping("/{beneficiaryId}")
    public ResponseEntity<BeneficiaryResponse> getBeneficiary(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID beneficiaryId) {

        return ResponseEntity.ok(
                beneficiaryService.getBeneficiary(userId, beneficiaryId)
        );
    }
    @DeleteMapping("/{beneficiaryId}")
public ResponseEntity<Void> deleteBeneficiary(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID beneficiaryId) {

    beneficiaryService.deleteBeneficiary(userId, beneficiaryId);

    return ResponseEntity.noContent().build();
}
@PutMapping("/{beneficiaryId}")
public ResponseEntity<BeneficiaryResponse> updateBeneficiary(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID beneficiaryId,
        @Valid @RequestBody UpdateBeneficiaryRequest request) {

    BeneficiaryResponse response = beneficiaryService.updateBeneficiary(
            userId,
            beneficiaryId,
            request
    );

    return ResponseEntity.ok(response);
}
@PatchMapping("/{beneficiaryId}/block")
public ResponseEntity<BeneficiaryResponse> blockBeneficiary(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID beneficiaryId) {

    return ResponseEntity.ok(
            beneficiaryService.blockBeneficiary(userId, beneficiaryId)
    );
}

@PatchMapping("/{beneficiaryId}/unblock")
public ResponseEntity<BeneficiaryResponse> unblockBeneficiary(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID beneficiaryId) {

    return ResponseEntity.ok(
            beneficiaryService.unblockBeneficiary(userId, beneficiaryId)
    );
}
}