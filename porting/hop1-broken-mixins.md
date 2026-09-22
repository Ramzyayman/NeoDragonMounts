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

---

# Verification (in-game, 1.21.5, NeoForge 21.5.98)

Tested against commit `bd36e02`.

| Behaviour | Result |
|---|---|
| Dragon scale shields block and take durability | PASS |
| Axe disables a blocking shield | not tested |
| Dragon core drops its essence exactly once | **PASS** |
| Tamed dragon's kills credit the owner | PASS |
| Dragon type line in item tooltips | PASS |
| Dragon spawn egg icons | FAIL - see below, not a port regression |

The dragon core case is the one that mattered. The core is a one-shot container:
`canPlaceItem` always returns false, and its closing animation ends in
`level.destroyBlock(pos, true)`, so it self-destructs. It is filled only by
`ServerDragonEntity#die` -> `spawnEssence`, and only when the dragon is tamed.

Confirmed drop path: `destroyBlock(pos, true)` -> `Block.dropResources` (there is no
`dragon_core` loot table, so nothing) -> `setBlock(air)` -> `preRemoveSideEffects`
(block entity still alive, drops contents) -> `removeBlockEntity` ->
`affectNeighborsAfterRemoval`. Exactly one drop, from the inherited hook.

Had `onRemove` been renamed to `affectNeighborsAfterRemoval` the obvious way, the
block-entity lookup would return null on this exact path and killing a tamed dragon
would have silently destroyed its essence, with no crash or log line.

## Known open issue: spawn egg icons

1.21.5 deleted the tinted-template spawn egg system outright:
`models/item/template_spawn_egg.json`, `textures/item/spawn_egg.png` and
`spawn_egg_overlay.png` are all gone, `SpawnEggItem` no longer carries colours, and
there is no replacement tint source. Every vanilla spawn egg now ships its own texture.

The mod's 17 dragon spawn eggs are defined purely as two constant tints over that
deleted template, and those model JSONs are committed under
`*/src/main/generated/assets/neodragonmounts/items/`. They break at 1.21.5 regardless
of the port; `DMModelProvider#generateSpawnEgg` regenerates the same dead reference.

Needs an art decision, not a code fix: either mod-authored base + overlay textures
plus a mod-side template model (keeps all 17 tints), or 17 pre-tinted textures.
Copying Mojang's 1.21.4 textures would work but redistributes their assets.

## Unrelated upstream bug found and fixed

`sunlight.nbt` was corrupt in the repository since commit `101535e`: `.gitattributes`
had `* text eol=lf` and no `*.nbt binary` rule, so committing it stripped nine CR
bytes and destroyed the gzip stream. It crashed worldgen on 1.21.4 as well - the empty
template produced a zero-size bounding box and `Mth.randomBetweenInclusive(random, 2, 0)`
threw, killing chunk generation. Recovered from `6639f61`, reapplied the
`dragonmounts.plus:` -> `neodragonmounts:` rename with corrected NBT length prefixes,
and added `*.nbt binary`. Worth reporting upstream.
