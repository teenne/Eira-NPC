package com.storyteller.npc.knowledge;

import java.util.List;

/**
 * A single knowledge entry that can be retrieved based on keywords.
 * Used for RAG (Retrieval-Augmented Generation) to give NPCs specific factual knowledge.
 */
public record KnowledgeEntry(
    String id,
    String category,
    List<String> keywords,
    String content,
    int priority
) {
    /**
     * Create a knowledge entry with default priority
     */
    public KnowledgeEntry(String id, String category, List<String> keywords, String content) {
        this(id, category, keywords, content, 5);
    }
}
