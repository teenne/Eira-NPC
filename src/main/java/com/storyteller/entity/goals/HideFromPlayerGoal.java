package com.storyteller.entity.goals;

import com.storyteller.config.ModConfig;
import com.storyteller.entity.StorytellerNPC;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * AI goal that makes the NPC hide from players using line-of-sight checks.
 * When a player can see the NPC, it flees to a position behind cover.
 */
public class HideFromPlayerGoal extends Goal {

    private final StorytellerNPC npc;
    private static final double FLEE_SPEED = 0.9D;
    private static final double DETECTION_RANGE = 32.0D;

    private Player visiblePlayer;
    private Vec3 hidePosition;
    private int ticksSinceLastCheck;
    private int ticksHiding;
    private static final int MAX_HIDING_TICKS = 200; // Stop hiding after 10 seconds if stuck

    public HideFromPlayerGoal(StorytellerNPC npc) {
        this.npc = npc;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Don't activate if in conversation
        if (npc.isInConversation()) {
            return false;
        }

        // Check line-of-sight periodically
        int checkInterval = ModConfig.COMMON.hidingCheckInterval.get();
        if (++ticksSinceLastCheck < checkInterval) {
            return false;
        }
        ticksSinceLastCheck = 0;

        // Find a player who can see us
        visiblePlayer = findPlayerWithLineOfSight();
        if (visiblePlayer == null) {
            return false;
        }

        // Find a hiding spot
        hidePosition = findHidingPosition(visiblePlayer);
        return hidePosition != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (npc.isInConversation()) {
            return false;
        }

        // Stop if we've been hiding too long (might be stuck)
        if (++ticksHiding > MAX_HIDING_TICKS) {
            return false;
        }

        // Continue until we reach the hiding spot
        if (!npc.getNavigation().isDone()) {
            return true;
        }

        // We've arrived - check if we're now hidden
        if (visiblePlayer != null && visiblePlayer.isAlive()) {
            // If player can still see us, try to find a new spot
            if (canPlayerSeeNPC(visiblePlayer)) {
                hidePosition = findHidingPosition(visiblePlayer);
                if (hidePosition != null) {
                    npc.getNavigation().moveTo(hidePosition.x, hidePosition.y, hidePosition.z, FLEE_SPEED);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void start() {
        ticksHiding = 0;
        if (hidePosition != null) {
            npc.getNavigation().moveTo(hidePosition.x, hidePosition.y, hidePosition.z, FLEE_SPEED);
        }
    }

    @Override
    public void stop() {
        visiblePlayer = null;
        hidePosition = null;
        ticksHiding = 0;
        npc.getNavigation().stop();
    }

    /**
     * Find a player within detection range who has line-of-sight to this NPC.
     */
    private Player findPlayerWithLineOfSight() {
        if (!(npc.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        List<Player> nearbyPlayers = serverLevel.getEntitiesOfClass(
            Player.class,
            npc.getBoundingBox().inflate(DETECTION_RANGE),
            player -> player.isAlive() && !player.isSpectator()
        );

        for (Player player : nearbyPlayers) {
            if (canPlayerSeeNPC(player)) {
                return player;
            }
        }

        return null;
    }

    /**
     * Check if a player has unobstructed line-of-sight to the NPC.
     */
    private boolean canPlayerSeeNPC(Player player) {
        Level level = npc.level();

        // Ray from player's eyes to NPC's center
        Vec3 playerEyes = player.getEyePosition();
        Vec3 npcCenter = npc.position().add(0, npc.getBbHeight() * 0.5, 0);

        // Check if player is looking roughly toward the NPC (within 90 degree cone)
        Vec3 toNPC = npcCenter.subtract(playerEyes).normalize();
        Vec3 playerLook = player.getLookAngle();
        double dot = toNPC.dot(playerLook);

        // Player needs to be looking somewhat toward the NPC (dot > 0 means less than 90 degrees)
        if (dot < 0.3) { // About 72 degree cone
            return false;
        }

        // Perform block raycast
        ClipContext context = new ClipContext(
            playerEyes,
            npcCenter,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        );

        BlockHitResult result = level.clip(context);

        // If we hit nothing or hit something beyond the NPC, player can see us
        if (result.getType() == HitResult.Type.MISS) {
            return true;
        }

        // Check if the hit point is beyond the NPC
        double distToHit = playerEyes.distanceTo(result.getLocation());
        double distToNPC = playerEyes.distanceTo(npcCenter);

        return distToHit >= distToNPC * 0.9; // Some tolerance
    }

    /**
     * Find a position behind cover, away from the player.
     */
    private Vec3 findHidingPosition(Player player) {
        Level level = npc.level();
        int fleeDistance = ModConfig.COMMON.hidingFleeDistance.get();

        Vec3 npcPos = npc.position();
        Vec3 playerPos = player.position();

        // Direction away from player
        Vec3 awayDir = npcPos.subtract(playerPos).normalize();

        // Try several positions in a cone away from the player
        for (int attempt = 0; attempt < 15; attempt++) {
            // Add some random spread to the direction
            double angleOffset = (npc.getRandom().nextDouble() - 0.5) * Math.PI * 0.5; // +/- 45 degrees
            double distance = fleeDistance * (0.5 + npc.getRandom().nextDouble() * 0.5);

            double angle = Math.atan2(awayDir.z, awayDir.x) + angleOffset;
            double targetX = npcPos.x + Math.cos(angle) * distance;
            double targetZ = npcPos.z + Math.sin(angle) * distance;

            // Find ground level
            BlockPos targetBase = new BlockPos((int) targetX, (int) npcPos.y, (int) targetZ);

            for (int yOffset = 3; yOffset >= -3; yOffset--) {
                BlockPos checkPos = targetBase.offset(0, yOffset, 0);
                BlockPos below = checkPos.below();

                // Check if valid standing position
                if (level.getBlockState(below).isSolid() &&
                    level.getBlockState(checkPos).isAir() &&
                    level.getBlockState(checkPos.above()).isAir()) {

                    Vec3 candidatePos = new Vec3(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5);

                    // Verify this position is actually hidden from the player
                    if (isPositionHiddenFrom(candidatePos, player)) {
                        return candidatePos;
                    }
                }
            }
        }

        // Fallback: just try to get distance even if not perfectly hidden
        for (int attempt = 0; attempt < 5; attempt++) {
            double angle = npc.getRandom().nextDouble() * Math.PI * 2;
            double distance = fleeDistance * 0.7;

            double targetX = npcPos.x + Math.cos(angle) * distance;
            double targetZ = npcPos.z + Math.sin(angle) * distance;

            BlockPos targetBase = new BlockPos((int) targetX, (int) npcPos.y, (int) targetZ);

            for (int yOffset = 2; yOffset >= -2; yOffset--) {
                BlockPos checkPos = targetBase.offset(0, yOffset, 0);
                BlockPos below = checkPos.below();

                if (level.getBlockState(below).isSolid() &&
                    level.getBlockState(checkPos).isAir() &&
                    level.getBlockState(checkPos.above()).isAir()) {

                    return new Vec3(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5);
                }
            }
        }

        return null;
    }

    /**
     * Check if a position would be hidden from the player's view.
     */
    private boolean isPositionHiddenFrom(Vec3 position, Player player) {
        Level level = npc.level();

        Vec3 playerEyes = player.getEyePosition();
        Vec3 targetCenter = position.add(0, npc.getBbHeight() * 0.5, 0);

        ClipContext context = new ClipContext(
            playerEyes,
            targetCenter,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        );

        BlockHitResult result = level.clip(context);

        // Position is hidden if raycast hits a block before reaching it
        if (result.getType() == HitResult.Type.BLOCK) {
            double distToHit = playerEyes.distanceTo(result.getLocation());
            double distToTarget = playerEyes.distanceTo(targetCenter);
            return distToHit < distToTarget * 0.9;
        }

        return false;
    }
}
