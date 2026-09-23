package net.dragonmounts.neo.mixin;

import net.dragonmounts.neo.common.client.debug.DebugInfoRenderer;
import net.dragonmounts.neo.config.ClientConfig;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// 1.21.11 moved debug drawing to gizmos: DebugRenderer#render became #emitGizmos, which submits
/// world-space shapes instead of drawing through a PoseStack. It is also called once per frame
/// rather than once per pass, so the opaque/translucent guard this used to need is gone.
@Mixin(DebugRenderer.class)
public abstract class DebugRendererMixin {
    @Inject(method = "emitGizmos", at = @At("HEAD"))
    public void emitExtraGizmos(Frustum frustum, double camX, double camY, double camZ, float partialTick, CallbackInfo info) {
        if (ClientConfig.INSTANCE.debug.get()) {
            DebugInfoRenderer.INSTANCE.emitGizmos(camX, camY, camZ, null, frustum, partialTick);
        }
    }
}
