package com.finsentinel.transaction.repository;

import com.finsentinel.transaction.entity.LedgerEntry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository
        extends JpaRepository<LedgerEntry, UUID> {

    // Find all ledger entries for a transaction
    List<LedgerEntry> findByTransaction_Id(UUID transactionId);

    // Find all entries belonging to a ledger account
    List<LedgerEntry> findByLedgerAccount_IdOrderByCreatedAtDesc(
            UUID ledgerAccountId
    );

    // Find all entries for a particular transaction
    boolean existsByTransaction_Id(UUID transactionId);
}