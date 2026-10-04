---
pageClass: section-overview
---
# Classes

Classes are the objects you work with after calling library methods.

If libraries are the front door, classes are the rooms where most real work happens.

For example, `World.getBlock(...)`{js} gives you a block helper class; `Hud.createDraw2D()`{js} gives you a render class; event callbacks receive event classes.

## How to think about classes

- Use libraries to **obtain** class instances.
- Use class methods to **inspect**, **transform**, and **act**.
- Chain class calls when it improves readability; split into variables when debugging.

## Practical usage pattern

```js
const block = World.getBlock(x, y, z);

if (block && block.getId() === "minecraft:chest") {
  Chat.say(`Chest at ${x}, ${y}, ${z}`);
}
```

## About constructors

Many classes expose constructors, but that does not mean they are meant to be instantiated directly from macros.

- Prefer library methods, event data, builders, or factory-style helpers to get instances.
- Direct construction may skip setup or context that JsMacros or Minecraft normally provides.
- If a class can be retrieved from an API, that is usually the safer and more stable approach.

## Mutable vs snapshot behavior

Some classes represent live game objects, others are snapshots/wrappers.

- Do not assume cached objects stay valid forever.
- Re-query when context changes (dimension swap, screen change, entity unload).
- Favor IDs/positions as stable references when possible.

## Working with helper-heavy APIs

Many APIs return helper classes (`*Helper`, render classes, scanner builders). This is done for a few reasons:

- helpers hide Minecraft internals
- type-specific methods reduce manual parsing
- wrappers keep scripts shorter and safer
- helpers' methods are often more efficient than raw reflection
- helpers try to offer a more stable API across Minecraft versions

When in doubt, inspect the returned helper methods first instead of reaching for `Reflection` or `getRaw`, as they are very brittle between versions.

## Reliability tips

- Null-check values from world/entity lookups.
- Guard against unloaded chunks/screens/world state.
- Keep class instances scoped to the logic that uses them; recreate cheaply when needed.
- Use event timing to decide when class data is safe to read.
