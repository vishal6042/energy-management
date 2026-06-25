package com.dems.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "orchestrator")
public record OrchestratorProperties(
        Ollama ollama,
        Qdrant qdrant,
        Backend backend,
        int embeddingDim,
        String knowledgeDir,
        int topK,
        Sql sql) {

    public record Ollama(String baseUrl, String chatModel, String embeddingModel) {}

    public record Qdrant(String baseUrl, String collection) {}

    public record Backend(String baseUrl) {}

    public record Sql(int maxRows, int timeoutSeconds) {}
}
