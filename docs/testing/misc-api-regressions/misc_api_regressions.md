# Miscellaneous API regression suite — full PR #29

Scope: PR base `61f12e64798806fdf50a9c6f4e3284cca6d179fe` through the
current fixes branch, including the follow-up CodeRabbit fixes and the Graal host
class-loader failure found during client validation. The coverage map
below accounts for **every changed source/documentation file**, not just those
follow-up fixes. It does not include the unrelated local `.gitignore` edit.

**A mapped test is not a passed test.** Client checks and manual scenarios have
not been executed merely because the suite passes a syntax or coverage audit.
Record PASS/FAIL/SKIP for each scenario, with Minecraft version, loader, script
engine, and log/screenshot evidence. Missing fixtures are SKIP, never PASS.

## Running and recording results

For checks that do not require Minecraft, run:

```sh
python3 docs/testing/misc-api-regressions/run_misc_api_core_regressions.py
```

This compiles **complete production source files**, not reimplemented/extracted
methods, against existing Gradle-cached dependencies (or an explicit
`--classpath`). It checks timeout settings on the connection actually used by
each of the eight HTTP overloads, successful delayed-response controls and real
loopback read timeouts, complete FileHandler reads, numeric-filter diagnostics,
and inherited/bridge/missing-method lookup. It needs a JDK and no Minecraft
client. A firewall-induced connect stall is still manual.

Add `--graal` to also run the actual macro's callback-adapter fixture with the
project's pinned Graal JavaScript version and production `IFilter` interface:

```sh
python3 docs/testing/misc-api-regressions/run_misc_api_core_regressions.py --graal
```

This runs Graal in a separate extension loader, with no Graal jars on the mod
classpath. It first reproduces the missing `org.graalvm.polyglot.Value` failure
using parent-only host lookup, then exercises the production host loader bridge.
It verifies original class identities, legacy package aliases, Java.extend for
mod/bootstrap interfaces, Java-to-JavaScript callback invocation, and selection
of both string/interface-overloaded methods. The overload fixture
does not replace in-client scanner assertions or the live scan scenario W1.
Use `--classpath` for core dependencies and `--graal-classpath` for isolated
Graal dependencies if overriding cached dependency discovery; combining them
would invalidate the class-loader test.

When validating the Graal host-loader fix, rebuild the mod/extension and restart
Minecraft first. Reloading only the macro leaves the old host class loader in
memory. Both loader builds embed the Graal extension; ensure the running profile
uses the rebuilt artifact rather than a previously extracted extension jar.

1. Use a disposable profile and world. Run `misc_api_regressions.js` as a
   **non-joined JavaScript macro**, not as a joined trigger or packet callback.
   It creates local objects and temporary listeners but does not register HUD
   drawings, send game packets, change player options, or modify the world.
   Temporary custom event names and ClassBuilder callback-map entries are removed.
   The suite rejects joined/client-thread invocation before running tests. Results
   are written immediately to `logs/latest.log`, then displayed together in chat
   on the client/render thread. A chat-reporting failure does not change test
   counts or replace the original assertion error; consult `latest.log`.
2. Run once on the title screen, then in a loaded world. For full boat dispatch
   coverage put an oak boat, bamboo raft, oak chest boat, and bamboo chest raft
   nearby. Registry-backed packet/enchantment cases require a connection.
3. Set `TEST_FILES = true` to enable the temporary-file round trip (removed in
   `finally`). Set `TEST_HTTP = true` only after starting the loopback fixture:

   ```sh
    python3 docs/testing/misc-api-regressions/misc_api_http_fixture.py
   ```

   No Internet server is needed. The HTTP matrix runs all eight request overloads
   against immediate and delayed responses; it also checks the configured
   connect/read timeout values on the underlying connection. This does **not**
   simulate a stalled TCP handshake; see N1 for that separate case.
4. Execute the scenarios below. Run the matrix on **1.21.5, 1.21.8, 1.21.10,
   1.21.11, and 26.1.2**, on **Fabric and NeoForge**, using Graal JavaScript.
   The cape scenario applies only to versions exposing `getCapeUrl()`.
5. Capture the macro summary and failures. Save a separate result ledger for
   manual tests; a green macro summary excludes those tests. Suggested columns:

   | Case | Version | Loader | Engine | Result | Evidence / skip reason |
   | --- | --- | --- | --- | --- | --- |
   | Safe macro | | | Graal JS | NOT RUN | |
   | G1–G3, I1–I2, W1, E1–E3, L1–L5, N1–N2, D1 | | | | NOT RUN | |

