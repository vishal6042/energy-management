package com.dems.orchestrator.agent;

import java.util.List;

/**
 * Output of a read/RAG tool: text fed back to the LLM, optional citation titles,
 * and an optional structured {@link Card} the frontend renders as a rich UI card.
 */
public record ToolResult(String content, List<String> citations, Card card) {

    public static ToolResult of(String content) {
        return new ToolResult(content, List.of(), null);
    }

    public static ToolResult of(String content, Card card) {
        return new ToolResult(content, List.of(), card);
    }
}
