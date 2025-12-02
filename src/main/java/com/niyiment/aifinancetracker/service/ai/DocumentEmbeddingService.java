package com.niyiment.aifinancetracker.service.ai;

import com.niyiment.aifinancetracker.entity.DocumentEmbedding;
import com.niyiment.aifinancetracker.exception.DocumentProcessingException;
import com.niyiment.aifinancetracker.repository.DocumentEmbeddingRepository;
import com.pgvector.PGvector;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentEmbeddingService {
    private final DocumentEmbeddingRepository repository;
    private final EmbeddingModel embeddingModel;

    @Value( "${finance.document-path}")
    private String documentPath;
    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;

    @PostConstruct
    public void initializeDocuments() {
        log.info("Initializing document embeddings from path: {}", documentPath);
        try {
            loadAndProcessDocuments();
        } catch (Exception e) {
            log.error("Error initializing document embeddings: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void loadAndProcessDocuments() {
        try{
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(documentPath + "*.pdf");

            log.info("Found {} documents to process", resources.length);

            for (Resource resource : resources) {
                String documentName = resource.getFilename();
                if (repository.existsByDocumentName(documentName)) {
                    log.debug("Document {} already exists in database", documentName);
                    continue;
                }
                log.info("Processing document: {}", documentName);
                processDocument(resource, documentName);
            }
            log.info("Finished processing documents");
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to load documents", e);
        }
    }

    private void processDocument(Resource resource, String documentName) {
        try {
            // Text from PDF
            List<String> chunks = extractAndChunkText(resource);

            log.debug("Created {} chunks from document: {}", chunks.size(), documentName);

            // Generate embeddings for each chunk
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                float[] embedding = generateEmbedding(chunk);

                DocumentEmbedding docEmbedding = DocumentEmbedding.builder()
                        .documentName(documentName)
                        .content(chunk)
                        .embedding(new PGvector(embedding))
                        .metadata(Map.of(
                                "chunkIndex", i,
                                "totalChunks", chunks.size(),
                                "source", documentName
                        ))
                        .build();

                repository.save(docEmbedding);
            }

            log.info("Successfully processed document: {} ({} chunks)", documentName, chunks.size());

        } catch (Exception e) {
            log.error("Failed to process document: {}", documentName, e);
            throw new DocumentProcessingException("Failed to process document: " + documentName, e);
        }
    }

    private List<String> extractAndChunkText(Resource resource) throws IOException {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(resource.getFile()))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            return chunkText(text);
        }
    }

    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());

            // Try to break at sentence boundary
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf('.', end);
                if (lastPeriod > start + CHUNK_SIZE / 2) {
                    end = lastPeriod + 1;
                }
            }

            chunks.add(text.substring(start, end).trim());
            start = end - CHUNK_OVERLAP;
        }

        return chunks;
    }

    private float[] generateEmbedding(String text) {
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
        return response.getResults().get(0).getOutput();
    }

    public List<DocumentEmbedding> findRelevantDocuments(String query, int limit) {
        log.debug("Finding relevant documents for query: {}", query);

        try {
            float[] queryEmbedding = generateEmbedding(query);
            String embeddingString = Arrays.toString(queryEmbedding);

            return repository.findSimilarDocuments(embeddingString, limit);

        } catch (Exception e) {
            log.error("Failed to find relevant documents", e);
            throw new DocumentProcessingException("Failed to search documents", e);
        }
    }

    public String buildContextFromDocuments(List<DocumentEmbedding> documents) {
        return documents.stream()
                .map(doc -> String.format("Source: %s\n%s",
                        doc.getDocumentName(),
                        doc.getContent()))
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}
