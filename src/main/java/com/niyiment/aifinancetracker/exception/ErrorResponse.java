package com.niyiment.aifinancetracker.exception;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ErrorResponse(
    String message,
    int status,
    LocalDateTime timestamp,
    String path,
    List<String> errors
) {}