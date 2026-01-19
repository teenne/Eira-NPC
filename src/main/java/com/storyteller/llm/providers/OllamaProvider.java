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
 * Ollama LLM provider for local model inference
 */
public class OllamaProvider implements LLMProvider {

    private static final Gson GSON = new Gson();

    private HttpClient client;
    private final AtomicBoolean available = new AtomicBoolean(false);

    private String endpoint;
    private String model;

    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.endpoint = ModConfig.COMMON.ollamaEndpoint.get();
                this.model = ModConfig.COMMON.ollamaModel.get();
                int timeout = ModConfig.COMMON.ollamaTimeout.get();

                this.client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

                // Test connection by checking if Ollama is running
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + "/api/tags"))
                    .timeout(Duration.ofSeconds(timeout))
                    .GET()
                    .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    StorytellerMod.LOGGER.info("Ollama connection successful at {}", endpoint);

                    // Check if the model is available
                    String body = response.body();
                    if (body != null && !body.contains(model.split(":")[0])) {
                        StorytellerMod.LOGGER.warn("Model '{}' may not be available. Available models: {}", model, body);
                        StorytellerMod.LOGGER.info("To pull the model, run: ollama pull {}", model);
                    }

                    available.set(true);
                    return true;
                } else {
                    StorytellerMod.LOGGER.error("Ollama connection failed: HTTP {}", response.statusCode());
                    return false;
                }
            } catch (Exception e) {
                StorytellerMod.LOGGER.error("Failed to initialize Ollama provider: {}", e.getMessage());
                StorytellerMod.LOGGER.info("Make sure Ollama is running: ollama serve");
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<String> chat(String systemPrompt, List<ChatMessage> messages) {
        return CompletableFuture.supplyAsync(() -> {
            if (!available.get()) {
                return "[Ollama is not available. Please check the server logs.]";
            }

            try {
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", model);
                requestBody.addProperty("stream", false);

                // Build messages array
                JsonArray messagesArray = new JsonArray();

                // Add system prompt
                JsonObject systemMessage = new JsonObject();
                systemMessage.addProperty("role", "system");
                systemMessage.addProperty("content", systemPrompt);
                messagesArray.add(systemMessage);

                // Add conversation history
                for (ChatMessage msg : messages) {
                    JsonObject messageObj = new JsonObject();
                    messageObj.addProperty("role", msg.role().name().toLowerCase());
                    messageObj.addProperty("content", msg.content());
                    messagesArray.add(messageObj);
                }

                requestBody.add("messages", messagesArray);

                // Generation parameters for roleplay - keep responses short and snappy
                JsonObject options = new JsonObject();
                options.addProperty("temperature", 0.7);
                options.addProperty("top_p", 0.9);
                options.addProperty("repeat_penalty", 1.1);
                options.addProperty("num_predict", 60); // Limit response to ~60 tokens (1-2 sentences)
                requestBody.add("options", options);

                int timeout = ModConfig.COMMON.ollamaTimeout.get();

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + "/api/chat"))
                    .timeout(Duration.ofSeconds(timeout))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                    .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
                    String responseBody = response.body();
                    JsonObject jsonResponse = GSON.fromJson(responseBody, JsonObject.class);

                    if (jsonResponse.has("message")) {
                        return jsonResponse.getAsJsonObject("message").get("content").getAsString();
                    } else if (jsonResponse.has("error")) {
                        String error = jsonResponse.get("error").getAsString();
                        StorytellerMod.LOGGER.error("Ollama error: {}", error);
                        return "[The storyteller seems lost in thought...]";
                    }
                } else {
                    StorytellerMod.LOGGER.error("Ollama request failed: HTTP {}", response.statusCode());
                }
            } catch (Exception e) {
                StorytellerMod.LOGGER.error("Ollama chat error: {}", e.getMessage());
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
        return "Ollama (" + model + ")";
    }

    @Override
    public void shutdown() {
        available.set(false);
        // HttpClient doesn't need explicit shutdown
    }
}
