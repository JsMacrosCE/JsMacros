---
outline: deep
---
# Key binds and input

Minecraft distinguishes two names for the same idea:

- a **key name** identifies a physical input, such as `key.keyboard.w` or
  `key.mouse.left`. The full list is on the
  [Minecraft Wiki key-code page](https://minecraft.wiki/w/Key_codes).
- a **keybind name** identifies an action, such as `key.forward` or `key.jump`.
  The full list is in the
  [Minecraft Wiki controls table](https://minecraft.wiki/w/Controls#Configurable_controls).

`KeyBind` methods that start with `key` take key names. Methods that start with
`keyBind` take keybind names. Mixing them is the most common mistake.

For a game action, prefer the **keybind name**. It follows the player's rebinds
and affects exactly one action. Use a key name only when you specifically mean the
physical input, for example to satisfy a mod that listens for raw keys. A key name
can trigger every action bound to that key.

`KeyBind.getKeyBindings()` returns the current key assignments, including bindings
added by other mods. Use it to discover a modded keybind name instead of guessing.

## Press and release

```js
KeyBind.pressKeyBind("key.forward");
Client.waitTick(20);
KeyBind.releaseKeyBind("key.forward");
```

`KeyBind.pressKeyBind` marks the action down; it stays down until you release it or
the game clears key states. `Client.waitTick(20)` holds it for roughly one second of
game time. The release call matters: a press without a release leaves the game
believing the key is still down.

Use key names when you mean a specific physical key:

```js
KeyBind.pressKey("key.keyboard.w");
Client.waitTick(10);
KeyBind.releaseKey("key.keyboard.w");
```

A press is a state change, not a held physical key. Opening any screen calls
Minecraft's `KeyMapping.releaseAll()`, so a scripted press is dropped and is not
restored when the screen closes. If movement must survive a screen, re-apply the
input after it closes; for longer movement, prefer `PlayerInput`.

## Rebind an action

```js
// put jump on numpad 0, then restore the previous binding
const before = KeyBind.getKeyBindings().get("key.jump");
KeyBind.setKeyBind("key.jump", "key.keyboard.numpad.0");
// ...
KeyBind.setKeyBind("key.jump", before);
```

`setKeyBind` changes the binding, not the pressed state. Passing `null` unbinds the
action. Restore the previous binding when you are done so the player is not left
with a changed control.

## React to a key

The `Key` event fires before the game handles the input and is cancellable:

```js
JsMacros.on("Key", JavaWrapper.methodToJava((event, context) => {
  if (event.action !== 1) return; // 0 is release, 1 is press
  if (event.key === "key.keyboard.j" && event.mods === "key.keyboard.left.control") {
    Chat.log("ctrl+J taken by the script");
    event.cancel(); // the game does not see it
  }
  context.releaseLock(); // release before any slow work
}));
```

The `Key` event is cancellable, so its listeners are joined to the main thread. Keep
the handler short. If the script needs to do slow work after deciding whether to
cancel, call `context.releaseLock()` first; see [Events](/guide/concepts/events) and
[Threading](/guide/concepts/threading).

Unknown keys and key repeats are filtered before the event is created. The event
still reports the release for keys that were already down when a screen opened.

When a screen is open, its input rules decide whether the event fires. The
**Settings → General → Disable KEY event when any screen is open** option
(`disableKeyWhenScreenOpen`, on by default) suppresses most key events. Even when it
is off, JsMacros suppresses events for its own screens, for a focused text field,
and for the recipe-book search box. The game may also handle the same key, so cancel
the event if the script should take it exclusively.

## State changes and screens

`KeyBind` state-setting calls are ignored while a screen is open. Reading the
keys JsMacros has marked pressed still works; this is its own ledger, not a query
of the physical keyboard:

```js
for (const key of KeyBind.getPressedKeys()) {
  Chat.log(`held: ${key}`);
}
```

This is deliberate: injecting movement into a text field or menu would be
surprising. Opening a screen also releases all key mappings, so a scripted press
does not carry across the screen. Close the screen first and re-apply the input if
the script needs to drive movement afterwards.

## Related pages

- [Threading and the main thread](/guide/concepts/threading)
- [Player and world](/guide/game/player-and-world)
- API reference: [`KeyBind`](/api/libraries/com/jsmacrosce/jsmacros/client/api/library/impl/FKeyBind),
  [`Key` event](/api/events/com/jsmacrosce/jsmacros/client/api/event/impl/EventKey)
