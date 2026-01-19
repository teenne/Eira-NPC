package com.storyteller.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.storyteller.StorytellerMod;
import com.storyteller.config.ModConfig;
import com.storyteller.entity.StorytellerNPC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLPaths;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renderer for Storyteller NPCs using humanoid model with custom skins
 */
public class StorytellerNPCRenderer extends MobRenderer<StorytellerNPC, StorytellerNPCRenderState, HumanoidModel<StorytellerNPCRenderState>> {

    // Cache for loaded custom skin textures
    private static final Map<String, ResourceLocation> skinCache = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> slimModelCache = new ConcurrentHashMap<>();

    // Default Steve skin as fallback
    private static final ResourceLocation DEFAULT_SKIN = DefaultPlayerSkin.getDefaultTexture();

    private final HumanoidModel<StorytellerNPCRenderState> slimModel;
    private final HumanoidModel<StorytellerNPCRenderState> wideModel;

    public StorytellerNPCRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);

        // Create both slim and wide models
        this.wideModel = this.model;
        this.slimModel = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM));
    }

    @Override
    public ResourceLocation getTextureLocation(StorytellerNPCRenderState state) {
        String skinFile = state.skinFile;

        if (skinFile == null || skinFile.isEmpty()) {
            return DEFAULT_SKIN;
        }

        // Check cache first
        if (skinCache.containsKey(skinFile)) {
            return skinCache.get(skinFile);
        }

        // For now, return default - full skin loading requires more setup
        return DEFAULT_SKIN;
    }

    @Override
    public StorytellerNPCRenderState createRenderState() {
        return new StorytellerNPCRenderState();
    }

    @Override
    public void extractRenderState(StorytellerNPC entity, StorytellerNPCRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skinFile = entity.getSkinFile();
        state.slimModel = entity.isSlimModel();
        state.thinking = entity.isThinking();

        // Store entity position for particle spawning
        state.entityX = entity.getX();
        state.entityY = entity.getY();
        state.entityZ = entity.getZ();
    }

    @Override
    public void render(StorytellerNPCRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(state, poseStack, bufferSource, packedLight);

        // Spawn thinking particles
        if (state.thinking && ModConfig.CLIENT.showThinkingParticles.get()) {
            spawnThinkingParticles(state);
        }
    }

    /**
     * Spawn enchantment-like particles above NPC's head when thinking
     */
    private void spawnThinkingParticles(StorytellerNPCRenderState state) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        // Only spawn particles occasionally (not every frame)
        if (level.random.nextFloat() > 0.15f) return;

        double x = state.entityX + (level.random.nextDouble() - 0.5) * 0.6;
        double y = state.entityY + 2.0 + level.random.nextDouble() * 0.3;
        double z = state.entityZ + (level.random.nextDouble() - 0.5) * 0.6;

        // Spawn enchantment glint particles that float upward
        level.addParticle(
            ParticleTypes.ENCHANT,
            x, y, z,
            0.0, 0.1, 0.0
        );
    }

    /**
     * Get texture location for an entity (used internally)
     */
    private ResourceLocation getTextureForEntity(StorytellerNPC entity) {
        String skinFile = entity.getSkinFile();

        if (skinFile == null || skinFile.isEmpty()) {
            return DEFAULT_SKIN;
        }

        // Check cache first
        if (skinCache.containsKey(skinFile)) {
            return skinCache.get(skinFile);
        }

        // Try to load custom skin
        Path skinsDir = FMLPaths.CONFIGDIR.get().resolve("storyteller").resolve("skins");
        Path skinPath = skinsDir.resolve(skinFile);

        if (Files.exists(skinPath)) {
            try {
                // Check if it's a slim model skin (64x64 with transparency in arm area)
                boolean isSlim = checkIfSlimSkin(skinPath);
                slimModelCache.put(skinFile, isSlim);

                StorytellerMod.LOGGER.debug("Loaded custom skin: {} (slim: {})", skinFile, isSlim);

                // For now, return default - full skin loading requires more setup
                skinCache.put(skinFile, DEFAULT_SKIN);
                return DEFAULT_SKIN;

            } catch (Exception e) {
                StorytellerMod.LOGGER.error("Failed to load skin {}: {}", skinFile, e.getMessage());
                skinCache.put(skinFile, DEFAULT_SKIN);
            }
        } else {
            StorytellerMod.LOGGER.warn("Skin file not found: {}", skinPath);
            skinCache.put(skinFile, DEFAULT_SKIN);
        }

        return DEFAULT_SKIN;
    }

    private boolean checkIfSlimSkin(Path skinPath) {
        try {
            BufferedImage image = ImageIO.read(skinPath.toFile());
            // Slim skins are 64x64 and have transparency in specific arm pixels
            if (image.getWidth() == 64 && image.getHeight() == 64) {
                // Check a pixel that would be transparent in slim skins
                int pixel = image.getRGB(50, 16);
                int alpha = (pixel >> 24) & 0xFF;
                return alpha == 0;
            }
        } catch (IOException e) {
            // Ignore, default to wide
        }
        return false;
    }

    @Override
    protected void scale(StorytellerNPCRenderState state, PoseStack poseStack) {
        // Switch model based on slim setting
        boolean useSlim = state.slimModel;
        String skinFile = state.skinFile;

        // Check if skin file indicates slim
        if (skinFile != null && slimModelCache.containsKey(skinFile)) {
            useSlim = slimModelCache.get(skinFile);
        }

        this.model = useSlim ? slimModel : wideModel;

        // Standard player scale
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    /**
     * Clear the skin cache (call when skins are reloaded)
     */
    public static void clearSkinCache() {
        skinCache.clear();
        slimModelCache.clear();
    }
}
