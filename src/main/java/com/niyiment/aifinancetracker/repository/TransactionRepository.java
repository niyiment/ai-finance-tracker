package com.niyiment.aifinancetracker.repository;

import com.niyiment.aifinancetracker.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    Page<Transaction> findByUserId(String userId, Pageable pageable);
    
    List<Transaction> findByUserIdAndTransactionDateBetween(
        String userId,
        LocalDateTime startDate,
        LocalDateTime endDate
    );
    
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId " +
           "AND t.transactionDate >= :startDate ORDER BY t.transactionDate DESC")
    List<Transaction> findRecentTransactionsByUser(
        @Param("userId") String userId,
        @Param("startDate") LocalDateTime startDate
    );
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.userId = :userId " +
           "AND t.transactionType = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserAndTypeAndDateRange(
        @Param("userId") String userId,
        @Param("type") Transaction.TransactionType type,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT t.category, COUNT(t), SUM(t.amount) FROM Transaction t " +
           "WHERE t.userId = :userId AND t.transactionDate >= :startDate " +
           "GROUP BY t.category ORDER BY SUM(t.amount) DESC")
    List<Object[]> getCategoryStatistics(
        @Param("userId") String userId,
        @Param("startDate") LocalDateTime startDate
    );
}