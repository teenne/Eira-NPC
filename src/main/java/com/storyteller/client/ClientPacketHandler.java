package com.storyteller.client;

import com.storyteller.network.NPCResponsePacket;
import com.storyteller.network.OpenChatScreenPacket;
import net.minecraft.client.Minecraft;

/**
 * Client-side packet handlers
 */
public class ClientPacketHandler {
    
    private static NPCChatScreen currentChatScreen = null;
    
    public static void handleOpenChatScreen(OpenChatScreenPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        
        currentChatScreen = new NPCChatScreen(
            packet.entityId(),
            packet.npcUuid(),
            packet.npcName(),
            packet.skinFile(),
            packet.slimModel()
        );
        
        mc.setScreen(currentChatScreen);
    }
    
    public static void handleNPCResponse(NPCResponsePacket packet) {
        if (currentChatScreen != null && currentChatScreen.getEntityId() == packet.entityId()) {
            currentChatScreen.receiveResponse(packet.response());
        }
    }
    
    public static void clearCurrentScreen() {
        currentChatScreen = null;
    }
}
