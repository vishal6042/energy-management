package com.dems.orchestrator.assistant;

import com.dems.orchestrator.agent.ToolDispatcher;
import com.dems.orchestrator.agent.ToolRegistry;
import com.dems.orchestrator.assistant.dto.ChatMessage;
import com.dems.orchestrator.assistant.dto.ChatResponse;
import com.dems.orchestrator.assistant.dto.ConfirmRequest;
import com.dems.orchestrator.assistant.dto.PendingAction;
import com.dems.orchestrator.client.OllamaClient;
import com.dems.orchestrator.client.OllamaClient.ChatResult;
import com.dems.orchestrator.client.OllamaClient.ToolCall;
import com.dems.orchestrator.config.OrchestratorProperties;
import com.dems.orchestrator.rag.Chunk;
import com.dems.orchestrator.rag.RetrievalService;
import com.dems.orchestrator.sql.SqlAgent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The Orchestrator. Routes each request via an explicit LLM router to one of:
 * SQL Agent (data), Knowledge/RAG (docs), Action (device control + HITL), or a plain reply.
 */
@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private static final String AGENT_SQL = "SQL Agent";
    private static final String AGENT_RAG = "Knowledge (RAG)";
    private static final String AGENT_ACTION = "Action";
    private static final String AGENT_CHAT = "Assistant";

    private static final String ACTION_PROMPT = """
            You change device state for a Building Energy Management System. Call set_device_status
            or apply_algorithm with the correct arguments. If the request needs several actions
            (e.g. turn a device on AND apply an algorithm), request ALL the action tools in the SAME
            turn so they can be confirmed together. Devices may be referenced by name or id.
            """;

    private static final String CHAT_PROMPT = """
            You are the BEMS Copilot, an on-premise, fully air-gapped assistant for a Building Energy
            Management System. Keep replies short and operator-friendly. For device data ask the user
            to phrase it as a question; for how-things-work, point them to ask about the algorithms.
            """;

    private final Router router;
    private final SqlAgent sqlAgent;
    private final RetrievalService retrieval;
    private final OllamaClient ollama;
    private final ToolRegistry registry;
    private final ToolDispatcher dispatcher;
    private final PermissionService permissions;
    private final int topK;

    public AssistantService(Router router, SqlAgent sqlAgent, RetrievalService retrieval,
                            OllamaClient ollama, ToolRegistry registry, ToolDispatcher dispatcher,
                            PermissionService permissions, OrchestratorProperties props) {
        this.router = router;
        this.sqlAgent = sqlAgent;
        this.retrieval = retrieval;
        this.ollama = ollama;
        this.registry = registry;
        this.dispatcher = dispatcher;
        this.permissions = permissions;
        this.topK = props.topK();
    }

    public ChatResponse chat(List<ChatMessage> history) {
        Router.Intent intent = router.classify(history);
        return switch (intent) {
            case DATA -> sqlFlow(history);
            case DOC -> ragFlow(history);
            case ACTION -> actionFlow(history);
            case CHAT -> chatFlow(history);
        };
    }

    private ChatResponse sqlFlow(List<ChatMessage> history) {
        SqlAgent.SqlResult r = sqlAgent.run(history);
        List<com.dems.orchestrator.agent.Card> cards =
                r.card() == null ? List.of() : List.of(r.card());
        return ChatResponse.message(r.answer(), List.of(), cards, AGENT_SQL);
    }

    private ChatResponse ragFlow(List<ChatMessage> history) {
        String question = lastUser(history);
        List<Chunk> chunks = retrieval.retrieve(question, topK);
        if (chunks.isEmpty()) {
            return ChatResponse.message(
                    "I don't have documentation covering that yet.", List.of(), List.of(), AGENT_RAG);
        }
        StringBuilder context = new StringBuilder();
        List<String> citations = new ArrayList<>();
        for (Chunk c : chunks) {
            context.append("[").append(c.title()).append("] ").append(c.text()).append("\n\n");
            if (!citations.contains(c.title())) {
                citations.add(c.title());
            }
        }
        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content",
                        "Answer the question using ONLY the documentation below. If it isn't covered, "
                                + "say so. Use Markdown.\n\nDocumentation:\n" + context),
                Map.of("role", "user", "content", question));
        String answer = ollama.chat(messages, List.of()).content();
        return ChatResponse.message(answer, citations, List.of(), AGENT_RAG);
    }

    private ChatResponse actionFlow(List<ChatMessage> history) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", ACTION_PROMPT));
        for (ChatMessage m : history) {
            messages.add(Map.of("role", m.role(), "content", m.content() == null ? "" : m.content()));
        }
        ChatResult result = ollama.chat(messages, registry.ollamaTools());
        if (!result.hasToolCalls()) {
            return ChatResponse.message(result.content(), List.of(), List.of(), AGENT_ACTION);
        }
        List<PendingAction> actions = new ArrayList<>();
        for (ToolCall call : result.toolCalls()) {
            if (registry.isAction(call.name())) {
                String summary = dispatcher.describeImpact(call.name(), call.arguments());
                actions.add(new PendingAction(call.name(), call.arguments(), summary));
            }
        }
        if (actions.isEmpty()) {
            return ChatResponse.message(result.content(), List.of(), List.of(), AGENT_ACTION);
        }
        String summary = actions.size() == 1
                ? actions.get(0).summary()
                : "Please confirm these " + actions.size() + " actions:";
        return ChatResponse.confirm(summary, actions, AGENT_ACTION);
    }

    private ChatResponse chatFlow(List<ChatMessage> history) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", CHAT_PROMPT));
        for (ChatMessage m : history) {
            messages.add(Map.of("role", m.role(), "content", m.content() == null ? "" : m.content()));
        }
        return ChatResponse.message(ollama.chat(messages, List.of()).content(), List.of(), List.of(), AGENT_CHAT);
    }

    public ChatResponse confirm(ConfirmRequest req) {
        List<String> results = new ArrayList<>();
        for (PendingAction action : req.actions()) {
            Map<String, Object> args = action.args() == null ? Map.of() : action.args();
            if (!permissions.canExecute(action.tool(), args)) {
                results.add("Skipped " + action.tool() + ": you don't have permission.");
                continue;
            }
            try {
                results.add(dispatcher.executeAction(action.tool(), args));
            } catch (IllegalArgumentException e) {
                results.add(e.getMessage());
            } catch (Exception e) {
                log.error("Action execution failed", e);
                results.add("Could not complete " + action.tool() + ": " + e.getMessage());
            }
        }
        return ChatResponse.message(String.join("\n", results), List.of(), List.of(), AGENT_ACTION);
    }

    private String lastUser(List<ChatMessage> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            if ("user".equals(history.get(i).role())) {
                return history.get(i).content();
            }
        }
        return "";
    }
}
