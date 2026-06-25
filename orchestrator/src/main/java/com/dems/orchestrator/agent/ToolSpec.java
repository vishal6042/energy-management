package com.dems.orchestrator.agent;

import java.util.Map;

/**
 * A tool the LLM may call. {@code parameters} is a JSON-schema object.
 * {@code action} marks state-changing tools that require HITL confirmation.
 */
public record ToolSpec(String name, String description, Map<String, Object> parameters, boolean action) {

    /** Convert to Ollama's function-tool wire format. */
    public Map<String, Object> toOllama() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name,
                        "description", description,
                        "parameters", parameters));
    }
}
