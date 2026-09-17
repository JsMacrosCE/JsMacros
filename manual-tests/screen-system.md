# Screen system suite

Run `suite.js`, select a category or `Run All Tests`, and use the result
controls after each check. A passed result means the documented automatic
checks passed and the visible/manual behavior matched. Use `Skipped` only when
a test explicitly says that its required context is unavailable.

## Coverage

| Area | Tests | Expected behavior |
| --- | --- | --- |
| Screen | `SCREEN-OPEN-001` through `SCREEN-ESC-001` | Creation, title/background, init/reload, failure recovery, close/parent, Escape, and pause flags work. |
| Input | `SCREEN-INPUT-001` | Mouse, drag, scroll, key, and character callbacks expose meaningful input data without blocking normal GUI behavior. |
| Widgets | `WIDGETS-DIRECT-001`, `WIDGETS-BUILDER-001` | Direct and builder APIs create interactive controls; callbacks and state setters work. |
| Rendering | `RENDER-PRIMITIVES-001`, `DRAW2D-OVERLAY-001`, `SCREEN-RENDER-001` | Render components, nested Draw2D, overlays, ordering/mutation, and the portable render callback path work. |
| Text | `TEXT-INTERACTION-001`, `TEXT-WORLD-HOVER-001` | Styled text hover/click behavior works, including item/entity payloads in a world. |
| Integration | `SCREEN-HOST-001` | The API wraps a vanilla screen captured before the suite opens. |

## Target-specific expectations

- On 26.1, character-typed callbacks may report modifier `0`; the vanilla
  event no longer supplies the old modifier value.
- Text input predicates are not implemented for 26.1 and are intentionally not
  treated as a suite failure.
- `TEXT-WORLD-HOVER-001` requires a loaded local player. Mark it skipped at
  the title screen.
- `SCREEN-HOST-001` requires starting the suite while a vanilla screen, such
  as inventory, is open. Mark it skipped when no host was captured.
- `DRAW2D-OVERLAY-001` is only visibly useful after leaving the test screen;
  use Back to confirm the overlay disappears during cleanup.

## Matrix procedure

Run the same suite for each supported Minecraft version and loader, record the
automatic-check count and manual outcome with `results-template.md`, then
compare the same test IDs across tuples. Do not treat an unsupported API noted
above as a regression.
