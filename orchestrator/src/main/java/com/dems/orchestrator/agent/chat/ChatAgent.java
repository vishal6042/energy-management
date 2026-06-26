package com.dems.orchestrator.agent.chat;

import com.dems.orchestrator.agent.Agent;
import com.dems.orchestrator.agent.AgentRequest;
import com.dems.orchestrator.agent.AgentResult;
import com.dems.orchestrator.agent.SpringAiMessages;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/** Fallback agent for greetings / out-of-scope requests — a short plain reply. */
@Component
public class ChatAgent implements Agent {

    private static final String CHAT_SYSTEM = """
            You are the BEMS Copilot, an on-premise, air-gapped assistant for a Building Energy
            Management System. Keep replies short and operator-friendly.
            """;

    private final ChatClient chatClient;

    public ChatAgent(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String id() {
        return "chat";
    }

    @Override
    public String displayName() {
        return "Assistant";
    }

    @Override
    public String routingDescription() {
        return "greetings, thanks, or anything else not covered by the other agents.";
    }

    @Override
    public AgentResult handle(AgentRequest request) {
        String answer = chatClient.prompt()
                .system(CHAT_SYSTEM)
                .messages(SpringAiMessages.from(request.history()))
                .call()
                .content();
        return AgentResult.message(answer);
    }
}
