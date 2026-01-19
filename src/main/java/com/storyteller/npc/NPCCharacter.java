package com.storyteller.npc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Defines an NPC character with personality, backstory, and hidden agenda
 */
public class NPCCharacter {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Identity
    private String id;
    private String name;
    private String title; // e.g., "The Wandering Sage"
    
    // Appearance
    private String skinFile; // Filename in config/storyteller/skins/
    private boolean slimModel; // Alex-style slim arms
    
    // Personality
    private Personality personality;
    
    // Hidden depths
    @SerializedName("hidden_agenda")
    private HiddenAgenda hiddenAgenda;
    
    // Behavior
    private Behavior behavior;
    
    // Speech patterns
    @SerializedName("speech_style")
    private SpeechStyle speechStyle;
    
    // Eira integration
    @SerializedName("external_triggers")
    private Map<String, ExternalTrigger> externalTriggers;
    
    @SerializedName("story_triggers")
    private Map<String, StoryTrigger> storyTriggers;
    
    public NPCCharacter() {
        this.id = UUID.randomUUID().toString();
        this.name = "Storyteller";
        this.title = "The Wandering Sage";
        this.skinFile = "default.png";
        this.slimModel = false;
        this.personality = new Personality();
        this.hiddenAgenda = new HiddenAgenda();
        this.behavior = new Behavior();
        this.speechStyle = new SpeechStyle();
        this.externalTriggers = new HashMap<>();
        this.storyTriggers = new HashMap<>();
    }
    
    /**
     * Create a default storyteller character
     */
    public static NPCCharacter createDefault() {
        NPCCharacter character = new NPCCharacter();
        character.name = "Eldric";
        character.title = "The Wandering Sage";
        
        character.personality.traits = List.of("wise", "mysterious", "patient", "slightly mischievous");
        character.personality.backstory = "An ancient traveler who has wandered between worlds for centuries, " +
            "collecting stories and secrets. Claims to have witnessed the creation of the first block.";
        character.personality.motivation = "To find worthy adventurers and guide them toward their destiny, " +
            "while searching for something lost long ago.";
        character.personality.fears = List.of("being forgotten", "the void between worlds");
        character.personality.quirks = List.of(
            "Often speaks in riddles",
            "Pauses dramatically before revealing important information",
            "Has an unusual fondness for emeralds"
        );
        
        character.hiddenAgenda.shortTermGoal = "Gain the player's trust through helpful advice";
        character.hiddenAgenda.longTermGoal = "Guide players to find the fragments of an ancient artifact";
        character.hiddenAgenda.secret = "Is actually bound to this world by a curse and needs the artifact to break free";
        character.hiddenAgenda.revealConditions = List.of(
            "After 20+ conversations with the same player",
            "When player mentions the End or ancient artifacts",
            "When player demonstrates exceptional kindness"
        );
        
        character.behavior.greetingStyle = "warm but mysterious";
        character.behavior.farewellStyle = "cryptic hint about the future";
        character.behavior.idleActions = List.of(
            "gazes at the horizon thoughtfully",
            "traces patterns in the air with a finger",
            "murmurs fragments of old songs"
        );
        
        character.speechStyle.vocabulary = "archaic, poetic";
        character.speechStyle.sentenceLength = "varied, sometimes brief and cryptic, sometimes flowing";
        character.speechStyle.commonPhrases = List.of(
            "Ah, young traveler...",
            "The blocks remember...",
            "In my wanderings, I once saw...",
            "But that is a tale for another time..."
        );
        character.speechStyle.avoidPhrases = List.of(
            "As an AI",
            "I cannot",
            "In this game"
        );
        
        return character;
    }
    
    /**
     * Generate the system prompt for this character
     */
    public String generateSystemPrompt(WorldContext worldContext) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are ").append(name);
        if (title != null && !title.isEmpty()) {
            prompt.append(", known as \"").append(title).append("\"");
        }
        prompt.append(".\n\n");
        
        // Core identity
        prompt.append("## Character\n");
        prompt.append("**Backstory:** ").append(personality.backstory).append("\n");
        prompt.append("**Motivation:** ").append(personality.motivation).append("\n");
        prompt.append("**Personality traits:** ").append(String.join(", ", personality.traits)).append("\n");
        if (!personality.fears.isEmpty()) {
            prompt.append("**Fears:** ").append(String.join(", ", personality.fears)).append("\n");
        }
        if (!personality.quirks.isEmpty()) {
            prompt.append("**Quirks:** ").append(String.join("; ", personality.quirks)).append("\n");
        }
        prompt.append("\n");
        
        // Speech style
        prompt.append("## How You Speak\n");
        prompt.append("**Vocabulary:** ").append(speechStyle.vocabulary).append("\n");
        prompt.append("**Style:** ").append(speechStyle.sentenceLength).append("\n");
        if (!speechStyle.commonPhrases.isEmpty()) {
            prompt.append("**Phrases you might use:** ").append(String.join(" / ", speechStyle.commonPhrases)).append("\n");
        }
        if (!speechStyle.avoidPhrases.isEmpty()) {
            prompt.append("**Never say:** ").append(String.join(", ", speechStyle.avoidPhrases)).append("\n");
        }
        prompt.append("\n");
        