Run `python3 docs/testing/misc-api-regressions/check_misc_api_coverage.py` to detect changed production
files missing from the coverage map, including staged, unstaged and untracked
production sources. Testing tools under `docs/testing/` are intentionally not
part of the production inventory; validate those with syntax and behavioral
checks. It is an **inventory audit**, not runtime
validation and not proof of statement/branch coverage. Pass `--base <commit>`
and `--head <commit> --committed-only` to audit another committed range.

The runner's logging/thread-handoff checks can be run without Minecraft:

```sh
node docs/testing/misc-api-regressions/misc_api_runner_logging.test.js
```

These use mocked client scheduling/host fixtures to check result accounting,
render-thread chat reporting, joined-invocation rejection, class-lookup helper
preservation after entity enumeration, the status-request singleton fixture, and
bundle rejection via registration versus codec validation.
They do not validate Minecraft font rendering, packet codecs or actual Graal
thread handoff.

## Safe local checks

The macro asserts line movement in 16 orientations; child drawing resize;
Draw2D item overloads, static-element preservation and child initialization;
percentage alignment and malformed input; component rotation wrapping; Surface
position ownership; Vec3D pitch/copy; formatting colors; permanent effects;
repair-component absence; independent tooltip visibility; persisted enchantment
removal; 2D/3D coordinate suggestion parsing; all six numeric diagnostics;
inherited filter lookup and unknown methods; all four scanner XOR truth
tables; sequential callback filters and pending-filter guards; packet codec and
phase metadata, ambiguity/errors, named/class decoding and buffer reset;
scoreboard bounds/null teams; BaseEvent defaults and registration; EventContainer
diagnostics/repeated release; cancellable custom `on`/`once` callbacks; and all
four ClassBuilder static-initializer construction paths. File and HTTP checks
are opt-in. The standalone JVM harness additionally checks bridge collisions,
selected declaring classes and invocation. Additional cases below test live
behavior that local objects cannot.

Use a **distinct child Draw2D for each independently sized wrapper**. Builders
do not copy children; reusing one child replaces its single pair of dimension
suppliers. The suite does not claim independent multi-wrapper ownership.

## G1 — Visible line rasterization

Create/register one Draw2D with horizontal, vertical and diagonal lines of
widths `-1`, `0`, `0.25`, `1`, `1.5`, and `3`, including non-integral diagonal
lengths, endpoint reversal, and a zero-length line. Use contrasting colors and
capture screenshots at GUI scales 1 and 3. Expected: nonpositive widths draw
nothing; positive fractional/one-pixel lines remain visible; diagonal lengths
round up without dropping the endpoint; thicker lines have the expected
coverage. The zero-length case must not crash. Unregister the drawing afterward.
This tests `Line.render()`, not just its geometry getters.

## G2 — ScriptScreen close and Draw2D resize lifecycle

On the client thread, open a `Hud.createScreen()` with no parent and close it
with Escape: the screen must close normally to the game/title screen. Repeat
with an explicit parent: closing must reopen that parent. Confirm close callbacks
still execute once. Register a static Draw2D with a nested child that has an
`onInit` callback; resize the window and change GUI scale. Static parent elements
must survive and the child callback must rerun. Repeat with a parent `onInit`
callback that rebuilds elements, and with a throwing parent callback plus its
error handler: surviving child elements must still initialize. Unregister/close
everything and restore GUI scale.

## G3 — FOV option setter isolation

Save `Minecraft.getInstance().options.fovEffectScale().get()` and
`hideLightningFlash().get()`. On the client thread call
`Client.getGameOptions().getAccessibilityOptions().setFovEffect(false)`, then
`true`. Expect FOV scale `0.0`, then `1.0`; lightning suppression must remain
unchanged. Restore both **original** values in `finally`, including an original
intermediate FOV scale. Do not save modified options to disk.

## I1 — Empty and populated enchanting screen

Open an empty enchanting table. Through the resulting `EnchantInventory`, call
`getEnchantments()`, `getEnchantmentHelpers()`, and `getEnchantmentIds()`. Each
must return three null slots without throwing for the `-1` clues. Insert an
enchantable item and lapis with sufficient levels: every available clue must
produce consistent name/helper/id/level entries; unavailable clues stay null.
Remove the item and repeat the empty checks. Do not select an enchantment.

