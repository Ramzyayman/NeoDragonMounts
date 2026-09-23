# Hop 3 (1.21.8 → 1.21.10) — notes

Absorbs 1.21.9. All three modules build clean.

## The big change: Feature Submissions (1.21.9)

Rendering split into **extract** and **submit** phases. Renderers no longer write to a
`MultiBufferSource`; they push work into a `SubmitNodeCollector` and the geometry is written
later, in a batch, by `FeatureRenderDispatcher`.

| 1.21.8 | 1.21.10 |
|---|---|
| `EntityRenderer#render(S, PoseStack, MultiBufferSource, int)` | `#submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)` |
| `RenderLayer#render(PoseStack, MultiBufferSource, int, S, float, float)` | `#submit(PoseStack, SubmitNodeCollector, int, S, float, float)` |
| `BlockEntityRenderer<T>` | `BlockEntityRenderer<T, S extends BlockEntityRenderState>` |
| `Model` (raw) | `Model<S>`, with `setupAnim(S)` on it |
| `model.renderToBuffer(...)` | `collector.submitModel(model, state, ...)` |
| `part.render(...)` | `collector.submitModelPart(part, ...)` |
| `ItemRenderer.getArmorFoilBuffer(...)` | second `submitModel` with `RenderType.armorEntityGlint()` |
| `TextureSheetParticle` | `SingleQuadParticle` + `getLayer()` |

### The trap: deferred draw breaks visibility toggling

`ModelFeatureRenderer#renderModel` calls `model.setupAnim(submit.state())` **immediately
before** `renderToBuffer`, and `Model#renderToBuffer` is `final`. So the old layer idiom

```java
saddle.visible = true;
model.renderToBuffer(...);   // immediate
saddle.visible = false;
```

silently draws nothing: by the time anything is written the flag is back to whatever the
last `setupAnim` left. Model submits are also all flushed *before* part submits, so nothing
can flip a flag in between either.

Vanilla's answer is a separate `Model` instance per overlay (see `SimpleEquipmentLayer`'s
`adultModel`/`babyModel`) — there are now **zero** `.visible =` assignments in any vanilla
render layer. `DragonModel` follows suit: `setupAnim` hides saddle and chest, and two small
`Model<DragonRenderState>` instances over the same baked parts reveal one each in their own
`setupAnim`. Which model was submitted decides what is visible when it is finally drawn.

The chest overlay is rooted at the `chest` part itself, so drawing it is exactly the old
`chest.render(...)`; the layer still supplies the body transform. This is not cosmetic:
`chest.png` is a sparse overlay whose opaque pixels sit at (192,132)-(255,155), and a scan
of all 76 boxes in `ModelFactory`/`BuiltinFactory` found another part with UVs at
(128,112)-(254,170) overlapping that window — so submitting the whole model with the chest
texture would have painted chest pixels onto it. The saddle stays a whole-model pass because
`saddle.png` has opaque pixels from x=45 while the saddle part's UVs only start at x=184,
i.e. it deliberately paints straps onto other parts.

## Other big API moves

### `ENTITY_DATA` is `TypedEntityData` now
`DataComponents.ENTITY_DATA` changed from `CustomData` to `TypedEntityData<EntityType<?>>`,
which carries the entity type beside the tag instead of leaving an `"id"` key for readers to
re-parse. `CustomData#parseEntityType`, `#loadInto` and `#read(MapDecoder)` are all gone.
`EntityContainer#simplifyData` now takes the type explicitly (compiler-enforced, no registry
round trip), and `saveEntityData` takes the `Entity` and does its own `saveWithId`.

### `SpawnEggItem` lost its `EntityType` parameter
The type now travels in the item's `ENTITY_DATA` component — `Properties#spawnEgg(type)` —
which is also what registers the item in `SpawnEggItem.BY_ID`. `getType`/`spawnsEntity` no
longer take a `HolderLookup.Provider`.

### Debug rendering was rebuilt
`DebugPackets` is deleted; entities implement `DebugValueSource` and `Mob#registerDebugValues`
publishes paths, goals and brains through `DebugSubscriptions`. `DebugPacketsMixin` is
therefore deleted on both loaders — `Mob` already does what it forced. `DebugRenderer#render`
gained a `translucent` flag and splits over two renderer lists, and `brainDebugRenderer` is no
longer a public field, so `DebugRendererMixin` now only draws the mod's own box/point overlay,
guarded to the opaque pass so it is not drawn twice.

