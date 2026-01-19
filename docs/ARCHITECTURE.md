# Technical Architecture Document
# Storyteller NPCs Mod

**Version:** 1.0  
**Last Updated:** January 2025

---

## 1. System Overview

### 1.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           MINECRAFT CLIENT                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                      │
│  │   Chat UI   │  │  Renderer   │  │   Packet    │                      │
│  │   Screen    │  │  (Skins)    │  │   Handler   │                      │
│  └──────┬──────┘  └─────────────┘  └──────┬──────┘                      │
│         │                                  │                             │
└─────────┼──────────────────────────────────┼─────────────────────────────┘
          │           Network                │
          ▼                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           MINECRAFT SERVER                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │  Storyteller │  │    NPC     │  │    LLM     │  │   Config    │    │
│  │     NPC     │◄─┤   Manager  │◄─┤   Manager  │  │   Manager   │    │
│  │   Entity    │  │            │  │            │  │             │    │
│  └──────┬──────┘  └─────────────┘  └──────┬─────┘  └─────────────┘    │
│         │                                  │                            │
│         │         ┌────────────────────────┼────────────────────┐      │
│         │         │                        │                    │      │
│         ▼         ▼                        ▼                    ▼      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │
│  │ Conversation│  │   World    │  │  Character  │  │    Skin     │  │
│  │   History   │  │   Context  │  │   Profiles  │  │   Loader    │  │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           LLM PROVIDERS                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                      │
│  │   Ollama    │  │   Claude    │  │   OpenAI   │                      │
│  │  (Local)    │  │   (Cloud)   │  │   (Cloud)  │                      │
│  └─────────────┘  └─────────────┘  └─────────────┘                      │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Component Responsibilities

| Component | Responsibility |
|-----------|---------------|
| **StorytellerNPC** | Entity logic, player interaction, message routing |
| **NPCManager** | Character loading, persistence, skin management |
| **LLMManager** | Provider selection, request routing, failover |
| **LLMProvider** | API communication with specific LLM service |
| **ConversationHistory** | Per-player message history, rate limiting |
| **WorldContext** | Game state extraction for prompt enrichment |
| **NPCCharacter** | Character data model, system prompt generation |
| **NPCChatScreen** | Client-side chat interface |
| **StorytellerNPCRenderer** | Entity rendering with custom skins |

---

## 2. Package Structure

```
com.storyteller/
├── StorytellerMod.java              # Mod entry point, lifecycle
│
├── config/
│   └── ModConfig.java               # NeoForge config spec
│
├── entity/
│   ├── ModEntities.java             # Entity type registration
│   └── StorytellerNPC.java          # NPC entity class
│
├── npc/
│   ├── NPCCharacter.java            # Character data model
│   ├── NPCManager.java              # Character lifecycle management
│   ├── ConversationHistory.java     # Chat history per player
│   └── WorldContext.java            # World state for prompts
│
├── llm/
│   ├── LLMProvider.java             # Provider interface
│   ├── LLMManager.java              # Provider orchestration
│   └── providers/
│       ├── OllamaProvider.java      # Ollama HTTP client
│       ├── ClaudeProvider.java      # Anthropic API client
│       └── OpenAIProvider.java      # OpenAI API client
│
├── network/
│   ├── ModNetwork.java              # Packet registration
│   ├── OpenChatScreenPacket.java    # S→C: Open chat UI
│   ├── NPCResponsePacket.java       # S→C: NPC reply
│   └── PlayerChatPacket.java        # C→S: Player message
│
├── client/
│   ├── ClientEvents.java            # Client-side registration
│   ├── ClientPacketHandler.java     # Client packet handling
│   ├── NPCChatScreen.java           # Chat GUI
│   └── StorytellerNPCRenderer.java  # Entity renderer
│
└── command/
    └── ModCommands.java             # Server commands
```

---

## 3. Data Flow

