package com.niyiment.aifinancetracker.dto.response;

import com.niyiment.aifinancetracker.dto.request.AdvisorQueryRequest;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record AdvisorResponse(
        String advice,
        String llmProvider,
        List<String> relevantDocuments,
        LocalDateTime timestamp
) {
}
