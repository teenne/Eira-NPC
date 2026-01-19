stor# Storyteller NPCs - Complete User Guide

A comprehensive guide to installing, configuring, and using the Storyteller NPCs mod for Minecraft 1.21.4.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Installation](#2-installation)
3. [LLM Provider Setup](#3-llm-provider-setup)
4. [First Launch](#4-first-launch)
5. [Configuration Reference](#5-configuration-reference)
6. [Using the Mod](#6-using-the-mod)
7. [Creating Custom Characters](#7-creating-custom-characters)
8. [Custom Skins](#8-custom-skins)
9. [Eira Relay Integration](#9-eira-relay-integration)
10. [Troubleshooting](#10-troubleshooting)
11. [Performance Tips](#11-performance-tips)

---

## 1. Prerequisites

### Required Software

| Software | Version | Download |
|----------|---------|----------|
| Minecraft | 1.21.4 | [minecraft.net](https://minecraft.net) |
| NeoForge | 21.4.x | [neoforged.net](https://neoforged.net) |
| Java | 21+ | Usually bundled with Minecraft |

### LLM Provider (Choose One)

| Provider | Requirements | Cost |
|----------|--------------|------|
| **Ollama** (Recommended) | Local installation, 8GB+ RAM | Free |
| **Claude** | API key from Anthropic | Pay per use |
| **OpenAI** | API key from OpenAI | Pay per use |

### Hardware Requirements

**For Ollama (Local LLM):**

| Model Size | Minimum RAM | Recommended RAM | GPU VRAM |
|------------|-------------|-----------------|----------|
| 3B parameters | 8 GB | 12 GB | 4 GB |
| 7B parameters | 12 GB | 16 GB | 6 GB |
| 13B parameters | 16 GB | 32 GB | 10 GB |

**For Cloud APIs (Claude/OpenAI):**
- No special hardware requirements
- Stable internet connection

---

## 2. Installation

### Step 1: Install NeoForge

1. Download the NeoForge installer for Minecraft 1.21.4 from [neoforged.net](https://neoforged.net)
2. Run the installer and select "Install client"
3. Launch Minecraft once with the NeoForge profile to generate folders

### Step 2: Install the Mod

1. Download the Storyteller NPCs mod JAR file
2. Locate your Minecraft folder:
   - **Windows:** `%appdata%\.minecraft`
   - **macOS:** `~/Library/Application Support/minecraft`
   - **Linux:** `~/.minecraft`
3. Place the JAR file in the `mods` folder
4. Do NOT launch Minecraft yet - set up your LLM first

### Step 3: Verify Installation

After completing LLM setup and launching:
```
/storyteller status
```
This should show "Connected" for your chosen provider.

---

## 3. LLM Provider Setup

### Option A: Ollama (Recommended)

Ollama runs AI models locally on your computer. It's free, private, and works offline.

#### Install Ollama

**Windows:**
1. Download from [ollama.ai](https://ollama.ai)
2. Run the installer
3. Ollama starts automatically as a service

**macOS:**
```bash
# Using Homebrew
brew install ollama

# Or download from ollama.ai
```

**Linux:**
```bash
curl -fsSL https://ollama.com/install.sh | sh
```

#### Download a Model

Open a terminal/command prompt and run:

```bash
# Recommended model - good balance of quality and speed
ollama pull mistral:7b-instruct

# Alternative: Faster but less capable
ollama pull llama3.2:3b

# Alternative: Best roleplay quality (requires more RAM)
ollama pull nous-hermes2:10.7b
```

#### Verify Ollama is Running

```bash
# Check if Ollama is responding
curl http://localhost:11434/api/tags

# Or simply run
ollama list
```

You should see your downloaded model(s) listed.

#### Model Recommendations

| Model | Size | Speed | Quality | Best For |
|-------|------|-------|---------|----------|
| `mistral:7b-instruct` | 4.1 GB | Fast | Good | **Most users** |
| `llama3.2:3b` | 2.0 GB | Very Fast | Moderate | Low-spec machines |
| `llama3.1:8b` | 4.7 GB | Fast | Good | General use |
| `nous-hermes2:10.7b` | 6.1 GB | Medium | Excellent | Best roleplay |
| `dolphin-mixtral:8x7b` | 26 GB | Slow | Excellent | High-end systems |

---

### Option B: Claude (Anthropic)

Claude provides high-quality responses via cloud API.

#### Get an API Key

1. Create an account at [console.anthropic.com](https://console.anthropic.com)
2. Navigate to API Keys section
3. Create a new API key
4. Copy the key (you won't see it again)

#### Configure the Mod

Edit `config/storyteller-common.toml`:

```toml
[llm]
provider = "CLAUDE"

[llm.claude]
apiKey = "sk-ant-your-api-key-here"
model = "claude-sonnet-4-20250514"
```

#### Pricing Estimate

| Model | Cost per 1K conversations |
|-------|--------------------------|
| Claude Haiku | ~$0.50 |
| Claude Sonnet | ~$3.00 |
| Claude Opus | ~$15.00 |

---

### Option C: OpenAI

OpenAI's GPT models provide excellent conversational quality.

#### Get an API Key

1. Create an account at [platform.openai.com](https://platform.openai.com)
2. Navigate to API Keys
3. Create a new secret key
4. Copy the key

#### Configure the Mod

Edit `config/storyteller-common.toml`:

```toml
[llm]
provider = "OPENAI"

[llm.openai]
apiKey = "sk-your-openai-key-here"
model = "gpt-4o"
```

#### Available Models

| Model | Quality | Speed | Cost |
|-------|---------|-------|------|
| `gpt-4o` | Excellent | Fast | Higher |
| `gpt-4o-mini` | Good | Very Fast | Lower |
| `gpt-4-turbo` | Excellent | Medium | Higher |

---

## 4. First Launch

### Launch Minecraft

1. Open Minecraft Launcher
2. Select the NeoForge 1.21.4 profile
3. Click Play
4. Wait for the game to load

### Verify the Mod Loaded

1. From the main menu, click "Mods"
2. Find "Storyteller NPCs" in the list
3. It should show version and description

### Create or Load a World

1. Create a new world or load an existing one
2. Wait for the world to fully load

### Check LLM Connection

```
/storyteller status
```

Expected output:
```
Storyteller NPC Status:
  LLM Provider: Ollama (mistral:7b-instruct)
  Status: Connected
  Characters Loaded: 1
```

### Spawn Your First NPC

```
/storyteller spawn
```

A default NPC will appear near you!

### Start Chatting

1. Walk up to the NPC (within 5 blocks)
2. Right-click the NPC
3. A chat window opens
4. Type a message and press Enter
5. Wait for the NPC's response

Congratulations! You've successfully set up Storyteller NPCs!

---

## 5. Configuration Reference

Configuration files are created automatically on first launch.

### File Locations

```
.minecraft/
├── config/
│   ├── storyteller-common.toml     # Server/LLM settings
│   ├── storyteller-client.toml     # Client display settings
│   └── storyteller/
│       ├── characters/             # Character JSON files
│       │   └── default.json
│       └── skins/                  # Custom skin PNG files
│           └── README.txt
```

### Common Config (storyteller-common.toml)

```toml
#====================================
# LLM Provider Settings
#====================================
[llm]
# Options: OLLAMA, CLAUDE, OPENAI
provider = "OLLAMA"

#------------------------------------
# Ollama Settings
#------------------------------------
[llm.ollama]
# Ollama API endpoint URL
endpoint = "http://localhost:11434"
# Model to use
model = "mistral:7b-instruct"
# Request timeout in seconds
timeout = 60

#------------------------------------
# Claude Settings (Anthropic)
#------------------------------------
[llm.claude]
# Your Anthropic API key (leave empty to disable)
apiKey = ""
# Claude model to use
model = "claude-sonnet-4-20250514"

#------------------------------------
# OpenAI Settings
#------------------------------------
[llm.openai]
# Your OpenAI API key (leave empty to disable)
apiKey = ""
# OpenAI model to use
model = "gpt-4o"

#====================================
# NPC Behavior Settings
#====================================
[npc]
# Messages kept in conversation history per player (5-100)
maxConversationHistory = 20

# LLM response timeout in seconds (5-120)
responseTimeout = 30

# Include world context (biome, time, weather) in prompts
includeWorldContext = true

# Delay before showing 'thinking' particles (ticks, 20=1sec)
thinkingIndicatorDelay = 20

#====================================
# Rate Limiting
#====================================
[ratelimit]
# Minimum ticks between messages to same NPC (0-200)
minTimeBetweenMessages = 20

# Max messages per player per minute (0=unlimited)
maxMessagesPerMinute = 10

#====================================
# Eira Relay Integration
#====================================
[integration]
# Enable Eira integration
eiraEnabled = true

# Radius to detect redstone signals (1-16 blocks)
redstoneDetectionRadius = 5

# NPCs emit redstone on story events
emitRedstoneOnEvents = true

# Cooldown between redstone events (ticks)
redstoneCooldown = 40

#------------------------------------
# Webhook Settings
#------------------------------------
[integration.webhooks]
# Enable webhook callbacks
enabled = true

# Webhook timeout (milliseconds)
timeout = 5000

# Webhook URLs (leave empty to disable specific webhooks)
onConversationStart = ""
onSecretRevealed = ""
onQuestStarted = ""
onQuestCompleted = ""
onMoodChanged = ""
onDangerWarning = ""
```

### Client Config (storyteller-client.toml)

```toml
[client]
# Show particles when NPC is generating response
showThinkingParticles = true

# Play sound when NPC responds
playChatSound = true

# How long messages display (ticks, 60-600)
chatDisplayTime = 200
```

### Applying Config Changes

After editing config files:

```
/storyteller reload
```

Or restart Minecraft for all changes to take effect.

---

## 6. Using the Mod

### Commands Reference

| Command | Description | Permission |
|---------|-------------|------------|
| `/storyteller spawn` | Spawn default NPC at your location | OP |
| `/storyteller spawn <name>` | Spawn specific character | OP |
| `/storyteller list` | List all available characters | All |
| `/storyteller skins` | List available skin files | All |
| `/storyteller create <name>` | Create new character template | OP |
| `/storyteller reload` | Reload all configurations | OP |
| `/storyteller status` | Show LLM and mod status | All |

### Spawning NPCs

**Spawn the default character:**
```
/storyteller spawn
```

**Spawn a specific character:**
```
/storyteller spawn Eldric
```
(Use the character's name or ID)

**Spawn at specific coordinates:**
```
/summon storyteller:storyteller_npc ~ ~ ~ {CharacterId:"eldric-wizard"}
```

### Interacting with NPCs

1. **Approach**: Walk within 5 blocks of the NPC
2. **Open Chat**: Right-click the NPC
3. **Chat Screen**: A custom interface opens
4. **Type Message**: Enter your message at the bottom
5. **Send**: Press Enter
6. **Wait**: The NPC "thinks" (particles appear)
7. **Response**: The NPC's reply appears
8. **Continue**: Keep chatting or press Escape to close

### NPC Behavior

- **Invulnerable**: NPCs cannot be damaged
- **Stationary**: NPCs don't wander (by design)
- **Persistent**: NPCs remain when you leave/return
- **Per-Player Memory**: Each player has separate conversation history
- **World-Aware**: NPCs know time of day, weather, biome

### Tips for Good Conversations

1. **Be Specific**: Ask detailed questions for detailed answers
2. **Stay In-World**: Treat the NPC as a real character
3. **Build Rapport**: NPCs respond to friendliness
4. **Probe Secrets**: Ask about their past to uncover hidden agendas
5. **Reference Context**: "This cave is dark" prompts atmospheric responses

---

## 7. Creating Custom Characters

### Quick Start

1. Navigate to `config/storyteller/characters/`
2. Create a new file: `my-character.json`
3. Add this minimal template:

```json
{
  "id": "my-character",
  "name": "My Character",
  "title": "The Friendly Guide",
  "personality": {
    "traits": ["friendly", "helpful", "curious"],
    "backstory": "A traveler who enjoys meeting new adventurers."
  }
}
```

4. Reload: `/storyteller reload`
5. Spawn: `/storyteller spawn "My Character"`

### Complete Character Template

```json
{
  "id": "unique-id",
  "name": "Display Name",
  "title": "The Character's Title",
  "skinFile": "myskin.png",
  "slimModel": false,

  "personality": {
    "traits": ["trait1", "trait2", "trait3"],
    "backstory": "Character's history and background...",
    "motivation": "What drives this character...",
    "fears": ["fear1", "fear2"],
    "quirks": ["quirk1", "quirk2"]
  },

  "hidden_agenda": {
    "short_term_goal": "Immediate objective...",
    "long_term_goal": "Ultimate aim...",
    "secret": "The truth they hide...",
    "reveal_conditions": [
      "Condition 1 for revealing hints",
      "Condition 2 for revealing more"
    ]
  },

  "behavior": {
    "greeting_style": "How they greet players",
    "farewell_style": "How they say goodbye",
    "idle_actions": ["What they do when idle"]
  },

  "speech_style": {
    "vocabulary": "Description of word choices",
    "sentence_length": "Short, medium, or flowing",
    "common_phrases": [
      "Phrase they often say 1",
      "Phrase they often say 2"
    ],
    "avoid_phrases": [
      "As an AI",
      "I cannot",
      "In this game"
    ]
  }
}
```

### Field Reference

| Field | Required | Description |
|-------|----------|-------------|
| `id` | Yes | Unique identifier (no spaces) |
| `name` | Yes | Display name shown to players |
| `title` | No | Subtitle/title under name |
| `skinFile` | No | Custom skin filename |
| `slimModel` | No | Use slim (Alex) arms |
| `personality.traits` | No | List of personality adjectives |
| `personality.backstory` | No | Character history |
| `personality.motivation` | No | What drives them |
| `personality.fears` | No | What they're afraid of |
| `personality.quirks` | No | Unique habits |
| `hidden_agenda.*` | No | Secret goals and reveals |
| `speech_style.*` | No | How they speak |

### Example Characters

See the `examples/` folder for complete character examples:
- `merchant-character.json` - A mysterious trader
- `knight-character.json` - A haunted warrior
- `oracle-eira-demo.json` - Character with Eira integration

For detailed character creation guidance, see [CHARACTER_GUIDE.md](CHARACTER_GUIDE.md).

---

## 8. Custom Skins

### Supported Formats

- **Resolution**: 64x64 pixels (modern) or 64x32 pixels (legacy)
- **Format**: PNG with transparency support
- **Model Types**:
  - `slimModel: false` - Classic (Steve) 4-pixel arms
  - `slimModel: true` - Slim (Alex) 3-pixel arms

### Adding a Custom Skin

1. **Find or Create a Skin**
   - [NameMC](https://namemc.com/) - Search by player name
   - [The Skindex](https://www.minecraftskins.com/) - Browse/create
   - [Nova Skin](https://minecraft.novaskin.me/) - Online editor

2. **Place in Skins Folder**
   ```
   config/storyteller/skins/wizard.png
   ```

3. **Reference in Character JSON**
   ```json
   {
     "skinFile": "wizard.png",
     "slimModel": false
   }
   ```

4. **Reload**
   ```
   /storyteller reload
   ```

### List Available Skins

```
/storyteller skins
```

---

## 9. Eira Relay Integration

Eira Relay allows NPCs to interact with the physical world through redstone signals and webhooks.

### Overview

| Direction | Description | Example |
|-----------|-------------|---------|
| Physical to Minecraft | Redstone → NPC behavior | Button press triggers NPC dialogue |
| Minecraft to Physical | NPC event → Webhook | NPC reveals secret → Light changes |

### Redstone Detection

NPCs detect redstone signals within a configurable radius:

```toml
[integration]
redstoneDetectionRadius = 5
redstoneCooldown = 40
```

When redstone activates near an NPC, it triggers an external event that can influence the conversation.

### Webhook Events

Configure webhook URLs in the config to receive HTTP POST notifications:

```toml
[integration.webhooks]
onConversationStart = "http://localhost:8080/events/conversation"
onSecretRevealed = "http://localhost:8080/events/secret"
onQuestStarted = "http://localhost:8080/events/quest"
```

**Webhook Payload Example:**
```json
{
  "event": "secret_revealed",
  "npc": {
    "id": "eldric-wizard",
    "name": "Eldric",
    "character_id": "eldric-wizard"
  },
  "player": {
    "name": "Steve",
    "uuid": "..."
  },
  "data": {
    "secret_level": 2
  },
  "world": {
    "dimension": "minecraft:overworld",
    "time": "evening",
    "weather": "clear"
  },
  "timestamp": "2024-01-15T20:30:00Z"
}
```

For detailed Eira integration documentation, see [EIRA_INTEGRATION.md](EIRA_INTEGRATION.md).

---

## 10. Troubleshooting

### Common Issues

#### "Ollama is not available"

**Causes & Solutions:**

1. **Ollama not running**
   ```bash
   # Start Ollama
   ollama serve

   # Or check if it's already running
   ollama list
   ```

2. **Wrong endpoint in config**
   - Default: `http://localhost:11434`
   - Check `storyteller-common.toml`

3. **Model not downloaded**
   ```bash
   ollama pull mistral:7b-instruct
   ```

4. **Firewall blocking port 11434**
   - Allow Ollama through your firewall

#### NPC Not Responding

1. Check LLM connection: `/storyteller status`
2. Check server logs for errors
3. Ensure you're within 5 blocks of NPC
4. Check rate limiting (wait a few seconds between messages)

#### Slow Responses

1. **Use a smaller model**: `llama3.2:3b` instead of 7B
2. **Reduce history**: Lower `maxConversationHistory` in config
3. **Check system resources**: Ensure enough RAM available
4. **GPU acceleration**: Enable if you have a compatible GPU

#### Character Not Loading

1. Validate JSON syntax (use [jsonlint.com](https://jsonlint.com))
2. Check file is in correct folder: `config/storyteller/characters/`
3. Ensure file extension is `.json`
4. Check for duplicate IDs
5. Run `/storyteller reload`
6. Check server logs for JSON parsing errors

#### Skin Not Appearing

1. Verify file is PNG format (64x64)
2. Check filename matches exactly (case-sensitive)
3. Place in `config/storyteller/skins/`
4. Match `skinFile` value in character JSON
5. Run `/storyteller reload`

#### NPC Breaks Character

1. Add more specific `avoid_phrases` in character JSON
2. Strengthen personality traits with more detail
3. Add "stay in character" guidance in backstory
4. Try a more capable model (7B+ parameters)

### Log Files

**Client Logs:**
```
.minecraft/logs/latest.log
```

**Look for entries tagged `[storyteller/]`:**
```
[storyteller/]: Storyteller mod initialized!
[storyteller/]: Ollama connection successful
[storyteller/]: LLM provider Ollama (mistral:7b-instruct) initialized
```

### Debug Mode

Add to JVM arguments for verbose logging:
```
-Dstoryteller.debug=true
```

---

## 11. Performance Tips

### Optimizing Response Speed

| Setting | Faster | Slower |
|---------|--------|--------|
| Model size | 3B | 13B+ |
| Conversation history | 10 | 50 |
| World context | Off | On |

### Recommended Settings by Hardware

**Low Spec (8GB RAM, no GPU):**
```toml
[llm.ollama]
model = "llama3.2:3b"

[npc]
maxConversationHistory = 10
includeWorldContext = false
```

**Mid Spec (16GB RAM, GPU):**
```toml
[llm.ollama]
model = "mistral:7b-instruct"

[npc]
maxConversationHistory = 20
includeWorldContext = true
```

**High Spec (32GB RAM, 12GB+ VRAM):**
```toml
[llm.ollama]
model = "nous-hermes2:10.7b"

[npc]
maxConversationHistory = 40
includeWorldContext = true
```

### GPU Acceleration (Ollama)

Ollama automatically uses GPU if available. Verify with:
```bash
ollama run mistral:7b-instruct "Hello"
# Check task manager/htop for GPU usage
```

For NVIDIA GPUs, ensure CUDA drivers are installed.

### Multiple NPCs

- Each NPC makes separate LLM requests
- Spread NPCs apart to avoid simultaneous conversations
- Consider using cloud APIs for many concurrent NPCs

---

## Quick Reference Card

### Essential Commands

| Command | Action |
|---------|--------|
| `/storyteller spawn` | Spawn default NPC |
| `/storyteller list` | List characters |
| `/storyteller reload` | Reload configs |
| `/storyteller status` | Check connection |

### Key Folders

| Location | Contents |
|----------|----------|
| `config/storyteller-common.toml` | Main settings |
| `config/storyteller/characters/` | Character JSONs |
| `config/storyteller/skins/` | Skin PNGs |

### Interaction

1. Right-click NPC to chat
2. Type message, press Enter
3. Wait for response
4. Press Escape to close

---

## Need More Help?

- [Character Creation Guide](CHARACTER_GUIDE.md) - Detailed character design
- [FAQ](FAQ.md) - Frequently asked questions
- [Eira Integration](EIRA_INTEGRATION.md) - Physical world bridge
- [Architecture](ARCHITECTURE.md) - Technical details
- [GitHub Issues](https://github.com/teenne/Eira-NPC/issues) - Report bugs

---

*Last updated: January 2026*
