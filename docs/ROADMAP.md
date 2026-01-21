# Project Roadmap
# Storyteller NPCs Mod

This document tracks the development progress and planned features.

---

## Development Status

| Phase | Status | Progress |
|-------|--------|----------|
| Phase 1: MVP | **Complete** | 100% |
| Phase 2: Polish | 🔄 In Progress | 40% |
| Phase 3: Extended Features | ⏳ Planned | 0% |
| Phase 4: Multi-Loader | ⏳ Future | 0% |

---

## Phase 1: MVP (Complete)

### Core Features (All Done)

- [x] Project setup and build system
- [x] Documentation structure
- [x] LLM Provider interface and implementations (Ollama, Claude, OpenAI)
- [x] NPCCharacter data model and JSON system
- [x] System prompt generation with world context
- [x] ConversationHistory manager with persistence
- [x] StorytellerNPC entity with custom rendering
- [x] Network packets for client-server communication
- [x] NPCChatScreen GUI
- [x] Server commands (/storyteller)
- [x] NeoForge 1.21.4 compatibility
- [x] Testing infrastructure (89 tests)

### PR Deliverables (All Merged)

| PR | Feature | Status |
|----|---------|--------|
| #1 | Quest notifications | Merged |
| #2 | Quest commands | Merged |
| #3 | Conversation persistence | Merged |
| #4 | Thinking particles | Merged |
| #5 | Chat sounds | Merged |
| #6 | Default characters | Merged |
| #7 | Feature toggles, event tracking, item awareness | Merged |
| #8 | NPC behaviour modes (stationary, anchored, follow, hiding) | Merged |
| #9 | Knowledge bases / RAG system | Merged |

---

## Phase 2: Polish (In Progress)

### Planned PRs

| PR | Feature | Priority | Complexity | Status |
|----|---------|----------|------------|--------|
| #10 | Emotion/mood system | High | Medium | Planned |
| #11 | Custom quest rewards | Medium | Low | Planned |
| #12 | NPC-to-NPC conversations | High | High | Planned |
| #13 | Voice synthesis (TTS) | Medium | High | Planned |
| #14 | Relationship system | Medium | Medium | Planned |

### Additional Polish Items

- [ ] Improved skin system (online fetching)
- [ ] Enhanced chat UI (timestamps, copy, export)
- [ ] In-game character editor GUI
- [ ] Config GUI integration
- [ ] Performance profiling and optimization

---

## Phase 3: Extended Features (Future)

### Long-Term Memory
- [ ] Vector embedding storage for conversations
- [ ] Cross-session memory retrieval
- [ ] Semantic search for relevant memories
- [ ] Memory importance scoring and pruning

### Advanced AI Features
- [ ] Adaptive personality evolution
- [ ] Player profile building
- [ ] Emotional state machine
- [ ] Context-aware topic selection

### NPC Behaviours
- [ ] Daily schedules (sleep, work, eat)
- [ ] Combat companion mode
- [ ] Trading with dynamic pricing
- [ ] Group/faction behaviours

### Quest System Expansion
- [ ] Quest chains with dependencies
- [ ] Branching quest outcomes
- [ ] Timed quests
- [ ] Multiplayer quest sharing

### Mod Integration
- [ ] Public API for other mods
- [ ] JEI integration
- [ ] Create mod integration
- [ ] Curios/accessories support

---

## Phase 4: Multi-Loader Support (Future)

### Fabric Port
- [ ] Create Fabric-compatible project structure
- [ ] Implement Fabric networking
- [ ] Fabric config system
- [ ] Fabric entity registration
- [ ] Testing on Fabric

### Unified Codebase
- [ ] Extract common code module
- [ ] Platform abstraction layer
- [ ] Automated multi-build system
- [ ] Unified release process

---

## Known Issues

| Issue | Severity | Status |
|-------|----------|--------|
| Skin loading can fail silently | Low | Known |
| Rate limit UX on first interaction | Low | Known |
| HIDING NPCs may get stuck in corners | Low | Known |

---

## Technical Debt

| Item | Impact | Effort | Status |
|------|--------|--------|--------|
| Unit tests for core classes | - | - | Done (89 tests) |
| Integration tests | Medium | High | Planned |
| Error message improvements | Medium | Low | Planned |
| Packet handling refactor | Low | Medium | Backlog |
| Internal API documentation | Low | Medium | Backlog |
| Performance profiling | Unknown | Medium | Planned |

---

## Ideas & Wishlist

*Features under consideration but not committed:*

- [ ] Dream sequences (special night conversations)
- [ ] NPC death/respawn mechanics
- [ ] Custom NPC sounds/voices
- [ ] VR support considerations
- [ ] Multiplayer NPC ownership
- [ ] Seasonal/event behaviours
- [ ] NPC building assistance
- [ ] Physical installation toolkit

---

## Version Targets

| Version | Scope | Status |
|---------|-------|--------|
| 0.8.0-beta | MVP + PRs #1-8 | Complete |
| 0.9.0-beta | + Knowledge/RAG (#9) | **Current** |
| 0.10.0-beta | + Emotion, Rewards (#10-11) | Planned |
| 1.0.0 | + NPC-NPC, TTS, Relationships (#12-14) | Planned |
| 1.1.0 | Quest chains, combat companion | Future |
| 2.0.0 | Multi-loader support | Future |

---

## Contributing to Roadmap

Have feature ideas?

1. Check if already listed above
2. Open a GitHub Issue
3. Describe the use case and user value
4. Community discussion on priority
5. Maintainers add to roadmap

---

## PR Implementation Plans

See [PR_PLAN.md](PR_PLAN.md) for detailed implementation plans for upcoming PRs.

---

*Last updated: January 2026*
