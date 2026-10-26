---
layout: home
hero:
  name: JsMacrosCE
  text: Script Minecraft with JavaScript
  tagline: Automate the boring parts, react to the game, and draw your own HUD — without writing a mod.
  actions:
    - theme: brand
      text: Start the guide
      link: /guide/
    - theme: alt
      text: API reference
      link: /api/
features:
  - title: Write JavaScript
    details: JavaScript is built in. Optional language extensions let you script in Python and other supported languages.
  - title: React to the game
    details: Listen for chat, ticks, key presses, inventory changes, and world updates instead of polling in a loop.
  - title: Draw on the HUD
    details: Create 2D overlays, 3D world markers, custom screens, and textures from the same script.
  - title: Match your Minecraft version
    details: The guide teaches concepts once. The API reference is generated separately for each supported Minecraft target.
---

## Start here

1. [Install JsMacrosCE and write your first script](/guide/getting-started)
2. [Understand how a script runs](/guide/concepts/how-scripts-run)
3. [Learn callbacks and events](/guide/concepts/callbacks)

## Two parts, two jobs

The **Guide** teaches the ideas that make JsMacros different from ordinary Java or
JavaScript code: callbacks, the script and game threads, event locks, overlays, and
sharing state between scripts. Read it in order the first time.

The **API reference** is the lookup. It is generated from the sources for one
Minecraft target, so every signature and every raw Minecraft type is accurate for
that version. Use the version selector to move between targets.

## Not sure where to start?

- **New to JsMacros?** Begin with [Install and first script](/guide/getting-started).
- **Coming from another macro mod?** Read [How a script runs](/guide/concepts/how-scripts-run)
  and [Events](/guide/concepts/events).
- **Looking for a method?** Open the [API reference](/api/) and search for the
  library or class name. [How to read the API reference](/guide/reference/how-to-read)
  explains the page layout.
- **Confused by a type name?** Read [Script type namespaces](/guide/advanced/script-types).

## Community

- [GitHub repository](https://github.com/JsMacrosCE/JsMacros/)
- [Modrinth page](https://modrinth.com/mod/jsmacrosce)
- [Discord community](https://discord.gg/P6W58J8)
