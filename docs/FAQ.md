# Frequently Asked Questions (FAQ)
# Storyteller NPCs Mod

---

## General Questions

### What is Storyteller NPCs?

Storyteller NPCs is a Minecraft mod that adds AI-powered NPCs to your world. These NPCs can have natural conversations with players, tell stories, give hints for adventures, and have their own personalities and hidden agendas.

### How does it work?

The mod connects to a Large Language Model (LLM) - either locally via Ollama or through cloud APIs (Claude, OpenAI). When you chat with an NPC, your message and the character's personality are sent to the LLM, which generates a contextual response.

### Is it free?

The mod itself is free and open source. 
- **Ollama (local)**: Completely free, runs on your computer
- **Claude/OpenAI**: Require API keys with usage-based pricing

### Does it work offline?

Yes! When using Ollama, everything runs locally on your machine. No internet required once the model is downloaded.

### What Minecraft versions are supported?

Currently: **Minecraft 1.21.4** with **NeoForge**.

Fabric and Forge support are planned for future versions.

---

## Installation & Setup

### How do I install the mod?

1. Install NeoForge for Minecraft 1.21.4
2. Download the mod JAR file
3. Place it in your `mods` folder
4. Install Ollama (recommended) from ollama.ai
5. Pull a model: `ollama pull mistral:7b-instruct`
6. Launch Minecraft!

### Why isn't Ollama connecting?

Common issues:
- **Ollama not running**: Run `ollama serve` in terminal
- **Wrong endpoint**: Check config for `http://localhost:11434`
- **Model not pulled**: Run `ollama pull mistral:7b-instruct`
- **Port blocked**: Check firewall settings

### Can I use it on a server?

Yes! The mod works on dedicated servers. Options:
- Run Ollama on the server machine
- Use Claude/OpenAI APIs (requires API key)
- Point to an Ollama instance on another machine

### How much RAM do I need?

- **Minimum (3B model)**: 8 GB RAM
- **Recommended (7B model)**: 16 GB RAM
- **Optimal (13B+ model)**: 32 GB RAM

For GPU acceleration, you also need VRAM.

---

## Usage Questions

### How do I spawn an NPC?

Use the command: `/storyteller spawn`

Or spawn a specific character: `/storyteller spawn Eldric`

### How do I talk to an NPC?

Right-click the NPC to open the chat screen. Type your message and press Enter.

### Can I create my own characters?

Absolutely! Create a JSON file in `config/storyteller/characters/`. See the [Character Creation Guide](CHARACTER_GUIDE.md) for details.

### How do I use custom skins?

1. Place a PNG skin file (64x64) in `config/storyteller/skins/`
2. Reference it in your character JSON: `"skinFile": "myskin.png"`
3. Reload with `/storyteller reload`

### Why does the NPC take so long to respond?

Response time depends on:
- **Model size**: Larger models are slower but smarter
- **Hardware**: More RAM/VRAM = faster
- **Cloud APIs**: Network latency applies

Try a smaller model like `llama3.2:3b` for faster responses.

### Can players grief/abuse NPCs?

NPCs are invulnerable by default. Rate limiting prevents spam. Server operators can configure restrictions.

---

## Character & Story Questions

### How do hidden agendas work?

NPCs have secrets defined in their character JSON. The LLM is instructed to:
1. Know the secret but not reveal it directly
2. Drop hints based on specified conditions
3. Gradually reveal more over many conversations

This creates emergent storytelling as players uncover the truth.

### Do NPCs remember past conversations?

Yes, within a session. Each player has separate conversation history with each NPC. History is limited to the last 20 messages (configurable).

Currently, history doesn't persist across server restarts (planned feature).

### Can NPCs give quests?

Not yet, but it's planned! Currently, NPCs can verbally suggest objectives and guide players, but there's no formal quest tracking.

### Why does my NPC break character?

The LLM might break character if:
- The system prompt is too weak
- The model is too small
- Conflicting instructions exist

Solutions:
- Add more detail to personality/backstory
- Use `avoid_phrases` to block problematic responses
- Try a larger/better model
- Strengthen the "stay in character" rules

---

## LLM & AI Questions

### Which LLM should I use?

**For local (Ollama)**:
- `mistral:7b-instruct` - Best balance (recommended)
- `llama3.2:3b` - Faster, good for weaker hardware
- `nous-hermes2:10.7b` - Best roleplay quality

**For cloud**:
- Claude Sonnet - Best quality/cost balance
- GPT-4o - Excellent quality
- Cheaper options for budget

### Is my data private?

**With Ollama**: Yes, everything stays on your machine.

**With Claude/OpenAI**: Your conversations are sent to their servers. Both have privacy policies, but be aware data leaves your machine.

### Can I use other LLM providers?

The mod supports custom providers. See [LLM API Documentation](LLM_API.md) for how to implement new providers.

### How much does Claude/OpenAI cost?

Rough estimates per 1000 conversations:
- **Claude Haiku**: ~$0.50
- **Claude Sonnet**: ~$3.00
- **GPT-4o-mini**: ~$0.30
- **GPT-4o**: ~$5.00

Actual costs depend on conversation length.

---

## Technical Questions

### Does it affect performance?

Minimal impact on Minecraft itself:
- NPC entity is lightweight
- LLM requests are async (non-blocking)
- Network packets are small

The LLM itself uses system resources (separate from Minecraft).

### Can I modify the source code?

Yes! The mod is open source under MIT license. Fork it, modify it, contribute back!

### Does it work with other mods?

Generally yes. It uses standard NeoForge APIs. Report conflicts as issues.

### How do I report bugs?

1. Check existing issues on GitHub
2. Gather: MC version, mod version, logs, steps to reproduce
3. Open a new issue with details

---

## Future Plans

### Will there be Fabric/Forge versions?

Yes, multi-loader support is planned after the NeoForge version stabilizes.

### Will NPCs be able to fight?

Possibly in the future. Combat AI adds complexity and balance considerations.

### Will there be voice acting?

Text-to-speech integration is planned. You'll be able to have NPCs speak aloud using local TTS.

### Can NPCs learn and remember permanently?

Long-term memory is planned. NPCs would remember important details across sessions and build relationships over time.

---

## Troubleshooting Quick Reference

| Problem | Solution |
|---------|----------|
| NPC doesn't respond | Check LLM connection (`/storyteller status`) |
| Slow responses | Use smaller model or check hardware |
| Character not found | Run `/storyteller reload` |
| Skin not loading | Check file path and PNG format |
| Rate limit error | Wait between messages |
| Crash on spawn | Check logs, report bug |

---

## Still Need Help?

- 📖 Read the [full documentation](docs/)
- 💬 Join our [Discord](#)
- 🐛 Open a [GitHub Issue](https://github.com/yourusername/storyteller/issues)
