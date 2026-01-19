package com.storyteller.npc;

import com.storyteller.llm.LLMProvider.ChatMessage;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConversationHistory
 *
 * Note: Some tests are limited because ConversationHistory depends on ModConfig
 * which requires NeoForge. Tests that need config values use reflection to
 * access internal state directly.
 */
class ConversationHistoryTest {

    private static final UUID TEST_NPC_ID = UUID.randomUUID();
    private static final UUID TEST_PLAYER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        // Clear static state before each test
        clearStaticMaps();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up after tests
        clearStaticMaps();
    }

    /**
     * Clear the static maps using reflection
     */
    private void clearStaticMaps() throws Exception {
        clearMap("histories");
        clearMap("conversationCounts");
        clearMap("lastInteractionTimes");
    }

    @SuppressWarnings("unchecked")
    private void clearMap(String fieldName) throws Exception {
        Field field = ConversationHistory.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        Map<?, ?> map = (Map<?, ?>) field.get(null);
        map.clear();
    }

    @SuppressWarnings("unchecked")
    private <K, V> Map<K, V> getStaticMap(String fieldName) throws Exception {
        Field field = ConversationHistory.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Map<K, V>) field.get(null);
    }

    @Test
    @DisplayName("getHistory should return empty list for unknown NPC")
    void getHistoryShouldReturnEmptyListForUnknownNPC() {
        List<ChatMessage> history = ConversationHistory.getHistory(UUID.randomUUID(), UUID.randomUUID());

        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    @DisplayName("getHistory should return empty list for unknown player")
    void getHistoryShouldReturnEmptyListForUnknownPlayer() {
        List<ChatMessage> history = ConversationHistory.getHistory(TEST_NPC_ID, UUID.randomUUID());

        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    @DisplayName("clearHistory should remove history for specific player")
    void clearHistoryShouldRemoveHistoryForPlayer() throws Exception {
        // Manually add history using reflection
        Map<UUID, Map<UUID, List<ChatMessage>>> histories = getStaticMap("histories");
        Map<UUID, List<ChatMessage>> npcHistory = new ConcurrentHashMap<>();
        npcHistory.put(TEST_PLAYER_ID, Collections.synchronizedList(new ArrayList<>(List.of(
            new ChatMessage(ChatMessage.Role.USER, "Hello")
        ))));
        histories.put(TEST_NPC_ID, npcHistory);

        // Verify history exists
        assertFalse(ConversationHistory.getHistory(TEST_NPC_ID, TEST_PLAYER_ID).isEmpty());

        // Clear history
        ConversationHistory.clearHistory(TEST_NPC_ID, TEST_PLAYER_ID);

        // Verify history is cleared
        assertTrue(ConversationHistory.getHistory(TEST_NPC_ID, TEST_PLAYER_ID).isEmpty());
    }

    @Test
    @DisplayName("clearNPCHistory should remove all history for NPC")
    void clearNPCHistoryShouldRemoveAllHistory() throws Exception {
        // Setup: Add history for multiple players
        Map<UUID, Map<UUID, List<ChatMessage>>> histories = getStaticMap("histories");
        Map<UUID, List<ChatMessage>> npcHistory = new ConcurrentHashMap<>();

        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        npcHistory.put(player1, Collections.synchronizedList(new ArrayList<>(List.of(
            new ChatMessage(ChatMessage.Role.USER, "Hello from player 1")
        ))));
        npcHistory.put(player2, Collections.synchronizedList(new ArrayList<>(List.of(
            new ChatMessage(ChatMessage.Role.USER, "Hello from player 2")
        ))));
        histories.put(TEST_NPC_ID, npcHistory);

        // Also add conversation counts
        Map<UUID, Map<UUID, Integer>> counts = getStaticMap("conversationCounts");
        Map<UUID, Integer> npcCounts = new ConcurrentHashMap<>();
        npcCounts.put(player1, 5);
        npcCounts.put(player2, 3);
        counts.put(TEST_NPC_ID, npcCounts);

        // Clear NPC history
        ConversationHistory.clearNPCHistory(TEST_NPC_ID);

        // Verify all data is cleared
        assertTrue(ConversationHistory.getHistory(TEST_NPC_ID, player1).isEmpty());
        assertTrue(ConversationHistory.getHistory(TEST_NPC_ID, player2).isEmpty());
        assertEquals(0, ConversationHistory.getConversationCount(TEST_NPC_ID, player1));
        assertEquals(0, ConversationHistory.getConversationCount(TEST_NPC_ID, player2));
    }

    @Test
    @DisplayName("incrementConversationCount should increase count")
    void incrementConversationCountShouldIncreaseCount() {
        assertEquals(0, ConversationHistory.getConversationCount(TEST_NPC_ID, TEST_PLAYER_ID));

        ConversationHistory.incrementConversationCount(TEST_NPC_ID, TEST_PLAYER_ID);
        assertEquals(1, ConversationHistory.getConversationCount(TEST_NPC_ID, TEST_PLAYER_ID));

        ConversationHistory.incrementConversationCount(TEST_NPC_ID, TEST_PLAYER_ID);
        assertEquals(2, ConversationHistory.getConversationCount(TEST_NPC_ID, TEST_PLAYER_ID));
    }

    @Test
    @DisplayName("getConversationCount should return 0 for new player")
    void getConversationCountShouldReturnZeroForNewPlayer() {
        int count = ConversationHistory.getConversationCount(TEST_NPC_ID, UUID.randomUUID());

        assertEquals(0, count);
    }

    @Test
    @DisplayName("getConversationCount should track counts per player")
    void getConversationCountShouldTrackPerPlayer() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        ConversationHistory.incrementConversationCount(TEST_NPC_ID, player1);
        ConversationHistory.incrementConversationCount(TEST_NPC_ID, player1);
        ConversationHistory.incrementConversationCount(TEST_NPC_ID, player2);

        assertEquals(2, ConversationHistory.getConversationCount(TEST_NPC_ID, player1));
        assertEquals(1, ConversationHistory.getConversationCount(TEST_NPC_ID, player2));
    }

    @Test
    @DisplayName("getConversationCount should track counts per NPC")
    void getConversationCountShouldTrackPerNPC() {
        UUID npc1 = UUID.randomUUID();
        UUID npc2 = UUID.randomUUID();

        ConversationHistory.incrementConversationCount(npc1, TEST_PLAYER_ID);
        ConversationHistory.incrementConversationCount(npc1, TEST_PLAYER_ID);
        ConversationHistory.incrementConversationCount(npc2, TEST_PLAYER_ID);

        assertEquals(2, ConversationHistory.getConversationCount(npc1, TEST_PLAYER_ID));
        assertEquals(1, ConversationHistory.getConversationCount(npc2, TEST_PLAYER_ID));
    }

    @Test
    @DisplayName("buildConversationSummary should return first conversation message for new player")
    void buildConversationSummaryShouldReturnFirstConversationMessage() {
        String summary = ConversationHistory.buildConversationSummary(TEST_NPC_ID, TEST_PLAYER_ID);

        assertEquals("This is your first conversation with this player.", summary);
    }

    @Test
    @DisplayName("buildConversationSummary should include conversation count")
    void buildConversationSummaryShouldIncludeCount() throws Exception {
        // Add some messages
        Map<UUID, Map<UUID, List<ChatMessage>>> histories = getStaticMap("histories");
        Map<UUID, List<ChatMessage>> npcHistory = new ConcurrentHashMap<>();
        npcHistory.put(TEST_PLAYER_ID, Collections.synchronizedList(new ArrayList<>(List.of(
            new ChatMessage(ChatMessage.Role.USER, "Hello"),
            new ChatMessage(ChatMessage.Role.ASSISTANT, "Hi there")
        ))));
        histories.put(TEST_NPC_ID, npcHistory);

        // Set conversation count
        ConversationHistory.incrementConversationCount(TEST_NPC_ID, TEST_PLAYER_ID);
        ConversationHistory.incrementConversationCount(TEST_NPC_ID, TEST_PLAYER_ID);
        ConversationHistory.incrementConversationCount(TEST_NPC_ID, TEST_PLAYER_ID);

        String summary = ConversationHistory.buildConversationSummary(TEST_NPC_ID, TEST_PLAYER_ID);

        assertTrue(summary.contains("3 conversation(s)"));
        assertTrue(summary.contains("Recent exchange:"));
    }

    @Test
    @DisplayName("buildConversationSummary should truncate long messages")
    void buildConversationSummaryShouldTruncateLongMessages() throws Exception {
        // Add a long message
        String longMessage = "A".repeat(200);
        Map<UUID, Map<UUID, List<ChatMessage>>> histories = getStaticMap("histories");
        Map<UUID, List<ChatMessage>> npcHistory = new ConcurrentHashMap<>();
        npcHistory.put(TEST_PLAYER_ID, Collections.synchronizedList(new ArrayList<>(List.of(
            new ChatMessage(ChatMessage.Role.USER, longMessage)
        ))));
        histories.put(TEST_NPC_ID, npcHistory);
        ConversationHistory.incrementConversationCount(TEST_NPC_ID, TEST_PLAYER_ID);

        String summary = ConversationHistory.buildConversationSummary(TEST_NPC_ID, TEST_PLAYER_ID);

        assertTrue(summary.contains("..."));
        // Message should be truncated to 100 chars + "..."
        assertFalse(summary.contains("A".repeat(200)));
    }

    @Test
    @DisplayName("buildConversationSummary should show recent messages up to 6")
    void buildConversationSummaryShouldShowRecentMessages() throws Exception {
        // Add 10 messages
        List<ChatMessage> messages = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ChatMessage.Role role = i % 2 == 0 ? ChatMessage.Role.USER : ChatMessage.Role.ASSISTANT;
            messages.add(new ChatMessage(role, "Message " + i));
        }

        Map<UUID, Map<UUID, List<ChatMessage>>> histories = getStaticMap("histories");
        Map<UUID, List<ChatMessage>> npcHistory = new ConcurrentHashMap<>();
        npcHistory.put(TEST_PLAYER_ID, Collections.synchronizedList(new ArrayList<>(messages)));
        histories.put(TEST_NPC_ID, npcHistory);
        ConversationHistory.incrementConversationCount(TEST_NPC_ID, TEST_PLAYER_ID);

        String summary = ConversationHistory.buildConversationSummary(TEST_NPC_ID, TEST_PLAYER_ID);

        // Should contain the last 6 messages (4-9)
        assertTrue(summary.contains("Message 4"));
        assertTrue(summary.contains("Message 9"));
        // Should NOT contain earlier messages
        assertFalse(summary.contains("Message 3"));
    }

    @Test
    @DisplayName("buildConversationSummary should format roles correctly")
    void buildConversationSummaryShouldFormatRoles() throws Exception {
        Map<UUID, Map<UUID, List<ChatMessage>>> histories = getStaticMap("histories");
        Map<UUID, List<ChatMessage>> npcHistory = new ConcurrentHashMap<>();
        npcHistory.put(TEST_PLAYER_ID, Collections.synchronizedList(new ArrayList<>(List.of(
            new ChatMessage(ChatMessage.Role.USER, "Hello"),
            new ChatMessage(ChatMessage.Role.ASSISTANT, "Greetings")
        ))));
        histories.put(TEST_NPC_ID, npcHistory);
        ConversationHistory.incrementConversationCount(TEST_NPC_ID, TEST_PLAYER_ID);

        String summary = ConversationHistory.buildConversationSummary(TEST_NPC_ID, TEST_PLAYER_ID);

        assertTrue(summary.contains("- Player: Hello"));
        assertTrue(summary.contains("- You: Greetings"));
    }

    @Test
    @DisplayName("Static maps should be thread-safe")
    void staticMapsShouldBeThreadSafe() throws Exception {
        int threadCount = 10;
        int operationsPerThread = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < operationsPerThread; i++) {
                        UUID npcId = UUID.randomUUID();
                        UUID playerId = UUID.randomUUID();

                        // Perform various operations
                        ConversationHistory.getHistory(npcId, playerId);
                        ConversationHistory.getConversationCount(npcId, playerId);
                        ConversationHistory.incrementConversationCount(npcId, playerId);
                        ConversationHistory.clearHistory(npcId, playerId);
                        ConversationHistory.buildConversationSummary(npcId, playerId);
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);

        assertTrue(completed, "Threads should complete within timeout");
        assertTrue(exceptions.isEmpty(), "No exceptions should occur: " + exceptions);
    }

    @Test
    @DisplayName("clearHistory should be idempotent for non-existent player")
    void clearHistoryShouldBeIdempotentForNonExistentPlayer() {
        // Should not throw for non-existent NPC/player
        assertDoesNotThrow(() ->
            ConversationHistory.clearHistory(UUID.randomUUID(), UUID.randomUUID())
        );
    }

    @Test
    @DisplayName("clearNPCHistory should be idempotent for non-existent NPC")
    void clearNPCHistoryShouldBeIdempotentForNonExistentNPC() {
        // Should not throw for non-existent NPC
        assertDoesNotThrow(() ->
            ConversationHistory.clearNPCHistory(UUID.randomUUID())
        );
    }

    // =========================================================================
    // The following tests require ModConfig which is not available in unit tests
    // They are documented here for reference and should be run in integration tests
    // =========================================================================

    @Test
    @DisplayName("addMessage should add to history (requires ModConfig)")
    @Disabled("Requires ModConfig - run in integration test")
    void addMessageShouldAddToHistory() {
        // This test requires ModConfig.COMMON.maxConversationHistory to be available
        ChatMessage message = new ChatMessage(ChatMessage.Role.USER, "Hello");

        ConversationHistory.addMessage(TEST_NPC_ID, TEST_PLAYER_ID, message);

        List<ChatMessage> history = ConversationHistory.getHistory(TEST_NPC_ID, TEST_PLAYER_ID);
        assertEquals(1, history.size());
        assertEquals("Hello", history.get(0).content());
    }

    @Test
    @DisplayName("addMessage should trim history to max size (requires ModConfig)")
    @Disabled("Requires ModConfig - run in integration test")
    void addMessageShouldTrimHistory() {
        // This test requires ModConfig.COMMON.maxConversationHistory
    }

    @Test
    @DisplayName("canInteract should return true for new player (requires ModConfig)")
    @Disabled("Requires ModConfig - run in integration test")
    void canInteractShouldReturnTrueForNewPlayer() {
        // This test requires ModConfig.COMMON.minTimeBetweenMessages
    }

    @Test
    @DisplayName("canInteract should block rapid interactions (requires ModConfig)")
    @Disabled("Requires ModConfig - run in integration test")
    void canInteractShouldBlockRapidInteractions() {
        // This test requires ModConfig.COMMON.minTimeBetweenMessages
    }

    @Test
    @DisplayName("getTimeUntilCanInteract should return remaining time (requires ModConfig)")
    @Disabled("Requires ModConfig - run in integration test")
    void getTimeUntilCanInteractShouldReturnRemainingTime() {
        // This test requires ModConfig.COMMON.minTimeBetweenMessages
    }
}
