package com.dems.orchestrator.rag;

import com.dems.orchestrator.client.OllamaClient;
import com.dems.orchestrator.client.QdrantClient;
import com.dems.orchestrator.config.OrchestratorProperties;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** RAG agent: embeds the query and retrieves matching knowledge chunks from Qdrant. */
@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private final OllamaClient ollama;
    private final QdrantClient qdrant;
    private final OrchestratorProperties props;

    public RetrievalService(OllamaClient ollama, QdrantClient qdrant, OrchestratorProperties props) {
        this.ollama = ollama;
        this.qdrant = qdrant;
        this.props = props;
    }

    public List<Chunk> retrieve(String query, int topK) {
        try {
            float[] vector = ollama.embed(query);
            List<QdrantClient.Hit> hits = qdrant.search(props.qdrant().collection(), vector, topK);
            List<Chunk> chunks = new ArrayList<>();
            for (QdrantClient.Hit h : hits) {
                chunks.add(new Chunk(
                        String.valueOf(h.payload().getOrDefault("title", "Untitled")),
                        String.valueOf(h.payload().getOrDefault("source", "")),
                        String.valueOf(h.payload().getOrDefault("text", "")),
                        h.score()));
            }
            return chunks;
        } catch (Exception e) {
            log.warn("Knowledge retrieval failed (is Qdrant up and the collection populated?): {}",
                    e.getMessage());
            return List.of();
        }
    }
}
