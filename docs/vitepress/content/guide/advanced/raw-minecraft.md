---
outline: deep
---
# Raw Minecraft access and reflection

Most scripts should use the helper classes. They are the stable part of the API:
they hide package names, often smooth over Minecraft version differences, and are
documented in the reference.

Sometimes the helper does not expose the field or method you need. JsMacros gives
you two ways to reach past the helpers: the raw object behind a helper, and Java
reflection.

Both of them are version-sensitive. A script that works through `World.getBlock`
may keep working across versions; a script that reaches into a raw Minecraft class
may not.

## The raw object behind a helper

Using the raw object is common, not an emergency escape hatch. A helper exposes the
stable part of an API; `getRaw()` gives you the Minecraft object underneath it, so
you can call a method the helper does not wrap or pass the object to another Java
API.

```js
const player = Player.getPlayer();
if (player !== null) {
  const rawPlayer = player.getRaw();
  // rawPlayer is the Minecraft LocalPlayer
}
```

`Client.getMinecraft()` returns the raw client instance, and many helpers have a
`getRaw()` inherited from `BaseHelper`. The raw object can be `null`, can become
stale when the world changes, and can have a different shape on another Minecraft
version.

### Finding the raw method name

The method name on the raw object is the name the game uses at runtime, and that
depends on the loader and target:

- **26.1 and newer** ship unobfuscated, so raw objects use Mojang names such as
  `getFiringYPosition()`.
- **Pre-26.1 Fabric** remaps to intermediary names at runtime. The source method may
  be `getFiringYPosition()`, but the raw object exposes something like
  `method_62822()`.
- **Pre-26.1 NeoForge** uses Mojang official names, so the source name is usually the
  name you call.

Use [mcsrc.dev](https://mcsrc.dev/) to read the Mojang source and find the method's
purpose and name. Use [mappings.dev](https://mappings.dev/) to see the corresponding
intermediary (Fabric) name for a specific version. For example, the
[1.21.8 Breeze mappings](https://mappings.dev/1.21.8/net/minecraft/world/entity/monster/breeze/Breeze.html)
show `getFiringYPosition()` as `method_62822()`.

```js
// 1.21.8 Fabric: the raw object exposes the intermediary name.
const breeze = World.getEntities("breeze");
if (breeze !== null && breeze.size() > 0) {
  Chat.log(breeze.get(0).getRaw().method_62822());
}
```

```js
// 26.1.2 and newer, or pre-26.1 NeoForge: the Mojang name works.
const breeze = World.getEntities("breeze");
if (breeze !== null && breeze.size() > 0) {
  Chat.log(breeze.get(0).getRaw().getFiringYPosition());
}
```

Older guides used the MinecraftMappingViewer for this lookup. Do not rely on that
tool now; it no longer works. `mappings.dev` covers the same job for the supported
targets.

Raw methods are not part of the JsMacros API. They have no stable signature across
targets, and the script editor cannot check them for you. Keep the lookup close to
the helper call and put the version-specific name in one place if the script has to
support more than one target.

## Reflection

`Reflection` loads classes and calls methods that are not part of the documented
script API:

```js
const ArrayList = Reflection.getClass("java.util.ArrayList");
const list = Reflection.newInstance(ArrayList);
list.add("diamond");

const size = Reflection.getDeclaredMethod(ArrayList, "size");
Chat.log(`the list holds ${Reflection.invokeMethod(size, list)}`);
```

Resolve a method separately from invoking it. That makes the lookup reusable and
gives you a clear error when the name or parameter types do not match.

```js
const ArrayList = Reflection.getClass("java.util.ArrayList");
const list = Reflection.newInstance(ArrayList);

const ObjectClass = Reflection.getClass("java.lang.Object");
const add = Reflection.getDeclaredMethod(ArrayList, "add", ObjectClass);
Reflection.invokeMethod(add, list, "diamond");
```

A declared-field lookup can find non-public fields, but reading one may still be
denied by Java access rules.

## `Java.type` and reflection

Graal's `Java.type("...")` loads a class directly in JavaScript. It is convenient
when you know the class name at script-writing time:

```js
const ClientState = Java.type('com.jsmacrosce.jsmacros.client.JsMacrosClient');
```

`Reflection` is the cross-language API and can load jars at runtime. Prefer the
helper or `Java.type` for a fixed class, and `Reflection` when the class or member
must be looked up dynamically.

## What breaks between versions

- Minecraft class and member names depend on the loader, mappings, and target.
- A raw method can be renamed, moved, or have a different signature.
- A helper can gain a stable wrapper for the same operation in a later JsMacros
  release; check the reference before reaching for reflection.
- A raw field can be null in a state where the helper method returns a useful
  default.

The reference is generated for one Minecraft target for a reason. If a raw access
works on 1.21.8, it is not automatically valid on 26.3. Use the version selector
and check the target's page.

## When it is worth it

Use raw access when:

- the helper does not expose the operation you need;
- you are passing an object to another Java API that expects the raw type;
- you are prototyping before adding a proper helper.

Prefer a helper when:

- the operation is part of normal script work;
- the script should keep working across Minecraft updates;
- the code needs null checks, coordinate handling, or threading behavior that the
  helper already provides.

## Related pages

- [Script type namespaces](/guide/advanced/script-types)
- [Player and world](/guide/game/player-and-world)
- API reference: [`Reflection`](/api/libraries/com/jsmacrosce/jsmacros/core/library/impl/FReflection),
  [`Client`](/api/libraries/com/jsmacrosce/jsmacros/client/api/library/impl/FClient),
  [`BaseHelper`](/api/classes/com/jsmacrosce/jsmacros/core/helpers/BaseHelper)