## I2 — Loom pattern upper bound

Open a loom with banner and dye. Use `listAvailablePatterns().size()` to obtain
the pattern count `n`. `selectPattern(-1)`, `selectPattern(n)`,
and `selectPattern(n + 1)` must return false, leave the selected pattern alone,
and send no inventory-button packet. Valid indices `0` and `n - 1` must select
their corresponding patterns when applicable. Repeat with a pattern-item
restriction and with empty inputs (`n` may be zero). Use SendPacket logging to
verify rejected indices do not produce `ServerboundContainerButtonClickPacket`.

## W1 — Chunk inclusive top and scanner callback execution

In a disposable creative world put a distinctive block at the chunk's
`getMaxY()` and another at `getMinY()` (compare the actual chunk bounds, not a
hard-coded height). Call `ChunkHelper.forEach(true, callback)` and assert
exactly `16 * 16 * (maxY - minY + 1)` positions, both boundary blocks, and no
duplicates/out-of-range Y. Repeat with `includeAir = false`: both blocks remain
and no air blocks are reported.

Build scanners using **each** callback overload `withBlockFilter(IFilter)` and
`withStateFilter(IFilter)`, and a combination with native AND/OR/XOR/NOT filters.
Scan that chunk and compare returned positions with a direct traversal oracle.
Record the callback thread; callback-bearing scanners must stay sequential and
must not produce Graal concurrent-context errors. Compare with equivalent
string/native scanner results. Exercise replacing a completed callback filter
and rejecting replacement of an incomplete named filter. Do not count inspecting
the `useParallelStream` flag alone as live scanning coverage.

## E1 — Entity helper nulls, flags and particle colors

Place the following fixtures in a disposable world and query them through
`World.getEntities()` (missing entities are SKIP):

- Guardian with no resolvable beam target: `getTarget()` is null without an
  exception. With a loaded target it wraps that target. During motion/stillness,
  `hasSpikesRetracted()` equals the raw guardian's `isMoving()` (observe both).
- Dolphin with client `treasurePos == null`: `getTreasurePos()` is null. For the
  non-null branch, inspect a server-thread dolphin with a treasure position and
  compare all coordinates; do not treat unsynchronized client state as server
  truth.
- Colored area-effect cloud: `getColor()` equals ARGB reconstructed from the
  **particle** alpha/red/green/blue, not its team/name color. A cloud using a
  non-color particle (e.g. smoke) returns `-1`. Use a nonwhite color so the old
  team-color implementation cannot pass accidentally.
- Every boat-family variant in the safe macro, plus a boat under source water,
  one under flowing water, and one in air/on land. `isUnderwater()` must be true
  for both `UNDER_WATER` and `UNDER_FLOWING_WATER`, false for other statuses.
  Compare against the raw `AbstractBoat.status` and record that both underwater
  states were actually observed.
- End crystals with both show-bottom values: `isNatural()` equals
  `showsBottom()`. Spawn one artificially with its bottom shown to demonstrate
  that the method is not proof of natural origin (documentation-only change).

## E2 — Integrated-server entity lookup and thread ownership

In singleplayer look up an existing client entity with `asServerEntity()` from
a non-joined script thread. It must return a helper for the same UUID and
dimension. **Access the returned helper only on the integrated-server thread**:
use `server.submit(java.util.concurrent.Callable)` and a synchronous
`JavaWrapper.methodToJava(...)` wrapper, then wait off the client thread. Test
lookup directly on the server thread as well to exercise the non-submitting
branch. Remove the server entity while retaining its old client helper: lookup
must return null. Repeat on a dedicated server: lookup must return null, not
UnsupportedOperationException. If testing interrupt handling, interrupt only
the disposable caller thread while it waits; expect IllegalStateException and
its interrupt flag preserved. Never block the client waiting for a callback
that itself needs the client thread.

## E3 — Stats identity and player-list optional data

Have at least two distinct nonzero stats of the same type (e.g. used/broken
items), and request a statistics refresh. With the synced `StatsHelper`, compare
every `getStatList()` key to the corresponding raw `Stat.getName()`. For each
key verify raw value, formatted value, text, raw-map and formatted-map entries.
Both same-type stats must remain separate rather than colliding under a type
translation key. Missing keys must throw IllegalArgumentException.

