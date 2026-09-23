package net.dragonmounts.neo.common.client.variant;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.dragonmounts.neo.common.client.DMParticleSprites;
import net.dragonmounts.neo.common.client.breath.BreathParticleFactory;
import net.dragonmounts.neo.common.client.breath.impl.FlameBreathParticle;
import net.dragonmounts.neo.common.client.model.dragon.DragonModel;
import net.dragonmounts.neo.common.client.renderer.RenderStateAccessor;
import net.dragonmounts.neo.common.client.renderer.dragon.DragonRenderState;
import net.dragonmounts.neo.common.entity.breath.BreathParticleOption;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class DefaultAppearance implements VariantAppearance {
    private static final Object2ObjectOpenHashMap<String, Map<ResourceKey<EquipmentAsset>, Identifier>> ARMOR_TEXTURES = new Object2ObjectOpenHashMap<>();
    private static final Map<ResourceKey<EquipmentAsset>, Identifier> DEFAULT_ARMOR_TEXTURES = getTextures(null);

    synchronized static Map<ResourceKey<EquipmentAsset>, Identifier> getTextures(@Nullable String category) {
        return ARMOR_TEXTURES.computeIfAbsent(category, $ -> new Reference2ObjectOpenHashMap<>());
    }

    public synchronized static void registerArmorTexture(@Nullable String category, ResourceKey<EquipmentAsset> asset, Identifier texture) {
        if (getTextures(category).put(asset, texture) != null) {
            throw new IllegalStateException("Duplicate asset: " + asset);
        }
    }

    public final ModelLayerLocation modelLocation;
    public final BreathParticleFactory factory;
    public final Identifier breath;
    public final Identifier body;
    public final RenderType base;
    public final RenderType decal;
    public final RenderType glow;
    public final RenderType glowDecal;
    public final RenderType chest;
    public final RenderType saddle;
    private DragonModel model;
    final Map<ResourceKey<EquipmentAsset>, Identifier> armors;

    public DefaultAppearance(
            ModelLayerLocation modelLocation,
            Identifier body,
            Identifier glow,
            Identifier breath,
            Map<ResourceKey<EquipmentAsset>, Identifier> armors,
            BreathParticleFactory factory
    ) {
        this.modelLocation = modelLocation;
        this.factory = factory;
        this.breath = breath;
        this.armors = armors;
        this.body = body;
        this.base = RenderTypes.entityCutout(body);
        this.decal = RenderStateAccessor.entityCutoutDecal(body, DEFAULT_DISSOLVE);
        this.glow = RenderTypes.entityTranslucentEmissive(glow);
        this.glowDecal = RenderStateAccessor.entityTranslucentEmissiveDecal(glow, DEFAULT_DISSOLVE);
        this.chest = RenderTypes.entityCutout(DEFAULT_CHEST);
        this.saddle = RenderTypes.entityCutout(DEFAULT_SADDLE);
    }

    @Override
    public void onReload(EntityModelSet models) {
        this.model = new DragonModel(models.bakeLayer(this.modelLocation));
    }

    @Override
    public DragonModel getModel(@Nullable DragonRenderState state) {
        return this.model;
    }

    @Override
    public Identifier getBodyTexture(DragonRenderState state) {
        return this.body;
    }

    @Override
    public RenderType getBase(@Nullable DragonRenderState state) {
        return this.base;
    }

    @Override
    public RenderType getGlow(@Nullable DragonRenderState state) {
        return this.glow;
    }

    @Override
    public RenderType getDecal(DragonRenderState state) {
        return this.decal;
    }

    @Override
    public RenderType getGlowDecal(DragonRenderState state) {
        return this.glowDecal;
    }

    @Override
    public RenderType getChest(DragonRenderState state) {
        return this.chest;
    }

    @Override
    public RenderType getSaddle(DragonRenderState state) {
        return this.saddle;
    }

    @Override
    public @Nullable Identifier getArmorTexture(ResourceKey<EquipmentAsset> asset) {
        var override = this.armors.get(asset);
        return override == null ? DEFAULT_ARMOR_TEXTURES.get(asset) : override;
    }

    @Override
    public Particle createBreathParticle(BreathParticleOption option, TextureAtlas atlas, ClientLevel level, double x, double y, double z, double motionX, double motionY, double motionZ) {
        return this.factory.createParticle(option, atlas.getSprite(this.breath), level, x, y, z, motionX, motionY, motionZ);
    }

    public static class Builder {
        public final ModelLayerLocation model;
        public BreathParticleFactory factory = FlameBreathParticle.FACTORY;
        public Identifier breath = DMParticleSprites.FLAME_BREATH;
        Map<ResourceKey<EquipmentAsset>, Identifier> armors = DEFAULT_ARMOR_TEXTURES;

        public Builder(ModelLayerLocation model) {
            this.model = model;
        }

        public Builder setArmorCategory(@Nullable String category) {
            this.armors = getTextures(category);
            return this;
        }

        public Builder withBreath(Identifier breath) {
            this.breath = breath;
            return this;
        }

        public Builder withBreath(Identifier breath, BreathParticleFactory factory) {
            this.factory = factory;
            return this.withBreath(breath);
        }

        public DefaultAppearance build(Identifier folder) {
            String path = folder.getPath();
            return this.build(
                    folder.withPath(TEXTURES_ROOT + path + "/body.png"),
                    folder.withPath(TEXTURES_ROOT + path + "/glow.png")
            );
        }

        public DefaultAppearance build(Identifier body, Identifier glow) {
            return new DefaultAppearance(this.model, body, glow, this.breath, this.armors, this.factory);
        }
    }
}
