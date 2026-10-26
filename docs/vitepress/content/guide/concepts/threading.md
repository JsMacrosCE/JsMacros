---
outline: deep
---
# Threading and the main thread

JsMacros scripts run on worker threads, while Minecraft renders and ticks on the
**client thread**. Most of the time you do not notice. When you do, the symptom is
usually a freeze, a watchdog cancellation, or an error saying the script is
"joined to main".

## Two kinds of waiting

Use the right wait for the job:

| Call | Waits for | Safe where |
| --- | --- | --- |
| `Client.waitTick(n)` | `n` client ticks | Script threads, not a thread joined to the main thread |
| `Time.sleep(ms)` | Wall-clock milliseconds | Script threads; yields the Graal context |
| `JsMacros.waitForEvent(...)` | The next matching event | Script threads; releases a bound event lock before blocking |

`Client.waitTick` is the one to use for movement and other game-time actions.
`Time.sleep` is for real time, such as polling an HTTP endpoint. Neither one is a
replacement for an event listener when the script only cares about a change.

::: warning Do not sleep on the main thread
A callback that runs joined to the main thread must return before the game can
continue. Sleeping or doing a long computation there freezes the game and can trip
the event watchdog. If you need to wait for another event, use `waitForEvent`: it
releases the lock first.
:::

## Running work on the client thread

Some game operations must happen on the client thread. `Client.runOnMainThread`
queues a callback there:

```js
Client.runOnMainThread(JavaWrapper.methodToJava(() => {
  const player = Player.getPlayer();
  if (player !== null) {
    player.setPos(0.5, 70, 0.5);
  }
}));
```

The basic form queues the task and returns. The full form can wait for it:

```js
Client.runOnMainThread(JavaWrapper.methodToJava(() => {
  // runs on the client thread
}), true, 5000); // await, watchdog limit in milliseconds
```

If the code is already on the client thread, it runs immediately. A thread that is
already joined to the main thread cannot wait on the main thread; that would
deadlock, so JsMacros throws instead.

## What "joined to main" means

Some events are **cancellable** or **joinable**. For those, the game waits for the
listener to finish before it continues. The listener is joined to the main thread
for that period. That is deliberate: if a chat message is cancelled, the game must
not send it before the listener says so.

The trade-off is that a joined listener is on the critical path. Keep it short:

```js
JsMacros.on("SendMessage", JavaWrapper.methodToJava((event) => {
  if (event.message === "/hello") {
    event.cancel(); // the message is not sent
  }
}));
```

If the handler needs to wait for something else, use `waitForEvent`. It releases
the current bound event lock before it blocks, so the main thread can continue
while the script waits:

```js
JsMacros.on("SendMessage", JavaWrapper.methodToJava((event) => {
  if (event.message !== "/confirm") return;

  // The current cancellable event is released while this waits.
  const next = JsMacros.waitForEvent("RecvMessage");
  try {
    if (next.event.text.getString() === "yes") {
      Chat.say("confirmed");
    }
  } finally {
    next.context.releaseLock();
  }
}));
```

The returned container holds a lock for the second event. Releasing it lets the
game continue before this callback returns:

```js
const next = JsMacros.waitForEvent("RecvMessage");
try {
  if (next.event.text.getString() === "yes") {
    Chat.say("confirmed");
  }
} finally {
  next.context.releaseLock();
}
```

The `finally` releases the lock before any work later in the callback. The
`methodToJava` wrapper also releases the event bound to the callback when the
callback exits, so a missing `finally` does not hold the main thread forever. It
does hold it for the rest of the callback body, which is why the release still
matters.

## Watchdogs and cancellation

A joined event or a client-thread task gets a watchdog time limit. If it overruns,
JsMacros can cancel the script instead of leaving the game frozen. The core option
`maxLockTime` sets the limit in milliseconds; initially it is `500`. Change it in
**Settings → General → Max Joined Lock Time**. The full `runOnMainThread`
overload also accepts an explicit limit for one task.

Closing a context releases its event locks and interrupts its threads. That is
what the stop button does, and it is why a cancelled script does not leave the game
waiting forever.

## A practical rule

- Read the world and react on a script thread.
- Change the world on the client thread.
- Keep joined event handlers short.
- Release an event lock as soon as you no longer need it.

## Related pages

- [Callbacks](/guide/concepts/callbacks)
- [Events](/guide/concepts/events)
- API reference: [`Client`](/api/libraries/com/jsmacrosce/jsmacros/client/api/library/impl/FClient),
  [`Time`](/api/libraries/com/jsmacrosce/jsmacros/core/library/impl/FTime),
  [`JsMacros`](/api/libraries/com/jsmacrosce/jsmacros/core/library/impl/FJsMacros)
