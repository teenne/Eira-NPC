package com.storyteller.npc;

import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NPCCharacter JSON serialization and system prompt generation
 */
class NPCCharacterTest {

    @Test
    @DisplayName("createDefault should create a valid character with all fields populated")
    void createDefaultShouldCreateValidCharacter() {
        NPCCharacter character = NPCCharacter.createDefault();

        assertNotNull(character);
        assertEquals("Eldric", character.getName());
        assertEquals("The Wandering Sage", character.getTitle());
        assertNotNull(character.getId());
        assertFalse(character.getId().isEmpty());

        // Check personality
        assertNotNull(character.getPersonality());
        assertTrue(character.getPersonality().traits.contains("wise"));
        assertFalse(character.getPersonality().backstory.isEmpty());
        assertFalse(character.getPersonality().motivation.isEmpty());
        assertFalse(character.getPersonality().fears.isEmpty());
        assertFalse(character.getPersonality().quirks.isEmpty());

        // Check hidden agenda
        assertNotNull(character.getHiddenAgenda());
        assertFalse(character.getHiddenAgenda().shortTermGoal.isEmpty());
        assertFalse(character.getHiddenAgenda().longTermGoal.isEmpty());
        assertFalse(character.getHiddenAgenda().secret.isEmpty());
        assertFalse(character.getHiddenAgenda().revealConditions.isEmpty());

        // Check speech style
        assertNotNull(character.getSpeechStyle());
        assertFalse(character.getSpeechStyle().vocabulary.isEmpty());
        assertTrue(character.getSpeechStyle().avoidPhrases.contains("As an AI"));
    }

    @Test
    @DisplayName("toJson and fromJson should round-trip correctly")
    void jsonRoundTripShouldPreserveData() {
        NPCCharacter original = NPCCharacter.createDefault();

        String json = original.toJson();
        NPCCharacter restored = NPCCharacter.fromJson(json);

        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getTitle(), restored.getTitle());
        assertEquals(original.getSkinFile(), restored.getSkinFile());
        assertEquals(original.isSlimModel(), restored.isSlimModel());

        // Check personality
        assertEquals(original.getPersonality().traits, restored.getPersonality().traits);
        assertEquals(original.getPersonality().backstory, restored.getPersonality().backstory);
        assertEquals(original.getPersonality().motivation, restored.getPersonality().motivation);
        assertEquals(original.getPersonality().fears, restored.getPersonality().fears);
        assertEquals(original.getPersonality().quirks, restored.getPersonality().quirks);

        // Check hidden agenda
        assertEquals(original.getHiddenAgenda().shortTermGoal, restored.getHiddenAgenda().shortTermGoal);
        assertEquals(original.getHiddenAgenda().longTermGoal, restored.getHiddenAgenda().longTermGoal);
        assertEquals(original.getHiddenAgenda().secret, restored.getHiddenAgenda().secret);

