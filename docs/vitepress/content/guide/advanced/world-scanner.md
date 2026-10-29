---
outline: deep
---
# Finding blocks with `WorldScanner`

`WorldScanner` answers questions such as "where are the chests in the chunks around
me", "how much ore is within twenty blocks", or "what can I reach right now". You
describe the blocks once, then ask the scanner to search a shape.

It is worth knowing two things before you build a filter:

- the scanner reads only chunks the client already has, and never asks the server
  for more;
- it remembers the answer for each block state, so the filter is not run again for
  a state it has already seen.

The second point is the one that surprises people, and the one that can make a
reused scanner feel stale. The rest of this page builds up to it.

## The two stages

A scanner has an optional **block filter** and an optional **block-state filter**.
They are stages, not alternatives: a block must pass the block filter, and its
state must then pass the state filter. Leaving one out keeps everything at that
stage.

```js
const scanner = World.getWorldScanner()
  .withBlockFilter("getHardness").is(">=", 10)
  .withStateFilter("isToolRequired").is(false)
  .build();
```

That keeps hard blocks whose state does not require a tool. Adding a state filter
narrowed the result; it did not replace the block filter.

## Describe a filter with method names

`withBlockFilter` and `withStateFilter` take the name of a public, no-argument
method on `BlockHelper` or `BlockStateHelper`. Complete the step with `is(...)`.
The arguments depend on what the method returns:

| Method returns | Complete with | Example |
| --- | --- | --- |
| `boolean` | the expected value | `.is(true)` |
| a number | an operator and a number | `.is(">=", 10)` |
| a string | an operation and text | `.is("ENDS_WITH", "_ore")` |

Numeric operators are `>=`, `>`, `<=`, `<`, `==`, and `!=`. String operations are
`EQUALS`, `CONTAINS`, `STARTS_WITH`, `ENDS_WITH`, and `MATCHES`. They are
case-sensitive, and `MATCHES` tests the whole string against a regular expression.

The method name is not limited to a short list: any public no-argument method that
returns a boolean, a number, or a string works, which is why the reference for
[`BlockHelper`](/api/classes/com/jsmacrosce/jsmacros/client/api/helper/world/BlockHelper)
and
[`BlockStateHelper`](/api/classes/com/jsmacrosce/jsmacros/client/api/helper/world/BlockStateHelper)
doubles as the filter vocabulary.

### Combine filters

Each stage can hold more than one test. Each further test combines with the filter
built so far: `and`, `or`, and `xor` add one more test, and `not` negates everything
accumulated so far.

```js
const scanner = World.getWorldScanner()
  .withStateFilter("isSolid").is(true)
  .orStateFilter("getId").is("ENDS_WITH", "torch")
  .notStateFilter()
  .build();
```

`notStateFilter()` negates the whole state filter built so far. The same four
operations exist for the block stage as `andBlockFilter`, `orBlockFilter`,
`xorBlockFilter`, and `notBlockFilter`.

## Match on the helper's text

Sometimes you want to match against the whole helper text rather than one method's
result. A string filter compares the helper's complete `toString()` output:

```js
const ores = World.getWorldScanner()
  .withStringBlockFilter().contains("_ore")
  .build();
```

`contains` is a literal substring check. `matches` is a whole-string regular
expression, `equals` is an exact match, and `startsWith` and `endsWith` do what
their names say:

```js
const decorative = World.getWorldScanner()
  .withStringBlockFilter().matches(".*_(ore|block).*")
  .build();
```

The text is not just the id: a block's string form looks like
`BlockHelper:{"id": "minecraft:gold_ore"}`, so include the surrounding text in an
`equals` or a whole-string `matches` pattern. `contains` is the easiest to get
right.

Extra arguments are alternatives, so `.contains("chest", "barrel")` keeps either.
This is the most flexible builder filter and also the slowest kind, because it
turns every state into text. Prefer a named method when one exists.

## Use a function when the condition is code

When the test is a real computation, pass two functions instead of a builder
chain:

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

Either function may be `null` to skip that stage. The block function receives a
`BlockHelper`, the state function a `BlockStateHelper`. In TypeScript, annotating
the parameter type tells the wrapper which helper it is being handed:

```ts
const scanner = World.getWorldScanner(
  JavaWrapper.methodToJava((block: BlockHelper) => block.getId() !== "minecraft:dirt"),
  null
);
```

`World.getWorldScanner(blockFilter, stateFilter)` returns `null` when no world is
loaded. The builder path does not add that guard, so check
`World.isWorldLoaded()` before you scan.

## The filter result is cached per block state

A chunk is stored as a small palette of block states plus a grid of palette
indices. A single chunk section can contain thousands of blocks but often only a
handful of distinct states. The scanner exploits that: the first time it meets a
state it runs your filter, stores the answer, and reuses it for every other block
with the same state.

