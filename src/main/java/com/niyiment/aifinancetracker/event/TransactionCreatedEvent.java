package com.niyiment.aifinancetracker.event;

import com.niyiment.aifinancetracker.entity.Transaction;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TransactionCreatedEvent(
        Long transactionId,
        String userId,
        BigDecimal amount,
        String category,
        Transaction.TransactionType transactionType,
        LocalDateTime transactionDate,
        String merchant,
        String location,
        LocalDateTime eventTime
) {}
