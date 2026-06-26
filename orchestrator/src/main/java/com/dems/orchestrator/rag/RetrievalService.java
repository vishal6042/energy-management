package com.dems.orchestrator.rag;

import com.dems.orchestrator.config.OrchestratorProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/** RAG agent: retrieves matching knowledge chunks from the Spring AI vector store. */
@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private final VectorStore vectorStore;
    private final OrchestratorProperties props;

    public RetrievalService(VectorStore vectorStore, OrchestratorProperties props) {
        this.vectorStore = vectorStore;
        this.props = props;
    }

    public List<Chunk> retrieve(String query, int topK) {
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder().query(query == null ? "" : query).topK(topK).build());
            if (docs == null) {
                return List.of();
            }
            return docs.stream()
                    .map(d -> new Chunk(
                            String.valueOf(d.getMetadata().getOrDefault("title", "Untitled")),
                            String.valueOf(d.getMetadata().getOrDefault("source", "")),
                            d.getText() == null ? "" : d.getText(),
                            0.0))
                    .toList();
        } catch (Exception e) {
            log.warn("Knowledge retrieval failed (is Qdrant up and the collection populated?): {}",
                    e.getMessage());
            return List.of();
        }
    }
}
