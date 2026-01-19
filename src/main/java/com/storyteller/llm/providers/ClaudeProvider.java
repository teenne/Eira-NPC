package com.storyteller.llm.providers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.storyteller.StorytellerMod;
import com.storyteller.config.ModConfig;
import com.storyteller.llm.LLMProvider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Claude (Anthropic) LLM provider
 */
public class ClaudeProvider implements LLMProvider {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";
    private static final Gson GSON = new Gson();

    private HttpClient client;
    private final AtomicBoolean available = new AtomicBoolean(false);

    private String apiKey;
    private String model;

    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.apiKey = ModConfig.COMMON.claudeApiKey.get();
                this.model = ModConfig.COMMON.claudeModel.get();

                if (apiKey == null || apiKey.isEmpty()) {
                    StorytellerMod.LOGGER.info("Claude API key not configured, skipping initialization");
                    return false;
                }

                int timeout = ModConfig.COMMON.responseTimeout.get();

                this.client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

                // Test with a minimal request
                JsonObject testRequest = new JsonObject();
                testRequest.addProperty("model", model);
                testRequest.addProperty("max_tokens", 10);

                JsonArray messages = new JsonArray();
                JsonObject testMsg = new JsonObject();
                testMsg.addProperty("role", "user");
                testMsg.addProperty("content", "Hi");
                messages.add(testMsg);
                testRequest.add("messages", messages);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(timeout))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", API_VERSION)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(testRequest)))
                    .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    StorytellerMod.LOGGER.info("Claude API connection successful");
                    available.set(true);
                    return true;
                } else {
                    String responseBody = response.body() != null ? response.body() : "Unknown error";
                    StorytellerMod.LOGGER.error("Claude API connection failed: HTTP {} - {}", response.statusCode(), responseBody);
                    return false;
                }
            } catch (Exception e) {
                StorytellerMod.LOGGER.error("Failed to initialize Claude provider: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<String> chat(String systemPrompt, List<ChatMessage> messages) {
        return CompletableFuture.supplyAsync(() -> {
            if (!available.get()) {
                return "[Claude is not available. Please check your API key.]";
            }

            try {
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", model);
                requestBody.addProperty("max_tokens", 1024);
                requestBody.addProperty("system", systemPrompt);

                // Build messages array
                JsonArray messagesArray = new JsonArray();

                for (ChatMessage msg : messages) {
                    JsonObject messageObj = new JsonObject();
                    // Claude uses "user" and "assistant" roles
                    String role = msg.role() == ChatMessage.Role.USER ? "user" : "assistant";
                    messageObj.addProperty("role", role);
                    messageObj.addProperty("content", msg.content());
                    messagesArray.add(messageObj);
                }

                requestBody.add("messages", messagesArray);

                int timeout = ModConfig.COMMON.responseTimeout.get();

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(timeout))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", API_VERSION)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                    .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
                    String responseBody = response.body();
                    JsonObject jsonResponse = GSON.fromJson(responseBody, JsonObject.class);

                    if (jsonResponse.has("content")) {
                        JsonArray content = jsonResponse.getAsJsonArray("content");
                        if (content.size() > 0) {
                            return content.get(0).getAsJsonObject().get("text").getAsString();
                        }
                    }
                } else {
                    String errorBody = response.body() != null ? response.body() : "Unknown error";
                    StorytellerMod.LOGGER.error("Claude request failed: HTTP {} - {}", response.statusCode(), errorBody);
                }
            } catch (Exception e) {
                StorytellerMod.LOGGER.error("Claude chat error: {}", e.getMessage());
            }

            return "[The storyteller seems lost in thought...]";
        });
    }

    @Override
    public boolean isAvailable() {
        return available.get();
    }

    @Override
    public String getName() {
        return "Claude (" + model + ")";
    }

    @Override
    public void shutdown() {
        available.set(false);
        // HttpClient doesn't need explicit shutdown
    }
}
