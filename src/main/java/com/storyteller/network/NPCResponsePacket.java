package com.storyteller.network;

import com.storyteller.client.ClientPacketHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from server to client with the NPC's response
 */
public record NPCResponsePacket(
    int entityId,
    String response
) implements CustomPacketPayload {
    
    public static final Type<NPCResponsePacket> TYPE = 
        new Type<>(ModNetwork.id("npc_response"));
    
    public static final StreamCodec<ByteBuf, NPCResponsePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, NPCResponsePacket::entityId,
        ByteBufCodecs.STRING_UTF8, NPCResponsePacket::response,
        NPCResponsePacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(NPCResponsePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientPacketHandler.handleNPCResponse(packet);
        });
    }
}
