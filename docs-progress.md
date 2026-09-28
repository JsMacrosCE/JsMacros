# JsMacrosCE — javadoc batch progress

One line per batch. Updated after every batch. `ALL_COMPLETE` is appended when every batch is done.

- [x] batch-01 (client.api.event.impl.world, 14) — done: 14/14 class docs + all 34 public fields
  documented + one `example:` per class. Build green, 14/14 examples type-check under `strict`,
  all 19 `{@link}` anchors resolve. 4 validator-found defects fixed (isCanceled TS2339, wrong
  `@link` overload via missing import, unsupported shutdown claim on EventEntityUnload, incomplete
  bossBar nullability). Doclet quirks recorded in docs-plan.md §"Doclet quirks learned in batch-01".
- [x] batch-02 (client.api.event.impl.player, 18) — done: 18/18 class docs + all 48 public fields
  + 19 `example:` blocks. Build green, 19/19 examples type-check under `strict`, 30/30 `{@link}`
  anchors resolve. 6 validator-found defects fixed: bare `<` in 4 examples silently corrupting the
  shipped Python stubs (the pydoclet DELETES `<`, turning `<=` into `=`), backwards firing order in
  EventHealthChange, wrong EventItemDamage precondition in EventArmorChange/EventHeldItemChange,
  dead StatusEffectUpdate conditions, wrong "health already changed" timing (also corrected in 2
  batch-01 files), and a strict-mode TS failure in the EventDeath respawn example.

  **PRODUCT BUG FOUND (not fixed — a code change, out of scope for this doc effort):**
  `client/mixin/events/MixinHungerManager.java:20` does `new EventHungerChange(foodLevel);` with no
  `.trigger()`, so the `HungerChange` event can never fire. The event is still registered, so
  `JsMacros.on("HungerChange", ...)` silently never calls back. There are 57 event constructions vs
  56 `.trigger()` calls in the mixin package; this is the only one missing it. The javadoc documents
  the dead state accurately. Flagged for a maintainer decision.
- [x] batch-03 (client.api.event.impl.inventory + event.filterer, 10) — done: 7/7 inventory
  events + 3 filterers fully documented (23/23 fields, 26/26 methods, 10/10 `example:` blocks).
  Build green, 10/10 examples type-check under `--strict`. Took 3 rounds: 2 inverted predicate
  descriptions in the packet filterers, and an `EventSlotUpdate` `oldStack`/`slot`/`getInventory()`
  caveat investigation where the doc writer correctly overturned the validator's premise
  (`player.containerMenu` IS the addressed menu in all branches; the `'UNKNOWN'` mismatch is the
  *screen*, not the stack). All 7 events confirmed to call `.trigger()` at every construction site.

  **TYPE BUG FOUND (not fixed — routed to a maintainer):** `SlotUpdateType` is declared via
  `@DocletDeclareType` as `'HELD' | 'INVENTORY' | 'SCREEN'`, but `MixinClientPlayNetworkHandler`
  emits `'HELD' | 'INVENTORY' | 'CONTAINER' | 'UNKNOWN'` and never `'SCREEN'`. The shipped
  `JsMacrosCE-2.0.0.d.ts` is therefore wrong, and `tsc --strict` rejects `type === "CONTAINER"`.
  The javadoc documents the real values and the example avoids the bogus `'SCREEN'`.

  **Investigated and dismissed (recorded so it is not re-raised):** a suspected mismatch between
  `MixinClientPlayNetworkHandler.java:208`'s `method = "handleContainerSetSlot"` and its
  `InventoryMenu.setItem` target. Verified against the 1.21.8 mapped sources: the method *does*
  contain that invoke, and `handleSetPlayerInventory` never calls it. The annotation is correct.

