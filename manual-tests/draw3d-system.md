# Draw3D suite

The Draw3D API tests are `auto` mode: `Run Automated` executes them headlessly
and reports pass/fail in chat with no page. The remaining Draw3D tests are
`observe` mode: `Run Visual` (or selecting one under the `Draw3D` category)
closes the suite screen, prints the checklist and clickable verdicts in chat,
and keeps a HUD hint while it runs. They require a world; the API tests also run
from a title screen.

> **Blocked on 26.1: 3D surfaces do not render.**
> `Surface.render(PoseStack, MultiBufferSource, float)` — the path `Draw3D.render`
> calls for every surface — has its whole body commented out in this branch, and
> nothing else draws a 3D surface (the Gizmos pass only emits `Box`/`Line3D`).
> So `DRAW3D-VISUAL-001`'s surface panel and the `DRAW3D-SURFACE-002`/
> `DRAW3D-SURFACE-003`/`DRAW3D-ITEM-001` panels render nothing today. These tests
> still exercise registration, element state and the intended visual contract;
> treat a surface that never appears as this known port gap (record `Skip`), not
> as a new regression, until the `main`/`surface-fixes` render rework is ported.

## Coverage

| Test | Expected behavior |
| --- | --- |
| `DRAW3D-LIFECYCLE-001` | `Hud.createDraw3D()`, `register()`/`unregister()` chaining and list membership, `clear()` across boxes/lines/trace lines/surfaces, and `Hud.clearDraw3Ds()` (pre-existing draws are re-registered). |
| `DRAW3D-PRIMITIVES-001` | Box, line, trace line, entity trace line, point (xyz and `Pos3D`), `boxBuilder().forBlock()`, add/remove/re-add, and entity-trace `setEntity`/`shouldRemove`/`dirty` state. |
| `DRAW3D-STYLE-001` | Packed ARGB outline/fill color and alpha, `fixAlpha` for bare RGB, `setAlpha`/`setFillAlpha` RGB preservation, `setColor`/`setFillColor`, `setPosToBlock`/`setPosToPoint`, and box/line/trace/entity-trace builder getters. |
| `DRAW3D-SURFACE-001` | Surface width/height versus minimum subdivisions, position/rotation/size mutators, and nested rect/text/line/image/item/Draw2D. |
| `DRAW3D-VISUAL-001` | In-world rendering of a filled box, line, trace line and surface, plus the always-on-top (`cull = false`) vs depth-tested (`cull = true`) box behavior. |
| `DRAW3D-ENTITY-001` | Entity trace `yOffset` (feet vs above head) tracking, and automatic removal once the entity is removed. Completes itself when the lines disappear, or accepts a chat verdict. Skipped when no non-player entity is nearby. |
| `DRAW3D-SURFACE-002` | Surface `zIndex` draw order (`zIndexScale` is writeable but inert on 26.1, where the z translation is disabled), `setRotateCenter`, image/item/nested elements, a tilted panel, and a head-locked panel re-anchored each tick. |
| `DRAW3D-SURFACE-003` | `setRotateToPlayer(true)` facing, and in-place `setSizes`/`removeRect`/`removeText`/`addRect`/`addText`/`setPos` mutation after the surface has been registered. |
| `DRAW3D-ITEM-001` | `Item.Builder.overlayVisible(true)`/`overlayText(...)` on both a 2D HUD overlay and a 3D surface. |

## Run steps

1. Run `[Run Automated]`; the automated tests report in chat.
2. Join a world.
3. Run `[Run Visual]` (or the `Draw3D` category). For the surface tests, look
   straight ahead; for `DRAW3D-ENTITY-001`, stand near a mob.
4. Open chat (`T`) and click `[Pass]`/`[Fail]`/`[Skip]`; `[H]` reopens the menu.
5. Record the automatic-check count plus the outcome.

## Expected results

- Every automatic check passes.
- `DRAW3D-VISUAL-001`: a red filled box, a green line and a blue trace line
  appear; the surface panel is subject to the blocker note above.
- With a wall between the player and the two far boxes, the yellow
  always-on-top box stays visible while the magenta depth-tested box is hidden.
- `DRAW3D-ENTITY-001`: both entity traces follow the mob; killing it removes
  both lines without leaving a dangling element.
- `DRAW3D-SURFACE-002` (once 3D surfaces render; see the blocker note): the green
  rect draws above the red rect, the image, item and nested panel are visible,
  and the bound panel stays in view while the camera moves.
- `DRAW3D-SURFACE-003` (once 3D surfaces render): the facing panel tracks the
  player, and the red panel keeps rendering after it is resized, recoloured and
  moved.
- `DRAW3D-ITEM-001`: `overlay_test_text` is visible on the 2D HUD item; the 3D
  surface half depends on the surface blocker above.
- Leaving a Draw3D test unregisters its elements; no test leaves a registered
  Draw3D behind.

## Exclusions / known issues

- `Draw3D.clear()` only clears the calling object's elements.
  `Hud.clearDraw3Ds()` empties the global registry; the lifecycle test snapshots
  the pre-existing draws and re-registers them so other scripts are unaffected.
- `Box.Builder.color(int, int)` used to assign `fillColor` instead of the
  requested `color` (upstream `origin/main` bug). The 26.1 sources are fixed, so
  `DRAW3D-STYLE-001` asserts the corrected behavior as a regression.
- `Surface.setSizes()`/`setMinSubdivisions()` on this `26.1` branch call
  `Surface.init()`, which re-runs `Draw2D.init()` and clears the surface's child
  elements (`main`/`surface-fixes` changed this to recompute scale only).
  `DRAW3D-SURFACE-003` re-adds its elements in the same game-thread task, so the
  final state matches both branches; if the rework lands, the now-redundant
  `removeRect`/`removeText` calls become meaningful.
- `Surface.setWorldLight()`/`setFullBrightLight()` do not exist on this `26.1`
  branch (they live on `main`/`surface-fixes`), so the old `surface_light_mode`
  visual check is intentionally not ported. Re-integrate it if the light-mode
  rework is merged into 26.1.
- From 1.21.11 the 3D components are emitted through the Minecraft Gizmos API
  and rendered from a pass added at the start of `LevelRenderer.addLateDebugPass`.
  Confirm the visual result on each target instead of assuming the older render
  pipeline.
- Runtime rendering cannot be asserted from script state; the visual checks are
  manual by design, though `observe` tests may add automatic checks in
  `prepare`/`verify` and a per-tick `tick(state)` hook.
