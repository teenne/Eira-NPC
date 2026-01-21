# PR Implementation Plan

This document provides detailed implementation plans for upcoming PRs. Each section is self-contained and can be used as context for a development session.

---

## PR #10: Emotion/Mood System

### Overview
Add an emotion system that tracks NPC mood states and affects their responses. Emotions decay over time and can be influenced by conversation topics, player actions, and world events.

### User Value
- NPCs feel more alive and reactive
- Conversations have emotional continuity
- Players can learn to read NPC moods
- Enables webhook notifications for mood changes (Eira integration)

### Implementation Plan

#### Phase 1: Core Data Model

**New File:** `src/main/java/com/storyteller/npc/EmotionState.java`

```java
public class EmotionState {
    public enum Emotion {
        NEUTRAL, HAPPY, SAD, ANGRY, FEARFUL, SURPRISED, DISGUSTED, CURIOUS
    }

    private Emotion primary = Emotion.NEUTRAL;
    private float intensity = 0.5f; // 0.0 - 1.0
    private long lastUpdateTime;

    // Decay toward neutral over time
    public void tick() { ... }

    // Modify based on conversation analysis
    public void applyImpact(Emotion emotion, float delta) { ... }

    // Serialize for persistence
    public CompoundTag toNBT() { ... }
    public static EmotionState fromNBT(CompoundTag tag) { ... }
}
```

#### Phase 2: NPC Integration

**Modify:** `StorytellerNPC.java`
- Add `EmotionState emotionState` field
- Add synced data accessor for client display
- Save/load in NBT methods
- Tick the emotion state in `tick()` method

**Modify:** `NPCCharacter.java`
- Add optional `emotional_baseline` config (some NPCs more emotional than others)
- Add `emotional_triggers` map (topics that affect mood)

#### Phase 3: LLM Integration

**Modify:** System prompt generation to include:
```
Current emotional state: [EMOTION] (intensity: [X]/10)
This affects how you respond - a sad NPC speaks more slowly,
an angry NPC is more curt, etc.
```

**Modify:** Response parsing to detect emotional indicators:
- Keywords: "thank you" → +happy
- Aggressive punctuation → +angry
- Question topics about fears → +fearful

#### Phase 4: Visual Feedback

**Modify:** `StorytellerNPCRenderer.java`
- Add subtle particle effects for strong emotions
- Optional: tint name tag based on mood

**Modify:** `NPCChatScreen.java`
- Show mood indicator in chat UI
- Colour-coded border or icon

#### Phase 5: Eira Integration

**Modify:** `EiraIntegrationManager.java`
- Trigger `onMoodChanged` webhook when emotion shifts significantly
- Emit redstone signal strength based on emotion intensity

### Files to Create/Modify

| File | Action | Description |
|------|--------|-------------|
| `npc/EmotionState.java` | Create | Core emotion tracking class |
| `entity/StorytellerNPC.java` | Modify | Add emotion field, sync, tick |
| `npc/NPCCharacter.java` | Modify | Add emotional config options |
| `client/NPCChatScreen.java` | Modify | Display mood indicator |
| `client/StorytellerNPCRenderer.java` | Modify | Emotion particles |
| `integration/EiraIntegrationManager.java` | Modify | Mood webhooks |
| `config/ModConfig.java` | Modify | Emotion decay rate config |

### Testing Checklist
- [ ] Emotions persist through server restart
- [ ] Emotions decay toward neutral over time
- [ ] Positive conversation topics improve mood
- [ ] Negative topics decrease mood
- [ ] Mood indicator displays correctly
- [ ] Webhook fires on significant mood change
- [ ] Config options work as expected

### Estimated Scope
- **Files:** 7-8
- **Lines of code:** ~400-500
- **Complexity:** Medium

---

## PR #11: Custom Quest Rewards

### Overview
Allow NPCs to give tangible rewards when quests are completed: items, experience, effects, or commands.

### User Value
- Complete gameplay loop for quests
- Incentivizes player engagement
- Enables adventure map creators to script rewards

### Implementation Plan

#### Phase 1: Reward Data Model

**Modify:** `QuestManager.java`

```java
public record QuestReward(
    RewardType type,
    String value,
    int amount
) {
    public enum RewardType {
        ITEM,       // value = item ID, amount = count
        EXPERIENCE, // amount = XP points
        EFFECT,     // value = effect ID, amount = duration (ticks)
        COMMAND     // value = command string (executed as server)
    }
}

public record Quest(
    UUID npcId,
    String description,
    QuestType type,
    String target,
    int targetCount,
    int progress,
    List<QuestReward> rewards  // NEW
) { ... }
```