- [x] batch-04 (vitepress-overlap set + core.event.impl remainder, 7) — done: 3 events + 4 core
  classes documented, 7/7 `example:` blocks, 7/7 type-check under `--strict`. **The critical
  property holds: ZERO prose divergence from `origin/backports/1.21.8-vitepress`** on
  EventRecvMessage / EventPlayerJoin / EventPlayerLeave — every vitepress line is carried verbatim
  and the only differences are pure additions (31 / 18 / 19 added lines). `@DocletCategory` was
  correctly not introduced. Took 3 rounds: an unsupported "float widens to double" claim, an
  easy-to-misread EventProfileLoad sentence, and a loose example comment.

  **KNOWN MERGE HAZARD (deliberate, do not "fix" here):** `EventRecvMessage.text`'s additive Note
  says "it is declared nullable here, so script type definitions allow it to be null". That is true
  on this branch (`@Nullable`) and FALSE on vitepress (`@NotNull`). At merge time either drop the
  Note or revert the `@NotNull`. Also: `messageType`'s "As of 1.21.11" version attribution is
  inaccurate (all four tag values exist in 1.21.5) but was kept byte-identical to preserve zero
  divergence — fix it on the vitepress branch, not here.

  **PRODUCT BUG FOUND (not fixed — routed to a maintainer):** `EventWrappedScript` is emitted as a
  key in the shipped `.d.ts` `Events` map, so `JsMacros.on("WrappedScript", ...)` type-checks, but it
  is **never passed to `addEvent(...)` anywhere**, so `FJsMacros.on` throws `IllegalArgumentException`
  at runtime. 57 `Events` keys vs 55 `addEvent` calls. The javadoc now documents this explicitly.
- [x] batch-05 (client.api.event.impl top-level + CommandContextHelper, 10) — done: 9 events +
  CommandContextHelper documented, 18 runnable `example:` blocks, 18/18 type-check under `--strict`.
  Build green. 3 validator corrections applied: a backwards explanation of EventKey's dead
  `InputConstants.UNKNOWN` guard, an over-claim about the GLFW modifier mask set, and 3
  `example:` labels on non-runnable value tables (the only such blocks in the API surface).

  **3 PRODUCT BUGS FOUND (not fixed — code changes, routed to a maintainer):**
  1. `PacketByteBufferHelper.BUFFER_TO_PACKET` is declared public and read, but **never populated
     anywhere in the repo** (exactly 2 sites: the declaration and the read; `init()` only fills
     `PACKETS`/`PACKET_NAMES`). So `toPacket()`, `toPacket(Class)` and `toPacket(String)` are 100% NPE,
     and the public `sendPacket()`/`receivePacket()` chain feeding them is dead too. The javadoc now
     documents this and no example uses `toPacket()`.
  2. `CommandContextHelper` carries `@Event("CommandContext")` and appears in the shipped `.d.ts`
     `Events` map, so `JsMacros.on("CommandContext", ...)` type-checks — but it is never
     `addEvent`'d and `registerHelper(CommandContext.class, ...)` is commented out at
     `ClientProfile:222`, so it throws at runtime. The javadoc documents it as "not a real event"
     and the example uses the command-callback route instead.
  3. **Listener dispatch is asynchronous, so `event.cancel()` is a race for every cancellable event.**
     `FJsMacros.ScriptEventListener.trigger` queues the callback onto `JsMacrosThreadPool` and
     returns, while every mixin reads `isCanceled()` on the next line — so a script's `cancel()`
     almost never lands in time. This contradicts the generated `Events.Cancellable` interface, the
     `cancel()` name, and ~45 classes of prose written in batches 01-05. Scheduled as **batch-22**
     to state the rule once on `BaseEvent`/`BaseEventRegistry`/`IEventListener`/`EventListener`
     rather than scattering a hedge. (A maintainer fix making `trigger()` synchronous for
     cancellable events would instead make all 45 docs correct as written.)

  **ALSO NOTED (harmless):** `ClientProfile:183`+`:192` and `:184`+`:193` are duplicate
  `addEvent(EventRecvPacket/EventSendPacket.class)` lines. Idempotent (they go into `Set`s), so
  dead-but-harmless code.