        // Check speech style
        assertEquals(original.getSpeechStyle().vocabulary, restored.getSpeechStyle().vocabulary);
        assertEquals(original.getSpeechStyle().commonPhrases, restored.getSpeechStyle().commonPhrases);
    }

    @Test
    @DisplayName("toJson should produce valid JSON with expected structure")
    void toJsonShouldProduceValidJson() {
        NPCCharacter character = NPCCharacter.createDefault();
        String json = character.toJson();

        assertNotNull(json);
        assertTrue(json.contains("\"name\":"));
        assertTrue(json.contains("\"personality\":"));
        assertTrue(json.contains("\"hidden_agenda\":"));
        assertTrue(json.contains("\"speech_style\":"));
        assertTrue(json.contains("\"Eldric\""));
    }

    @Test
    @DisplayName("fromJson with malformed JSON should throw JsonSyntaxException")
    void fromJsonWithMalformedJsonShouldThrow() {
        assertThrows(JsonSyntaxException.class, () ->
            NPCCharacter.fromJson("{ this is not valid json }"));
    }

    @Test
    @DisplayName("fromJson with empty object should create character with defaults")
    void fromJsonWithEmptyObjectShouldUseDefaults() {
        NPCCharacter character = NPCCharacter.fromJson("{}");

        assertNotNull(character);
        // Fields should be null or have default values from constructor
        assertNotNull(character.getPersonality());
        assertNotNull(character.getHiddenAgenda());
        assertNotNull(character.getBehavior());
        assertNotNull(character.getSpeechStyle());
    }

    @Test
    @DisplayName("fromJson with minimal fields should parse correctly")
    void fromJsonWithMinimalFieldsShouldParse() {
        String minimalJson = """
            {
                "id": "test-id",
                "name": "Test Character",
                "title": "The Tester"
            }
            """;

        NPCCharacter character = NPCCharacter.fromJson(minimalJson);

        assertEquals("test-id", character.getId());
        assertEquals("Test Character", character.getName());
        assertEquals("The Tester", character.getTitle());
    }

    @Test
    @DisplayName("generateSystemPrompt should include all required sections")
    void generateSystemPromptShouldIncludeAllSections() {
        NPCCharacter character = NPCCharacter.createDefault();
        String prompt = character.generateSystemPrompt(null);

        assertNotNull(prompt);

        // Check identity section
        assertTrue(prompt.contains("You are Eldric"));
        assertTrue(prompt.contains("The Wandering Sage"));

        // Check character section
        assertTrue(prompt.contains("## Character"));
        assertTrue(prompt.contains("**Backstory:**"));
        assertTrue(prompt.contains("**Motivation:**"));
        assertTrue(prompt.contains("**Personality traits:**"));
        assertTrue(prompt.contains("**Fears:**"));
        assertTrue(prompt.contains("**Quirks:**"));

        // Check speech style section
        assertTrue(prompt.contains("## How You Speak"));
        assertTrue(prompt.contains("**Vocabulary:**"));
        assertTrue(prompt.contains("**Never say:**"));

        // Check hidden agenda section
        assertTrue(prompt.contains("## Your Secret Agenda"));
        assertTrue(prompt.contains("**Short-term goal:**"));
        assertTrue(prompt.contains("**Long-term goal:**"));
        assertTrue(prompt.contains("**Your secret:**"));

        // Check rules section
        assertTrue(prompt.contains("## Rules"));
        assertTrue(prompt.contains("Stay in character"));
        assertTrue(prompt.contains("Never break the fourth wall"));
    }

    @Test
    @DisplayName("generateSystemPrompt without title should still work")
    void generateSystemPromptWithoutTitleShouldWork() {
        NPCCharacter character = new NPCCharacter();
        character.setName("TestNPC");
        character.setTitle(""); // Empty title

        String prompt = character.generateSystemPrompt(null);

        assertTrue(prompt.contains("You are TestNPC"));
        assertFalse(prompt.contains("known as"));
    }

    @Test
    @DisplayName("generateSystemPrompt with empty fears should exclude fears section")
    void generateSystemPromptWithEmptyFearsShouldExclude() {
        NPCCharacter character = new NPCCharacter();
        character.setName("TestNPC");
        character.getPersonality().fears = List.of();

        String prompt = character.generateSystemPrompt(null);

        assertFalse(prompt.contains("**Fears:**"));
    }

    @Test
    @DisplayName("generateSystemPrompt with empty quirks should exclude quirks section")
    void generateSystemPromptWithEmptyQuirksShouldExclude() {
        NPCCharacter character = new NPCCharacter();
        character.setName("TestNPC");
        character.getPersonality().quirks = List.of();

        String prompt = character.generateSystemPrompt(null);

        assertFalse(prompt.contains("**Quirks:**"));
    }

    @Test
    @DisplayName("getExternalTrigger should return null for missing trigger")
    void getExternalTriggerShouldReturnNullForMissing() {
        NPCCharacter character = NPCCharacter.createDefault();

        assertNull(character.getExternalTrigger("nonexistent_trigger"));
    }

    @Test
    @DisplayName("getStoryTrigger should return null for missing trigger")
    void getStoryTriggerShouldReturnNullForMissing() {
        NPCCharacter character = NPCCharacter.createDefault();

        assertNull(character.getStoryTrigger("nonexistent_trigger"));
    }

    @Test
    @DisplayName("External triggers should be parsed correctly from JSON")
    void externalTriggersShouldBeParsedFromJson() {
        String jsonWithTriggers = """
            {
                "id": "test",
                "name": "Test",
                "external_triggers": {
                    "redstone_on": {
                        "action": "speak",
                        "message": "I sense something!",
                        "mood_shift": "alert",
                        "reveal_level": 1
                    }
                }
            }
            """;

        NPCCharacter character = NPCCharacter.fromJson(jsonWithTriggers);

        NPCCharacter.ExternalTrigger trigger = character.getExternalTrigger("redstone_on");
        assertNotNull(trigger);
        assertEquals("speak", trigger.action);
        assertEquals("I sense something!", trigger.message);
        assertEquals("alert", trigger.moodShift);
        assertEquals(1, trigger.revealLevel);
    }

    @Test
    @DisplayName("Story triggers should be parsed correctly from JSON")
    void storyTriggersShouldBeParsedFromJson() {
        String jsonWithTriggers = """
            {
                "id": "test",
                "name": "Test",
                "story_triggers": {
                    "secret_revealed": {
                        "emit_redstone": true,
                        "redstone_strength": 15,
                        "redstone_duration": 100,
                        "redstone_pattern": "pulse_3x"
                    }
                }
            }
            """;

        NPCCharacter character = NPCCharacter.fromJson(jsonWithTriggers);

        NPCCharacter.StoryTrigger trigger = character.getStoryTrigger("secret_revealed");
        assertNotNull(trigger);
        assertTrue(trigger.emitRedstone());
        assertEquals(15, trigger.strength());
        assertEquals(100, trigger.durationTicks());
        assertEquals("pulse_3x", trigger.pattern());
    }

    @Test
    @DisplayName("getExternalTriggerMessage should return message for valid trigger")
    void getExternalTriggerMessageShouldReturnMessage() {
        String jsonWithTriggers = """
            {
                "id": "test",
                "name": "Test",
                "external_triggers": {
                    "redstone_on": {
                        "message": "The power flows!"
                    }
                }
            }
            """;

        NPCCharacter character = NPCCharacter.fromJson(jsonWithTriggers);

        String message = character.getExternalTriggerMessage("redstone_on");
        assertEquals("The power flows!", message);
    }

    @Test
    @DisplayName("getExternalTriggerMessage should return null for missing trigger")
    void getExternalTriggerMessageShouldReturnNullForMissing() {
        NPCCharacter character = NPCCharacter.createDefault();

        assertNull(character.getExternalTriggerMessage("missing"));
    }

    @Test
    @DisplayName("getExternalTriggers should return empty map when null")
    void getExternalTriggersShouldReturnEmptyMapWhenNull() {
        NPCCharacter character = new NPCCharacter();
        // Force externalTriggers to null via JSON
        character = NPCCharacter.fromJson("{\"id\":\"test\",\"name\":\"Test\"}");

        assertNotNull(character.getExternalTriggers());
        assertTrue(character.getExternalTriggers().isEmpty());
    }

    @Test
    @DisplayName("getStoryTriggers should return empty map when null")
    void getStoryTriggersShouldReturnEmptyMapWhenNull() {
        NPCCharacter character = NPCCharacter.fromJson("{\"id\":\"test\",\"name\":\"Test\"}");

        assertNotNull(character.getStoryTriggers());
        assertTrue(character.getStoryTriggers().isEmpty());
    }

    @Test
    @DisplayName("Setters should update character fields")
    void settersShouldUpdateFields() {
        NPCCharacter character = new NPCCharacter();

        character.setId("new-id");
        character.setName("New Name");
        character.setTitle("New Title");
        character.setSkinFile("custom.png");
        character.setSlimModel(true);

        assertEquals("new-id", character.getId());
        assertEquals("New Name", character.getName());
        assertEquals("New Title", character.getTitle());
        assertEquals("custom.png", character.getSkinFile());
        assertTrue(character.isSlimModel());
    }

    @Test
    @DisplayName("Default constructor should initialize all nested objects")
    void defaultConstructorShouldInitializeNestedObjects() {
        NPCCharacter character = new NPCCharacter();

        assertNotNull(character.getPersonality());
        assertNotNull(character.getHiddenAgenda());
        assertNotNull(character.getBehavior());
        assertNotNull(character.getSpeechStyle());

        // Check default values
        assertEquals("Storyteller", character.getName());
        assertEquals("The Wandering Sage", character.getTitle());
        assertEquals("default.png", character.getSkinFile());
        assertFalse(character.isSlimModel());
    }
}
