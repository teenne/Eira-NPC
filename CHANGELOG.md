# Changelog

All notable changes to Storyteller NPCs will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Test Infrastructure**
  - JUnit 5, Mockito, MockWebServer, Awaitility dependencies
  - 89 unit tests across 6 test classes
  - NPCCharacter JSON serialization tests
  - ConversationHistory thread safety tests
  - LLM provider tests with MockWebServer

### Fixed
- **NeoForge 1.21.4 API Compatibility**
  - Entity renderer updated for new MobRenderer/HumanoidModel API
  - Created StorytellerNPCRenderState for render state management
  - Fixed entity spawn API (EntitySpawnReason replaces MobSpawnType)
  - Fixed entity type registration with ResourceKey
  - Fixed damage handling (isInvulnerableTo instead of hurt override)

### Planned
- Fabric/Forge multi-loader support
- Quest integration system
- Voice synthesis (TTS)
- NPC-to-NPC conversations
- Persistent memory across server restarts

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
- Async HTTP with OkHttp
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
- [Issue Tracker](https://github.com/yourusername/storyteller/issues)
- [Discord](#)
