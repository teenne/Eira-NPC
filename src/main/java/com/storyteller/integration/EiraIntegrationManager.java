package com.storyteller.integration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.storyteller.StorytellerMod;
import com.storyteller.config.ModConfig;
import com.storyteller.entity.StorytellerNPC;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import okhttp3.*;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages integration with Eira Relay mod for HTTP-based event communication.
 * 
 * Enables:
 * - Inbound: Physical world events → NPC behaviors (via redstone detection)
 * - Outbound: NPC events → Physical world actions (via redstone emission + webhooks)
 */
public class EiraIntegrationManager {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Webhook HTTP client
    private OkHttpClient webhookClient;
    
    // Track redstone states to detect changes
    private final Map<UUID, Map<BlockPos, Boolean>> npcRedstoneStates = new ConcurrentHashMap<>();
    
    // Cooldown tracking for events
    private final Map<UUID, Long> eventCooldowns = new ConcurrentHashMap<>();
    
    // Track NPCs currently emitting redstone
    private final Map<UUID, RedstoneEmission> activeEmissions = new ConcurrentHashMap<>();
    
    private boolean initialized = false;
    
    public void initialize() {
        if (!ModConfig.COMMON.eiraEnabled.get()) {
            StorytellerMod.LOGGER.info("Eira Relay integration disabled in config");
            return;
        }
        
        int timeout = ModConfig.COMMON.webhookTimeout.get();
        
        this.webhookClient = new OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.MILLISECONDS)
            .readTimeout(timeout, TimeUnit.MILLISECONDS)
            .writeTimeout(timeout, TimeUnit.MILLISECONDS)
            .build();
        
