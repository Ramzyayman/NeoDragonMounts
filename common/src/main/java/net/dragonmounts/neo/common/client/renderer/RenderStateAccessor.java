package net.dragonmounts.neo.common.client.renderer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/// ponytail: the masked dissolve pipeline is stubbed out for the duration of the
/// 1.21.4 -> 26.1 port. Rendering was rewritten in 1.21.5, again in 1.21.11 and again
/// in 26.1, so implementing it at each hop would mean writing the hardest code in the
/// mod four times. These fall back to vanilla render types instead: a dying dragon
/// simply disappears rather than eroding away. Everything else renders normally.
///
/// Rebuild the real pipeline ONCE, at 26.1. The full behavioural spec — data flow,
/// shader logic and acceptance criteria — is in porting/dissolve-effect-spec.md.
///
/// The `mask` parameter is deliberately retained and ignored so call sites stay
/// untouched and the restored signature is already in place.
public final class RenderStateAccessor {
    private RenderStateAccessor() {}

    public static RenderType entityCutoutDecal(ResourceLocation texture, ResourceLocation mask) {
        return RenderType.entityCutoutNoCull(texture);
    }

    public static RenderType entityTranslucentEmissiveDecal(ResourceLocation texture, ResourceLocation mask) {
        return RenderType.entityTranslucentEmissive(texture);
    }
}
