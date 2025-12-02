package com.niyiment.aifinancetracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal fraudScore;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AlertStatus status = AlertStatus.PENDING;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime detectedAt;
    
    private LocalDateTime resolvedAt;
    
    @PrePersist
    protected void onCreate() {
        detectedAt = LocalDateTime.now();
    }
    
    public enum AlertStatus {
        PENDING,
        CONFIRMED,
        FALSE_POSITIVE,
        UNDER_REVIEW
    }
}