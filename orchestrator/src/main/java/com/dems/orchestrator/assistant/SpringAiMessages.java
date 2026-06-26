package com.dems.orchestrator.assistant;

import com.dems.orchestrator.assistant.dto.ChatMessage;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

/** Converts the API's conversation history into Spring AI chat messages. */
final class SpringAiMessages {

    private SpringAiMessages() {}

    static List<Message> from(List<ChatMessage> history) {
        List<Message> out = new ArrayList<>();
        for (ChatMessage m : history) {
            String content = m.content() == null ? "" : m.content();
            if ("assistant".equalsIgnoreCase(m.role())) {
                out.add(new AssistantMessage(content));
            } else {
                out.add(new UserMessage(content));
            }
        }
        return out;
    }
}
