package com.storyteller.llm.providers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.storyteller.StorytellerMod;
import com.storyteller.config.ModConfig;
import com.storyteller.llm.LLMProvider;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ollama LLM provider for local model inference
 */
public class OllamaProvider implements LLMProvider {
    
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Gson GSON = new Gson();
    
    private OkHttpClient client;
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
                
                this.client = new OkHttpClient.Builder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .readTimeout(Duration.ofSeconds(timeout))
                    .writeTimeout(Duration.ofSeconds(timeout))
                    .build();
                
                // Test connection by checking if Ollama is running
                Request request = new Request.Builder()
                    .url(endpoint + "/api/tags")
                    .get()
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        StorytellerMod.LOGGER.info("Ollama connection successful at {}", endpoint);
                        
                        // Check if the model is available
                        String body = response.body() != null ? response.body().string() : "";
                        if (!body.contains(model.split(":")[0])) {
                            StorytellerMod.LOGGER.warn("Model '{}' may not be available. Available models: {}", model, body);
                            StorytellerMod.LOGGER.info("To pull the model, run: ollama pull {}", model);
                        }
                        
                        available.set(true);
                        return true;
                    } else {
                        StorytellerMod.LOGGER.error("Ollama connection failed: HTTP {}", response.code());
                        return false;
                    }
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
                
                // Optional: Add generation parameters for better roleplay
                JsonObject options = new JsonObject();
                options.addProperty("temperature", 0.8);
                options.addProperty("top_p", 0.9);
                options.addProperty("repeat_penalty", 1.1);
                requestBody.add("options", options);
                
                RequestBody body = RequestBody.create(GSON.toJson(requestBody), JSON);
                Request request = new Request.Builder()
                    .url(endpoint + "/api/chat")
                    .post(body)
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JsonObject jsonResponse = GSON.fromJson(responseBody, JsonObject.class);
                        
                        if (jsonResponse.has("message")) {
                            return jsonResponse.getAsJsonObject("message").get("content").getAsString();
                        } else if (jsonResponse.has("error")) {
                            String error = jsonResponse.get("error").getAsString();
                            StorytellerMod.LOGGER.error("Ollama error: {}", error);
                            return "[The storyteller seems lost in thought...]";
                        }
                    } else {
                        StorytellerMod.LOGGER.error("Ollama request failed: HTTP {}", response.code());
                    }
                }
            } catch (IOException e) {
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
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }
}
