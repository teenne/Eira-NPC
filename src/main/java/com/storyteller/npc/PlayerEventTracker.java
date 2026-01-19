package com.storyteller.npc;

import com.storyteller.StorytellerMod;
import com.storyteller.config.ModConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player events for NPC conversation context.
 * NPCs can react to recent player achievements and actions.
 */
public class PlayerEventTracker {

    // Player UUID -> Recent events (max 10, expire after 5 minutes)
    private static final Map<UUID, List<PlayerEvent>> recentEvents = new ConcurrentHashMap<>();
    private static final int MAX_EVENTS = 10;
    private static final long EVENT_EXPIRY_MS = 5 * 60 * 1000; // 5 minutes

    /**
     * Record a player event
     */
    public static void recordEvent(UUID playerId, PlayerEvent event) {
        // Check if event tracking is enabled
        if (!ModConfig.COMMON.enableEventTracking.get()) {
            return;
        }

        recentEvents.computeIfAbsent(playerId, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(event);

        // Trim to max size
        List<PlayerEvent> events = recentEvents.get(playerId);
        while (events.size() > MAX_EVENTS) {
            events.remove(0);
        }
    }

    /**
     * Get recent events for a player, filtering out expired ones
     */
    public static List<PlayerEvent> getRecentEvents(UUID playerId) {
        // Return empty if event tracking is disabled
        if (!ModConfig.COMMON.enableEventTracking.get()) {
            return Collections.emptyList();
        }

        List<PlayerEvent> events = recentEvents.getOrDefault(playerId, Collections.emptyList());
        long now = System.currentTimeMillis();
        long expiryMs = ModConfig.COMMON.eventExpiryMinutes.get() * 60 * 1000L;

        // Filter expired events
        List<PlayerEvent> valid = new ArrayList<>();
        for (PlayerEvent event : events) {
            if (now - event.timestamp() < expiryMs) {
                valid.add(event);
            }
        }

        return valid;
    }

    /**
     * Build context string for NPC conversation
     */
    public static String buildEventContext(UUID playerId) {
        List<PlayerEvent> events = getRecentEvents(playerId);
        if (events.isEmpty()) {
            return null;
        }

        StringBuilder context = new StringBuilder();
        context.append("## Recent Player Activity (react naturally if relevant)\n");

        for (PlayerEvent event : events) {
            context.append("- ").append(event.description()).append("\n");
        }

        return context.toString();
    }

    /**
     * Clear events for a player
     */
    public static void clearEvents(UUID playerId) {
        recentEvents.remove(playerId);
    }

    // Event record
    public record PlayerEvent(EventType type, String description, long timestamp) {
        public PlayerEvent(EventType type, String description) {
            this(type, description, System.currentTimeMillis());
        }
    }

    public enum EventType {
        ADVANCEMENT,
        MOB_KILL,
        RARE_ITEM,
        DIMENSION_CHANGE,
        DEATH
    }

    // ==================== Event Handlers ====================

    @SubscribeEvent
    public void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String advancementName = event.getAdvancement().id().getPath()
                .replace("_", " ")
                .replace("/", " - ");

            // Skip recipe unlocks and hidden advancements
            if (advancementName.contains("recipe") || advancementName.startsWith("recipes/")) {
                return;
            }

            recordEvent(player.getUUID(), new PlayerEvent(
                EventType.ADVANCEMENT,
                "Just earned the advancement: " + advancementName
            ));

            StorytellerMod.LOGGER.debug("Tracked advancement for {}: {}", player.getName().getString(), advancementName);
        }
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            EntityType<?> entityType = event.getEntity().getType();
            String mobName = entityType.getDescription().getString();

            // Only track notable kills
            if (isNotableMob(entityType)) {
                recordEvent(player.getUUID(), new PlayerEvent(
                    EventType.MOB_KILL,
                    "Recently killed a " + mobName
                ));

                StorytellerMod.LOGGER.debug("Tracked mob kill for {}: {}", player.getName().getString(), mobName);
            }
        }
    }

    private boolean isNotableMob(EntityType<?> type) {
        String name = EntityType.getKey(type).getPath();
        // Boss mobs and rare/dangerous mobs
        return name.contains("dragon") ||
               name.contains("wither") ||
               name.contains("elder_guardian") ||
               name.contains("warden") ||
               name.contains("ravager") ||
               name.contains("evoker") ||
               name.contains("vindicator") ||
               name.contains("pillager") ||
               name.contains("witch") ||
               name.contains("blaze") ||
               name.contains("ghast") ||
               name.contains("enderman");
    }

    /**
     * Check if player has a notable item in their inventory (call periodically or on interaction)
     */
    public static void checkNotableItems(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (isNotableItem(stack)) {
                String itemName = stack.getHoverName().getString();

                // Avoid duplicates
                List<PlayerEvent> events = getRecentEvents(player.getUUID());
                boolean alreadyTracked = events.stream()
                    .anyMatch(e -> e.type() == EventType.RARE_ITEM && e.description().contains(itemName));

                if (!alreadyTracked) {
                    recordEvent(player.getUUID(), new PlayerEvent(
                        EventType.RARE_ITEM,
                        "Carrying a " + itemName
                    ));
                }
            }
        }
    }

    private static boolean isNotableItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        String name = stack.getItem().toString().toLowerCase();

        // Notable items NPCs might comment on
        return name.contains("netherite") ||
               name.contains("diamond") ||
               name.contains("totem") ||
               name.contains("elytra") ||
               name.contains("trident") ||
               name.contains("nether_star") ||
               name.contains("dragon") ||
               name.contains("enchanted_golden_apple") ||
               name.contains("beacon") ||
               stack.isEnchanted(); // Any enchanted item
    }
}
