package com.storyteller.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.storyteller.StorytellerMod;
import com.storyteller.entity.ModEntities;
import com.storyteller.entity.StorytellerNPC;
import com.storyteller.npc.NPCCharacter;
import com.storyteller.npc.NPCManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = StorytellerMod.MOD_ID)
public class ModCommands {
    
    private static final SuggestionProvider<CommandSourceStack> CHARACTER_SUGGESTIONS = (context, builder) -> {
        NPCManager manager = StorytellerMod.getInstance().getNPCManager();
        return SharedSuggestionProvider.suggest(
            manager.getAllCharacters().values().stream()
                .map(NPCCharacter::getName),
            builder
        );
    };
    
    private static final SuggestionProvider<CommandSourceStack> SKIN_SUGGESTIONS = (context, builder) -> {
        NPCManager manager = StorytellerMod.getInstance().getNPCManager();
        return SharedSuggestionProvider.suggest(
            java.util.Arrays.asList(manager.getAvailableSkins()),
            builder
        );
    };
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("storyteller")
            .requires(source -> source.hasPermission(2))
            
            // Spawn NPC
            .then(Commands.literal("spawn")
                .executes(ModCommands::spawnDefault)
                .then(Commands.argument("character", StringArgumentType.string())
                    .suggests(CHARACTER_SUGGESTIONS)
                    .executes(ModCommands::spawnCharacter)
                )
            )
            
            // List characters
            .then(Commands.literal("list")
                .executes(ModCommands::listCharacters)
            )
            
            // List available skins
            .then(Commands.literal("skins")
                .executes(ModCommands::listSkins)
            )
            
            // Reload configurations
            .then(Commands.literal("reload")
                .executes(ModCommands::reload)
            )
            
            // Create new character
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(ModCommands::createCharacter)
                )
            )
            
            // LLM status
            .then(Commands.literal("status")
                .executes(ModCommands::showStatus)
            )
        );
        
        StorytellerMod.LOGGER.info("Registered /storyteller command");
    }
    
    private static int spawnDefault(CommandContext<CommandSourceStack> context) {
        return spawnNPC(context, null);
    }
    
    private static int spawnCharacter(CommandContext<CommandSourceStack> context) {
        String characterName = StringArgumentType.getString(context, "character");
        return spawnNPC(context, characterName);
    }
    
    private static int spawnNPC(CommandContext<CommandSourceStack> context, String characterName) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getLevel() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("Must be run in a world"));
            return 0;
        }
        
        Vec3 pos = source.getPosition();
        NPCManager manager = StorytellerMod.getInstance().getNPCManager();
        
        // Find character
        NPCCharacter character;
        if (characterName != null) {
            character = manager.getAllCharacters().values().stream()
                .filter(c -> c.getName().equalsIgnoreCase(characterName))
                .findFirst()
                .orElse(null);
            
            if (character == null) {
                source.sendFailure(Component.literal("Character not found: " + characterName));
                source.sendFailure(Component.literal("Use /storyteller list to see available characters"));
                return 0;
            }
        } else {
            character = manager.getDefaultCharacter();
        }
        
        // Spawn the NPC
        StorytellerNPC npc = ModEntities.STORYTELLER_NPC.get().create(
            level,
            EntitySpawnReason.COMMAND
        );

        if (npc != null) {
            npc.setPos(pos.x(), pos.y(), pos.z());
            npc.setCharacter(character);
            level.addFreshEntity(npc);
            
            source.sendSuccess(() -> Component.literal(
                "Spawned " + character.getName() + " (" + character.getTitle() + ")"
            ), true);
            return 1;
        }
        
        source.sendFailure(Component.literal("Failed to spawn NPC"));
        return 0;
    }
    
    private static int listCharacters(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        NPCManager manager = StorytellerMod.getInstance().getNPCManager();
        
        var characters = manager.getAllCharacters();
        
        if (characters.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No characters configured"), false);
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§6Available Characters:"), false);
        for (NPCCharacter character : characters.values()) {
            source.sendSuccess(() -> Component.literal(
                "  §a" + character.getName() + "§r - " + character.getTitle()
            ), false);
        }
        
        source.sendSuccess(() -> Component.literal(
            "§7Characters are stored in: config/storyteller/characters/"
        ), false);
        
        return characters.size();
    }
    
    private static int listSkins(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        NPCManager manager = StorytellerMod.getInstance().getNPCManager();
        
        String[] skins = manager.getAvailableSkins();
        
        if (skins.length == 0) {
            source.sendSuccess(() -> Component.literal("No custom skins found"), false);
            source.sendSuccess(() -> Component.literal(
                "§7Place .png skin files in: config/storyteller/skins/"
            ), false);
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§6Available Skins:"), false);
        for (String skin : skins) {
            source.sendSuccess(() -> Component.literal("  §a" + skin), false);
        }
        
        return skins.length;
    }
    
    private static int reload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        StorytellerMod.getInstance().getNPCManager().loadNPCs();
        
        source.sendSuccess(() -> Component.literal("§aReloaded Storyteller configurations"), true);
        return 1;
    }
    
    private static int createCharacter(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "name");
        
        NPCManager manager = StorytellerMod.getInstance().getNPCManager();
        
        // Create new character with the given name
        NPCCharacter character = NPCCharacter.createDefault();
        character.setName(name);
        character.setTitle("The " + name);
        
        manager.registerCharacter(character);
        
        source.sendSuccess(() -> Component.literal(
            "§aCreated new character: " + name
        ), true);
        source.sendSuccess(() -> Component.literal(
            "§7Edit the character file at: config/storyteller/characters/" + character.getId() + ".json"
        ), false);
        
        return 1;
    }
    
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        var llmManager = StorytellerMod.getInstance().getLLMManager();
        var npcManager = StorytellerMod.getInstance().getNPCManager();
        
        source.sendSuccess(() -> Component.literal("§6=== Storyteller Status ==="), false);
        
        // LLM Status
        if (llmManager.isAvailable()) {
            source.sendSuccess(() -> Component.literal(
                "§aLLM Provider: " + llmManager.getActiveProviderName() + " (connected)"
            ), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                "§cLLM Provider: Not connected"
            ), false);
        }
        
        // Character count
        int charCount = npcManager.getAllCharacters().size();
        source.sendSuccess(() -> Component.literal(
            "§eCharacters loaded: " + charCount
        ), false);
        
        // Skin count
        int skinCount = npcManager.getAvailableSkins().length;
        source.sendSuccess(() -> Component.literal(
            "§eCustom skins available: " + skinCount
        ), false);
        
        return 1;
    }
}