### 3.1 Player Message Flow

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  Player  │────▶│  Client  │────▶│  Server  │────▶│   NPC    │
│  Types   │     │  Screen  │     │  Packet  │     │  Entity  │
└──────────┘     └──────────┘     └──────────┘     └────┬─────┘
                                                        │
     ┌──────────────────────────────────────────────────┘
     │
     ▼
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  Build   │────▶│  Build   │────▶│   LLM    │────▶│  Send    │
│ Context  │     │  Prompt  │     │ Request  │     │ Response │
└──────────┘     └──────────┘     └──────────┘     └──────────┘
```

### 3.2 Detailed Sequence

```
Player              Client              Server              LLM
  │                   │                   │                  │
  │ Right-click NPC   │                   │                  │
  │──────────────────▶│                   │                  │
  │                   │ InteractionEvent  │                  │
  │                   │──────────────────▶│                  │
  │                   │                   │ OpenChatPacket   │
  │                   │◀──────────────────│                  │
  │   Chat Screen     │                   │                  │
  │◀──────────────────│                   │                  │
  │                   │                   │                  │
  │ Type message      │                   │                  │
  │──────────────────▶│                   │                  │
  │                   │ PlayerChatPacket  │                  │
  │                   │──────────────────▶│                  │
  │                   │                   │ Build context    │
  │                   │                   │────────┐         │
  │                   │                   │◀───────┘         │
  │                   │                   │                  │
  │                   │                   │ HTTP Request     │
  │                   │                   │─────────────────▶│
  │                   │                   │                  │ Generate
  │                   │                   │                  │────┐
  │                   │                   │                  │◀───┘
  │                   │                   │ HTTP Response    │
  │                   │                   │◀─────────────────│
  │                   │                   │                  │
  │                   │ NPCResponsePacket │                  │
  │                   │◀──────────────────│                  │
  │   Display reply   │                   │                  │
  │◀──────────────────│                   │                  │
```

---

## 4. Key Classes

### 4.1 StorytellerNPC Entity

```java
public class StorytellerNPC extends PathfinderMob {
    // Synced entity data
    private static final EntityDataAccessor<String> DATA_CHARACTER_ID;
    private static final EntityDataAccessor<String> DATA_DISPLAY_NAME;
    private static final EntityDataAccessor<Boolean> DATA_IS_THINKING;
    private static final EntityDataAccessor<String> DATA_SKIN_FILE;
    private static final EntityDataAccessor<Boolean> DATA_SLIM_MODEL;
    
    // Server-side only
    private NPCCharacter character;
    private AtomicBoolean processingRequest;
    
    // Key methods
    public InteractionResult mobInteract(Player, InteractionHand);
    public void processPlayerMessage(ServerPlayer, String);
    public NPCCharacter getCharacter();
    public void setCharacter(NPCCharacter);
}
```

### 4.2 NPCCharacter Model

```java
public class NPCCharacter {
    // Identity
    private String id;
    private String name;
    private String title;
    
    // Appearance
    private String skinFile;
    private boolean slimModel;
    
    // Nested data classes
    private Personality personality;    // traits, backstory, motivation, fears, quirks
    private HiddenAgenda hiddenAgenda;  // goals, secret, reveal conditions
    private Behavior behavior;          // greeting/farewell style, idle actions
    private SpeechStyle speechStyle;    // vocabulary, phrases to use/avoid
    
    // Key methods
    public String generateSystemPrompt(WorldContext);
    public static NPCCharacter createDefault();
    public String toJson();
    public static NPCCharacter fromJson(String);
}
```

### 4.3 LLMProvider Interface

```java
public interface LLMProvider {
    CompletableFuture<Boolean> initialize();
    CompletableFuture<String> chat(String systemPrompt, List<ChatMessage> messages);
    boolean isAvailable();
    String getName();
    void shutdown();
    
    record ChatMessage(Role role, String content) {
        enum Role { USER, ASSISTANT, SYSTEM }
    }
}
```

### 4.4 WorldContext Builder

```java
public class WorldContext {
    private String biome;
    private String timeOfDay;      // dawn, morning, midday, etc.
    private String weather;        // clear, rain, thunderstorm
    private String dimension;      // Overworld, Nether, End
    private String playerName;
    private int playerHealth;
    private int playerHunger;
    private boolean playerIsUnderground;
    
