package com.dems.orchestrator.client;

import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Builds RestClients with sensible timeouts (the local LLM can be slow). */
final class HttpClients {

    private HttpClients() {}

    static RestClient create(String baseUrl, int readTimeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
