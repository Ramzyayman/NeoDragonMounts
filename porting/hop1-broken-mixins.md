# Hop 1 (1.21.4 → 1.21.5) — mixins that compile but are broken

All three modules build green at 1.21.5. These five do **not** fail compilation — they
emit warnings during `:fabric:compileJava` and would fail at runtime, either by crashing
on mixin apply or by silently not applying. They are the reason "it builds" is not the
same as "it works" for this port.

The same mixins exist in the `neoforge` module and need identical treatment; NeoForge's
compile does not surface these warnings, so they must be checked by hand there.

## 1. AxeItemMixin — target method renamed

`AxeItem.playerHasShieldUseIntent` → **`playerHasBlockingItemUseIntent`** (1.21.5).

Straight rename in the `@ModifyExpressionValue(method = ...)` target.

## 2. LivingEntityMixin — two `@Shadow` fields changed

| 1.21.4 | 1.21.5 |
|---|---|
| `protected Player lastHurtByPlayer` | `protected EntityReference<Player> lastHurtByPlayer` |
| `protected int lastHurtByPlayerTime` | `protected int lastHurtByPlayerMemoryTime` |

The type change matters: any read of `lastHurtByPlayer` must now resolve the reference
rather than use it as a `Player` directly.

## 3. ItemStackMixin — `@Shadow` signature changed

```java
// 1.21.4 (private)
private <T extends TooltipProvider> void addToTooltip(
        DataComponentType<T>, Item.TooltipContext, Consumer<Component>, TooltipFlag)

// 1.21.5 (public, gained TooltipDisplay)
public <T extends TooltipProvider> void addToTooltip(
        DataComponentType<T>, Item.TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)
```

Also note `TooltipProvider` moved to `net.minecraft.world.item.component`.
Since the method is now `public`, the `@Shadow` may be droppable entirely.

## 4 & 5. PlayerEntityMixin — the shield mixin has no target left

**This one is a design change, not a rename.**

`Player.hurtCurrentlyUsedShield` is **gone entirely from 1.21.5** — confirmed absent from
the whole `net.minecraft` tree, not merely moved.

The mod used it to make `DragonScaleShieldItem` behave like a vanilla shield:

```java
@ModifyExpressionValue(method = "hurtCurrentlyUsedShield",
        at = @At(value = "INVOKE", target = "…ItemStack;is(…Item;)Z"))
public boolean isShield(boolean original) {
    return original || this.useItem.getItem() instanceof DragonScaleShieldItem;
}
```

1.21.5 made shields data-driven via the **`BlocksAttacks`** component
(`DataComponents.BLOCKS_ATTACKS`, `net.minecraft.world.item.component.BlocksAttacks`).

The fix is to give the dragon scale shields that component in `DMItems`
(`makeDragonScaleShield`) and **delete the mixin**. That is a net simplification — the
same direction as deleting `CoreShadersMixin`. It needs a real decision about the
component's values (block delay, damage reductions, disable sound), so it should not be
guessed; compare against vanilla `Items.SHIELD`.

## Why these matter

A mixin that cannot find its target is the failure mode the compiler cannot catch. Of the
five, only the shield one changes behaviour visibly in a way a player would notice
immediately (dragon scale shields would stop blocking). The `LivingEntity` ones affect
loot/XP attribution and would be easy to miss for a long time.

**Do not mark hop 1 complete until these are resolved and the mod launches.**
