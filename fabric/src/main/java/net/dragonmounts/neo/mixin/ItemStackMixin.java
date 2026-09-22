package net.dragonmounts.neo.mixin;

import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.common.item.EntityContainer;
import net.dragonmounts.neo.compat.registry.ArmorEffectSourceType;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    /// 1.21.5 moved the `Item#appendHoverText` call out of `getTooltipLines` and into
    /// `addDetailsToTooltip`, and threaded a `TooltipDisplay` through both it and
    /// `ItemStack#addToTooltip`. Injecting into `getTooltipLines` still compiles but no
    /// longer matches anything, so the target moved with it. The tooltip consumer is now a
    /// real parameter rather than a `@Local` capture.
    @Shadow
    public abstract <T extends TooltipProvider> void addToTooltip(
            DataComponentType<T> type, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag
    );

    @Shadow
    public abstract Item getItem();

    @Shadow
    public abstract DataComponentMap getComponents();

    @Inject(method = "addDetailsToTooltip", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V"
    ))//Append tooltip at a position before "additional tooltip"
    public void appendDragonTypifiedText(
            Item.TooltipContext context,
            TooltipDisplay display,
            @Nullable Player a,
            TooltipFlag flag,
            Consumer<Component> consumer,
            CallbackInfo info
    ) {
        if (this.getItem() instanceof EntityContainer<?>) return;
        var component = this.getComponents().get(DMDataComponents.ARMOR_EFFECT_SOURCE);
        if (component != null && component.getType() == ArmorEffectSourceType.BUILTIN) return;
        this.addToTooltip(DMDataComponents.DRAGON_TYPE, context, display, consumer, flag);
    }

    private ItemStackMixin() {}
}
