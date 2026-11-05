---
description: "Subscribe to game events with JsMacros.on, wait for one with waitForEvent, and handle event locks and cancellation."
outline: deep
---
# Events

An event is a typed payload delivered when something happens: a chat message, a
tick, a key press, an inventory change, an entity loading, and so on. Most scripts
are easier to write as reactions to events than as loops that check for changes.

Each event has its own [reference page](/api/events) for its payload fields,
whether it is cancellable, and whether it has a filterer. This page explains the
subscription and waiting APIs around those events.

## What a context is

Every delivery comes with an **event context**, an `EventContainer`. It is the
handle for that one delivery, and it owns the lock that can keep the game's main
thread waiting. You receive it as the second callback argument and as
`EventAndContext.context` from `waitForEvent`.

You do not need to create one. You do need to know when it is released:

- an `on`/`once` callback releases its context when the callback returns;
- a `waitForEvent` result holds its context until you call
  `context.releaseLock()`, and the wrapper releases it at callback exit at the
  latest.

## Subscribe with `on`

```js
const listener = JsMacros.on("RecvMessage", JavaWrapper.methodToJava((event, context) => {
  if (event.messageType === "Chat Error") {
    event.cancel();
    return;
  }
  Chat.log(`[${event.messageType}] ${event.text.getString()}`);
}));
```

`event` is the typed payload. `context` is the delivery handle described above;
this callback does not need to touch it because the callback returns immediately.

`on` returns an [`IEventListener`](/api/classes/com/jsmacrosce/jsmacros/core/event/IEventListener) listener handle.
Pass it to `JsMacros.off(listener)` when you no longer want it.