### Smaller moves
- `GameProfile` → `NameAndId` (`GameProfileArgument.getGameProfiles`, `PlayerList#isOp`,
  `Player#nameAndId()`); `getId()`/`getName()` → `id()`/`name()`
- `Player#setEntityOnShoulder` moved down to `ServerPlayer`
- `Entity#getServer()` removed — go through `level().getServer()`
- `Container#startOpen/stopOpen(Player)` → `(ContainerUser)`; `ContainerUser#getLivingEntity()`
- `BlockBehaviour#getAnalogOutputSignal` gained the reading `Direction`
- `BeehiveBlock#dropHoneycomb(Level, BlockPos)` → the full block-interact loot context
  `(ServerLevel, ItemStack, BlockState, BlockEntity, Entity, BlockPos)`
- `EntityType` constructor gained `allowedInPeaceful`
- `EntityReference` constructors are private — use `EntityReference.of(uuid)`
- `ParticleTypes.DRAGON_BREATH` is a `ParticleType<PowerParticleOption>`; wrap with
  `PowerParticleOption.create(type, 1.0F)`
- `ParticleProvider#createParticle` gained a `RandomSource`
- `GuiEventListener#keyPressed(int,int,int)` → `keyPressed(KeyEvent)` (a record)
- `LocalPlayer#clientLevel` removed — `level()` carries `playLocalSound`
- `KeyMapping` categories are registered `KeyMapping.Category` records keyed by a
  `ResourceLocation`; `ToggleKeyMapping` gained a `shouldRestore` flag
- `FMLLoader.getDist()` is an instance method — `FMLLoader.getCurrent().getDist()`
- `SimpleDebugRenderer#render` gained `DebugValueAccess` and `Frustum`

## Lang change

The key-binding category label is derived from its `ResourceLocation` now, so
`key.categories.neodragonmounts` became `key.category.neodragonmounts.dragon_mounts`.
Renamed in all four lang files.

## What still needs testing

Hop 3 is almost entirely a *rendering* hop, so the failure mode is visible rather than
silent — but only if you look at the right things. See the verification table below.

---

# Verification (1.21.10, NeoForge 21.10.64)

| Check | Result |
|---|---|
| `:common`, `:neoforge`, `:fabric` compile | PASS |
| Fabric `validateAccessWidener` | PASS |
| Both loader jars build | PASS |
| Client reaches main menu, no `InvalidInjectionException` | PASS |
| All 6 client mixins applied (incl. the two rewritten) | PASS |
| Dedicated server generates a world and reaches `Done` | PASS |

`NoiseColumnMixin` and `TrialSpawnerMixin` never loaded in either run (no noise column is
sampled at a fresh spawn; no trial chamber exists there). Both are pure `@Shadow` mixins with
no `@At` descriptors, so their only failure mode is a missing member — which mixin checks at
apply time. Their shadowed members were verified against 1.21.10 sources instead:
`NoiseColumn.column`/`minY`, `TrialSpawner.data`/`config`/`setState`,
`TrialSpawner.FullConfig(normal, ominous, targetCooldownLength, requiredPlayerRange)` and
`TrialSpawnerStateData#reset` all still exist with the same shapes.

## In-game testing: PASS

Played on 1.21.10 / NeoForge 21.10.64, no issues found across the list below. That matters more
than usual here: the saddle/chest overlay-model rework is the approach hops 4 and 5 build on, so
it is now confirmed before it gets carried forward rather than after.

Checked:

- **Saddle on a dragon** — the overlay-model rework. If the split is wrong the saddle is
  either invisible or smeared over the whole body in the body pass.
- **Chest on a dragon** — same rework, and the case where a wrong call would have painted
  chest pixels onto an unrelated part.
- **Dragon armour**, including an enchanted (foil) set — the glint is a separate submit now.
- **Dragon head block** (wall and floor) and the **dragon head worn as a helmet** — the
  block-entity renderer and `CustomHeadLayerMixin` both changed.
- **Dragon core** block and its item form — new render state + `submitModel`.
- **Dragon egg** past the crack threshold — the crumbling decal is now custom geometry.
- **Breath particles** — `SingleQuadParticle` migration.
- **Key bindings** appear under the renamed category in Controls.

Still outstanding, unchanged by this hop and expected: the **death dissolve** remains stubbed to
vanilla render types (rebuilt at 26.1 from `dissolve-effect-spec.md`), and **spawn egg artwork**
is still the open decision from hop 1.