- [x] batch-06 (api.math + api + api.helper, 7) — done: Pos2D/Pos3D/Vec2D/Vec3D/Plane3D fully
  documented, **all 20 previously-undocumented public fields now documented**, 9 examples
  (Plane3D's removed — see below), 9/9 type-check under the shipped tsconfig AND `--strict`.
  14 validator defects fixed, incl. three false angle claims, the false "PlayerInput turns the
  player" claim, and 3 examples that failed type-check.

  **4 PRODUCT BUGS FOUND (not fixed — code changes, routed to a maintainer):**
  1. `Vec3D.getPitch()` returns **270.0** for a straight-up or zero-length segment instead of ±90 —
     the outer `Mth.wrapDegrees` that vanilla `Entity.getViewXRot` applies is missing from the body.
     Verified by exhaustive sweep (65,160 cases: 360 azimuths × 181 elevations); the only
     out-of-range outputs are exactly 270.0. Real players never see it because `Entity.setXRot`
     clamps. `getYaw()` returns `(-180, 180]` with `+180` at north and `-180` unreachable.
  2. **`PlayerInput.yaw` never reaches the player.** `MixinClientPlayerEntity.overwriteInputs` only
     takes the *sign* of each movement axis (vanilla `KeyboardInput.calculateImpulse` returns only
     `0.0`/`1.0`/`-1.0`); the only `setYRot(yaw)` is the prediction dummy. So
     `Player.moveForward/moveBackward/moveStrafeLeft/moveStrafeRight` — whose sole purpose is to add
     a *relative* yaw — cannot turn the player at all, and `predictInput` disagrees with where the
     player actually goes (`MovementQueue.tick` has a debug log for exactly that). `.pitch` is never
     applied either.
  3. `Vec3D` does not override `Vec2D.to3D()`, so `vec3d.to3D()` silently discards both z
     coordinates — the single hole in an otherwise complete override set.
  4. `Plane3D` is unreferenced dead code AND structurally excluded from the shipped `.d.ts`
     (`tsdoclet/Main.java:41-43` whitelists only `client.api.helper.` and
     `client.api.classes.inventory.`, and `Plane3D` is referenced by nothing). A published public
     class no typed script can construct. Its `example:` was removed rather than shipping a
     `Java.type(...)` snippet that fails both type-check configs.

  **New precedent recorded in docs-plan.md:** a doclet-hostile tag such as `@see` may be
  *replaced* by prose or an `{@link}`, but never silently dropped — and the substitution must be
  reported. (`PlayerInput` removed 7 `@see` tags; 6 were replaced, the 7th on a `private static`
  method was initially dropped and has now been restored as an `{@link}`.)
- [x] batch-22 (core event cancellation semantics, 4) — done: `BaseEvent` / `BaseEventRegistry` /
  `IEventListener` / `EventListener` fully documented — 25 members, 15 empty `@param`/`@return`
  bodies filled, 3 examples. Build green, 0 non-comment lines changed, 0 `deadType` links, all 187
  internal anchors resolve, 3/3 examples type-check under the shipped tsconfig AND `--strict`.
  Took 2 rounds.

  **The central claim survived independent re-derivation from source, link by link:** the two
  convenience `JsMacros.on` overloads pass `joined = false`; `ScriptEventListener.trigger` hands the
  callback to `threadPool.runTask` and returns the container immediately; `BaseProfile.triggerEvent`
  **discards** that container on the plain path; `awaitLock` really parks the caller on the joined
  path; `addEvent(Class)` really puts every cancellable event into `joinableEvents`. **14 mixin sites
  read `isCanceled()` right after `trigger()` — at 13 of 14 it is the literal next statement.**

  **Round 1 defects (validator, 1 finding / 2 instances):** two dead `{@link}`s in
  `BaseEventRegistry` to `EventCustom#joinable`/`#cancelable`, which are `public boolean` — the doclet
  emits `href="" class="deadType"` for any primitive-typed target. Fixed to `{@code}`. This is
  docs-plan quirk 9 biting despite being named explicitly in the writer's brief: listing a rule is
  not the same as a writer hitting it.

  **Round 1 non-blocking findings I escalated into round 2 as real fixes:**
  1. Both examples were **self-defeating** — they called `JsMacros.off(listener)` immediately after
     `on(...)`, so as printed the listener was removed before it could ever fire. Correct code, but a
     reader copying the snippet gets a no-op, in the one batch whose whole thesis is "this is how you
     make `cancel()` work". `BaseEvent`'s now unregisters from *inside* its own callback (verified
     safe: `on()` never invokes the callback, so no TDZ, and `getListeners` returns an
     `ImmutableSet` snapshot so removal mid-dispatch cannot disturb the walk); `IEventListener`'s now
     leaves a watcher registered, which is what `joined = false` actually buys.
  2. **`BaseEvent.joinable()` under-specified its mechanism** — for ordinary events the profile never
     calls `joinable()`, it tests `joinableEvents.contains(eventName)`; only `EventCustom` is judged by
     asking the event. Same outcome, wrong mechanism stated.
  3. **The joined-listener watchdog was undocumented** and batch-22's advice walks straight into it.

  **THE WATCHDOG (documented in this batch, and it is a real trap):** `runJoinedEventListener` arms
  `EventLockWatchdog` on every joined listener; `maxLockTime` defaults to **500 ms**
  (`CoreConfigV2.java:17`). On overrun it calls `closeContext()` — out from under a still-running body
  — `releaseLock()`, and, for a macro trigger only, `trigger.enabled = false`, a **permanent silent
  disable**. `BaseListener.off()` is the same one line. The user's only sign is one logged
  `WatchdogException`; the profile editor still shows the macro as present.

  **Correction I made and the fix-writer caught back (worth keeping):** I asserted that events raised
  from a mixin run on the client thread, so `joinedMain` is true in the ordinary case. **Too broad.**
  `MixinClientConnection` raises `RecvPacket`/`SendPacket` from `channelRead0`/`sendPacket` — the
  **netty** thread — so `checkJoinedThreadStack()` is false there and **no watchdog is armed at all**.
  Identical user code, opposite behaviour, depending on which event fired. The shipped prose hedges
  correctly ("provided the event was raised on a thread the profile treats as joinable").

  **PRODUCT BUGS FOUND (not fixed — code changes, routed to a maintainer):**
  1. The batch-22 bug itself, now with a wider blast radius than batch-05 recorded: the race applies
     to macro triggers too, because `BaseLanguage.trigger` has the identical shape.
  2. `EventLockWatchdog` silently and permanently disables an overrunning macro trigger, and
     `WatchdogException` is a `private static class` — unreachable for a user to inspect, so the
     message string is the only evidence. A user-visible signal belongs here.
  3. The watchdog's thread-dependence is undocumented anywhere user-facing and looks like an
     oversight: a joined listener on a netty-thread event has no time limit, the same listener on a
     client-thread event is killed at 500 ms.
  4. `closeContext()` runs while the script body is still going, and the body's own
     `finally { p.releaseLock(); }` then runs against an already-released lock. **Flagged as
     UNVERIFIED** — nobody traced whether the double release corrupts state. Worth a dedicated look.
  5. `BaseEvent.cancellable()`/`joinable()`/`getEventName()` NPE on a subclass with no `@Event` (no
     null check on the annotation lookup). Documented on each; a null check with a real message is
     the better fix.
  6. `BaseEventRegistry.addEvent(Class)` throws a bare `RuntimeException` where `IllegalArgumentException`
     is the obvious choice.
  7. `removeListener(String, …)` and `getListeners(String)` create map entries for unknown event names
     (`putIfAbsent`/`computeIfAbsent`), so the live `listeners` map accumulates names nothing ever
     registered and no `on()` could have validated.
  8. `addEvent(String, boolean)` and `addEvent(String, boolean, boolean)` are dead surface: only the
     3-arg form is ever called (for `ANYTHING`). Combined with `EventCustom.registerEvent()` using
     only the 1-arg form, a custom event can never be joined by name.

  **Deliberate omission, do not "fix":** `EventListener` has no `example:` block. It is a
  Java-internal macro-trigger wrapper with no script-facing construction path, and an example would
  have had to invent an API. Prose carries it.

  **Known doclet-wide defect (found by the validator, affects every batch):** the web doclet
  **silently drops `{@link}` labels** — `{@link Foo#bar() Bar}` renders as `Foo#bar()`, not `Bar`.
  Not a correctness problem, but labelled links are not doing what their authors think. Stop writing
  them in later batches.
