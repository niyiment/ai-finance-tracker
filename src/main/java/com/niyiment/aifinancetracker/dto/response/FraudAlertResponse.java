package com.niyiment.aifinancetracker.dto.response;

import com.niyiment.aifinancetracker.entity.FraudAlert;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record FraudAlertResponse(
    Long id,
    Long transactionId,
    String userId,
    BigDecimal fraudScore,
    String reason,
    FraudAlert.AlertStatus status,
    LocalDateTime detectedAt,
    LocalDateTime resolvedAt
) {}