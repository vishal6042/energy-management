package com.dems.orchestrator.web;

import com.dems.orchestrator.assistant.AssistantService;
import com.dems.orchestrator.assistant.dto.ChatRequest;
import com.dems.orchestrator.assistant.dto.ChatResponse;
import com.dems.orchestrator.assistant.dto.ConfirmRequest;
import com.dems.orchestrator.rag.KnowledgeIngestionService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistant;
    private final KnowledgeIngestionService ingestion;

    public AssistantController(AssistantService assistant, KnowledgeIngestionService ingestion) {
        this.assistant = assistant;
        this.ingestion = ingestion;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return assistant.chat(request.messages());
    }

    @PostMapping("/confirm")
    public ChatResponse confirm(@Valid @RequestBody ConfirmRequest request) {
        return assistant.confirm(request);
    }

    /** Re-ingest the knowledge folder into Qdrant (call after adding/updating docs). */
    @PostMapping("/reindex")
    public Map<String, Object> reindex() {
        int chunks = ingestion.ingest(true);
        return Map.of("ingestedChunks", chunks);
    }
}