    public static WorldContext build(ServerLevel, ServerPlayer, Entity npc);
    public String toPromptString();
}
```

---

## 5. Network Protocol

### 5.1 Packet Types

| Packet | Direction | Purpose |
|--------|-----------|---------|
| `OpenChatScreenPacket` | Server → Client | Open chat UI for specific NPC |
| `PlayerChatPacket` | Client → Server | Send player message to NPC |
| `NPCResponsePacket` | Server → Client | Deliver NPC response |

### 5.2 Packet Definitions

```java
// Server → Client: Open chat screen
record OpenChatScreenPacket(
    int entityId,
    UUID npcUuid,
    String npcName,
    String skinFile,
    boolean slimModel
)

// Client → Server: Player message
record PlayerChatPacket(
    int entityId,
    String message      // max 500 chars
)

// Server → Client: NPC response
record NPCResponsePacket(
    int entityId,
    String response
)
```

---

## 6. LLM Integration

### 6.1 System Prompt Structure

```
You are {name}, known as "{title}".

## Character
**Backstory:** {backstory}
**Motivation:** {motivation}
**Personality traits:** {traits}
**Fears:** {fears}
**Quirks:** {quirks}

## How You Speak
**Vocabulary:** {vocabulary}
**Style:** {sentence_length}
**Phrases you might use:** {common_phrases}
**Never say:** {avoid_phrases}

## Your Secret Agenda (do not reveal directly)
**Short-term goal:** {short_term_goal}
**Long-term goal:** {long_term_goal}
**Your secret:** {secret}
**You may hint at your secret when:** {reveal_conditions}

## Current World State
- Location: {biome} biome in {dimension}
- Time: {time_of_day}
- Weather: {weather}
- Speaking with: {player_name}
- [conditional: player health/hunger warnings]

## Conversation Context
You have had {count} conversation(s) with this player.
[Recent exchange summary]

## Rules
- Stay in character at all times
- Never break the fourth wall or mention being an AI
- Keep responses concise (1-3 paragraphs usually)
- You exist in the Minecraft world - reference blocks, mobs, biomes naturally
- Give hints for adventures but don't solve everything for the player
- Remember details the player shares and reference them later
- Your hidden agenda should subtly influence your suggestions
```

### 6.2 Ollama API

**Endpoint:** `POST /api/chat`

```json
{
  "model": "mistral:7b-instruct",
  "stream": false,
  "messages": [
    {"role": "system", "content": "..."},
    {"role": "user", "content": "Hello!"},
    {"role": "assistant", "content": "Greetings, traveler..."},
    {"role": "user", "content": "What brings you here?"}
  ],
  "options": {
    "temperature": 0.8,
    "top_p": 0.9,
    "repeat_penalty": 1.1
  }
}
```

### 6.3 Claude API

**Endpoint:** `POST https://api.anthropic.com/v1/messages`

```json
{
  "model": "claude-sonnet-4-20250514",
  "max_tokens": 1024,
  "system": "...",
  "messages": [
    {"role": "user", "content": "Hello!"},
    {"role": "assistant", "content": "Greetings..."},
    {"role": "user", "content": "What brings you here?"}
  ]
}
```

### 6.4 OpenAI API

**Endpoint:** `POST https://api.openai.com/v1/chat/completions`

```json
{
  "model": "gpt-4o",
  "max_tokens": 1024,
  "temperature": 0.8,
  "messages": [
    {"role": "system", "content": "..."},
    {"role": "user", "content": "Hello!"},
    {"role": "assistant", "content": "Greetings..."},
    {"role": "user", "content": "What brings you here?"}
  ]
}
```

---

## 7. Configuration Schema

### 7.1 Common Config (storyteller-common.toml)

```toml
[llm]
    # OLLAMA, CLAUDE, or OPENAI
    provider = "OLLAMA"

    [llm.ollama]
        endpoint = "http://localhost:11434"
        model = "mistral:7b-instruct"
        timeout = 60

    [llm.claude]
        apiKey = ""
        model = "claude-sonnet-4-20250514"

    [llm.openai]
        apiKey = ""
        model = "gpt-4o"

[npc]
    maxConversationHistory = 20
    responseTimeout = 30
    includeWorldContext = true
    thinkingIndicatorDelay = 20

[ratelimit]
    minTimeBetweenMessages = 20
    maxMessagesPerMinute = 10
```

