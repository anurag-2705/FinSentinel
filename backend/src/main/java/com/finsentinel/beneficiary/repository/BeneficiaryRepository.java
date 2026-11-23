package com.finsentinel.beneficiary.repository;

import com.finsentinel.beneficiary.entity.Beneficiary;
import com.finsentinel.beneficiary.entity.BeneficiaryStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {

    List<Beneficiary> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    Optional<Beneficiary> findByIdAndUser_Id(UUID id, UUID userId);

    Optional<Beneficiary> findByIdAndUser_IdAndStatus(
            UUID id,
            UUID userId,
            BeneficiaryStatus status
    );

    boolean existsByUser_IdAndIfscCodeAndAccountNumber(
            UUID userId,
            String ifscCode,
            String accountNumber
    );

    List<Beneficiary> findByUser_IdAndStatusOrderByCreatedAtDesc(
            UUID userId,
            BeneficiaryStatus status
    );

    boolean existsByUser_IdAndIfscCodeAndAccountNumberAndIdNot(
            UUID userId,
            String ifscCode,
            String accountNumber,
            UUID beneficiaryId
    );
}