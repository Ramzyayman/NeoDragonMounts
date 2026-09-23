# Hop 4 (1.21.10 → 1.21.11) — notes

Expected to be the small hop. It was not.

## 1.21.11 is Mojang's unobfuscated release

Mojang shipped official names, and they are **not** the Mojmap names mods have been built
against. 687 classes disappeared and 899 appeared between 1.21.10 and 1.21.11.

That headline number overstates the damage. Core types are untouched — `Level`, `Entity`,
`ItemStack`, `BlockPos`, `CompoundTag` all kept their names and packages — so of the mod's
**529 distinct `net.minecraft` imports, only 19 were affected.**

Exactly one is a true class rename:

| 1.21.10 | 1.21.11 |
|---|---|
| `resources.ResourceLocation` | `resources.Identifier` |

229 usages across 66 files. The other 18 are package moves:

| 1.21.10 | 1.21.11 |
|---|---|
| `net.minecraft.Util` | `net.minecraft.util.Util` |
| `advancements.critereon.*` (8 classes) | `advancements.criterion.*` |
| `client.renderer.RenderType` | `client.renderer.rendertype.RenderType` |
| `world.entity.animal.Bee` | `world.entity.animal.bee.Bee` |
| `world.entity.animal.MushroomCow` | `world.entity.animal.cow.MushroomCow` |
| `world.entity.animal.SnowGolem` | `world.entity.animal.golem.SnowGolem` |
| `world.entity.animal.horse.*` (3 classes) | `world.entity.animal.equine.*` |
| `world.entity.monster.Bogged` | `world.entity.monster.skeleton.Bogged` |
| `world.level.GameRules` | `world.level.gamerules.GameRules` |

The method that produced this list is worth reusing at hop 5: dump the class list from the old
and new Minecraft jars, extract every `net.minecraft` import the mod actually uses, and diff. It
turns "everything was renamed" into a concrete list of 19 in about a minute.

## Real API changes, beyond renames

- **`RenderType` split.** The type stayed; every static factory (`entityCutoutNoCull`,
  `armorCutoutNoCull`, `armorEntityGlint`, `lines`, …) moved to a new `RenderTypes` class in the
  same package. Mechanical, but it cannot be done with a blanket rename — `RenderType` as a type
  must survive, so only `RenderType.<lowercase>` and `RenderType::` call sites move.
- **Game rules restructured.** `GameRules.Key<BooleanValue>` constants became `GameRule<Boolean>`,
  renamed from camelCase ids to snake_case: `RULE_DOMOBLOOT` ("doMobLoot") is now `MOB_DROPS`
  ("mob_drops"). `getGameRules().getBoolean(x)` became `getGameRules().get(x)`.
- **`SlotAccess.NULL` is gone.** `Entity#getSlot` returns `@Nullable SlotAccess` and defaults to
  `null`, so the sentinel comparison becomes a plain null check.
- **`Entity.hasImpulse` → `needsSync`.** Same field position, same two assignment sites.
- **`Button` is abstract now** and `AbstractButton#renderWidget` is final. Subclasses implement
  `protected void renderContents(GuiGraphics, int, int, float)` instead. `ToggleButton` gained the
  default `renderDefaultSprite` + `renderDefaultLabel` body that `Button` used to provide.
- **`Screen#resize(Minecraft, int, int)` → `resize(int, int)`**, which sets width/height and calls
  `repositionElements()`. The mod's override now delegates to `super` instead of re-running `init`.
- **`SpecialModelRenderer#getExtents(Set<Vector3f>)` → `getExtents(Consumer<Vector3fc>)`**, and
  `ModelPart#getExtentsForGui` took the same change.
- **`ResourceKey#location()` → `identifier()`**, following the `ResourceLocation` rename.
- **Command permissions.** `CommandSourceStack#hasPermission(int)` is gone; gating is now
  `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` (was level 2) and `LEVEL_ADMINS` (level 3).

## Debug rendering moved to gizmos

`DebugRenderer.SimpleDebugRenderer#render(PoseStack, MultiBufferSource, …)` became
`emitGizmos(double, double, double, DebugValueAccess, Frustum, float)`. There is no PoseStack and
no buffer source: shapes are submitted in **world space** via a static `Gizmos` API
(`Gizmos.cuboid(AABB, GizmoStyle)`, `Gizmos.point(Vec3, colour, size)`) and the gizmo system draws
them.

This made `DebugInfoRenderer` **shorter**. The whole VoxelShape conversion and the two cached
shape lists existed only to feed `renderVoxelShape`; `Gizmos` takes the `AABB` and `Vec3` directly.
The manual `-camX, -camY, -camZ` offsets are gone too, since gizmos are world-space.

`DebugRenderer#render` likewise became `emitGizmos(Frustum, double, double, double, float)`, and it
is called **once per frame** rather than once per pass — so the opaque/translucent guard added to
`DebugRendererMixin` at hop 3 is no longer needed and was removed.

## The build infrastructure is now the constraint

