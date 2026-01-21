package com.storyteller.npc.knowledge;

import com.storyteller.StorytellerMod;
import com.storyteller.config.ModConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Manages knowledge bases for all characters.
 * Knowledge bases are loaded from JSON files in config/storyteller/knowledge/
 */
public class KnowledgeManager {

    private static final Map<String, KnowledgeBase> knowledgeBases = new ConcurrentHashMap<>();
    private static Path knowledgeDir;

    /**
     * Load all knowledge bases from the knowledge directory.
     *
     * @param configDir The storyteller config directory
     */
    public static void loadAll(Path configDir) {
        knowledgeDir = configDir.resolve("knowledge");

        try {
            // Create knowledge directory if it doesn't exist
            Files.createDirectories(knowledgeDir);

            // Create README file
            Path readme = knowledgeDir.resolve("README.txt");
            if (!Files.exists(readme)) {
                Files.writeString(readme, """
                    STORYTELLER NPC KNOWLEDGE BASES
                    ===============================

                    Place knowledge JSON files here to give NPCs specific factual knowledge.
                    Each file should be named after the character ID (e.g., eira-storyteller.json).

                    File format:
                    {
                      "character_id": "eira-storyteller",
                      "entries": [
                        {
                          "id": "unique-entry-id",
                          "category": "organization",
                          "keywords": ["what", "who", "eira", "about"],
                          "content": "Eira is an educational organization...",
                          "priority": 10
                        }
                      ]
                    }

                    Keywords: Words that trigger this entry when found in player messages
                    Priority: Higher priority entries are shown first (default: 5)

                    See docs/CHARACTER_GUIDE.md for more details.
                    """);
            }

            // Clear existing knowledge bases
            knowledgeBases.clear();

            // Load all JSON files
            if (Files.exists(knowledgeDir)) {
                try (Stream<Path> paths = Files.list(knowledgeDir)) {
                    paths.filter(p -> p.toString().endsWith(".json"))
                        .forEach(KnowledgeManager::loadKnowledgeFile);
                }
            }

            StorytellerMod.LOGGER.info("Loaded {} knowledge base(s)", knowledgeBases.size());

        } catch (IOException e) {
            StorytellerMod.LOGGER.error("Failed to initialize knowledge directory: {}", e.getMessage());
        }
    }

    private static void loadKnowledgeFile(Path file) {
        KnowledgeBase kb = KnowledgeBase.load(file);
        if (kb != null && kb.getEntryCount() > 0) {
            knowledgeBases.put(kb.getCharacterId(), kb);
            StorytellerMod.LOGGER.info("Loaded knowledge base for '{}' with {} entries",
                kb.getCharacterId(), kb.getEntryCount());
        }
    }

    /**
     * Get the knowledge base for a specific character.
     *
     * @param characterId The character ID
     * @return The knowledge base, or null if none exists
     */
    public static KnowledgeBase getForCharacter(String characterId) {
        if (!ModConfig.COMMON.enableKnowledge.get()) {
            return null;
        }
        return knowledgeBases.get(characterId);
    }

    /**
     * Reload knowledge base for a specific character.
     *
     * @param characterId The character ID to reload
     * @return true if reload succeeded
     */
    public static boolean reload(String characterId) {
        if (knowledgeDir == null) {
            return false;
        }

        Path file = knowledgeDir.resolve(characterId + ".json");
        if (!Files.exists(file)) {
            knowledgeBases.remove(characterId);
            return false;
        }

        KnowledgeBase kb = KnowledgeBase.load(file);
        if (kb != null && kb.getEntryCount() > 0) {
            knowledgeBases.put(characterId, kb);
            return true;
        }

        return false;
    }

    /**
     * Reload all knowledge bases.
     */
    public static void reloadAll() {
        if (knowledgeDir != null) {
            Path configDir = knowledgeDir.getParent();
            loadAll(configDir);
        }
    }

    /**
     * Get all loaded knowledge base character IDs.
     */
    public static List<String> getLoadedCharacterIds() {
        return List.copyOf(knowledgeBases.keySet());
    }

    /**
     * Check if a character has a knowledge base.
     */
    public static boolean hasKnowledgeBase(String characterId) {
        return knowledgeBases.containsKey(characterId);
    }

    /**
     * Retrieve knowledge for a character based on a player message.
     * Convenience method that handles null checking.
     *
     * @param characterId The character ID
     * @param playerMessage The player's message
     * @return List of relevant knowledge entries, or empty list if no knowledge base
     */
    public static List<KnowledgeEntry> retrieve(String characterId, String playerMessage) {
        if (!ModConfig.COMMON.enableKnowledge.get()) {
            return List.of();
        }

        KnowledgeBase kb = knowledgeBases.get(characterId);
        if (kb == null) {
            return List.of();
        }

        int maxEntries = ModConfig.COMMON.maxRetrievedEntries.get();
        return kb.retrieve(playerMessage, maxEntries);
    }

    /**
     * Build a knowledge context string for injection into the system prompt.
     *
     * @param characterId The character ID
     * @param playerMessage The player's message
     * @return Knowledge context string, or null if no relevant knowledge
     */
    public static String buildKnowledgeContext(String characterId, String playerMessage) {
        List<KnowledgeEntry> entries = retrieve(characterId, playerMessage);

        if (entries.isEmpty()) {
            return null;
        }

        StringBuilder context = new StringBuilder();
        context.append("## Your Knowledge\n");
        context.append("Use this information to answer the player accurately:\n");

        for (KnowledgeEntry entry : entries) {
            context.append("- ").append(entry.content()).append("\n");
        }

        return context.toString();
    }
}
