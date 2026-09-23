package net.dragonmounts.neo.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Cancellable;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.config.ClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract float getMaxZoom(float distance);

    @Shadow
    protected abstract void move(float x, float y, float z);

    /// 26.1 renamed Camera#setup to #alignWithEntity and dropped its Entity parameter, so the
    /// entity comes from the camera itself now rather than from a @Local argument.
    @Shadow
    public abstract @Nullable Entity entity();

    @ModifyExpressionValue(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/ClientHooks;getDetachedCameraDistance(Lnet/minecraft/client/Camera;ZFFFF)F"))
    public float detachedCameraOffset(float original, @Cancellable CallbackInfo info) {
        var host = this.entity();
        if (host != null && host.getVehicle() instanceof TameableDragonEntity) {
            this.move(-this.getMaxZoom(ClientConfig.INSTANCE.cameraDistance.getAsFloat()), 0.0F, -ClientConfig.INSTANCE.cameraOffset.getAsFloat());
            info.cancel();
        }
        return original;
    }

    private CameraMixin() {}
}
