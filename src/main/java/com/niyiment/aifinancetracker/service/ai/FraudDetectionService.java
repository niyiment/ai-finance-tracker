package com.niyiment.aifinancetracker.service.ai;

import com.niyiment.aifinancetracker.dto.response.FraudAlertResponse;
import com.niyiment.aifinancetracker.entity.FraudAlert;
import com.niyiment.aifinancetracker.entity.Transaction;
import com.niyiment.aifinancetracker.event.FraudDetectedEvent;
import com.niyiment.aifinancetracker.event.TransactionCreatedEvent;
import com.niyiment.aifinancetracker.exception.FraudDetectionException;
import com.niyiment.aifinancetracker.exception.ResourceNotFoundException;
import com.niyiment.aifinancetracker.repository.FraudAlertRepository;
import com.niyiment.aifinancetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {
    
    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionRepository transactionRepository;
    private final LlmService llmService;
    private final KafkaTemplate<String, FraudDetectedEvent> kafkaTemplate;
    
    @Value("${finance.kafka.topics.fraud-detected}")
    private String fraudDetectedTopic;
    
    @Value("${finance.ai.fraud-detection.threshold}")
    private BigDecimal fraudThreshold;
    
    @Value("${finance.ai.fraud-detection.enabled}")
    private boolean fraudDetectionEnabled;
    
    @KafkaListener(
        topics = "${finance.kafka.topics.transaction-created}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        if (!fraudDetectionEnabled) {
            return;
        }
        
        log.info("Processing transaction for fraud detection: {}", event.transactionId());
        
        try {
            Transaction transaction = transactionRepository
                .findById(event.transactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
            
            FraudAnalysisResult result = analyzeTransaction(transaction);
            
            if (result.isFraudulent()) {
                createFraudAlert(transaction, result);
            }
            
        } catch (Exception e) {
            log.error("Fraud detection failed for transaction: {}", event.transactionId(), e);
            throw new FraudDetectionException("Failed to process fraud detection", e);
        }
    }
    
    private FraudAnalysisResult analyzeTransaction(Transaction transaction) {
        // Build transaction context
        String transactionDetails = buildTransactionContext(transaction);
        
        // Get AI analysis
        String aiAnalysis = llmService.analyzeFraudPattern(transactionDetails);
        
        // Parse AI response
        return parseAiAnalysis(aiAnalysis);
    }
    
    private String buildTransactionContext(Transaction transaction) {
        // Get user's transaction history for comparison
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Transaction> recentTransactions = transactionRepository
            .findRecentTransactionsByUser(transaction.getUserId(), thirtyDaysAgo);
        
        StringBuilder context = new StringBuilder();
        context.append("Current Transaction:\n");
        context.append("Amount: $").append(transaction.getAmount()).append("\n");
        context.append("Category: ").append(transaction.getCategory()).append("\n");
        context.append("Merchant: ").append(transaction.getMerchant()).append("\n");
        context.append("Location: ").append(transaction.getLocation()).append("\n");
        context.append("Time: ").append(transaction.getTransactionDate()).append("\n\n");
        
        context.append("Recent Transaction History:\n");
        recentTransactions.stream().limit(10).forEach(t -> {
            context.append("- $").append(t.getAmount())
                   .append(" at ").append(t.getMerchant())
                   .append(" (").append(t.getCategory()).append(")\n");
        });
        
        return context.toString();
    }
    
    private FraudAnalysisResult parseAiAnalysis(String aiResponse) {
        // Expected format: RISK_LEVEL|Explanation
        String[] parts = aiResponse.split("\\|", 2);
        
        if (parts.length < 2) {
            log.warn("Invalid AI response format, defaulting to LOW risk");
            return new FraudAnalysisResult(BigDecimal.valueOf(0.2), "Unable to parse AI response", false);
        }
        
        String riskLevel = parts[0].trim();
        String explanation = parts[1].trim();
        
        BigDecimal fraudScore = switch (riskLevel.toUpperCase()) {
            case "HIGH" -> BigDecimal.valueOf(0.9);
            case "MEDIUM" -> BigDecimal.valueOf(0.6);
            default -> BigDecimal.valueOf(0.2);
        };
        
        boolean isFraudulent = fraudScore.compareTo(fraudThreshold) >= 0;
        
        return new FraudAnalysisResult(fraudScore, explanation, isFraudulent);
    }
    
    @CacheEvict(value = "fraudAlerts", key = "#transaction.userId")
    public void createFraudAlert(Transaction transaction, FraudAnalysisResult result) {
        log.warn("Fraud detected for transaction: {} (score: {})", 
                 transaction.getId(), result.fraudScore());
        
        FraudAlert alert = FraudAlert.builder()
            .transaction(transaction)
            .userId(transaction.getUserId())
            .fraudScore(result.fraudScore())
            .reason(result.reason())
            .status(FraudAlert.AlertStatus.PENDING)
            .build();
        
        FraudAlert saved = fraudAlertRepository.save(alert);
        
        publishFraudDetectedEvent(saved);
    }
    
    private void publishFraudDetectedEvent(FraudAlert alert) {
        FraudDetectedEvent event = FraudDetectedEvent.builder()
            .alertId(alert.getId())
            .transactionId(alert.getTransaction().getId())
            .userId(alert.getUserId())
            .fraudScore(alert.getFraudScore())
            .reason(alert.getReason())
            .detectedAt(alert.getDetectedAt())
            .build();
        
        kafkaTemplate.send(fraudDetectedTopic, alert.getUserId(), event);
        log.info("Published fraud detected event for alert: {}", alert.getId());
    }
    
    public List<FraudAlertResponse> getUserFraudAlerts(String userId) {
        return fraudAlertRepository.findByUserId(userId, null)
            .getContent()
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public FraudAlertResponse updateAlertStatus(Long alertId, FraudAlert.AlertStatus newStatus) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
            .orElseThrow(() -> new ResourceNotFoundException("Fraud alert not found with ID: " + alertId));
        
        alert.setStatus(newStatus);
        
        if (newStatus == FraudAlert.AlertStatus.CONFIRMED || 
            newStatus == FraudAlert.AlertStatus.FALSE_POSITIVE) {
            alert.setResolvedAt(LocalDateTime.now());
        }
        
        FraudAlert updated = fraudAlertRepository.save(alert);
        return mapToResponse(updated);
    }
    
    private FraudAlertResponse mapToResponse(FraudAlert alert) {
        return FraudAlertResponse.builder()
            .id(alert.getId())
            .transactionId(alert.getTransaction().getId())
            .userId(alert.getUserId())
            .fraudScore(alert.getFraudScore())
            .reason(alert.getReason())
            .status(alert.getStatus())
            .detectedAt(alert.getDetectedAt())
            .resolvedAt(alert.getResolvedAt())
            .build();
    }
    
    private record FraudAnalysisResult(
        BigDecimal fraudScore,
        String reason,
        boolean isFraudulent
    ) {}
}