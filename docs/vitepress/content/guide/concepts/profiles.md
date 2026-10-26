---
outline: deep
---
# Profiles

A profile is a named set of trigger configuration. It is not a separate Minecraft
account, a separate script folder, or a separate runtime. Switching profiles saves
the current trigger list and loads another one, so you can keep different trigger
sets for different servers, worlds, or kinds of play without editing files by
hand.

## Where profiles live

Open **Settings → Profile**. There you can see and change the current profile, the
default profile used at startup, and the list of profiles. The list supports
adding, renaming, and deleting profiles. A profile's stored data is its list of
scripts and their triggers.

The default profile starts with one trigger: the `ProfileLoad` event running
`index.js`. A newly created profile starts empty, so it only runs what you add.

## What switches

When you switch profiles:

- the current profile's triggers are saved under its name;
- the new profile's triggers replace them;
- the new profile's name becomes the current profile;
- scripts started by the new triggers can run immediately.

Trigger lists are replaced, not merged. If a script should exist in two profiles,
add it to both, or keep the shared setup in a service.

## What does not switch

- **Script files** still come from the same `config/jsMacros/Macros` folder.
- **Services** are registered and started by the service manager, separately from
  profiles. A service keeps its own enabled/running state across profile switches.
- **`GlobalVars` is shared by the whole JsMacros runtime and is not cleared by a
  profile switch.** The library is created once, so values written under one
  profile are still visible under another. Use distinct keys, `remove`, or a file
  if profiles must not share state.
- **Listeners registered with `JsMacros.on` are not triggers.** The trigger list is
  replaced, but a script context that is already alive with its own listener stays
  registered until it is stopped or the context closes.

That last point is the one that surprises people. If a profile's `index.js`
registers a `ProfileLoad` listener, that listener can observe later profile
switches even though the trigger list has been replaced.

## Example: report profile switches

```js
JsMacros.on("ProfileLoad", JavaWrapper.methodToJava((event) => {
  Chat.log(`profile "${event.profileName}" is ready`);
}));
```

`ProfileLoad` fires after the new profile's triggers have been registered,
including at startup. It does not fire when the profile has no trigger list at all;
a missing profile is created empty and does fire the event.

## When a profile is useful

Profiles are useful when the same installation needs different trigger sets:

- one profile for a survival server and one for a creative test world;
- a minimal profile for recording or performance-sensitive play;
- a profile that enables a different set of utility scripts for a different
  server.

If you only ever want the same triggers everywhere, you can ignore profiles and
stay on the default one. Services are the better tool for a script that should
always be running.

## Related pages

- [How a script runs](/guide/concepts/how-scripts-run)
- [Sharing state](/guide/concepts/shared-state)
- [Getting started](/guide/getting-started)
- API reference: [`JsMacros`](/api/libraries/com/jsmacrosce/jsmacros/core/library/impl/FJsMacros),
  [`ProfileLoad`](/api/events/com/jsmacrosce/jsmacros/core/event/impl/EventProfileLoad),
  [`GlobalVars`](/api/libraries/com/jsmacrosce/jsmacros/core/library/impl/FGlobalVars)
