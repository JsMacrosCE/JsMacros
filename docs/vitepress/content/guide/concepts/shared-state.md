---
outline: deep
---
# Sharing state between scripts

Ordinary script variables belong to one execution context. If two triggers run the
same file, each run has its own variables. To share a value between runs, scripts,
or profiles, use `GlobalVars`.

`GlobalVars` is a named map of Java values shared by scripts in the same JsMacros
runtime. The library is created once, so a profile switch does not clear it. Values
stay until they are removed or the game session ends.

## Typed values

Use the typed setters and getters:

```js
GlobalVars.putInt("runs", 0);
GlobalVars.putString("lastPlayer", "Steve");
GlobalVars.putDouble("ratio", 0.5);
GlobalVars.putBoolean("enabled", true);

const runs = GlobalVars.getInt("runs");       // Integer or null
const name = GlobalVars.getString("lastPlayer");
const type = GlobalVars.getType("runs");     // "Int"
```

The typed getters do not coerce. If `runs` was stored as a double, `getInt("runs")`
returns `null`, not a truncated integer. Check `getType` when a key can hold more
than one shape.

The getters return Java values. Template literals interpolate them normally when
you build a message.

## Atomic counters

The increment methods are atomic, which matters when two scripts update the same
counter at once:

```js
const next = GlobalVars.incrementAndGetInt("runs");
Chat.log(`this is run ${next}`);

const previous = GlobalVars.getAndIncrementInt("runs");
Chat.log(`the previous value was ${previous}`);
```

`getAndIncrementInt` returns the old value; `incrementAndGetInt` returns the new
one. The decrement variants work the same way.

## Complex values

`putObject` accepts a Java object, not a guest JavaScript object:

```js
const map = JavaUtils.createHashMap();
map.put("count", 1);
GlobalVars.putObject("state", map);

const state = GlobalVars.getObject("state");
```

Passing a JavaScript object throws an assertion error. If you need structured
data, build a Java collection or map through `JavaUtils`, or write JSON to a file
with `FS`. A plain JavaScript object is tied to one script context and cannot be
shared safely.

`getRaw()` returns the live backing map. Writes through it bypass the guest-object
check, so prefer the typed methods unless you are deliberately working with the
whole map.

## What not to use it for

- **Large caches.** `GlobalVars` is a map, not a database. Use `FS` for data that
  should survive a restart.
- **Per-run temporary state.** Use local variables. They are easier to reason
  about and are released with the context.
- **Profile-specific state.** `GlobalVars` is shared, not per profile. If two
  profiles must not see each other's values, prefix keys by profile or write
  separate files.

## Example: remember when each player was last seen

```js
const players = World.getPlayers();
if (players === null) {
  // cconsole.log outputs to the Minecraft log file (STDOUT). Chat.log wouldn't be visible given there's no chat visible.
  console.log("join a server first");
} else {
  const now = Time.time();
  for (let i = 0; i < players.size(); i += 1) {
    const name = players.get(i).getName();
    if (name === null) continue;

    const key = `lastSeen.${name}`;
    const stored = GlobalVars.getDouble(key);
    const lastSeen = stored === null
      ? "first seen"
      : `last seen ${new Date(Number(stored)).toLocaleString()}`;
    Chat.log(`${name}: ${lastSeen}`);

    GlobalVars.putDouble(key, now);
  }
}
```

## Related pages

- [How a script runs](/guide/concepts/how-scripts-run)
- API reference: [`GlobalVars`](/api/libraries/com/jsmacrosce/jsmacros/core/library/impl/FGlobalVars),
  [`JavaUtils`](/api/libraries/com/jsmacrosce/jsmacros/api/library/FJavaUtils),
  [`World`](/api/libraries/com/jsmacrosce/jsmacros/client/api/library/impl/FWorld),
  [`Time`](/api/libraries/com/jsmacrosce/jsmacros/core/library/impl/FTime),
  [`FS`](/api/libraries/com/jsmacrosce/jsmacros/core/library/impl/FFS)
