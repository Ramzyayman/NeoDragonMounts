package net.dragonmounts.neo.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.dragonmounts.neo.common.client.debug.DebugInfoRenderer;
import net.dragonmounts.neo.config.ClientConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// 1.21.10 rebuilt the debug renderers: DebugRenderer#render gained a `translucent` flag and
/// splits its work over two lists, BrainDebugRenderer is no longer a public field on it, and brain
/// dumps now reach the client through DebugSubscriptions instead of a mod-pushed payload. Only the
/// mod's own box/point overlay is left to draw, and only on the opaque pass so it is not drawn twice.
@Mixin(DebugRenderer.class)
public abstract class DebugRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    public void renderExtraLayers(
            PoseStack matrices,
            Frustum frustum,
            MultiBufferSource.BufferSource buffers,
            double camX,
            double camY,
            double camZ,
            boolean translucent,
            CallbackInfo info
    ) {
        if (!translucent && ClientConfig.INSTANCE.debug.get()) {
            DebugInfoRenderer.INSTANCE.render(matrices, buffers, camX, camY, camZ, null, frustum);
        }
    }
}
