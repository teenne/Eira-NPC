package com.storyteller.npc;

import com.storyteller.StorytellerMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple quest system for Storyteller NPCs.
 * NPCs can give quests which are tracked automatically.
 */
public class QuestManager {

    // Player UUID -> Active quests
    private static final Map<UUID, List<Quest>> activeQuests = new ConcurrentHashMap<>();

    // Player UUID -> Completed quests (for history)
    private static final Map<UUID, List<Quest>> completedQuests = new ConcurrentHashMap<>();

    // Patterns to detect quest-like statements in NPC responses
    private static final Pattern BRING_PATTERN = Pattern.compile(
        "bring (?:me )?(?:back )?(\\d+)?\\s*([\\w\\s]+?)(?:\\s+and|\\.|,|!|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIND_PATTERN = Pattern.compile(
        "find (?:me )?(\\d+)?\\s*([\\w\\s]+?)(?:\\s+and|\\.|,|!|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern KILL_PATTERN = Pattern.compile(
        "(?:kill|slay|defeat) (\\d+)?\\s*([\\w\\s]+?)(?:\\s+and|\\.|,|!|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COLLECT_PATTERN = Pattern.compile(
        "collect (\\d+)?\\s*([\\w\\s]+?)(?:\\s+and|\\.|,|!|$)", Pattern.CASE_INSENSITIVE);

    /**
     * Parse NPC response for quest-like statements and create quests automatically
     */
    public static List<Quest> parseQuestsFromResponse(UUID npcId, UUID playerId, String response) {
        List<Quest> quests = new ArrayList<>();

        // Try to detect item collection quests
        for (Pattern pattern : List.of(BRING_PATTERN, FIND_PATTERN, COLLECT_PATTERN)) {
            Matcher matcher = pattern.matcher(response);
            while (matcher.find()) {
                int count = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : 1;
                String target = matcher.group(2).trim().toLowerCase();

                // Skip common false positives
                if (target.length() < 3 || target.contains("way") || target.contains("you") ||
                    target.contains("something") || target.contains("anything")) {
                    continue;
                }

                Quest quest = new Quest(
                    UUID.randomUUID(),
                    npcId,
                    QuestType.COLLECT_ITEM,
                    target,
                    count,
                    "Collect " + count + " " + target
                );
                quests.add(quest);
            }
        }

        // Try to detect mob kill quests
        Matcher killMatcher = KILL_PATTERN.matcher(response);
        while (killMatcher.find()) {
            int count = killMatcher.group(1) != null ? Integer.parseInt(killMatcher.group(1)) : 1;
            String target = killMatcher.group(2).trim().toLowerCase();

            // Skip common false positives
            if (target.length() < 3 || target.contains("time")) {
                continue;
            }

            Quest quest = new Quest(
                UUID.randomUUID(),
                npcId,
                QuestType.KILL_MOB,
                target,
                count,
                "Kill " + count + " " + target
            );
            quests.add(quest);
        }

        // Register detected quests
        for (Quest quest : quests) {
            addQuest(playerId, quest);
            StorytellerMod.LOGGER.info("Auto-detected quest for {}: {}", playerId, quest.description());
        }

        return quests;
    }

    /**
     * Add a quest for a player
     */
    public static void addQuest(UUID playerId, Quest quest) {
        activeQuests.computeIfAbsent(playerId, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(quest);
    }

    /**
     * Get active quests for a player
     */
    public static List<Quest> getActiveQuests(UUID playerId) {
        return activeQuests.getOrDefault(playerId, Collections.emptyList());
    }

    /**
     * Get active quests from a specific NPC
     */
    public static List<Quest> getQuestsFromNPC(UUID playerId, UUID npcId) {
        return getActiveQuests(playerId).stream()
            .filter(q -> q.npcId().equals(npcId))
            .toList();
    }

    /**
     * Clear all active quests for a player
     */
    public static void clearQuests(UUID playerId) {
        activeQuests.remove(playerId);
        StorytellerMod.LOGGER.info("Cleared all quests for player {}", playerId);
    }

    /**
     * Check if any quests are completed and return them
     */
    public static List<Quest> checkQuestCompletion(ServerPlayer player) {
        List<Quest> completed = new ArrayList<>();
        List<Quest> active = activeQuests.getOrDefault(player.getUUID(), Collections.emptyList());

        for (Quest quest : new ArrayList<>(active)) {
            boolean isComplete = switch (quest.type()) {
                case COLLECT_ITEM -> checkItemQuest(player, quest);
                case KILL_MOB -> quest.progress() >= quest.targetCount();
            };

            if (isComplete) {
                completed.add(quest);
                active.remove(quest);
                completedQuests.computeIfAbsent(player.getUUID(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(quest);
                StorytellerMod.LOGGER.info("Quest completed for {}: {}", player.getName().getString(), quest.description());
            }
        }

        return completed;
    }

    private static boolean checkItemQuest(ServerPlayer player, Quest quest) {
        int count = 0;
        String target = quest.target().toLowerCase();

        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                String itemName = stack.getItem().toString().toLowerCase();
                String displayName = stack.getHoverName().getString().toLowerCase();

                if (itemName.contains(target) || displayName.contains(target)) {
                    count += stack.getCount();
                }
            }
        }

        return count >= quest.targetCount();
    }

    /**
     * Build quest context for NPC conversation
     */
    public static String buildQuestContext(UUID playerId, UUID npcId) {
        List<Quest> npcQuests = getQuestsFromNPC(playerId, npcId);
        if (npcQuests.isEmpty()) {
            return null;
        }

        StringBuilder context = new StringBuilder();
        context.append("## Active Quests You Gave This Player\n");

        for (Quest quest : npcQuests) {
            context.append("- ").append(quest.description());
            if (quest.type() == QuestType.KILL_MOB) {
                context.append(" (Progress: ").append(quest.progress()).append("/").append(quest.targetCount()).append(")");
            }
            context.append("\n");
        }

        return context.toString();
    }

    // Event handler for mob kills
    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            String mobName = event.getEntity().getType().getDescription().getString().toLowerCase();

            List<Quest> active = activeQuests.getOrDefault(player.getUUID(), Collections.emptyList());
            for (Quest quest : active) {
                if (quest.type() == QuestType.KILL_MOB && mobName.contains(quest.target())) {
                    quest.incrementProgress();
                    StorytellerMod.LOGGER.debug("Quest progress: {} - {}/{}", quest.description(), quest.progress(), quest.targetCount());
                }
            }
        }
    }

    // Quest record
    public static class Quest {
        private final UUID id;
        private final UUID npcId;
        private final QuestType type;
        private final String target;
        private final int targetCount;
        private final String description;
        private int progress = 0;

        public Quest(UUID id, UUID npcId, QuestType type, String target, int targetCount, String description) {
            this.id = id;
            this.npcId = npcId;
            this.type = type;
            this.target = target;
            this.targetCount = targetCount;
            this.description = description;
        }

        public UUID id() { return id; }
        public UUID npcId() { return npcId; }
        public QuestType type() { return type; }
        public String target() { return target; }
        public int targetCount() { return targetCount; }
        public String description() { return description; }
        public int progress() { return progress; }

        public void incrementProgress() {
            this.progress++;
        }
    }

    public enum QuestType {
        COLLECT_ITEM,
        KILL_MOB
    }
}
