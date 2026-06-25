package com.dems.orchestrator.agent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The action tools exposed to the LLM in the ACTION branch. Reads go through the
 * SQL agent and docs through RAG, so only state-changing tools live here. The
 * dispatcher binds each to a validated backend call; all require HITL confirmation.
 */
@Component
public class ToolRegistry {

    private final List<ToolSpec> tools = List.of(
            new ToolSpec("set_device_status",
                    "Turn a device on (online) or off (offline). State-changing: requires confirmation.",
                    object(Map.of(
                            "device", str("Device id or name"),
                            "status", enumStr("Target status", "online", "offline")),
                            List.of("device", "status")),
                    true),

            new ToolSpec("apply_algorithm",
                    "Apply an energy-saving algorithm to a device (or 'none' to clear). State-changing: requires confirmation.",
                    object(Map.of(
                            "device", str("Device id or name"),
                            "algorithm", enumStr("Algorithm to apply", "comfort", "target", "none")),
                            List.of("device", "algorithm")),
                    true));

    public List<ToolSpec> all() {
        return tools;
    }

    public List<Map<String, Object>> ollamaTools() {
        return tools.stream().map(ToolSpec::toOllama).toList();
    }

    public boolean isAction(String name) {
        return tools.stream().anyMatch(t -> t.name().equals(name) && t.action());
    }

    // --- JSON-schema helpers ---

    private static Map<String, Object> object(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    private static Map<String, Object> str(String description) {
        return Map.of("type", "string", "description", description);
    }

    private static Map<String, Object> enumStr(String description, String... values) {
        return Map.of("type", "string", "description", description, "enum", List.of(values));
    }
}