For a player without a signed chat session, `getPublicKey()` must be null; for
one with a session it must equal the encoded raw profile key. On **1.21.10+**
use a profile with visibly distinct skin and cape assets, wait for the skin
future, and check `getCapeUrl()` against the downloaded **cape**, not body,
asset URL. A missing/non-downloaded cape returns null. Earlier versions without
this method are N/A, not a pass for the changed branch.

## L1 — Joined/cancellable dispatch and callback completion matrix

Use separate producer and listener macros/services so cross-context execution
really occurs. Register a custom event and set its `joinable`/`cancelable` flags
as appropriate. Test `on` and `once`, named and `ANYTHING` listeners, all four
joinable/cancellable combinations, and joined versus non-joined listeners.
Write a callback-complete marker after a short sleep (well below watchdog time).
When joining is required, the producer must see completion before `trigger()`
returns; cancellable events must reflect cancellation synchronously even when
the listener wasn't explicitly joined. Non-joinable events must not join merely
because the listener requests it. Async cases require a bounded wait for the
marker, not an immediate assumption of completion. Verify `once` fires once.

Repeat with an actual cancellable SendPacket/RecvPacket event in a disposable
connection (cancel only a selected harmless packet, not all packets), including
`ANYTHING`. Verify cancelled packets aren't dispatched and callbacks complete
before network continuation. Keep callbacks short. Test same-context non-joined
cancellable listeners inline (also automated), filter rejection, exceptions
(listener removed/logged, locks released), and a same-context **explicitly
joined** listener: it must retain the IllegalThreadStateException guard rather
than deadlock. Remove all listeners/services and custom registry names.

## L2 — Watchdog quarantine, closure and non-main producers

This intentionally stalls scripts; use a disposable profile.

1. Save the lock-time setting, then set it to 500 ms.
2. Create a macro containing `Time.sleep(2000);` and an enabled **joined** key
   trigger. Press once: expect WatchdogException, trigger disabled, context
   closed, joined caller released before two seconds, and no stale joined-thread
   or context event entries. Press again: no execution/new timeout.
3. Repeat with a joined event trigger and with a cross-context custom-event
   producer running off the client thread. The watchdog must run even when
   `checkJoinedThreadStack()` is false; a timed-out persistent listener must
   be quarantined via `off()` before context closure.
4. Repeat with a temporary script-added listener: context closes, listener is
   cleaned up and caller released. A normally completed short joined callback
   must not be disabled or produce a late timeout.
5. Remove fixtures and restore settings. After each case fire a short joined
   event successfully to detect lingering locks.

## L3 — EventContainer idempotent release with a waiter

Create a container with a real disposable context and lock thread; register a
waiter with a completion counter using `awaitLock()`. Release it twice, including
concurrent release attempts from two Java threads. Expected: waiter wakes,
completion callback runs once, context events and joined-thread entry removed
once, no exception. Test `toString()` before a lock thread is assigned (also
automated). Do not use the macro's own context for destructive close tests.

## L4 — CommandContext dispatch and Fabric command-builder guard

Register a unique temporary client command with an execution callback, and
listeners for `CommandContext` and `ANYTHING`. Execute the command: expect one
event carrying the same raw command context as the callback. Capture ordering
using a joined listener from a **separate** context: event dispatch must precede
the execution callback. On Fabric, `or()` on a fresh command builder must throw
AssertionError with `Can't use or() on the head of the command`; valid branching
after arguments must still register and execute both branches. Run the common
command event test on NeoForge too. Unregister command and listeners.

Also construct an event ScriptTrigger for a never-registered event name and call
`EventRegistry.removeScriptTrigger()` without adding it: expect false, not NPE.
Add/remove a real trigger and remove it a second time: true, then false. Do not
modify existing user triggers or clear the whole registry.

## L5 — WrappedScript dispatch across all invocation paths

