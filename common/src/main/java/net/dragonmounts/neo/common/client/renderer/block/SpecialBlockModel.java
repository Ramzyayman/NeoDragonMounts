package net.dragonmounts.neo.common.client.renderer.block;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import org.joml.Matrix4fc;

/// 26.1 split special *block* models from special item-model renderers. A block registers a
/// `BlockModel.Unbaked`, which bakes to a `BlockModel` that feeds a `BlockModelRenderState`,
/// while items still register a `SpecialModelRenderer.Unbaked` codec. The two describe the same
/// renderer, so this adapts one to the other rather than duplicating every Unbaked record.
///
/// `BlockModel.BakingContext` implements `SpecialModelRenderer.BakingContext`, so it passes straight
/// through.
public record SpecialBlockModel<T>(SpecialModelRenderer.Unbaked<T> unbaked) implements BlockModel.Unbaked {
    @Override
    public BlockModel bake(BlockModel.BakingContext context, Matrix4fc transformation) {
        var renderer = this.unbaked.bake(context);
        return (output, blockState, displayContext, seed) -> {
            if (renderer != null) {
                output.setupSpecialModel(renderer, transformation);
            }
        };
    }
}
