package com.dems.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "orchestrator")
public record OrchestratorProperties(
        Backend backend,
        String knowledgeDir,
        int topK,
        Sql sql) {

    public record Backend(String baseUrl) {}

    public record Sql(int maxRows, int timeoutSeconds) {}
}
