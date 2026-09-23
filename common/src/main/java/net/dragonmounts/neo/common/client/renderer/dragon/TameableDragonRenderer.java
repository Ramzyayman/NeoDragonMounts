package net.dragonmounts.neo.common.client.renderer.dragon;

import com.mojang.blaze3d.vertex.PoseStack;
import net.dragonmounts.neo.common.client.ClientDragonEntity;
import net.dragonmounts.neo.common.client.model.dragon.BuiltinFactory;
import net.dragonmounts.neo.common.client.model.dragon.DragonModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.client.renderer.entity.EnderDragonRenderer.submitCrystalBeams;

public class TameableDragonRenderer extends MobRenderer<ClientDragonEntity, DragonRenderState, DragonModel> {
    public TameableDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new DragonModel(context.getModelSet().bakeLayer(BuiltinFactory.NORMAL.location)), 2);
        this.addLayer(new TameableDragonLayer(this));
    }

    @Override
    public void extractRenderState(ClientDragonEntity dragon, DragonRenderState state, float partialTick) {
        super.extractRenderState(dragon, state, partialTick);
        dragon.animator.extractRenderState(state, partialTick);
    }

    /// 1.21.9 replaced the immediate render(...) call with a two-phase submission model:
    /// renderers now push work into a SubmitNodeCollector instead of writing to a
    /// MultiBufferSource, and the light value comes off the render state rather than a
    /// parameter. Mirrors vanilla EndCrystalRenderer#submit.
    @Override
    public void submit(DragonRenderState state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
        this.model = state.variant.appearance.getModel(state);
        if (state.renderCrystalBeams && state.crystal != null) {
            matrices.pushPose();
            var crystal = state.crystal;
            submitCrystalBeams(
                    (float) (crystal.x - state.x),
                    (float) (crystal.y - state.y),
                    (float) (crystal.z - state.z),
                    state.ageInTicks,
                    matrices,
                    collector,
                    state.lightCoords
            );
            matrices.popPose();
        }
        super.submit(state, matrices, collector, camera);
    }

    @Override
    protected void scale(DragonRenderState state, PoseStack matrices) {
        super.scale(state, matrices);
        float scale = state.ageScale;
        matrices.scale(scale, scale, scale);
        matrices.translate(0.0F, state.offsetY, -1.5F);
    }

    @Override
    public Identifier getTextureLocation(DragonRenderState state) {
        return state.variant.appearance.getBodyTexture(state);
    }

    @Override
    public DragonRenderState createRenderState() {
        return new DragonRenderState();
    }

    @Override
    protected @Nullable RenderType getRenderType(DragonRenderState state, boolean visible, boolean translucent, boolean glowing) {
        // During death, do not use the standard rendering and let the death layer handle it. Hacky, but better than mixins.
        return state.deathTime > 0 ? null : super.getRenderType(state, visible, translucent, glowing);
    }

    @Override
    protected float getFlipDegrees() {
        return 0.0F; // dragons dissolve during death, not flip.
    }
}
