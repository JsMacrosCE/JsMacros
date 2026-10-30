---
outline: deep
---
# Events

An event is a typed payload delivered when something happens: a chat message, a
tick, a key press, an inventory change, an entity loading, and so on. Most scripts
are easier to write as reactions to events than as loops that check for changes.

Each event has its own reference page for its payload fields, whether it is
cancellable, and whether it has a filterer. This page explains the subscription
and waiting APIs around those events.

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

`on` returns a listener handle. Pass it to `JsMacros.off(listener)` when you no
longer want it.

## `once` and `off`

`once` registers a listener that removes itself after the first event:

```js
JsMacros.once("Tick", JavaWrapper.methodToJava(() => {
  Chat.log("one tick later");
}));
```

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
normal action. `SendMessage` is the common example:

```js
JsMacros.on("SendMessage", JavaWrapper.methodToJava((event) => {
  if (event.message === "/hello") {
    event.cancel();
  }
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

## Joining a non-cancellable event

A non-cancellable event can still be joinable. Pass `true` as the second argument
to request joined handling:

```js
JsMacros.on("Tick", true, JavaWrapper.methodToJava(() => {
  // the game waits for this callback when the event supports joining
}));
```

Joining is a request, not a guarantee: the event must permit it. Cancellable events
join regardless of this flag.

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

Filters are event-specific. An event page shows its `Filterer` when one exists;
open that filterer's page for the available fields and setters. `JsMacros.on`
rejects a filterer that does not support the event.

## Keep handlers small

High-frequency events such as `Tick`, `RecvPacket`, and `SendPacket` can run many
times per second. A handler that scans the world or waits on the main thread on
every event will hurt the game. Filter first, do the smallest amount of work, and
defer anything expensive.

## Related pages

- [Threading and the main thread](/guide/concepts/threading)
- [Callbacks](/guide/concepts/callbacks)
- [How a script runs](/guide/concepts/how-scripts-run)
- API reference: [`JsMacros`](/api/libraries/com/jsmacrosce/jsmacros/core/library/impl/FJsMacros),
  [`BaseEvent`](/api/classes/com/jsmacrosce/jsmacros/core/event/BaseEvent),
  [`RecvMessage`](/api/events/com/jsmacrosce/jsmacros/client/api/event/impl/EventRecvMessage),
  [`SendMessage`](/api/events/com/jsmacrosce/jsmacros/client/api/event/impl/EventSendMessage)
