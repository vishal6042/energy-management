package com.dems.orchestrator.assistant;

import com.dems.orchestrator.agent.Agent;
import com.dems.orchestrator.agent.AgentRequest;
import com.dems.orchestrator.agent.AgentResult;
import com.dems.orchestrator.agent.Agents;
import com.dems.orchestrator.agent.action.ActionAgent;
import com.dems.orchestrator.agent.router.Router;
import com.dems.orchestrator.assistant.dto.ChatMessage;
import com.dems.orchestrator.assistant.dto.ChatResponse;
import com.dems.orchestrator.assistant.dto.ConfirmRequest;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * The Orchestrator (thin). The router picks one agent; the agent produces an {@link AgentResult}
 * which is mapped to the HTTP {@link ChatResponse}. Confirmed actions are run by the action agent.
 */
@Service
public class AssistantService {

    private final Router router;
    private final Agents agents;
    private final ActionAgent actionAgent;

    public AssistantService(Router router, Agents agents, ActionAgent actionAgent) {
        this.router = router;
        this.agents = agents;
        this.actionAgent = actionAgent;
    }

    public ChatResponse chat(List<ChatMessage> history) {
        Agent agent = agents.forId(router.classify(history));
        AgentResult r = agent.handle(new AgentRequest(history, agents));
        return toResponse(r, agent.displayName());
    }

    public ChatResponse confirm(ConfirmRequest req) {
        return toResponse(actionAgent.execute(req), actionAgent.displayName());
    }

    private ChatResponse toResponse(AgentResult r, String agentName) {
        return r.confirm()
                ? ChatResponse.confirm(r.content(), r.pendingActions(), agentName)
                : ChatResponse.message(r.content(), r.citations(), r.cards(), agentName);
    }
}
