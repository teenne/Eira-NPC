# LLM Integration API Documentation
# Storyteller NPCs Mod

This document details the LLM (Large Language Model) integration architecture, API specifications, and implementation guidelines.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Provider Interface](#2-provider-interface)
3. [Ollama Integration](#3-ollama-integration)
4. [Claude Integration](#4-claude-integration)
5. [OpenAI Integration](#5-openai-integration)
6. [Prompt Engineering](#6-prompt-engineering)
7. [Adding Custom Providers](#7-adding-custom-providers)
8. [Performance Considerations](#8-performance-considerations)
9. [Error Handling](#9-error-handling)

---

## 1. Overview

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      LLMManager                              │
│  - Provider selection                                        │
│  - Failover handling                                         │
│  - Request routing                                           │
└────────────────────────┬────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│   Ollama    │  │   Claude    │  │   OpenAI    │
│  Provider   │  │  Provider   │  │  Provider   │
└──────┬──────┘  └──────┬──────┘  └──────┬──────┘
       │                │                │
       ▼                ▼                ▼
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│  localhost  │  │  Anthropic  │  │   OpenAI    │
│   :11434    │  │     API     │  │     API     │
└─────────────┘  └─────────────┘  └─────────────┘
```

### Design Principles

1. **Local-First**: Ollama is the default, enabling offline play
2. **Async**: All LLM calls are non-blocking
3. **Fallback**: Automatic failover when primary fails
4. **Abstraction**: Providers implement a common interface
5. **Configurability**: All settings exposed via config

---

## 2. Provider Interface

### LLMProvider Interface

```java
package com.storyteller.llm;

public interface LLMProvider {
    
    /**
     * Initialize the provider (test connection, validate credentials)
     * @return CompletableFuture resolving to true if successful
     */
    CompletableFuture<Boolean> initialize();
    
    /**
     * Send a chat completion request
     * @param systemPrompt The system prompt defining character and context
     * @param messages Conversation history (user/assistant pairs)
     * @return CompletableFuture resolving to the assistant's response
     */
    CompletableFuture<String> chat(String systemPrompt, List<ChatMessage> messages);
    
    /**
     * Check if the provider is ready to accept requests
     */
    boolean isAvailable();
    
    /**
     * Get human-readable provider name for logging/display
     */
    String getName();
    
    /**
     * Clean up resources on shutdown
     */
    void shutdown();
    
    /**
     * Chat message record
     */
    record ChatMessage(Role role, String content) {
        public enum Role {
            USER,       // Player message
            ASSISTANT,  // NPC response
            SYSTEM      // System prompt (handled separately)
        }
    }
}
```

### LLMManager

```java
package com.storyteller.llm;

public class LLMManager {
    
    private final Map<LLMProvider, LLMProvider> providers;
    private LLMProvider activeProvider;
    
    /**
     * Initialize all providers, activate configured default
     */
    public void initialize();
    
    /**
     * Route chat request to active provider
     */
    public CompletableFuture<String> chat(String systemPrompt, List<ChatMessage> messages);
    
    /**
     * Check if any provider is available
     */
    public boolean isAvailable();
    
    /**
     * Get active provider name
     */
    public String getActiveProviderName();
    
    /**
     * Switch to different provider at runtime
     */
    public CompletableFuture<Boolean> switchProvider(LLMProviderType type);
    
    /**
     * Shutdown all providers
     */
    public void shutdown();
}
```

---

## 3. Ollama Integration

### Configuration

```toml
[llm.ollama]
    endpoint = "http://localhost:11434"
    model = "mistral:7b-instruct"
    timeout = 60
```

### API Endpoint

**POST** `/api/chat`

### Request Format

```json
{
  "model": "mistral:7b-instruct",
  "stream": false,
  "messages": [
    {
      "role": "system",
      "content": "You are Eldric, The Wandering Sage..."
    },
    {
      "role": "user",
      "content": "Hello, traveler!"
    },
    {
      "role": "assistant",
      "content": "Ah, greetings, young adventurer..."
    },
    {
      "role": "user",
      "content": "What brings you to these lands?"
    }
  ],
  "options": {
    "temperature": 0.8,
    "top_p": 0.9,
    "repeat_penalty": 1.1
  }
}
```

### Response Format

```json
{
  "model": "mistral:7b-instruct",
  "created_at": "2025-01-18T10:30:00.000Z",
  "message": {
    "role": "assistant",
    "content": "I wander seeking ancient knowledge..."
  },
  "done": true,
  "total_duration": 2500000000,
  "load_duration": 100000000,
  "prompt_eval_count": 150,
  "eval_count": 50
}
```

### Recommended Models

| Model | Size | Use Case | Speed |
|-------|------|----------|-------|
| `mistral:7b-instruct` | 4.1GB | **Recommended** - Best balance | Fast |
| `llama3.1:8b` | 4.7GB | Good general purpose | Fast |
| `nous-hermes2:10.7b` | 6.4GB | Best roleplay quality | Medium |
| `llama3.2:3b` | 2.0GB | Low-spec machines | Very Fast |
| `mixtral:8x7b` | 26GB | Highest quality | Slow |

### Generation Options

```json
{
  "options": {
    "temperature": 0.8,      // Creativity (0.0-2.0)
    "top_p": 0.9,            // Nucleus sampling
    "top_k": 40,             // Vocabulary restriction
    "repeat_penalty": 1.1,   // Reduce repetition
    "num_ctx": 4096          // Context window
  }
}
```

### Implementation

```java
public class OllamaProvider implements LLMProvider {
    
    private static final MediaType JSON = MediaType.get("application/json");
    private OkHttpClient client;
    private String endpoint;
    private String model;
    
    @Override
    public CompletableFuture<String> chat(String systemPrompt, List<ChatMessage> messages) {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject request = new JsonObject();
            request.addProperty("model", model);
            request.addProperty("stream", false);
            
            JsonArray msgs = new JsonArray();
            
            // Add system prompt
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", systemPrompt);
            msgs.add(systemMsg);
            
            // Add conversation history
            for (ChatMessage msg : messages) {
                JsonObject m = new JsonObject();
                m.addProperty("role", msg.role().name().toLowerCase());
                m.addProperty("content", msg.content());
                msgs.add(m);
            }
            
            request.add("messages", msgs);
            
            // Add generation options
            JsonObject options = new JsonObject();
            options.addProperty("temperature", 0.8);
            options.addProperty("top_p", 0.9);
            options.addProperty("repeat_penalty", 1.1);
            request.add("options", options);
            
            // Execute request
            Request httpRequest = new Request.Builder()
                .url(endpoint + "/api/chat")
                .post(RequestBody.create(request.toString(), JSON))
                .build();
            
            try (Response response = client.newCall(httpRequest).execute()) {
                JsonObject json = parseResponse(response);
                return json.getAsJsonObject("message").get("content").getAsString();
            }
        });
    }
}
```

---

## 4. Claude Integration

### Configuration

```toml
[llm.claude]
    apiKey = "sk-ant-..."
    model = "claude-sonnet-4-20250514"
```

### API Endpoint

**POST** `https://api.anthropic.com/v1/messages`

### Request Format

```json
{
  "model": "claude-sonnet-4-20250514",
  "max_tokens": 1024,
  "system": "You are Eldric, The Wandering Sage...",
  "messages": [
    {
      "role": "user",
      "content": "Hello, traveler!"
    },
    {
      "role": "assistant",
      "content": "Ah, greetings, young adventurer..."
    },
    {
      "role": "user",
      "content": "What brings you to these lands?"
    }
  ]
}
```

### Headers

```
x-api-key: sk-ant-...
anthropic-version: 2023-06-01
content-type: application/json
```

### Response Format

```json
{
  "id": "msg_...",
  "type": "message",
  "role": "assistant",
  "content": [
    {
      "type": "text",
      "text": "I wander seeking ancient knowledge..."
    }
  ],
  "model": "claude-sonnet-4-20250514",
  "stop_reason": "end_turn",
  "usage": {
    "input_tokens": 150,
    "output_tokens": 50
  }
}
```

### Available Models

| Model | Speed | Quality | Cost |
|-------|-------|---------|------|
| `claude-3-haiku-20240307` | Fastest | Good | Lowest |
| `claude-sonnet-4-20250514` | Fast | Better | Medium |
| `claude-opus-4-20250514` | Slowest | Best | Highest |

### Implementation Notes

- System prompt is a separate field, not a message
- Only `user` and `assistant` roles in messages array
- Response content is an array (typically one text block)
- Handle rate limits (429 errors) with exponential backoff

---

## 5. OpenAI Integration

### Configuration

```toml
[llm.openai]
    apiKey = "sk-..."
    model = "gpt-4o"
```

### API Endpoint

**POST** `https://api.openai.com/v1/chat/completions`

### Request Format

```json
{
  "model": "gpt-4o",
  "max_tokens": 1024,
  "temperature": 0.8,
  "messages": [
    {
      "role": "system",
      "content": "You are Eldric, The Wandering Sage..."
    },
    {
      "role": "user",
      "content": "Hello, traveler!"
    },
    {
      "role": "assistant",
      "content": "Ah, greetings, young adventurer..."
    },
    {
      "role": "user",
      "content": "What brings you to these lands?"
    }
  ]
}
```

### Headers

```
Authorization: Bearer sk-...
Content-Type: application/json
```

### Response Format

```json
{
  "id": "chatcmpl-...",
  "object": "chat.completion",
  "created": 1705580000,
  "model": "gpt-4o",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "I wander seeking ancient knowledge..."
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 150,
    "completion_tokens": 50,
    "total_tokens": 200
  }
}
```

### Available Models

| Model | Speed | Quality | Cost |
|-------|-------|---------|------|
| `gpt-4o-mini` | Fastest | Good | Lowest |
| `gpt-4o` | Fast | Best | Medium |
| `gpt-4-turbo` | Medium | Best | Higher |

---

## 6. Prompt Engineering

### System Prompt Template

```
You are {name}, known as "{title}".

## Character
**Backstory:** {backstory}
**Motivation:** {motivation}
**Personality traits:** {traits}
**Fears:** {fears}
**Quirks:** {quirks}

## How You Speak
**Vocabulary:** {vocabulary}
**Style:** {sentence_length}
**Phrases you might use:** {common_phrases}
**Never say:** {avoid_phrases}

## Your Secret Agenda (do not reveal directly)
**Short-term goal:** {short_term_goal}
**Long-term goal:** {long_term_goal}
**Your secret:** {secret}
**You may hint at your secret when:** {reveal_conditions}

## Current World State
{world_context}

## Conversation Context
{conversation_summary}

## Rules
- Stay in character at all times
- Never break the fourth wall or mention being an AI
- Keep responses concise (1-3 paragraphs usually)
- You exist in the Minecraft world - reference blocks, mobs, biomes naturally
- Give hints for adventures but don't solve everything
- Remember details shared and reference them later
- Your hidden agenda should subtly influence suggestions
```

### World Context Generation

```java
public String toPromptString() {
    StringBuilder sb = new StringBuilder();
    sb.append("- Location: ").append(biome).append(" in ").append(dimension);
    sb.append("- Time: ").append(timeOfDay);
    sb.append("- Weather: ").append(weather);
    
    if (playerHealth <= 6) {
        sb.append("- The player looks badly wounded");
    }
    if (playerHunger <= 6) {
        sb.append("- The player looks famished");
    }
    if (playerIsUnderground) {
        sb.append("- You are deep underground");
    }
    
    return sb.toString();
}
```

### Conversation Context

```java
public static String buildConversationSummary(UUID npcId, UUID playerId) {
    List<ChatMessage> history = getHistory(npcId, playerId);
    int count = getConversationCount(npcId, playerId);
    
    StringBuilder summary = new StringBuilder();
    summary.append("You have had ").append(count).append(" conversation(s).\n");
    
    // Include last 6 messages for context
    int start = Math.max(0, history.size() - 6);
    for (int i = start; i < history.size(); i++) {
        ChatMessage msg = history.get(i);
        String role = msg.role() == Role.USER ? "Player" : "You";
        String content = truncate(msg.content(), 100);
        summary.append("- ").append(role).append(": ").append(content).append("\n");
    }
    
    return summary.toString();
}
```

---

## 7. Adding Custom Providers

### Step 1: Implement Interface

```java
package com.storyteller.llm.providers;

public class CustomProvider implements LLMProvider {
    
    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            // Test connection
            // Return true if successful
            return true;
        });
    }
    
    @Override
    public CompletableFuture<String> chat(String systemPrompt, List<ChatMessage> messages) {
        return CompletableFuture.supplyAsync(() -> {
            // Build request for your API
            // Send request
            // Parse response
            // Return assistant content
            return "Response from custom provider";
        });
    }
    
    @Override
    public boolean isAvailable() {
        return /* check if ready */;
    }
    
    @Override
    public String getName() {
        return "Custom Provider";
    }
    
    @Override
    public void shutdown() {
        // Cleanup resources
    }
}
```

### Step 2: Add Configuration

```java
// In ModConfig.java

// Add enum value
public enum LLMProvider {
    OLLAMA, CLAUDE, OPENAI, CUSTOM
}

// Add config section
builder.comment("Custom Provider Settings").push("custom");
customEndpoint = builder.define("endpoint", "http://localhost:8080");
customApiKey = builder.define("apiKey", "");
customModel = builder.define("model", "default");
builder.pop();
```

### Step 3: Register Provider

```java
// In LLMManager.initialize()

providers.put(ModConfig.LLMProvider.CUSTOM, new CustomProvider());
```

---

## 8. Performance Considerations

### Caching

Consider caching for repeated similar prompts:

```java
private final Cache<String, String> responseCache = 
    CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
```

### Connection Pooling

```java
OkHttpClient client = new OkHttpClient.Builder()
    .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
    .build();
```

### Timeout Configuration

```java
OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(Duration.ofSeconds(10))
    .readTimeout(Duration.ofSeconds(timeout))
    .writeTimeout(Duration.ofSeconds(timeout))
    .build();
```

### Response Streaming (Future)

For faster perceived response, implement streaming:

```java
// Ollama supports streaming
{
  "stream": true,
  ...
}

// Handle chunked response
// Send partial responses to client as they arrive
```

---

## 9. Error Handling

### Error Categories

| Category | Response |
|----------|----------|
| Connection refused | "LLM service unavailable" + log warning |
| Timeout | "NPC seems lost in thought..." |
| Rate limit (429) | Exponential backoff + retry |
| Auth error (401/403) | "Check API key" + disable provider |
| Model not found | "Check model name" + log error |
| Invalid response | Fallback message + log error |

### Fallback Messages

```java
private static final String[] FALLBACKS = {
    "[The storyteller seems lost in thought...]",
    "[A distant look crosses their face...]",
    "[They pause, listening to something you cannot hear...]"
};

private String getFallbackResponse() {
    return FALLBACKS[random.nextInt(FALLBACKS.length)];
}
```

### Retry Logic

```java
public CompletableFuture<String> chatWithRetry(String prompt, List<ChatMessage> messages) {
    return chat(prompt, messages)
        .exceptionally(e -> {
            if (shouldRetry(e) && retryCount < MAX_RETRIES) {
                return chatWithRetry(prompt, messages);
            }
            return getFallbackResponse();
        });
}
```

### Logging

```java
// Debug: Request details
LOGGER.debug("LLM Request: model={}, messages={}", model, messages.size());

// Info: Successful responses
LOGGER.info("LLM Response received in {}ms", duration);

// Warn: Recoverable errors
LOGGER.warn("LLM request timed out, using fallback");

// Error: Unrecoverable errors
LOGGER.error("LLM provider failed: {}", e.getMessage());
```
