# Eira Relay Integration Guide
# Storyteller NPCs + Eira Relay

This guide explains how to integrate Storyteller NPCs with [Eira Relay](https://github.com/eira/eira-relay) to create immersive experiences that bridge the physical and Minecraft worlds.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Integration Architecture](#2-integration-architecture)
3. [Setup](#3-setup)
4. [Inbound Events (World → NPC)](#4-inbound-events-world--npc)
5. [Outbound Events (NPC → World)](#5-outbound-events-npc--world)
6. [Event Types](#6-event-types)
7. [Configuration](#7-configuration)
8. [Examples](#8-examples)
9. [Troubleshooting](#9-troubleshooting)

---

## 1. Overview

### What is Eira Relay?

Eira Relay is a Minecraft mod that provides HTTP communication blocks:
- **HTTP Receiver Block**: Emits redstone when receiving HTTP requests
- **HTTP Sender Block**: Sends HTTP requests when receiving redstone

### Why Integrate?

Combining Storyteller NPCs with Eira Relay enables:

| Direction | Example |
|-----------|---------|
| Physical → Minecraft | QR code scan triggers NPC to tell a secret |
| Physical → Minecraft | Motion sensor activates NPC dialogue |
| Minecraft → Physical | NPC reveals secret → lights change color |
| Minecraft → Physical | Quest complete → sends SMS notification |
| Minecraft → Physical | Player angers NPC → room lights flicker |

### Integration Methods

Storyteller NPCs supports two integration methods:

1. **Redstone Integration** (Simple)
   - NPCs detect nearby redstone signals
   - NPCs can emit redstone to trigger actions
   - No mod dependency required

2. **HTTP API Integration** (Advanced)
   - Direct HTTP endpoints for NPC control
   - Webhook callbacks for NPC events
   - Richer event data and control

---

## 2. Integration Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        PHYSICAL WORLD                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │
│  │  QR Scanner │  │   Motion    │  │   Smart     │                  │
│  │             │  │   Sensor    │  │   Lights    │                  │
│  └──────┬──────┘  └──────┬──────┘  └──────▲──────┘                  │
│         │                │                │                          │
└─────────┼────────────────┼────────────────┼──────────────────────────┘
          │                │                │
          │    HTTP        │    HTTP        │    HTTP
          │    POST        │    POST        │    POST
          ▼                ▼                │
┌─────────────────────────────────────────────────────────────────────┐
│                         EIRA RELAY                                   │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                    HTTP Server (:8080)                       │    │
│  └─────────────────────────────────────────────────────────────┘    │
│         │                │                ▲                          │
│         ▼                ▼                │                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │
│  │  Receiver   │  │  Receiver   │  │   Sender    │                  │
│  │   Block     │  │   Block     │  │   Block     │                  │
│  │ /qr/scan    │  │ /motion     │  │ lights API  │                  │
│  └──────┬──────┘  └──────┬──────┘  └──────▲──────┘                  │
│         │ redstone       │ redstone       │ redstone                │
└─────────┼────────────────┼────────────────┼──────────────────────────┘
          │                │                │
          ▼                ▼                │
┌─────────────────────────────────────────────────────────────────────┐
│                      STORYTELLER NPCS                                │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                    Event Manager                             │    │
│  │  - Monitors redstone near NPCs                               │    │
│  │  - Triggers NPC behaviors on events                          │    │
│  │  - Emits redstone on story events                            │    │
│  └─────────────────────────────────────────────────────────────┘    │
│         │                │                │                          │
│         ▼                ▼                ▼                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │
│  │ NPC reveals │  │ NPC starts  │  │ NPC emits   │                  │
│  │   secret    │  │  quest      │  │  signal     │                  │
│  └─────────────┘  └─────────────┘  └─────────────┘                  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. Setup

### Prerequisites

- Minecraft 1.21.4 with NeoForge
- Storyteller NPCs mod installed
- Eira Relay mod installed
- Both mods configured

### Basic Setup Steps

1. **Install Both Mods**
   ```
   mods/
   ├── storyteller-1.0.0.jar
   └── eira-relay-1.21.1.jar
   ```

2. **Configure Eira Relay**
   - Default HTTP server: `http://localhost:8080`
   - Configure in `.minecraft/config/eira-relay.toml`

3. **Configure Storyteller**
   - Enable Eira integration in `storyteller-common.toml`:
   ```toml
   [integration.eira]
       enabled = true
       redstoneDetectionRadius = 5
       emitRedstoneOnEvents = true
   ```

4. **Place Blocks Near NPCs**
   - Place Eira Receiver/Sender blocks within detection radius of NPCs
   - Connect with redstone as needed

---

## 4. Inbound Events (World → NPC)

External events can trigger NPC behaviors.

### Method 1: Redstone Detection

NPCs automatically detect redstone signals within their configured radius.

**Setup:**
1. Place HTTP Receiver Block near NPC
2. Configure receiver endpoint (e.g., `/npc/trigger`)
3. When HTTP request received → redstone signal → NPC reacts

**NPC Reaction Configuration:**

In character JSON, add `external_triggers`:

```json
{
  "name": "Guardian Spirit",
  "external_triggers": {
    "redstone_on": {
      "action": "speak",
      "message": "I sense a disturbance in the physical realm...",
      "mood_shift": "alert"
    },
    "redstone_pulse": {
      "action": "reveal_hint",
      "hint_level": 1
    }
  }
}
```

### Method 2: HTTP API (Advanced)

Storyteller provides its own HTTP endpoints when Eira is present.

**Endpoint:** `POST /storyteller/npc/{npc-id}/trigger`

**Request:**
```json
{
  "event": "external_trigger",
  "source": "motion_sensor",
  "data": {
    "location": "front_door",
    "intensity": "high"
  }
}
```

**NPC Response:**
The NPC will incorporate this event into their next response contextually.

---

## 5. Outbound Events (NPC → World)

NPC story events can trigger physical world actions.

### Method 1: Redstone Emission

NPCs can emit redstone signals on specific events.

**Configuration:**

In character JSON:
```json
{
  "name": "The Oracle",
  "story_triggers": {
    "secret_revealed": {
      "emit_redstone": true,
      "redstone_strength": 15,
      "redstone_duration": 40
    },
    "quest_given": {
      "emit_redstone": true,
      "redstone_strength": 8
    },
    "player_angered_npc": {
      "emit_redstone": true,
      "redstone_pattern": "pulse_3x"
    }
  }
}
```

**Patterns:**
- `constant` - Steady signal for duration
- `pulse_Nx` - N pulses (e.g., `pulse_3x`)
- `fade` - Gradually decreasing strength
- `sos` - SOS pattern (emergency events)

### Method 2: Direct HTTP Callbacks

Configure webhook URLs for NPC events.

**Configuration (`storyteller-common.toml`):**

```toml
[integration.webhooks]
    # Called when NPC reveals part of their secret
    onSecretRevealed = "http://localhost:8080/lights/dramatic"
    
    # Called when player starts a quest
    onQuestStarted = "http://localhost:8080/notify/quest"
    
    # Called on significant conversation milestones
    onConversationMilestone = "http://localhost:8080/log/event"
```

**Webhook Payload:**

```json
{
  "event": "secret_revealed",
  "npc": {
    "id": "oracle-123",
    "name": "The Oracle"
  },
  "player": {
    "name": "Steve",
    "uuid": "..."
  },
  "data": {
    "secret_level": 2,
    "total_levels": 5,
    "conversation_count": 15
  },
  "timestamp": "2025-01-18T10:30:00Z"
}
```

---

## 6. Event Types

### Inbound Events (External → NPC)

| Event | Description | NPC Reaction |
|-------|-------------|--------------|
| `motion_detected` | Physical motion sensor triggered | NPC becomes alert, looks around |
| `qr_scanned` | QR code scanned | Unlock special dialogue |
| `button_pressed` | Physical button pressed | Tell a story, give hint |
| `time_trigger` | Scheduled event | Change mood, reveal info |
| `door_opened` | Physical door sensor | Greet visitor, warn of danger |
| `custom` | Any custom event | Configurable response |

### Outbound Events (NPC → External)

| Event | Trigger | Suggested Physical Action |
|-------|---------|--------------------------|
| `conversation_started` | Player begins chat | Ambient lighting change |
| `secret_hinted` | NPC drops a hint | Subtle light flicker |
| `secret_revealed` | Full secret exposed | Dramatic lighting |
| `quest_started` | NPC gives quest | Notification sound |
| `quest_completed` | Quest finished | Celebration effects |
| `npc_angered` | Player upset NPC | Lights dim/red |
| `npc_pleased` | Player pleased NPC | Lights brighten |
| `danger_warning` | NPC warns of danger | Alert/alarm |

---

## 7. Configuration

### Storyteller Config (`storyteller-common.toml`)

```toml
[integration]
    # Enable Eira Relay integration
    eiraEnabled = true

[integration.redstone]
    # Radius (blocks) to detect redstone signals
    detectionRadius = 5
    
    # Emit redstone on NPC events
    emitOnEvents = true
    
    # Default signal strength (1-15)
    defaultStrength = 15
    
    # Default duration in ticks (20 = 1 second)
    defaultDuration = 40

[integration.webhooks]
    # Enable webhook callbacks
    enabled = true
    
    # Timeout for webhook calls (ms)
    timeout = 5000
    
    # Retry failed webhooks
    retryCount = 2
    
    # Event webhook URLs (leave empty to disable)
    onConversationStart = ""
    onSecretRevealed = ""
    onQuestStarted = ""
    onQuestCompleted = ""
    onMoodChanged = ""
```

### Character-Level Config

```json
{
  "name": "Event-Aware NPC",
  
  "eira_integration": {
    "enabled": true,
    
    "inbound": {
      "react_to_redstone": true,
      "redstone_cooldown": 100,
      "event_handlers": {
        "redstone_on": {
          "inject_context": "Something in the physical world has activated.",
          "mood": "curious"
        }
      }
    },
    
    "outbound": {
      "emit_redstone": true,
      "events": {
        "secret_revealed": {
          "strength": 15,
          "duration": 60,
          "pattern": "pulse_3x"
        }
      },
      "webhooks": {
        "secret_revealed": "http://localhost:8080/dramatic/reveal"
      }
    }
  }
}
```

---

## 8. Examples

### Example 1: QR Code Unlocks NPC Secret

**Scenario:** Players find a physical QR code. Scanning it makes the NPC reveal a secret.

**Setup:**

1. Create QR code pointing to: `http://your-server:8080/secret/unlock`

2. Place Eira HTTP Receiver Block near NPC:
   - Endpoint: `/secret/unlock`
   - Emits redstone when triggered

3. Configure NPC character:
```json
{
  "name": "The Keeper",
  "external_triggers": {
    "redstone_on": {
      "action": "reveal_secret",
      "message": "Ah! You found the ancient mark. Very well, I shall tell you what I know...",
      "reveal_level": 1
    }
  }
}
```

4. Connect redstone from Receiver to area near NPC

**Flow:**
```
QR Scan → HTTP POST → Eira Receiver → Redstone → NPC detects → Reveals secret
```

### Example 2: NPC Warning Triggers Alarm

**Scenario:** When NPC warns about danger, a physical alarm sounds.

**Setup:**

1. Place Eira HTTP Sender Block near NPC:
   - URL: `http://home-assistant:8123/api/services/alarm/trigger`
   - Method: POST

2. Configure NPC:
```json
{
  "name": "The Watchman",
  "story_triggers": {
    "danger_warning": {
      "emit_redstone": true,
      "redstone_strength": 15
    }
  },
  "personality": {
    "traits": ["vigilant", "protective"],
    "backstory": "Guards the village from threats..."
  }
}
```

3. Connect redstone from NPC area to Sender Block

4. The LLM is instructed to emit "danger_warning" when contextually appropriate

**Flow:**
```
Player asks about danger → NPC warns → Emits redstone → Eira Sender → HTTP POST → Alarm
```

### Example 3: Motion Sensor Greets Visitor

**Scenario:** Physical motion sensor triggers NPC to greet someone entering a room.

**Setup:**

1. Motion sensor sends POST to: `http://minecraft:8080/entrance/motion`

2. Eira HTTP Receiver at Minecraft entrance:
   - Endpoint: `/entrance/motion`

3. NPC configuration:
```json
{
  "name": "Doorkeeper",
  "external_triggers": {
    "redstone_on": {
      "action": "greet",
      "message": "Welcome, traveler! I sensed your approach before you even crossed the threshold.",
      "speak_to_nearest_player": true
    }
  }
}
```

### Example 4: Conversation Milestone Changes Lighting

**Scenario:** As players get closer to the truth, room lighting changes.

**Setup:**

1. Configure webhook in `storyteller-common.toml`:
```toml
[integration.webhooks]
    onSecretRevealed = "http://home-assistant:8123/api/webhook/minecraft_secret"
```

2. Home Assistant automation uses webhook data:
```yaml
automation:
  - trigger:
      platform: webhook
      webhook_id: minecraft_secret
    action:
      - service: light.turn_on
        target:
          entity_id: light.game_room
        data:
          color_name: "{{ trigger.json.data.secret_level | int * 20 + 20 }}"
          brightness_pct: "{{ 50 + trigger.json.data.secret_level | int * 10 }}"
```

3. As secrets are revealed (levels 1-5), lights progressively change

---

## 9. Troubleshooting

### Redstone Not Detected

- Check NPC is within `detectionRadius` of redstone
- Verify `eiraEnabled = true` in config
- Check Eira Receiver is actually emitting signal
- Look for log messages: `Storyteller: Redstone detected near NPC`

### Webhooks Not Firing

- Check webhook URL is correct and reachable
- Verify `webhooks.enabled = true`
- Check server logs for HTTP errors
- Test webhook manually with curl:
  ```bash
  curl -X POST http://localhost:8080/your/endpoint -H "Content-Type: application/json" -d '{}'
  ```

### NPC Not Reacting to Events

- Ensure `external_triggers` is defined in character JSON
- Check trigger type matches (`redstone_on`, `redstone_pulse`, etc.)
- Verify cooldown hasn't been triggered recently
- Check logs: `Storyteller: Processing external event for NPC`

### Eira Relay Not Receiving

- Verify Eira server is running (check `:8080`)
- Check endpoint path matches exactly
- Ensure HTTP method is correct (usually POST)
- Test with: `curl -X POST http://localhost:8080/your/endpoint`

### Performance Issues

- Reduce webhook frequency with cooldowns
- Limit redstone detection radius
- Use async webhooks (default)
- Consider batching events

---

## API Reference

### Storyteller HTTP Endpoints (when Eira present)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/storyteller/npc/{id}/trigger` | POST | Trigger event on NPC |
| `/storyteller/npc/{id}/speak` | POST | Make NPC say something |
| `/storyteller/npc/{id}/mood` | POST | Change NPC mood |
| `/storyteller/status` | GET | Get mod status |

### Webhook Payload Schema

```json
{
  "event": "string",
  "npc": {
    "id": "string",
    "name": "string",
    "character_id": "string"
  },
  "player": {
    "name": "string",
    "uuid": "string"
  },
  "data": {
    "...event-specific data..."
  },
  "world": {
    "dimension": "string",
    "time": "string",
    "weather": "string"
  },
  "timestamp": "ISO-8601 string"
}
```

---

## Resources

- [Eira Relay GitHub](https://github.com/eira/eira-relay)
- [Storyteller NPCs Documentation](./README.md)
- [Home Assistant Webhooks](https://www.home-assistant.io/docs/automation/trigger/#webhook-trigger)
- [Eira Organization](https://eira.org)