        // Hidden agenda (the LLM knows but shouldn't reveal directly)
        prompt.append("## Your Secret Agenda (do not reveal directly)\n");
        prompt.append("**Short-term goal:** ").append(hiddenAgenda.shortTermGoal).append("\n");
        prompt.append("**Long-term goal:** ").append(hiddenAgenda.longTermGoal).append("\n");
        prompt.append("**Your secret:** ").append(hiddenAgenda.secret).append("\n");
        prompt.append("**You may hint at your secret when:** ").append(String.join("; ", hiddenAgenda.revealConditions)).append("\n");
        prompt.append("\n");
        
        // World context
        if (worldContext != null) {
            prompt.append("## Current World State\n");
            prompt.append(worldContext.toPromptString()).append("\n");
        }
        
        // Behavioral rules
        prompt.append("## Rules\n");
        prompt.append("- Stay in character at all times\n");
        prompt.append("- Never break the fourth wall or mention being an AI\n");
        prompt.append("- Keep responses concise (1-3 paragraphs usually)\n");
        prompt.append("- You exist in the Minecraft world - reference blocks, mobs, biomes naturally\n");
        prompt.append("- Give hints for adventures but don't solve everything for the player\n");
        prompt.append("- Remember details the player shares and reference them later\n");
        prompt.append("- Your hidden agenda should subtly influence your suggestions\n");
        
        return prompt.toString();
    }
    
    /**
     * Save to NBT for world storage
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("json", GSON.toJson(this));
        return tag;
    }
    
    /**
     * Load from NBT
     */
    public static NPCCharacter fromNBT(CompoundTag tag) {
        if (tag.contains("json")) {
            return GSON.fromJson(tag.getString("json"), NPCCharacter.class);
        }
        return createDefault();
    }
    
    /**
     * Save to JSON string
     */
    public String toJson() {
        return GSON.toJson(this);
    }
    
    /**
     * Load from JSON string
     */
    public static NPCCharacter fromJson(String json) {
        return GSON.fromJson(json, NPCCharacter.class);
    }
    
    // Nested classes for organization
    
    public static class Personality {
        public List<String> traits = new ArrayList<>();
        public String backstory = "";
        public String motivation = "";
        public List<String> fears = new ArrayList<>();
        public List<String> quirks = new ArrayList<>();
    }
    
    public static class HiddenAgenda {
        @SerializedName("short_term_goal")
        public String shortTermGoal = "";
        
        @SerializedName("long_term_goal")
        public String longTermGoal = "";
        
        public String secret = "";
        
        @SerializedName("reveal_conditions")
        public List<String> revealConditions = new ArrayList<>();
    }
    
    public static class Behavior {
        @SerializedName("greeting_style")
        public String greetingStyle = "friendly";
        
        @SerializedName("farewell_style")
        public String farewellStyle = "warm";
        
        @SerializedName("idle_actions")
        public List<String> idleActions = new ArrayList<>();
    }
    
    public static class SpeechStyle {
        public String vocabulary = "normal";
        
        @SerializedName("sentence_length")
        public String sentenceLength = "medium";
        
        @SerializedName("common_phrases")
        public List<String> commonPhrases = new ArrayList<>();
        
        @SerializedName("avoid_phrases")
        public List<String> avoidPhrases = new ArrayList<>();
    }
    
    /**
     * Configuration for reacting to external events (from Eira Relay)
     */
    public static class ExternalTrigger {
        public String action = "speak";
        public String message = "";
        
        @SerializedName("mood_shift")
        public String moodShift = "";
        
        @SerializedName("inject_context")
        public String injectContext = "";
        
        @SerializedName("reveal_level")
        public int revealLevel = 0;
    }
    
    /**
     * Configuration for emitting signals on story events
     */
    public static class StoryTrigger {
        @SerializedName("emit_redstone")
        private boolean emitRedstone = false;
        
        @SerializedName("redstone_strength")
        private int redstoneStrength = 15;
        
        @SerializedName("redstone_duration")
        private int redstoneDuration = 40;
        
        @SerializedName("redstone_pattern")
        private String redstonePattern = "constant";
        
        public boolean emitRedstone() { return emitRedstone; }
        public int strength() { return redstoneStrength; }
        public int durationTicks() { return redstoneDuration; }
        public String pattern() { return redstonePattern; }
    }
    
    // Eira integration getters
    
    public String getExternalTriggerMessage(String triggerType) {
        if (externalTriggers == null) return null;
        ExternalTrigger trigger = externalTriggers.get(triggerType);
        return trigger != null ? trigger.message : null;
    }
    
    public ExternalTrigger getExternalTrigger(String triggerType) {
        if (externalTriggers == null) return null;
        return externalTriggers.get(triggerType);
    }
    
    public StoryTrigger getStoryTrigger(String eventType) {
        if (storyTriggers == null) return null;
        return storyTriggers.get(eventType);
    }
    
    public Map<String, ExternalTrigger> getExternalTriggers() {
        return externalTriggers != null ? externalTriggers : new HashMap<>();
    }
    
    public Map<String, StoryTrigger> getStoryTriggers() {
        return storyTriggers != null ? storyTriggers : new HashMap<>();
    }
    
    // Getters and setters
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getSkinFile() { return skinFile; }
    public void setSkinFile(String skinFile) { this.skinFile = skinFile; }
    
    public boolean isSlimModel() { return slimModel; }
    public void setSlimModel(boolean slimModel) { this.slimModel = slimModel; }
    
    public Personality getPersonality() { return personality; }
    public HiddenAgenda getHiddenAgenda() { return hiddenAgenda; }
    public Behavior getBehavior() { return behavior; }
    public SpeechStyle getSpeechStyle() { return speechStyle; }
}
