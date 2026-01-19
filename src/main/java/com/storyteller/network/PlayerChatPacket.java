package com.storyteller.network;

import com.storyteller.StorytellerMod;
import com.storyteller.entity.StorytellerNPC;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from client to server when player sends a message to an NPC
 */
public record PlayerChatPacket(
    int entityId,
    String message
) implements CustomPacketPayload {
    
    public static final Type<PlayerChatPacket> TYPE = 
        new Type<>(ModNetwork.id("player_chat"));
    
    public static final StreamCodec<ByteBuf, PlayerChatPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, PlayerChatPacket::entityId,
        ByteBufCodecs.STRING_UTF8, PlayerChatPacket::message,
        PlayerChatPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(PlayerChatPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                // Validate message
                String message = packet.message();
                if (message == null || message.isBlank() || message.length() > 500) {
                    return;
                }
                
                // Find the NPC entity
                Entity entity = serverPlayer.level().getEntity(packet.entityId());
                if (entity instanceof StorytellerNPC npc) {
                    // Check distance (prevent chat from far away)
                    if (serverPlayer.distanceToSqr(npc) > 100) { // 10 blocks
                        return;
                    }
                    
                    StorytellerMod.LOGGER.debug("Player {} sent message to NPC {}: {}", 
                        serverPlayer.getName().getString(),
                        npc.getDisplayName(),
                        message.substring(0, Math.min(50, message.length()))
                    );
                    
                    npc.processPlayerMessage(serverPlayer, message);
                }
            }
        });
    }
}
