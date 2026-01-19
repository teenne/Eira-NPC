package com.storyteller.client;

import com.storyteller.config.ModConfig;
import com.storyteller.network.NPCResponsePacket;
import com.storyteller.network.OpenChatScreenPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

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

            // Play chat response sound
            if (ModConfig.CLIENT.playChatSound.get()) {
                playChatSound();
            }
        }
    }

    /**
     * Play a subtle sound when NPC responds
     */
    private static void playChatSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            // Use a soft villager-like sound for NPC responses
            mc.player.playSound(
                SoundEvents.VILLAGER_YES,
                0.5f,  // Lower volume
                1.2f   // Slightly higher pitch for a gentler sound
            );
        }
    }

    public static void clearCurrentScreen() {
        currentChatScreen = null;
    }
}