#### Phase 2: Reward Parsing

**Modify:** `QuestManager.parseQuestsFromResponse()`

Detect reward patterns in NPC dialogue:
- "I'll give you [item]" → ITEM reward
- "You'll earn [X] experience" → EXPERIENCE reward
- "I'll grant you [effect]" → EFFECT reward

Add explicit reward syntax for character JSON:
```json
{
  "quest_rewards": {
    "default": [
      { "type": "EXPERIENCE", "amount": 100 }
    ],
    "collect_spider_eyes": [
      { "type": "ITEM", "value": "minecraft:emerald", "amount": 5 }
    ]
  }
}
```

#### Phase 3: Reward Distribution

**Modify:** `QuestManager.checkQuestCompletion()`

When quest completes:
1. Parse rewards from quest record
2. Apply each reward to player:
   - ITEM: `player.getInventory().add(itemStack)`
   - EXPERIENCE: `player.giveExperiencePoints(amount)`
   - EFFECT: `player.addEffect(new MobEffectInstance(...))`
   - COMMAND: `server.getCommands().performCommand(...)`

3. Show reward notification to player

#### Phase 4: UI Feedback

**Modify:** `NPCChatScreen.java`
- Show rewards in quest completion message
- Display pending rewards in quest list

**Add notification:** Toast or chat message showing rewards received

### Files to Create/Modify

| File | Action | Description |
|------|--------|-------------|
| `npc/QuestManager.java` | Modify | Add reward records and distribution |
| `npc/NPCCharacter.java` | Modify | Add quest_rewards config |
| `client/NPCChatScreen.java` | Modify | Display rewards |
| `command/ModCommands.java` | Modify | Show rewards in /storyteller quests |

### Testing Checklist
- [ ] Item rewards appear in inventory
- [ ] Experience rewards add XP correctly
- [ ] Effect rewards apply with correct duration
- [ ] Command rewards execute (with appropriate permissions)
- [ ] Rewards persist if player offline at completion
- [ ] Reward parsing from NPC dialogue works

### Estimated Scope
- **Files:** 4-5
- **Lines of code:** ~250-300
- **Complexity:** Low-Medium

---

## PR #12: NPC-to-NPC Conversations

### Overview
NPCs can converse with each other autonomously, creating ambient storytelling. Players within range can observe these conversations.

### User Value
- World feels more alive
- Emergent storytelling opportunities
- NPCs share information (foreshadowing, hints)
- Enables "overheard dialogue" gameplay

### Implementation Plan

#### Phase 1: Conversation Orchestrator

**New File:** `src/main/java/com/storyteller/npc/NPCConversationManager.java`

```java
public class NPCConversationManager {
    // Active NPC-to-NPC conversations
    private final Map<UUID, NPCConversation> activeConversations = new ConcurrentHashMap<>();

    // Check if two NPCs should start talking
    public void tick(ServerLevel level) {
        // Find NPCs within range of each other
        // Check if neither is in player conversation
        // Probability-based conversation start
        // Manage turn-taking
    }

    public record NPCConversation(
        UUID npc1,
        UUID npc2,
        List<ChatMessage> messages,
        long startTime,
        int turnsRemaining
    ) {}
}
```

#### Phase 2: AI Goal for Conversation

**New File:** `src/main/java/com/storyteller/entity/goals/ConversationGoal.java`

```java
public class ConversationGoal extends Goal {
    // Low priority goal that activates when:
    // - No player conversation active
    // - Another NPC is nearby
    // - Random chance triggers

    // Makes NPC face conversation partner
    // Sends messages through NPCConversationManager
}
```

#### Phase 3: Cross-NPC Context

**Modify:** System prompt generation to include:
```
You are having a conversation with [OTHER_NPC_NAME],
a [OTHER_NPC_TRAITS]. They just said: "[LAST_MESSAGE]"
Respond briefly (1-2 sentences) as yourself.
```

**Modify:** `NPCCharacter.java`
- Add `npc_relationships` config (who they like/dislike talking to)
- Add `conversation_topics` (what they discuss with other NPCs)

#### Phase 4: Player Observation

**New packet:** `NPCConversationPacket` (Server→Client)
- Broadcast conversation lines to nearby players
- Display as chat messages with NPC name prefixes

**Modify:** `ClientEvents.java`
- Render speech bubbles above NPCs during conversations

#### Phase 5: Configuration

