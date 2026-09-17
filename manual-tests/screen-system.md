# Screen system suite

Run `suite.js` and open the browse screen from the chat panel or with `H`. The
tests below are all `screen` mode: `Run Screen` (or selecting one from a
category) opens the first test with `Passed`/`Failed`/`Previous`/`Next`/`Back`
controls; Passed/Failed record the result and advance. A passed result means the
documented automatic checks passed and the visible/manual behavior matched. Use
`Skipped` only when a test explicitly says that its required context is
unavailable.

## Coverage

| Area | Tests | Expected behavior |
| --- | --- | --- |
| Screen | `SCREEN-OPEN-001` through `SCREEN-ESC-001` | Creation, title/background, init/reload, failure recovery, close/parent, Escape, and pause flags work. |
| Input | `SCREEN-INPUT-001`, `SCREEN-INPUT-002` | Mouse, drag, scroll, key, and character callbacks expose meaningful input data; key callbacks report Shift/Ctrl/Alt/Super and character callbacks report the delivered modifiers. |
| Widgets | `WIDGETS-DIRECT-001`, `WIDGETS-BUILDER-001`, `WIDGETS-PREDICATE-001` | Direct and builder APIs create interactive controls; callbacks and state setters work; text-field predicates filter typed and programmatic text. |
| Rendering | `RENDER-PRIMITIVES-001`, `SCREEN-RENDER-001` | Render components, nested Draw2D, ordering/mutation, and the portable render callback path work. |
| Text | `TEXT-INTERACTION-001`, `TEXT-WORLD-HOVER-001` | Styled text hover/click behavior works, including item/entity payloads in a world. |
| Integration | `SCREEN-HOST-001` | The API wraps a vanilla screen captured before the suite opens. |

`DRAW2D-OVERLAY-001` and `INTERACT-TARGET-001` moved to `observe` mode (world
visible); see [world-system.md](world-system.md).

## Target-specific expectations

- On 26.1 the vanilla `CharacterEvent` no longer carries modifiers, so
  `SCREEN-INPUT-002` derives them from the live key state. Ctrl/Alt/Super are
  asserted on the key path (`KeyEvent.modifiers`, always populated); the char
  path asserts plain and Shift and reports the rest, because many platforms do
  not deliver char events for Ctrl/Alt at all. A missing Shift bit is a
  regression, not a target-specific exception.
- Text input predicates are implemented on 26.1 through the helper's responder,
  because `EditBox` dropped `setFilter`; `WIDGETS-PREDICATE-001` covers both
  typed and programmatic input on every target.
- `TEXT-WORLD-HOVER-001` requires a loaded local player. Mark it skipped at
  the title screen.
- `SCREEN-HOST-001` requires starting the suite while a vanilla screen, such
  as inventory, is open. Mark it skipped when no host was captured.
- `DRAW2D-OVERLAY-001` is only visibly useful after leaving the test screen;
  use Back to confirm the overlay disappears during cleanup.

## Matrix procedure

Run the same suite for each supported Minecraft version and loader: `Run All
Tests` (or `Run Automated` → `Run Visual` → `Run Screen`). Record each test's
automatic-check count and outcome with `results-template.md`, then compare the
same test IDs across tuples. Do not treat an unsupported API noted above as a
regression.
