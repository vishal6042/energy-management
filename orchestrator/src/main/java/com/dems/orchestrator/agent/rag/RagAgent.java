package com.dems.orchestrator.agent.rag;

import com.dems.orchestrator.agent.Agent;
import com.dems.orchestrator.agent.AgentRequest;
import com.dems.orchestrator.agent.AgentResult;
import com.dems.orchestrator.config.OrchestratorProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/** RAG agent: retrieves knowledge chunks and synthesizes a grounded, cited answer. */
@Component
public class RagAgent implements Agent {

    private final RetrievalService retrieval;
    private final ChatClient chatClient;
    private final int topK;

    public RagAgent(RetrievalService retrieval, ChatClient chatClient, OrchestratorProperties props) {
        this.retrieval = retrieval;
        this.chatClient = chatClient;
        this.topK = props.topK();
    }

    @Override
    public String id() {
        return "rag";
    }

    @Override
    public String displayName() {
        return "Knowledge (RAG)";
    }

    @Override
    public String routingDescription() {
        return "how things work, explanations of algorithms/concepts, documentation.";
    }

    @Override
    public AgentResult handle(AgentRequest request) {
        String question = request.lastUser();
        List<Chunk> chunks = retrieval.retrieve(question, topK);
        if (chunks.isEmpty()) {
            return AgentResult.message("I don't have documentation covering that yet.");
        }
        StringBuilder context = new StringBuilder();
        List<String> citations = new ArrayList<>();
        for (Chunk c : chunks) {
            context.append("[").append(c.title()).append("] ").append(c.text()).append("\n\n");
            if (!citations.contains(c.title())) {
                citations.add(c.title());
            }
        }
        String answer = chatClient.prompt()
                .system("Answer the question using ONLY the documentation below. If it isn't covered, "
                        + "say so. Use Markdown.\n\nDocumentation:\n" + context)
                .user(question)
                .call()
                .content();
        return AgentResult.message(answer, citations, List.of());
    }
}
