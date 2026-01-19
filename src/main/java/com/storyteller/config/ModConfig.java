package com.storyteller.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class ModConfig {
    
    // Common config (server-side)
    public static final ModConfigSpec SPEC;
    public static final CommonConfig COMMON;
    
    // Client config
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ClientConfig CLIENT;
    
    static {
        Pair<CommonConfig, ModConfigSpec> commonPair = new ModConfigSpec.Builder().configure(CommonConfig::new);
        COMMON = commonPair.getLeft();
        SPEC = commonPair.getRight();
        
        Pair<ClientConfig, ModConfigSpec> clientPair = new ModConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT = clientPair.getLeft();
        CLIENT_SPEC = clientPair.getRight();
    }
    
    public static class CommonConfig {
        // LLM Provider settings
        public final ModConfigSpec.EnumValue<LLMProvider> llmProvider;
        
        // Ollama settings
        public final ModConfigSpec.ConfigValue<String> ollamaEndpoint;
        public final ModConfigSpec.ConfigValue<String> ollamaModel;
        public final ModConfigSpec.IntValue ollamaTimeout;
        
        // Claude settings
        public final ModConfigSpec.ConfigValue<String> claudeApiKey;
        public final ModConfigSpec.ConfigValue<String> claudeModel;
        
        // OpenAI settings
        public final ModConfigSpec.ConfigValue<String> openaiApiKey;
        public final ModConfigSpec.ConfigValue<String> openaiModel;
        
        // NPC settings
        public final ModConfigSpec.IntValue maxConversationHistory;
        public final ModConfigSpec.IntValue responseTimeout;
        public final ModConfigSpec.BooleanValue includeWorldContext;
        public final ModConfigSpec.IntValue thinkingIndicatorDelay;
        public final ModConfigSpec.BooleanValue persistConversations;
        public final ModConfigSpec.IntValue maxPersistedMessages;
        
        // Rate limiting
        public final ModConfigSpec.IntValue minTimeBetweenMessages;
        public final ModConfigSpec.IntValue maxMessagesPerMinute;
        
        public CommonConfig(ModConfigSpec.Builder builder) {
            builder.comment("Storyteller NPC Configuration")
                   .push("llm");
            
            llmProvider = builder
                .comment("Which LLM provider to use")
                .defineEnum("provider", LLMProvider.OLLAMA);
            
            builder.comment("Ollama Settings").push("ollama");
            
            ollamaEndpoint = builder
                .comment("Ollama API endpoint URL")
                .define("endpoint", "http://localhost:11434");
            
            ollamaModel = builder
                .comment("Ollama model to use (recommended: mistral:7b-instruct, llama3.1:8b, or nous-hermes2:10.7b for best roleplay)")
                .define("model", "mistral:7b-instruct");
            
            ollamaTimeout = builder
                .comment("Request timeout in seconds")
                .defineInRange("timeout", 60, 10, 300);
            
            builder.pop();
            
            builder.comment("Claude Settings (Anthropic)").push("claude");
            
            claudeApiKey = builder
                .comment("Claude API key (leave empty to disable)")
                .define("apiKey", "");
            
            claudeModel = builder
                .comment("Claude model to use")
                .define("model", "claude-sonnet-4-20250514");
            
            builder.pop();
            
            builder.comment("OpenAI Settings").push("openai");
            
            openaiApiKey = builder
                .comment("OpenAI API key (leave empty to disable)")
                .define("apiKey", "");
            
            openaiModel = builder
                .comment("OpenAI model to use")
                .define("model", "gpt-4o");
            
            builder.pop();
            builder.pop();
            
            builder.comment("NPC Behavior Settings").push("npc");
            
            maxConversationHistory = builder
                .comment("Maximum number of messages to keep in conversation history per player")
                .defineInRange("maxConversationHistory", 20, 5, 100);
            
            responseTimeout = builder
                .comment("How long to wait for LLM response before timeout (seconds)")
                .defineInRange("responseTimeout", 30, 5, 120);
            
            includeWorldContext = builder
                .comment("Include world context (biome, time, weather, nearby structures) in prompts")
                .define("includeWorldContext", true);
            
            thinkingIndicatorDelay = builder
                .comment("Delay in ticks before showing 'thinking' particles (20 ticks = 1 second)")
                .defineInRange("thinkingIndicatorDelay", 20, 0, 100);

            persistConversations = builder
                .comment("Save conversation history to disk (survives server restarts)")
                .define("persistConversations", true);

            maxPersistedMessages = builder
                .comment("Maximum messages to persist per player-NPC pair")
                .defineInRange("maxPersistedMessages", 50, 10, 200);

            builder.pop();
            
            builder.comment("Rate Limiting").push("ratelimit");
            
            minTimeBetweenMessages = builder
                .comment("Minimum time between player messages to same NPC (ticks)")
                .defineInRange("minTimeBetweenMessages", 20, 0, 200);
            
            maxMessagesPerMinute = builder
                .comment("Maximum messages per player per minute (0 = unlimited)")
                .defineInRange("maxMessagesPerMinute", 10, 0, 60);
            
            builder.pop();
            
            builder.comment("Eira Relay Integration").push("integration");
            
            eiraEnabled = builder
                .comment("Enable Eira Relay integration for physical world events")
                .define("eiraEnabled", true);
            
            redstoneDetectionRadius = builder
                .comment("Radius (blocks) to detect redstone signals from Eira Receiver blocks")
                .defineInRange("redstoneDetectionRadius", 5, 1, 16);
            
            emitRedstoneOnEvents = builder
                .comment("NPCs emit redstone signals on story events (for Eira Sender blocks)")
                .define("emitRedstoneOnEvents", true);
            
            redstoneCooldown = builder
                .comment("Cooldown (ticks) between processing redstone events for same NPC")
                .defineInRange("redstoneCooldown", 40, 0, 200);
            
            builder.comment("Webhook Settings").push("webhooks");
            
            webhooksEnabled = builder
                .comment("Enable webhook callbacks for NPC events")
                .define("enabled", true);
            
            webhookTimeout = builder
                .comment("Webhook request timeout (milliseconds)")
                .defineInRange("timeout", 5000, 1000, 30000);
            
            webhookConversationStart = builder
                .comment("Webhook URL called when conversation starts (empty to disable)")
                .define("onConversationStart", "");
            
            webhookSecretRevealed = builder
                .comment("Webhook URL called when NPC reveals part of their secret")
                .define("onSecretRevealed", "");
            
            webhookQuestStarted = builder
                .comment("Webhook URL called when NPC gives a quest")
                .define("onQuestStarted", "");
            
            webhookQuestCompleted = builder
                .comment("Webhook URL called when quest is completed")
                .define("onQuestCompleted", "");
            
            webhookMoodChanged = builder
                .comment("Webhook URL called when NPC mood changes significantly")
                .define("onMoodChanged", "");
            
            webhookDangerWarning = builder
                .comment("Webhook URL called when NPC warns of danger")
                .define("onDangerWarning", "");
            
            builder.pop();
            builder.pop();
        }
        
        // Eira integration config values
        public final ModConfigSpec.BooleanValue eiraEnabled;
        public final ModConfigSpec.IntValue redstoneDetectionRadius;
        public final ModConfigSpec.BooleanValue emitRedstoneOnEvents;
        public final ModConfigSpec.IntValue redstoneCooldown;
        public final ModConfigSpec.BooleanValue webhooksEnabled;
        public final ModConfigSpec.IntValue webhookTimeout;
        public final ModConfigSpec.ConfigValue<String> webhookConversationStart;
        public final ModConfigSpec.ConfigValue<String> webhookSecretRevealed;
        public final ModConfigSpec.ConfigValue<String> webhookQuestStarted;
        public final ModConfigSpec.ConfigValue<String> webhookQuestCompleted;
        public final ModConfigSpec.ConfigValue<String> webhookMoodChanged;
        public final ModConfigSpec.ConfigValue<String> webhookDangerWarning;
    }
    
    public static class ClientConfig {
        public final ModConfigSpec.BooleanValue showThinkingParticles;
        public final ModConfigSpec.BooleanValue playChatSound;
        public final ModConfigSpec.IntValue chatDisplayTime;
        
        public ClientConfig(ModConfigSpec.Builder builder) {
            builder.comment("Client-side Storyteller Settings").push("client");
            
            showThinkingParticles = builder
                .comment("Show particles when NPC is 'thinking'")
                .define("showThinkingParticles", true);
            
            playChatSound = builder
                .comment("Play sound when NPC responds")
                .define("playChatSound", true);
            
            chatDisplayTime = builder
                .comment("How long NPC chat messages display (ticks)")
                .defineInRange("chatDisplayTime", 200, 60, 600);
            
            builder.pop();
        }
    }
    
    public enum LLMProvider {
        OLLAMA("Ollama (Local)"),
        CLAUDE("Claude (Anthropic)"),
        OPENAI("OpenAI");
        
        private final String displayName;
        
        LLMProvider(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
