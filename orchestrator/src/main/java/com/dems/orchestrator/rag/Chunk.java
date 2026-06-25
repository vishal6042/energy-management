package com.dems.orchestrator.rag;

/** A retrieved knowledge snippet with its source title for citation. */
public record Chunk(String title, String source, String text, double score) {
}
