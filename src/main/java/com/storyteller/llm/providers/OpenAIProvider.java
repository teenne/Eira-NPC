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
 * OpenAI LLM provider
 */
public class OpenAIProvider implements LLMProvider {
    
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Gson GSON = new Gson();
    
    private OkHttpClient client;
    private final AtomicBoolean available = new AtomicBoolean(false);
    
    private String apiKey;
    private String model;
    
    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.apiKey = ModConfig.COMMON.openaiApiKey.get();
                this.model = ModConfig.COMMON.openaiModel.get();
                
                if (apiKey == null || apiKey.isEmpty()) {
                    StorytellerMod.LOGGER.info("OpenAI API key not configured, skipping initialization");
                    return false;
                }
                
                int timeout = ModConfig.COMMON.responseTimeout.get();
                
                this.client = new OkHttpClient.Builder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .readTimeout(Duration.ofSeconds(timeout))
                    .writeTimeout(Duration.ofSeconds(timeout))
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
                
                RequestBody body = RequestBody.create(GSON.toJson(testRequest), JSON);
                Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        StorytellerMod.LOGGER.info("OpenAI API connection successful");
                        available.set(true);
                        return true;
                    } else {
                        String responseBody = response.body() != null ? response.body().string() : "Unknown error";
                        StorytellerMod.LOGGER.error("OpenAI API connection failed: HTTP {} - {}", response.code(), responseBody);
                        return false;
                    }
                }
            } catch (Exception e) {
                StorytellerMod.LOGGER.error("Failed to initialize OpenAI provider: {}", e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<String> chat(String systemPrompt, List<ChatMessage> messages) {
        return CompletableFuture.supplyAsync(() -> {
            if (!available.get()) {
                return "[OpenAI is not available. Please check your API key.]";
            }
            
            try {
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", model);
                requestBody.addProperty("max_tokens", 1024);
                requestBody.addProperty("temperature", 0.8);
                
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
                    String role = msg.role() == ChatMessage.Role.USER ? "user" : "assistant";
                    messageObj.addProperty("role", role);
                    messageObj.addProperty("content", msg.content());
                    messagesArray.add(messageObj);
                }
                
                requestBody.add("messages", messagesArray);
                
                RequestBody body = RequestBody.create(GSON.toJson(requestBody), JSON);
                Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JsonObject jsonResponse = GSON.fromJson(responseBody, JsonObject.class);
                        
                        if (jsonResponse.has("choices")) {
                            JsonArray choices = jsonResponse.getAsJsonArray("choices");
                            if (choices.size() > 0) {
                                return choices.get(0).getAsJsonObject()
                                    .getAsJsonObject("message")
                                    .get("content").getAsString();
                            }
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        StorytellerMod.LOGGER.error("OpenAI request failed: HTTP {} - {}", response.code(), errorBody);
                    }
                }
            } catch (IOException e) {
                StorytellerMod.LOGGER.error("OpenAI chat error: {}", e.getMessage());
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
        return "OpenAI (" + model + ")";
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
