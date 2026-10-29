---
outline: deep
---
# JsMacrosCE Guide

JsMacrosCE runs scripts inside Minecraft. A script can read the game, change what
you do, react to events, and draw on the screen. The Guide teaches the ideas that
make that work; it does not try to list every method.

If you want to look up an exact signature or a raw Minecraft type, use the
[API reference](/api/) for your Minecraft version instead.

## Read this first

1. [Install JsMacrosCE and write your first script](/guide/getting-started)
2. [Understand how a script runs](/guide/concepts/how-scripts-run)
3. [Learn callbacks](/guide/concepts/callbacks) and [events](/guide/concepts/events)
4. [Learn what the game thread means for your script](/guide/concepts/threading)

After that, read the pages for what you want to build: [the player and the
world](/guide/game/player-and-world), [key binds](/guide/game/keybinds),
[HUD drawing](/guide/ui/hud-and-draw2d), or [custom screens](/guide/ui/screens).

## How the guide is organised

- **Concepts** explain the runtime: how scripts start and stop, why callbacks are
  wrapped, how events hold the main thread, and how scripts share state.
- **Working with the game** shows the common ways to inspect and control the
  player, world, input, and network.
- **On screen** covers overlays and script-created screens.
- **Advanced** goes deeper: scanning for blocks with `WorldScanner`, script-type
  namespaces, and the escape hatches of raw game objects and reflection.

Each page ends with links into the API reference for the exact classes and
methods it mentions.

::: tip Versions
The Guide is written for all supported Minecraft versions. When behaviour differs
between versions, the page says so and links to the matching reference. The version
selector in the navigation bar changes which generated reference you are reading.
:::
