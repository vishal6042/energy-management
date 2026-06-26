package com.dems.orchestrator.rag;

import com.dems.orchestrator.config.OrchestratorProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

/**
 * Ingests knowledge documents (.md/.txt under {@code orchestrator.knowledge-dir}) into the Spring AI
 * vector store. Section-chunked by level-2 headings; embedding is handled by Spring AI's EmbeddingModel.
 * Document ids are deterministic (UUID from source+index), so re-ingesting upserts in place (idempotent)
 * instead of creating duplicates. Runs on startup and via {@code POST /api/assistant/reindex}.
 */
@Service
public class KnowledgeIngestionService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionService.class);

    private final VectorStore vectorStore;
    private final OrchestratorProperties props;

    public KnowledgeIngestionService(VectorStore vectorStore, OrchestratorProperties props) {
        this.vectorStore = vectorStore;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ingest();
        } catch (Exception e) {
            log.warn("Startup knowledge ingestion skipped: {}", e.getMessage());
        }
    }

    /** @return number of chunks (re)ingested. */
    public int ingest() {
        Path dir = Path.of(props.knowledgeDir());
        if (!Files.isDirectory(dir)) {
            log.warn("Knowledge dir {} not found; nothing to ingest.", dir.toAbsolutePath());
            return 0;
        }

        List<Document> docs = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(this::isDoc).toList()) {
                String title = fileTitle(file);
                String source = file.getFileName().toString();
                String content = Files.readString(file);
                List<String> sections = chunk(content);
                for (int i = 0; i < sections.size(); i++) {
                    String id = UUID.nameUUIDFromBytes(
                            (source + "#" + i).getBytes(StandardCharsets.UTF_8)).toString();
                    String text = title + "\n\n" + sections.get(i);
                    docs.add(Document.builder()
                            .id(id)
                            .text(text)
                            .metadata(Map.of("title", title, "source", source))
                            .build());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed reading knowledge dir: " + e.getMessage(), e);
        }

        if (!docs.isEmpty()) {
            vectorStore.add(docs);
        }
        log.info("Ingested {} knowledge chunks into the vector store.", docs.size());
        return docs.size();
    }

    private boolean isDoc(Path p) {
        String n = p.getFileName().toString().toLowerCase();
        if (n.equals("readme.md")) {
            return false;
        }
        return n.endsWith(".md") || n.endsWith(".txt");
    }

    private String fileTitle(Path p) {
        String n = p.getFileName().toString();
        int dot = n.lastIndexOf('.');
        return (dot > 0 ? n.substring(0, dot) : n).replace('-', ' ').replace('_', ' ');
    }

    /** Split into sections at level-2 ("## ") headings so each topic stays in one chunk. */
    private List<String> chunk(String content) {
        List<String> sections = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String line : content.split("\n")) {
            if (line.startsWith("## ") && !cur.toString().isBlank()) {
                sections.add(cur.toString().strip());
                cur.setLength(0);
            }
            cur.append(line).append("\n");
        }
        if (!cur.toString().isBlank()) {
            sections.add(cur.toString().strip());
        }
        List<String> result = new ArrayList<>();
        for (String s : sections) {
            if (s.length() >= 20) {
                result.add(s);
            }
        }
        if (result.isEmpty() && !content.isBlank()) {
            result.add(content.strip());
        }
        return result;
    }
}
