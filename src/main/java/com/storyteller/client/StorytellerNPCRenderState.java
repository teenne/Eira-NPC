package com.storyteller.client;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * Render state for Storyteller NPCs
 */
public class StorytellerNPCRenderState extends HumanoidRenderState {
    public String skinFile = "";
    public boolean slimModel = false;
    public boolean thinking = false;

    // Entity position for particle spawning
    public double entityX = 0;
    public double entityY = 0;
    public double entityZ = 0;
}
