package com.storyteller.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.storyteller.StorytellerMod;
import com.storyteller.entity.ModEntities;
import com.storyteller.entity.NPCBehaviorMode;
import com.storyteller.entity.StorytellerNPC;
import com.storyteller.npc.NPCCharacter;
import com.storyteller.npc.NPCManager;
import com.storyteller.npc.QuestManager;
import com.storyteller.npc.knowledge.KnowledgeEntry;
import com.storyteller.npc.knowledge.KnowledgeManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

    private static final SuggestionProvider<CommandSourceStack> NPC_SUGGESTIONS = (context, builder) -> {
        ServerLevel level = context.getSource().getLevel();
        List<String> npcNames = level.getEntitiesOfClass(
            StorytellerNPC.class,
            context.getSource().getEntity() != null
                ? context.getSource().getEntity().getBoundingBox().inflate(50)
                : new AABB(context.getSource().getPosition(), context.getSource().getPosition()).inflate(50)
        ).stream().map(StorytellerNPC::getNPCDisplayName).distinct().toList();

        List<String> suggestions = new java.util.ArrayList<>();
        suggestions.add("nearest");
        suggestions.addAll(npcNames);
        return SharedSuggestionProvider.suggest(suggestions, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> BEHAVIOR_MODE_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
            Arrays.stream(NPCBehaviorMode.values()).map(NPCBehaviorMode::getId),
            builder
        );
    };

    private static final SuggestionProvider<CommandSourceStack> KNOWLEDGE_CHARACTER_SUGGESTIONS = (context, builder) -> {
        NPCManager manager = StorytellerMod.getInstance().getNPCManager();
        return SharedSuggestionProvider.suggest(
            manager.getAllCharacters().values().stream()
                .map(NPCCharacter::getId),
            builder
        );
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("storyteller")
            .requires(source -> source.hasPermission(2))
            .executes(ModCommands::showHelp)

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

            // Quest management
            .then(Commands.literal("quests")
                .executes(ModCommands::listQuests)
                .then(Commands.literal("clear")
                    .executes(ModCommands::clearQuests)
                )
            )

            // Behavior commands
            .then(Commands.literal("behavior")
                .then(Commands.argument("npc", StringArgumentType.string())
                    .suggests(NPC_SUGGESTIONS)
                    // Info subcommand
                    .then(Commands.literal("info")
                        .executes(ModCommands::behaviorInfo)
                    )
                    // Stationary mode
                    .then(Commands.literal("stationary")
                        .executes(ModCommands::setBehaviorStationary)
                    )
                    // Anchored mode
                    .then(Commands.literal("anchored")
                        .executes(ModCommands::setBehaviorAnchoredDefault)
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                            .executes(ModCommands::setBehaviorAnchoredRadius)
                        )
                        .then(Commands.literal("here")
                            .executes(ModCommands::setBehaviorAnchoredHereDefault)
                            .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                                .executes(ModCommands::setBehaviorAnchoredHere)
                            )
                        )
                    )
                    // Follow mode
                    .then(Commands.literal("follow")
                        .executes(ModCommands::setBehaviorFollowSelf)
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(ModCommands::setBehaviorFollowPlayer)
                        )
                        .then(Commands.argument("distance", IntegerArgumentType.integer(1, 50))
                            .executes(ModCommands::setBehaviorFollowDistance)
                        )
                    )
                    // Hiding mode
                    .then(Commands.literal("hiding")
                        .executes(ModCommands::setBehaviorHiding)
                    )
                )
            )

            // Knowledge commands
            .then(Commands.literal("knowledge")
                .then(Commands.literal("reload")
                    .executes(ModCommands::knowledgeReloadAll)
                    .then(Commands.argument("character", StringArgumentType.string())
                        .suggests(KNOWLEDGE_CHARACTER_SUGGESTIONS)
                        .executes(ModCommands::knowledgeReloadCharacter)
                    )
                )
                .then(Commands.literal("list")
                    .then(Commands.argument("character", StringArgumentType.string())
                        .suggests(KNOWLEDGE_CHARACTER_SUGGESTIONS)
                        .executes(ModCommands::knowledgeList)
                    )
                )
                .then(Commands.literal("test")
                    .then(Commands.argument("character", StringArgumentType.string())
                        .suggests(KNOWLEDGE_CHARACTER_SUGGESTIONS)
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                            .executes(ModCommands::knowledgeTest)
                        )
                    )
                )
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

            // Set the spawner player UUID if run by a player
            if (source.getEntity() instanceof ServerPlayer player) {
                npc.setSpawnerPlayerUUID(player.getUUID());
            }

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

    private static int listQuests(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (source.getEntity() == null) {
            source.sendFailure(Component.literal("This command must be run as a player"));
            return 0;
        }

        var quests = QuestManager.getActiveQuests(source.getEntity().getUUID());

        if (quests.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7You have no active quests."), false);
            source.sendSuccess(() -> Component.literal("§7Talk to NPCs to receive quests!"), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§6=== Your Active Quests ==="), false);

        int index = 1;
        for (var quest : quests) {
            final int questNum = index++;
            final String questDesc = quest.description();
            final String progress = quest.type() == QuestManager.QuestType.KILL_MOB
                ? " §e(" + quest.progress() + "/" + quest.targetCount() + ")"
                : "";

            source.sendSuccess(() -> Component.literal(
                "§a[" + questNum + "]§r " + questDesc + progress
            ), false);
        }

        return quests.size();
    }

    private static int clearQuests(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (source.getEntity() == null) {
            source.sendFailure(Component.literal("This command must be run as a player"));
            return 0;
        }

        QuestManager.clearQuests(source.getEntity().getUUID());
        source.sendSuccess(() -> Component.literal("§aCleared all active quests."), true);

        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("§6=== Storyteller NPC Commands ==="), false);
        source.sendSuccess(() -> Component.literal("§e/storyteller spawn §7- Spawn default NPC"), false);
        source.sendSuccess(() -> Component.literal("§e/storyteller spawn <name> §7- Spawn specific character"), false);
        source.sendSuccess(() -> Component.literal("§e/storyteller list §7- List available characters"), false);
        source.sendSuccess(() -> Component.literal("§e/storyteller skins §7- List available skins"), false);
        source.sendSuccess(() -> Component.literal("§e/storyteller create <name> §7- Create new character"), false);
        source.sendSuccess(() -> Component.literal("§e/storyteller reload §7- Reload configurations"), false);
        source.sendSuccess(() -> Component.literal("§e/storyteller status §7- Show LLM connection status"), false);
        source.sendSuccess(() -> Component.literal("§e/storyteller quests §7- List your active quests"), false);
        source.sendSuccess(() -> Component.literal("§e/storyteller quests clear §7- Clear all quests"), false);
        source.sendSuccess(() -> Component.literal("§6=== Behavior Commands ==="), false);
        source.sendSuccess(() -> Component.literal("§e/storyteller behavior <npc> info §7- Show NPC behavior"), false);
        source.sendSuccess(() -> Component.literal("§e/storyteller behavior <npc> stationary §7- Stay in place"), false);
        source.sendSuccess(() -> Component.literal("§e/storyteller behavior <npc> anchored [radius] §7- Wander near anchor"), false);
        source.sendSuccess(() -> Component.literal("§e/storyteller behavior <npc> anchored here [radius] §7- Set anchor to current pos"), false);
        source.sendSuccess(() -> Component.literal("§e/storyteller behavior <npc> follow [player] §7- Follow a player"), false);
        source.sendSuccess(() -> Component.literal("§e/storyteller behavior <npc> hiding §7- Hide from players"), false);
        source.sendSuccess(() -> Component.literal("§7Use 'nearest' for <npc> to select closest NPC"), false);
        source.sendSuccess(() -> Component.literal("§6=== Knowledge Commands ==="), false);
        source.sendSuccess(() -> Component.literal("§e/storyteller knowledge reload §7- Reload all knowledge bases"), false);
        source.sendSuccess(() -> Component.literal("§e/storyteller knowledge list <character> §7- List knowledge entries"), false);
        source.sendSuccess(() -> Component.literal("§e/storyteller knowledge test <character> <message> §7- Test retrieval"), false);
        source.sendSuccess(() -> Component.literal("§7Right-click an NPC to start chatting!"), false);

        return 1;
    }

    // ==================== Behavior Commands ====================

    /**
     * Find an NPC by selector: "nearest" or by display name
     */
    private static StorytellerNPC findNPC(CommandSourceStack source, String selector) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        List<StorytellerNPC> npcs = level.getEntitiesOfClass(
            StorytellerNPC.class,
            new AABB(pos, pos).inflate(100)
        );

        if (npcs.isEmpty()) {
            return null;
        }

        if (selector.equalsIgnoreCase("nearest")) {
            // Find the closest NPC
            StorytellerNPC nearest = null;
            double nearestDist = Double.MAX_VALUE;
            for (StorytellerNPC npc : npcs) {
                double dist = npc.distanceToSqr(pos);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = npc;
                }
            }
            return nearest;
        }

        // Try to match by display name
        for (StorytellerNPC npc : npcs) {
            if (npc.getNPCDisplayName().equalsIgnoreCase(selector)) {
                return npc;
            }
        }

        // Try to match by UUID
        try {
            UUID uuid = UUID.fromString(selector);
            for (StorytellerNPC npc : npcs) {
                if (npc.getUUID().equals(uuid)) {
                    return npc;
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Not a valid UUID, that's fine
        }

        return null;
    }

    private static int behaviorInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String selector = StringArgumentType.getString(context, "npc");

        StorytellerNPC npc = findNPC(source, selector);
        if (npc == null) {
            source.sendFailure(Component.literal("NPC not found: " + selector));
            return 0;
        }

        NPCBehaviorMode mode = npc.getBehaviorMode();
        source.sendSuccess(() -> Component.literal("§6=== " + npc.getNPCDisplayName() + " Behavior ==="), false);
        source.sendSuccess(() -> Component.literal("§eMode: §a" + mode.getId() + " §7(" + mode.getDescription() + ")"), false);

        switch (mode) {
            case ANCHORED -> {
                BlockPos anchor = npc.getAnchorPosition();
                int radius = npc.getAnchorRadius();
                source.sendSuccess(() -> Component.literal("§eAnchor: §a" +
                    (anchor != null ? anchor.getX() + ", " + anchor.getY() + ", " + anchor.getZ() : "Not set")), false);
                source.sendSuccess(() -> Component.literal("§eRadius: §a" + radius + " blocks"), false);
            }
            case FOLLOW_PLAYER -> {
                UUID target = npc.getTargetPlayerUUID();
                UUID spawner = npc.getSpawnerPlayerUUID();
                int distance = npc.getFollowDistance();

                String targetName = "None";
                if (target != null) {
                    ServerPlayer player = source.getServer().getPlayerList().getPlayer(target);
                    targetName = player != null ? player.getName().getString() : target.toString();
                } else if (spawner != null) {
                    ServerPlayer player = source.getServer().getPlayerList().getPlayer(spawner);
                    targetName = player != null ? player.getName().getString() + " (spawner)" : spawner.toString();
                }
                String finalTargetName = targetName;
                source.sendSuccess(() -> Component.literal("§eFollowing: §a" + finalTargetName), false);
                source.sendSuccess(() -> Component.literal("§eDistance: §a" + distance + " blocks"), false);
            }
            case HIDING -> {
                source.sendSuccess(() -> Component.literal("§eHides from players within line of sight"), false);
            }
            case STATIONARY -> {
                source.sendSuccess(() -> Component.literal("§eStays mostly in place with rare wandering"), false);
            }
        }

        return 1;
    }

    private static int setBehaviorStationary(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String selector = StringArgumentType.getString(context, "npc");

        StorytellerNPC npc = findNPC(source, selector);
        if (npc == null) {
            source.sendFailure(Component.literal("NPC not found: " + selector));
            return 0;
        }

        npc.setBehaviorMode(NPCBehaviorMode.STATIONARY);
        source.sendSuccess(() -> Component.literal(
            "§a" + npc.getNPCDisplayName() + " is now stationary"
        ), true);

        return 1;
    }

    private static int setBehaviorAnchoredDefault(CommandContext<CommandSourceStack> context) {
        return setBehaviorAnchored(context, null, false);
    }

    private static int setBehaviorAnchoredRadius(CommandContext<CommandSourceStack> context) {
        int radius = IntegerArgumentType.getInteger(context, "radius");
        return setBehaviorAnchored(context, radius, false);
    }

    private static int setBehaviorAnchoredHereDefault(CommandContext<CommandSourceStack> context) {
        return setBehaviorAnchored(context, null, true);
    }

    private static int setBehaviorAnchoredHere(CommandContext<CommandSourceStack> context) {
        int radius = IntegerArgumentType.getInteger(context, "radius");
        return setBehaviorAnchored(context, radius, true);
    }

    private static int setBehaviorAnchored(CommandContext<CommandSourceStack> context, Integer radius, boolean setAnchorHere) {
        CommandSourceStack source = context.getSource();
        String selector = StringArgumentType.getString(context, "npc");

        StorytellerNPC npc = findNPC(source, selector);
        if (npc == null) {
            source.sendFailure(Component.literal("NPC not found: " + selector));
            return 0;
        }

        if (setAnchorHere) {
            npc.setAnchorPosition(BlockPos.containing(source.getPosition()));
        } else if (npc.getAnchorPosition() == null) {
            // Default to NPC's current position
            npc.setAnchorPosition(npc.blockPosition());
        }

        if (radius != null) {
            npc.setAnchorRadius(radius);
        }

        npc.setBehaviorMode(NPCBehaviorMode.ANCHORED);

        BlockPos anchor = npc.getAnchorPosition();
        int finalRadius = npc.getAnchorRadius();
        source.sendSuccess(() -> Component.literal(
            "§a" + npc.getNPCDisplayName() + " is now anchored at " +
                anchor.getX() + ", " + anchor.getY() + ", " + anchor.getZ() +
                " (radius: " + finalRadius + " blocks)"
        ), true);

        return 1;
    }

    private static int setBehaviorFollowSelf(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String selector = StringArgumentType.getString(context, "npc");

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command must be run as a player"));
            return 0;
        }

        StorytellerNPC npc = findNPC(source, selector);
        if (npc == null) {
            source.sendFailure(Component.literal("NPC not found: " + selector));
            return 0;
        }

        npc.setTargetPlayerUUID(player.getUUID());
        npc.setBehaviorMode(NPCBehaviorMode.FOLLOW_PLAYER);

        source.sendSuccess(() -> Component.literal(
            "§a" + npc.getNPCDisplayName() + " is now following you"
        ), true);

        return 1;
    }

    private static int setBehaviorFollowPlayer(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String selector = StringArgumentType.getString(context, "npc");

        StorytellerNPC npc = findNPC(source, selector);
        if (npc == null) {
            source.sendFailure(Component.literal("NPC not found: " + selector));
            return 0;
        }

        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            npc.setTargetPlayerUUID(target.getUUID());
            npc.setBehaviorMode(NPCBehaviorMode.FOLLOW_PLAYER);

            source.sendSuccess(() -> Component.literal(
                "§a" + npc.getNPCDisplayName() + " is now following " + target.getName().getString()
            ), true);

            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Player not found"));
            return 0;
        }
    }

    private static int setBehaviorFollowDistance(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String selector = StringArgumentType.getString(context, "npc");

        StorytellerNPC npc = findNPC(source, selector);
        if (npc == null) {
            source.sendFailure(Component.literal("NPC not found: " + selector));
            return 0;
        }

        int distance = IntegerArgumentType.getInteger(context, "distance");
        npc.setFollowDistance(distance);

        // If not already in follow mode, set to follow the command issuer
        if (npc.getBehaviorMode() != NPCBehaviorMode.FOLLOW_PLAYER) {
            if (source.getEntity() instanceof ServerPlayer player) {
                npc.setTargetPlayerUUID(player.getUUID());
            }
            npc.setBehaviorMode(NPCBehaviorMode.FOLLOW_PLAYER);
        }

        source.sendSuccess(() -> Component.literal(
            "§a" + npc.getNPCDisplayName() + " follow distance set to " + distance + " blocks"
        ), true);

        return 1;
    }

    private static int setBehaviorHiding(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String selector = StringArgumentType.getString(context, "npc");

        StorytellerNPC npc = findNPC(source, selector);
        if (npc == null) {
            source.sendFailure(Component.literal("NPC not found: " + selector));
            return 0;
        }

        npc.setBehaviorMode(NPCBehaviorMode.HIDING);
        source.sendSuccess(() -> Component.literal(
            "§a" + npc.getNPCDisplayName() + " is now hiding from players"
        ), true);

        return 1;
    }

    // ==================== Knowledge Commands ====================

    private static int knowledgeReloadAll(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        KnowledgeManager.reloadAll();

        List<String> loaded = KnowledgeManager.getLoadedCharacterIds();
        source.sendSuccess(() -> Component.literal(
            "§aReloaded " + loaded.size() + " knowledge base(s)"
        ), true);

        if (!loaded.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                "§7Characters with knowledge: " + String.join(", ", loaded)
            ), false);
        }

        return 1;
    }

    private static int knowledgeReloadCharacter(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String characterId = StringArgumentType.getString(context, "character");

        boolean success = KnowledgeManager.reload(characterId);

        if (success) {
            source.sendSuccess(() -> Component.literal(
                "§aReloaded knowledge base for: " + characterId
            ), true);
            return 1;
        } else {
            source.sendFailure(Component.literal(
                "No knowledge file found for: " + characterId +
                "\n§7Expected: config/storyteller/knowledge/" + characterId + ".json"
            ));
            return 0;
        }
    }

    private static int knowledgeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String characterId = StringArgumentType.getString(context, "character");

        if (!KnowledgeManager.hasKnowledgeBase(characterId)) {
            source.sendFailure(Component.literal(
                "No knowledge base found for: " + characterId
            ));
            return 0;
        }

        var kb = KnowledgeManager.getForCharacter(characterId);
        if (kb == null) {
            source.sendFailure(Component.literal("Knowledge base is disabled or unavailable"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
            "§6=== Knowledge Base: " + characterId + " ==="
        ), false);

        source.sendSuccess(() -> Component.literal(
            "§eEntries: " + kb.getEntryCount()
        ), false);

        int index = 1;
        for (KnowledgeEntry entry : kb.getEntries()) {
            final int num = index++;
            final String id = entry.id();
            final String category = entry.category();
            final int priority = entry.priority();
            final String keywords = String.join(", ", entry.keywords());

            source.sendSuccess(() -> Component.literal(
                "§a[" + num + "]§r " + id + " §7(cat: " + category + ", pri: " + priority + ")"
            ), false);
            source.sendSuccess(() -> Component.literal(
                "    §7Keywords: " + keywords
            ), false);
        }

        return kb.getEntryCount();
    }

    private static int knowledgeTest(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String characterId = StringArgumentType.getString(context, "character");
        String message = StringArgumentType.getString(context, "message");

        if (!KnowledgeManager.hasKnowledgeBase(characterId)) {
            source.sendFailure(Component.literal(
                "No knowledge base found for: " + characterId
            ));
            return 0;
        }

        List<KnowledgeEntry> results = KnowledgeManager.retrieve(characterId, message);

        source.sendSuccess(() -> Component.literal(
            "§6=== Knowledge Test: " + characterId + " ==="
        ), false);
        source.sendSuccess(() -> Component.literal(
            "§7Query: \"" + message + "\""
        ), false);
        source.sendSuccess(() -> Component.literal(
            "§eResults: " + results.size()
        ), false);

        if (results.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                "§7No matching knowledge entries found."
            ), false);
        } else {
            for (KnowledgeEntry entry : results) {
                source.sendSuccess(() -> Component.literal(
                    "§a[" + entry.id() + "]§r " + entry.content()
                ), false);
            }
        }

        return results.size();
    }
}
