# Manual client test catalog

Modes: `auto` (headless, chat report), `screen` (suite page controls),
`observe` (world visible, chat verdict), `event` (reserved). See
[harness.md](harness.md).

| ID | Title | Mode | Behavior | Script | Status |
| --- | --- | --- | --- | --- | --- |
| `SCREEN-OPEN-001` | Screen Basics | `screen` | Script-screen metadata, dimensions, title, and baseline rendering. | [suite.js](suite.js) | Manual |
| `SCREEN-LIFECYCLE-001` | Screen Lifecycle | `screen` | Initialization and reload rebuild the screen without stale controls. | [suite.js](suite.js) | Manual |
| `SCREEN-FAILURE-001` | Init Failure Recovery | `screen` | A controlled init failure reaches `setOnFailInit` and returns safely. | [suite.js](suite.js) | Manual |
| `SCREEN-CLOSE-001` | Close and Parent | `screen` | Close callback ordering and parent restoration. | [suite.js](suite.js) | Manual |
| `SCREEN-ESC-001` | Escape and Pause | `screen` | Title visibility, Escape suppression, and non-pausing behavior. | [suite.js](suite.js) | Manual |
| `SCREEN-INPUT-001` | Screen Input | `screen` | Mouse, drag, scroll, key, and character callbacks. | [suite.js](suite.js) | Manual |
| `SCREEN-INPUT-002` | Input Modifiers | `screen` | Key callbacks report Shift/Ctrl/Alt/Super; character callbacks report the delivered modifiers. | [suite.js](suite.js) | Manual |
| `WIDGETS-DIRECT-001` | Direct Widgets | `screen` | Direct widget construction, callbacks, setters, tooltips, and programmatic click. | [suite.js](suite.js) | Manual |
| `WIDGETS-BUILDER-001` | Widget Builders | `screen` | Builder-created button, checkbox, slider, lock, text field, and cycling button. | [suite.js](suite.js) | Manual |
| `WIDGETS-PREDICATE-001` | Text Field Predicate | `screen` | `setTextPredicate`/`resetTextPredicate` filter typed and programmatic text. | [suite.js](suite.js) | Manual |
| `RENDER-PRIMITIVES-001` | Render Primitives | `screen` | Text, rect, line, image, item, nested Draw2D, ordering, and mutation. | [suite.js](suite.js) | Manual |
| `DRAW2D-OVERLAY-001` | Draw2D Overlay | `observe` | Overlay registration, HUD rendering, and unregistration. | [suite.js](suite.js) | Visual, chat verdict |
| `TEXT-INTERACTION-001` | Text Interaction | `screen` | Text hover for exact styled segments and a custom click action. | [suite.js](suite.js) | Manual |
| `TEXT-WORLD-HOVER-001` | World Hover | `screen` | Item and entity hover payloads using the local player. | [suite.js](suite.js) | Manual, requires world |
| `SCREEN-RENDER-001` | Screen Render Callback | `screen` | Screen render callback execution without raw draw-context coupling. | [suite.js](suite.js) | Manual |
| `SCREEN-HOST-001` | Host Screen | `screen` | Captured vanilla-screen wrapping and return navigation. | [suite.js](suite.js) | Manual, requires launch from host screen |
| `INTERACT-TARGET-001` | Target Override | `observe` | `setTarget` overrides the crosshair target and survives client picks. | [suite.js](suite.js) | Visual, chat verdict, requires world |
| `DRAW3D-LIFECYCLE-001` | Draw3D Lifecycle | `auto` | `Hud.createDraw3D()`, register/unregister chaining, list membership, `clear()` across all lists, and `Hud.clearDraw3Ds()`. | [suite.js](suite.js) | Automatic |
| `DRAW3D-PRIMITIVES-001` | Draw3D Primitives | `auto` | Box, line, trace line, entity trace line, point (xyz and `Pos3D`), `forBlock`, add/remove/re-add, and entity-trace state. | [suite.js](suite.js) | Automatic |
| `DRAW3D-STYLE-001` | Draw3D Style and Builders | `auto` | ARGB color/alpha, `fixAlpha`, fill semantics, position helpers, builder getters, and the `Box.Builder.color(int,int)` fix. | [suite.js](suite.js) | Automatic |
| `DRAW3D-SURFACE-001` | Draw3D Surface | `auto` | Surface size/subdivision/transform and nested element API. | [suite.js](suite.js) | Automatic |
| `INVENTORY-ITEMTAGS-001` | Item Tags | `auto` | `ItemStackHelper.getTags()` returns namespaced tag ids. | [suite.js](suite.js) | Automatic |
| `DRAW3D-VISUAL-001` | Draw3D Visual | `observe` | Box/line/trace/surface rendering and always-on-top depth behavior. | [suite.js](suite.js) | Visual, chat verdict, requires world |
| `DRAW3D-ENTITY-001` | Entity Trace Line | `observe` | Entity trace `yOffset` tracking and automatic removal when the entity is gone. | [suite.js](suite.js) | Visual, chat verdict, requires a nearby mob |
| `DRAW3D-SURFACE-002` | Surface Render Regression | `observe` | Surface `zIndex` draw order, `setRotateCenter`, image/item/nested elements, and a head-locked panel. | [suite.js](suite.js) | Visual, 3D surface blocked on 26.1 |
| `DRAW3D-SURFACE-003` | Surface Facing and Mutation | `observe` | `setRotateToPlayer(true)` and in-place size/remove/add/position mutation while registered. | [suite.js](suite.js) | Visual, 3D surface blocked on 26.1 |
| `DRAW3D-ITEM-001` | Item Overlay Text | `observe` | 2D HUD item `overlayVisible`/`overlayText`, plus the same on a 3D surface. | [suite.js](suite.js) | Visual, 3D surface blocked on 26.1 |
| `HUD-DEBUG-001` | Debug Screen Overlay | `observe` | Registered `Draw2D` overlay renders over the F3 debug screen. | [suite.js](suite.js) | Visual, chat verdict, Fabric only |

> On the current `26.1` branch `Surface.render(PoseStack, ...)` is commented out,
> so 3D surfaces do not draw. Tests marked "3D surface blocked on 26.1" still run
> their registration/state checks; record `Skip` if the panels never appear. See
> [draw3d-system.md](draw3d-system.md).

Add new tests by behavior area (for example, `SCREEN-*`, `DRAW2D-*`,
`DRAW3D-*`, `INVENTORY-*`, or `EVENT-*`) rather than by Minecraft version. A
target-specific exception belongs in that test's documentation. Future event
tests use the reserved `event` mode described in [harness.md](harness.md).
