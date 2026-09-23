package net.dragonmounts.neo.common.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dragonmounts.neo.common.block.entity.DragonCoreBlockEntity;
import net.dragonmounts.neo.common.client.model.DragonCoreModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Set;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

/// @see net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer
@SuppressWarnings("UnstableApiUsage")
@NotNullByDefault
public class DragonCoreRenderer implements BlockEntityRenderer<DragonCoreBlockEntity, DragonCoreRenderState> {
    private static final ResourceLocation TEXTURE_LOCATION = makeId("textures/block/dragon_core.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE_LOCATION);
    final DragonCoreModel model;

    public DragonCoreRenderer(BlockEntityRendererProvider.Context context) {
        this(context.entityModelSet());
    }

    public DragonCoreRenderer(EntityModelSet models) {
        this.model = new DragonCoreModel(models.bakeLayer(ModelLayers.SHULKER_BOX));
    }

    @Override
    public DragonCoreRenderState createRenderState() {
        return new DragonCoreRenderState();
    }

    /// 1.21.9 split rendering into extract and submit. Everything the submit phase needs is
    /// copied off the block entity here, because submit no longer sees it.
    @Override
    public void extractRenderState(
            DragonCoreBlockEntity core,
            DragonCoreRenderState state,
            float partialTick,
            Vec3 cameraPosition,
            @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(core, state, partialTick, cameraPosition, breakProgress);
        state.facing = core.getBlockState().getValueOrElse(HORIZONTAL_FACING, Direction.SOUTH);
        state.progress = core.getProgress(partialTick);
    }

    @Override
    public void submit(DragonCoreRenderState state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
        this.submit(matrices, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.facing, state.progress, state.breakProgress, 0);
    }

    /// Shared by the block-entity path and the item (`Special`) path, as before - only the sink
    /// changed from a MultiBufferSource to a SubmitNodeCollector.
    public void submit(
            PoseStack matrices,
            SubmitNodeCollector collector,
            int packedLight,
            int packedOverlay,
            Direction facing,
            float progress,
            @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
            int outlineColor
    ) {
        matrices.pushPose();
        matrices.translate(0.5F, 0.5F, 0.5F);
        matrices.scale(0.9995F, 0.9995F, 0.9995F);
        matrices.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        matrices.scale(1.0F, -1.0F, -1.0F);
        matrices.translate(0.0F, -1.0F, 0.0F);
        collector.submitModel(
                this.model,
                progress,
                matrices,
                RENDER_TYPE,
                packedLight,
                packedOverlay,
                -1,
                null,
                outlineColor,
                crumblingOverlay
        );
        matrices.popPose();
    }

    public record Special(
            DragonCoreRenderer renderer,
            float openness,
            Direction facing
    ) implements NoDataSpecialModelRenderer {
        @Override
        public void submit(
                ItemDisplayContext context,
                PoseStack matrices,
                SubmitNodeCollector collector,
                int packedLight,
                int packedOverlay,
                boolean hasFoil,
                int outlineColor
        ) {
            this.renderer.submit(matrices, collector, packedLight, packedOverlay, this.facing, this.openness, null, outlineColor);
        }

        /// New abstract method in 1.21.6 - reports the model's transformed extents so the GUI
        /// can size the item. Mirrors vanilla ChestSpecialRenderer: pose the model, then collect.
        @Override
        public void getExtents(Set<Vector3f> output) {
            var model = this.renderer.model;
            model.setupAnim(this.openness);
            model.root().getExtentsForGui(new PoseStack(), output);
        }
    }

    public record Unbaked(float openness, Direction facing) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("openness", 0.0F).forGetter(Unbaked::openness),
                Direction.CODEC.optionalFieldOf("facing", Direction.UP).forGetter(Unbaked::facing)
        ).apply(instance, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }

        /// 1.21.9 replaced the bare EntityModelSet parameter with a BakingContext that also
        /// carries the material set and skin cache.
        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new Special(new DragonCoreRenderer(context.entityModelSet()), this.openness, this.facing);
        }
    }
}
