package com.dems.orchestrator.agent.action;

import com.dems.orchestrator.agent.Agent;
import com.dems.orchestrator.agent.AgentRequest;
import com.dems.orchestrator.agent.AgentResult;
import com.dems.orchestrator.agent.SpringAiMessages;
import com.dems.orchestrator.assistant.dto.ConfirmRequest;
import com.dems.orchestrator.assistant.dto.PendingAction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Action agent: proposes device state changes and runs them behind HITL. It asks the LLM for a JSON
 * array of actions (structured output, not auto-executed tool calls), drafts them for confirmation,
 * and only executes once the user confirms.
 */
@Component
public class ActionAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(ActionAgent.class);

    private static final String ACTION_SYSTEM = """
            You control devices in a Building Energy Management System. From the user's request decide
            which state-changing actions to perform. Respond with ONLY a JSON array (no markdown).
            Each element is one of:
              {"tool":"set_device_status","device":"<id or name>","status":"online|offline"}
              {"tool":"apply_algorithm","device":"<id or name>","algorithm":"comfort|target|none"}
            If the request needs several actions (e.g. turn a device on AND apply an algorithm), include
            all of them. If no device action is requested, respond with [].
            """;

    private final ChatClient chatClient;
    private final ToolDispatcher dispatcher;
    private final PermissionService permissions;
    private final ObjectMapper mapper;

    public ActionAgent(ChatClient chatClient, ToolDispatcher dispatcher,
                       PermissionService permissions, ObjectMapper mapper) {
        this.chatClient = chatClient;
        this.dispatcher = dispatcher;
        this.permissions = permissions;
        this.mapper = mapper;
    }

    @Override
    public String id() {
        return "action";
    }

    @Override
    public String displayName() {
        return "Action";
    }

    @Override
    public String routingDescription() {
        return "requests to change device state: turn a device on/off, apply or clear an algorithm.";
    }

    @Override
    public AgentResult handle(AgentRequest request) {
        String raw = chatClient.prompt()
                .system(ACTION_SYSTEM)
                .messages(SpringAiMessages.from(request.history()))
                .call()
                .content();
        List<PendingAction> actions = parseActions(raw);
        if (actions.isEmpty()) {
            return AgentResult.message("I didn't detect a device action to perform. Could you rephrase?");
        }
        String summary = actions.size() == 1
                ? actions.get(0).summary()
                : "Please confirm these " + actions.size() + " actions:";
        return AgentResult.confirm(summary, actions);
    }

    /** Execute confirmed actions in order (called by the orchestrator on /confirm). */
    public AgentResult execute(ConfirmRequest req) {
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
        return AgentResult.message(String.join("\n", results));
    }

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
                Map<String, Object> args = new LinkedHashMap<>();
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
}
