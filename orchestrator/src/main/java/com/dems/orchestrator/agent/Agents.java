package com.dems.orchestrator.agent;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Registry of all {@link Agent} beans, keyed by id. Built from Spring's injected agent list, so
 * adding a new agent requires no change here. Also the inter-agent invocation hub.
 */
@Component
public class Agents {

    private final Map<String, Agent> byId = new LinkedHashMap<>();

    public Agents(List<Agent> agents) {
        for (Agent a : agents) {
            byId.put(a.id(), a);
        }
    }

    public Agent forId(String id) {
        return byId.getOrDefault(id, byId.get("chat"));
    }

    public Collection<Agent> all() {
        return byId.values();
    }

    /** Let one agent delegate to another (Orchestrator-Workers). */
    public AgentResult invoke(String id, AgentRequest request) {
        return forId(id).handle(request);
    }
}
