package com.storyteller.llm;

import com.storyteller.StorytellerMod;
import com.storyteller.config.ModConfig;
import com.storyteller.llm.providers.ClaudeProvider;
import com.storyteller.llm.providers.OllamaProvider;
import com.storyteller.llm.providers.OpenAIProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Manages LLM providers and routes requests to the active provider
 */
public class LLMManager {
    
    private final Map<ModConfig.LLMProvider, LLMProvider> providers = new HashMap<>();
    private LLMProvider activeProvider;
    
    public void initialize() {
        StorytellerMod.LOGGER.info("Initializing LLM providers...");
        
        // Create all providers
        providers.put(ModConfig.LLMProvider.OLLAMA, new OllamaProvider());
        providers.put(ModConfig.LLMProvider.CLAUDE, new ClaudeProvider());
        providers.put(ModConfig.LLMProvider.OPENAI, new OpenAIProvider());
        
        // Get configured provider
        ModConfig.LLMProvider configuredProvider = ModConfig.COMMON.llmProvider.get();
        StorytellerMod.LOGGER.info("Configured LLM provider: {}", configuredProvider.getDisplayName());
        
        // Initialize the configured provider
        LLMProvider provider = providers.get(configuredProvider);
        provider.initialize().thenAccept(success -> {
            if (success) {
                activeProvider = provider;
                StorytellerMod.LOGGER.info("LLM provider {} initialized successfully", provider.getName());
            } else {
                StorytellerMod.LOGGER.warn("Failed to initialize {}, trying fallbacks...", provider.getName());
                tryFallbackProviders(configuredProvider);
            }
        });
    }
    
    private void tryFallbackProviders(ModConfig.LLMProvider excludeProvider) {
        // Try other providers as fallback
        for (Map.Entry<ModConfig.LLMProvider, LLMProvider> entry : providers.entrySet()) {
            if (entry.getKey() != excludeProvider) {
                LLMProvider provider = entry.getValue();
                provider.initialize().thenAccept(success -> {
                    if (success && activeProvider == null) {
                        activeProvider = provider;
                        StorytellerMod.LOGGER.info("Fallback LLM provider {} initialized", provider.getName());
                    }
                });
            }
        }
    }
    
    /**
     * Send a chat request to the active LLM provider
     */
    public CompletableFuture<String> chat(String systemPrompt, List<LLMProvider.ChatMessage> messages) {
        if (activeProvider == null || !activeProvider.isAvailable()) {
            return CompletableFuture.completedFuture(
                "[No LLM provider available. Please check your configuration.]"
            );
        }
        
        return activeProvider.chat(systemPrompt, messages);
    }
    
    /**
     * Check if any LLM provider is available
     */
    public boolean isAvailable() {
        return activeProvider != null && activeProvider.isAvailable();
    }
    
    /**
     * Get the name of the active provider
     */
    public String getActiveProviderName() {
        return activeProvider != null ? activeProvider.getName() : "None";
    }
    
    /**
     * Switch to a different provider at runtime
     */
    public CompletableFuture<Boolean> switchProvider(ModConfig.LLMProvider providerType) {
        LLMProvider provider = providers.get(providerType);
        if (provider == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        return provider.initialize().thenApply(success -> {
            if (success) {
                if (activeProvider != null) {
                    activeProvider.shutdown();
                }
                activeProvider = provider;
                StorytellerMod.LOGGER.info("Switched to LLM provider: {}", provider.getName());
            }
            return success;
        });
    }
    
    /**
     * Shutdown all providers
     */
    public void shutdown() {
        StorytellerMod.LOGGER.info("Shutting down LLM providers...");
        for (LLMProvider provider : providers.values()) {
            provider.shutdown();
        }
        activeProvider = null;
    }
}
