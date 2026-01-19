package com.storyteller.entity;

import com.storyteller.StorytellerMod;
import com.storyteller.config.ModConfig;
import com.storyteller.integration.EiraIntegrationManager;
import com.storyteller.integration.EiraIntegrationManager.ExternalEvent;
import com.storyteller.integration.EiraIntegrationManager.NPCEvent;
import com.storyteller.llm.LLMProvider.ChatMessage;
import com.storyteller.network.ModNetwork;
import com.storyteller.network.OpenChatScreenPacket;
import com.storyteller.network.NPCResponsePacket;
import com.storyteller.npc.ConversationHistory;
import com.storyteller.npc.NPCCharacter;
import com.storyteller.npc.WorldContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The Storyteller NPC entity - an AI-powered character
 */
public class StorytellerNPC extends PathfinderMob {
    
    // Synced data
    private static final EntityDataAccessor<String> DATA_CHARACTER_ID = SynchedEntityData.defineId(
        StorytellerNPC.class, EntityDataSerializers.STRING
    );
    private static final EntityDataAccessor<String> DATA_DISPLAY_NAME = SynchedEntityData.defineId(
        StorytellerNPC.class, EntityDataSerializers.STRING
    );
    private static final EntityDataAccessor<Boolean> DATA_IS_THINKING = SynchedEntityData.defineId(
        StorytellerNPC.class, EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<String> DATA_SKIN_FILE = SynchedEntityData.defineId(
        StorytellerNPC.class, EntityDataSerializers.STRING
    );
    private static final EntityDataAccessor<Boolean> DATA_SLIM_MODEL = SynchedEntityData.defineId(
        StorytellerNPC.class, EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Boolean> DATA_IN_CONVERSATION = SynchedEntityData.defineId(
        StorytellerNPC.class, EntityDataSerializers.BOOLEAN
    );

    // Server-side only
    private NPCCharacter character;
    private final AtomicBoolean processingRequest = new AtomicBoolean(false);
    private UUID currentlyTalkingTo = null;
    private long thinkingStartTime = 0;
    private long conversationStartTime = 0;
    private static final long CONVERSATION_TIMEOUT_MS = 60000; // 60 seconds without interaction ends conversation
    
    // Eira integration state
    private boolean emittingRedstone = false;
    private int redstoneStrength = 0;
    private String pendingExternalContext = null;
    
    public StorytellerNPC(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 100.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.25D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHARACTER_ID, "");
        builder.define(DATA_DISPLAY_NAME, "Storyteller");
        builder.define(DATA_IS_THINKING, false);
        builder.define(DATA_SKIN_FILE, "");
        builder.define(DATA_SLIM_MODEL, false);
        builder.define(DATA_IN_CONVERSATION, false);
    }
    
    @Override
    protected void registerGoals() {
        // Basic AI goals - NPCs should be mostly stationary but can wander a bit
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6D, 0.001F)); // Very rare wandering
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        // Skip AI goal processing when in conversation - prevents wandering
        if (isInConversation()) {
            return;
        }
        super.customServerAiStep(level);
    }
    
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        
        ServerPlayer serverPlayer = (ServerPlayer) player;
        
        // Check rate limiting
        if (!ConversationHistory.canInteract(this.getUUID(), player.getUUID())) {
            long waitTime = ConversationHistory.getTimeUntilCanInteract(this.getUUID(), player.getUUID());
            serverPlayer.sendSystemMessage(Component.literal(
                getNPCDisplayName() + " holds up a hand. \"A moment, please...\""
            ));
            return InteractionResult.CONSUME;
        }
        
        // Open the chat screen on client
        PacketDistributor.sendToPlayer(serverPlayer, new OpenChatScreenPacket(
            this.getId(),
            this.getUUID(),
            getNPCDisplayName(),
            getSkinFile(),
            isSlimModel()
        ));

        // Start conversation - NPC stops moving and looks at player
        setInConversation(true);
        currentlyTalkingTo = player.getUUID();
        conversationStartTime = System.currentTimeMillis();
        this.getNavigation().stop();
        this.getLookControl().setLookAt(player, 30.0F, 30.0F);

        return InteractionResult.CONSUME;
    }
    
    /**
     * Process a chat message from a player (called from network handler)
     */
    public void processPlayerMessage(ServerPlayer player, String message) {
        if (processingRequest.get()) {
            player.sendSystemMessage(Component.literal(
                "[" + getNPCDisplayName() + " is still thinking...]"
            ));
            return;
        }
        
        if (!ConversationHistory.canInteract(this.getUUID(), player.getUUID())) {
            return;
        }
        
        processingRequest.set(true);
        currentlyTalkingTo = player.getUUID();
        conversationStartTime = System.currentTimeMillis(); // Refresh conversation timeout
        setInConversation(true);
        setThinking(true);
        thinkingStartTime = System.currentTimeMillis();
        this.getNavigation().stop(); // Stop any movement
        
        // Build context
        NPCCharacter npcChar = getCharacter();
        WorldContext worldContext = null;
        
        if (ModConfig.COMMON.includeWorldContext.get() && level() instanceof ServerLevel serverLevel) {
            worldContext = WorldContext.build(serverLevel, player, this);
        }
        
        // Get conversation history
        List<ChatMessage> history = new ArrayList<>(
            ConversationHistory.getHistory(this.getUUID(), player.getUUID())
        );
        
        // Add the new message
        ChatMessage userMessage = new ChatMessage(ChatMessage.Role.USER, message);
        history.add(userMessage);
        
        // Build system prompt
        String systemPrompt = npcChar.generateSystemPrompt(worldContext);
        
        // Add conversation summary if there's history
        int convCount = ConversationHistory.getConversationCount(this.getUUID(), player.getUUID());
        if (convCount > 0) {
            systemPrompt += "\n\n## Conversation Context\n" + 
                ConversationHistory.buildConversationSummary(this.getUUID(), player.getUUID());
        }
        
        // Send to LLM
        StorytellerMod.getInstance().getLLMManager()
            .chat(systemPrompt, history)
            .thenAccept(response -> {
                // Back on server, send response to player
                if (player.isAlive() && player.connection != null) {
                    // Save to history
                    ConversationHistory.addMessage(this.getUUID(), player.getUUID(), userMessage);
                    ConversationHistory.addMessage(this.getUUID(), player.getUUID(), 
                        new ChatMessage(ChatMessage.Role.ASSISTANT, response));
                    ConversationHistory.incrementConversationCount(this.getUUID(), player.getUUID());
                    
                    // Send to client
                    PacketDistributor.sendToPlayer(player, new NPCResponsePacket(
                        this.getId(),
                        response
                    ));
                }
                
                processingRequest.set(false);
                setThinking(false);
                currentlyTalkingTo = null;
            })
            .exceptionally(e -> {
                StorytellerMod.LOGGER.error("Error processing NPC chat: {}", e.getMessage());
                processingRequest.set(false);
                setThinking(false);
                currentlyTalkingTo = null;
                
                if (player.isAlive() && player.connection != null) {
                    player.sendSystemMessage(Component.literal(
                        "[" + getNPCDisplayName() + " seems distracted and doesn't respond...]"
                    ));
                }
                return null;
            });
    }
    
    @Override
    public void tick() {
        super.tick();

        // Server-side: manage conversation state
        if (!level().isClientSide()) {
            if (isInConversation()) {
                // Check for conversation timeout
                if (System.currentTimeMillis() - conversationStartTime > CONVERSATION_TIMEOUT_MS) {
                    endConversation();
                } else {
                    // Stop any movement while in conversation
                    this.getNavigation().stop();

                    // Keep looking at the player we're talking to
                    if (currentlyTalkingTo != null && level() instanceof ServerLevel serverLevel) {
                        ServerPlayer player = serverLevel.getServer().getPlayerList()
                            .getPlayer(currentlyTalkingTo);
                        if (player != null && player.distanceToSqr(this) < 100) {
                            this.getLookControl().setLookAt(player, 30.0F, 30.0F);
                        } else {
                            // Player moved away or disconnected
                            endConversation();
                        }
                    }
                }
            }
        }

        // Show thinking particles after delay
        if (isThinking() && level().isClientSide()) {
            if (random.nextFloat() < 0.1) {
                // Particles handled in client renderer
            }
        }
    }

    /**
     * End the current conversation, allowing the NPC to move again
     */
    public void endConversation() {
        setInConversation(false);
        setThinking(false);
        currentlyTalkingTo = null;
        processingRequest.set(false);
    }
    
    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        // NPCs are invulnerable by default (can be changed in config if desired)
        return true;
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }
    
    @Override
    protected void pushEntities() {
        // Don't push other entities
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("CharacterId", getCharacterId());
        tag.putString("DisplayName", getNPCDisplayName());
        tag.putString("SkinFile", getSkinFile());
        tag.putBoolean("SlimModel", isSlimModel());
        
        if (character != null) {
            tag.put("Character", character.toNBT());
        }
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        
        if (tag.contains("CharacterId")) {
            setCharacterId(tag.getString("CharacterId"));
        }
        if (tag.contains("DisplayName")) {
            setDisplayName(tag.getString("DisplayName"));
        }
        if (tag.contains("SkinFile")) {
            setSkinFile(tag.getString("SkinFile"));
        }
        if (tag.contains("SlimModel")) {
            setSlimModel(tag.getBoolean("SlimModel"));
        }
        if (tag.contains("Character")) {
            this.character = NPCCharacter.fromNBT(tag.getCompound("Character"));
        }
    }
    
    // Character management
    
    public NPCCharacter getCharacter() {
        if (character == null) {
            // Try to load from manager
            String charId = getCharacterId();
            if (!charId.isEmpty()) {
                character = StorytellerMod.getInstance().getNPCManager()
                    .getCharacter(charId)
                    .orElseGet(NPCCharacter::createDefault);
            } else {
                character = StorytellerMod.getInstance().getNPCManager().getDefaultCharacter();
                setCharacterId(character.getId());
            }
            
            // Sync display data
            setDisplayName(character.getName());
            setSkinFile(character.getSkinFile());
            setSlimModel(character.isSlimModel());
        }
        return character;
    }
    
    public void setCharacter(NPCCharacter character) {
        this.character = character;
        setCharacterId(character.getId());
        setDisplayName(character.getName());
        setSkinFile(character.getSkinFile());
        setSlimModel(character.isSlimModel());
    }
    
    // Synced data accessors
    
    public String getCharacterId() {
        return this.entityData.get(DATA_CHARACTER_ID);
    }
    
    public void setCharacterId(String id) {
        this.entityData.set(DATA_CHARACTER_ID, id);
    }
    
    public String getNPCDisplayName() {
        return this.entityData.get(DATA_DISPLAY_NAME);
    }
    
    public void setDisplayName(String name) {
        this.entityData.set(DATA_DISPLAY_NAME, name);
        this.setCustomName(Component.literal(name));
        this.setCustomNameVisible(true);
    }
    
    public boolean isThinking() {
        return this.entityData.get(DATA_IS_THINKING);
    }
    
    public void setThinking(boolean thinking) {
        this.entityData.set(DATA_IS_THINKING, thinking);
    }
    
    public String getSkinFile() {
        return this.entityData.get(DATA_SKIN_FILE);
    }
    
    public void setSkinFile(String skinFile) {
        this.entityData.set(DATA_SKIN_FILE, skinFile != null ? skinFile : "");
    }
    
    public boolean isSlimModel() {
        return this.entityData.get(DATA_SLIM_MODEL);
    }
    
    public void setSlimModel(boolean slim) {
        this.entityData.set(DATA_SLIM_MODEL, slim);
    }

    public boolean isInConversation() {
        return this.entityData.get(DATA_IN_CONVERSATION);
    }

    public void setInConversation(boolean inConversation) {
        this.entityData.set(DATA_IN_CONVERSATION, inConversation);
    }

    // ==================== Eira Integration ====================
    
    /**
     * Handle an external event from Eira Relay (e.g., redstone activation)
     */
    public void onExternalEvent(ExternalEvent event) {
        StorytellerMod.LOGGER.debug("NPC {} received external event: {}", getNPCDisplayName(), event.type());
        
        NPCCharacter npcChar = getCharacter();
        
        // Build context injection for next conversation
        String contextInjection = switch (event.type()) {
            case REDSTONE_ON -> "You sense a disturbance - something in the physical world has just activated. " +
                "This might be significant to your visitor.";
            case REDSTONE_OFF -> "The strange energy you felt has subsided.";
            case REDSTONE_PULSE -> "You feel a rhythmic pulse of energy from beyond the game world.";
            case HTTP_TRIGGER -> "An external force has reached out to you.";
            case CUSTOM -> "Something unusual has occurred.";
        };
        
        // Store for next conversation
        this.pendingExternalContext = contextInjection;
        
        // If NPC has specific external trigger handlers in their character config,
        // they could auto-speak to nearby players
        if (event.type() == ExternalEvent.Type.REDSTONE_ON) {
            speakToNearbyPlayers(npcChar.getExternalTriggerMessage("redstone_on"));
        }
    }
    
    /**
     * Speak a message to all players within range
     */
    private void speakToNearbyPlayers(String message) {
        if (message == null || message.isEmpty()) return;
        if (!(level() instanceof ServerLevel serverLevel)) return;
        
        serverLevel.players().stream()
            .filter(p -> p.distanceToSqr(this) < 100) // 10 block radius
            .forEach(player -> {
                player.sendSystemMessage(Component.literal(
                    "§6[" + getNPCDisplayName() + "]§r " + message
                ));
            });
    }
    
    /**
     * Trigger a story event that may emit redstone or call webhooks
     */
    public void triggerStoryEvent(NPCEvent event, ServerPlayer player, Map<String, Object> data) {
        EiraIntegrationManager eira = StorytellerMod.getInstance().getEiraManager();
        if (eira == null || !eira.isEnabled()) return;
        
        NPCCharacter npcChar = getCharacter();
        
        // Check if this event should emit redstone
        var storyTrigger = npcChar.getStoryTrigger(event.name().toLowerCase());
        if (storyTrigger != null && storyTrigger.emitRedstone()) {
            eira.emitRedstone(
                this,
                storyTrigger.strength(),
                storyTrigger.durationTicks(),
                storyTrigger.pattern()
            );
        }
        
        // Send webhook
        eira.sendWebhook(this, player, event, data);
        
        StorytellerMod.LOGGER.debug("NPC {} triggered story event: {}", getNPCDisplayName(), event);
    }
    
    /**
     * Set redstone emission state (called by EiraIntegrationManager)
     */
    public void setEmittingRedstone(boolean emitting, int strength) {
        this.emittingRedstone = emitting;
        this.redstoneStrength = strength;
        
        // Update nearby blocks to recalculate redstone
        if (level() instanceof ServerLevel serverLevel) {
            BlockPos pos = blockPosition();
            for (Direction dir : Direction.values()) {
                serverLevel.updateNeighborsAt(pos.relative(dir), serverLevel.getBlockState(pos).getBlock());
            }
        }
    }
    
    /**
     * Check if NPC is currently emitting redstone (for comparators/observers)
     */
    public boolean isEmittingRedstone() {
        return emittingRedstone;
    }
    
    /**
     * Get current redstone signal strength
     */
    public int getRedstoneStrength() {
        return redstoneStrength;
    }
    
    /**
     * Get and clear any pending external context
     */
    public String consumePendingExternalContext() {
        String context = this.pendingExternalContext;
        this.pendingExternalContext = null;
        return context;
    }
    
    // ==================== End Eira Integration ====================

    @Override
    public Component getName() {
        String name = getNPCDisplayName();
        return name.isEmpty() ? super.getName() : Component.literal(name);
    }
}
