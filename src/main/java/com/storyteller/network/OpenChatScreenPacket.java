package com.storyteller.network;

import com.storyteller.StorytellerMod;
import com.storyteller.client.ClientPacketHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Sent from server to client to open the NPC chat screen
 */
public record OpenChatScreenPacket(
    int entityId,
    UUID npcUuid,
    String npcName,
    String skinFile,
    boolean slimModel
) implements CustomPacketPayload {
    
    public static final Type<OpenChatScreenPacket> TYPE = 
        new Type<>(ModNetwork.id("open_chat_screen"));
    
    public static final StreamCodec<ByteBuf, OpenChatScreenPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, OpenChatScreenPacket::entityId,
        ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString), OpenChatScreenPacket::npcUuid,
        ByteBufCodecs.STRING_UTF8, OpenChatScreenPacket::npcName,
        ByteBufCodecs.STRING_UTF8, OpenChatScreenPacket::skinFile,
        ByteBufCodecs.BOOL, OpenChatScreenPacket::slimModel,
        OpenChatScreenPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(OpenChatScreenPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Handle on client
            ClientPacketHandler.handleOpenChatScreen(packet);
        });
    }
}
