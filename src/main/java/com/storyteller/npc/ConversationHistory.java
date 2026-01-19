package com.storyteller.npc;

import com.storyteller.config.ModConfig;
import com.storyteller.llm.LLMProvider.ChatMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks conversation history between players and NPCs
 */
public class ConversationHistory {
    
    // Map of NPC UUID -> Player UUID -> Message history
    private static final Map<UUID, Map<UUID, List<ChatMessage>>> histories = new ConcurrentHashMap<>();
    
    // Track conversation counts for hidden agenda reveal conditions
    private static final Map<UUID, Map<UUID, Integer>> conversationCounts = new ConcurrentHashMap<>();
    
    // Track last interaction times for rate limiting
    private static final Map<UUID, Map<UUID, Long>> lastInteractionTimes = new ConcurrentHashMap<>();
    
    /**
     * Add a message to the conversation history
     */
    public static void addMessage(UUID npcId, UUID playerId, ChatMessage message) {
        histories.computeIfAbsent(npcId, k -> new ConcurrentHashMap<>())
                 .computeIfAbsent(playerId, k -> Collections.synchronizedList(new ArrayList<>()))
                 .add(message);
        
        // Trim to max history size
        int maxHistory = ModConfig.COMMON.maxConversationHistory.get();
        List<ChatMessage> playerHistory = histories.get(npcId).get(playerId);
        
        while (playerHistory.size() > maxHistory) {
            playerHistory.remove(0);
        }
        
        // Update interaction time
        lastInteractionTimes.computeIfAbsent(npcId, k -> new ConcurrentHashMap<>())
                           .put(playerId, System.currentTimeMillis());
    }
    
    /**
     * Get conversation history for a player with an NPC
     */
    public static List<ChatMessage> getHistory(UUID npcId, UUID playerId) {
        return histories.getOrDefault(npcId, Collections.emptyMap())
                       .getOrDefault(playerId, Collections.emptyList());
    }
    
    /**
     * Clear conversation history for a player with an NPC
     */
    public static void clearHistory(UUID npcId, UUID playerId) {
        Map<UUID, List<ChatMessage>> npcHistories = histories.get(npcId);
        if (npcHistories != null) {
            npcHistories.remove(playerId);
        }
    }
    
    /**
     * Clear all history for an NPC
     */
    public static void clearNPCHistory(UUID npcId) {
        histories.remove(npcId);
        conversationCounts.remove(npcId);
        lastInteractionTimes.remove(npcId);
    }
    
    /**
     * Increment conversation count (called when a full exchange completes)
     */
    public static void incrementConversationCount(UUID npcId, UUID playerId) {
        conversationCounts.computeIfAbsent(npcId, k -> new ConcurrentHashMap<>())
                         .merge(playerId, 1, Integer::sum);
    }
    
    /**
     * Get total conversation count between player and NPC
     */
    public static int getConversationCount(UUID npcId, UUID playerId) {
        return conversationCounts.getOrDefault(npcId, Collections.emptyMap())
                                .getOrDefault(playerId, 0);
    }
    
    /**
     * Check if enough time has passed since last interaction (for rate limiting)
     */
    public static boolean canInteract(UUID npcId, UUID playerId) {
        long minTime = ModConfig.COMMON.minTimeBetweenMessages.get() * 50L; // Convert ticks to ms
        
        Long lastTime = lastInteractionTimes.getOrDefault(npcId, Collections.emptyMap())
                                            .get(playerId);
        
        if (lastTime == null) {
            return true;
        }
        
        return System.currentTimeMillis() - lastTime >= minTime;
    }
    
    /**
     * Get time until player can interact again (in milliseconds)
     */
    public static long getTimeUntilCanInteract(UUID npcId, UUID playerId) {
        long minTime = ModConfig.COMMON.minTimeBetweenMessages.get() * 50L;
        
        Long lastTime = lastInteractionTimes.getOrDefault(npcId, Collections.emptyMap())
                                            .get(playerId);
        
        if (lastTime == null) {
            return 0;
        }
        
        long elapsed = System.currentTimeMillis() - lastTime;
        return Math.max(0, minTime - elapsed);
    }
    
    /**
     * Build a summary of the conversation for context
     * Useful for including key topics discussed in the prompt
     */
    public static String buildConversationSummary(UUID npcId, UUID playerId) {
        List<ChatMessage> history = getHistory(npcId, playerId);
        if (history.isEmpty()) {
            return "This is your first conversation with this player.";
        }
        
        int count = getConversationCount(npcId, playerId);
        
        StringBuilder summary = new StringBuilder();
        summary.append("You have had ").append(count).append(" conversation(s) with this player.\n");
        
        // Include recent messages for context
        int recentCount = Math.min(6, history.size());
        if (recentCount > 0) {
            summary.append("Recent exchange:\n");
            List<ChatMessage> recent = history.subList(history.size() - recentCount, history.size());
            for (ChatMessage msg : recent) {
                String role = msg.role() == ChatMessage.Role.USER ? "Player" : "You";
                String content = msg.content();
                if (content.length() > 100) {
                    content = content.substring(0, 100) + "...";
                }
                summary.append("- ").append(role).append(": ").append(content).append("\n");
            }
        }
        
        return summary.toString();
    }
}
