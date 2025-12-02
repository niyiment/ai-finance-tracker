package com.niyiment.aifinancetracker.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record FraudDetectedEvent(
    Long alertId,
    Long transactionId,
    String userId,
    BigDecimal fraudScore,
    String reason,
    LocalDateTime detectedAt
) {}