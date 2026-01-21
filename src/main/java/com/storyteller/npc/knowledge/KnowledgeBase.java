package com.storyteller.npc.knowledge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.storyteller.StorytellerMod;
import com.storyteller.config.ModConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Stores and retrieves knowledge entries for a character using keyword-based matching.
 * This provides simple RAG (Retrieval-Augmented Generation) capabilities for NPCs.
 */
public class KnowledgeBase {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final String characterId;
    private final List<KnowledgeEntry> entries;

    public KnowledgeBase(String characterId, List<KnowledgeEntry> entries) {
        this.characterId = characterId;
        this.entries = new ArrayList<>(entries);
    }

    public String getCharacterId() {
        return characterId;
    }

    public List<KnowledgeEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    public int getEntryCount() {
        return entries.size();
    }

    /**
     * Retrieve relevant knowledge entries based on a player's message.
     * Uses keyword matching to score and rank entries.
     *
     * @param playerMessage The message from the player
     * @param maxResults Maximum number of entries to return
     * @return List of relevant knowledge entries, sorted by relevance
     */
    public List<KnowledgeEntry> retrieve(String playerMessage, int maxResults) {
        if (entries.isEmpty() || playerMessage == null || playerMessage.isBlank()) {
            return List.of();
        }

        // Normalize and tokenize the query
        String[] queryWords = playerMessage.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", " ")
            .split("\\s+");

        int minMatches = ModConfig.COMMON.minKeywordMatches.get();

        // Score each entry
        List<ScoredEntry> scored = new ArrayList<>();
        for (KnowledgeEntry entry : entries) {
            int score = scoreEntry(entry, queryWords);
            if (score >= minMatches) {
                scored.add(new ScoredEntry(entry, score));
            }
        }

        // Sort by score (descending) then by priority (descending)
        scored.sort(Comparator
            .comparingInt((ScoredEntry se) -> se.score)
            .thenComparingInt(se -> se.entry.priority())
            .reversed());

        // Return top results
        return scored.stream()
            .limit(maxResults)
            .map(se -> se.entry)
            .toList();
    }

    /**
     * Score how well an entry matches the query words.
     * Higher score = more relevant.
     */
    private int scoreEntry(KnowledgeEntry entry, String[] queryWords) {
        int matches = 0;

        for (String keyword : entry.keywords()) {
            String lowerKeyword = keyword.toLowerCase();
            for (String queryWord : queryWords) {
                // Check for exact match or partial match
                if (queryWord.equals(lowerKeyword) ||
                    queryWord.contains(lowerKeyword) ||
                    lowerKeyword.contains(queryWord)) {
                    matches++;
                    break; // Only count each keyword once
                }
            }
        }

        return matches;
    }

    /**
     * Load a knowledge base from a JSON file.
     *
     * @param file Path to the JSON file
     * @return The loaded KnowledgeBase, or null if loading fails
     */
    public static KnowledgeBase load(Path file) {
        try {
            String json = Files.readString(file);
            JsonObject root = GSON.fromJson(json, JsonObject.class);

            String characterId = root.has("character_id")
                ? root.get("character_id").getAsString()
                : file.getFileName().toString().replace(".json", "");

            List<KnowledgeEntry> entries = new ArrayList<>();

            if (root.has("entries") && root.get("entries").isJsonArray()) {
                JsonArray entriesArray = root.getAsJsonArray("entries");

                for (int i = 0; i < entriesArray.size(); i++) {
                    JsonObject entryObj = entriesArray.get(i).getAsJsonObject();

                    String id = entryObj.has("id")
                        ? entryObj.get("id").getAsString()
                        : "entry-" + i;

                    String category = entryObj.has("category")
                        ? entryObj.get("category").getAsString()
                        : "general";

                    List<String> keywords = new ArrayList<>();
                    if (entryObj.has("keywords") && entryObj.get("keywords").isJsonArray()) {
                        JsonArray keywordsArray = entryObj.getAsJsonArray("keywords");
                        for (int j = 0; j < keywordsArray.size(); j++) {
                            keywords.add(keywordsArray.get(j).getAsString());
                        }
                    }

                    String content = entryObj.has("content")
                        ? entryObj.get("content").getAsString()
                        : "";

                    int priority = entryObj.has("priority")
                        ? entryObj.get("priority").getAsInt()
                        : 5;

                    if (!content.isEmpty()) {
                        entries.add(new KnowledgeEntry(id, category, keywords, content, priority));
                    }
                }
            }

            StorytellerMod.LOGGER.debug("Loaded {} knowledge entries for character {}",
                entries.size(), characterId);

            return new KnowledgeBase(characterId, entries);

        } catch (IOException e) {
            StorytellerMod.LOGGER.error("Failed to read knowledge file {}: {}", file, e.getMessage());
            return null;
        } catch (Exception e) {
            StorytellerMod.LOGGER.error("Failed to parse knowledge file {}: {}", file, e.getMessage());
            return null;
        }
    }

    /**
     * Helper class for scoring entries during retrieval
     */
    private record ScoredEntry(KnowledgeEntry entry, int score) {}
}