### 7.2 Character JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["id", "name"],
  "properties": {
    "id": { "type": "string" },
    "name": { "type": "string" },
    "title": { "type": "string" },
    "skinFile": { "type": "string" },
    "slimModel": { "type": "boolean" },
    "personality": {
      "type": "object",
      "properties": {
        "traits": { "type": "array", "items": { "type": "string" } },
        "backstory": { "type": "string" },
        "motivation": { "type": "string" },
        "fears": { "type": "array", "items": { "type": "string" } },
        "quirks": { "type": "array", "items": { "type": "string" } }
      }
    },
    "hidden_agenda": {
      "type": "object",
      "properties": {
        "short_term_goal": { "type": "string" },
        "long_term_goal": { "type": "string" },
        "secret": { "type": "string" },
        "reveal_conditions": { "type": "array", "items": { "type": "string" } }
      }
    },
    "speech_style": {
      "type": "object",
      "properties": {
        "vocabulary": { "type": "string" },
        "sentence_length": { "type": "string" },
        "common_phrases": { "type": "array", "items": { "type": "string" } },
        "avoid_phrases": { "type": "array", "items": { "type": "string" } }
      }
    }
  }
}
```

---

## 8. Threading Model

### 8.1 Thread Safety Requirements

| Component | Thread | Notes |
|-----------|--------|-------|
| Entity tick | Server main | Standard Minecraft |
| Packet handling | Netty I/O → Server main | enqueueWork() |
| LLM requests | CompletableFuture pool | Async HTTP |
| LLM responses | Server main | via thenAccept scheduling |
| Conversation history | ConcurrentHashMap | Thread-safe collections |
| Config access | Any | NeoForge config is thread-safe |

### 8.2 Async Pattern

```java
// In StorytellerNPC.processPlayerMessage()
CompletableFuture.supplyAsync(() -> {
    // Build prompt (can access world state safely from server thread)
    return llmManager.chat(systemPrompt, history);
}).thenAccept(response -> {
    // Back on server thread (via scheduling)
    // Send response packet to player
});
```

---

## 9. Error Handling

### 9.1 Error Categories

| Error | Handling |
|-------|----------|
| LLM unavailable | Show friendly message, log warning |
| LLM timeout | "NPC seems distracted..." message |
| Invalid character JSON | Skip file, log error, use defaults |
| Missing skin file | Fallback to default Steve skin |
| Network packet invalid | Silently ignore, log debug |
| Rate limit exceeded | Inform player, reject message |

### 9.2 Fallback Responses

```java
private static final String[] FALLBACK_RESPONSES = {
    "[The storyteller seems lost in thought...]",
    "[A distant look crosses their face...]",
    "[They pause, as if listening to something far away...]"
};
```

---

## 10. Security Considerations

### 10.1 Input Validation

- Player messages limited to 500 characters
- Character JSON validated against schema
- API keys never logged or transmitted to clients
- Distance check (10 blocks) before processing messages

### 10.2 Content Safety

- System prompt includes behavioral guardrails
- "avoid_phrases" config for explicit restrictions
- Rate limiting prevents abuse
- Server operators can disable/enable per-player

### 10.3 Data Privacy

- Conversation history stored in-memory only (by default)
- Local LLM (Ollama) keeps all data on-device
- Cloud APIs: only conversation data sent, no identifiers
- No telemetry or analytics

---

## 11. Testing Strategy

### 11.1 Unit Tests

- NPCCharacter JSON serialization/deserialization
- WorldContext prompt generation
- ConversationHistory management
- LLM provider mock responses

### 11.2 Integration Tests

- Full conversation flow with mock LLM
- Packet round-trip validation
- Config loading/saving

### 11.3 Manual Testing

- Visual verification of skins
- Chat UI interaction
- Performance under load
- Multi-player scenarios

---

## 12. Future Considerations

### 12.1 Scalability

- Connection pooling for cloud APIs
- Caching for repeated similar prompts
- Batching for multiple simultaneous requests

### 12.2 Extensibility Points

- Custom LLM providers via API
- Character behavior hooks for other mods
- Event system for conversation milestones
- Quest integration interface
