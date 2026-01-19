# Storyteller NPCs

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4-green.svg)](https://minecraft.net)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.4.x-orange.svg)](https://neoforged.net)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**AI-powered NPCs for Minecraft that actually talk back.**

Storyteller NPCs adds characters to your Minecraft world that you can have real conversations with. Ask them questions, hear their stories, discover their secrets. Each NPC has their own personality, memories, and hidden motives - powered by large language models running locally or in the cloud.

---

## What is this?

Vanilla Minecraft villagers are disappointing. They grunt, they trade, they get killed by zombies. They don't *talk*.

Storyteller NPCs fixes that. You spawn an NPC, give them a personality, and have actual conversations:

```
You: "What brings you to this village?"

Eldric: "Ah, a curious soul. I came seeking the ruins beneath
the mountain - they say an artifact lies buried there. But the
nights grow dangerous, and I find myself... lingering. Tell me,
have you seen anything unusual in these lands?"
```

The NPC remembers what you've discussed. They know if it's raining. They notice you're hurt. They have secrets they'll only reveal after you've earned their trust.

---

## Why use it?

**For adventure map creators:** Build immersive quests with NPCs that guide players through stories, give contextual hints, and react to player choices - without scripting every possible dialogue.

**For server owners:** Add memorable characters to spawn towns, quest hubs, or special locations. NPCs can serve as guides, merchants with personality, or mysterious figures with secrets to uncover.

**For modpack developers:** Create narrative experiences that feel alive. NPCs respond naturally to any player input, making your world feel less like a game and more like a story.

**For solo players:** Give your world some company. Have someone to talk to on those long mining trips. Create characters that feel like actual inhabitants of your world.

---

## How it works

1. **You spawn an NPC** with `/storyteller spawn`
2. **Right-click to chat** - a dialogue screen opens
3. **Type anything** - your message goes to an AI language model
4. **Get a response** - the NPC replies in character, aware of time, weather, biome, and your conversation history

The AI runs locally via [Ollama](https://ollama.ai/) (free, private, no internet needed) or through cloud APIs (Claude, OpenAI) for stronger responses.

---

## Features

- **Real conversations** - NPCs respond to anything you type, not canned dialogue
- **Persistent memory** - They remember past conversations with you
- **World awareness** - NPCs know biome, time of day, weather, and your health
- **Player awareness** - NPCs notice what you're holding and comment on your gear
- **Achievement reactions** - NPCs react when you earn advancements or kill bosses
- **Auto quests** - NPCs can give you quests that are tracked automatically
- **Custom personalities** - Define traits, backstories, speech patterns, and secrets
- **Custom skins** - Any Minecraft skin works
- **Local-first** - Ollama runs on your machine, completely private
- **Cloud options** - Claude and OpenAI for enhanced responses
- **Eira integration** - Bridge to physical world via redstone and HTTP webhooks

---

## Quick Start

### 1. Install Ollama

Download from [ollama.ai](https://ollama.ai/), then:

```bash
ollama pull llama3.1:8b
ollama serve
```

### 2. Install the mod

Drop the JAR in your `mods/` folder. Requires NeoForge 21.4.x and Java 21.

### 3. Spawn an NPC

```
/storyteller spawn
```

Right-click to start talking.

---

## Configuration

After first launch, edit `config/storyteller-common.toml`:

```toml
[llm]
provider = "OLLAMA"  # or CLAUDE, OPENAI

[llm.ollama]
endpoint = "http://localhost:11434"
model = "llama3.1:8b"
```

For Claude or OpenAI, add your API key to the respective section.

---

## Creating Characters

Create JSON files in `config/storyteller/characters/`:

```json
{
  "id": "village-elder",
  "name": "Old Mira",
  "title": "The Village Elder",

  "personality": {
    "traits": ["wise", "cautious", "kind"],
    "backstory": "Lived in this village for 60 years. Remembers when the forest was safe.",
    "motivation": "Protect the village and its people",
    "quirks": ["calls everyone 'child'", "always mentions the weather"]
  },

  "hidden_agenda": {
    "secret": "Knows the location of the ancient temple but won't reveal it until she trusts you",
    "reveal_conditions": ["After 5+ conversations", "If player helps defend the village"]
  },

  "speech_style": {
    "vocabulary": "simple, folksy",
    "common_phrases": ["In my day...", "The old ways taught us..."]
  }
}
```

Or use the command: `/storyteller create "Old Mira"`

---

## Commands

| Command | Description |
|---------|-------------|
| `/storyteller` | Show help |
| `/storyteller spawn [name]` | Spawn default or named NPC |
| `/storyteller list` | List available characters |
| `/storyteller create <name>` | Create new character template |
| `/storyteller reload` | Reload configurations |
| `/storyteller status` | Show LLM connection status |

---

## Recommended Models

| Model | VRAM | Notes |
|-------|------|-------|
| `llama3.1:8b` | ~5GB | Good balance of speed and quality |
| `mistral:7b-instruct` | ~4GB | Fast, good for roleplay |
| `llama3.2:3b` | ~2GB | For lower-spec machines |

---

## Troubleshooting

**NPC says "Ollama is not available"**
- Run `ollama serve` in a terminal
- Check `ollama list` shows your model

**Responses are slow**
- Use a smaller model
- Make sure Ollama has enough RAM
- Responses are limited to ~150 tokens by default

**NPC won't respond**
- Check `/storyteller status`
- Look at server logs for errors

---

## Dynamic NPC Features

### Achievement & Event Reactions

NPCs are aware of your recent accomplishments and will react naturally:

```
[You just earned "Monster Hunter" advancement]

You: "Hello there!"

Eldric: "Well met, brave one! Word travels fast - I heard you've
been slaying monsters. The village could use someone with your skills."
```

**Events NPCs notice:**
- Advancements earned (last 5 minutes)
- Boss kills (Ender Dragon, Wither, Elder Guardian, Warden)
- Notable mob kills (Evoker, Ravager, etc.)
- Rare items you're carrying (Netherite gear, Elytra, Totems)

### Item Awareness

NPCs see what you're holding and can comment on it:

```
[You're holding an enchanted Diamond Sword]

You: "Can you help me?"

Blacksmith: "That's a fine blade you've got there - enchanted too!
I see you take your craft seriously. What do you need?"
```

NPCs notice:
- Main hand and off-hand items
- Enchantments on gear
- Damaged/worn equipment
- Rare and valuable items

### Automatic Quest System

When an NPC says something quest-like, it's automatically tracked:

```
Merchant: "I desperately need 10 iron ingots for my forge.
Bring them to me and I'll reward you handsomely!"

[Quest Added: Collect 10 iron ingot]
```

**Quest types detected:**
- **Collection quests**: "bring me X", "find me X", "collect X"
- **Kill quests**: "kill X zombies", "slay the dragon"

Quests are tracked per-player and NPCs remember your progress:

```
[You return with 10 iron ingots]

You: "I have those ingots you wanted."

Merchant: "Excellent! You've fulfilled your promise. Here is your reward."
```

---

## Part of the Eira Ecosystem

Storyteller NPCs is part of **Eira** - a collection of Minecraft mods for immersive educational experiences, maintained by a non-commercial educational organization.

| Mod | Purpose | Repository |
|-----|---------|------------|
| **Eira Core** | Foundation - event bus, teams, story framework, API server | [teenne/eira-core](https://github.com/teenne/eira-core) |
| **Eira Relay** | Physical bridge - HTTP blocks convert webhooks ↔ redstone | [Narratimo/HappyHttpMod](https://github.com/Narratimo/HappyHttpMod) |
| **Eira NPC** | AI characters - LLM-powered dynamic conversations | This repository |

### How they work together

**Physical → Minecraft:** A visitor scans a QR code → webhook hits Eira Relay's HTTP Receiver block → redstone signal triggers NPC dialogue → the NPC "senses" something and speaks to nearby players.

**Minecraft → Physical:** Player discovers an NPC's secret → NPC emits redstone → Eira Relay's HTTP Sender fires webhook → room lights change color or a display updates.

This enables escape rooms, museum exhibits, educational installations, and interactive storytelling that spans both digital and physical spaces.

### Enabling Eira Integration

In `storyteller-common.toml`:

```toml
[integration]
eiraEnabled = true
emitRedstoneOnEvents = true

[integration.webhooks]
enabled = true
onSecretRevealed = "https://your-server.com/webhook"
```

See [Eira Integration Guide](docs/EIRA_INTEGRATION.md) for full setup.

---

## Documentation

- [User Guide](docs/USER_GUIDE.md) - Complete setup instructions
- [Character Guide](docs/CHARACTER_GUIDE.md) - Creating custom NPCs
- [Eira Integration](docs/EIRA_INTEGRATION.md) - Physical world bridge
- [Architecture](docs/ARCHITECTURE.md) - Technical details

---

## Building from Source

```bash
git clone https://github.com/teenne/Eira-NPC.git
cd Eira-NPC
./gradlew build
```

---

## License

MIT License - see [LICENSE](LICENSE)
