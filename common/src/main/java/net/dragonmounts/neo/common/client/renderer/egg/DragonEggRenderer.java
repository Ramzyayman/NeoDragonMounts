package net.dragonmounts.neo.common.client.renderer.egg;

import com.mojang.blaze3d.vertex.PoseStack;
import net.dragonmounts.neo.common.entity.dragon.HatchableDragonEggEntity;
import net.dragonmounts.neo.common.init.DMBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import static net.dragonmounts.neo.common.entity.dragon.HatchableDragonEggEntity.EGG_CRACK_THRESHOLD;
import static net.dragonmounts.neo.common.util.math.MathUtil.HALF_RAD_FACTOR;

/// @see net.minecraft.client.renderer.entity.FallingBlockRenderer
public class DragonEggRenderer extends EntityRenderer<HatchableDragonEggEntity, DragonEggRenderState> {
    /// Textures from 0 to 8 (inclusive) indicate unhatchable and the last one (9) indicates hatchable.
    protected final static float CRACK_PROGRESS_TO_STAGE = (ModelBakery.DESTROY_STAGE_COUNT - 1) / (1.0F - EGG_CRACK_THRESHOLD);
    protected final BlockModelResolver resolver;

    public DragonEggRenderer(EntityRendererProvider.Context context) {
        super(context);
        // 26.1 replaced BlockRenderDispatcher with BlockModelResolver.
        this.resolver = context.getBlockModelResolver();
    }

    @Override
    public void extractRenderState(HatchableDragonEggEntity egg, DragonEggRenderState state, float partialTicks) {
        super.extractRenderState(egg, state, partialTicks);
        state.progress = egg.getIncubationProgress();
        state.block = egg.asBlock(DMBlocks.ENDER_DRAGON_EGG.get()).defaultBlockState();
        this.resolver.update(state.blockModel, state.block, BlockDisplayContext.create());
        state.amplitude = egg.getAmplitude(partialTicks);
        if (state.amplitude != 0.0F) {
            state.axis = egg.getWobbleAxis();
            state.amplitude *= HALF_RAD_FACTOR;
        }
    }

    /// 26.1 has a purpose-built submitBreakingBlockModel for the crumbling decal, which replaces
    /// the SheetedDecalTextureGenerator wrapping this used to need.
    @Override
    public void submit(DragonEggRenderState state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
        matrices.pushPose();
        if (state.amplitude != 0.0F) {
            float sin = Mth.sin(state.amplitude);
            matrices.mulPose(new Quaternionf(
                    Mth.cos(state.axis) * sin,
                    0.0F,
                    Mth.sin(state.axis) * sin,
                    Mth.cos(state.amplitude)
            ));
        }
        matrices.translate(-0.5, 0.0, -0.5);
        state.blockModel.submit(matrices, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        if (state.progress >= EGG_CRACK_THRESHOLD) {
            collector.submitBreakingBlockModel(
                    matrices,
                    Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state.block),
                    0L,
                    Math.min((int) ((state.progress - EGG_CRACK_THRESHOLD) * CRACK_PROGRESS_TO_STAGE), 9)
            );
        }
        super.submit(state, matrices, collector, camera);
        matrices.popPose();
    }

    @Override
    public DragonEggRenderState createRenderState() {
        return new DragonEggRenderState();
    }
}