- [x] batch-07 (api.library + core.library.impl, 8) — done: 8 `@Library` entry points
  (Utils/JavaUtils/JsMacros/Reflection/FS/GlobalVars/Request/Time), **49 `example:` blocks** (47
  new; the codebase went from 1 pre-existing example to 50), last 4 documentation gaps filled.
  Build green, 49/49 type-check under the shipped tsconfig AND `--strict`. 12 defects fixed across
  3 rounds, incl. 6 examples that shipped broken TypeScript (a `const file` redeclaring the shipped
  `Packages.java.io.File` global — also a runtime `SyntaxError`; a `waitForEvent` filter callback
  inferring `unknown`; a `net.minecraft` reflection name; two `TS18047` null narrowings).

  **Notable corrections to shipped claims:** `FJsMacros`' keep-alive is *automatic* (creating the
  `MethodWrapper` via `JavaWrapper.methodToJava` is what sets `hasMethodWrapperBeenInvoked`) — the
  docs had been telling users an extra step was needed; `FFS.createFile("", name, true)` works fine
  (the old doc said it was the one case that did not); `FFS.getDir`'s `@throws` was
  `UnsupportedOperationException` when the code throws `NullPointerException`.

  **INCIDENT (resolved, verified):** a fix-writer subagent's throwaway script created a directory
  named `FJsMacros.java`, and its cleanup deleted the real file along with the uncommitted round-2
  javadoc. It recovered the text from the validator's own tool log and replayed the intended edits.
  Validator audited the recovery five ways: 70/70 HEAD declarations still present with identical
  signatures, 0 non-comment changed lines, 0 of 330 HEAD javadoc lines missing, `assertEvent`
  byte-identical to HEAD, and all 215 added javadoc lines individually accounted for. **No prose
  lost.** Orchestrator independently confirmed balanced braces/parens, 45/45 `/**` vs `*/`, 534
  source files, and a green doclet build.

  **3 more maintainer items found (not fixed):** `java.lang.Array` is absent from the shipped
  `Packages` tree, making `Graal.d.ts`'s `type JavaArray<T> = Packages.java.lang.Array<T>` an error
  type for every user (masked by `skipLibCheck: true`); `LibraryParser`'s de-duplication regex
  expects two spaces after `*` where the doclet emits one, so "An instance of this class is passed
  to scripts as the X variable." leaks into all 15 library namespace headers; and the shipped `.d.ts`
  leaks `{@link ...}` verbatim (693 occurrences file-wide) because `genComment`'s cleanup only
  catches resolvable anchors.
