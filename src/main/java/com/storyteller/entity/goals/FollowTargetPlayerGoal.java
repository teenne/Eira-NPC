package com.storyteller.entity.goals;

import com.storyteller.entity.StorytellerNPC;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.UUID;

/**
 * AI goal that makes the NPC follow a specific player at a configured distance.
 */
public class FollowTargetPlayerGoal extends Goal {

    private final StorytellerNPC npc;
    private static final double FOLLOW_SPEED = 0.7D;
    private static final int PATH_RECALC_INTERVAL = 10; // Recalculate path every 10 ticks

    private Player targetPlayer;
    private int ticksUntilPathRecalc;

    public FollowTargetPlayerGoal(StorytellerNPC npc) {
        this.npc = npc;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Don't activate if in conversation
        if (npc.isInConversation()) {
            return false;
        }

        targetPlayer = findTargetPlayer();
        if (targetPlayer == null || !targetPlayer.isAlive()) {
            return false;
        }

        // Check if player is too far away (start following)
        double distanceSq = npc.distanceToSqr(targetPlayer);
        int followDist = npc.getFollowDistance();

        // Start following if player is more than 1.5x the follow distance away
        return distanceSq > (followDist * followDist * 2.25);
    }

    @Override
    public boolean canContinueToUse() {
        if (npc.isInConversation()) {
            return false;
        }

        if (targetPlayer == null || !targetPlayer.isAlive()) {
            return false;
        }

        // Continue following until we're at the desired distance
        double distanceSq = npc.distanceToSqr(targetPlayer);
        int followDist = npc.getFollowDistance();

        // Stop following when we're within the follow distance
        return distanceSq > (followDist * followDist);
    }

    @Override
    public void start() {
        ticksUntilPathRecalc = 0;
    }

    @Override
    public void stop() {
        targetPlayer = null;
        npc.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetPlayer == null) {
            return;
        }

        // Look at the player
        npc.getLookControl().setLookAt(targetPlayer, 10.0F, npc.getMaxHeadXRot());

        // Recalculate path periodically
        if (--ticksUntilPathRecalc <= 0) {
            ticksUntilPathRecalc = PATH_RECALC_INTERVAL;

            double distanceSq = npc.distanceToSqr(targetPlayer);
            int followDist = npc.getFollowDistance();

            // Only move if we're too far
            if (distanceSq > (followDist * followDist)) {
                npc.getNavigation().moveTo(targetPlayer, FOLLOW_SPEED);
            }
        }
    }

    private Player findTargetPlayer() {
        if (!(npc.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        // First try the explicitly set target player
        UUID targetUUID = npc.getTargetPlayerUUID();
        if (targetUUID != null) {
            Player player = serverLevel.getServer().getPlayerList().getPlayer(targetUUID);
            if (player != null && player.isAlive()) {
                return player;
            }
        }

        // Fall back to the player who spawned this NPC
        UUID spawnerUUID = npc.getSpawnerPlayerUUID();
        if (spawnerUUID != null) {
            Player player = serverLevel.getServer().getPlayerList().getPlayer(spawnerUUID);
            if (player != null && player.isAlive()) {
                return player;
            }
        }

        // No valid target
        return null;
    }
}
