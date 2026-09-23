package net.dragonmounts.neo.common.client.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/// 1.21.9 made `Model` generic over its render state and moved `setupAnim` onto it, so the
/// old bespoke `animate(float)` becomes `setupAnim(Float)`. Mirrors vanilla
/// ShulkerBoxRenderer.ShulkerBoxModel, which is also a `Model<Float>` keyed on openness.
public class DragonCoreModel extends Model<Float> {
    public final ModelPart lid;

    public DragonCoreModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.lid = root.getChild("lid");
    }

    @Override
    public void setupAnim(Float progress) {
        super.setupAnim(progress);
        this.lid.setPos(0.0F, 24.0F - progress * 0.5F * 16.0F, 0.0F);
        this.lid.yRot = 270.0F * progress * (float) (Math.PI / 180.0);
    }
}