**Modify:** `ModConfig.java`
```java
public final ModConfigSpec.BooleanValue enableNPCConversations;
public final ModConfigSpec.IntValue npcConversationRange; // blocks
public final ModConfigSpec.IntValue npcConversationCooldown; // ticks
public final ModConfigSpec.IntValue maxConversationTurns;
public final ModConfigSpec.DoubleValue npcConversationChance;
```

### Files to Create/Modify

| File | Action | Description |
|------|--------|-------------|
| `npc/NPCConversationManager.java` | Create | Orchestrate NPC conversations |
| `entity/goals/ConversationGoal.java` | Create | AI goal for NPC talking |
| `network/NPCConversationPacket.java` | Create | Broadcast to nearby players |
| `client/ClientEvents.java` | Modify | Speech bubble rendering |
| `npc/NPCCharacter.java` | Modify | NPC relationship config |
| `config/ModConfig.java` | Modify | Conversation settings |
| `StorytellerMod.java` | Modify | Register conversation manager |

### Testing Checklist
- [ ] Two NPCs near each other eventually start talking
- [ ] Conversations have reasonable length (not infinite)
- [ ] Players nearby see the conversation
- [ ] NPCs face each other during conversation
- [ ] Player conversations take priority
- [ ] Cooldown prevents spam conversations
- [ ] Config toggles work

### Estimated Scope
- **Files:** 7-8
- **Lines of code:** ~600-800
- **Complexity:** High

---

## PR #13: Voice Synthesis (TTS)

### Overview
Add text-to-speech capabilities so NPCs can speak their responses aloud using local TTS engines.

### User Value
- Major accessibility improvement
- Immersion for players who prefer audio
- Useful for physical installations
- Different voices per NPC character

### Implementation Plan

#### Phase 1: TTS Provider Interface

**New File:** `src/main/java/com/storyteller/tts/TTSProvider.java`

```java
public interface TTSProvider {
    CompletableFuture<byte[]> synthesize(String text, VoiceConfig voice);
    boolean isAvailable();
    List<String> getAvailableVoices();
}

public record VoiceConfig(
    String voiceId,
    float pitch,      // 0.5 - 2.0
    float speed,      // 0.5 - 2.0
    float volume      // 0.0 - 1.0
) {}
```

#### Phase 2: Piper TTS Integration

**New File:** `src/main/java/com/storyteller/tts/PiperProvider.java`

