---
description: "Register a client command from a JsMacrosCE script, read typed arguments, suggest values, and remove it again."
outline: deep
---
# Custom commands

A custom command is one a script adds to Minecraft's command tree. The player
types `/name`, the client runs your callback, and nothing is sent to the server.
That makes commands a good front end for a script: they appear in tab completion,
take typed arguments, and work in single-player or on a server that knows nothing
about them.

Do not build a command by listening to `SendMessage` and matching the text.
Cancelling that event only drops a chat message; it gives you no argument parsing,
validation, or suggestions. Register a command instead, and keep `SendMessage`
listeners for filtering chat.

## Register a command

Get the command manager from `Chat`, build a command, then register it:

```js
const command = Chat.getCommandManager().createCommandBuilder("greet");
command.literalArg("world").executes(JavaWrapper.methodToJava((ctx) => {
  Chat.log("hello world");
}));
command.register();
```

The name has no leading slash. A builder does nothing until `register()`; after
that, `/greet world` runs the callback.

Wrap the callback with `JavaWrapper.methodToJava`. The async form queues the
callback and returns, so the command completes before your code runs. The callback
runs on the client thread, so treat it like a joined handler: `Client.waitTick`
throws there, and a blocking request freezes the game. Keep it short and hand slow
work to another thread.

## Arguments

Each argument method adds one required argument to the current branch. The name
you pass is the name `ctx.getArg` uses:

```js
const command = Chat.getCommandManager().createCommandBuilder("count");
command.intArg("amount", 1, 64).executes(JavaWrapper.methodToJava((ctx) => {
  Chat.log(`count is ${ctx.getArg("amount")}`);
}));
command.register();
```

`/count 32` prints `count is 32`. `/count 100` is rejected by the argument's range
before the callback runs. Common argument methods:

| Method | Accepts | `getArg` returns |
| --- | --- | --- |
| `literalArg` | one fixed word | nothing |
| `wordArg` | one word | the word |
| `quotedStringArg` | quoted text, spaces allowed | the text, without quotes |
| `greedyStringArg` | the rest of the line | the text |
| `intArg`, `longArg`, `doubleArg` | a number, optionally bounded | the number |
| `booleanArg` | `true` or `false` | a boolean |
| `blockPosArg` | block coordinates | `BlockPosHelper` |
| `itemArg` | an item id | `ItemStackHelper` holding one item |
| `identifierArg` | `namespace:path` | the string |
| `textArgType` | vanilla text-component syntax | `TextHelper` |

The builder covers the rest of the vanilla argument grammar, including ranges,
NBT, entity selectors, and block predicates. Some values come back wrapped rather
than raw; [`CommandContextHelper.getArg`](/api/events/com/jsmacrosce/jsmacros/client/api/helper/CommandContextHelper)
lists the conversions and the cases that throw.

An argument is required once it is added. Because `executes` attaches to the node
built so far, you can attach a callback before an argument and another to the
argument itself to accept both forms.

## Branches

Arguments chain in a line. To add a second branch under the same command, step
back up with `or()` first:

```js
const command = Chat.getCommandManager().createCommandBuilder("count");
command.literalArg("set")
  .intArg("amount", 1, 64)
  .executes(JavaWrapper.methodToJava((ctx) => {
    Chat.log(`count set to ${ctx.getArg("amount")}`);
  }));

command.or(1); // back to /count, beside "set"
command.literalArg("show").executes(JavaWrapper.methodToJava(() => {
  Chat.log("count shown");
}));

command.register();
```

`or()` merges the innermost argument one level up. `or(1)` climbs back to the
command name itself, counting the root as depth 1. `register()` folds any branch
still open the same way, so a chain does not need a final `or`. Where `or` is
reserved or awkward to type, `otherwise()` does the same thing.

## Suggestions

`suggestMatching` offers fixed values for the argument it follows:

```js
const command = Chat.getCommandManager().createCommandBuilder("count");
command.intArg("amount", 1, 64)
  .suggestMatching("10", "32", "64")
  .executes(JavaWrapper.methodToJava((ctx) => {
    Chat.log(`count is ${ctx.getArg("amount")}`);
  }));
command.register();
```

Suggestions belong to the most recent argument, not to a literal; using one after
`literalArg` throws. `suggestIdentifier` marks the values as resource locations,
`suggestBlockPositions` and `suggestPositions` offer coordinates, and `suggest`
takes a callback when the list has to be computed. `suggest` needs its callback
wrapped in `JavaWrapper.methodToJava`; the fixed-value helpers do not.

## Remove a command

`register()` puts the command in the client tree and re-applies it on later
connections. Like an event listener, the callback keeps the registering run open;
the command does not disappear when the top-level code finishes. Remove it when it
should go away:

```js
command.unregister();
```

If the run is stopped while the command is still registered, the command stays in
the tree and using it fails with a closed-context error. Remove it first if it
should not outlive the run. `Chat.getCommandManager().unregisterCommand(name)`
removes any command by name and returns a node that `reRegisterCommand` can put
back.

## Related pages

- [Events](/guide/concepts/events)
- [Threading and the main thread](/guide/concepts/threading)
- API reference: [`CommandManager`](/api/classes/com/jsmacrosce/jsmacros/client/api/classes/inventory/CommandManager),
  [`CommandBuilder`](/api/classes/com/jsmacrosce/jsmacros/client/api/classes/inventory/CommandBuilder),
  [`CommandContextHelper`](/api/events/com/jsmacrosce/jsmacros/client/api/helper/CommandContextHelper),
  [`Chat`](/api/libraries/com/jsmacrosce/jsmacros/client/api/library/impl/FChat)
