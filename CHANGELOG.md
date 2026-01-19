# Changelog

All notable changes to Storyteller NPCs will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Quest Notifications** (PR #1)
  - Chat notifications when quests are added
  - Progress updates for kill quests
  - Completion messages with checkmark

- **Quest Commands** (PR #2)
  - `/storyteller quests` - List all active quests
  - `/storyteller quests clear` - Clear all quests (admin)

- **Conversation Persistence** (PR #3)
  - Conversations saved to disk automatically
  - Survives server restarts
  - Configurable max messages per conversation
  - Storage in `config/storyteller/conversations/`

- **Thinking Particles** (PR #4)
  - Enchantment particles appear above NPC while generating response
  - Can be disabled in client config

- **Chat Sounds** (PR #5)
  - Plays villager sound when NPC responds
  - Can be disabled in client config

- **Default Characters** (PR #6)
  - `village-elder.json` - Wise grandmother figure
  - `mysterious-stranger.json` - Enigmatic helper
  - `village-guard.json` - Knight seeking redemption

- **Feature Config Toggles** (PR #7)
  - `enableQuestSystem` - Toggle automatic quest detection
  - `showQuestNotifications` - Toggle quest chat messages
  - `showQuestProgress` - Toggle kill quest progress updates
  - `enableEventTracking` - Toggle player event tracking
  - `eventExpiryMinutes` - Configure event expiry time
  - `enableItemAwareness` - Toggle NPC item awareness

- **NPC Behavior Modes** (PR #8)
  - **STATIONARY** - Default mode, NPC stays in place with rare wandering
  - **ANCHORED** - NPC wanders within a configurable radius of an anchor position
  - **FOLLOW_PLAYER** - NPC follows a specific player at a configurable distance
  - **HIDING** - NPC hides from players using line-of-sight checks
  - New commands:
    - `/storyteller behavior <npc> info` - Show NPC's current behavior
    - `/storyteller behavior <npc> stationary` - Set to stationary mode
    - `/storyteller behavior <npc> anchored [radius]` - Set to anchored mode
    - `/storyteller behavior <npc> anchored here [radius]` - Anchor at current position
    - `/storyteller behavior <npc> follow [player]` - Set to follow mode
    - `/storyteller behavior <npc> hiding` - Set to hiding mode
  - NPC selector supports "nearest", display name, or UUID
  - Behavior settings persist through server restarts
  - Configurable defaults in `behavior_modes` config section

- **Player Event Tracking**
  - NPCs react to recent player achievements (advancements, boss kills)
  - Notable mob kills tracked (Dragon, Wither, Warden, Evoker, etc.)
  - Rare item detection (Netherite gear, Elytra, Totems, enchanted items)
  - Events expire after configurable time (default 5 minutes)

- **Automatic Quest System**
  - NPCs can give quests detected from natural dialogue
  - Collection quests: "bring me X", "find me X", "collect X"
  - Kill quests: "kill X mobs", "slay X creatures"
  - Per-player quest tracking with progress
  - Quest completion detection (item count, mob kills)
  - Quest context provided to NPCs for follow-up conversations

- **Item Awareness**
  - NPCs see player's main hand and off-hand items
  - Enchantment detection ("[enchanted]")
  - Durability status ("[worn]", "[badly damaged]")
  - Item count for stacks

- **LLM Warmup System**
  - Model pre-loaded during mod initialization
  - Faster first response times (2-5s instead of 30-60s)
  - Warmup happens while player is in menus

- **Returning Visitor Recognition**
  - NPCs greet new visitors differently from returning ones
  - Conversation history checked for previous interactions

- **Test Infrastructure**
  - JUnit 5, Mockito, Awaitility dependencies
  - 89 unit tests across 6 test classes
  - NPCCharacter JSON serialization tests
  - ConversationHistory thread safety tests
  - LLM provider unit tests

- **Documentation**
  - Comprehensive USER_GUIDE.md with step-by-step setup instructions
  - Complete configuration reference
  - Troubleshooting guide
  - Performance optimization tips

### Changed
- **HTTP Client**
  - Replaced OkHttp with Java 21's built-in `java.net.http.HttpClient`
  - Reduced JAR size from 2.7MB to ~100KB (no bundled dependencies)
  - Better compatibility with NeoForge's modular classloader

### Fixed
- **NeoForge 1.21.4 API Compatibility**
  - Entity renderer updated for new MobRenderer/HumanoidModel API
  - Created StorytellerNPCRenderState for render state management
  - Fixed entity spawn API (EntitySpawnReason replaces MobSpawnType)
  - Fixed entity type registration with ResourceKey
  - Fixed damage handling (isInvulnerableTo instead of hurt override)

- **Runtime Issues**
  - Fixed NoClassDefFoundError for HTTP dependencies at runtime

### Planned
- Fabric/Forge multi-loader support
- Voice synthesis (TTS)
- NPC-to-NPC conversations
- Custom quest rewards

---

## [1.0.0] - TBD

### Added
- **Core NPC System**
  - StorytellerNPC entity with player-like model
  - Custom skin support (64x64 PNG files)
  - Slim and wide model variants
  - Invulnerable by default

- **AI Conversation System**
  - Ollama integration (local LLM, default)
  - Claude API integration (Anthropic)
  - OpenAI API integration
  - Async request handling
  - Automatic provider fallback

- **Character System**
  - JSON-based character definitions
  - Personality traits and backstory
  - Hidden agendas with reveal conditions
  - Speech patterns and vocabulary
  - Multiple characters supported

- **World Awareness**
  - Biome detection
  - Time of day awareness
  - Weather awareness
  - Player health/hunger monitoring
  - Dimension detection

- **User Interface**
  - Custom chat screen GUI
  - Scrollable message history
  - "Thinking" indicators
  - Rate limiting feedback

- **Commands**
  - `/storyteller spawn [character]`
  - `/storyteller list`
  - `/storyteller skins`
  - `/storyteller create <name>`
  - `/storyteller reload`
  - `/storyteller status`

- **Configuration**
  - NeoForge config system
  - Per-provider settings
  - NPC behavior tuning
  - Rate limiting controls

### Technical
- NeoForge 1.21.4 support
- Java 21 required
- Async HTTP with Java's built-in HttpClient
- Thread-safe conversation history

---

## Version History

| Version | Minecraft | NeoForge | Status |
|---------|-----------|----------|--------|
| 1.0.0   | 1.21.4    | 21.4.x   | Development |

---

## Upgrade Notes

### Upgrading to 1.0.0
This is the initial release. No upgrade path needed.

### Future Migration Guides
Migration guides for future versions will be documented here.

---

## Links

- [Documentation](docs/)
- [Issue Tracker](https://github.com/teenne/Eira-NPC/issues)
- [Discord](#)