- [x] batch-08 (core.library.impl.classes + core.library core, 9) — done: ~130 members documented
  including `ClassBuilder`'s six inner builder classes; 76 `example:` blocks, 76/76 type-check under
  the shipped tsconfig AND `--strict`. 18 defects fixed across 3 rounds. The writer corrected the
  brief: `WrappedScript` is NOT what `JavaWrapper.methodToJava` produces (that is a private
  `JSMethodWrapper` in `extension/graal/.../FWrapper.java`); `WrappedScript` is the concrete
  `MethodWrapper` behind `JsMacros.wrapScriptRun`/`wrapScriptRunAsync`.

  **3 PRODUCT BUGS FOUND (not fixed — code changes, routed to a maintainer):**
  1. **`ClassBuilder.addClinit()` cannot work.** `CtConstructor` hard-codes the method name to
     `<init>`, and `ConstructorBuilder` then sets `ACC_STATIC` for `clInit`, so `toClass()` throws
     `ClassFormatError: Method <init> ... illegal modifiers: 0x9`. It never produces a `<clinit>`.
     Verified twice (writer and validator) against javassist 3.30.2. A fix needs
     `CtClass.makeClassInitializer()` + `addMethod`; `LibraryBuilder`'s generated-constructor path
     must keep using the `<init>` path. The javadoc now says the method does not work and its
     example was removed.
  2. **`HTTPRequest.setConnectTimeout`/`setReadTimeout` are dead** — the fields are written by their
     own setters and read by nothing, and no timeout is ever put on the connection. Note for
     whoever fixes it: a naive fix is worse than the bug, because `URLConnection`'s `0` means
     *infinite*, so it needs a `> 0` guard.
  3. `FileHandler.readBytes()` uses a single `in.read(bytes)`, which is not guaranteed to fill the
     array — a file larger than the stream buffer returns a short array with trailing zero bytes,
     while the doc says "the whole file".
  Also: `Websocket.onConnectError` is the one unguarded handler path (no null check on `onError`,
     no try/catch) — currently unreachable because `connect()` never calls it, but `getWs()` hands
     out the raw client socket, on which nv-websocket-client's `connectAsynchronously()` **does**
     reach it. And `BodyBuilder.appendGuestCode`'s `tokenBefore` is typed `string` in the shipped
     `.d.ts` despite the body null-checking it.

  **Doclet quirks added to docs-plan.md:** a `{@link}` from a nested class to *itself* is a dead
  link (the `clazz.equals(this.type)` branch emits `href="#"` with no anchor) — use `{@code}`; and
  `{@code}` bodies pass through `XMLBuilder` verbatim, so an `&lt;` inside `{@code}` round-trips
  correctly in the browser, while a bare `<` ships malformed HTML.

  **One accepted trade-off:** `{@code &lt;init&gt;}` is right for the web docs but shows the literal
  `&lt;init&gt;` in the shipped Python stub, because `pydoclet` emits `{@code}` bodies raw into a
  docstring where the entity is not decoded. There is no source form that satisfies both doclets;
  trading a Python-stub cosmetic for well-formed web docs is the right call.
- [ ] batch-09 (client.api.library.impl, 7) — in-progress (NEXT)
- [ ] batch-10 (client.api.helper top-level, 19) — pending
- [ ] batch-11 (client.api.classes.worldscanner.filter.**, 17) — pending
- [ ] batch-12 (worldscanner + client.api.classes core, 7) — pending
- [ ] batch-13 (client.api.classes.render + components3d, 12) — pending
- [ ] batch-14 (client.api.classes.render.components, 9) — pending
- [ ] batch-15 (client.api.classes.inventory, 11) — pending
- [ ] batch-16 (client.api.classes.inventory, 11) — pending
- [ ] batch-17 (client.api.helper.world + helper.inventory, 20) — pending
- [ ] batch-18 (client.api.helper.world.entity + helper.screen, 21) — pending
- [ ] batch-19 (entity.specialized boss/decoration/display/other/projectile/vehicle, 21) — pending
- [ ] batch-20 (entity.specialized.mob, 21) — pending
- [ ] batch-21 (entity.specialized.passive, 30) — pending

## In-game test queue (examples that could not be fully verified)

_(empty)_
