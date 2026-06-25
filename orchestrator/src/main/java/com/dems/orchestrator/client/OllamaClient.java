package com.dems.orchestrator.client;

import com.dems.orchestrator.config.OrchestratorProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Talks to the local Ollama runtime for embeddings and tool-calling chat. */
@Component
public class OllamaClient {

    private final RestClient http;
    private final ObjectMapper mapper;
    private final String chatModel;
    private final String embeddingModel;

    public OllamaClient(OrchestratorProperties props, ObjectMapper mapper) {
        this.http = HttpClients.create(props.ollama().baseUrl(), 300);
        this.mapper = mapper;
        this.chatModel = props.ollama().chatModel();
        this.embeddingModel = props.ollama().embeddingModel();
    }

    /** Embed a single text into a vector. */
    public float[] embed(String text) {
        JsonNode resp = http.post().uri("/api/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", embeddingModel, "input", text))
                .retrieve().body(JsonNode.class);
        JsonNode arr = resp.path("embeddings").path(0);
        float[] v = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            v[i] = (float) arr.get(i).asDouble();
        }
        return v;
    }

    /**
     * One chat turn. {@code messages} is the running conversation (Ollama format);
     * {@code tools} is the tool registry in Ollama's function-tool format.
     */
    @SuppressWarnings("unchecked")
    public ChatResult chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", chatModel);
        body.put("messages", messages);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
        }
        body.put("stream", false);
        body.put("options", Map.of("temperature", 0.2));

        JsonNode resp = http.post().uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve().body(JsonNode.class);

        JsonNode msg = resp.path("message");
        String content = msg.path("content").asText("");

        List<ToolCall> calls = new ArrayList<>();
        for (JsonNode tc : msg.path("tool_calls")) {
            String name = tc.path("function").path("name").asText();
            JsonNode argsNode = tc.path("function").path("arguments");
            Map<String, Object> args = argsNode.isObject()
                    ? mapper.convertValue(argsNode, Map.class)
                    : Map.of();
            calls.add(new ToolCall(name, args));
        }

        Map<String, Object> rawMessage = mapper.convertValue(msg, Map.class);
        return new ChatResult(rawMessage, content, calls);
    }

    public record ToolCall(String name, Map<String, Object> arguments) {}

    public record ChatResult(Map<String, Object> rawMessage, String content, List<ToolCall> toolCalls) {
        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }
}
