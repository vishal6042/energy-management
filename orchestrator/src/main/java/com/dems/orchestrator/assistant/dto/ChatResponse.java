package com.dems.orchestrator.assistant.dto;

import com.dems.orchestrator.agent.Card;
import java.util.List;

/**
 * Either a normal assistant message ({@code type="message"}, optionally with
 * structured data {@code cards}) or a HITL confirmation request
 * ({@code type="confirm"}). {@code agent} names which agent produced it
 * (e.g. "SQL Agent", "Knowledge (RAG)", "Action", "Assistant").
 */
public record ChatResponse(
        String type,
        String content,
        List<String> citations,
        List<Card> cards,
        List<PendingAction> pendingActions,
        String agent) {

    public static ChatResponse message(String content, List<String> citations, List<Card> cards,
                                        String agent) {
        return new ChatResponse("message", content, citations, cards, List.of(), agent);
    }

    public static ChatResponse confirm(String summary, List<PendingAction> actions, String agent) {
        return new ChatResponse("confirm", summary, List.of(), List.of(), actions, agent);
    }
}
