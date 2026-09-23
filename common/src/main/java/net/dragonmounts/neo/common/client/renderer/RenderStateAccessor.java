package net.dragonmounts.neo.common.client.renderer;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/// The masked dissolve effect, stubbed out from 1.21.5 through 1.21.11 and rebuilt here once, as
/// planned. It turned out not to need a custom pipeline at all: 26.1 ships
/// `RenderTypes.entityCutoutDissolve(texture, mask)`, whose shader does the same mask-alpha
/// threshold discard the mod's own shader pair used to do.
///
/// Two differences from the original, both deliberate:
///
/// 1. **The comparison is inverted.** The mod's shader discarded where `mask.a < threshold`, so
///    alpha counted *up* as the dragon eroded. Vanilla discards where `vertexColor.a < mask.a`,
///    so alpha has to count *down*. `TameableDragonLayer` inverts it; porting the old calculation
///    unchanged would make dragons assemble instead of dissolve.
/// 2. **There is no emissive or translucent dissolve variant** - vanilla ships only the cutout one,
///    and its shader forces `faceVertexColor.a = 1.0` after the discard because "the dissolve
///    effect entirely replaces translucency". The glow pass therefore uses the same render type and
///    relies on being submitted at FULL_BRIGHT, which is what made it look emissive anyway.
public interface RenderStateAccessor {
    static RenderType entityCutoutDecal(Identifier texture, Identifier mask) {
        return RenderTypes.entityCutoutDissolve(texture, mask);
    }

    static RenderType entityTranslucentEmissiveDecal(Identifier texture, Identifier mask) {
        return RenderTypes.entityCutoutDissolve(texture, mask);
    }
}
