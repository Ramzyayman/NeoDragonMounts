package net.dragonmounts.neo.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.dragonmounts.neo.common.api.DynamicAttributeEntity;
import net.dragonmounts.neo.common.entity.dragon.ServerDragonEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /// 1.21.5 renamed `lastHurtByPlayerTime` to `lastHurtByPlayerMemoryTime` and changed
    /// `lastHurtByPlayer` from a `Player` to an `EntityReference<Player>`. Both shadows
    /// resolved to nothing beforehand, which mixin reports only as a warning.
    @Shadow
    protected int lastHurtByPlayerMemoryTime;

    @Shadow
    @Nullable
    protected EntityReference<Player> lastHurtByPlayer;

    @Shadow
    public abstract void setLastHurtByPlayer(Player player, int memoryTime);

    @ModifyExpressionValue(method = "<init>", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/attributes/DefaultAttributes;getSupplier(Lnet/minecraft/world/entity/EntityType;)Lnet/minecraft/world/entity/ai/attributes/AttributeSupplier;"
    ))
    public AttributeSupplier applyDynamicAttributes(AttributeSupplier original) {
        return this instanceof DynamicAttributeEntity ? ((DynamicAttributeEntity) this).getDynamicAttributes() : original;
    }

    /// Mirrors how vanilla now handles the equivalent tamed-wolf case in
    /// `LivingEntity#resolvePlayerResponsibleForDamage`: assign through the setter when an
    /// owner is present, otherwise clear the reference and its memory timer directly.
    @Inject(method = "resolvePlayerResponsibleForDamage", at = @At("HEAD"), cancellable = true)
    public void appendDragonTypifiedText(DamageSource source, CallbackInfoReturnable<Player> info) {
        if (source.getEntity() instanceof ServerDragonEntity dragon && dragon.isTame()) {
            if (dragon.getOwner() instanceof Player player) {
                this.setLastHurtByPlayer(player, 100);
                info.setReturnValue(player);
            } else {
                this.lastHurtByPlayer = null;
                this.lastHurtByPlayerMemoryTime = 0;
                info.setReturnValue(null);
            }
        }
    }
}
