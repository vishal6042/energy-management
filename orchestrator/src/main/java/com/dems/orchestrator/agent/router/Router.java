package com.dems.orchestrator.agent.router;

import com.dems.orchestrator.agent.Agent;
import com.dems.orchestrator.agent.Agents;
import com.dems.orchestrator.agent.SpringAiMessages;
import com.dems.orchestrator.assistant.dto.ChatMessage;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Routing agentic pattern: an LLM classifies each request to one agent id. The candidate list is
 * built from the registered agents, so adding an agent automatically extends routing.
 */
@Component
public class Router {

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    private final ChatClient chatClient;
    private final Agents agents;

    public Router(ChatClient chatClient, Agents agents) {
        this.chatClient = chatClient;
        this.agents = agents;
    }

    /** @return the chosen agent id (falls back to "chat"). */
    public String classify(List<ChatMessage> history) {
        StringBuilder sys = new StringBuilder(
                "Classify the user's latest request and reply with ONLY the matching agent id.\n\n"
                        + "Agent ids and when to use them:\n");
        for (Agent a : agents.all()) {
            sys.append("  ").append(a.id()).append(" - ").append(a.routingDescription()).append('\n');
        }
        sys.append("\nUse the conversation so far to resolve references (e.g. \"turn them on\" after "
                + "listing devices is the action agent).\nReply with exactly one agent id.");

        String raw = chatClient.prompt()
                .system(sys.toString())
                .messages(SpringAiMessages.from(history))
                .call()
                .content();
        String chosen = match(raw);
        log.info("Router → {} (raw='{}')", chosen, raw == null ? "" : raw.strip());
        return chosen;
    }

    private String match(String raw) {
        String label = raw == null ? "" : raw.toLowerCase();
        for (Agent a : agents.all()) {
            if (label.contains(a.id())) {
                return a.id();
            }
        }
        return "chat";
    }
}
