package com.dems.orchestrator.rag;

import com.dems.orchestrator.client.OllamaClient;
import com.dems.orchestrator.client.QdrantClient;
import com.dems.orchestrator.config.OrchestratorProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

/**
 * Ingests knowledge documents (.md/.txt under {@code orchestrator.knowledge-dir})
 * into Qdrant. Runs on startup (best-effort) and can be re-triggered via the
 * /reindex endpoint. Resilient: if Qdrant/Ollama are down it logs and continues
 * so the service still boots for the SQL/Tool agents.
 */
@Service
public class KnowledgeIngestionService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionService.class);

    private final OllamaClient ollama;
    private final QdrantClient qdrant;
    private final OrchestratorProperties props;

    public KnowledgeIngestionService(OllamaClient ollama, QdrantClient qdrant,
                                     OrchestratorProperties props) {
        this.ollama = ollama;
        this.qdrant = qdrant;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ingest(false);
        } catch (Exception e) {
            log.warn("Startup knowledge ingestion skipped: {}", e.getMessage());
        }
    }

    /**
     * @param force if true, (re)embed and upsert even when the collection already has points.
     * @return number of chunks ingested this run.
     */
    public int ingest(boolean force) {
        String collection = props.qdrant().collection();
        if (force) {
            // Clear stale chunks so re-chunking/re-embedding fully replaces the index.
            qdrant.deleteCollection(collection);
        }
        qdrant.ensureCollection(collection, props.embeddingDim());

        if (!force && qdrant.count(collection) > 0) {
            log.info("Knowledge collection already populated; skipping (use force/reindex to rebuild).");
            return 0;
        }

        Path dir = Path.of(props.knowledgeDir());
        if (!Files.isDirectory(dir)) {
            log.warn("Knowledge dir {} not found; nothing to ingest.", dir.toAbsolutePath());
            return 0;
        }

        List<QdrantClient.Point> points = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(this::isDoc).toList()) {
                String title = fileTitle(file);
                String content = Files.readString(file);
                for (String section : chunk(content)) {
                    // Prepend the doc title so each chunk's embedding carries topic context.
                    String text = title + "\n\n" + section;
                    float[] vector = ollama.embed(text);
                    points.add(new QdrantClient.Point(
                            UUID.randomUUID().toString(),
                            vector,
                            Map.of("title", title, "source", file.getFileName().toString(), "text", text)));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed reading knowledge dir: " + e.getMessage(), e);
        }

        if (!points.isEmpty()) {
            qdrant.upsert(collection, points);
        }
        log.info("Ingested {} knowledge chunks into '{}'.", points.size(), collection);
        return points.size();
    }

    private boolean isDoc(Path p) {
        String n = p.getFileName().toString().toLowerCase();
        if (n.equals("readme.md")) {
            return false; // ingestion instructions, not knowledge
        }
        return n.endsWith(".md") || n.endsWith(".txt");
    }

    private String fileTitle(Path p) {
        String n = p.getFileName().toString();
        int dot = n.lastIndexOf('.');
        return (dot > 0 ? n.substring(0, dot) : n).replace('-', ' ').replace('_', ' ');
    }

    /**
     * Split into sections at level-2 ("## ") headings so each topic (e.g. an entire
     * algorithm) stays in one chunk together with its heading — better recall than
     * splitting every paragraph.
     */
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
