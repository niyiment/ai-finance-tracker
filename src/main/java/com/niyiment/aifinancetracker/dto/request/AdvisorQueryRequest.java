package com.niyiment.aifinancetracker.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@Builder
public record AdvisorQueryRequest(
        String userId,
        String query,
        LlmProvider provider,
        Boolean includeDocumentContext
) {

    public enum LlmProvider{
        OLLAMA,
        OPENAI
    }
}
