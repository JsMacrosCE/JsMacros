# World and HUD suite

These areas depend on a loaded world or a loader-specific client hook.
`HUD-DEBUG-001`, `DRAW2D-OVERLAY-001` and `INTERACT-TARGET-001` are `observe`
mode: the suite screen closes, instructions appear in chat, and the verdict is a
chat click. `INVENTORY-ITEMTAGS-001` is `auto` mode and runs headlessly under
`Run Automated`.

## Coverage

| Test | Area | Expected behavior |
| --- | --- | --- |
| `HUD-DEBUG-001` | Hud | A registered `Draw2D` overlay renders above the F3 debug screen. |
| `DRAW2D-OVERLAY-001` | Rendering | A registered `Draw2D` overlay renders on the HUD, and register/unregister update `Hud.listDraw2Ds()`. |
| `INTERACT-TARGET-001` | Interaction | `setTarget` overrides the crosshair target; the target outline stays locked while looking around and the override survives client picks. |
| `INVENTORY-ITEMTAGS-001` | Inventory | `ItemStackHelper.getTags()` returns namespaced tag ids for a known item. |

## Run steps

1. Run `[Run Automated]` for `INVENTORY-ITEMTAGS-001`.
2. Join a world.
3. `HUD-DEBUG-001` (Fabric): run it from the `Hud` category or `[Run Visual]`,
   press F3, confirm the cyan overlay text, press F3 again, then open chat (`T`)
   and click a verdict.
4. `DRAW2D-OVERLAY-001`: run it; the cyan overlay appears at the top-left of the
   HUD while the test runs. Click a verdict.
5. `INTERACT-TARGET-001`: run it; the crosshair target is overridden to the
   solid block under you. Look around and confirm the outline stays locked on
   that block, then click a verdict.
6. Record the automatic-check count plus the outcome.

## Expected results

- `HUD-DEBUG-001` (Fabric): the overlay text stays visible while the debug
  screen is open.
- `DRAW2D-OVERLAY-001`: the cyan overlay is visible at the top-left, and the
  automatic checks confirm register/unregister list membership.
- `INTERACT-TARGET-001`: `hasTargetOverride()` is true after setting, the block
  outline stays on the overridden block while looking around, and
  `getTargetedBlock()` still matches when the result is marked.
- `INVENTORY-ITEMTAGS-001`: the diamond reports a non-empty, namespaced tag
  list; the names are not printed because the test runs headlessly.

## Exclusions / known issues

- `HUD-DEBUG-001` is skipped on non-Fabric loaders. The debug-screen overlay
  hook is a Fabric mixin on this branch; NeoForge has no equivalent hook, so the
  overlay does not render over F3 there. The test marks itself skipped on
  NeoForge.
- Before 26.1 the client pick hook is Fabric-only (`GameRenderer.pick`), so on
  NeoForge for those versions `INTERACT-TARGET-001` verifies the override API
  but not the pick integration. From 26.1 the hook moved to `Minecraft.pick` and
  is common to both loaders. Mark the pick check with that exception in mind.
- `INTERACT-TARGET-001` overrides the block directly under the player because
  the block the player stands in is normally air and would show no outline.
- `INVENTORY-ITEMTAGS-001` asserts namespacing and a non-empty tag list rather
  than specific tag ids, so it stays valid across data-pack and version changes.
- These tests never call `Hud.clearDraw2Ds()`/`clearDraw3Ds()`; doing so would
  remove other scripts' registrations.

## Out of suite

- `EventTitle` with type `ACTIONBAR` is only fired for server/system overlay
  chat (`ChatListener.handleOverlay`). There is no public client-side trigger,
  and the suite does not use servers or commands. Verify it with a server that
  sends an overlay system message.
- The JsMacros menu keybind only acts when no screen is open
  (`TickBasedEvents` checks `mc.screen == null`), so it cannot be checked from a
  suite screen. Manual check: close the suite, press the configured key (default
  `K`), and confirm the menu opens; this is the only coverage for the Fabric
  26.1 key-mapping registration path.
