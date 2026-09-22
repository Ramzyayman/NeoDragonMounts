# Dissolve effect — behavioural spec

Captured from the working **1.21.4** build (`dev` @ `acba4bc`) so the effect can be
rebuilt at 26.1 without a working reference to compare against.

This is the one part of the port that the compiler cannot verify. Everything below
was read out of the 1.21.4 source and confirmed against video of the live build.

## What it is

A threshold dissolve that plays **only on death**. The body erodes away in an
irregular, hard-edged noise pattern — holes open in the torso, wings and tail, the
world shows through them, and the silhouette breaks into disconnected fragments
before vanishing. Roughly **3 seconds** end to end.

Not a fade. Alpha never ramps; fragments are either drawn or discarded.

Scope is narrower than it looks: `getDecal` / `getGlowDecal` have exactly one call
site, inside `if (state.deathTime > 0)` in `TameableDragonLayer`. Egg hatching does
*not* use this path — `DragonEggRenderer` never references it, so whatever the hatch
visual is, it is unrelated and unaffected.

Note the death branch `return`s early, so saddle, chest and armour layers are
deliberately **not** drawn while a dragon is dissolving. Preserve that.

## Data flow

1. `TameableDragonRenderer#getRenderType` returns `null` while `deathTime > 0`, which
   suppresses normal rendering entirely — the layer below takes over.
2. `TameableDragonLayer` encodes progress into the **vertex colour alpha**:
   ```java
   int color = ARGB.color(Math.min(Mth.floor(state.deathTime * 255.0F / state.maxDeathTime), 255), -1);
   ```
   RGB is white (`-1`); only alpha carries signal.
3. Vertex shader lifts it straight out: `threshold = Color.a;`
4. Fragment shader discards against the mask:
   ```glsl
   if (texture(Sampler3, texCoord0).a < threshold) discard;
   vec4 color = texture(Sampler0, texCoord0);
   if (color.a < 0.1) discard;
   ```
   - `Sampler0` = the variant's body or glow texture
   - `Sampler3` = `textures/entity/dragon/dissolve.png` — its **alpha channel** encodes
     the erosion order, sampled at the *model's own UV*, so the pattern is fixed to the
     mesh rather than screen space.
5. `EMISSIVE` define skips the lightmap multiply so the glow layer stays bright:
   ```glsl
   #ifndef EMISSIVE
       vertexColor *= texelFetch(Sampler2, UV2 / 16, 0);
   #endif
   ```

Two render types share the one shader pair:

| Render type | Layer | Transparency | Depth write |
|---|---|---|---|
| `entity_cutout_decal` | base body texture | `NO_TRANSPARENCY` | yes |
| `entity_translucent_emissive_decal` | glow texture | `TRANSLUCENT_TRANSPARENCY` | no (`COLOR_WRITE`) |

Both: `NEW_ENTITY` format, `QUADS`, buffer 1536, `NO_CULL`, `OVERLAY`.

## What survives the port

Steps 1–2 are ordinary Java against the render state. They are **not** API-coupled and
should carry through every hop unchanged.

Steps 3–5 are the shader, and that whole mechanism is gone from 1.21.5 onward.

## What has to be rebuilt at 26.1

- `DMCoreShaders` — `ShaderProgram` + `ShaderDefines` → `RenderPipeline.builder()` / `.register()`
- `RenderStateAccessor` — `RenderType.create` + `CompositeState.builder()` → pipeline builder
  methods (`withBlend`, `withCull`, `withDepthWrite`, `withColorWrite`, …)
- `MaskedTextureStateShard` — `RenderSystem.setShaderTexture(0/3, …)` → `pass.bindSampler("Sampler0"/"Sampler3", …)`
- `entity_decal.vsh` / `.fsh` — port to the current uniform/UBO convention; the two
  `discard` tests and the `EMISSIVE` branch are the whole logic and must be preserved verbatim
- The two `rendertype_*.json` shader definitions — no longer JSON; folded into the pipeline
- Registration: NeoForge `event.registerShader(...)` and Fabric `CoreShadersMixin` both go
  away in favour of `RenderPipeline#register`. **`CoreShadersMixin` should be deleted, not ported.**

## Acceptance criteria

Reference frames in `reference/` from the 1.21.4 build, Ender Dragon variant:

- `dissolve-t48.4.png` — mid dissolve. Grass visible through holes in the torso and neck;
  wing membrane mostly gone leaving the dark finger bones; tail broken into separate chunks.
- `dissolve-t49.0.png`, `dissolve-t49.6.png` — late stages.

Checks that actually matter:
- Holes are **hard-edged**, not soft/feathered — it is `discard`, not alpha blending.
- The pattern is **fixed to the model**, not to the screen. Moving the camera must not
  move the holes.
- Purple emissive markings erode on the **same** mask and timing as the body.
- No glowing rim at the dissolve boundary. The 1.21.4 build has none; do not add one.
- Progress tracks `deathTime / maxDeathTime`, finishing right as the body disappears.

## Reproducing

From the `dev` worktree:

```
./gradlew :neoforge:runClient
/summon neodragonmounts:dragon
/kill @e[type=neodragonmounts:dragon]
```
