---
pageClass: section-overview
---
# Libraries

Libraries are your script's toolbelt.

When a script starts, JsMacros injects global variables like `JsMacros`, `World`, `Player`, `Hud`, and `Chat`. Those globals are library instances. In practice, most scripts follow this pattern:

1. read state from a library
2. make a decision
3. call another library method to act

## How to use libraries well

- Treat libraries as **entry points**, not data containers.
- Pull data out quickly, then move logic into your own functions.
- Keep handlers small: gather state, branch, act, return.
- Prefer stable API methods over raw internals unless you need advanced behavior.

## Typical script flow

```js
// 1) read state
const dim = World.getDimension();

// 2) decide
if (dim !== "minecraft:overworld") return;

// 3) act
Chat.say("Running overworld routine");
```

## Synchronous vs event-driven usage

- Use direct method calls (`World.getTime()`{js}, `Player.getPlayer()`{js}) for one-off checks.
- Use `JsMacros.on(...)`{js} / `JsMacros.waitForEvent(...)`{js} when behavior should react to game activity.
- If an event fires frequently, filter aggressively and keep work lightweight.

## Choosing the right library

Think in terms of intent:

- **automation/control:** `Player`, `Client`, `KeyBind`
- **world inspection:** `World`
- **UI and rendering:** `Hud`
- **communication:** `Chat`
- **runtime orchestration:** `JsMacros`, `Time`, `GlobalVars`
- **IO/integration:** `FS`, `Request`

If a method returns an object you do not recognize, follow its type into the **Classes** section; that is where most chained behavior lives.

## Common pitfall

Avoid writing scripts that only poll in a tight loop. Prefer event-driven logic where possible, and use `Time.sleep(...)`{js} or scheduled/event patterns to reduce load.
