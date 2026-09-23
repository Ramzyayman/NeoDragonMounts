# Hop 2 (1.21.5 → 1.21.8) — notes

Absorbs 1.21.6 and 1.21.7. All three modules build clean, zero warnings.

## The big change: ValueInput / ValueOutput

1.21.6 moved entity and block-entity serialization off `CompoundTag`:

| 1.21.5 | 1.21.6+ |
|---|---|
| `readAdditionalSaveData(CompoundTag)` | `(ValueInput)` |
| `addAdditionalSaveData(CompoundTag)` | `(ValueOutput)` |
| `loadAdditional(CompoundTag, Provider)` | `(ValueInput)` |
| `saveAdditional(CompoundTag, Provider)` | `(ValueOutput)` |

Most getter names carried over (`getStringOr`, `getIntOr`, `read(key, codec)`), but
`ValueInput` has **no** `Optional<Boolean> getBoolean` - only `getBooleanOr`. So the old
`if (contains(k)) setX(...)` pattern became `setX(input.getBooleanOr(k, <current value>))`,
passing the current field value so an absent key leaves it untouched.

`EntityUtil#saveWithoutId` / `#load` were added to bridge back to a real `CompoundTag`
where one is still required (item `ENTITY_DATA` components, the egg-hatch round trip,
`Player#setEntityOnShoulder`), using the same shape as vanilla `Entity#restoreFrom`.

## Save format: unchanged except one attachment

`DragonInventory` now uses vanilla `ItemStackWithSlot.CODEC` instead of the hand-rolled
`ArrayUtil#saveItems`. Verified identical on disk: `"Slot"` as an unsigned byte plus
`ItemStack.MAP_CODEC`, which is exactly what the old writer produced. `ArrayUtil#saveItems`
was deleted as dead.

**One deliberate format change.** NeoForge 21.8 replaced
`AttachmentType.Builder#serialize(Codec<T>)` with `serialize(MapCodec<T>)`, and a MapCodec
must write named fields. The flute attachment is therefore stored as `{flute: <stack>}`
where 21.5 wrote the ItemStack encoding bare. There is no way to express "bare value" as a
MapCodec, so this is forced by NeoForge's own API change rather than chosen.

Consequence: flute attachment data written by a pre-1.21.6 build will not read - NeoForge's
generated serializer throws on a failed parse. Not considered a practical problem because
this is a cross-MC-version port and mod attachment data does not survive such an upgrade
anyway, but it is a real break and should be stated in any release notes.

## Behaviour preserved deliberately

`PathNavigation#canNavigateGround` is new and abstract in 1.21.8. The obvious answer for a
walking mob is `true`, but 1.21.5's `GoalUtils#hasGroundPathNavigation` tested
`instanceof GroundPathNavigation`, and `DragonPathNavigation extends PathNavigation`
directly - so it had always been **false**. Returning `true` would have silently changed AI
goal selection for every dragon. Returns `false`.

## The catch: a stale access widener

Fabric's `validateAccessWidener` task failed on
`TrialSpawnerData#getOrCreateNextSpawnData` - 1.21.8 renamed the class to
`TrialSpawnerStateData`. NeoForge's `accesstransformer.cfg` carried the **identical** stale
entry and built without complaint, because NeoForge does not validate ATs at build time; it
would have failed only at runtime.

This is the mirror of hop 1, where only Fabric's compile surfaced broken mixin targets.
**Neither loader catches everything - both must be built, and Fabric's validation output is
worth reading even when the NeoForge build is green.**

## Other API moves

- `AreaEffectCloud#setParticle` → `setCustomParticle`
- `ServerPlayer#serverLevel()` → `level()` (now returns ServerLevel)
- `LivingEntity.ATTRIBUTES_FIELD` → `TAG_ATTRIBUTES`
- `Entity#canBeCollidedWith()` gained an `@Nullable Entity` parameter
- `NumericTag#getAsByte/getAsDouble/getAsInt` → `byteValue/doubleValue/intValue`
- `TagParser#parseTag` → `parseCompoundFully`
- `RenderType::guiTextured` → `RenderPipelines.GUI_TEXTURED`; `blitSprite` takes a `RenderPipeline`
- `SpecialModelRenderer#getExtents(Set<Vector3f>)` is new and abstract
- `PacketDistributor#sendToServer` → new `ClientPacketDistributor` class
- `TrialSpawner`'s normal/ominous config fields → a single `FullConfig` record
- Tag datagen: `IntrinsicTagAppender<T>` → `TagAppender<E, T>`; vanilla `ItemTagsProvider`
  and its `copy()` are gone (NeoForge supplies its own provider; the two block→item tag
  mirrorings are now explicit). Fabric renamed `getOrCreateTagBuilder` → `builder`
  (ResourceKey-typed) and added `valueLookupBuilder` (object-typed).

## What still needs testing

Hop 2 rewrote persistence, so the failure mode is silent data loss rather than visible
misbehaviour. See the save/load round trip in the testing section of the README/notes.

---

# Verification (in-game, 1.21.8, NeoForge 21.8.54)

| Behaviour | Result |
|---|---|
| Client reaches main menu, no mixin failures | PASS (after two fixes below) |
| Saddle persists across save/reload | **PASS** |
| Chest persists across save/reload | **PASS** |
| Name tag renames a dragon | PASS (needs anvil-naming first, as in vanilla) |

The saddle/chest result is the one that mattered: it proves the `ItemStackWithSlot.CODEC`
migration round-trips correctly, which was the main risk of the whole hop.

## Two runtime-only breakages a clean build did not catch

Both compiled with zero errors and zero warnings.

1. **PlayerEntityMixin handler signatures.** `Player#addAdditionalSaveData` and
   `#readAdditionalSaveData` now take `ValueOutput`/`ValueInput`. The bodies were migrated
   but the handler parameters were still `CompoundTag`. javac never checks a mixin handler
   against its target, so this died at class-load with `InvalidInjectionException`.

2. **CameraMixin injection point.** NeoForge's
   `ClientHooks#getDetachedCameraDistance` gained two parameters in 1.21.8
   (`vehicleEntityScale`, `vehicleDistance`), so the `@At` descriptor matched nothing -
   "Scanned 0 target(s)". The method still exists; only its shape changed.

**Lesson for later hops:** a name-based mixin audit is not enough - it passes (2) happily.
An attempt to audit `@At` descriptors with `javap` produced 19 false positives (too narrow
a classpath, no superclass walking) and was discarded. Launching the client is currently
the only reliable check, and it must be done every hop, not just at the end.

## Pre-existing upstream bug, deliberately not changed

Fabric's `PlayerEntityMixin` writes armor-effect cooldowns to `"ForgeCaps"` but reads them
from `SERIALIZATION_KEY`, so they never persist on Fabric. Identical on `dev` at 1.21.4, so
it is not port damage. Both keys were preserved exactly rather than silently changing
behaviour inside a port commit. Worth reporting upstream alongside the sunlight.nbt
corruption.
