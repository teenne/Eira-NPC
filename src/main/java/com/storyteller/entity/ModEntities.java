package com.storyteller.entity;

import com.storyteller.StorytellerMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = StorytellerMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, StorytellerMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<StorytellerNPC>> STORYTELLER_NPC =
        ENTITY_TYPES.register("storyteller_npc", () ->
            EntityType.Builder.of(StorytellerNPC::new, MobCategory.MISC)
                .sized(0.6F, 1.8F) // Player-sized
                .clientTrackingRange(10)
                .updateInterval(3)
                .build(ResourceKey.create(Registries.ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(StorytellerMod.MOD_ID, "storyteller_npc")))
        );
    
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(STORYTELLER_NPC.get(), StorytellerNPC.createAttributes().build());
    }
}
