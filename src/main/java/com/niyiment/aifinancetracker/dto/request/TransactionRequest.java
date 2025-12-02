package com.niyiment.aifinancetracker.dto.request;

import com.niyiment.aifinancetracker.entity.Transaction;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionRequest(
    
    @NotBlank(message = "User ID is required")
    String userId,
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount,
    
    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    String category,
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,
    
    @NotNull(message = "Transaction type is required")
    Transaction.TransactionType transactionType,
    
    @NotNull(message = "Transaction date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    LocalDateTime transactionDate,
    
    @Size(max = 255, message = "Merchant name must not exceed 255 characters")
    String merchant,
    
    @Size(max = 255, message = "Location must not exceed 255 characters")
    String location
) {}