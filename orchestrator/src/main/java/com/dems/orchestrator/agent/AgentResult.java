package com.dems.orchestrator.agent;

import com.dems.orchestrator.assistant.dto.Card;
import com.dems.orchestrator.assistant.dto.PendingAction;
import java.util.List;

/**
 * Uniform agent output. The orchestrator maps this to a ChatResponse, adding the agent's name.
 * {@code confirm=true} means a HITL confirmation is required ({@code pendingActions} populated).
 */
public record AgentResult(
        boolean confirm,
        String content,
        List<String> citations,
        List<Card> cards,
        List<PendingAction> pendingActions) {

    public static AgentResult message(String content, List<String> citations, List<Card> cards) {
        return new AgentResult(false, content,
                citations == null ? List.of() : citations,
                cards == null ? List.of() : cards,
                List.of());
    }

    public static AgentResult message(String content) {
        return message(content, List.of(), List.of());
    }

    public static AgentResult confirm(String summary, List<PendingAction> actions) {
        return new AgentResult(true, summary, List.of(), List.of(), actions);
    }
}
