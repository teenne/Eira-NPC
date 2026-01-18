# Storyteller NPCs - AI-Powered Minecraft Characters

A NeoForge mod that adds AI-powered NPCs to Minecraft. These characters can tell stories, give hints for adventures, and have their own personalities, motives, and hidden agendas - all powered by local LLMs (Ollama) or cloud APIs (Claude, OpenAI).

## Features

- 🎭 **Dynamic AI Characters** - NPCs with unique personalities, backstories, and hidden agendas
- 🗣️ **Natural Conversations** - Powered by LLMs for dynamic, context-aware dialogue
- 🌍 **World-Aware** - NPCs know about biomes, time of day, weather, and your condition
- 🎨 **Custom Skins** - Use any Minecraft player skin for your NPCs
- 🔒 **Local-First** - Works with Ollama for complete privacy and offline play
- ☁️ **Cloud Options** - Optional Claude or OpenAI integration for enhanced responses
- 📚 **Multiple Characters** - Create and manage many different NPC personalities

## Requirements

- Minecraft 1.21.4
- NeoForge 21.4.x
- Java 21
- **For local LLM**: [Ollama](https://ollama.ai/) running on your machine

## Quick Start

### 1. Install Ollama (Recommended)

```bash
# Install Ollama from https://ollama.ai/

# Pull the recommended model
ollama pull mistral:7b-instruct

# Start Ollama (if not running as service)
ollama serve
```

### 2. Install the Mod

1. Download the mod JAR from releases
2. Place in your `mods/` folder
3. Launch Minecraft with NeoForge 1.21.4

### 3. Spawn Your First NPC

```
/storyteller spawn
```

Right-click the NPC to start chatting!

## Configuration

Configuration files are created in `config/storyteller/`:

```
config/storyteller/
├── storyteller-common.toml    # LLM and NPC settings
├── storyteller-client.toml    # Client-side settings
├── characters/                 # Character JSON files
│   └── [character-id].json
└── skins/                      # Custom skin PNG files
    └── README.txt
```

### LLM Provider Settings

Edit `storyteller-common.toml`:

```toml
[llm]
# Options: OLLAMA, CLAUDE, OPENAI
provider = "OLLAMA"

[llm.ollama]
endpoint = "http://localhost:11434"
model = "mistral:7b-instruct"
timeout = 60

[llm.claude]
apiKey = ""  # Your Anthropic API key
model = "claude-sonnet-4-20250514"

[llm.openai]
apiKey = ""  # Your OpenAI API key
model = "gpt-4o"
```

### Recommended Ollama Models

| Model | Size | Best For |
|-------|------|----------|
| `mistral:7b-instruct` | ~4GB | **Recommended** - Great roleplay, fast |
| `llama3.1:8b` | ~4.5GB | Good general purpose |
| `nous-hermes2:10.7b` | ~6GB | Best roleplay quality |
| `llama3.2:3b` | ~2GB | Lower spec machines |

## Creating Characters

### Using Commands

```bash
# Create a new character with default template
/storyteller create "Mysterious Wizard"

# List all characters
/storyteller list

# Check status
/storyteller status
```

### Manual JSON Creation

Create a JSON file in `config/storyteller/characters/`:

```json
{
  "id": "unique-id",
  "name": "Character Name",
  "title": "The Character's Title",
  "skinFile": "myskin.png",
  "slimModel": false,
  
  "personality": {
    "traits": ["wise", "mysterious", "helpful"],
    "backstory": "A traveler from distant lands...",
    "motivation": "Seeking ancient knowledge...",
    "fears": ["the void", "being forgotten"],
    "quirks": ["speaks in riddles", "collects emeralds"]
  },
  
  "hidden_agenda": {
    "short_term_goal": "Gain the player's trust",
    "long_term_goal": "Guide them to find the artifact",
    "secret": "Is actually cursed and needs help",
    "reveal_conditions": [
      "After many conversations",
      "When player shows exceptional kindness"
    ]
  },
  
  "speech_style": {
    "vocabulary": "archaic, poetic",
    "common_phrases": ["Ah, young traveler...", "The blocks remember..."],
    "avoid_phrases": ["As an AI", "I cannot"]
  }
}
```

### Custom Skins

1. Download or create a Minecraft skin (64x64 PNG)
2. Place in `config/storyteller/skins/`
3. Reference in character JSON: `"skinFile": "filename.png"`

Skin sources:
- [NameMC](https://namemc.com/)
- [The Skindex](https://www.minecraftskins.com/)
- [Planet Minecraft](https://www.planetminecraft.com/skins/)

## Commands

| Command | Description |
|---------|-------------|
| `/storyteller spawn [character]` | Spawn an NPC (default or named) |
| `/storyteller list` | List all characters |
| `/storyteller skins` | List available skin files |
| `/storyteller create <name>` | Create new character template |
| `/storyteller reload` | Reload all configurations |
| `/storyteller status` | Show LLM and mod status |

## How It Works

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   Player    │────▶│ Storyteller  │────▶│   Ollama    │
│  (Client)   │◀────│    NPC       │◀────│   (LLM)     │
└─────────────┘     └──────────────┘     └─────────────┘
                           │
                           ▼
                    ┌──────────────┐
                    │   Context    │
                    │  • Biome     │
                    │  • Time      │
                    │  • Weather   │
                    │  • History   │
                    └──────────────┘
```

1. Player right-clicks NPC → Opens chat screen
2. Player types message → Sent to server
3. Server builds context (world state, conversation history)
4. Request sent to LLM with character system prompt
5. Response returned and displayed to player
6. Conversation history maintained per player

## Tips for Great Characters

1. **Be Specific** - Detailed backstories lead to consistent roleplay
2. **Hidden Agendas** - Give NPCs secrets to make conversations intriguing  
3. **Speech Patterns** - Unique phrases make characters memorable
4. **World Integration** - Reference Minecraft elements naturally
5. **Gradual Reveals** - Let secrets emerge over multiple conversations

## Troubleshooting

### "Ollama is not available"
- Ensure Ollama is running: `ollama serve`
- Check the endpoint in config (default: `http://localhost:11434`)
- Verify the model is downloaded: `ollama list`

### Slow Responses
- Use a smaller model (`llama3.2:3b`)
- Reduce `maxConversationHistory` in config
- Ensure Ollama has enough RAM

### NPC Won't Respond
- Check `/storyteller status` for LLM connection
- Look at server logs for errors
- Ensure player is within 10 blocks of NPC

## Building from Source

```bash
git clone https://github.com/yourusername/storyteller
cd storyteller
./gradlew build
```

The mod JAR will be in `build/libs/`.

## Future Plans

- [ ] Multi-loader support (Fabric/Forge)
- [ ] Quests and objectives system
- [ ] NPC-to-NPC conversations
- [ ] Voice synthesis integration
- [ ] Memory persistence across server restarts
- [ ] Web UI for character creation

## License

MIT License - See LICENSE file

## Credits

- Built for NeoForge 1.21.4
- LLM integration via Ollama, Anthropic Claude, and OpenAI
- Inspired by the desire for more immersive Minecraft NPCs