So the filter does **not** run once per block, and it does not run once per match.
It runs once per distinct block state, and the cache lives for as long as that
scanner object does. A second `scanAroundPlayer` on the same scanner reuses the
answers from the first.

That is what makes repeated scans cheap. It also means the filter must depend only
on the block or state it is given. If it reads script state, the cached answer does
not update when that state changes:

```js
let huntingOres = true;

const blockFilter = JavaWrapper.methodToJava((block) =>
  huntingOres || block.getId().endsWith("_ore")
);

const first = World.getWorldScanner(blockFilter, null);
first.scanAroundPlayer(1); // caches an answer for each state it meets

huntingOres = false;
first.scanAroundPlayer(1); // still uses the answers cached above

// A new scanner starts with an empty cache.
const second = World.getWorldScanner(blockFilter, null);
second.scanAroundPlayer(1); // evaluates the new condition
```

`getCachedAmount()` reports how many distinct states a scanner has tested. Compare
it with the number of blocks in the area to see how much variety the filter had to
handle.

## Parallelism

The scanner can split a scan across threads, but only when both filters support
it. Builder filters are plain Java checks and qualify. A
`JavaWrapper.methodToJava` wrapper carries the script context, and a normal script
context is not multithreaded, so a function-based scanner runs sequentially. That
is deliberate: the callback stays on one thread and can read the script context
without racing itself.

## Choose a search area

The scan methods differ in the units they take. Chunk searches take **chunk**
coordinates; area searches take **block** coordinates. A chunk coordinate is the
block coordinate divided by sixteen, rounding down.

| Method | Area | Notes |
| --- | --- | --- |
| `scanAroundPlayer(range)` | chunks around the player | full height; empty when there is no player |
| `scanChunkRange(cx, cz, range)` | a square of chunks | `range` 1 is a 3×3 chunk square |
| `scanCubeArea(pos, range)` | a cube around one block position | `range` 1 is 3×3×3, both ends included |
| `scanCubeArea(pos1, pos2)` | a box between two positions | first included, second excluded |
| `scanCubeAreaInclusive(pos1, pos2)` | a box between two positions | both ends included |
| `scanSphereArea(pos, radius)` | a sphere | origin is offset by half a block; distance is to block positions |
| `scanReachable()` | the player's interaction range | from the eyes; obstructions are not considered |
| `scanClosestReachable()` | the player's interaction range | nearest match, or `null` |

All positions are clamped to the world height. A negative range or radius throws
`IllegalArgumentException`; a chunk that is not loaded is skipped.

```js
const scanner = World.getWorldScanner()
  .withStringBlockFilter().contains("chest")
  .build();

const nearby = scanner.scanAroundPlayer(1);   // chunks around the player
const nearest = scanner.scanClosestReachable(); // or null
```

`scanReachable` measures to each block's unit box and, in its strict mode, also
requires the block's shape to be within reach. It does not check for walls or other
obstructions, so a block behind a wall still counts as reachable.

## Count blocks instead of listing them

If you only need totals, the count methods avoid building a list of positions:

```js
const counts = scanner.getBlocksInChunks(0, 0, 1, true);
for (const key of counts.keySet()) {
  Chat.log(`${key}: ${counts.get(key)}`);
}
```

`getBlocksInChunk(cx, cz, ignoreState)` covers one chunk;
`getBlocksInChunks(cx, cz, range, ignoreState)` covers a square. Both take chunk
coordinates.

The keys depend on `ignoreState`. With `true`, states are grouped under the block
id, such as `minecraft:oak_log`. With `false`, the key includes the state
properties, so different facings or water levels appear separately.

## Keep scans cheap

A scan is synchronous, so it is fine on a script thread but not something to put
on every tick.

- Only loaded chunks are read, so a large range returns less than it promises
  rather than waiting on the server.
- The first scan pays for the filter evaluation; later scans on the same scanner
  reuse the cache.
- A function filter trades parallel speed for safe access to script state.
- A string filter is the slowest builder filter; a named numeric or boolean test
  is cheaper.
- Keep the area small. `scanAroundPlayer(1)` covers nine chunks at full height.

## Related pages

- [Player and world](/guide/game/player-and-world)
- [Threading and the main thread](/guide/concepts/threading)
- API reference: [`WorldScanner`](/api/classes/com/jsmacrosce/jsmacros/client/api/classes/worldscanner/WorldScanner),
  [`WorldScannerBuilder`](/api/classes/com/jsmacrosce/jsmacros/client/api/classes/worldscanner/WorldScannerBuilder),
  [`World`](/api/libraries/com/jsmacrosce/jsmacros/client/api/library/impl/FWorld)
