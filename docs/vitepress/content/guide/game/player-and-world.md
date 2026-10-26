---
outline: deep
---
# Player and world

Most game scripts should start by checking that there is a world and a player, then
read one piece of state and act on it. JsMacros returns wrappers, not raw Minecraft
objects, so method names stay readable and often work across versions.

## Check the context first

There is no useful player or world in the main menu. Guard before doing anything:

```js
if (!World.isWorldLoaded()) {
  Chat.log("join a world first");
} else {
  const player = Player.getPlayer();
  if (player !== null) {
    Chat.log(`playing as ${player.getName().getString()}`);
  }
}
```

`World.isWorldLoaded()` says whether a client world exists. `Player.getPlayer()`
returns `null` when there is no local player, which is different from the world
being loaded but the player not being available yet.

## Read the world

```js
if (World.isWorldLoaded()) {
  const player = Player.getPlayer();
  if (player !== null) {
    const below = World.getBlock(player.getBlockPos().down());
    if (below !== null) {
      Chat.log(`standing on ${below.getName()}`);
    }
  }
}
```

Coordinates use block positions for `World.getBlock`. `player.getBlockPos().down()`
returns the block under the player. `getBlock` returns `null` for void air or when
the client has no data for that position, so keep the null check even inside a
loaded world.

Useful world queries include:

- `World.getDimension()` and `World.getBiome()`
- `World.getTime()` and `World.getTimeOfDay()`
- `World.getBlock(...)` and `World.getChunk(...)`
- `World.getEntities()` and `World.getPlayers()`
- `World.findBlocksMatching(id, chunkRange)`

`findBlocksMatching` only inspects loaded chunks. A large range is expensive; keep
it small and run it on a script thread.

## Entities

`World.getEntities()` returns a snapshot of entity wrappers, or `null` when no world
is loaded:

```js
const entities = World.getEntities();
if (entities !== null) {
  for (const entity of entities) {
    if (entity.getType() === "minecraft:item") {
      Chat.log(`item entity at ${entity.getPos().getX()}, ${entity.getPos().getY()}`);
    }
  }
}
```

The wrappers are live views of the entities. A wrapper can become stale when the
entity unloads or the world changes, so do not cache one across dimension changes
or long waits.

## Finding blocks with `WorldScanner`

For anything more complex than an ID match, build a scanner. The builder has two
stages: a **block** filter and a **block-state** filter. A state filter only sees
the states of blocks that the block filter kept. The examples below assume a world
is loaded.

A named filter calls a public no-argument method on the helper and completes it
with a comparison. The comparison shape follows the method's return type:

```js
const scanner = World.getWorldScanner()
  .withBlockFilter("getHardness").is(">=", 10)
  .withStateFilter("isToolRequired").is(false)
  .build();
```

A string filter compares the helper's complete `toString()` text. `contains` is a
literal substring check; `matches` is a regular expression:

```js
const ores = World.getWorldScanner()
  .withStringBlockFilter().matches(".*_ore.*")
  .build();
```

That is useful when you know the text of a helper, but it is also the slowest kind
of filter. Named filters are more precise, and a Boolean or numeric comparison is
usually cheaper than matching a long string.

For a real computation, use the two-function overload. It receives block and state
helpers and is the right choice when the condition cannot be written as a chain:

```js
if (World.isWorldLoaded()) {
  const scanner = World.getWorldScanner(
    JavaWrapper.methodToJava((block) => block.getId() !== "minecraft:dirt"),
    JavaWrapper.methodToJava((state) => state.isSolid())
  );
  if (scanner !== null) {
    Chat.log(`${scanner.scanAroundPlayer(2).size()} solid non-dirt blocks`);
  }
}
```

Both callbacks are called once per block, not once per match. A slow callback makes
the whole scan slow. The [WorldScannerBuilder reference](/api/classes/com/jsmacrosce/jsmacros/client/api/classes/worldscanner/WorldScannerBuilder)
has more comparison examples.

Scanning is synchronous and reads loaded chunks. It is fine on a script thread,
but do not put a large scan on every tick.

## Moving with `PlayerInput`

Input helpers are values, not immediate actions. `Player.moveForward(0)` queues
**one tick** of forward movement; it does not hold the key while you wait.

To move for a while, queue one input per tick:

```js
const player = Player.getPlayer();
if (player !== null) {
  for (let tick = 0; tick < 20; tick += 1) {
    Player.moveForward(0);
  }
  Client.waitTick(20);
  Player.clearInputs();
}
```

For anything beyond the convenience methods, build a `PlayerInput`. It holds
`movementForward` and `movementSideways` (positive is forward/left), absolute yaw
and pitch in degrees, and the jump, sneak, and sprint flags. Yaw `0` faces south,
`+90` west, `180` north, and `-90` east; positive pitch points down. It affects one
tick when queued:

```js
const player = Player.getPlayer();
if (player !== null) {
  const step = Player.createPlayerInput(1, 0, player.getYaw());
  for (let tick = 0; tick < 20; tick += 1) {
    Player.addInput(step);
  }
  Client.waitTick(20);
}
```

`addInput` copies the input, so reusing one object is safe. `predictInput` returns
where one tick would put you without moving the player, and `clearInputs` drops the
rest of the queue. For key presses that should behave like a real key, see
[Key binds and input](/guide/game/keybinds).

## Related pages

- [Threading and the main thread](/guide/concepts/threading)
- [Key binds and input](/guide/game/keybinds)
- [HUD and Draw2D](/guide/ui/hud-and-draw2d)
- API reference: [`World`](/api/libraries/com/jsmacrosce/jsmacros/client/api/library/impl/FWorld),
  [`Player`](/api/libraries/com/jsmacrosce/jsmacros/client/api/library/impl/FPlayer),
  [`WorldScanner`](/api/classes/com/jsmacrosce/jsmacros/client/api/classes/worldscanner/WorldScanner),
  [`WorldScannerBuilder`](/api/classes/com/jsmacrosce/jsmacros/client/api/classes/worldscanner/WorldScannerBuilder)
