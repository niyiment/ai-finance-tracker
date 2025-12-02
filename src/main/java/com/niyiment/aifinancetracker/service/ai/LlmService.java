package com.niyiment.aifinancetracker.service.ai;

import com.niyiment.aifinancetracker.dto.request.AdvisorQueryRequest;
import com.niyiment.aifinancetracker.exception.LlmProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmService {
    
    @Qualifier("ollamaChatClient")
    private final ChatClient ollamaChatClient;
    
    @Qualifier("openaiChatClient")
    private final ChatClient openaiChatClient;
    
    @Value("${finance.ai.default-provider}")
    private String defaultProvider;
    
    private static final String SYSTEM_PROMPT = """
        You are an expert financial advisor with deep knowledge of personal finance,
        investment strategies, stock management, and financial planning.
        
        Your role is to provide accurate, helpful, and actionable financial advice
        based on the user's query and their financial data.
        
        Guidelines:
        - Be specific and practical in your recommendations
        - Consider risk tolerance and time horizons
        - Cite relevant financial principles when applicable
        - Always remind users that this is educational advice, not professional financial planning
        - Be concise but thorough
        - Use the provided context from financial documents to support your advice
        """;
    
    public String generateAdvice(String userQuery, String context, AdvisorQueryRequest.LlmProvider provider) {
        log.info("Generating advice using provider: {}", provider);
        
        try {
            ChatClient client = selectChatClient(provider);
            
            String fullPrompt = buildPrompt(userQuery, context);
            
            String response = client.prompt()
                .system(SYSTEM_PROMPT)
                .user(fullPrompt)
                .call()
                .content();
            
            log.debug("Generated advice successfully");
            return response;
            
        } catch (Exception e) {
            log.error("Failed to generate advice", e);
            throw new LlmProcessingException("Failed to generate financial advice", e);
        }
    }
    
    private ChatClient selectChatClient(AdvisorQueryRequest.LlmProvider provider) {
        if (provider == null) {
            provider = AdvisorQueryRequest.LlmProvider.valueOf(defaultProvider.toUpperCase());
        }
        
        return switch (provider) {
            case OPENAI -> {
                log.debug("Using OpenAI chat client");
                yield openaiChatClient;
            }
            case OLLAMA -> {
                log.debug("Using Ollama chat client");
                yield ollamaChatClient;
            }
        };
    }
    
    private String buildPrompt(String userQuery, String context) {
        if (context != null && !context.isBlank()) {
            return String.format("""
                Based on the following financial knowledge and context:
                
                %s
                
                Please answer the following question:
                %s
                
                Provide specific, actionable advice referencing the context where relevant.
                """, context, userQuery);
        } else {
            return userQuery;
        }
    }
    
    public String analyzeFraudPattern(String transactionDetails) {
        log.debug("Analyzing transaction for fraud patterns");
        
        String prompt = String.format("""
            Analyze the following transaction for potential fraud indicators.
            Consider factors like:
            - Unusual amount patterns
            - Location anomalies
            - Merchant reputation
            - Transaction timing
            - Spending patterns
            
            Transaction Details:
            %s
            
            Provide a fraud risk assessment (LOW, MEDIUM, HIGH) and explain the reasoning.
            Format your response as: RISK_LEVEL|Explanation
            Example: HIGH|Transaction amount is 500%% higher than average monthly spending
            """, transactionDetails);
        
        try {
            return ollamaChatClient.prompt()
                .user(prompt)
                .call()
                .content();
        } catch (Exception e) {
            log.error("Failed to analyze fraud pattern", e);
            throw new LlmProcessingException("Failed to analyze transaction for fraud", e);
        }
    }
}