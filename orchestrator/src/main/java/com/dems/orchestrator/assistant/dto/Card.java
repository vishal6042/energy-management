package com.dems.orchestrator.assistant.dto;

import java.util.Map;

/** A structured, typed payload the frontend renders as a rich React card. */
public record Card(String kind, Map<String, Object> payload) {
}