        initialized = true;
        StorytellerMod.LOGGER.info("Eira Relay integration initialized");
    }
    
    public boolean isEnabled() {
        return initialized && ModConfig.COMMON.eiraEnabled.get();
    }
    
    /**
     * Called every tick to check for redstone changes near NPCs
     */
    public void tick(ServerLevel level) {
        if (!isEnabled()) return;
        
        int radius = ModConfig.COMMON.redstoneDetectionRadius.get();
        
        // Find all Storyteller NPCs in the level
        level.getEntities().getAll().forEach(entity -> {
            if (entity instanceof StorytellerNPC npc) {
                checkRedstoneNearNPC(level, npc, radius);
                updateRedstoneEmission(level, npc);
            }
        });
    }
    
    /**
     * Check for redstone signal changes near an NPC
     */
    private void checkRedstoneNearNPC(ServerLevel level, StorytellerNPC npc, int radius) {
        UUID npcId = npc.getUUID();
        BlockPos npcPos = npc.blockPosition();
        
        Map<BlockPos, Boolean> previousStates = npcRedstoneStates.computeIfAbsent(
            npcId, k -> new ConcurrentHashMap<>()
        );
        
        // Check all blocks in radius for redstone power
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = npcPos.offset(x, y, z);
                    
                    // Get redstone power level at this position
                    int power = level.getBestNeighborSignal(checkPos);
                    boolean isPowered = power > 0;
                    
                    Boolean wasPowered = previousStates.get(checkPos);
                    
                    if (wasPowered == null) {
                        // First check, just record state
                        previousStates.put(checkPos, isPowered);
                    } else if (isPowered && !wasPowered) {
                        // Rising edge - redstone just turned on
                        onRedstoneActivated(npc, checkPos, power);
                        previousStates.put(checkPos, true);
                    } else if (!isPowered && wasPowered) {
                        // Falling edge - redstone just turned off
                        onRedstoneDeactivated(npc, checkPos);
                        previousStates.put(checkPos, false);
                    }
                }
            }
        }
    }
    
    /**
     * Handle redstone activation near NPC (from Eira Receiver or other source)
     */
    private void onRedstoneActivated(StorytellerNPC npc, BlockPos pos, int power) {
        UUID npcId = npc.getUUID();
        
        // Check cooldown
        long cooldown = ModConfig.COMMON.redstoneCooldown.get() * 50L; // ticks to ms
        Long lastEvent = eventCooldowns.get(npcId);
        if (lastEvent != null && System.currentTimeMillis() - lastEvent < cooldown) {
            return;
        }
        eventCooldowns.put(npcId, System.currentTimeMillis());
        
        StorytellerMod.LOGGER.debug("Redstone activated near NPC {} at {} (power: {})", 
            npc.getDisplayName(), pos, power);
        
        // Trigger NPC external event handler
        npc.onExternalEvent(new ExternalEvent(
            ExternalEvent.Type.REDSTONE_ON,
            Map.of(
                "position", pos.toShortString(),
                "power", power,
                "source", "eira_relay"
            )
        ));
    }
    
    /**
     * Handle redstone deactivation near NPC
     */
    private void onRedstoneDeactivated(StorytellerNPC npc, BlockPos pos) {
        StorytellerMod.LOGGER.debug("Redstone deactivated near NPC {} at {}", 
            npc.getDisplayName(), pos);
        
        npc.onExternalEvent(new ExternalEvent(
            ExternalEvent.Type.REDSTONE_OFF,
            Map.of("position", pos.toShortString())
        ));
    }
    
    /**
     * Make an NPC emit a redstone signal (to trigger Eira Sender blocks)
     */
    public void emitRedstone(StorytellerNPC npc, int strength, int durationTicks, String pattern) {
        if (!isEnabled() || !ModConfig.COMMON.emitRedstoneOnEvents.get()) return;
        
        UUID npcId = npc.getUUID();
        
        activeEmissions.put(npcId, new RedstoneEmission(
            strength,
            durationTicks,
            pattern,
            System.currentTimeMillis()
        ));
        
        StorytellerMod.LOGGER.debug("NPC {} emitting redstone: strength={}, duration={}, pattern={}", 
            npc.getDisplayName(), strength, durationTicks, pattern);
    }
    
    /**
     * Update active redstone emissions (handle duration and patterns)
     */
    private void updateRedstoneEmission(ServerLevel level, StorytellerNPC npc) {
        UUID npcId = npc.getUUID();
        RedstoneEmission emission = activeEmissions.get(npcId);
        
        if (emission == null) return;
        
        long elapsed = System.currentTimeMillis() - emission.startTime();
        int elapsedTicks = (int) (elapsed / 50);
        
        if (elapsedTicks >= emission.durationTicks()) {
            // Emission complete
            activeEmissions.remove(npcId);
            npc.setEmittingRedstone(false, 0);
            return;
        }
        
        // Calculate current strength based on pattern
        int currentStrength = calculatePatternStrength(emission, elapsedTicks);
        npc.setEmittingRedstone(currentStrength > 0, currentStrength);
    }
    
    private int calculatePatternStrength(RedstoneEmission emission, int elapsedTicks) {
        String pattern = emission.pattern();
        int baseStrength = emission.strength();
        
        return switch (pattern) {
            case "constant" -> baseStrength;
            case "fade" -> {
                float progress = (float) elapsedTicks / emission.durationTicks();
                yield (int) (baseStrength * (1 - progress));
            }
            case "pulse_3x" -> {
                int cycleLength = emission.durationTicks() / 3;
                int cyclePosition = elapsedTicks % cycleLength;
                yield cyclePosition < cycleLength / 2 ? baseStrength : 0;
            }
            case "sos" -> {
                // SOS pattern: ... --- ...
                int[] sosPattern = {1,0,1,0,1,0,0,1,1,0,1,1,0,1,1,0,0,1,0,1,0,1,0,0};
                int index = (elapsedTicks / 4) % sosPattern.length;
                yield sosPattern[index] == 1 ? baseStrength : 0;
            }
            default -> baseStrength;
        };
    }
    
    /**
     * Send webhook for NPC event
     */
    public CompletableFuture<Boolean> sendWebhook(StorytellerNPC npc, ServerPlayer player, 
                                                   NPCEvent event, Map<String, Object> data) {
        if (!isEnabled() || !ModConfig.COMMON.webhooksEnabled.get()) {
            return CompletableFuture.completedFuture(false);
        }
        
        String webhookUrl = getWebhookUrl(event);
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject payload = buildWebhookPayload(npc, player, event, data);
                
                RequestBody body = RequestBody.create(
                    GSON.toJson(payload),
                    MediaType.get("application/json")
                );
                
                Request request = new Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build();
                
                try (Response response = webhookClient.newCall(request).execute()) {
                    boolean success = response.isSuccessful();
                    if (!success) {
                        StorytellerMod.LOGGER.warn("Webhook failed: {} returned {}", 
                            webhookUrl, response.code());
                    }
                    return success;
                }
            } catch (IOException e) {
                StorytellerMod.LOGGER.error("Webhook error for {}: {}", webhookUrl, e.getMessage());
                return false;
            }
        });
    }
    
    private String getWebhookUrl(NPCEvent event) {
        return switch (event) {
            case CONVERSATION_STARTED -> ModConfig.COMMON.webhookConversationStart.get();
            case SECRET_REVEALED -> ModConfig.COMMON.webhookSecretRevealed.get();
            case QUEST_STARTED -> ModConfig.COMMON.webhookQuestStarted.get();
            case QUEST_COMPLETED -> ModConfig.COMMON.webhookQuestCompleted.get();
            case MOOD_CHANGED -> ModConfig.COMMON.webhookMoodChanged.get();
            case DANGER_WARNING -> ModConfig.COMMON.webhookDangerWarning.get();
        };
    }
    
    private JsonObject buildWebhookPayload(StorytellerNPC npc, ServerPlayer player,
                                           NPCEvent event, Map<String, Object> data) {
        JsonObject payload = new JsonObject();
        
        payload.addProperty("event", event.name().toLowerCase());
        
        // NPC info
        JsonObject npcInfo = new JsonObject();
        npcInfo.addProperty("id", npc.getUUID().toString());
        npcInfo.addProperty("name", npc.getNPCDisplayName());
        npcInfo.addProperty("character_id", npc.getCharacterId());
        payload.add("npc", npcInfo);
        
        // Player info
        if (player != null) {
            JsonObject playerInfo = new JsonObject();
            playerInfo.addProperty("name", player.getName().getString());
            playerInfo.addProperty("uuid", player.getUUID().toString());
            payload.add("player", playerInfo);
        }
        
        // Event data
        JsonObject eventData = new JsonObject();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof Number num) {
                eventData.addProperty(entry.getKey(), num);
            } else if (entry.getValue() instanceof Boolean bool) {
                eventData.addProperty(entry.getKey(), bool);
            } else {
                eventData.addProperty(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        payload.add("data", eventData);
        
        // World context
        if (npc.level() instanceof ServerLevel level) {
            JsonObject world = new JsonObject();
            world.addProperty("dimension", level.dimension().location().toString());
            world.addProperty("time", getTimeOfDay(level.getDayTime() % 24000));
            world.addProperty("weather", level.isThundering() ? "thunder" : 
                level.isRaining() ? "rain" : "clear");
            payload.add("world", world);
        }
        
        payload.addProperty("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        
        return payload;
    }
    
    private String getTimeOfDay(long dayTime) {
        if (dayTime < 6000) return "morning";
        if (dayTime < 12000) return "day";
        if (dayTime < 18000) return "evening";
        return "night";
    }
    
    public void shutdown() {
        if (webhookClient != null) {
            webhookClient.dispatcher().executorService().shutdown();
            webhookClient.connectionPool().evictAll();
        }
        npcRedstoneStates.clear();
        activeEmissions.clear();
        eventCooldowns.clear();
        initialized = false;
    }
    
    /**
     * Clear state for a specific NPC (when removed from world)
     */
    public void clearNPCState(UUID npcId) {
        npcRedstoneStates.remove(npcId);
        activeEmissions.remove(npcId);
        eventCooldowns.remove(npcId);
    }
    
    // Data classes
    
    public record ExternalEvent(Type type, Map<String, Object> data) {
        public enum Type {
            REDSTONE_ON,
            REDSTONE_OFF,
            REDSTONE_PULSE,
            HTTP_TRIGGER,
            CUSTOM
        }
    }
    
    public enum NPCEvent {
        CONVERSATION_STARTED,
        SECRET_REVEALED,
        QUEST_STARTED,
        QUEST_COMPLETED,
        MOOD_CHANGED,
        DANGER_WARNING
    }
    
    private record RedstoneEmission(
        int strength,
        int durationTicks,
        String pattern,
        long startTime
    ) {}
}