The optional `true` argument asks a joinable event to wait for this listener.
That is an advanced, multi-script feature; see
[joinable events](#joinable-events-making-trigger-wait-for-another-script).

## `once` and `off`

`once` registers a listener that removes itself after the first event:

```js
JsMacros.once("Tick", JavaWrapper.methodToJava(() => {
  Chat.log("one tick later");
}));
```

It returns the same [`IEventListener`](/api/classes/com/jsmacrosce/jsmacros/core/event/IEventListener) handle as `on`, so `JsMacros.off(listener)`
can cancel a one-shot listener before it fires.

`off` also accepts the event name and handle, or you can remove every script
listener for an event:

```js
JsMacros.off(listener);
JsMacros.disableScriptListeners("Tick");
JsMacros.disableScriptListeners(); // every script listener
```

Built-in listeners are not removed by these calls.

## Cancellable events

Some events can be cancelled. Calling `event.cancel()` tells the game to skip its
normal action. A practical use is preventing an accidental item drop: `DropSlot`
fires for the drop key and for throwing an item out of an open inventory, and
cancelling it leaves the item where it is.

```js
// Require sneak, so a stray press of the drop key cannot throw away the held item.
JsMacros.on("DropSlot", JavaWrapper.methodToJava((event) => {
  const player = Player.getPlayer();
  if (player === null || player.isSneaking()) return;
  event.cancel();
}));
```

Cancellation only does something for an event that declares itself cancellable.
Calling `cancel()` on any other event throws `UnsupportedOperationException`.

Cancellable events also **join** the main thread. The game waits for the callback
to finish before it decides whether to continue. This happens even if you registered
the listener with `joined: false`: the cancellable flag forces joined dispatch. Keep
the handler short.

If the handler only needs a quick decision, cancel or not, and then wants to do
slow work, release the context first:

```js
JsMacros.on("Key", JavaWrapper.methodToJava((event, context) => {
  if (event.action !== 1 || event.key !== "key.keyboard.j") return;

  // The decision is made. Let the game continue before the expensive part.
  context.releaseLock();
  Chat.log("this can take as long as it needs");
}));
```

Do not release before `event.cancel()` if you intend to cancel. The game reads the
cancelled flag when the lock is released. See [Threading](/guide/concepts/threading)
for the full model.

## Waiting for one event

`waitForEvent` blocks the script until a matching event arrives:

```js
const next = JsMacros.waitForEvent("RecvMessage");

// Reading the payload does not need the lock. Release it before unrelated work.
next.context.releaseLock();

Chat.log(`next message: ${next.event.text.getString()}`);
```

The result has two fields: `event` and `context`. For a cancellable event, the main
thread is waiting on that lock, so release it as soon as you no longer need the
delivery. If you need to cancel the event, cancel first, then release.

`waitForEvent` also releases the event lock the script was already holding before
it blocks. That is what makes it safe to call from inside a cancellable handler:
the game can continue while the script waits for the next event.

You can filter and add a pre-wait callback:

```js
const next = JsMacros.waitForEvent(
  "RecvMessage",
  JavaWrapper.methodToJava((event) => event.text.getString().includes("done"))
);
```

There is no timeout. If the event never arrives, the script waits until it is
cancelled or the context closes.

## Filters

For high-frequency events, use a filter. It runs before the callback enters the
script queue, so a filtered-out event costs far less than a callback that wakes up
and immediately returns. For `Tick`, packet, block-update, and movement events this
is usually the difference between a usable script and a stutter.

```js
const nearbyChestFilter = JsMacros.createEventFilterer("BlockUpdate")
  .setPos(0, -60, 0)
  .setBlockId("minecraft:chest");

JsMacros.on("BlockUpdate", nearbyChestFilter, JavaWrapper.methodToJava((event) => {
  Chat.log("the chest at 0, -60, 0 changed");
}));
```

Filters are event-specific. [`JsMacros.createEventFilterer`](/api/libraries/com/jsmacrosce/jsmacros/core/library/impl/FJsMacros)
returns the filterer for an event, and the [event's page](/api/events) shows which
filterer it has. Open that page for the available fields and setters; the shared
behaviour is on [`EventFilterer`](/api/classes/com/jsmacrosce/jsmacros/core/event/EventFilterer).
`JsMacros.on` rejects a filterer that does not support the event.

## Keep handlers small

High-frequency events such as `Tick`, `RecvPacket`, and `SendPacket` can run many
times per second. A handler that scans the world or waits on the main thread on
every event will hurt the game. Filter first, do the smallest amount of work, and
defer anything expensive.

## Joinable events: making `trigger()` wait for another script

::: tip You probably do not need this
Normal listeners are fire-and-forget. Joinable events are for one script that
needs another script's listener to finish before it continues. Most scripts never
need them.
:::

An event is **joinable** when the code that raises it is willing to wait for
listeners. A listener must also ask for that wait by passing `true` as the second
argument to `JsMacros.on`. Only then does `trigger()` wait for that listener.

There are two separate switches:

- **The event allows it.** For a custom event, set `joinable = true` on the
  instance you are about to trigger. `registerEvent()` only makes the name
  subscribable.
- **The listener asks for it.** Pass `true` to `JsMacros.on`.

The listener's `true` is only a request. An event that is not joinable ignores it.
Cancellable events already wait for their listeners, so passing `true` there adds
nothing, and a custom event with `cancelable = true` is already joinable. Among
the built-in events, every joinable event is currently cancellable, so joining is
practical only for custom events raised by another script.

For example, script A owns a cache and answers `Lookup` requests, and script B
needs the answer before its next line:

```js
// script A: keep state and answer requests
const cache = { apple: 12, bread: 7 };
JsMacros.createCustomEvent("Lookup").registerEvent();
JsMacros.on("Lookup", true, JavaWrapper.methodToJava((event) => {
  event.putInt("count", cache[event.getString("key")] ?? 0);
}));
```

```js
// script B: wait for A's answer
const request = JsMacros.createCustomEvent("Lookup");
request.joinable = true;
request.putString("key", "apple");
request.trigger();

Chat.log(`count: ${request.getInt("count")}`); // filled in by script A
```

Without `joinable` or without `true`, B would continue immediately and could read
`null` before A's callback runs.

The listener must already be registered when B calls `trigger()`, so A must
register the name before subscribing. A script cannot wait for a listener in its
own run: the callback could not run while `trigger()` holds that script. Keep the
listener in a different script.

Joining orders work; it is not a function call. The listener's return value does
not come back to `trigger()`, and an exception is reported in A rather than thrown
in B. `trigger()` waits until the listener releases its event lock: a normal
callback does that on return, but `context.releaseLock()` releases it early and
lets B continue while the rest of A's callback still runs.

If you only need to trigger without blocking the caller, `triggerAsync(callback)`
dispatches on a background task and runs `callback` after dispatch returns. It
does not make the next line wait.

## Related pages

- [Threading and the main thread](/guide/concepts/threading)
- [Callbacks](/guide/concepts/callbacks)
- [Custom commands](/guide/game/custom-commands)
- [How a script runs](/guide/concepts/how-scripts-run)
- API reference: [`JsMacros`](/api/libraries/com/jsmacrosce/jsmacros/core/library/impl/FJsMacros),
  [`BaseEvent`](/api/classes/com/jsmacrosce/jsmacros/core/event/BaseEvent),
  [`RecvMessage`](/api/events/com/jsmacrosce/jsmacros/client/api/event/impl/EventRecvMessage),
  [`SendMessage`](/api/events/com/jsmacrosce/jsmacros/client/api/event/impl/EventSendMessage)
