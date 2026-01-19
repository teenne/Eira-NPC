package com.storyteller.llm;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for LLM providers (Ollama, Claude, OpenAI, etc.)
 */
public interface LLMProvider {
    
    /**
     * Initialize the provider (test connection, etc.)
     * @return true if initialization successful
     */
    CompletableFuture<Boolean> initialize();
    
    /**
     * Send a chat completion request
     * @param systemPrompt The system prompt defining the character
     * @param messages Conversation history
     * @return The assistant's response
     */
    CompletableFuture<String> chat(String systemPrompt, List<ChatMessage> messages);
    
    /**
     * Check if the provider is available and ready
     * @return true if ready to accept requests
     */
    boolean isAvailable();
    
    /**
     * Get the provider name for display/logging
     */
    String getName();
    
    /**
     * Shutdown the provider, cleanup resources
     */
    void shutdown();
    
    /**
     * Represents a chat message in the conversation
     */
    record ChatMessage(Role role, String content) {
        public enum Role {
            USER,
            ASSISTANT,
            SYSTEM
        }
    }
}
