package com.storyteller.npc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.storyteller.StorytellerMod;
import com.storyteller.npc.knowledge.KnowledgeManager;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Manages NPC characters and their configurations
 */
public class NPCManager {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final Map<String, NPCCharacter> characters = new HashMap<>();
    private final Path configDir;
    private final Path skinsDir;
    private final Path charactersDir;
    
    public NPCManager() {
        this.configDir = FMLPaths.CONFIGDIR.get().resolve("storyteller");
        this.skinsDir = configDir.resolve("skins");
        this.charactersDir = configDir.resolve("characters");
    }
    
    /**
     * Load all NPC configurations from disk
     */
    public void loadNPCs() {
        try {
            // Create directories if they don't exist
            Files.createDirectories(configDir);
            Files.createDirectories(skinsDir);
            Files.createDirectories(charactersDir);
            
            // Create default skin readme
            Path skinReadme = skinsDir.resolve("README.txt");
            if (!Files.exists(skinReadme)) {
                Files.writeString(skinReadme, """
                    STORYTELLER NPC SKINS
                    ====================
                    
                    Place player skin PNG files (64x64 or 64x32) in this folder.
                    Reference them in character configs by filename.
                    
                    Example: If you place "wizard.png" here, set "skinFile": "wizard.png"
                    in your character JSON.
                    
                    Skin format: Standard Minecraft player skin format
                    - 64x64 for modern skins (with slim arm support)
                    - 64x32 for legacy skins
                    
                    You can download skins from sites like:
                    - NameMC (namemc.com)
                    - MinecraftSkins (minecraftskins.com)
                    - The Skindex (minecraftskins.com)
                    """);
            }
            
            // Load all character JSON files
            if (Files.exists(charactersDir)) {
                try (Stream<Path> paths = Files.list(charactersDir)) {
                    paths.filter(p -> p.toString().endsWith(".json"))
                         .forEach(this::loadCharacterFile);
                }
            }
            
            // If no characters loaded, create a default one
            if (characters.isEmpty()) {
                StorytellerMod.LOGGER.info("No characters found, creating default character...");
                NPCCharacter defaultChar = NPCCharacter.createDefault();
                characters.put(defaultChar.getId(), defaultChar);
                saveCharacter(defaultChar);
            }

            StorytellerMod.LOGGER.info("Loaded {} NPC character(s)", characters.size());

            // Load knowledge bases for RAG
            KnowledgeManager.loadAll(configDir);

        } catch (IOException e) {
            StorytellerMod.LOGGER.error("Failed to load NPCs: {}", e.getMessage());
        }
    }
    
    private void loadCharacterFile(Path file) {
        try {
            String json = Files.readString(file);
            NPCCharacter character = NPCCharacter.fromJson(json);
            characters.put(character.getId(), character);
            StorytellerMod.LOGGER.debug("Loaded character: {} ({})", character.getName(), character.getId());
        } catch (Exception e) {
            StorytellerMod.LOGGER.error("Failed to load character from {}: {}", file, e.getMessage());
        }
    }
    
    /**
     * Save all NPC configurations to disk
     */
    public void saveNPCs() {
        for (NPCCharacter character : characters.values()) {
            saveCharacter(character);
        }
    }
    
    /**
     * Save a single character to disk
     */
    public void saveCharacter(NPCCharacter character) {
        try {
            Files.createDirectories(charactersDir);
            Path file = charactersDir.resolve(character.getId() + ".json");
            Files.writeString(file, character.toJson());
            StorytellerMod.LOGGER.debug("Saved character: {} to {}", character.getName(), file);
        } catch (IOException e) {
            StorytellerMod.LOGGER.error("Failed to save character {}: {}", character.getName(), e.getMessage());
        }
    }
    
    /**
     * Get a character by ID
     */
    public Optional<NPCCharacter> getCharacter(String id) {
        return Optional.ofNullable(characters.get(id));
    }
    
    /**
     * Get the default character (first available or create new)
     */
    public NPCCharacter getDefaultCharacter() {
        if (characters.isEmpty()) {
            NPCCharacter defaultChar = NPCCharacter.createDefault();
            characters.put(defaultChar.getId(), defaultChar);
            return defaultChar;
        }
        return characters.values().iterator().next();
    }
    
    /**
     * Get all characters
     */
    public Map<String, NPCCharacter> getAllCharacters() {
        return new HashMap<>(characters);
    }
    
    /**
     * Register a new character
     */
    public void registerCharacter(NPCCharacter character) {
        characters.put(character.getId(), character);
        saveCharacter(character);
    }
    
    /**
     * Remove a character
     */
    public boolean removeCharacter(String id) {
        NPCCharacter removed = characters.remove(id);
        if (removed != null) {
            try {
                Path file = charactersDir.resolve(id + ".json");
                Files.deleteIfExists(file);
                return true;
            } catch (IOException e) {
                StorytellerMod.LOGGER.error("Failed to delete character file: {}", e.getMessage());
            }
        }
        return false;
    }
    
    /**
     * Get the path to a skin file
     */
    public Optional<Path> getSkinPath(String skinFile) {
        if (skinFile == null || skinFile.isEmpty()) {
            return Optional.empty();
        }
        Path skinPath = skinsDir.resolve(skinFile);
        if (Files.exists(skinPath)) {
            return Optional.of(skinPath);
        }
        return Optional.empty();
    }
    
    /**
     * List available skin files
     */
    public String[] getAvailableSkins() {
        try {
            if (Files.exists(skinsDir)) {
                try (Stream<Path> paths = Files.list(skinsDir)) {
                    return paths.filter(p -> p.toString().toLowerCase().endsWith(".png"))
                                .map(p -> p.getFileName().toString())
                                .toArray(String[]::new);
                }
            }
        } catch (IOException e) {
            StorytellerMod.LOGGER.error("Failed to list skins: {}", e.getMessage());
        }
        return new String[0];
    }
    
    public Path getSkinsDir() {
        return skinsDir;
    }
    
    public Path getCharactersDir() {
        return charactersDir;
    }
}
