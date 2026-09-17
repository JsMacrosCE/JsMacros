# Manual client test catalog

| ID | Title | Behavior | Script | Status |
| --- | --- | --- | --- | --- |
| `SCREEN-OPEN-001` | Screen Basics | Script-screen metadata, dimensions, title, and baseline rendering. | [suite.js](suite.js) | Manual |
| `SCREEN-LIFECYCLE-001` | Screen Lifecycle | Initialization and reload rebuild the screen without stale controls. | [suite.js](suite.js) | Manual |
| `SCREEN-FAILURE-001` | Init Failure Recovery | A controlled init failure reaches `setOnFailInit` and returns safely. | [suite.js](suite.js) | Manual |
| `SCREEN-CLOSE-001` | Close and Parent | Close callback ordering and parent restoration. | [suite.js](suite.js) | Manual |
| `SCREEN-ESC-001` | Escape and Pause | Title visibility, Escape suppression, and non-pausing behavior. | [suite.js](suite.js) | Manual |
| `SCREEN-INPUT-001` | Screen Input | Mouse, drag, scroll, key, and character callbacks. | [suite.js](suite.js) | Manual |
| `WIDGETS-DIRECT-001` | Direct Widgets | Direct widget construction, callbacks, setters, tooltips, and programmatic click. | [suite.js](suite.js) | Manual |
| `WIDGETS-BUILDER-001` | Widget Builders | Builder-created button, checkbox, slider, lock, text field, and cycling button. | [suite.js](suite.js) | Manual |
| `RENDER-PRIMITIVES-001` | Render Primitives | Text, rect, line, image, item, nested Draw2D, ordering, and mutation. | [suite.js](suite.js) | Manual |
| `DRAW2D-OVERLAY-001` | Draw2D Overlay | Overlay registration, rendering, unregistration, and cleanup. | [suite.js](suite.js) | Manual |
| `TEXT-INTERACTION-001` | Text Interaction | Text hover for exact styled segments and a custom click action. | [suite.js](suite.js) | Manual |
| `TEXT-WORLD-HOVER-001` | World Hover | Item and entity hover payloads using the local player. | [suite.js](suite.js) | Manual, requires world |
| `SCREEN-RENDER-001` | Screen Render Callback | Screen render callback execution without raw draw-context coupling. | [suite.js](suite.js) | Manual |
| `SCREEN-HOST-001` | Host Screen | Captured vanilla-screen wrapping and return navigation. | [suite.js](suite.js) | Manual, requires launch from host screen |

Add new tests by behavior area (for example, `SCREEN-*`, `DRAW2D-*`,
`INVENTORY-*`, or `EVENT-*`) rather than by Minecraft version. A target-specific
exception belongs in that test's documentation.
