package com.storyteller.client;

import com.storyteller.StorytellerMod;
import com.storyteller.entity.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = StorytellerMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.STORYTELLER_NPC.get(), StorytellerNPCRenderer::new);
        StorytellerMod.LOGGER.info("Registered Storyteller NPC renderer");
    }
    
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // Player model layer definitions are already registered by vanilla
        // We use those for our NPC
    }
}
