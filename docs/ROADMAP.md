# Project Roadmap & TODO
# Storyteller NPCs Mod

This document tracks the development progress and planned features.

---

## Development Status

| Phase | Status | Progress |
|-------|--------|----------|
| Phase 1: MVP | 🔄 In Progress | 60% |
| Phase 2: Polish | ⏳ Planned | 0% |
| Phase 3: Multi-Loader | ⏳ Planned | 0% |
| Phase 4: Extended | ⏳ Future | 0% |

---

## Phase 1: MVP (Core Functionality)

### ✅ Completed

- [x] Project setup and build system
- [x] Documentation structure (PRD, Architecture, etc.)
- [x] LLM Provider interface design
- [x] Ollama provider implementation
- [x] Claude provider implementation
- [x] OpenAI provider implementation
- [x] LLM Manager with fallback
- [x] NPCCharacter data model
- [x] System prompt generation
- [x] WorldContext builder
- [x] ConversationHistory manager
- [x] StorytellerNPC entity class
- [x] Entity registration
- [x] Network packet definitions
- [x] NPCChatScreen GUI
- [x] Client-side rendering (basic)
- [x] Server commands

### 🔄 In Progress

- [x] NeoForge 1.21.4 compatibility verification (API fixes complete)
- [x] Testing infrastructure (89 tests, 67 passing)
- [ ] Custom skin loading (full implementation)
- [ ] In-game testing with Ollama

### ⏳ Todo

- [ ] Entity AI improvements (look at player, idle animations)
- [ ] Sound effects for chat
- [ ] "Thinking" particle effects
- [ ] Config GUI (optional)
- [ ] Default character variations

---

## Phase 2: Polish

### Planned Features

- [ ] **Improved Skin System**
  - Dynamic texture loading
  - Skin preview in commands
  - Online skin fetching (by username)

- [ ] **Enhanced Chat UI**
  - Typing indicator animation
  - Message timestamps
  - Copy message feature
  - Conversation export

- [ ] **Better World Integration**
  - Nearby structure detection
  - Player inventory awareness (optional)
  - Recent mob encounters
  - Achievement/advancement awareness

- [ ] **Quality of Life**
  - In-game character editor GUI
  - Skin selector UI
  - Provider status indicator
  - Chat history persistence (optional)

- [ ] **Performance**
  - Response caching
  - Prompt optimization
  - Memory usage improvements

---

## Phase 3: Multi-Loader Support

### Fabric Port

- [ ] Create Fabric-compatible project structure
- [ ] Implement Fabric networking
- [ ] Fabric config system
- [ ] Fabric entity registration
- [ ] Testing on Fabric

### Forge (Legacy) Port

- [ ] Evaluate 1.20.x Forge support demand
- [ ] Create Forge-compatible structure
- [ ] Implement Forge networking
- [ ] Testing on Forge

### Unified Codebase

- [ ] Extract common code module
- [ ] Platform abstraction layer
- [ ] Automated multi-build system
- [ ] Unified release process

---

## Phase 4: Extended Features

### Quest System

- [ ] Simple quest data structure
- [ ] Quest assignment by NPCs
- [ ] Progress tracking
- [ ] Completion detection
- [ ] Rewards system
- [ ] Quest chains

### Voice Synthesis

- [ ] Research TTS options
- [ ] Local TTS integration (e.g., Piper)
- [ ] Character voice configuration
- [ ] Audio playback system
- [ ] Lip sync (stretch goal)

### NPC Relationships

- [ ] NPC-to-NPC conversation system
- [ ] Relationship tracking
- [ ] Group conversations
- [ ] Overheard dialogue

### Advanced AI Features

- [ ] Long-term memory persistence
- [ ] Cross-session continuity
- [ ] Player profile building
- [ ] Adaptive personality
- [ ] Emotional states

### Mod Integration

- [ ] API for other mods
- [ ] JEI integration (show NPC info)
- [ ] Curios/accessories support
- [ ] Create mod integration
- [ ] Supplementaries integration

---

## Known Issues

| Issue | Priority | Status |
|-------|----------|--------|
| Skin loading incomplete | Medium | In Progress |
| No persistence across restarts | Low | Planned |
| Rate limit UX could be better | Low | Planned |

---

## Ideas & Wishlist

*Features under consideration but not committed:*

- [ ] NPC following player (companion mode)
- [ ] NPC combat assistance
- [ ] NPC building assistance
- [ ] Multiplayer NPC ownership
- [ ] NPC trading integration
- [ ] Seasonal/event behaviors
- [ ] Dream sequences
- [ ] NPC death/respawn mechanics
- [ ] Custom NPC sounds/voices
- [ ] VR support considerations

---

## Technical Debt

- [x] Add unit tests for core classes (89 tests added)
- [ ] Add integration tests
- [ ] Improve error messages
- [ ] Refactor packet handling
- [ ] Document internal APIs
- [ ] Performance profiling

---

## Version Targets

| Version | Target Date | Scope |
|---------|-------------|-------|
| 0.1.0-alpha | TBD | Core MVP, Ollama only |
| 0.2.0-alpha | TBD | All providers, basic UI |
| 0.5.0-beta | TBD | Full MVP feature-complete |
| 1.0.0 | TBD | Stable release |
| 1.1.0 | TBD | Quest system |
| 2.0.0 | TBD | Multi-loader support |

---

## Contributing to Roadmap

Have feature ideas? 

1. Check if already listed above
2. Open a GitHub Discussion
3. Describe the use case
4. Community votes on priority
5. Maintainers add to roadmap

---

*Last updated: January 2026*
