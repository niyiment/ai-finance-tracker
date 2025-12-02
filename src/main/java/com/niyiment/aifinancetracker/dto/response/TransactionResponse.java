package com.niyiment.aifinancetracker.dto.response;

import com.niyiment.aifinancetracker.entity.Transaction;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TransactionResponse(
    Long id,
    String userId,
    BigDecimal amount,

    String category,
    String description,
    Transaction.TransactionType transactionType,

    LocalDateTime transactionDate,

    String merchant,

    String location,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}