Register a `WrappedScript` observer and wrap a tiny script that logs its
arguments and sets `event.result`. Exercise `accept(t)`, `accept(t,u)`, `apply(t)`,
`apply(t,u)`, `test(t)`, `test(t,u)`, `run()`, `compare(a,b)`, and `get()`. Supply
appropriate Boolean/Number results for predicate/comparator paths. Each generated
EventWrappedScript must be observed once and pass its arguments/result through.
Repeat accept/run for async wrappers with a bounded completion wait. Passing an
existing BaseEvent to single-argument `accept()` must **not** generate a spurious
WrappedScript event. Verify registration in a clean profile (also automated).
Use the observed comparator arguments as well as its result; this suite covers
changed dispatch, not an assumption about unrelated comparator semantics.
Remove observers and close only the disposable wrapped contexts.

## N1 — HTTP connect deadline and packet phase-specific codecs

The local HTTP fixture and opt-in macro cover read timeouts for get, post
string/bytes, put string/bytes, and send method/string/bytes, plus timeout
configuration. To test an actual **connect** timeout, use a controlled local
network namespace/firewall that drops TCP SYNs to a known test endpoint. Expect
SocketTimeoutException near the configured deadline for each overload, with a
generous upper bound. Refusal/unknown host is not timeout coverage. Do not point
these checks at random public IPs or change the host firewall as part of the
safe macro.

For packet codecs, capture a configuration-phase custom payload and a
play-phase custom payload (and ShowDialog on versions where present). Explicitly
construct/decode with the recorded phase and compare their fields/bytes, proving
the configuration/context-free versus gameplay codec selection. Test packet
round trips in handshake, status, login, configuration and play using a fixture
packet for each; compare metadata IDs with the actual ProtocolInfo packet list,
not only `id >= 0`. Test shared packets' explicit phase overloads and current
connection preference, unique packets off-world, wrong/unknown phase, unknown
class/name, bundles without codecs, and unregistered packets. Verify both event
support queries after replacing/nulling `event.packet`.

Repeat registry-backed reset with a **nonempty** item stack carrying registry
data and with a caller-supplied RegistryFriendlyByteBuf containing known bytes.
After multiple decode/reset cycles, original bytes, indices, buffer subtype and
the original RegistryAccess must be preserved. Repeat plain caller-supplied and
default-buffer constructors off-world and in-world. No packet should be sent
merely to test encoding/decoding. A support query checks codec availability,
not whether malformed packet contents can be encoded.

## N2 — WebSocket callback-specific error contexts

Start the loopback fixture. Run each of these in a fresh non-joined macro:

```js
const Websocket = Java.type('com.jsmacrosce.jsmacros.core.library.impl.classes.Websocket');
const ws = new Websocket('ws://127.0.0.1:18729/ws/text');
ws.onTextMessage = JavaWrapper.methodToJava(() => {
  throw new Error('misc-api expected text callback failure');
});
ws.connect();
Time.sleep(1500);
ws.close();
```

Leave `onConnect` null. Repeat changing URL/callback to `/ws/disconnect` /
`onDisconnect`, `/ws/error` / `onError`, and `/ws/frame` / `onFrame`. Expect the
deliberately thrown callback error to be logged through **that callback's**
context, never a secondary NullPointerException from `onConnect.getCtx()`.
Record that each callback actually ran. Repeat with `onConnect` in a separate
live context and prove errors use the failing callback's context; repeat with a
contextless Java MethodWrapper to exercise stderr fallback. Close all sockets.
Expected diagnostic errors are not test failures; missing/secondary errors are.

## D1 — CustomImage creation, drawing and filesystem handling

