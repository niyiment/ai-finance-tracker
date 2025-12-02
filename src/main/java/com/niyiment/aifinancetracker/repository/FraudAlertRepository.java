package com.niyiment.aifinancetracker.repository;

import com.niyiment.aifinancetracker.entity.FraudAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    
    Page<FraudAlert> findByUserId(String userId, Pageable pageable);
    
    List<FraudAlert> findByUserIdAndStatus(String userId, FraudAlert.AlertStatus status);
    
    @Query("SELECT fa FROM FraudAlert fa WHERE fa.userId = :userId " +
           "AND fa.status = :status AND fa.detectedAt >= :startDate")
    List<FraudAlert> findRecentAlertsByUserAndStatus(
        @Param("userId") String userId,
        @Param("status") FraudAlert.AlertStatus status,
        @Param("startDate") LocalDateTime startDate
    );
    
    Long countByUserIdAndStatus(String userId, FraudAlert.AlertStatus status);
}