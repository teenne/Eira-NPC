# Storyteller NPCs

AI-powered conversational NPCs for Minecraft that respond dynamically to player input using large language models.

Storyteller NPCs solves the problem of static, scripted dialogue in Minecraft. Rather than pre-written responses, NPCs use LLMs (running locally via Ollama or through cloud APIs) to generate contextual responses based on their defined personality, the game world state, and conversation history. This is useful for adventure map creators, educational installations, and anyone who wants NPCs that feel alive rather than mechanical.

---

## Project Status and Maturity

**Status: Beta**

The mod is functional and actively maintained. Core features (conversations, personalities, quests, persistence) are stable. The API and configuration format may change between minor versions. Expect occasional bugs, particularly around edge cases in LLM response parsing.

Active maintenance means:
- Bug reports are triaged within a week
- Security issues are addressed promptly
- Pull requests are reviewed, though turnaround varies

---run 

## Why This Project Exists

Existing Minecraft NPC solutions fall into two categories: simple villager-like entities with no dialogue, or complex scripting systems that require writing every possible response. Neither approach creates NPCs that feel like actual characters.

Large language models change this equation. An LLM can generate contextually appropriate responses to arbitrary player input, maintaining character consistency without exhaustive scripting. The challenge is integrating LLMs into Minecraft's architecture cleanly.

Storyteller NPCs was built with these principles:
- **Local-first**: Default to Ollama so users control their data and costs
- **Character-driven**: Personalities defined in simple JSON, not code
- **World-aware**: NPCs know about time, weather, biomes, and player state
- **Non-blocking**: LLM requests happen asynchronously to avoid server lag

---

## Core Features

- **Dynamic conversations**: NPCs respond to any text input using configured LLM
- **Persistent memory**: Conversation history survives server restarts
- **World context**: NPCs aware of biome, time, weather, player health, held items
- **Automatic quest detection**: Parses NPC dialogue for quest-like statements and tracks progress
- **Player event awareness**: NPCs react to recent advancements and notable kills
- **Character system**: JSON-defined personalities with traits, backstories, secrets, and speech patterns
- **Custom skins**: Standard Minecraft skin format (64x64 PNG)
- **Multiple LLM providers**: Ollama (local), Claude (Anthropic), OpenAI
- **Behaviour modes**: Stationary, anchored to location, follow player, or hide from player
- **Knowledge bases (RAG)**: Give NPCs specific factual knowledge via keyword-based retrieval
- **Eira integration**: Redstone and webhook bridge for physical installations

### Non-Features

This project does not aim to:
- Replace scripted dialogue systems for deterministic gameplay
- Provide voice synthesis or text-to-speech
- Support multiplayer conversations (one player per NPC at a time)
- Work without an LLM backend (Ollama or API key required)

---

## Quick Start

### Prerequisites
- Minecraft 1.21.4 with NeoForge 21.4.x
- Java 21
- Ollama installed and running (or Claude/OpenAI API key)

### Installation

