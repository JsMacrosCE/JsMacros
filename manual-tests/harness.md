# Suite harness

`suite.js` is a single long-lived script. This page describes how the runner
works and the contract a new test follows, including the reserved `event` mode.

## Modes

| Mode | UI while running | How it ends | Declaration |
| --- | --- | --- | --- |
| `auto` | none | programmatic checks finish | `run(state)` |
| `screen` | the suite test screen | user presses Mark Passed/Failed | `setup(screen, state)` |
| `observe` | none (world visible); chat + HUD hint | user clicks Pass/Fail/Skip/Retry, or `done(state)` | `prepare(state)`, `instructions`, `verify?`, `teardown?`, `tick?`, `done?`, `timeoutSeconds?` |
| `event` | none; chat + HUD hint | `done(state)`, a user verdict, or `timeoutSeconds` | same as observe plus `done?`/`timeoutSeconds` |

Every test declares `id`, `category`, `mode`, `title`; `screen` tests may add
`background: false` to keep the world visible.

## Execution model

- The top-level script runs on a JsMacros pool thread, not the game thread.
- `Client.waitTick()` drives a loop; between ticks it drains a request queue
  that screen buttons fill. Chat verdict clickables instead set
  `session.decision` directly, and `[Menu]` opens the menu directly, because a
  queued request would not run while a test's wait loop is active. This keeps
  the script context alive while the suite screen is closed (a running script
  thread is not closed by `BaseScriptContext.shouldKeepAlive`).
- A `JsMacros.on("Key", ...)` listener for `H` reopens the browse screen. The
  listener also keeps the context strongly referenced. `EventKey` does not fire
  while a JsMacros screen is open, so `H` only acts when the suite is closed.
- Screen construction and `Hud.openScreen` are marshalled to the game thread
  with `Client.runOnMainThread(callback, await = true, 120000)`. Never block the
  game thread; only the script thread waits.
- Building `Text` (`Draw2D.addText`, a surface's `addText`, an overlay's
  `setOnInit`) measures the font and can bake glyphs, which requires the render
  thread. `Draw2D.register()` calls `init()` synchronously, so `runAuto`,
  `observe.prepare`, and `showHint` all run their body through `onMain`.
  `unregister()` is just a set removal and is safe off-thread.
- Chat is the verdict channel: `withCustomClickEvent` attaches a callback to a
  styled text segment, and a click sets `session.decision`, which the waiting
  script thread reads on its next tick. Clicks dispatch only while the chat
  screen is open (`T`), which the on-screen hint states. Note that chat colors
  use `TextBuilder.withColor(r, g, b)`; the single-int overload takes a
  `ChatFormatting` id, not RGB.
- While an `observe`/`event` test runs, a small `Draw2D` overlay shows the test
  name and the click instruction; it is unregistered when the test ends.
- An `observe`/`event` test may declare `tick(state)`. It runs once per tick
  iteration of the wait loop, on the script thread, and exists for animated
  visuals such as a head-locked surface or a staged mutation. Work that builds
  `Text` (or otherwise needs the render thread) must marshal with `onMain`.
- `done(state)` and `timeoutSeconds` work for both `observe` and `event`:
  `done` is polled each tick and records an automatic `Passed` when it returns
  true (used by `DRAW3D-ENTITY-001`), and `timeoutSeconds` records a `Failed`
  with a chat notice.
- `auto` tests run on the script thread through the `run(state)` callback and
  never open a page. `withDraw(fn)` isolates each Draw3D test by always clearing
  and unregistering in `finally`.

## State and results

- `session.results[id] = { outcome, automatic }` holds the current session's
  results. `automatic` is the `passed/total` string from `state.checks`.
- Results are not persisted; use [results-template.md](results-template.md) for
  durable records. The `FS` library can append to a file next to the script if
  persistence is needed later.
- `state.cleanup` (screen tests) or `teardown(state)` (observe/event tests) runs
  when a test ends, when the page closes, or when a retry restarts the test.

## Future `event` tests

The runner already supports the shape an event test needs:

```js
{
    id: "EVENT-ACTIONBAR-001",
    category: "Events",
    mode: "event",
    title: "Actionbar Event",
    timeoutSeconds: 30,
    prepare(state) {
        state.seen = null;
        state.listener = JsMacros.on("Title", wrap((event, context) => {
            if (event.type === "ACTIONBAR") state.seen = event.message;
        }));
        return true;
    },
    instructions: ["Trigger an actionbar message; the result is recorded automatically."],
    done(state) { return state.seen != null; },
    verify(state) {
        // state.checks.push(...) based on state.seen
    },
    teardown(state) { if (state.listener != null) state.listener.off(); }
}
```

Contract notes for event tests:

- Register listeners in `prepare` and remove them in `teardown`. The listener
  wrapper keeps the context alive, which is what the runner needs while no
  screen is open.
- `done(state)` is polled every client tick. Return `true` once the captured
  data is complete; the runner records an automatic `Passed`. The user can still
  override with a chat verdict while waiting.
- `timeoutSeconds` bounds the wait and records a `Failed` with a chat notice.
- `verify(state)` runs after the wait and should add `check` entries; the
  automatic count is what the results summary reports.
- Avoid cancelling events unless the test is explicitly about cancellation, and
  restore any listener or registrable in `teardown`.

## Known limits

- Chat clickables need the chat screen open; JsMacros has no bare-HUD click
  dispatch.
- The `H` hotkey is suppressed while any JsMacros screen is open, and may be
  suppressed while a vanilla screen is focused if `disableKeyWhenScreenOpen` is
  enabled.
- Blocking the game thread with `Client.waitTick()` or `runOnMainThread` from a
  joined callback throws; the harness keeps all waiting on the script thread.
