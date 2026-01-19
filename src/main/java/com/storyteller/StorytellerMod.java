package com.storyteller;

import com.storyteller.config.ModConfig;
import com.storyteller.entity.ModEntities;
import com.storyteller.integration.EiraIntegrationManager;
import com.storyteller.llm.LLMManager;
import com.storyteller.network.ModNetwork;
import com.storyteller.npc.NPCManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(StorytellerMod.MOD_ID)
public class StorytellerMod {
    public static final String MOD_ID = "storyteller";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static StorytellerMod instance;
    
    private final LLMManager llmManager;
    private final NPCManager npcManager;
    private final EiraIntegrationManager eiraManager;
    
    public StorytellerMod(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;
        
        // Register config
        modContainer.registerConfig(Type.COMMON, ModConfig.SPEC, "storyteller-common.toml");
        modContainer.registerConfig(Type.CLIENT, ModConfig.CLIENT_SPEC, "storyteller-client.toml");
        
        // Initialize managers
        this.llmManager = new LLMManager();
        this.npcManager = new NPCManager();
        this.eiraManager = new EiraIntegrationManager();
        
        // Register mod event listeners
        modEventBus.addListener(this::commonSetup);
        
        // Register entity types
        ModEntities.ENTITY_TYPES.register(modEventBus);
        
        // Register game event listeners
        NeoForge.EVENT_BUS.register(this);
        
        LOGGER.info("Storyteller mod initialized!");
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.register();
            LOGGER.info("Storyteller network registered");
        });
    }
    
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Storyteller: Server starting, initializing LLM connection...");
        llmManager.initialize();
        npcManager.loadNPCs();
        eiraManager.initialize();
    }
    
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Storyteller: Server stopping, cleaning up...");
        llmManager.shutdown();
        npcManager.saveNPCs();
        eiraManager.shutdown();
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        // Tick Eira integration for redstone detection (every 4 ticks to reduce overhead)
        if (event.getServer().getTickCount() % 4 == 0) {
            event.getServer().getAllLevels().forEach(level -> {
                eiraManager.tick(level);
            });
        }
    }
    
    public static StorytellerMod getInstance() {
        return instance;
    }
    
    public LLMManager getLLMManager() {
        return llmManager;
    }
    
    public NPCManager getNPCManager() {
        return npcManager;
    }
    
    public EiraIntegrationManager getEiraManager() {
        return eiraManager;
    }
}
