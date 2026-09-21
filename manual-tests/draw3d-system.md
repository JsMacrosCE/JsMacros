# Draw3D suite

The Draw3D API tests are `auto` mode: `Run Automated` executes them headlessly
and reports pass/fail in chat with no page. The remaining Draw3D tests are
`observe` mode: `Run Visual` (or selecting one under the `Draw3D` category)
closes the suite screen, prints the checklist and clickable verdicts in chat,
and keeps a HUD hint while it runs. They require a world; the API tests also run
from a title screen.

> **Surface rendering on this branch.**
> Surfaces draw their elements directly into the pass buffer source (rects, lines
> and images via the custom `SurfaceRenderTypes` factory). On 1.21.11+ `Surface.renderDirect` runs inside the Gizmos
> pass (after the always-on-top depth clear); on 1.21.5-1.21.10 `Surface.render`
> runs from the direct pass. Items use 26.1's
> `SubmitNodeCollector`/`ItemFeatureRenderer`, the 1.21.10/1.21.11
> `ItemStackRenderState` accessor mixins, or `<1.21.10`
> `ItemRenderer.renderStatic`.
> Rects, lines and images use a custom `SurfaceRenderTypes` factory on every
> target (backed by access-widener entries for `RenderType.create`/`RenderPipelines`),
> so culling and depth testing are explicit and see-through works everywhere. The
> factory has two backends: `RenderType.create(name, RenderSetup)` on 1.21.11+, and
> the older `CompositeState` form on 1.21.5-1.21.10. Surface elements no longer use
> the `RenderPipelines.*.depthTestFunction` reflection. Text and items still use
> stock render types: the font and the item model choose their own render types
> internally, so they cannot be swapped for custom pipelines. The whole path is
> compile-verified on every target.
>
> The light-mode API (`setWorldLight`/`setFullBrightLight`/`setLight` and the
> matching `Surface.Builder` methods) feeds each element's packed light: in an
> enclosed area or at night the WORLD surface dims while BRIGHT/CUSTOM stay lit.
> Cast shadows are not modelled.

## Coverage

| Test | Expected behavior |
| --- | --- |
| `DRAW3D-LIFECYCLE-001` | `Hud.createDraw3D()`, `register()`/`unregister()` chaining and list membership, `clear()` across boxes/lines/trace lines/surfaces, and `Hud.clearDraw3Ds()` (pre-existing draws are re-registered). |
| `DRAW3D-PRIMITIVES-001` | Box, line, trace line, entity trace line, point (xyz and `Pos3D`), `boxBuilder().forBlock()`, add/remove/re-add, and entity-trace `setEntity`/`shouldRemove`/`dirty` state. |
| `DRAW3D-STYLE-001` | Packed ARGB outline/fill color and alpha, `fixAlpha` for bare RGB, `setAlpha`/`setFillAlpha` RGB preservation, `setColor`/`setFillColor`, `setPosToBlock`/`setPosToPoint`, and box/line/trace/entity-trace builder getters. |
| `DRAW3D-SURFACE-001` | Surface width/height versus minimum subdivisions, position/rotation/size mutators, nested rect/text/line/image/item/Draw2D, `renderBack` field/builder, and resize preserving children. |
| `DRAW3D-VISUAL-001` | In-world rendering of a filled box, line, trace line and surface, plus the always-on-top (`cull = false`) vs depth-tested (`cull = true`) box behavior. |
| `DRAW3D-ENTITY-001` | Entity trace `yOffset` (feet vs above head) tracking, and automatic removal once the entity is removed. Completes itself when the lines disappear, or accepts a chat verdict. Skipped when no non-player entity is nearby. |
| `DRAW3D-SURFACE-002` | Surface direct rendering of rect/line/text/image (1.21.11+) and item (26.1), `zIndex` draw order, `setRotateCenter`, a nested Draw2D, a tilted panel, and a head-locked panel re-anchored each tick. |
| `DRAW3D-SURFACE-003` | `setRotateToPlayer(true)` facing, and in-place `setSizes`/`removeRect`/`removeText`/`addRect`/`addText`/`setPos` mutation after the surface has been registered. |
| `DRAW3D-SURFACE-004` | `setWorldLight`/`setFullBrightLight`/`setLight` and the matching builder methods: world light dims in the dark while full-bright and custom stay lit. |
| `DRAW3D-SURFACE-005` | `renderBack = false` is single-sided (the panel disappears when viewed from behind); `renderBack = true` stays visible. |
| `DRAW3D-SURFACE-006` | `cull = false` surfaces are always-on-top (draw over a wall); `cull = true` surfaces are depth-tested (hidden behind it). |
| `DRAW3D-ITEM-001` | `Item.Builder.overlayVisible(true)`/`overlayText(...)` on a 2D HUD overlay, plus the same item on a 3D surface (26.1 direct path). |

## Run steps

1. Run `[Run Automated]`; the automated tests report in chat.
2. Join a world.
3. Run `[Run Visual]` (or the `Draw3D` category). For the surface tests, look
   straight ahead; for `DRAW3D-ENTITY-001`, stand near a mob.
4. Open chat (`T`) and click `[Pass]`/`[Fail]`/`[Skip]`; `[H]` reopens the menu.
5. Record the automatic-check count plus the outcome.

## Expected results

- Every automatic check passes.
- `DRAW3D-VISUAL-001`: a red filled box, a green line, a blue trace line and a
  surface panel appear (panel shows its rect/text/image/item content).
- With a wall between the player and the two far boxes, the yellow
  always-on-top box stays visible while the magenta depth-tested box is hidden.
