package com.storyteller.entity.goals;

import com.storyteller.entity.StorytellerNPC;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * AI goal that makes the NPC wander randomly within its anchor radius.
 */
public class WanderNearAnchorGoal extends Goal {

    private final StorytellerNPC npc;
    private static final double WANDER_PROBABILITY = 0.01D; // 1% chance per tick
    private static final double WANDER_SPEED = 0.5D;

    private Vec3 targetPos;

    public WanderNearAnchorGoal(StorytellerNPC npc) {
        this.npc = npc;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Don't activate if in conversation
        if (npc.isInConversation()) {
            return false;
        }

        // Low probability of activation
        if (npc.getRandom().nextDouble() >= WANDER_PROBABILITY) {
            return false;
        }

        BlockPos anchor = npc.getAnchorPosition();
        if (anchor == null) {
            return false;
        }

        // Find a random position within the anchor radius
        targetPos = findRandomPosNearAnchor(anchor);
        return targetPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (npc.isInConversation()) {
            return false;
        }
        return !npc.getNavigation().isDone();
    }

    @Override
    public void start() {
        if (targetPos != null) {
            npc.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, WANDER_SPEED);
        }
    }

    @Override
    public void stop() {
        targetPos = null;
    }

    private Vec3 findRandomPosNearAnchor(BlockPos anchor) {
        int radius = npc.getAnchorRadius();
        Level level = npc.level();

        // Try a few times to find a valid position
        for (int attempts = 0; attempts < 10; attempts++) {
            // Random offset within radius
            double angle = npc.getRandom().nextDouble() * Math.PI * 2;
            double distance = npc.getRandom().nextDouble() * radius * 0.8; // Stay a bit inside radius

            int targetX = anchor.getX() + (int)(Math.cos(angle) * distance);
            int targetZ = anchor.getZ() + (int)(Math.sin(angle) * distance);

            // Find ground level at this position
            BlockPos targetBase = new BlockPos(targetX, anchor.getY(), targetZ);

            // Search for solid ground
            for (int yOffset = 3; yOffset >= -3; yOffset--) {
                BlockPos checkPos = targetBase.offset(0, yOffset, 0);
                BlockPos below = checkPos.below();

                // Check if this is a valid standing position
                if (level.getBlockState(below).isSolid() &&
                    level.getBlockState(checkPos).isAir() &&
                    level.getBlockState(checkPos.above()).isAir()) {
                    return new Vec3(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5);
                }
            }
        }

        return null;
    }
}
