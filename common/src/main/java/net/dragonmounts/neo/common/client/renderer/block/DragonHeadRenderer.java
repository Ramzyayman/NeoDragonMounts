package net.dragonmounts.neo.common.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dragonmounts.neo.common.block.DragonHeadBlock;
import net.dragonmounts.neo.common.block.entity.DragonHeadBlockEntity;
import net.dragonmounts.neo.common.client.variant.VariantAppearance;
import net.dragonmounts.neo.common.init.DragonVariants;
import net.dragonmounts.neo.common.item.DragonHeadItem;
import net.dragonmounts.neo.compat.registry.DragonVariant;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Set;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

@SuppressWarnings("UnstableApiUsage")
@NotNullByDefault
public enum DragonHeadRenderer
        implements BlockEntityRenderer<DragonHeadBlockEntity, DragonHeadBlockRenderState>,
        BlockEntityRendererProvider<DragonHeadBlockEntity, DragonHeadBlockRenderState> {
    INSTANCE;

    /// 1.21.9 replaced ModelPart#render against a MultiBufferSource with
    /// SubmitNodeCollector#submitModelPart. The two passes (base, then full-bright glow) are
    /// unchanged - only the sink differs.
    public static void submitHead(
            ModelPart head,
            VariantAppearance appearance,
            PoseStack matrices,
            SubmitNodeCollector collector,
            double offsetX,
            double offsetY,
            double offsetZ,
            int light,
            int overlay,
            @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
            int outlineColor
    ) {
        matrices.pushPose();
        matrices.translate(offsetX, offsetY, offsetZ);
        matrices.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModelPart(head, matrices, appearance.getBase(null), light, overlay, null, false, false, -1, crumblingOverlay, outlineColor);
        collector.submitModelPart(
                head, matrices, appearance.getGlow(null), LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY,
                null, false, false, -1, crumblingOverlay, outlineColor
        );
        matrices.popPose();
    }

    @Override
    public DragonHeadBlockRenderState createRenderState() {
        return new DragonHeadBlockRenderState();
    }

    @Override
    public void extractRenderState(
            DragonHeadBlockEntity entity,
            DragonHeadBlockRenderState state,
            float partialTick,
            Vec3 cameraPosition,
            @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, cameraPosition, breakProgress);
        var blockState = entity.getBlockState();
        if (blockState.getBlock() instanceof DragonHeadBlock head) {
            state.variant = head.variant;
            state.animation = entity.getAnimation(partialTick);
            state.yRot = head.getYRotation(blockState);
            state.isOnWall = head.isOnWall;
            if (head.isOnWall) {
                state.facing = blockState.getValue(HORIZONTAL_FACING);
            }
        } else {
            state.variant = null;
        }
    }

    @Override
    public void submit(DragonHeadBlockRenderState state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
        var variant = state.variant;
        if (variant == null) return;
        var appearance = variant.appearance;
        var model = appearance.getModel(null);
        if (model == null) return;
        model.setupBlock(state.animation, state.yRot, 0.75F);
        if (state.isOnWall) {
            submitHead(
                    model.head, appearance, matrices, collector,
                    0.5 - state.facing.getStepX() * 0.25,
                    0.25,
                    0.5 - state.facing.getStepZ() * 0.25,
                    state.lightCoords, OverlayTexture.NO_OVERLAY, state.breakProgress, 0
            );
        } else {
            submitHead(
                    model.head, appearance, matrices, collector,
                    0.5, 0.0, 0.5,
                    state.lightCoords, OverlayTexture.NO_OVERLAY, state.breakProgress, 0
            );
        }
    }

    @Override
    public BlockEntityRenderer<DragonHeadBlockEntity, DragonHeadBlockRenderState> create(Context context) {
        return this;
    }

    public record Special(float animation, DragonVariant fallback) implements SpecialModelRenderer<DragonVariant> {
        /// New abstract method in 1.21.6. There is no variant argument here, so the extents are
        /// taken from the fallback variant's head - the same model submit() falls back to.
        @Override
        public void getExtents(Set<Vector3f> output) {
            var model = this.fallback.appearance.getModel(null);
            if (model == null) return;
            model.setupBlock(this.animation, 180.0F, 0.75F);
            model.head.getExtentsForGui(new PoseStack(), output);
        }

        @Override
        public void submit(
                @Nullable DragonVariant variant,
                ItemDisplayContext context,
                PoseStack matrices,
                SubmitNodeCollector collector,
                int packedLight,
                int packedOverlay,
                boolean hasFoil,
                int outlineColor
        ) {
            if (variant == null) {
                variant = this.fallback;
            }
            var appearance = variant.appearance;
            var model = appearance.getModel(null);
            if (model == null) return;
            model.setupBlock(this.animation, 180.0F, 0.75F);
            submitHead(model.head, appearance, matrices, collector, 0.5, 0.0, 0.5, packedLight, packedOverlay, null, outlineColor);
        }

        @Override
        public @Nullable DragonVariant extractArgument(ItemStack stack) {
            return stack.getItem() instanceof DragonHeadItem head ? head.variant : null;
        }
    }

    public record Unbaked(DragonVariant variant, float animation) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                DragonVariant.CODEC.optionalFieldOf("variant", DragonVariants.ENDER_FEMALE).forGetter(Unbaked::variant),
                Codec.FLOAT.optionalFieldOf("animation", 0.0F).forGetter(Unbaked::animation)
        ).apply(instance, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new Special(this.animation, this.variant);
        }
    }
}