Run image/texture operations on the client thread, forwarding assertion failures
back to the test macro (FClient's main-thread helper logs thrown errors).

1. Create a 2×2 destination BufferedImage and a differently colored source image.
   Construct CustomImage from the destination and use the region `drawImage(img,
   ...)` overload. Inspect destination pixels: they must match the **source**
   region, not redraw the destination itself. Use distinct quadrant colors to
   catch wrong cropping/scaling. Close the texture afterward.
2. Call `saveImage()` with a unique nested path under the disposable config
   folder. Assert parents are directories, the `.png` target is a regular file
   (not a directory), and ImageIO reads the expected dimensions/pixels. Repeat
   saving over that file and writing into an already existing parent directory.
   Test an unwritable/file-valued parent: expect logged failure, no malformed
   directory named `*.png`. Delete only these explicitly created fixtures.
3. Write non-image bytes to a unique config-relative file and pass that path to
   `CustomImage.createWidget(path, name)`: expect null and a logged read error,
   not an NPE/texture registration. Repeat with a missing file and a valid PNG;
   valid PNG yields an image with correct dimensions. Close/remove all fixtures.

## Coverage map

Source paths below are relative to
`common/src/main/java/com/jsmacrosce/jsmacros/`, except the explicit
repository-relative paths. **A** = automated client-macro assertions,
**O** = opt-in automated, **M** = manual runtime scenario,
**D** = documentation/annotation verification, **J** = standalone JVM checks.
Combined modes are intentional.

| Source | Coverage | Mode |
| --- | --- | --- |
| `api/math/Vec3D.java` | Pitch directions/zero vector and independent 3D copy | A |
| `client/api/classes/CustomImage.java` | D1: source region, nested PNG parents, null image decode | M |
| `client/api/classes/inventory/CommandBuilder.java` | L4: CommandContext event before execution | M |
| `client/api/classes/inventory/EnchantInventory.java` | I1: all three getter arrays, missing/present clues | M |
| `client/api/classes/inventory/LoomInventory.java` | I2: rejected upper bound/no packet and valid selection | M |
| `client/api/classes/render/Draw2D.java` | Item overloads, init retention/child callbacks; G2: resize/errors | A+M |
| `client/api/classes/render/ScriptScreen.java` | G2: parent/no-parent close and callback | M |
| `client/api/classes/render/components/Alignable.java` | Both percentage sides, malformed split inputs, axes/builders | A |
| `client/api/classes/render/components/Draw2DElement.java` | Child dimension suppliers, resize, rotation constructor/setter | A |
| `client/api/classes/render/components/Image.java` | Constructor rotation wrap | A |
| `client/api/classes/render/components/Item.java` | Constructor rotation wrap | A |
| `client/api/classes/render/components/Line.java` | Move element/builder directions, rotation; G1: raster widths/rounding | A+M |
| `client/api/classes/render/components3d/Surface.java` | Builder/sibling/built-position independence | A |
| `client/api/classes/worldscanner/WorldScanner.java` | Sequential callback flag; W1: live sequential scan | A+M |
| `client/api/classes/worldscanner/WorldScannerBuilder.java` | Four XOR paths, state stringify, callback overloads/pending guard; W1 | A+M |
| `client/api/classes/worldscanner/filter/ClassWrapperFilter.java` | Inherited/Object/bridge lookup and unknown-name diagnostics | A+J |
| `client/api/classes/worldscanner/filter/compare/NumberCompareFilter.java` | >= diagnostic and operation for all numeric wrappers | A+J |
| `client/api/event/impl/EventRecvPacket.java` | Supported/unsupported/replaced packet support; N1: actual codec phases | A+M |
| `client/api/event/impl/EventSendPacket.java` | Supported/unsupported/replaced packet support; N1: actual codec phases | A+M |
| `client/api/helper/FormattingHelper.java` | Colors/modifiers/reset sentinel and toString | A |
| `client/api/helper/OptionsHelper.java` | G3: FOV true/false and unrelated lightning setting isolation | M |
| `client/api/helper/PacketByteBufferHelper.java` | Round trip/reset, registry, names, metadata/errors; N1: phase/ID oracle | A+M |
| `client/api/helper/StatsHelper.java` | E3: per-stat list/getters/maps and same-type collision | M |
| `client/api/helper/StatusEffectHelper.java` | Infinite, finite and expired effects | A |
| `client/api/helper/SuggestionsBuilderHelper.java` | Whitespace, 2D/3D, empty/malformed/mixed inputs | A |
| `client/api/helper/inventory/CreativeItemStackHelper.java` | Removal persistence, unrelated retention, id/helper overloads | A |
| `client/api/helper/inventory/ItemHelper.java` | Absent repair component returns false | A |
| `client/api/helper/inventory/ItemStackHelper.java` | Independent enchantment/modifier/unbreakable visibility | A |
| `client/api/helper/world/ChunkHelper.java` | W1: min/max Y, counts, includeAir branches | M |
| `client/api/helper/world/PlayerListEntryHelper.java` | E3: nullable public key and version-gated cape/body source | M+D |
| `client/api/helper/world/ScoreboardsHelper.java` | Team/slot bounds, nullable team/color/objective and toString | A |
| `client/api/helper/world/entity/EntityHelper.java` | Boat-family dispatch; E2: integrated-server/thread/absent cases | A+M |
| `client/api/helper/world/entity/specialized/decoration/EndCrystalEntityHelper.java` | E1: show-bottom documentation, no origin assertion | M+D |
| `client/api/helper/world/entity/specialized/mob/GuardianEntityHelper.java` | E1: nullable target and moving/retracted equivalence | M |
| `client/api/helper/world/entity/specialized/other/AreaEffectCloudEntityHelper.java` | E1: particle ARGB versus team color and non-color sentinel | M |
| `client/api/helper/world/entity/specialized/passive/DolphinEntityHelper.java` | E1: null and present treasure positions | M |
| `client/api/helper/world/entity/specialized/vehicle/BoatEntityHelper.java` | All chest-family detection; E1: both underwater statuses | A+M |
| `client/config/ClientProfile.java` | CommandContext registered; L4: real command dispatch | A+M |
| `client/event/EventRegistry.java` | L4: never-registered and repeated trigger removal | M |
| `client/mixin/events/MixinClientPlayerEntity.java` | Movement scenario below: queued yaw/pitch applied | M |
| `client/mixin/events/MixinHungerManager.java` | Hunger scenario below: changed-only dispatch | M |
| `client/movement/MovementDummy.java` | Movement scenario below: simulated yaw/pitch | M |
| `core/EventLockWatchdog.java` | L2: timeout/off/close/release, successful completion, exception type | M |
| `core/config/BaseProfile.java` | Custom inline dispatch/registration; L1/L2: joining/ANYTHING/watchdog | A+M |
| `core/event/BaseEvent.java` | Annotation-free name/join/cancel defaults | A |
| `core/language/EventContainer.java` | Null-thread toString/release twice; L3: waiter cleanup once | A+M |
| `core/library/impl/FJsMacros.java` | on/once inline completion; L1: apply/accept, filters/errors/thread guard | A+M |
| `core/library/impl/classes/ClassBuilder.java` | Java/guest/buildBody/callback clinit, null receiver, mixed body | A |
| `core/library/impl/classes/FileHandler.java` | Empty/multi-megabyte full byte read, cleanup | O+J |
| `core/library/impl/classes/HTTPRequest.java` | All eight overloads, actual configured/read deadline; N1: connect stall | O+J+M |
| `core/library/impl/classes/Websocket.java` | N2: all four failing callback contexts, null onConnect/fallback | M |
| `core/library/impl/classes/WrappedScript.java` | L5: dispatch for every invocation route/async and existing event | M |
| `fabric/src/main/java/com/jsmacrosce/jsmacros/fabric/client/api/classes/CommandBuilderFabric.java` | L4: invalid head or() and valid branch execution | M |
| `extension/graal/src/main/java/com/jsmacrosce/jsmacros/graal/language/impl/GraalLanguageDefinition.java` | Isolated Graal host-loader bridge; live Java.extend(IFilter) scanner macro | J+A |
| `extension/graal/src/main/java/com/jsmacrosce/jsmacros/graal/language/impl/GraalHostClassLoader.java` | Split-loader adapter creation, class identity, legacy alias and missing-type behavior | J |
| `KNOWN_ISSUES.md` | E1/E2: client-state caveats; villager scenario below; L1/L2: sync packet callbacks | D+M |

### Movement and hunger scenarios

- Queue a `PlayerInput` with nonzero yaw and pitch and zero movement in a
  disposable world. After the queued tick, assert raw player yaw/pitch match
  the input; repeat with forward movement and another angle to ensure direction
  follows the new yaw. Simulate the same input using MovementDummy and verify
  its `getYRot()` and `getXRot()` match too. Restore original view and clear only
  test queue inputs. The simulation alone does not test the player Mixin.
- Register a HungerChange listener from a separate context. Change the client
  food level on the client thread (or induce a real survival food update):
  exactly one event contains the new value. Setting the same value again must
  not dispatch. Restore the original food level and remove the listener. Do not
  use event construction as evidence the Mixin called `trigger()`.

### Documentation-only/client-state caveats

With a trading screen open, verify `VillagerInventory.getTrades()` supplies
offers, while a merchant entity helper is not claimed to synchronize/refresh
them. Compare horse-owner/wolf wetness client state against server-thread state
and record the limitation without asserting equality. End-crystal show-bottom
and integrated-server ownership are covered by E1/E2. These entries validate
the changed `KNOWN_ISSUES.md` descriptions, not nonexistent implementation fixes.
