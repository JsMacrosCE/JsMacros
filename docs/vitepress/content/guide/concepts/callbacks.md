---
outline: deep
---
# Callbacks

Many JsMacros methods do not return a value; they ask you for a function to run
later. Examples are event listeners, button presses, world scanner filters, and
WebSocket messages. Java cannot call a JavaScript function directly, so JsMacros
passes these functions as `MethodWrapper` objects.

`JavaWrapper.methodToJava` creates one.

```js
const callback = JavaWrapper.methodToJava((event) => {
  Chat.log("a message arrived");
});

JsMacros.on("RecvMessage", callback);
```

## Synchronous or asynchronous

There are two wrappers with different scheduling:

| Wrapper | When the callback runs | Use for |
| --- | --- | --- |
| `JavaWrapper.methodToJava(fn)` | The caller's thread, synchronously | Event listeners, widget callbacks, filters |
| `JavaWrapper.methodToJavaAsync(fn)` | Scheduled on the script thread pool | Callbacks whose return value is ignored |

`methodToJava` is the default. It runs the callback on the calling thread and lets
the caller use its return value. `methodToJavaAsync` is only for callbacks whose
result is ignored: it queues them on the script thread pool and returns immediately,
logging any failure. A callback that must return a value still runs synchronously,
and its exceptions reach the caller; use `methodToJava` for filters and predicates.

For event listeners, use `methodToJava`. The event system already decides how the
listener is scheduled; wrapping it again with the async form only makes ordering
harder to reason about.

## Why a plain function is not enough

A JavaScript function has no Java type that a Java method can accept, and it does
not carry the script context that owns it. The wrapper carries both:

- the script context, so the callback can enter the right Graal runtime;
- the function, so the Java side can invoke it;
- the cancellation and thread bookkeeping around the call.

That context is also why a script with a live callback stays alive. The wrapper is
a reference to the script, not a detached function.

JsMacros cannot do this automatically. The Java API expects a `MethodWrapper`, and
the wrapper has to know which script lifetime should keep the callback reachable.
A raw guest function does not carry that lifetime, so the conversion is explicit.

## Variables inside a callback

A callback closes over the variables of the script that created it. Those variables
belong to that script context, not to the Java method that called the callback.

For JavaScript, callbacks for one context are serialized: the runtime enters the
Graal context on one thread at a time, so two callbacks do not execute the same
script variable in parallel. They can still interleave at a `waitForEvent`,
`Time.sleep`, or other yielding boundary, and `methodToJavaAsync` changes when a
callback runs. Treat a variable shared across callbacks as state you must reason
about, and use `GlobalVars` when the state must be shared between scripts. Java
objects you pass around keep their own thread-safety rules.

## Callback arguments

Read the method's documentation for the arguments. Common shapes are:

```js
// event listener: (event, context)
JsMacros.on("Tick", JavaWrapper.methodToJava((event, context) => {
  // event is the Tick payload, context is its EventContainer
}));

// button: (button)
screen.addButton(10, 10, 100, 20, "Press me", JavaWrapper.methodToJava((button) => {
  Chat.log("pressed");
}));

// world scanner filter: (entity) -> boolean
const items = World.getEntities(JavaWrapper.methodToJava((entity) => {
  return entity.getType() === "minecraft:item";
}));
```

Methods that return a value from the callback expect the wrapper's return value to
be converted back to Java. Return the value directly; do not call
`methodToJava` inside the callback unless the API asks for another callback.

## Errors

When an event listener throws an exception, JsMacros removes that listener to
prevent a broken callback from firing forever. However, this also means that a
mistake can silently stop a script. Check the script log when a listener stops
working.

Wrap risky work in `try`/`catch` when the callback should survive one bad event:

```js
JsMacros.on("RecvMessage", JavaWrapper.methodToJava((event) => {
  try {
    const text = event.text.getString();
    if (text.includes("auction")) {
      Chat.log("auction message");
    }
  } catch (error) {
    Chat.log(`handler failed: ${error}`);
  }
}));
```

## Stopping

`JavaWrapper.stop()` closes the current script context. It releases event locks,
interrupts threads bound to the context, and unregisters the context. Use it when a
script has finished its job but still has callbacks registered.

For a single listener, prefer `JsMacros.off(listener)` so the rest of the script
can continue.

## Related pages

- [How a script runs](/guide/concepts/how-scripts-run)
- [Events](/guide/concepts/events)
- [Threading](/guide/concepts/threading)
- API reference: [`JavaWrapper`](/api/libraries/com/jsmacrosce/jsmacros/graal/library/impl/FWrapper),
  [`JsMacros`](/api/libraries/com/jsmacrosce/jsmacros/core/library/impl/FJsMacros)
