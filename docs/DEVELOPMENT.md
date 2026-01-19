# Development Setup Guide
# Storyteller NPCs Mod

This guide will help you set up your development environment for contributing to or modifying the Storyteller NPCs mod.

---

## Prerequisites

### Required Software

| Software | Version | Download |
|----------|---------|----------|
| JDK | 21+ | [Adoptium](https://adoptium.net/) or [Oracle](https://www.oracle.com/java/technologies/downloads/) |
| Git | Latest | [git-scm.com](https://git-scm.com/) |
| IDE | Any | IntelliJ IDEA (recommended), Eclipse, VS Code |
| Ollama | Latest | [ollama.ai](https://ollama.ai/) |

### Recommended

- IntelliJ IDEA (Community or Ultimate)
- 16+ GB RAM
- SSD for faster builds

---

## 1. Clone the Repository

```bash
git clone https://github.com/yourusername/storyteller.git
cd storyteller
```

---

## 2. IDE Setup

### IntelliJ IDEA (Recommended)

1. **Open Project**
   - File → Open → Select the `storyteller` folder
   - Wait for Gradle sync to complete (may take several minutes first time)

2. **Configure JDK**
   - File → Project Structure → Project
   - Set SDK to Java 21
   - Set Language Level to 21

3. **Generate Run Configurations**
   ```bash
   ./gradlew genIntellijRuns
   ```
   Or on Windows:
   ```cmd
   gradlew.bat genIntellijRuns
   ```

4. **Refresh Gradle**
   - Click the Gradle refresh button in the Gradle tool window

5. **Run Configurations**
   - You should now have "runClient" and "runServer" configurations

### Eclipse

1. **Generate Eclipse Files**
   ```bash
   ./gradlew genEclipseRuns
   ```

2. **Import Project**
   - File → Import → Existing Gradle Project
   - Select the `storyteller` folder

3. **Configure JDK**
   - Window → Preferences → Java → Installed JREs
   - Add Java 21 JDK

### VS Code

1. **Install Extensions**
   - Extension Pack for Java
   - Gradle for Java

2. **Open Folder**
   - File → Open Folder → Select `storyteller`

3. **Build**
   - Terminal → Run Task → gradle: build

---

## 3. Ollama Setup

Ollama is required for testing locally.

### Install Ollama

**Windows:**
```powershell
# Download and run installer from https://ollama.ai/download
```

**macOS:**
```bash
brew install ollama
```

**Linux:**
```bash
curl -fsSL https://ollama.ai/install.sh | sh
```

### Pull Recommended Model

```bash
# Pull the recommended model for development
ollama pull mistral:7b-instruct

# Or a smaller model for faster iteration
ollama pull llama3.2:3b
```

### Start Ollama

```bash
# Start the Ollama service (if not running automatically)
ollama serve
```

Verify it's running:
```bash
curl http://localhost:11434/api/tags
```

---

## 4. Build & Run

### Build the Mod

```bash
# Full build
./gradlew build

# Build without tests
./gradlew build -x test

# Clean build
./gradlew clean build
```

The built JAR will be in `build/libs/`.

### Run Client (Development)

```bash
./gradlew runClient
```

Or use the IDE run configuration "runClient".

### Run Server (Development)

```bash
./gradlew runServer
```

Or use the IDE run configuration "runServer".

### Debug

In IntelliJ:
1. Set breakpoints in your code
2. Click Debug (bug icon) on "runClient" or "runServer"

---

## 5. Project Structure

```
storyteller/
├── build.gradle.kts          # Main build script
├── settings.gradle.kts       # Project settings
├── gradle.properties         # Gradle configuration
│
├── src/
│   └── main/
│       ├── java/             # Java source code
│       │   └── com/storyteller/
│       └── resources/
│           └── META-INF/
│               └── neoforge.mods.toml
│
├── docs/                     # Documentation
│   ├── PRD.md
│   ├── ARCHITECTURE.md
│   └── DEVELOPMENT.md
│
├── examples/                 # Example configurations
│   └── merchant-character.json
│
├── run/                      # Client run directory (gitignored)
└── run-server/               # Server run directory (gitignored)
```

---

## 6. Configuration for Development

### Test Characters

Place test character files in:
```
run/config/storyteller/characters/
```

Example minimal character for testing:
```json
{
  "id": "test-npc",
  "name": "Test NPC",
  "title": "The Tester",
  "personality": {
    "traits": ["helpful", "brief"],
    "backstory": "A test character for development."
  }
}
```

### Test Skins

Place test skins in:
```
run/config/storyteller/skins/
```

### Config Overrides

Edit `run/config/storyteller-common.toml` to change LLM settings during development:

```toml
[llm]
    provider = "OLLAMA"

[llm.ollama]
    endpoint = "http://localhost:11434"
    model = "llama3.2:3b"  # Faster for testing
    timeout = 30
```

---

## 7. Common Development Tasks

### Adding a New LLM Provider

1. Create new class in `com.storyteller.llm.providers`
2. Implement `LLMProvider` interface
3. Add to `LLMManager.initialize()`
4. Add config options to `ModConfig`
5. Add enum value to `ModConfig.LLMProvider`

### Adding a New Packet

1. Create record class in `com.storyteller.network`
2. Define `TYPE` and `STREAM_CODEC`
3. Implement `handle()` method
4. Register in `ModNetwork.registerPayloads()`

### Adding Entity Data

1. Add `EntityDataAccessor` field to `StorytellerNPC`
2. Define in `defineSynchedData()`
3. Add getter/setter methods
4. Save/load in `addAdditionalSaveData()`/`readAdditionalSaveData()`

### Adding a New Command

1. Add to `ModCommands.onRegisterCommands()`
2. Create handler method
3. Define argument types and suggestions

---

## 8. Testing

### Manual Testing Checklist

- [ ] Spawn NPC with `/storyteller spawn`
- [ ] Right-click to open chat
- [ ] Send message and receive response
- [ ] Verify world context in prompt (check logs)
- [ ] Test conversation history persists
- [ ] Test rate limiting
- [ ] Verify skin loading
- [ ] Test with different LLM providers
- [ ] Test in multiplayer

### Log Levels

Set log level in `run/config/log4j2.xml` or via JVM args:
```
-Dlog4j.configurationFile=log4j2-debug.xml
```

---

## 9. Debugging Tips

### View LLM Requests

Add to `OllamaProvider.chat()`:
```java
StorytellerMod.LOGGER.debug("Sending to Ollama: {}", GSON.toJson(requestBody));
```

### Network Packet Debugging

Enable packet logging:
```java
StorytellerMod.LOGGER.debug("Received packet: {}", packet);
```

### Entity Data Debugging

Watch entity data in F3 debug screen or log:
```java
StorytellerMod.LOGGER.debug("NPC state: thinking={}, name={}", 
    isThinking(), getDisplayName());
```

---

## 10. Code Style

### General Guidelines

- Use Java 21 features where appropriate
- Follow NeoForge naming conventions
- Prefer composition over inheritance
- Use `CompletableFuture` for async operations
- Log at appropriate levels (DEBUG for development, INFO for important events)

### Formatting

- 4-space indentation
- 120 character line limit
- Braces on same line
- Use `var` for obvious local types

### Naming

- Classes: `PascalCase`
- Methods/variables: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Packages: `lowercase`

---

## 11. Contributing

### Branch Naming

- Features: `feature/description`
- Bugfixes: `fix/description`
- Documentation: `docs/description`

### Commit Messages

```
type(scope): brief description

Longer explanation if needed.

Fixes #123
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

### Pull Request Process

1. Fork the repository
2. Create feature branch
3. Make changes with tests
4. Update documentation
5. Submit PR with description
6. Address review feedback

---

## 12. Troubleshooting

### "Could not resolve neoforge"

```bash
./gradlew --refresh-dependencies
```

### "Java version mismatch"

Ensure JAVA_HOME points to Java 21:
```bash
echo $JAVA_HOME
java -version
```

### "Ollama connection refused"

```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Start Ollama
ollama serve
```

### "Model not found"

```bash
# Pull the model
ollama pull mistral:7b-instruct
```

### Gradle build fails

```bash
# Clean and rebuild
./gradlew clean build --stacktrace
```

---

## 13. Resources

- [NeoForge Documentation](https://docs.neoforged.net/)
- [NeoForge Discord](https://discord.neoforged.net/)
- [Minecraft Wiki - Modding](https://minecraft.wiki/w/Mods)
- [Ollama Documentation](https://github.com/ollama/ollama/blob/main/docs/api.md)
- [Anthropic Claude API](https://docs.anthropic.com/claude/reference/messages_post)
- [OpenAI API](https://platform.openai.com/docs/api-reference/chat)
