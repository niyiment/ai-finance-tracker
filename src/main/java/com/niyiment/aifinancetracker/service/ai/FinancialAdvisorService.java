package com.niyiment.aifinancetracker.service.ai;

import com.niyiment.aifinancetracker.dto.request.AdvisorQueryRequest;
import com.niyiment.aifinancetracker.dto.response.AdvisorResponse;
import com.niyiment.aifinancetracker.entity.DocumentEmbedding;
import com.niyiment.aifinancetracker.service.query.TransactionQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialAdvisorService {
    
    private final LlmService llmService;
    private final DocumentEmbeddingService documentEmbeddingService;
    private final TransactionQueryService transactionQueryService;
    
    private static final int MAX_RELEVANT_DOCUMENTS = 3;
    private static final int DAYS_FOR_CONTEXT = 90;
    
    public AdvisorResponse getFinancialAdvice(AdvisorQueryRequest request) {
        log.info("Processing financial advice request for user: {}", request.userId());
        
        // Build comprehensive context
        String context = buildAdviceContext(request);
        
        // Generate advice using LLM
        String advice = llmService.generateAdvice(
            request.query(),
            context,
            request.provider()
        );
        
        // Get relevant document names for response
        List<String> relevantDocs = getRelevantDocumentNames(request.query(), request.includeDocumentContext());
        
        return AdvisorResponse.builder()
            .advice(advice)
            .llmProvider(request.provider().name())
            .relevantDocuments(relevantDocs)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    private String buildAdviceContext(AdvisorQueryRequest request) {
        StringBuilder context = new StringBuilder();
        
        // Add user's financial summary
        context.append("User's Financial Summary (Last ").append(DAYS_FOR_CONTEXT).append(" days):\n");
        Map<String, Object> summary = transactionQueryService
            .getUserFinancialSummary(request.userId(), DAYS_FOR_CONTEXT);
        
        context.append("Total Income: $").append(summary.get("totalIncome")).append("\n");
        context.append("Total Expenses: $").append(summary.get("totalExpenses")).append("\n");
        context.append("Total Investments: $").append(summary.get("totalInvestments")).append("\n");
        context.append("Net Savings: $").append(summary.get("netSavings")).append("\n\n");
        
        // Add relevant document context if requested
        if (Boolean.TRUE.equals(request.includeDocumentContext())) {
            List<DocumentEmbedding> relevantDocs = documentEmbeddingService
                .findRelevantDocuments(request.query(), MAX_RELEVANT_DOCUMENTS);
            
            if (!relevantDocs.isEmpty()) {
                context.append("Relevant Financial Knowledge:\n\n");
                context.append(documentEmbeddingService.buildContextFromDocuments(relevantDocs));
                context.append("\n\n");
            }
        }
        
        return context.toString();
    }
    
    private List<String> getRelevantDocumentNames(String query, Boolean includeContext) {
        if (Boolean.FALSE.equals(includeContext)) {
            return List.of();
        }
        
        return documentEmbeddingService
            .findRelevantDocuments(query, MAX_RELEVANT_DOCUMENTS)
            .stream()
            .map(DocumentEmbedding::getDocumentName)
            .distinct()
            .collect(Collectors.toList());
    }
}