Loom 1.11-SNAPSHOT cannot set up Minecraft 1.21.11 (it fails on Fabric API 0.141.x with
"Javadoc provided by mod must have an intermediary source namespace"). The ceiling is tight:

| Loom | Requires |
|---|---|
| 1.13.2 | Gradle 8 ✓, Java 21 ✓ — **what this hop uses** |
| 1.14.3 – 1.15.2 | Gradle 9.2+ |
| 1.17.21 | Gradle 9.5+ |
| 1.18.2 | JVM 25+ |

Gradle 8.14.1 cannot itself run on the installed JDK 26 (buildSrc's Groovy rejects class file
major version 70), so "just use a newer JDK" is not available without also upgrading Gradle.

1.13.2 clears hop 4. **Hop 5 should assume a Gradle upgrade is part of the work**, alongside the
already-planned Java 21 → 25 move for 26.1.

## Fabric's access widener earned its keep again

`validateAccessWidener` failed on `RenderType$CompositeState`. Repointing it at the new
`rendertype` package did not help — the class was **removed from Minecraft entirely**. Nothing in
the mod referenced it, so the line was deleted rather than repaired. NeoForge's
`accesstransformer.cfg` never carried that entry, so there was nothing to fix on that side.

Third hop running where Fabric's validation surfaced something NeoForge would not have.

---

# Verification (1.21.11, NeoForge 21.11.45)

| Check | Result |
|---|---|
| `:common`, `:neoforge`, `:fabric` compile | PASS |
| Fabric `validateAccessWidener` | PASS (after removing a dead entry) |
| Both loader jars build | PASS |
| Client reaches main menu | PASS |
| **All 17 mixins applied**, zero `InvalidInjectionException` | PASS |
| Integrated server loads a world | PASS |
| Crashes / exceptions | none |

This is the strongest runtime check of any hop so far. `NoiseColumnMixin` and `TrialSpawnerMixin`
had never actually loaded in hops 1–3 (no noise column is sampled and no trial chamber exists at a
fresh spawn) and could only be verified by reading sources. Here the run got far enough into
worldgen that both applied for real.

## Open: GL framebuffer spam, unattributed

The run logs **3,207** `GL_INVALID_FRAMEBUFFER_OPERATION` errors, continuously from early startup
to the end. The same run at 1.21.10 logged **zero**, so it is new in 1.21.11.

What is known:
- The client renders a world normally and never crashes.
- The errors begin during initial resource/pipeline setup, before any mod entity or GUI is drawn.
- They are emitted from Mojang's `GlDebug` watcher thread, not from a mod call site.

What is **not** known: whether they come from the mod at all. Confirming that needs a vanilla
1.21.11 client with no mod loaded as a baseline, which has not been run. Do not assume this is
port damage, and do not assume it is harmless — it is simply unmeasured. Worth resolving before
1.21.11 is released to anyone.

## Caught after the fact: all 15 shields lost their models

1.21.11 now **rejects a model that samples more than one atlas**. `template_shield.json` drew
`#layer0` from the mod's shield texture (items atlas) and `#metal` from `block/anvil` (blocks
atlas), which every previous version tolerated:

```
IllegalStateException: Multiple atlases used in model,
expected minecraft:textures/atlas/items.png, but also got minecraft:textures/atlas/blocks.png
        at BlockModelWrapper.detectRenderType
```

All 15 dragon scale shields failed to bake and rendered as missing-texture placeholders.
`template_shield_blocking` inherits from `template_shield`, so it broke with it.

Fixed by authoring `neodragonmounts:item/shield_metal`, an original 16x16 metal texture on the
items atlas, and pointing `#metal` at it. Mojang's anvil texture was not copied. Verified: bake
failures went 15 -> 0.

A survey of every mod model referencing a `block/` path found no other cross-atlas case. The other
matches are model *parents* (`minecraft:block/cube_all`, `minecraft:block/dragon_egg`), and
`dragon_nest`'s `minecraft:block/magma` is inside a block model, so it stays on one atlas.

### Why this was missed

The hop 4 client run **did** log this, 15 times, before I called the hop verified. The startup
check only grepped for `InvalidInjectionException`, crashes and mixin-apply failures, so a
`WARN`-level `ModelBakery` / `ModelManager` message sailed past.

**Every future hop's launch check must also grep for:**

```
Unable to bake item model | No model loaded for default item model | Multiple atlases
```

Mixins applying cleanly says nothing about whether resources resolved. Both have to be checked,
and neither implies the other.

## Still needs in-game testing

Everything hop 3 listed still applies, since hop 4 touched the same rendering surface via the
`RenderType` → `RenderTypes` split. Additionally:

- **Buttons in the dragon inventory GUI** — `Button` became abstract and `renderWidget` final, so
  `ToggleButton` now supplies its own `renderContents`. Both the icon toggles and the text toggles
  should look and behave exactly as before.
- **Resizing the dragon inventory screen** — the `resize` override changed shape; the name field's
  contents must survive a window resize.
- **The `/dm` command permission gates** — level 2 and 3 checks moved to `Commands.hasPermission`.
- **Dragon death drops** — the `doMobLoot` game rule lookup changed class, name and call shape.
