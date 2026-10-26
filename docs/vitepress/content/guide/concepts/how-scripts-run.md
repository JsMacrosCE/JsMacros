---
outline: deep
---
# How a script runs

JsMacros does not run one long-lived program that owns the game. Each run of a
script gets its own **execution context**, and that context decides which globals
exist, which callbacks belong to it, and when it can be cleaned up.

## Triggers start a run

A trigger is stored configuration: a script file, an event or key, and whether
the run should ask to join the event. The trigger types are:

- **Event** — start the script when a named event fires.
- **Key rising** — start it when a matching key is pressed.
- **Key falling** — start it when a matching key is released.
- **Key both** — start it for both press and release.

Triggers belong to the current **profile**. Loading another profile replaces the
trigger list, which is why a profile switch can stop some scripts and start
others. The `ProfileLoad` event fires after the new triggers are registered. See
[Profiles](/guide/concepts/profiles) for what does and does not change.

When a trigger fires, the script is scheduled on a worker thread and run from top
to bottom. It is not run on the Minecraft main thread. That is why a script may
do some work while the game keeps rendering, and why work that must touch the game
has a separate rule (see [Threading](/guide/concepts/threading)).

The script still runs on that worker for a cancellable event. What changes is that
the game's main thread may wait, or be **joined**, on the script's event lock. The
script is not moved onto the main thread; the main thread is made to wait for it.

## The context lives while it is needed

A context closes when the run finishes and nothing keeps it alive. Things that
keep it alive include:

- a callback wrapped with `JavaWrapper.methodToJava` or `methodToJavaAsync`;
- a listener registered with `JsMacros.on`, `once`, or `waitForEvent`;
- a service that is designed to stay running.

This is the useful part: a script can end at the bottom of the file and still have
a listener waiting for a chat message. The context stays open, and the callback
runs later in the same script world.

When you no longer need the listeners, turn them off or close the context
explicitly with `JavaWrapper.stop()`. Closing releases event locks, interrupts
threads bound to the context, and unregisters it from the runner.

```js
const listener = JsMacros.on("Tick", JavaWrapper.methodToJava(() => {
  // ...
}));

// Stop listening and let the context close when nothing else holds it.
JsMacros.off(listener);
```

## One script, many runs

An event trigger runs the script file again for every matching event. A listener
is registered once and called many times. They look similar in a small example but
have different costs:

- A trigger that runs on `Tick` starts a new context twenty times per second. Keep
  such a script tiny.
- A `Tick` listener registered once avoids the repeated setup, but still runs your
  callback twenty times per second. Filter early.

If a script both has an event trigger and registers listeners, be clear about which
part runs per event and which part is set up once.

## Do not build a polling loop

A loop that checks the world as fast as possible burns CPU and usually misses the
point of the event system. Prefer a listener, or `JsMacros.waitForEvent` when the
script should pause until something specific happens.

```js
const next = JsMacros.waitForEvent("RecvMessage");
try {
  Chat.log(`someone said: ${next.event.text.getString()}`);
} finally {
  // Cancellable events join the main thread; release it as soon as you are done.
  next.context.releaseLock();
}
```

Use `Client.waitTick(n)` when you need to wait for game time, and `Time.sleep(ms)`
when you need wall-clock time. Both yield the script thread rather than blocking
the game thread.

## Related pages

- [Threading and the main thread](/guide/concepts/threading)
- [Callbacks](/guide/concepts/callbacks)
- [Events](/guide/concepts/events)
- API reference: [`JsMacros`](/api/libraries/com/jsmacrosce/jsmacros/core/library/impl/FJsMacros),
  [`JavaWrapper`](/api/libraries/com/jsmacrosce/jsmacros/graal/library/impl/FWrapper),
  [`Client`](/api/libraries/com/jsmacrosce/jsmacros/client/api/library/impl/FClient),
  [`Time`](/api/libraries/com/jsmacrosce/jsmacros/core/library/impl/FTime)
