package com.dems.orchestrator.assistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/** A drafted, not-yet-executed action awaiting human confirmation (HITL). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PendingAction(String tool, Map<String, Object> args, String summary) {
}
