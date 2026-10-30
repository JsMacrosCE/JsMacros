---
outline: deep
---
# Install and first script

## What you need

You need a Minecraft Java Edition installation with a supported **Fabric** or
**NeoForge** loader, and the matching JsMacrosCE build from
[Modrinth](https://modrinth.com/mod/jsmacrosce) or the
[GitHub releases](https://github.com/JsMacrosCE/JsMacros/releases). JavaScript is
built into the mod. Python and other languages are optional extensions.

The mod creates its configuration under your Minecraft instance:

```text
config/jsMacros/
  options.json
  Macros/          ← your scripts live here
    index.js
```

The in-game menu is bound to <kbd>K</kbd> by default. It opens the script editor,
profiles, and trigger settings. You can change the key in Minecraft's controls
screen.

## The in-game screen

The first time you press <kbd>K</kbd>, the screen can look like a small IDE with
no obvious starting point. The tabs along the top are the main ideas:

- **Keys** — scripts started by a key press or release.
- **Events** — scripts started by a game event, such as a chat message or a tick.
- **Services** — long-running scripts that start with the game and can be
  reloaded when their file changes.
- **Settings** — editor, general, language, and profile settings.
- **Running** — the scripts currently executing, with a stop button for each.
- **About** — the mod version and links.

The current profile name is shown at the top of the screen. Each row in the Keys
or Events list is a **trigger**: an enabled/disabled button, the key or event, the
key's press/release mode (Keys only), whether the run asks to join the event, the
script file, an edit button, and a remove button. The `+` button adds a trigger;
the **Run** button runs a file once without saving a trigger.

A **profile** is a named set of triggers. You can keep one profile for a survival
server and another for a test world, then switch between them in
**Settings → Profile**. See [Profiles](/guide/concepts/profiles) for what changes
when you switch.

## Your first script

Create a file in `config/jsMacros/Macros/`, for example `hello.js`:

```js
Chat.log("Hello from JsMacrosCE");
```

`Chat.log` writes a local message to your chat HUD. It does not send anything to
the server. To send a message, use `Chat.say("...")`; that does reach the server.

Run the script from the in-game editor. The message appears in chat. When the
script reaches the end and has no callbacks left, its execution context closes.

## Make it react to something

A script that only runs once is not very useful. Register a listener instead:

```js
JsMacros.on("RecvMessage", JavaWrapper.methodToJava((event) => {
  Chat.log(`chat: ${event.text.getString()}`);
}));
```

`JsMacros.on` registers the listener for the lifetime of the script context.
`JavaWrapper.methodToJava` wraps the JavaScript function so the Java event system
can call it. The listener keeps the context alive after the script body finishes,
so the callback can still run when a message arrives later.

To stop listening, keep the handle and turn it off:

```js
const listener = JsMacros.on("RecvMessage", JavaWrapper.methodToJava((event) => {
  Chat.log(event.text.getString());
}));

// later, for example inside another event handler
JsMacros.off(listener);
```

## Trigger versus listener

There are two ways to make a script react:

- A **trigger** starts the script file itself when an event or key fires. The
  script runs again for each trigger.
- A **listener** is registered from inside a running script with `JsMacros.on`,
  `once`, or `waitForEvent`. The script stays alive and the callback runs when the
  event arrives.

Do not put `JsMacros.on` inside a trigger that fires every tick unless you really
want a new listener every tick. Pick one model per piece of behaviour.

See [How a script runs](/guide/concepts/how-scripts-run) for the execution model
and [Events](/guide/concepts/events) for listeners and event locks.

## Where to go next

- [How a script runs](/guide/concepts/how-scripts-run)
- [Callbacks](/guide/concepts/callbacks)
- [Events](/guide/concepts/events)
- [Player and world](/guide/game/player-and-world)
- API reference: [`Chat`](/api/libraries/com/jsmacrosce/jsmacros/client/api/library/impl/FChat),
  [`JsMacros`](/api/libraries/com/jsmacrosce/jsmacros/core/library/impl/FJsMacros),
  [`JavaWrapper`](/api/libraries/com/jsmacrosce/jsmacros/graal/library/impl/FWrapper)
