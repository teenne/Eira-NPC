# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Storyteller NPCs is a NeoForge mod for Minecraft 1.21.4 that adds AI-powered NPCs capable of dynamic conversations and storytelling. It integrates with Large Language Models (Ollama, Claude, OpenAI) and bridges digital/physical worlds through the Eira Relay ecosystem.

## Build Commands

```bash
# Full build
./gradlew build

# Development client (testing NPCs in-game)
./gradlew runClient

# Development server
./gradlew runServer

# Generate IDE run configurations
./gradlew genIntellijRuns   # IntelliJ IDEA
./gradlew genEclipseRuns    # Eclipse

# Clean rebuild
./gradlew clean build
```

**Requirements:** JDK 21, Ollama running locally (for default LLM provider)

## Architecture

### Layer Model

```
CLIENT TIER (client package)
├── NPCChatScreen - Custom chat GUI for NPC conversations
├── StorytellerNPCRenderer - Entity rendering with custom skins
└── ClientEvents, ClientPacketHandler

NETWORK TIER (network package)
├── OpenChatScreenPacket (Server→Client)
├── PlayerChatPacket (Client→Server)
└── NPCResponsePacket (Server→Client)

SERVER TIER
├── StorytellerNPC (entity) - Main entity implementation
├── NPCManager (npc) - Character lifecycle management
├── ConversationHistory (npc) - Per-player chat history
├── WorldContext (npc) - Game state extraction for LLM context
└── ModCommands (command) - /storyteller commands

LLM INTEGRATION TIER (llm package)
├── LLMManager - Provider orchestration, async request handling
└── providers/ - OllamaProvider, ClaudeProvider, OpenAIProvider

EIRA INTEGRATION TIER (integration package)
└── EiraIntegrationManager - Physical-digital bridge via redstone
```

### Message Processing Flow

1. Player types message in `NPCChatScreen`
2. `PlayerChatPacket` sent to server
3. `StorytellerNPC.processPlayerMessage()` builds context:
   - WorldContext (biome, time, weather, player state)
   - ConversationHistory
   - NPCCharacter system prompt
   - Pending Eira external context
4. `LLMManager` makes async HTTP request via `CompletableFuture`
5. Response saved to history, sent back via `NPCResponsePacket`

### Threading Model

- **Server main thread:** Entity ticks, command handling, packet processing
- **CompletableFuture pool:** LLM HTTP requests (non-blocking)
- **Thread-safe collections:** ConcurrentHashMap for conversation history

### Character System

Characters are JSON files in `config/storyteller/characters/` with:
- **Identity:** id, name, title, skinFile, slimModel
- **Personality:** traits, backstory, motivation, fears, quirks
- **HiddenAgenda:** goals, secret, reveal conditions
- **SpeechStyle:** vocabulary, sentence length, common/forbidden phrases
- **ExternalTriggers/StoryTriggers:** Eira integration events

See `examples/` for complete character examples including Eira integration.

### Configuration

- **Common config:** `config/storyteller-common.toml` - LLM settings, rate limits, NPC behavior
- **Character files:** `config/storyteller/characters/*.json`
- **Skin files:** `config/storyteller/skins/*.png` (64x64)

## Key Packages

| Package | Purpose |
|---------|---------|
| `com.storyteller` | Mod entry point (`StorytellerMod`) |
| `com.storyteller.entity` | `StorytellerNPC` entity, `ModEntities` registration |
| `com.storyteller.npc` | `NPCCharacter`, `NPCManager`, `ConversationHistory`, `WorldContext` |
| `com.storyteller.llm` | `LLMManager`, `LLMProvider` interface |
| `com.storyteller.llm.providers` | `OllamaProvider`, `ClaudeProvider`, `OpenAIProvider` |
| `com.storyteller.network` | Packet classes for client-server communication |
| `com.storyteller.client` | Rendering, UI, client-side event handling |
| `com.storyteller.config` | `ModConfig` NeoForge ConfigSpec |
| `com.storyteller.integration` | `EiraIntegrationManager` physical world bridge |

## In-Game Commands

- `/storyteller spawn [character]` - Spawn NPC
- `/storyteller list` - List available characters
- `/storyteller create <name>` - Create new character template
- `/storyteller reload` - Reload configurations
- `/storyteller status` - Show LLM connection status

## Development Notes

- Test characters go in `run/config/storyteller/characters/` (created on first runClient)
- Default LLM is Ollama at `localhost:11434` - install and run `ollama pull mistral:7b-instruct`
- LLM requests are async; responses handled via `CompletableFuture.thenAccept()`
- Rate limiting configured in common config (default: 20 ticks between messages, 10/minute max)
