package com.niyiment.aifinancetracker.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    
    @Value("${finance.kafka.topics.transaction-created}")
    private String transactionCreatedTopic;
    
    @Value("${finance.kafka.topics.fraud-detected}")
    private String fraudDetectedTopic;
    
    @Bean
    public NewTopic transactionCreatedTopic() {
        return TopicBuilder.name(transactionCreatedTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic fraudDetectedTopic() {
        return TopicBuilder.name(fraudDetectedTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }
}