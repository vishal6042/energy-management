package com.dems.orchestrator.assistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** A single conversation turn from the client. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatMessage(String role, String content) {
}
