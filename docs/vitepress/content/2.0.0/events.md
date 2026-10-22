---
pageClass: section-overview
---
# Events

Events are how scripts react to the game.

An event is a typed payload delivered when something happens (tick, chat message, inventory update, key press, entity activity, world change, and more).

## Event-driven mindset

Instead of repeatedly checking state in loops, subscribe to changes and run logic only when relevant data arrives.

```js
JsMacros.on("RecvMessage", JavaWrapper.methodToJava((event, ctx) => {
  if (event.text && event.text.getString().includes("auction")) {
    Chat.say("Detected auction message");
  }
}));
```

## Designing good handlers

- Exit early when the event is not relevant.
- Keep handler bodies short and predictable.
- Offload heavy work to helper functions or deferred tasks.
- Prefer specific events over broad/high-frequency ones when possible.

## Frequency and performance

Not all events are equal:

- High-frequency events (like ticks or packet streams) need strict filtering.
- Medium-frequency events (inventory/player updates) can do moderate work.
- Low-frequency events (join/quit/open screen) are good for setup and teardown.

If you need sustained logic from a high-frequency source, combine event filters with lightweight state tracking.

## Event + class interplay

Event objects are classes. Read event fields, then branch into library/class operations.

Typical flow:

1. event arrives
2. validate fields and context
3. call libraries/classes to respond
4. return quickly

## Reliability tips

- Validate nullable fields before use.
- Assume world/screen context can change between events.
- Unregister listeners you no longer need.
- For one-shot waits, use `JsMacros.waitForEvent(...)`{js} instead of long-lived listeners.
