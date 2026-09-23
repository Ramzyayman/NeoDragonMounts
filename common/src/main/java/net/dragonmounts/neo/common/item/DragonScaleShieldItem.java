package net.dragonmounts.neo.common.item;

import net.dragonmounts.neo.common.api.DragonTypified;
import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.BlocksAttacks;

import java.util.List;
import java.util.Optional;

import static net.dragonmounts.neo.common.DragonMountsShared.ITEM_TRANSLATION_KEY_PREFIX;

public class DragonScaleShieldItem extends ShieldItem implements DragonTypified {
    public static final String TRANSLATION_KEY = ITEM_TRANSLATION_KEY_PREFIX + "dragon_scale_shield";
    public static final int UNIT_DURABILITY = 50;

    /// Blocking became data-driven in 1.21.5. Before that, three mixins existed purely to
    /// splice DragonScaleShieldItem into vanilla's hardcoded `is(Items.SHIELD)` checks:
    /// `Player#hurtCurrentlyUsedShield` (deleted upstream), `AxeItem#playerHasShieldUseIntent`
    /// (now `playerHasBlockingItemUseIntent`, which tests for this component instead), and the
    /// paired `isShield` injections. Carrying this component makes all of them unnecessary, so
    /// they were deleted rather than ported - see porting/hop1-broken-mixins.md.
    ///
    /// Values are copied verbatim from vanilla `Items.SHIELD`, because the mixins they replace
    /// made these shields behave exactly like a vanilla shield. Durability and repair material
    /// stay per-dragon-type, as before.

    public final DragonType type;

    public DragonScaleShieldItem(DragonType type, Properties props) {
        super(props.component(DMDataComponents.DRAGON_TYPE, type)
                .durability(UNIT_DURABILITY * type.material.durability())
                .repairable(type.material.repairIngredient())
                .equippableUnswappable(EquipmentSlot.OFFHAND)
                /// 26.1 made BlocksAttacks#bypassedBy a HolderSet instead of a TagKey, so the tag can
                /// only be resolved once registries exist - hence delayedComponent, as vanilla's own
                /// shield now does. Values still copied verbatim from Items.SHIELD.
                .delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(
                        0.25F,
                        1.0F,
                        List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
                        new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
                        Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
                        Optional.of(SoundEvents.SHIELD_BLOCK),
                        Optional.of(SoundEvents.SHIELD_BREAK)
                ))
                .component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK)
        );
        this.type = type;
    }

    @Override
    public DragonType getDragonType() {
        return this.type;
    }
}
