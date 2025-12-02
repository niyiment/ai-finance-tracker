package com.niyiment.aifinancetracker.service.query;

import com.niyiment.aifinancetracker.config.CacheConfig;
import com.niyiment.aifinancetracker.dto.response.TransactionResponse;
import com.niyiment.aifinancetracker.entity.Transaction;
import com.niyiment.aifinancetracker.exception.ResourceNotFoundException;
import com.niyiment.aifinancetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionQueryService {
    private final TransactionRepository repository;

    @Cacheable(value = CacheConfig.TRANSACTION_CACHE, key = "#id")
    public TransactionResponse getTransactionById(Long id) {
        log.info("Fetching transaction with ID: {}", id);

        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }

    public Page<TransactionResponse> getTransactionsByUser(String userId, Pageable pageable) {
        log.debug("Fetching transactions for user with ID: {}", userId);

        return repository.findByUserId(userId, pageable)
                .map(this::toResponse);
    }

    public List<TransactionResponse> getTransactionsByDateRange(
            String userId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        log.debug("Fetching transactions for user with ID: {} between dates: {} and {}", userId, startDate, endDate);

        return repository
                .findByUserIdAndTransactionDateBetween(userId, startDate, endDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Cacheable(value = CacheConfig.USER_STATS_CACHE, key = "#userId + '-recent'")
    public List<TransactionResponse> getRecentTransactions(String userId, int days) {
        log.debug("Fetching recent transactions for user with ID: {} (last {} days)", userId, days);

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        return repository
                .findRecentTransactionsByUser(userId, startDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Cacheable(value = CacheConfig.USER_STATS_CACHE, key = "#userId + '-summary'")
    public Map<String, Object> getUserFinancialSummary(String userId, int days) {
        log.debug("Fetching financial summary for user with ID: {} (last {} days)", userId, days);

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        LocalDateTime endDate = LocalDateTime.now();

        BigDecimal totalIncome = repository.sumAmountByUserAndTypeAndDateRange(
                userId, Transaction.TransactionType.INCOME, startDate, endDate
        );

        BigDecimal totalExpenses = repository.sumAmountByUserAndTypeAndDateRange(
                userId, Transaction.TransactionType.EXPENSE, startDate, endDate
        );

        BigDecimal totalInvestment = repository.sumAmountByUserAndTypeAndDateRange(
                userId, Transaction.TransactionType.INVESTMENT, startDate, endDate
        );

        List<Object[]> categoryStats = repository.getCategoryStatistics(userId, startDate);

        return Map.of(
                "totalIncome", totalIncome != null ? totalIncome : BigDecimal.ZERO,
                "totalExpenses", totalExpenses != null ? totalExpenses : BigDecimal.ZERO,
                "totalInvestment", totalInvestment != null ? totalInvestment : BigDecimal.ZERO,
                "netSavings", calculateNetSavings(totalIncome, totalExpenses),
                "topCategories", categoryStats,
                "period", days + " days"
        );
    }

    private BigDecimal calculateNetSavings(BigDecimal income, BigDecimal expense) {
        BigDecimal totalIncome = income != null ? income : BigDecimal.ZERO;
        BigDecimal totalExpenses = expense != null ? expense : BigDecimal.ZERO;

        return totalIncome.subtract(totalExpenses);
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
