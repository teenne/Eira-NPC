package com.storyteller.entity.goals;

import com.storyteller.entity.StorytellerNPC;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * AI goal that makes the NPC return to its anchor position when outside the configured radius.
 */
public class ReturnToAnchorGoal extends Goal {

    private final StorytellerNPC npc;
    private static final double RETURN_SPEED = 0.8D;

    public ReturnToAnchorGoal(StorytellerNPC npc) {
        this.npc = npc;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Don't activate if in conversation
        if (npc.isInConversation()) {
            return false;
        }

        BlockPos anchor = npc.getAnchorPosition();
        if (anchor == null) {
            return false;
        }

        // Check if we're outside the anchor radius
        double distanceSq = npc.blockPosition().distSqr(anchor);
        int radius = npc.getAnchorRadius();
        return distanceSq > (radius * radius);
    }

    @Override
    public boolean canContinueToUse() {
        if (npc.isInConversation()) {
            return false;
        }

        BlockPos anchor = npc.getAnchorPosition();
        if (anchor == null) {
            return false;
        }

        // Continue until we're back within radius (with some buffer)
        double distanceSq = npc.blockPosition().distSqr(anchor);
        int radius = npc.getAnchorRadius();
        // Stop a bit before the edge to avoid jitter
        return distanceSq > (radius * radius * 0.7) && !npc.getNavigation().isDone();
    }

    @Override
    public void start() {
        BlockPos anchor = npc.getAnchorPosition();
        if (anchor != null) {
            // Move toward the anchor position
            npc.getNavigation().moveTo(
                anchor.getX() + 0.5,
                anchor.getY(),
                anchor.getZ() + 0.5,
                RETURN_SPEED
            );
        }
    }

    @Override
    public void stop() {
        npc.getNavigation().stop();
    }

    @Override
    public void tick() {
        // Recalculate path periodically if navigation gets stuck
        if (npc.getNavigation().isDone()) {
            BlockPos anchor = npc.getAnchorPosition();
            if (anchor != null) {
                npc.getNavigation().moveTo(
                    anchor.getX() + 0.5,
                    anchor.getY(),
                    anchor.getZ() + 0.5,
                    RETURN_SPEED
                );
            }
        }
    }
}
