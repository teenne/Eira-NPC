package com.storyteller.client;

import com.storyteller.StorytellerMod;
import com.storyteller.network.PlayerChatPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The chat screen for interacting with Storyteller NPCs
 */
public class NPCChatScreen extends Screen {
    
    private final int entityId;
    private final UUID npcUuid;
    private final String npcName;
    private final String skinFile;
    private final boolean slimModel;
    
    private EditBox inputBox;
    private Button sendButton;
    
    private final List<ChatEntry> chatHistory = new ArrayList<>();
    private boolean awaitingResponse = false;
    private int scrollOffset = 0;
    
    // Layout constants
    private static final int PADDING = 10;
    private static final int INPUT_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 60;
    private static final int CHAT_AREA_TOP = 40;
    
    public NPCChatScreen(int entityId, UUID npcUuid, String npcName, String skinFile, boolean slimModel) {
        super(Component.literal("Chat with " + npcName));
        this.entityId = entityId;
        this.npcUuid = npcUuid;
        this.npcName = npcName;
        this.skinFile = skinFile;
        this.slimModel = slimModel;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int chatWidth = Math.min(400, this.width - 40);
        int centerX = this.width / 2;
        int inputY = this.height - PADDING - INPUT_HEIGHT;
        
        // Input box
        int inputWidth = chatWidth - BUTTON_WIDTH - 5;
        this.inputBox = new EditBox(
            this.font,
            centerX - chatWidth / 2,
            inputY,
            inputWidth,
            INPUT_HEIGHT,
            Component.literal("Type a message...")
        );
        this.inputBox.setMaxLength(500);
        this.inputBox.setFocused(true);
        this.addRenderableWidget(inputBox);
        
        // Send button
        this.sendButton = Button.builder(Component.literal("Send"), this::onSend)
            .bounds(centerX - chatWidth / 2 + inputWidth + 5, inputY, BUTTON_WIDTH, INPUT_HEIGHT)
            .build();
        this.addRenderableWidget(sendButton);
        
        // Initial greeting if no history
        if (chatHistory.isEmpty()) {
            chatHistory.add(new ChatEntry(npcName, "Ah, a visitor! How may I help you today?", false));
        }
    }
    
    private void onSend(Button button) {
        sendMessage();
    }
    
    private void sendMessage() {
        String message = inputBox.getValue().trim();
        if (message.isEmpty() || awaitingResponse) {
            return;
        }
        
        // Add to local history
        chatHistory.add(new ChatEntry("You", message, true));
        
        // Send to server
        PacketDistributor.sendToServer(new PlayerChatPacket(entityId, message));
        
        // Clear input and show waiting
        inputBox.setValue("");
        awaitingResponse = true;
        chatHistory.add(new ChatEntry(npcName, "...", false));
        
        // Scroll to bottom
        scrollToBottom();
    }
    
    public void receiveResponse(String response) {
        // Remove the "..." placeholder
        if (!chatHistory.isEmpty() && chatHistory.get(chatHistory.size() - 1).message().equals("...")) {
            chatHistory.remove(chatHistory.size() - 1);
        }
        
        // Add the actual response
        chatHistory.add(new ChatEntry(npcName, response, false));
        awaitingResponse = false;
        
        // Scroll to bottom
        scrollToBottom();
    }
    
    private void scrollToBottom() {
        // Will be calculated during render
        scrollOffset = Integer.MAX_VALUE;
    }
    
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Use transparent background (no blur) instead of default
        this.renderTransparentBackground(graphics);
        // Then draw our semi-transparent overlay
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render our custom background (calls override above)
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        int chatWidth = Math.min(400, this.width - 40);
        int centerX = this.width / 2;
        int chatLeft = centerX - chatWidth / 2;
        int chatRight = centerX + chatWidth / 2;
        int chatBottom = this.height - PADDING - INPUT_HEIGHT - 10;
        
        // Draw chat area background
        graphics.fill(chatLeft - 5, CHAT_AREA_TOP - 5, chatRight + 5, chatBottom + 5, 0x80000000);
        
        // Draw title
        graphics.drawCenteredString(this.font, this.title, centerX, PADDING, 0xFFFFFF);
        
        // Draw NPC name with title styling
        String displayTitle = "§6" + npcName;
        graphics.drawCenteredString(this.font, displayTitle, centerX, PADDING + 15, 0xFFD700);
        
        // Render chat messages
        renderChatHistory(graphics, chatLeft, CHAT_AREA_TOP, chatWidth, chatBottom - CHAT_AREA_TOP);
        
        // Render widgets (input box, button)
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // Show "thinking" indicator
        if (awaitingResponse) {
            String thinking = npcName + " is thinking...";
            int thinkingWidth = this.font.width(thinking);
            graphics.drawString(this.font, thinking, centerX - thinkingWidth / 2, chatBottom + 2, 0xAAAAAA);
        }
    }
    
    private void renderChatHistory(GuiGraphics graphics, int x, int y, int width, int height) {
        // Calculate total height needed
        int totalHeight = 0;
        List<RenderedMessage> rendered = new ArrayList<>();
        
        for (ChatEntry entry : chatHistory) {
            String prefix = (entry.isPlayer() ? "§b" : "§a") + entry.sender() + ": §r";
            String fullMessage = prefix + entry.message();
            
            List<String> lines = wrapText(fullMessage, width - 10);
            int messageHeight = lines.size() * (font.lineHeight + 2) + 5;
            
            rendered.add(new RenderedMessage(entry, lines, messageHeight));
            totalHeight += messageHeight;
        }
        
        // Clamp scroll offset
        int maxScroll = Math.max(0, totalHeight - height);
        if (scrollOffset == Integer.MAX_VALUE) {
            scrollOffset = maxScroll;
        }
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        
        // Enable scissor to clip chat area
        graphics.enableScissor(x, y, x + width, y + height);
        
        // Render messages
        int currentY = y - scrollOffset;
        for (RenderedMessage msg : rendered) {
            if (currentY + msg.height() > y && currentY < y + height) {
                // Message is at least partially visible
                int lineY = currentY;
                for (String line : msg.lines()) {
                    if (lineY >= y - font.lineHeight && lineY < y + height) {
                        graphics.drawString(this.font, line, x + 5, lineY, 0xFFFFFF);
                    }
                    lineY += font.lineHeight + 2;
                }
            }
            currentY += msg.height();
        }
        
        graphics.disableScissor();
        
        // Draw scroll indicators if needed
        if (scrollOffset > 0) {
            graphics.drawCenteredString(font, "▲", x + width / 2, y + 2, 0x888888);
        }
        if (scrollOffset < maxScroll) {
            graphics.drawCenteredString(font, "▼", x + width / 2, y + height - font.lineHeight - 2, 0x888888);
        }
    }
    
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (currentLine.isEmpty()) {
                currentLine.append(word);
            } else {
                String test = currentLine + " " + word;
                if (font.width(test) <= maxWidth) {
                    currentLine.append(" ").append(word);
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                }
            }
        }
        
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        
        return lines.isEmpty() ? List.of("") : lines;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= (int)(scrollY * 15);
        return true;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter to send
        if (keyCode == 257 || keyCode == 335) { // Enter or numpad enter
            sendMessage();
            return true;
        }
        
        // Escape to close
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public void onClose() {
        ClientPacketHandler.clearCurrentScreen();
        super.onClose();
    }
    
    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause in singleplayer
    }
    
    public int getEntityId() {
        return entityId;
    }
    
    // Helper records
    private record ChatEntry(String sender, String message, boolean isPlayer) {}
    private record RenderedMessage(ChatEntry entry, List<String> lines, int height) {}
}
