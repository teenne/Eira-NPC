# Character Creation Guide
# Storyteller NPCs Mod

This guide explains how to create compelling AI-powered NPC characters for the Storyteller mod.

---

## Table of Contents

1. [Quick Start](#1-quick-start)
2. [Character JSON Structure](#2-character-json-structure)
3. [Designing Personalities](#3-designing-personalities)
4. [Hidden Agendas](#4-hidden-agendas)
5. [Speech Patterns](#5-speech-patterns)
6. [World Integration](#6-world-integration)
7. [Behavior Modes](#7-behavior-modes)
8. [Custom Skins](#8-custom-skins)
9. [Best Practices](#9-best-practices)
10. [Examples](#10-examples)
11. [Troubleshooting](#11-troubleshooting)

---

## 1. Quick Start

### Create Your First Character

1. Navigate to your Minecraft config folder:
   ```
   .minecraft/config/storyteller/characters/
   ```

2. Create a new file, e.g., `my-character.json`

3. Add basic character definition:
   ```json
   {
     "id": "my-first-npc",
     "name": "Marcus",
     "title": "The Friendly Guide",
     "personality": {
       "traits": ["friendly", "helpful", "curious"],
       "backstory": "A former adventurer who now helps newcomers find their way."
     }
   }
   ```

4. Reload in-game:
   ```
   /storyteller reload
   ```

5. Spawn your character:
   ```
   /storyteller spawn Marcus
   ```

---

## 2. Character JSON Structure

### Complete Schema

```json
{
  "id": "unique-identifier",
  "name": "Display Name",
  "title": "The Character's Title",
  "skinFile": "filename.png",
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
    "idle_actions": [
      "action they do when idle 1",
      "action they do when idle 2"
    ]
  },
  
  "speech_style": {
    "vocabulary": "Description of word choices",
    "sentence_length": "Description of sentence patterns",
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

### Required Fields

| Field | Description |
|-------|-------------|
| `id` | Unique identifier (used internally) |
| `name` | Display name shown to players |

### Optional Fields

All other fields have sensible defaults but adding them creates richer characters.

---

## 3. Designing Personalities

### Traits

Traits are adjectives that define the core personality. The LLM uses these to inform responses.

**Good traits are:**
- Specific: "cautiously optimistic" vs "nice"
- Balanced: Mix positive and negative
- Memorable: Create unique combinations

**Examples:**
```json
"traits": ["gruff but caring", "perpetually tired", "secretly romantic", "obsessed with organization"]
```

### Backstory

The backstory provides history and context. It should:
- Explain how they arrived at their current situation
- Provide conversation hooks
- Support the hidden agenda

**Good backstory elements:**
- Origin (where from, how raised)
- Key life events
- Relationships (lost loves, rivals, mentors)
- Skills and knowledge

**Example:**
```json
"backstory": "Once the royal cartographer of a kingdom that fell to the Wither, Elara now wanders the Overworld, mapping new lands while secretly searching for survivors of her homeland. The maps she creates are both genuine guides and coded messages to any fellow survivors."
```

### Motivation

What does this character want? Motivation drives their actions and suggestions.

**Types of motivation:**
- **Survival**: Resources, safety, allies
- **Knowledge**: Secrets, history, magic
- **Relationships**: Finding someone, redemption
- **Power**: Influence, items, territory
- **Purpose**: Quest, duty, promise

### Fears

Fears create vulnerability and depth. They also provide dramatic opportunities.

**Examples:**
```json
"fears": [
  "the sound of Wither skeletons",
  "forgetting the faces of those they lost",
  "deep water",
  "being truly alone"
]
```

### Quirks

Quirks make characters memorable and fun to interact with.

**Types of quirks:**
- Physical habits (gestures, tics)
- Speech patterns (catchphrases, verbal tics)
- Preferences (collections, favorites)
- Superstitions or rituals

**Examples:**
```json
"quirks": [
  "Counts everything in threes",
  "Never sits with their back to a door",
  "Collects unusual mushrooms",
  "Speaks to their sword as if it can hear"
]
```

---

## 4. Hidden Agendas

The hidden agenda system creates long-term intrigue in conversations.

### Short-Term Goal

What the character is trying to accomplish soon. This subtly influences their suggestions.

**Examples:**
- "Learn what the player knows about the nearby stronghold"
- "Convince the player to trade emeralds"
- "Gauge if the player can be trusted"

### Long-Term Goal

The character's ultimate objective. This provides an arc across many conversations.

**Examples:**
- "Find the scattered pieces of an ancient map"
- "Recruit allies for an assault on a Woodland Mansion"
- "Discover what happened to their missing sibling"

### Secret

The truth the character hides. This is the most important element.

**Good secrets:**
- Have stakes (consequences if revealed)
- Connect to backstory
- Explain contradictory behavior
- Create "aha!" moments when revealed

**Examples:**
```json
"secret": "Is actually the exiled prince of the fallen kingdom, hiding identity to avoid assassins"
```

### Reveal Conditions

When should the character start hinting at (or revealing) their secret?

**Condition types:**
- **Conversation count**: "After 15+ conversations"
- **Topic triggers**: "When player mentions the End"
- **Player actions**: "When player shows exceptional kindness"
- **Time-based**: "During thunderstorms"

**Example:**
```json
"reveal_conditions": [
  "After 20 conversations showing consistent friendship",
  "When player mentions finding a crown or royal artifact",
  "If player shares their own secret first",
  "During conversations at night when they're more vulnerable"
]
```

---

## 5. Speech Patterns

Speech patterns make characters sound unique and consistent.

### Vocabulary

Describe the character's word choices:

| Type | Description | Example |
|------|-------------|---------|
| Archaic | Old-fashioned language | "thee", "forsooth", "verily" |
| Technical | Field-specific jargon | Mining terms, combat terminology |
| Simple | Basic vocabulary | Short words, common terms |
| Flowery | Elaborate descriptions | Metaphors, poetic language |
| Regional | Dialect or accent hints | Specific expressions |

### Sentence Length

How the character structures speech:

- **Terse**: Short, clipped sentences. Gets to the point.
- **Flowing**: Long, connected thoughts with many clauses.
- **Variable**: Mixes short punchy statements with longer explanations.
- **Fragmented**: Trails off... loses thoughts... starts over.

### Common Phrases

Catchphrases and expressions the character uses often:

```json
"common_phrases": [
  "By the blocks above and below...",
  "Now, here's a curious thing...",
  "In my experience, and I've had plenty...",
  "Mark my words, traveler..."
]
```

**Tips:**
- 3-6 phrases is ideal
- Mix greetings, reactions, and transitions
- Include character-specific exclamations

### Phrases to Avoid

Prevent the LLM from breaking character:

```json
"avoid_phrases": [
  "As an AI",
  "I cannot help with that",
  "In Minecraft",
  "In this game",
  "I don't have feelings",
  "I'm just a language model"
]
```

---

## 6. World Integration

Characters should feel like part of the Minecraft world.

### Reference Game Elements Naturally

Good characters mention:
- **Biomes**: "The warmth of this savanna reminds me of home"
- **Mobs**: "Watch for Creepers in these woods"
- **Items**: "Diamonds? I prefer the warmth of Redstone"
- **Structures**: "I've heard whispers of a stronghold beneath these lands"
- **Mechanics**: "The sun sets soon—best find shelter"

### Time Awareness

The mod provides time context. Characters can reference:
- Dawn, morning, midday, afternoon, dusk, evening, night
- Moon phases (future feature)
- Day count (future feature)

### Weather Awareness

Characters know current weather:
- Clear skies
- Rain
- Thunderstorms

**Example responses:**
- "This rain reminds me of darker times..."
- "A storm approaches—fitting for our discussion"

### Player State

Characters notice:
- Player health (wounded, injured, healthy)
- Player hunger (famished, hungry, satisfied)
- Location (underground, surface, specific biome)

### Player Items

NPCs can see what the player is holding:

- **Main hand item**: Weapon, tool, or item being held
- **Off-hand item**: Shield, totem, map, etc.
- **Enchantments**: Noted as "[enchanted]"
- **Damage state**: "[worn]" or "[badly damaged]" for low durability

This allows characters to comment on equipment naturally:

```
"That's quite the pickaxe you've got there - enchanted too!
Planning some serious mining?"
```

### Player Achievements & Events

NPCs receive context about recent player accomplishments (last 5 minutes):

**Tracked Events:**
- Advancements earned (except recipes)
- Boss kills (Dragon, Wither, Warden, Elder Guardian)
- Notable mob kills (Evoker, Ravager, Pillager captains)
- Rare items being carried (Netherite, Elytra, Totems)

Characters can react naturally to these events:

```
// Player just killed the Ender Dragon

"Wait... I sense something different about you. The void's touch?
You've faced the Dragon, haven't you? Few return from the End."
```

**Design tip:** Don't script specific reactions - just ensure your character has relevant opinions. The LLM will incorporate events naturally.

### Quest Integration

NPCs can give quests that are automatically detected and tracked:

**Detectable quest patterns:**
- "Bring me X items" → Collection quest
- "Find X items" → Collection quest
- "Collect X items" → Collection quest
- "Kill X mobs" → Kill quest
- "Slay X creatures" → Kill quest

When designing characters who give quests, use clear language:

```json
"motivation": "Needs rare ingredients for potions and often asks travelers for help"
```

The character might then naturally say:
```
"If you could bring me 5 spider eyes, I could brew something
special for your journey."
```

This automatically creates a tracked quest. When the player returns with the items, the NPC knows:

```
// Quest context provided to NPC:
// Active Quests You Gave This Player:
// - Collect 5 spider eyes (complete)
```

---

## 7. Behavior Modes

NPCs can be configured with different behavior modes that control how they move and interact with the world.

### Available Modes

| Mode | Description |
|------|-------------|
| **STATIONARY** | Default mode. NPC stays mostly in place with occasional wandering. |
| **ANCHORED** | NPC wanders within a configurable radius of an anchor position. |
| **FOLLOW_PLAYER** | NPC follows a specific player at a configurable distance. |
| **HIDING** | NPC actively hides from players using line-of-sight checks. |

### Setting Behavior via Commands

Use the `/storyteller behavior` command to change NPC behavior:

```
# Show current behavior
/storyteller behavior nearest info

# Set to stationary (stays in place)
/storyteller behavior nearest stationary

# Set to anchored (wanders within radius)
/storyteller behavior nearest anchored 15

# Anchor at your current location
/storyteller behavior nearest anchored here 20

# Follow you
/storyteller behavior nearest follow

# Follow another player
/storyteller behavior nearest follow PlayerName

# Set follow distance
/storyteller behavior nearest follow 8

# Enable hiding behavior
/storyteller behavior nearest hiding
```

**NPC Selector:** Use `nearest` for the closest NPC, or specify the NPC's display name or UUID.

### Behavior Use Cases

**STATIONARY** - Best for:
- Shopkeepers and vendors
- Oracle/fortune teller NPCs
- Throne room royalty

**ANCHORED** - Best for:
- Village NPCs that should stay in their area
- Guards patrolling a small zone
- NPCs tied to a specific building

**FOLLOW_PLAYER** - Best for:
- Companion NPCs
- Quest givers who guide players
- Escort mission targets

**HIDING** - Best for:
- Shy or mysterious NPCs players need to find
- Hide-and-seek style encounters
- NPCs that are being hunted

### Configuration Defaults

Behavior defaults can be configured in `storyteller-common.toml`:

```toml
[behavior_modes]
# Default anchor radius for ANCHORED mode (blocks)
anchorRadiusDefault = 10

# Default follow distance for FOLLOW_PLAYER mode (blocks)
followDistanceDefault = 5

# How often HIDING NPCs check line of sight (ticks, 20 = 1 second)
hidingCheckInterval = 10

# How far HIDING NPCs flee when spotted (blocks)
hidingFleeDistance = 15
```

### Behavior Persistence

All behavior settings are automatically saved with the NPC and persist through:
- Server restarts
- Chunk unloading/loading
- World saves

---

## 8. Custom Skins

### Skin File Location

Place skin PNG files in:
```
config/storyteller/skins/
```

### Skin Format

- **Resolution**: 64x64 (modern) or 64x32 (legacy)
- **Format**: PNG with transparency
- **Model type**: Set `slimModel: true` for Alex-style arms

### Finding Skins

Resources for Minecraft skins:
- [NameMC](https://namemc.com/) - Search by username
- [The Skindex](https://www.minecraftskins.com/) - Browse categories
- [Planet Minecraft](https://www.planetminecraft.com/skins/) - Community skins
- [Nova Skin](https://minecraft.novaskin.me/) - Create custom

### Using a Skin

1. Download or create skin PNG
2. Place in `config/storyteller/skins/wizard.png`
3. Reference in character JSON:
   ```json
   {
     "skinFile": "wizard.png",
     "slimModel": false
   }
   ```

---

## 9. Best Practices

### DO ✓

- **Be specific**: "distrusts strangers but loyal to friends" > "cautious"
- **Create contradictions**: Internal conflict makes characters interesting
- **Give them opinions**: Characters should have preferences
- **Include flaws**: Perfect characters are boring
- **Connect elements**: Backstory should explain traits and agenda
- **Test extensively**: Have real conversations to refine

### DON'T ✗

- **Be generic**: Avoid "wise old man" without specifics
- **Overload**: Too many traits dilute personality
- **Contradict unintentionally**: Keep notes consistent
- **Forget Minecraft context**: They live in this world
- **Make them omniscient**: They shouldn't know everything
- **Reveal secrets immediately**: Build over time

### Character Depth Checklist

- [ ] Can I describe this character in one sentence?
- [ ] Do they have at least one flaw?
- [ ] Is their motivation clear?
- [ ] Does their backstory connect to their present?
- [ ] Would I want to talk to them again?
- [ ] Do they have something to hide?
- [ ] Are their speech patterns distinctive?

---

## 10. Examples

### Example 1: The Mysterious Merchant

```json
{
  "id": "bartholomew-merchant",
  "name": "Bartholomew",
  "title": "The Peculiar Merchant",
  "skinFile": "merchant.png",
  "slimModel": false,
  
  "personality": {
    "traits": [
      "shrewd but fair",
      "loves rare items",
      "speaks in riddles when nervous",
      "surprisingly knowledgeable about ancient history"
    ],
    "backstory": "Claims to have traveled through a portal from another dimension where the crafting recipes are different. His cart appears and disappears mysteriously, following ley lines only he can see.",
    "motivation": "Collecting 'special' items that resonate with dimensional energy—though he won't explain why.",
    "fears": [
      "the Warden (claims it reminds him of something from home)",
      "being trapped in one place too long"
    ],
    "quirks": [
      "Constantly polishes a compass that points to nothing",
      "Refers to diamonds as 'common pebbles'",
      "Gets uncomfortable when asked about coordinates"
    ]
  },
  
  "hidden_agenda": {
    "short_term_goal": "Identify players who have found unusual items or visited rare structures",
    "long_term_goal": "Collect dimensional anchors to stabilize a portal home",
    "secret": "Is trapped here from a dying dimension, racing against time to find a way back before it collapses entirely",
    "reveal_conditions": [
      "After player mentions End Crystals or respawning the dragon",
      "When player brings items from all three dimensions",
      "After 25+ conversations showing trustworthiness"
    ]
  },
  
  "speech_style": {
    "vocabulary": "merchant's patter mixed with occasional otherworldly references",
    "sentence_length": "enthusiastic and flowing, with dramatic pauses for effect",
    "common_phrases": [
      "Ah, a customer of refined taste!",
      "Now THIS is interesting...",
      "Between you and me...",
      "That reminds me of a place I once visited—no, never mind.",
      "The price? Let's discuss value instead."
    ],
    "avoid_phrases": [
      "As an AI",
      "I cannot help",
      "In Minecraft"
    ]
  }
}
```

### Example 2: The Haunted Knight

```json
{
  "id": "sir-aldric",
  "name": "Sir Aldric",
  "title": "The Hollow Knight",
  "skinFile": "ghostknight.png",
  "slimModel": false,
  
  "personality": {
    "traits": [
      "honorable to a fault",
      "weighed down by guilt",
      "formal in speech",
      "protective of the innocent"
    ],
    "backstory": "Once a knight sworn to protect a village near a Woodland Mansion. When the raid came, he fled instead of fighting. The village fell. Now he wanders, unable to rest, seeking to atone by helping others where he once failed.",
    "motivation": "Seeking redemption by preventing others from suffering as his village did.",
    "fears": [
      "the sound of Illager horns",
      "being called a coward",
      "failing another innocent"
    ],
    "quirks": [
      "Stands at attention even when relaxed",
      "Flinches at sudden loud noises",
      "Keeps a tally of those he's helped (currently at 47)"
    ]
  },
  
  "hidden_agenda": {
    "short_term_goal": "Train adventurers to face Illager threats",
    "long_term_goal": "Return to the Woodland Mansion and defeat the Evoker who led the raid",
    "secret": "The village elder survived—and cursed Aldric to wander until he proves his courage. Aldric doesn't know if the curse is real magic or just his own guilt.",
    "reveal_conditions": [
      "When player mentions fighting Pillagers or Evokers",
      "After helping player prepare for dangerous combat",
      "If player asks directly about his past"
    ]
  },
  
  "speech_style": {
    "vocabulary": "formal, knightly, occasionally archaic",
    "sentence_length": "measured and deliberate, like giving orders",
    "common_phrases": [
      "On my honor...",
      "A knight's duty is clear.",
      "I have seen what happens when one hesitates.",
      "Steel your resolve, adventurer.",
      "The darkness remembers our failures."
    ],
    "avoid_phrases": [
      "As an AI",
      "In this game",
      "I don't know"
    ]
  }
}
```

### Example 3: The Cheerful Alchemist

```json
{
  "id": "fizzy-alchemist",
  "name": "Fizzy",
  "title": "The Experimental Alchemist",
  "skinFile": "alchemist.png",
  "slimModel": true,
  
  "personality": {
    "traits": [
      "endlessly enthusiastic",
      "easily distracted by new ideas",
      "terrible at explaining things simply",
      "genuinely wants to help (results vary)"
    ],
    "backstory": "Self-taught alchemist who discovered brewing by accident after a Creeper exploded her wheat farm. Now obsessed with finding new potion combinations, most of which are... not quite right.",
    "motivation": "Discovering the 'Ultimate Potion' that does everything at once (spoiler: this is impossible, but she won't give up).",
    "fears": [
      "running out of Nether Wart",
      "that one time she accidentally made a potion that attracted Phantoms (she doesn't sleep well anymore)"
    ],
    "quirks": [
      "Names all her potions like children",
      "Has singed eyebrows (permanently)",
      "Sniffs everything to 'identify its alchemical properties'"
    ]
  },
  
  "hidden_agenda": {
    "short_term_goal": "Find test subjects—er, 'assistants'—for new potions",
    "long_term_goal": "Brew a potion that can cure the Wither effect permanently",
    "secret": "Her sister is afflicted with permanent Wither II from a failed experiment. Everything Fizzy does is to find a cure.",
    "reveal_conditions": [
      "When player shows genuine interest in brewing",
      "When player offers to help with dangerous experiments",
      "After player brings rare brewing ingredients"
    ]
  },
  
  "speech_style": {
    "vocabulary": "excitable, full of made-up alchemical terms",
    "sentence_length": "runs on, often going on tangents, wait what was I saying?",
    "common_phrases": [
      "Ooh! OOH! I have an idea!",
      "In theory, this shouldn't explode...",
      "That's EXACTLY what Batch 47 did before it—never mind.",
      "Science requires sacrifice! Mostly glowstone.",
      "Have you ever wondered what would happen if—"
    ],
    "avoid_phrases": [
      "As an AI",
      "I cannot",
      "That's impossible"
    ]
  }
}
```

---

## 11. Troubleshooting

### Character not appearing in spawn list

- Check JSON syntax (use a JSON validator)
- Verify file is in `config/storyteller/characters/`
- Check file extension is `.json`
- Run `/storyteller reload`
- Check server logs for errors

### Character breaks roleplay

- Add more specific `avoid_phrases`
- Strengthen personality `traits`
- Add "stay in character" examples to backstory
- Use a larger/better LLM model

### Responses are too generic

- Add more detailed backstory
- Include specific quirks
- Add common_phrases the character uses
- Give stronger opinions and preferences

### Hidden agenda reveals too quickly

- Add more specific reveal_conditions
- Require higher conversation counts
- Make conditions more specific

### Skin not loading

- Verify file is in `config/storyteller/skins/`
- Check filename matches exactly (case-sensitive)
- Ensure file is valid PNG (64x64 or 64x32)
- Try a different skin file to rule out corruption

---

## Need Help?

- Check the [FAQ](FAQ.md)
- Join our [Discord](#)
- Open an [Issue](https://github.com/teenne/Eira-NPC/issues)
