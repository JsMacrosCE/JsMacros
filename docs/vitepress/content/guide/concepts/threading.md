---
description: "JsMacrosCE scripts run on worker threads while Minecraft runs on the client thread. When they must meet, and what blocks."
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
already joined to the main thread cannot use either form: even with `await` set to
`false`, JsMacros throws, because queued work would not run until the handler
returns. Release the event lock first, or move the work to an unjoined thread.

## What "joined to main" means

Whether the game can continue depends on where the event fires in the game's own
code. Some events sit immediately before an action the game is about to take:
sending a chat message, handling a key press, opening a container, clicking a
slot, playing a sound, sending or receiving a packet. Those are **cancellable**,
because there is still something to stop. The game cannot continue until it knows
the answer, so dispatch waits for the listener: the listener is joined to the main
thread. The event decides this, not the registration. So `joined: false` does not opt
out of a cancellable event.

Events that report something already done like a tick passing, health changing, a
chunk loading, which have nothing to stop. Their listeners are queued instead: the game
continues, the callback runs afterwards on the script's thread, and
`event.cancel()` is not available.

**Requesting a join** covers the middle case: an event that is not cancellable but
whose raiser is willing to wait. A listener asks by passing `true` as the second
argument to `JsMacros.on`, and a custom event must set `joinable = true` before it
is triggered. Cancellable events already do this. Dispatch then waits until the
listener releases its event lock, normally when the callback returns. The request
is per listener: only listeners that asked are joined, and a script cannot join
its own listener. See
[Joinable events](/guide/concepts/events#joinable-events-making-trigger-wait-for-another-script)
for the full rules and example.

The trade-off is that a joined listener is on the critical path. Keep it short:

```js
const blocked = ["password", "secret"];
JsMacros.on("SendMessage", JavaWrapper.methodToJava((event) => {
  if (event.message === null) return;
  if (blocked.some((word) => event.message.toLowerCase().includes(word))) {
    event.cancel(); // the message is not sent
  }
}));
```

If the handler needs to wait for something else, use `waitForEvent`. It releases
the current bound event lock before it blocks, so the main thread can continue
while the script waits:

```js
JsMacros.on("SendMessage", JavaWrapper.methodToJava((event) => {
  if (event.message !== "confirm") return;
  event.cancel(); // hold this message back until there is an answer

  // The current cancellable event is released while this waits.
  const next = JsMacros.waitForEvent("RecvMessage", JavaWrapper.methodToJava(
    (received) => received.text.getString() === "yes"
  ));
  try {
    // Remember, this will trigger another SendMessage event
    Chat.say("confirmed");
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
