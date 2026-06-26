package com.dems.orchestrator.agent;

import com.dems.orchestrator.assistant.dto.ChatMessage;
import java.util.List;

/**
 * Input to an agent: the conversation so far, plus the {@link Agents} registry so an agent can
 * delegate to another agent (the Orchestrator-Workers seam for inter-agent collaboration).
 */
public record AgentRequest(List<ChatMessage> history, Agents agents) {

    /** The latest user message (for agents that only need the question). */
    public String lastUser() {
        for (int i = history.size() - 1; i >= 0; i--) {
            if ("user".equals(history.get(i).role())) {
                return history.get(i).content();
            }
        }
        return "";
    }
}
