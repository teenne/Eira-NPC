package com.storyteller.network;

import com.storyteller.StorytellerMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = StorytellerMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetwork {
    
    public static final String PROTOCOL_VERSION = "1";
    
    public static void register() {
        // Registration happens via event
    }
    
    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(StorytellerMod.MOD_ID)
            .versioned(PROTOCOL_VERSION);
        
        // Server -> Client: Open chat screen
        registrar.playToClient(
            OpenChatScreenPacket.TYPE,
            OpenChatScreenPacket.STREAM_CODEC,
            OpenChatScreenPacket::handle
        );
        
        // Server -> Client: NPC response
        registrar.playToClient(
            NPCResponsePacket.TYPE,
            NPCResponsePacket.STREAM_CODEC,
            NPCResponsePacket::handle
        );
        
        // Client -> Server: Player message to NPC
        registrar.playToServer(
            PlayerChatPacket.TYPE,
            PlayerChatPacket.STREAM_CODEC,
            PlayerChatPacket::handle
        );
        
        StorytellerMod.LOGGER.info("Registered network payloads");
    }
    
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(StorytellerMod.MOD_ID, path);
    }
}
