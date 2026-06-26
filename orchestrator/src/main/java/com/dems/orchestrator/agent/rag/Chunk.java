package com.dems.orchestrator.agent.rag;

/** A retrieved knowledge snippet with its source title for citation. */
public record Chunk(String title, String source, String text, double score) {
}
