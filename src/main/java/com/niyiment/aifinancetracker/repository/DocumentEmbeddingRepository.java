package com.niyiment.aifinancetracker.repository;

import com.niyiment.aifinancetracker.entity.DocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentEmbeddingRepository extends JpaRepository<DocumentEmbedding, Long> {
    boolean existsByDocumentName(String documentName);

    List<DocumentEmbedding> findSimilarDocuments(String embeddingString, int limit);
}