1. Install Ollama from [ollama.ai](https://ollama.ai/)
2. Pull a model and start the server:
   ```bash
   ollama pull llama3.1:8b
   ollama serve
   ```
3. Place the mod JAR in your `mods/` folder
4. Launch Minecraft

### First NPC

```
/storyteller spawn
```

Right-click the spawned NPC to open the chat interface. Type anything.

---

## Usage Overview

### Spawning NPCs

```
/storyteller spawn                    # Default character
/storyteller spawn village-elder      # Named character
/storyteller list                     # Available characters
```

### Character Definition

Characters are JSON files in `config/storyteller/characters/`. Example:

```json
{
  "id": "village-elder",
  "name": "Old Mira",
  "personality": {
    "traits": ["wise", "cautious"],
    "backstory": "Lived here for sixty years.",
    "motivation": "Protect the village"
  },
  "hidden_agenda": {
    "secret": "Knows the temple location",
    "reveal_conditions": ["After 5+ conversations"]
  },
  "speech_style": {
    "vocabulary": "simple, folksy",
    "common_phrases": ["In my day..."]
  }
}
```

### Behaviour Modes

NPCs support four behaviour modes:

| Mode | Description |
|------|-------------|
| `stationary` | Stays in place (default) |
| `anchored` | Wanders within radius of anchor point |
| `follow` | Follows a specific player |
| `hiding` | Hides from players using line-of-sight |

```
/storyteller behavior nearest anchored 15
/storyteller behavior nearest follow
/storyteller behavior nearest hiding
```

### Configuration

Edit `config/storyteller-common.toml`:

```toml
[llm]
provider = "OLLAMA"

[llm.ollama]
endpoint = "http://localhost:11434"
model = "llama3.1:8b"
```

---

## Architecture and Design Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        Client                                │
│  NPCChatScreen ──────────────────────► StorytellerNPCRenderer│
└──────────────────────────┬──────────────────────────────────┘
                           │ Packets
┌──────────────────────────▼──────────────────────────────────┐
│                        Server                                │
│  StorytellerNPC ◄────► NPCManager ◄────► NPCCharacter       │
│        │                                                     │
│        ▼                                                     │
│  ConversationHistory ◄──► LLMManager ◄──► Providers         │
│        │                                    (Ollama/Claude)  │
│        ▼                                                     │
│  WorldContext, QuestManager, PlayerEventTracker             │
└─────────────────────────────────────────────────────────────┘
```

**Key components:**
- `StorytellerNPC`: Entity with synced data, AI goals, and conversation state
- `LLMManager`: Async HTTP requests to LLM providers via CompletableFuture
- `ConversationHistory`: Per-player message storage with persistence
- `WorldContext`: Extracts game state (biome, time, weather) for prompts
- `NPCCharacter`: Deserialised character JSON with personality data

**Extension points:**
- Add LLM providers by implementing `LLMProvider` interface
- Add AI behaviours by creating custom `Goal` classes
- Add world context by extending `WorldContext.build()`

---

## Contributing

Contributions welcome in these areas:
- Bug fixes with clear reproduction steps
- New LLM provider integrations
- Performance improvements to pathfinding and LOS checks
- Documentation improvements

Before starting significant work, open an issue to discuss the approach. This avoids wasted effort on changes that don't fit the project direction.

**Expectations:**
- Code compiles with `./gradlew build`
- New features include basic documentation
- Follow existing code style (no enforced formatter, just consistency)

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

---

## Governance and Decision-Making

This project is maintained by the Eira educational organisation. Day-to-day decisions are made by active maintainers. Significant changes (API breaks, new dependencies, architectural shifts) are discussed in GitHub issues before implementation.

Disagreements are resolved through discussion. If consensus cannot be reached, maintainers have final say for their areas of responsibility.

Roadmap items are tracked in [docs/ROADMAP.md](docs/ROADMAP.md). The roadmap reflects current intentions, not commitments.

---

## Licence and Usage Expectations

MIT License. See [LICENSE](LICENSE).

The project is free for any use, including commercial. However, please be mindful that:
- LLM outputs may contain unexpected content; implement appropriate filtering for public-facing deployments
- Cloud API costs are your responsibility
- The Eira ecosystem is designed for educational contexts

---

## Community and Support

- **Bug reports**: [GitHub Issues](https://github.com/teenne/Eira-NPC/issues)
- **Questions**: GitHub Discussions or Issues
- **Feature requests**: Open an issue with rationale

Response times vary. Maintainers are volunteers with other commitments. Well-documented issues with reproduction steps receive faster attention than vague reports.

Please be respectful and constructive. Demanding responses or aggressive language will be ignored.

---

## Part of the Eira Ecosystem

Storyteller NPCs integrates with other Eira mods for physical-digital bridging:

| Mod | Purpose |
|-----|---------|
| **Eira Core** | Event bus, teams, story framework |
| **Eira Relay** | HTTP ↔ redstone conversion |
| **Eira NPC** | This project |

Together, these enable installations where physical world events (button presses, sensor triggers) affect in-game NPCs, and in-game events trigger physical outputs (lights, displays).

See [docs/EIRA_INTEGRATION.md](docs/EIRA_INTEGRATION.md) for setup.

---

## Closing Note

This project exists because we wanted NPCs that felt like characters rather than vending machines. It grew from experiments in educational game design, where static dialogue trees couldn't adapt to diverse learner questions.

If you're building something similar—adventure maps, educational games, interactive exhibits—we hope this saves you time. If you improve it along the way, consider contributing back.

---

## Development Roadmap

### Completed Features

| Feature | PR | Status |
|---------|------|--------|
| Core NPC entity & rendering | - | Done |
| LLM integration (Ollama, Claude, OpenAI) | - | Done |
| Character JSON system | - | Done |
| World context awareness | - | Done |
| Custom chat GUI | - | Done |
| Conversation history | - | Done |
| Quest notifications | #1 | Done |
| Quest commands | #2 | Done |
| Conversation persistence | #3 | Done |
| Thinking particles | #4 | Done |
| Chat sounds | #5 | Done |
| Default characters | #6 | Done |
| Feature config toggles | #7 | Done |
| Player event tracking | #7 | Done |
| Item awareness | #7 | Done |
| **NPC behaviour modes** | #8 | Done |
| **Knowledge bases (RAG)** | #9 | Done |

### In Progress / Next Up

| Feature | PR | Priority | Complexity | Description |
|---------|-----|----------|------------|-------------|
| Emotion/mood system | #10 | High | Medium | NPCs have emotional states that affect responses |
| Custom quest rewards | #11 | Medium | Low | NPCs give items/effects on quest completion |
| NPC-to-NPC conversations | #12 | High | High | NPCs can talk to each other within earshot |
| Voice synthesis (TTS) | #13 | Medium | High | Text-to-speech for NPC dialogue |
| Relationship tracking | #14 | Medium | Medium | NPCs remember and react to relationship quality |
| Combat companion mode | - | Medium | Medium | NPCs can fight alongside players |
| In-game character editor | - | Low | Medium | GUI for creating characters without JSON |
| Fabric/Forge ports | - | Low | High | Multi-loader support |

### Future Vision

| Feature | Description |
|---------|-------------|
| **Long-term memory** | NPCs remember across sessions using vector embeddings |
| **Adaptive personality** | NPCs evolve based on player interactions |
| **Group conversations** | Multiple players talking to one NPC |
| **NPC schedules** | NPCs have daily routines (sleep, work, eat) |
| **NPC trading** | Dynamic trading based on relationship and scarcity |
| **Dream sequences** | Special conversation modes triggered at night |
| **Mod integration API** | Public API for other mods to interact with NPCs |
| **Physical installation toolkit** | Turnkey system for museums/exhibits |

---

## Tech Debt, Bugs, and Unfinished Work

### Known Bugs

| Bug | Severity | Workaround |
|-----|----------|------------|
| Skin loading can fail silently | Low | Check file exists in `config/storyteller/skins/` |
| Rate limit message shows even on first interaction occasionally | Low | Wait a moment and retry |
| HIDING mode NPCs may get stuck in corners | Low | Set to STATIONARY temporarily |

### Technical Debt

| Item | Impact | Effort |
|------|--------|--------|
| No integration tests | Medium | High - need mock LLM server |
| Packet handling is verbose | Low | Medium - refactor to use record codecs |
| Error messages are developer-focused | Medium | Low - add user-friendly messages |
| No performance profiling done | Unknown | Medium - profile with large conversation histories |
| `rebuildGoals()` clears all goals | Low | Low - could be more surgical |
| Conversation history unbounded in memory | Medium | Low - add max in-memory limit |

### Unfinished / Partial Implementations

| Item | Current State | Remaining Work |
|------|---------------|----------------|
| Online skin fetching | Not started | Implement username → skin URL → texture |
| Config GUI | Not started | NeoForge config screen integration |
| Lip sync for TTS | Not started | Research and implement |
| Cross-NPC memory | Not started | Shared context between NPCs |
| Quest chains | Not started | Sequential quest dependencies |

### Code Quality Notes

- Test coverage: ~89 tests, focused on serialisation and history
- Missing: integration tests, pathfinding tests, packet tests
- Documentation: Javadoc sparse, inline comments adequate
- Thread safety: `ConcurrentHashMap` used correctly, but no stress testing done

---

## Recommended Next Development Stages

Based on user value, technical feasibility, and dependencies:

### Stage 1: Emotion System (PR #10)
**Why first:** Foundational for making NPCs feel more alive. Affects response generation without complex dependencies.

### Stage 2: Custom Quest Rewards (PR #11)
**Why second:** Completes the quest loop. Players need tangible rewards for engagement.

### Stage 3: NPC-to-NPC Conversations (PR #12)
**Why third:** High user value. Creates emergent storytelling. Requires emotion system to be meaningful.

### Stage 4: Voice Synthesis (PR #13)
**Why fourth:** Major accessibility and immersion improvement. Can be developed in parallel.

### Stage 5: Relationship System (PR #14)
**Why fifth:** Builds on emotion system. Enables long-term engagement mechanics.

See [docs/PR_PLAN.md](docs/PR_PLAN.md) for detailed implementation plans.

---

## Documentation

- [Character Guide](docs/CHARACTER_GUIDE.md) - Creating custom NPCs
- [Architecture](docs/ARCHITECTURE.md) - Technical details
- [Eira Integration](docs/EIRA_INTEGRATION.md) - Physical world bridge
- [FAQ](docs/FAQ.md) - Common questions
- [Changelog](CHANGELOG.md) - Version history
- [PR Plan](docs/PR_PLAN.md) - Detailed implementation roadmap
