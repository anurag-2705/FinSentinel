package com.finsentinel.beneficiary.service;

import com.finsentinel.beneficiary.dto.UpdateBeneficiaryRequest;
import com.finsentinel.beneficiary.dto.AddBeneficiaryRequest;
import com.finsentinel.beneficiary.dto.BeneficiaryResponse;
import com.finsentinel.beneficiary.entity.Beneficiary;
import com.finsentinel.beneficiary.entity.BeneficiaryStatus;
import com.finsentinel.beneficiary.repository.BeneficiaryRepository;
import com.finsentinel.user.entity.AppUser;
import com.finsentinel.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;

    public BeneficiaryService(
            BeneficiaryRepository beneficiaryRepository,
            UserRepository userRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BeneficiaryResponse addBeneficiary(
            UUID userId,
            AddBeneficiaryRequest request) {

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String accountNumber = request.accountNumber().trim();
        String ifscCode = request.ifscCode().trim().toUpperCase();

        if (beneficiaryRepository.existsByUser_IdAndIfscCodeAndAccountNumber(
                userId, ifscCode, accountNumber)) {
            throw new IllegalArgumentException("Beneficiary already exists");
        }

        Beneficiary beneficiary = new Beneficiary();

        beneficiary.setUser(user);
        beneficiary.setBeneficiaryName(request.beneficiaryName().trim());
        beneficiary.setNickname(
                request.nickname() == null || request.nickname().isBlank()
                        ? null
                        : request.nickname().trim()
        );
        beneficiary.setBankName(request.bankName().trim());
        beneficiary.setAccountNumber(accountNumber);
        beneficiary.setIfscCode(ifscCode);
        beneficiary.setStatus(BeneficiaryStatus.ACTIVE);

        Beneficiary savedBeneficiary = beneficiaryRepository.save(beneficiary);

        return BeneficiaryResponse.from(savedBeneficiary);
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getBeneficiaries(UUID userId) {

        return beneficiaryRepository
                .findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(BeneficiaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BeneficiaryResponse getBeneficiary(UUID userId, UUID beneficiaryId) {

        Beneficiary beneficiary = beneficiaryRepository
                .findByIdAndUser_Id(beneficiaryId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found"));

        return BeneficiaryResponse.from(beneficiary);
    }
    @Transactional
public void deleteBeneficiary(UUID userId, UUID beneficiaryId) {

    Beneficiary beneficiary = beneficiaryRepository
            .findByIdAndUser_Id(beneficiaryId, userId)
            .orElseThrow(() ->
                    new IllegalArgumentException("Beneficiary not found"));

    beneficiaryRepository.delete(beneficiary);
}
@Transactional
public BeneficiaryResponse updateBeneficiary(
        UUID userId,
        UUID beneficiaryId,
        UpdateBeneficiaryRequest request) {

    Beneficiary beneficiary = beneficiaryRepository
            .findByIdAndUser_Id(beneficiaryId, userId)
            .orElseThrow(() ->
                    new IllegalArgumentException("Beneficiary not found"));

    String accountNumber = request.accountNumber().trim();
    String ifscCode = request.ifscCode().trim().toUpperCase();

    boolean duplicate = beneficiaryRepository
            .existsByUser_IdAndIfscCodeAndAccountNumberAndIdNot(
                    userId,
                    ifscCode,
                    accountNumber,
                    beneficiaryId
            );

    if (duplicate) {
        throw new IllegalArgumentException("Beneficiary already exists");
    }

    beneficiary.setBeneficiaryName(request.beneficiaryName().trim());

    beneficiary.setNickname(
            request.nickname() == null || request.nickname().isBlank()
                    ? null
                    : request.nickname().trim()
    );

    beneficiary.setBankName(request.bankName().trim());
    beneficiary.setAccountNumber(accountNumber);
    beneficiary.setIfscCode(ifscCode);

    Beneficiary updatedBeneficiary =
            beneficiaryRepository.save(beneficiary);

    return BeneficiaryResponse.from(updatedBeneficiary);
}
@Transactional
public BeneficiaryResponse blockBeneficiary(UUID userId, UUID beneficiaryId) {
    Beneficiary beneficiary = beneficiaryRepository
            .findByIdAndUser_Id(beneficiaryId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found"));

    beneficiary.setStatus(BeneficiaryStatus.BLOCKED);

    return BeneficiaryResponse.from(beneficiaryRepository.save(beneficiary));
}

@Transactional
public BeneficiaryResponse unblockBeneficiary(UUID userId, UUID beneficiaryId) {
    Beneficiary beneficiary = beneficiaryRepository
            .findByIdAndUser_Id(beneficiaryId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found"));

    beneficiary.setStatus(BeneficiaryStatus.ACTIVE);

    return BeneficiaryResponse.from(beneficiaryRepository.save(beneficiary));
}
}