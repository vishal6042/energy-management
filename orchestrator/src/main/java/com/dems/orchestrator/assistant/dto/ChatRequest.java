package com.dems.orchestrator.assistant.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ChatRequest(@NotEmpty List<ChatMessage> messages) {
}
