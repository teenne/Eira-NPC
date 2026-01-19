package com.storyteller.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.List;

/**
 * Captures world context to make NPC responses world-aware
 */
public class WorldContext {
    
    private String biome;
    private String timeOfDay;
    private String weather;
    private String dimension;
    private List<String> nearbyStructures;
    private String playerName;
    private int playerHealth;
    private int playerHunger;
    private boolean playerIsUnderground;
    private int lightLevel;
    
    private WorldContext() {
        this.nearbyStructures = new ArrayList<>();
    }
    
    /**
     * Build context from the current game state
     */
    public static WorldContext build(ServerLevel level, ServerPlayer player, Entity npc) {
        WorldContext ctx = new WorldContext();
        
        BlockPos npcPos = npc.blockPosition();
        
        // Biome
        Holder<Biome> biomeHolder = level.getBiome(npcPos);
        ctx.biome = formatBiomeName(biomeHolder.unwrapKey()
            .map(key -> key.location().getPath())
            .orElse("unknown"));
        
        // Time of day
        long dayTime = level.getDayTime() % 24000;
        ctx.timeOfDay = getTimeOfDay(dayTime);
        
        // Weather
        if (level.isThundering()) {
            ctx.weather = "thunderstorm";
        } else if (level.isRaining()) {
            ctx.weather = "rain";
        } else {
            ctx.weather = "clear";
        }
        
        // Dimension
        ctx.dimension = formatDimensionName(level.dimension().location().getPath());
        
        // Nearby structures (within 100 blocks)
        ctx.nearbyStructures = findNearbyStructures(level, npcPos, 100);
        
        // Player info
        ctx.playerName = player.getName().getString();
        ctx.playerHealth = (int) player.getHealth();
        ctx.playerHunger = player.getFoodData().getFoodLevel();
        
        // Underground check
        ctx.playerIsUnderground = player.blockPosition().getY() < level.getSeaLevel() - 10 
            && !level.canSeeSky(player.blockPosition());
        
        // Light level at NPC
        ctx.lightLevel = level.getMaxLocalRawBrightness(npcPos);
        
        return ctx;
    }
    
    private static String getTimeOfDay(long dayTime) {
        if (dayTime < 1000) return "dawn";
        if (dayTime < 6000) return "morning";
        if (dayTime < 12000) return "midday";
        if (dayTime < 13000) return "afternoon";
        if (dayTime < 14000) return "dusk";
        if (dayTime < 18000) return "evening";
        if (dayTime < 22000) return "night";
        return "late night";
    }
    
    private static String formatBiomeName(String biomeId) {
        return biomeId.replace("_", " ");
    }
    
    private static String formatDimensionName(String dimId) {
        return switch (dimId) {
            case "overworld" -> "the Overworld";
            case "the_nether" -> "the Nether";
            case "the_end" -> "the End";
            default -> dimId.replace("_", " ");
        };
    }
    
    private static List<String> findNearbyStructures(ServerLevel level, BlockPos pos, int radius) {
        List<String> structures = new ArrayList<>();
        
        // Check for common structures
        // Note: This is a simplified check - full structure detection is complex
        var structureManager = level.structureManager();
        
        // The actual structure detection would require iterating through
        // registered structures, which is complex. For now, we'll use
        // a simplified approach that checks chunk features.
        
        // This is a placeholder - in a full implementation you'd want to
        // properly query the structure manager
        
        return structures;
    }
    
    /**
     * Convert to a string for inclusion in the LLM prompt
     */
    public String toPromptString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("- Location: ").append(biome).append(" biome in ").append(dimension).append("\n");
        sb.append("- Time: ").append(timeOfDay).append("\n");
        sb.append("- Weather: ").append(weather).append("\n");
        
        if (lightLevel < 7) {
            sb.append("- It is quite dark here\n");
        }
        
        sb.append("- Speaking with: ").append(playerName).append("\n");
        
        // Health commentary
        if (playerHealth <= 6) {
            sb.append("- The player looks badly wounded\n");
        } else if (playerHealth <= 12) {
            sb.append("- The player appears somewhat injured\n");
        }
        
        // Hunger commentary
        if (playerHunger <= 6) {
            sb.append("- The player looks famished\n");
        }
        
        if (playerIsUnderground) {
            sb.append("- You are deep underground\n");
        }
        
        if (!nearbyStructures.isEmpty()) {
            sb.append("- Nearby: ").append(String.join(", ", nearbyStructures)).append("\n");
        }
        
        return sb.toString();
    }
    
    // Getters for potential use elsewhere
    
    public String getBiome() { return biome; }
    public String getTimeOfDay() { return timeOfDay; }
    public String getWeather() { return weather; }
    public String getDimension() { return dimension; }
    public String getPlayerName() { return playerName; }
    public int getPlayerHealth() { return playerHealth; }
    public boolean isPlayerUnderground() { return playerIsUnderground; }
}
