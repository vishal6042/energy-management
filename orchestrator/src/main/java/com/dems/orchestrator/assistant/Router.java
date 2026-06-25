package com.dems.orchestrator.assistant;

import com.dems.orchestrator.assistant.dto.ChatMessage;
import com.dems.orchestrator.client.OllamaClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Classifies each request's intent so the orchestrator can route it to the right agent. */
@Component
public class Router {

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    public enum Intent { DATA, DOC, ACTION, CHAT }

    private static final String SYSTEM = """
            Classify the user's latest request into exactly ONE label and reply with ONLY that word:

            DATA   - questions answerable from the database: device lists/counts/status, energy usage,
                     savings, costs, history, comparisons, analytics.
            DOC    - how things work, explanations of algorithms/concepts, documentation.
            ACTION - requests to change device state: turn a device on/off, apply or clear an algorithm.
            CHAT   - greetings, thanks, or anything else.

            Use the conversation so far to resolve references (e.g. "turn them on" after listing devices is ACTION).
            Reply with one word: DATA, DOC, ACTION, or CHAT.
            """;

    private final OllamaClient ollama;

    public Router(OllamaClient ollama) {
        this.ollama = ollama;
    }

    public Intent classify(List<ChatMessage> history) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM));
        for (ChatMessage m : history) {
            messages.add(Map.of("role", m.role(), "content", m.content() == null ? "" : m.content()));
        }
        String raw = ollama.chat(messages, List.of()).content();
        String label = raw == null ? "" : raw.trim().toUpperCase();
        Intent intent = parse(label);
        log.info("Router intent: {} (raw='{}')", intent, raw == null ? "" : raw.strip());
        return intent;
    }

    private Intent parse(String label) {
        if (label.contains("DATA")) return Intent.DATA;
        if (label.contains("ACTION")) return Intent.ACTION;
        if (label.contains("DOC")) return Intent.DOC;
        return Intent.CHAT;
    }
}
