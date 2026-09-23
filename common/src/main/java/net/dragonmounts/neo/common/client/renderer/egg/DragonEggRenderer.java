package net.dragonmounts.neo.common.client.renderer.egg;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import net.dragonmounts.neo.common.entity.dragon.HatchableDragonEggEntity;
import net.dragonmounts.neo.common.init.DMBlocks;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
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
    protected final BlockRenderDispatcher dispatcher;

    public DragonEggRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.dispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void extractRenderState(HatchableDragonEggEntity egg, DragonEggRenderState state, float partialTicks) {
        super.extractRenderState(egg, state, partialTicks);
        state.progress = egg.getIncubationProgress();
        state.block = egg.asBlock(DMBlocks.ENDER_DRAGON_EGG.get()).defaultBlockState();
        state.amplitude = egg.getAmplitude(partialTicks);
        if (state.amplitude != 0.0F) {
            state.axis = egg.getWobbleAxis();
            state.amplitude *= HALF_RAD_FACTOR;
        }
    }

    /// 1.21.9 replaced the immediate render(...) with submissions. The block itself goes through
    /// submitBlock, but nothing on the collector carries a crumbling decal for a *block*, so the
    /// crack overlay is submitted as custom geometry against the destroy-stage render type and
    /// wrapped in the same SheetedDecalTextureGenerator this used before.
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
        collector.submitBlock(matrices, state.block, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        if (state.progress >= EGG_CRACK_THRESHOLD) {
            var block = state.block;
            int light = state.lightCoords;
            collector.submitCustomGeometry(
                    matrices,
                    ModelBakery.DESTROY_TYPES.get(Math.min((int) ((state.progress - EGG_CRACK_THRESHOLD) * CRACK_PROGRESS_TO_STAGE), 9)),
                    (pose, consumer) -> {
                        var decal = new SheetedDecalTextureGenerator(consumer, pose, 1.0F);
                        var stack = new PoseStack();
                        stack.last().set(pose);
                        this.dispatcher.renderSingleBlock(block, stack, type -> decal, light, OverlayTexture.NO_OVERLAY);
                    }
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
