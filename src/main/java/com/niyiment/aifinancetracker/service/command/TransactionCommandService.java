package com.niyiment.aifinancetracker.service.command;


import com.niyiment.aifinancetracker.dto.request.TransactionRequest;
import com.niyiment.aifinancetracker.dto.response.TransactionResponse;
import com.niyiment.aifinancetracker.entity.Transaction;
import com.niyiment.aifinancetracker.event.TransactionCreatedEvent;
import com.niyiment.aifinancetracker.exception.InvalidTransactionException;
import com.niyiment.aifinancetracker.exception.ResourceNotFoundException;
import com.niyiment.aifinancetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCommandService {
    private final TransactionRepository repository;
    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

    @Value("${finance.kafka.topics.transaction-created}")
    private String transactionCreatedTopic;

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        log.info("Creating transaction: {}", request.userId());

        validateTransaction(request);

        Transaction transaction = Transaction.builder()
                .userId(request.userId())
                .amount(request.amount())
                .category(request.category())
                .transactionType(request.transactionType())
                .transactionDate(request.transactionDate())
                .merchant(request.merchant())
                .location(request.location())
                .build();
        Transaction savedTransaction = repository.save(transaction);
        log.debug("Saved transaction with ID: {}", savedTransaction.getId());

        // publish event for fraud detection
        publishTransactionCreatedEvent(request);

        return toResponse(savedTransaction);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest request) {
        log.info("Updating transaction with ID: {}", id);

        Transaction transaction = repository.findById(id)
                .orElseThrow(() -> new InvalidTransactionException("Transaction not found with ID: " + id));

        validateTransaction(request);

        transaction.setAmount(request.amount());
        transaction.setCategory(request.category());
        transaction.setTransactionType(request.transactionType());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setMerchant(request.merchant());
        transaction.setLocation(request.location());

        Transaction updatedTransaction = repository.save(transaction);
        log.debug("Updated transaction with ID: {}", updatedTransaction.getId());

        return toResponse(updatedTransaction);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        log.info("Deleting transaction with ID: {}", id);

        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Transaction not found with ID: " + id);
        }

        repository.deleteById(id);
        log.debug("Deleted transaction with ID: {}", id);
    }

    ///  Helper methods
    private void validateTransaction(TransactionRequest request) {
        if (request.amount().signum() <= 0) {
            throw new InvalidTransactionException("Invalid transaction amount: " + request.amount());
        }

        if (request.transactionDate().isAfter(LocalDateTime.now())) {
            throw new InvalidTransactionException("Invalid transaction date: " + request.transactionDate());
        }
    }

    private void publishTransactionCreatedEvent(TransactionRequest request) {
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .userId(request.userId())
                .amount(request.amount())
                .transactionDate(request.transactionDate())
                .transactionType(request.transactionType())
                .category(request.category())
                .build();

        kafkaTemplate.send(transactionCreatedTopic, event);
        log.info("Published transaction created event: {}", event);
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .userId(transaction.getUserId())
                .amount(transaction.getAmount())
                .category(transaction.getCategory())
                .transactionType(transaction.getTransactionType())
                .transactionDate(transaction.getTransactionDate())
                .merchant(transaction.getMerchant())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }

}
