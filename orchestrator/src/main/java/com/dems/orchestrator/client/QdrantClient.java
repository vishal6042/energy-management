package com.dems.orchestrator.client;

import com.dems.orchestrator.config.OrchestratorProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Minimal Qdrant REST client: collection management, upsert, and vector search. */
@Component
public class QdrantClient {

    private final RestClient http;
    private final ObjectMapper mapper;

    public QdrantClient(OrchestratorProperties props, ObjectMapper mapper) {
        this.http = HttpClients.create(props.qdrant().baseUrl(), 30);
        this.mapper = mapper;
    }

    /** Create the collection if missing (cosine distance). Idempotent. */
    public void ensureCollection(String name, int dim) {
        boolean exists;
        try {
            http.get().uri("/collections/{n}", name).retrieve().body(JsonNode.class);
            exists = true;
        } catch (Exception e) {
            // 404 from RestClient throws; treat as "does not exist".
            exists = false;
        }
        if (exists) {
            return;
        }
        http.put().uri("/collections/{n}", name)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("vectors", Map.of("size", dim, "distance", "Cosine")))
                .retrieve().toBodilessEntity();
    }

    public void deleteCollection(String name) {
        try {
            http.delete().uri("/collections/{n}", name).retrieve().toBodilessEntity();
        } catch (Exception e) {
            // not present / unreachable — nothing to delete
        }
    }

    public long count(String name) {
        try {
            JsonNode resp = http.post().uri("/collections/{n}/points/count", name)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("exact", true))
                    .retrieve().body(JsonNode.class);
            return resp.path("result").path("count").asLong(0);
        } catch (Exception e) {
            return 0;
        }
    }

    public void upsert(String name, List<Point> points) {
        List<Map<String, Object>> wire = new ArrayList<>();
        for (Point p : points) {
            wire.add(Map.of("id", p.id(), "vector", p.vector(), "payload", p.payload()));
        }
        http.put().uri("/collections/{n}/points?wait=true", name)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("points", wire))
                .retrieve().toBodilessEntity();
    }

    public List<Hit> search(String name, float[] vector, int topK) {
        JsonNode resp = http.post().uri("/collections/{n}/points/search", name)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("vector", vector, "limit", topK, "with_payload", true))
                .retrieve().body(JsonNode.class);
        List<Hit> hits = new ArrayList<>();
        for (JsonNode h : resp.path("result")) {
            double score = h.path("score").asDouble();
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = mapper.convertValue(h.path("payload"), Map.class);
            hits.add(new Hit(score, payload));
        }
        return hits;
    }

    public record Point(String id, float[] vector, Map<String, Object> payload) {}

    public record Hit(double score, Map<String, Object> payload) {}
}
