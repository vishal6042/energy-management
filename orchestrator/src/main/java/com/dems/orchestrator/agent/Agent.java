package com.dems.orchestrator.agent;

/**
 * A specialized agent (Spring AI agentic patterns — Routing / Orchestrator-Workers).
 * Each agent is a self-contained Spring bean; new agents are added simply by implementing
 * this interface — the {@link Agents} registry and the router pick them up automatically.
 */
public interface Agent {

    /** Stable id used for routing and registry lookup, e.g. "sql", "rag", "action", "chat". */
    String id();

    /** Human label shown as the agent badge in chat, e.g. "SQL Agent". */
    String displayName();

    /** One line describing when to use this agent — fed to the router's classification prompt. */
    String routingDescription();

    /** Handle a request and return a uniform result. */
    AgentResult handle(AgentRequest request);
}
