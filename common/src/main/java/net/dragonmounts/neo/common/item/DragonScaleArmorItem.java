package net.dragonmounts.neo.common.item;

import net.dragonmounts.neo.common.api.DescribedArmorEffect;
import net.dragonmounts.neo.common.api.DragonTypified;
import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DragonScaleArmorItem extends Item implements DragonTypified {
    public final DragonType type;
    public final @Nullable DescribedArmorEffect effect;

    public DragonScaleArmorItem(DragonType type, @Nullable DescribedArmorEffect effect, ArmorType slot, Properties props) {
        super(props.humanoidArmor(type.material, slot).component(DMDataComponents.DRAGON_TYPE, type));
        this.type = type;
        this.effect = effect;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.ofNullable(this.effect);
    }

    @Override
    public DragonType getDragonType() {
        return this.type;
    }
}
