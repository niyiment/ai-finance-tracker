package com.niyiment.aifinancetracker.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiConfig {
    @Bean
    @Qualifier("ollamaChatClient")
    public ChatClient ollamChatClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @Qualifier("openaiChatClient")
    public ChatClient openaiChatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @Primary
    public EmbeddingModel embeddingModel(OpenAiEmbeddingModel openAiEmbeddingModel) {
        return openAiEmbeddingModel;
    }

    @Bean
    @Qualifier("ollamaEmbedding")
    public EmbeddingModel ollamaEmbedding(OllamaEmbeddingModel ollamaEmbeddingModel) {
        return ollamaEmbeddingModel;
    }
}
