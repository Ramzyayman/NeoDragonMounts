package net.dragonmounts.neo.common.client.renderer.dragon;

import com.mojang.blaze3d.vertex.PoseStack;
import net.dragonmounts.neo.common.client.model.dragon.DragonModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import static net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
import static net.minecraft.client.renderer.RenderType.armorCutoutNoCull;

public class TameableDragonLayer extends RenderLayer<DragonRenderState, DragonModel> {
    public TameableDragonLayer(RenderLayerParent<DragonRenderState, DragonModel> parent) {
        super(parent);
    }

    /// 1.21.9 turned rendering into submissions: layers push models into a SubmitNodeCollector and
    /// the geometry is written later, so every pass has to be an explicit submit and overlays go in
    /// their own order bucket the way vanilla's EyesLayer and HorseMarkingLayer do.
    @Override
    public void submit(PoseStack matrices, SubmitNodeCollector collector, int light, DragonRenderState state, float yRot, float xRot) {
        var appearance = state.variant.appearance;
        var model = appearance.getModel(state);
        int outline = state.outlineColor;
        if (!state.isInvisible) {
            if (state.deathTime > 0) {
                int color = ARGB.color(Math.min(Mth.floor(state.deathTime * 255.0F / state.maxDeathTime), 255), -1);
                collector.order(1).submitModel(
                        model, state, matrices, appearance.getDecal(state),
                        light, OverlayTexture.pack(0.0F, state.hurtTime > 0), color, null, outline, null
                );
                collector.order(2).submitModel(
                        model, state, matrices, appearance.getGlowDecal(state),
                        FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color, null, outline, null
                );
                return;
            }
            //glow
            collector.order(1).submitModel(
                    model, state, matrices, appearance.getGlow(state),
                    FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1, null, outline, null
            );
        }
        //saddle
        if (state.isSaddled) {
            collector.order(2).submitModel(
                    model.saddleOverlay, state, matrices, appearance.getSaddle(state),
                    light, OverlayTexture.NO_OVERLAY, -1, null, outline, null
            );
        }
        //chest
        if (state.hasChest) {
            matrices.pushPose();
            model.root().translateAndRotate(matrices);
            model.body.translateAndRotate(matrices);
            collector.order(3).submitModel(
                    model.chestOverlay, state, matrices, appearance.getChest(state),
                    light, OverlayTexture.NO_OVERLAY, -1, null, outline, null
            );
            matrices.popPose();
        }
        //armor
        var equippable = state.armor.get(DataComponents.EQUIPPABLE);
        if (equippable == null) return;
        var material = equippable.assetId();
        if (material.isEmpty()) return;
        var texture = appearance.getArmorTexture(material.get());
        if (texture == null) return;
        collector.order(4).submitModel(
                model, state, matrices, armorCutoutNoCull(texture),
                light, OverlayTexture.NO_OVERLAY, -1, null, outline, null
        );
        /// getArmorFoilBuffer is gone; 1.21.9 draws the glint as a second submit with
        /// RenderType#armorEntityGlint, the way vanilla's EquipmentLayerRenderer does.
        if (state.armor.hasFoil()) {
            collector.order(5).submitModel(
                    model, state, matrices, RenderType.armorEntityGlint(),
                    light, OverlayTexture.NO_OVERLAY, -1, null, outline, null
            );
        }
    }
}
