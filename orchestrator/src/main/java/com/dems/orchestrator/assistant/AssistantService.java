package com.dems.orchestrator.assistant;

import com.dems.orchestrator.agent.Card;
import com.dems.orchestrator.agent.ToolDispatcher;
import com.dems.orchestrator.assistant.dto.ChatMessage;
import com.dems.orchestrator.assistant.dto.ChatResponse;
import com.dems.orchestrator.assistant.dto.ConfirmRequest;
import com.dems.orchestrator.assistant.dto.PendingAction;
import com.dems.orchestrator.config.OrchestratorProperties;
import com.dems.orchestrator.rag.Chunk;
import com.dems.orchestrator.rag.RetrievalService;
import com.dems.orchestrator.sql.SqlAgent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * The Orchestrator. An explicit LLM router sends each request to one agent:
 * SQL Agent (data), Knowledge/RAG (docs), Action (device control + HITL), or a plain reply.
 * All LLM calls go through Spring AI's {@link ChatClient}.
 */
@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private static final String AGENT_SQL = "SQL Agent";
    private static final String AGENT_RAG = "Knowledge (RAG)";
    private static final String AGENT_ACTION = "Action";
    private static final String AGENT_CHAT = "Assistant";

    private static final String ACTION_SYSTEM = """
            You control devices in a Building Energy Management System. From the user's request decide
            which state-changing actions to perform. Respond with ONLY a JSON array (no markdown).
            Each element is one of:
              {"tool":"set_device_status","device":"<id or name>","status":"online|offline"}
              {"tool":"apply_algorithm","device":"<id or name>","algorithm":"comfort|target|none"}
            If the request needs several actions (e.g. turn a device on AND apply an algorithm), include
            all of them. If no device action is requested, respond with [].
            """;

    private static final String CHAT_SYSTEM = """
            You are the BEMS Copilot, an on-premise, air-gapped assistant for a Building Energy
            Management System. Keep replies short and operator-friendly.
            """;

    private final Router router;
    private final SqlAgent sqlAgent;
    private final RetrievalService retrieval;
    private final ChatClient chatClient;
    private final ToolDispatcher dispatcher;
    private final PermissionService permissions;
    private final ObjectMapper mapper;
    private final int topK;

    public AssistantService(Router router, SqlAgent sqlAgent, RetrievalService retrieval,
                            ChatClient chatClient, ToolDispatcher dispatcher,
                            PermissionService permissions, ObjectMapper mapper,
                            OrchestratorProperties props) {
        this.router = router;
        this.sqlAgent = sqlAgent;
        this.retrieval = retrieval;
        this.chatClient = chatClient;
        this.dispatcher = dispatcher;
        this.permissions = permissions;
        this.mapper = mapper;
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
        List<Card> cards = r.card() == null ? List.of() : List.of(r.card());
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
        String answer = chatClient.prompt()
                .system("Answer the question using ONLY the documentation below. If it isn't covered, "
                        + "say so. Use Markdown.\n\nDocumentation:\n" + context)
                .user(question)
                .call()
                .content();
        return ChatResponse.message(answer, citations, List.of(), AGENT_RAG);
    }

    private ChatResponse actionFlow(List<ChatMessage> history) {
        String raw = chatClient.prompt()
                .system(ACTION_SYSTEM)
                .messages(SpringAiMessages.from(history))
                .call()
                .content();
        List<PendingAction> actions = parseActions(raw);
        if (actions.isEmpty()) {
            return ChatResponse.message(
                    "I didn't detect a device action to perform. Could you rephrase?",
                    List.of(), List.of(), AGENT_ACTION);
        }
        String summary = actions.size() == 1
                ? actions.get(0).summary()
                : "Please confirm these " + actions.size() + " actions:";
        return ChatResponse.confirm(summary, actions, AGENT_ACTION);
    }

    private ChatResponse chatFlow(List<ChatMessage> history) {
        String answer = chatClient.prompt()
                .system(CHAT_SYSTEM)
                .messages(SpringAiMessages.from(history))
                .call()
                .content();
        return ChatResponse.message(answer, List.of(), List.of(), AGENT_CHAT);
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

    /** Parse the model's JSON action array into drafted, not-yet-executed PendingActions. */
    @SuppressWarnings("unchecked")
    private List<PendingAction> parseActions(String raw) {
        List<PendingAction> actions = new ArrayList<>();
        if (raw == null) {
            return actions;
        }
        String json = raw.strip();
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return actions;
        }
        json = json.substring(start, end + 1);
        try {
            List<Map<String, Object>> items = mapper.readValue(json, List.class);
            for (Map<String, Object> item : items) {
                String tool = str(item.get("tool"));
                if (!"set_device_status".equals(tool) && !"apply_algorithm".equals(tool)) {
                    continue;
                }
                Map<String, Object> args = new java.util.LinkedHashMap<>();
                args.put("device", item.get("device"));
                if (item.containsKey("status")) {
                    args.put("status", item.get("status"));
                }
                if (item.containsKey("algorithm")) {
                    args.put("algorithm", item.get("algorithm"));
                }
                actions.add(new PendingAction(tool, args, dispatcher.describeImpact(tool, args)));
            }
        } catch (Exception e) {
            log.warn("Could not parse action JSON [{}]: {}", json, e.getMessage());
        }
        return actions;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
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
