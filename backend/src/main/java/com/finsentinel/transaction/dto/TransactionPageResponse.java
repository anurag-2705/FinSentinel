package com.finsentinel.transaction.dto;

import java.util.List;

public record TransactionPageResponse(

        List<TransactionResponse> transactions,

        int currentPage,

        int pageSize,

        long totalElements,

        int totalPages,

        boolean first,

        boolean last

) {
}