- `DRAW3D-ENTITY-001`: both entity traces follow the mob; killing it removes
  both lines without leaving a dangling element.
- `DRAW3D-SURFACE-002` (1.21.11+; item half 26.1): the green rect draws above the
  red rect, the nested panel, text, diamond image and diamond sword item are
  visible, and the bound panel stays in view while the camera moves.
- `DRAW3D-SURFACE-003` (1.21.11+): the facing panel tracks the player, and the
  red panel keeps rendering after it is resized, recolored and moved.
- `DRAW3D-SURFACE-004` (1.21.11+): in an enclosed area or at night the blue WORLD
  surface dims while the orange BRIGHT and green CUSTOM surfaces stay lit.
- `DRAW3D-SURFACE-005`: from behind, the green DOUBLE panel stays visible and the
  red SINGLE panel disappears.
- `DRAW3D-SURFACE-006`: with a wall in between, the red ALWAYS TOP panel draws
  over the wall and the blue DEPTH panel is hidden.
- `DRAW3D-ITEM-001`: `overlay_test_text` is visible on the 2D HUD item and on the
  3D surface item (26.1).
- Leaving a Draw3D test unregisters its elements; no test leaves a registered
  Draw3D behind.

## Exclusions / known issues

- `Draw3D.clear()` only clears the calling object's elements.
  `Hud.clearDraw3Ds()` empties the global registry; the lifecycle test snapshots
  the pre-existing draws and re-registers them so other scripts are unaffected.
- `Box.Builder.color(int, int)` used to assign `fillColor` instead of the
  requested `color` (upstream `origin/main` bug). The 26.1 sources are fixed, so
  `DRAW3D-STYLE-001` asserts the corrected behavior as a regression.
- `Surface.setSizes()`/`setMinSubdivisions()` now only recompute the scale (ported
  from `main`/`surface-fixes`), so resizing keeps the surface's child elements.
  `DRAW3D-SURFACE-003` animates a grow, a recolor and a slide to cover this.
- `Surface.setWorldLight()`/`setFullBrightLight()`/`setLight(...)` were ported from
  `main`/`surface-fixes` onto this branch. The chosen light is passed to each
  element: rect/line/text/image multiply their RGB by the light brightness and the
  item uses the packed light coords. It includes the day/night sky darken but not
  cast shadows, so an enclosed WORLD surface can go very dark.
  `DRAW3D-SURFACE-004` covers it.
- Every surface element type draws directly into the pass buffer source (from
  `Surface.renderDirect` on 1.21.11+, `Surface.render` on older targets).
  Rects/lines/images use `SurfaceRenderTypes` on every target, text uses the font
  path, and items use 26.1's
  `SubmitNodeCollector`/`ItemFeatureRenderer`, the 1.21.10/1.21.11 accessors, or
  `<1.21.10` `renderStatic` (items whose model uses a special renderer are not
  handled). Each leaf element is flushed as its own batch because `debugQuads`
  re-sorts quads by camera distance, which would otherwise ignore `zIndex` order.
  Surfaces are drawn back-to-front by camera distance because no surface render
  type writes depth, so a nearer surface covers a farther one (painter's
  algorithm). A nested Draw2D panel is offset by its own `zIndex`, so it is not
  coplanar with the parent's rects (older targets write depth in `debugQuads`, so
  coplanar geometry z-fights). The item's fixed-buffer batches are flushed before
  its overlay text so the text is not occluded.
- A `cull = false` surface (the default in these tests) is fully always-on-top:
  rects, lines, text, images and items all draw over the world. On 1.21.11+ the
  pass clears depth before the always-on-top group when such a surface exists; on
  1.21.5-1.21.10 the pass is split the same way (depth-tested group, depth clear,
  then always-on-top surfaces), so items are on top there too.
- On 1.21.5-1.21.10 the direct pass is injected at the head of
  `LevelRenderer.addLateDebugPass`, i.e. after the cloud and weather passes, so
  clouds do not draw over surfaces.
- `renderBack = false` makes a surface single-sided: the whole panel is skipped
  when the camera is behind its readable face. The readable face is local **+Z**
  (the axis `setRotateToPlayer(true)` points at the camera, and the direction the
  item overlay text is nudged along), so "behind" means the camera lies on the -Z
  side. `renderBack = true` renders both sides; text is still front-only there
  because the text pipeline back-face culls and no non-culling text render type is
  reachable on 26.1.
- The flag was inert before this work, so `addDraw2D`'s convenience overloads
  (which pass `renderBack = false`) now create single-sided panels. Static test
  panels are oriented at the player at `prepare` time via `facingYaw(...)`; the
  side test uses the render camera, not the eye, so run these tests in first
  person - in third person the camera sits behind the player and a panel may be
  judged from the wrong side. `rotateToPlayer` panels are unaffected.
- This path is compile-verified on every target and
  the image/item halves were runtime-confirmed on 26.1; the rest is not.
- From 1.21.11 `Box`/`Line3D`/`TraceLine` are emitted through the Minecraft Gizmos API
  and rendered from a pass added at the start of `LevelRenderer.addLateDebugPass`.
  Surfaces no longer use gizmos; their `renderDirect` output is drawn in the same
  pass, split around the always-on-top depth clear.
  Confirm the visual result on each target instead of assuming the older render
  pipeline.
- Runtime rendering cannot be asserted from script state; the visual checks are
  manual by design, though `observe` tests may add automatic checks in
  `prepare`/`verify` and a per-tick `tick(state)` hook.
