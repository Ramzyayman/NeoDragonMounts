# Hop 5 (1.21.11 → 26.1.2) — notes

The last hop, and the one where the build system was a bigger obstacle than the mod.

## The toolchain had to move first

Nothing compiled until four upgrades landed, each blocking the next:

| Component | From | To | Why |
|---|---|---|---|
| Gradle | 8.14.1 | 9.7.1 | 8.14.1 cannot run on a JDK new enough for Loom |
| foojay resolver | 0.8.0 | 1.0.0 | Gradle 9 removed `JvmVendorSpec.IBM_SEMERU` |
| moddev-gradle | 2.0.107 | 2.0.147 | 2.0.107 could not parse the `26.x` scheme; it silently fell back to 1.21.8 capabilities, then died on `preProcessJar has invalid tool: null` |
| Fabric Loom | 1.13.2 | 1.18.2 | 1.13.2 predates 26.1 |

Java moves 21 → 25: Mojang's manifest lists `javaVersion.majorVersion: 25` for 26.1.2.

**Parchment is gone for good.** No `parchment-26.1` artifact exists, and 26.1 ships Mojang's
official parameter names anyway, so `officialMojangMappings()` alone is correct. The dead
ParchmentMC repository declaration in `buildSrc` was left in place deliberately — it shares an
`exclusiveContent` block with the NeoForge maven, and removing the block would take that with it.

## Fabric cannot build on 26.1, for a reason this project cannot fix

Minecraft 26.1 is unobfuscated, so **Mojang publishes no mapping file at all**: 26.1.2's manifest
lists only `client` and `server`, where 1.21.11 still had `client_mappings` and `server_mappings`.
Fabric's `intermediary` and `yarn` both stop at **1.21.11**.

Four configurations were tried across two Loom versions:

| Attempt | Result |
|---|---|
| `officialMojangMappings()` | `Failed to find official mojang mappings for 26.1.2` |
| `noIntermediateMappings()` + `mappings loom.layered {}` | `NullPointerException: srcNamespace is null` |
| no `mappings` declaration | `Configuration 'mappings' has no dependencies` |
| Loom `1.18-SNAPSHOT` | identical to the first |

Loom requires a mappings artifact that does not exist for 26.1. Because `:fabric` fails during
*configuration*, it takes the whole build down — that is why `:common:compileJava` failed with zero
Java errors. `include('fabric')` is commented out in `settings.gradle` with a note saying exactly
when to restore it.

**26.1.2 is NeoForge-only until Loom supports an unobfuscated Minecraft.**

## The mod itself was the easy part

Only **5** of the mod's 490 `net.minecraft` imports were affected, despite 304 classes disappearing
between 1.21.11 and 26.1.2. Core types were untouched again.

| 1.21.11 | 26.1.2 |
|---|---|
| `client.gui.GuiGraphics` | `client.gui.GuiGraphicsExtractor` |
| `client.renderer.LightTexture` | `util.LightCoordsUtil` |
| `client.renderer.block.BlockRenderDispatcher` | removed — see below |
| `client.renderer.state.CameraRenderState` | `...state.level.CameraRenderState` |
| `world.level.block.FarmBlock` | `FarmlandBlock` |

### GUIs got the extract/submit split entities got in 1.21.9

`render(GuiGraphics, …)` became `extractRenderState(GuiGraphicsExtractor, …)` throughout:
`renderLabels`→`extractLabels`, `renderContents`→`extractContents`, `renderTooltip`→`extractTooltip`,
`drawString`→`text`, `renderImage`→`extractImage`. `AbstractContainerScreen#renderBg` has **no
replacement at all** — the background is drawn at the head of an `extractContents` override.
`imageWidth`/`imageHeight` became `final` and move into a five-argument `super(...)`.

### Render type naming inverted, which is a trap

`entityCutoutNoCull` is gone. `entityCutout` is now the **no-cull** variant and `entityCutoutCull`
is the explicit culled one. Verified through `EntityModel`'s default: 1.21.11 defaulted to
`entityCutoutNoCull`, 26.1.2 defaults to `entityCutout` — same role, renamed. A blind rename to
`entityCutoutCull` would have compiled and silently started culling back faces on every dragon.

### Special block models split from special item-model renderers

`BlockRenderDispatcher` and `renderSingleBlock` are gone. Blocks register a `BlockModel.Unbaked`
(baking to a `BlockModel` that feeds a `BlockModelRenderState`) through NeoForge's
`RegisterBlockModelsEvent`; items still register a `SpecialModelRenderer.Unbaked` codec through
`RegisterSpecialModelRendererEvent`. **These are two different events** with different signatures,
and `RegisterBlockModelsEvent#register` takes `(model, block)` — the reverse order.