[Piper](https://github.com/rhasspy/piper) is a fast, local neural TTS engine.

```java
public class PiperProvider implements TTSProvider {
    // Calls piper executable with text input
    // Returns WAV audio data
    // Configurable voice model path
}
```

#### Phase 3: Audio Playback

**Modify:** `ClientPacketHandler.java`
- Receive audio data packet
- Play through Minecraft's sound system

**New packet:** `NPCAudioPacket` (Server→Client)
- Contains PCM audio data or reference to cached audio
- Played at NPC's position with spatial audio

#### Phase 4: Character Voice Config

**Modify:** `NPCCharacter.java`
```json
{
  "voice": {
    "provider": "piper",
    "voice_id": "en_US-amy-medium",
    "pitch": 1.0,
    "speed": 1.0
  }
}
```

#### Phase 5: Caching

**New File:** `src/main/java/com/storyteller/tts/AudioCache.java`

- Cache generated audio by text hash
- Configurable cache size limit
- LRU eviction policy

### Files to Create/Modify

| File | Action | Description |
|------|--------|-------------|
| `tts/TTSProvider.java` | Create | Provider interface |
| `tts/PiperProvider.java` | Create | Piper TTS implementation |
| `tts/TTSManager.java` | Create | Orchestrate TTS requests |
| `tts/AudioCache.java` | Create | Cache synthesized audio |
| `network/NPCAudioPacket.java` | Create | Send audio to clients |
| `client/ClientPacketHandler.java` | Modify | Play received audio |
| `npc/NPCCharacter.java` | Modify | Voice configuration |
| `config/ModConfig.java` | Modify | TTS settings |

### Testing Checklist
- [ ] Piper produces audio for text input
- [ ] Audio plays at correct spatial position
- [ ] Different NPCs can have different voices
- [ ] Pitch/speed modifiers work
- [ ] Caching prevents repeated synthesis
- [ ] Graceful fallback when TTS unavailable
- [ ] Config can disable TTS entirely

### Estimated Scope
- **Files:** 8-10
- **Lines of code:** ~700-900
- **Complexity:** High (audio handling)

### External Dependencies
- Piper TTS executable (user-installed)
- Voice model files (user-downloaded)

---

## PR #14: Relationship System

### Overview
Track player-NPC relationships over time. Relationship quality affects NPC responses, available quests, and hidden agenda reveals.

### User Value
- Long-term engagement incentive
- NPCs feel like they remember and care
- Unlock content through relationship building
- Natural progression system

### Implementation Plan

#### Phase 1: Relationship Data Model

**New File:** `src/main/java/com/storyteller/npc/RelationshipManager.java`

```java
public class RelationshipManager {
    public record Relationship(
        int level,           // -100 to +100
        int totalInteractions,
        long firstMet,
        long lastInteraction,
        Set<String> sharedSecrets,
        Set<String> completedQuests
    ) {}

    // Per player-NPC pair
    private final Map<PlayerNPCPair, Relationship> relationships = new ConcurrentHashMap<>();

    public void recordInteraction(UUID player, UUID npc, InteractionType type) { ... }
    public Relationship getRelationship(UUID player, UUID npc) { ... }
}
```

#### Phase 2: Relationship Modifiers

Define what affects relationships:

```java
public enum InteractionType {
    CONVERSATION(+1),
    QUEST_COMPLETED(+10),
    QUEST_FAILED(-5),
    GIFT_GIVEN(+5),     // Future feature
    INSULTED(-10),      // Detected from conversation
    HELPED_IN_COMBAT(+15),
    IGNORED(-1);        // Walked away from conversation

    final int relationshipDelta;
}
```

#### Phase 3: Relationship Effects

**Modify:** System prompt to include relationship context:
```
Relationship with this player: [LEVEL] ([DESCRIPTION])
- Met [X] times over [Y] days
- Player has completed [N] quests for you
- You have shared [secrets]

Adjust your warmth and trust accordingly.
```

**Modify:** `NPCCharacter.java` for relationship thresholds:
```json
{
  "relationship_thresholds": {
    "reveal_secret": 50,
    "offer_special_quest": 30,
    "refuse_conversation": -50
  }
}
```

#### Phase 4: Persistence

**Modify:** Save/load in world data
- Store relationships per world
- Clean up relationships for deleted NPCs

#### Phase 5: UI Feedback

**Modify:** `NPCChatScreen.java`
- Show relationship indicator (icon or bar)
- Show relationship change notifications

**Modify:** `/storyteller` commands
- `/storyteller relationship <npc>` - Show relationship status

### Files to Create/Modify

| File | Action | Description |
|------|--------|-------------|
| `npc/RelationshipManager.java` | Create | Core relationship tracking |
| `entity/StorytellerNPC.java` | Modify | Hook relationship updates |
| `npc/NPCCharacter.java` | Modify | Relationship config |
| `client/NPCChatScreen.java` | Modify | Relationship UI |
| `command/ModCommands.java` | Modify | Relationship command |
| `config/ModConfig.java` | Modify | Relationship settings |

### Testing Checklist
- [ ] Relationships persist through restart
- [ ] Positive interactions increase level
- [ ] Negative interactions decrease level
- [ ] NPCs respond differently at different levels
- [ ] Secrets unlock at threshold
- [ ] Quests gate based on relationship
- [ ] UI shows current relationship

### Estimated Scope
- **Files:** 6-7
- **Lines of code:** ~500-600
- **Complexity:** Medium

---

## Development Session Quick Reference

When starting a new session, provide this context to Claude:

```
Continue development on Storyteller NPCs mod.

Current state:
- PRs #1-9 merged (quests, persistence, behavior modes, knowledge/RAG)
- Build: ./gradlew build (passing)
- Minecraft: 1.21.4, NeoForge 21.4.x, Java 21

Next PR: #10 - Emotion/Mood System
See docs/PR_PLAN.md for implementation details.

Key files:
- Entity: src/main/java/com/storyteller/entity/StorytellerNPC.java
- Config: src/main/java/com/storyteller/config/ModConfig.java
- Commands: src/main/java/com/storyteller/command/ModCommands.java
- LLM: src/main/java/com/storyteller/llm/LLMManager.java
- Knowledge: src/main/java/com/storyteller/npc/knowledge/KnowledgeManager.java
```

---

## PR Dependencies

```
PR #9 (Knowledge/RAG) ────────────────► Complete ✓

PR #10 (Emotion) ─────────────────┬──► PR #12 (NPC-to-NPC)
                                  │
                                  └──► PR #14 (Relationships)

PR #11 (Quest Rewards) ───────────────► Standalone

PR #13 (TTS) ─────────────────────────► Standalone
```

PRs #11 and #13 can be developed in any order or in parallel.
PRs #12 and #14 benefit from #10 being completed first.

---

*Last updated: January 2026*
