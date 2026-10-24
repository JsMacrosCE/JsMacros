# World and HUD suite

These areas depend on a loaded world or a loader-specific client hook.
`HUD-DEBUG-001`, `DRAW2D-OVERLAY-001`, `INTERACT-TARGET-001`,
`INVENTORY-BREWING-001` and `INVENTORY-FUEL-001` are `observe` mode: the suite
screen closes, instructions appear in chat, and the verdict is a chat click.
`BLOCK-TAGS-001`, `BLOCK-MOVEMENT-001` and `INVENTORY-ITEMTAGS-001` are `auto`
mode and run headlessly under `Run Automated`. `EVENT-SIGNEDIT-001` and
`EVENT-DROP-001` use `event` mode and complete automatically once their listener
captures the expected event.

## Coverage

| Test | Area | Expected behavior |
| --- | --- | --- |
| `HUD-DEBUG-001` | Hud | A registered `Draw2D` overlay renders above the F3 debug screen. |
| `DRAW2D-OVERLAY-001` | Rendering | A registered `Draw2D` overlay renders on the HUD, and register/unregister update `Hud.listDraw2Ds()`. |
| `INTERACT-TARGET-001` | Interaction | `setTarget` overrides the crosshair target; the target outline stays locked while looking around and the override survives client picks. |
| `BLOCK-TAGS-001` | World | Block-tag membership, namespace omission, `#` handling and block/state helper parity. |
| `BLOCK-MOVEMENT-001` | World | The legacy movement-blocking predicate for stone, air, cobweb and bamboo sapling. |
| `INVENTORY-ITEMTAGS-001` | Inventory | `ItemStackHelper.getTags()` returns namespaced tag ids for a known item. |
| `INVENTORY-BREWING-001` | Inventory | Brewing recipe, ingredient, preview and fuel helpers work with water and nether wart. |
| `INVENTORY-FUEL-001` | Inventory | Furnace fuel values and burn-time helpers work with coal. |
| `EVENT-SIGNEDIT-001` | Events | Front and back sign editors fire `SignEdit` with position, side and four lines. |
| `EVENT-DROP-001` | Events | A hotbar drop fires `DropSlot` with an inventory slot and can be cancelled. |

## Run steps

1. Join a world, then run `[Run Automated]` for `BLOCK-TAGS-001`,
   `BLOCK-MOVEMENT-001` and `INVENTORY-ITEMTAGS-001`.
2. `HUD-DEBUG-001` (Fabric): run it from the `Hud` category or `[Run Visual]`,
   press F3, confirm the cyan overlay text, press F3 again, then open chat (`T`)
   and click a verdict.
3. `DRAW2D-OVERLAY-001`: run it; the cyan overlay appears at the top-left of the
   HUD while the test runs. Click a verdict.
4. `INTERACT-TARGET-001`: run it; the crosshair target is overridden to the
   solid block under you. Look around and confirm the outline stays locked on
   that block, then click a verdict.
5. `INVENTORY-BREWING-001`: set up a brewing stand with a water bottle and
   nether wart, run the test, close chat, then open the stand. The test
   completes and records the recipe checks automatically.
6. `INVENTORY-FUEL-001`: set up a furnace with coal in the fuel slot, run the
   test, close chat, then open the furnace. The test completes automatically.
7. `EVENT-SIGNEDIT-001`: run the test, close chat, open a sign's front editor
   and close it, then open the back editor. The test completes automatically.
8. `EVENT-DROP-001`: run the test with an item in the selected hotbar slot,
   close chat, and press Q. The event is cancelled so the item remains.
9. Record the automatic-check count plus the outcome.

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
- `BLOCK-TAGS-001`: stone reports `minecraft:base_stone_overworld`, oak leaves
  report `minecraft:leaves`, and unknown tags return false.
- `BLOCK-MOVEMENT-001`: stone blocks movement; air, cobwebs and bamboo saplings
  do not.
- `INVENTORY-BREWING-001`: water plus nether wart previews a brewable potion;
  the ingredient, recipe and fuel helpers report success. Run it on both an
  integrated server and a remote server to cover live and fallback recipes.
- `INVENTORY-FUEL-001`: coal has a positive fuel value and the fuel-time
  readouts are non-negative. Run it on both server types to cover live and
  fallback fuel data.
- `EVENT-SIGNEDIT-001`: both sign sides fire the event with the same position
  and four text lines.
- `EVENT-DROP-001`: the hotbar key reports a slot in 36-44, and cancelling the
  event leaves the item in place.

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
- `INVENTORY-BREWING-001` and `INVENTORY-FUEL-001` wait for the named container
  and the exact setup items; they time out rather than modify the inventory.
- `EVENT-SIGNEDIT-001` records editor-open events only; it does not change the
  sign or send an edit packet.
- `EVENT-DROP-001` cancels the drop it observes, so the held item remains.
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
