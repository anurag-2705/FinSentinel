
package com.finsentinel.transaction.service;

import com.finsentinel.transaction.dto.TransactionFilterRequest;
import com.finsentinel.transaction.dto.TransactionPageResponse;
import com.finsentinel.transaction.dto.TransactionResponse;
import com.finsentinel.transaction.entity.FinancialTransaction;
import com.finsentinel.transaction.repository.FinancialTransactionRepository;
import com.finsentinel.transaction.repository.TransactionSpecifications;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TransactionHistoryService {

    private final FinancialTransactionRepository transactionRepository;

    public TransactionHistoryService(
            FinancialTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(UUID userId) {

        List<FinancialTransaction> transactions =
                transactionRepository.findByUser_IdOrderByCreatedAtDesc(userId);

        return transactions.stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionPageResponse getFilteredTransactionHistory(
            UUID userId,
            TransactionFilterRequest request) {

        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? 10 : request.size();

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }

        if (request.startDate() != null
                && request.endDate() != null
                && !request.startDate().isBefore(request.endDate())) {

            throw new IllegalArgumentException(
                    "Start date must be earlier than end date"
            );
        }

        Page<FinancialTransaction> transactionPage =
                transactionRepository.findAll(
                        TransactionSpecifications.filtered(userId, request),
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Direction.DESC, "createdAt")
                        )
                );

        List<TransactionResponse> transactions =
                transactionPage.getContent()
                        .stream()
                        .map(TransactionResponse::from)
                        .toList();

        return new TransactionPageResponse(
                transactions,
                transactionPage.getNumber(),
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages(),
                transactionPage.isFirst(),
                transactionPage.isLast()
        );
    }
}