Since `BlockModel.BakingContext` implements `SpecialModelRenderer.BakingContext`, one adapter
(`SpecialBlockModel`) lets the existing `Unbaked` records serve both roles instead of duplicating them.

The dragon egg's crumbling decal got *simpler*: `submitBreakingBlockModel(pose, model, seed,
progress)` replaces hop 3's custom geometry wrapped in a `SheetedDecalTextureGenerator`.

### Other API moves

- `Brain` became declarative: activities are supplied to `Brain.provider(...)` via an
  `ActivitySupplier`, and the imperative `addActivity(Activity, int, ImmutableList)` overloads are
  gone. `DragonAi` keeps its imperative style through two local shims reproducing exactly what the
  deleted overloads did — read from the 1.21.11 bodies, not inferred.
- `BehaviorControl` gained `getRequiredMemories()`.
- `Brain.Provider#makeBrain(Dynamic)` → `makeBrain(E, Brain.Packed)`; `Mob#brainProvider()` is gone.
- `PathType` renames, mapped by **matching malus values and ordinal position** rather than by name,
  because the names transpose easily: `DANGER_FIRE`→`FIRE_IN_NEIGHBOR` (8.0), `DAMAGE_FIRE`→`FIRE`
  (16.0), `DANGER_OTHER`→`DAMAGING_IN_NEIGHBOR` (8.0), `DAMAGE_OTHER`→`DAMAGING` (−1.0).
  Switch-case labels needed separate handling, since they are unqualified.
- `RecipeSerializer` is a **record**, not an interface. `Recipe#group()` and `#showNotification()`
  are abstract; `assemble` lost its `HolderLookup.Provider`; results are `ItemStackTemplate`.
- `Level.random` is protected (`getRandom()`); `Entity.fluidHeight` → `isInLiquid()`;
  `entity.getType().is(tag)` → `entity.is(tag)`; `ResourceKey#location()` → `identifier()`.
- `CommandSourceStack#hasPermission(int)` → `Commands.hasPermission(Commands.LEVEL_*)`.
- `ItemInput` is a record; `SpawnEggItem.byId` returns `Optional<Holder<Item>>`.
- `Material` lost its atlas parameter; `TextureMapping` works in `Material`, not raw identifiers.
- `smelting`/`blasting` gained a `CookingBookCategory`; `smoking`/`campfireCooking` did **not**.
- `BlocksAttacks#bypassedBy` is a `HolderSet`, so it cannot resolve at static-init — vanilla's own
  shield now uses `.delayedComponent(…, context -> …)` and this follows suit.

## The dissolve effect, finally

Deferred since hop 1 specifically so it would be written once. That paid off: 26.1 ships
`RenderTypes.entityCutoutDissolve(texture, mask)`, whose shader does the same mask-alpha threshold
discard the mod's own shader pair did. No custom pipeline was needed.

Two differences from the original, both deliberate:

1. **The comparison is inverted.** The mod discarded where `mask.a < threshold`, counting alpha *up*
   as the dragon eroded. Vanilla discards where `vertexColor.a < mask.a`, so alpha counts *down*.
   Porting the old calculation unchanged would have made dragons **assemble** instead of dissolve —
   and it would have compiled and looked deliberate.
2. **No emissive or translucent dissolve variant exists.** Vanilla ships only the cutout one, and its
   shader forces `faceVertexColor.a = 1.0` after the discard ("the dissolve effect entirely replaces
   translucency"). The glow pass uses the same render type and relies on being submitted at
   `FULL_BRIGHT`, which is what made it read as emissive in the first place.

Verify against the reference frames in `porting/reference/` before calling this done.

## A runtime-only failure that compiling could never catch

The first launch died during mod construction:

```
NullPointerException: Components not bound yet
  at Holder$Reference.components
  at ItemStack.<init>
  at DragonFood.<clinit>(DragonFood.java:136)
```

26.1 made `ItemStack`'s constructor read the item holder's components. `DragonFood` built three
`ItemStack`s in a static block, and `DMDataComponents` touches `DragonFood.CODEC` during mod
construction — so the class initialised before registries were bound and took the whole mod with it.

The table moved from `static { … }` to `public static void init()`, enqueued from `commonSetup`
alongside `DMItems::setup`.

The tempting fix — changing the record field from `Optional<ItemStack>` to `Optional<Item>` — was
rejected: `particles` is serialized through `ItemStack.CODEC` as `override_particles`, so that would
silently change the datapack format for anyone customising dragon food.

## Known regressions, both marked in code

- **Spawn-egg breeding is orphaned.** `SpawnEggItem#spawnOffspringFromSpawnEgg` became `static` and
  is called from `Mob` directly, so the mod's override no longer hooks anything — an instance method
  cannot even share the signature, so it had to be renamed. The logic is kept intact; restoring the
  behaviour needs a mixin on the static method.
- **Fabric is disabled.** See above.
