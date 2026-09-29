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
- [x] batch-09 (client.api.library.impl, 7) — done: 202/207 members, **199 `example:` blocks** (the
  package had zero), 4 gaps closed, 36 duplicate `@param`/`@return` removed. Committed `39053d29`.
  Build green, **199/199** examples type-check, 0 non-comment lines, 0 `@Doclet*` changes, 6/7 stubs
  `ast.parse` (FWorld.py:21 is the pre-existing `@Nullable` TypeVar mangling). Took **4 rounds** plus a
  conditional pass — the largest batch so far, and the one that produced the reusable findings.

  **The four defects that were found, in order:** 13 `&lt;`/`&gt;` examples shipping broken JS into the
  Python stubs; an `y = 10` gloss calling it "sea level"; a duplicate `@param gameMode` that silently
  deleted a whole example from `FPlayer.py`; a dead `{@link Reflection#getClass(String)}` (no such
  class — it is `FReflection`); an `Array.from` rewrite that didn't type-check; and a wrong
  `createTexture` null contract (a readable non-image NPEs in `CustomImage`'s ctor rather than
  returning null).

  **Two rules I got wrong and a subagent caught — the most transferable outcome of the batch:**
  - I claimed "pydoclet deletes `&` and `<`" and cited **Java code lines** as javadoc evidence. There
    are in fact **zero** bare `&` in these files' javadoc; the writer had already done the nested-`if`
    rewrite correctly.
  - I then claimed "a bare character is always fine." A fix-writer built it, and the `.py` was
    **still broken**: a bare `&` and bare `<` are *not* `TEXT` in javac's parser — they are non-`TEXT`
    trees that `pydoclet` (no `default:` arm) drops while `tsdoclet`/`webdoclet` keep them.
    **`{@code X}` is the only form correct in all three.** My earlier "no `<` and no `&` in an example"
    rule was *over*-broad, needlessly contorted 13 working examples, and **caused** the `Array.from`
    type error.
  - **Method that caught both: run the doclets and diff the artefacts.** Inferring behaviour from
    doclet source was wrong three times. Now recorded in docs-plan.md and in
    `.opencode/references/doclet-behaviour.md`.

  **Accepted trade-offs (flagged, not fixed):** `FPlayer.writeSign`'s example lost its "skip lines
  over 15 chars" guard, since that cannot be written without `<`/`>` and `Math.min(l.length,15)===l.length`
  is unreadable; the method's prose still states the real limit. The two TPS examples no longer branch
  on a threshold, they just log. `FChat` now documents the same literal two ways (`{@code &a}` in
  prose, `"\x26a"` in the example) — **both correct, only one can be canonical; that is a maintainer's
  editorial call, not a defect.**
- [x] batch-10 (client.api.helper top-level, 18) — done: 3760 added javadoc lines, **130 `example:`
  blocks** (the package had zero), 29 method gaps + 8 fields + 2 class docs closed.
  Build green, 0/18 files with non-comment changes, **0 `&` bytes in all 18 generated `.py`**,
  130/130 examples type-check, 7/7 rounds-1-4 fixes held. **Took 6 validation rounds** — by far
  the most of any batch, and the one that produced the transferable findings.

  **SCOPING CALL I MADE UP FRONT (this changed what "done" meant):** the audit tool reported ~40
  undocumented members, but **12 were `toString()` overrides that must stay bare** (documenting
  them injects new members into the shipped `.d.ts` per quirk 13/16). Stripping those out cut the
  real work to 29 methods + 8 fields + 2 class docs. I also told the writer outright that fewer
  correct examples beat many thin ones, and to report what it left undone — which is why
  `OptionsHelper` (233 members) got 3 examples rather than 233.

  **THE DOMINANT DEFECT CLASS — and why 6 rounds were needed.** Every blocking defect in every
  round was the same thing: **a confident, specific, plausible claim about Minecraft semantics that
  nobody had checked against the implementation.** In order, across rounds:
  `getColorValue()` "gives 0 for modifiers" (it **throws** — `getColor()` returns a boxed `Integer`);
  "`dark_red` is `r`" (it is **`4`**, the code is the hex digit of the colour index); an example
  calling **`getRawId()`, a method existing nowhere in the repo**; "9 is white" (9 is BLUE, white
  is 15); "the firework shade is usually brighter" (lower for 11 of 16); `getClickAction()`
  documented with the *hover* action's lowercase ids when it returns **uppercase** `Enum.name()`;
  `BlockPredicateHelper` claiming `test()` applies the data-component matcher when it reaches the
  `BlockInWorld` overload that never reads it; and a packet census of 202 counting **Stonecutter-
  gated entries that do not ship in 1.21.8** (the real figure, from the compiled class, is 200).
  **~20 defects in total, none of which a build, a `tsc` or a `node --check` can see.**

  **THE METHODOLOGICAL FINDING — the most transferable thing this batch produced.** Rounds 1–4 each
  ran "pair every `@return` with its own body" and each found more. That method is **structurally
  incapable** of finding the round-4/5 defects, because *all* of them are prose about something
  **other than** the method's own return. The audit has to be **inverted**: enumerate every
  `{@link}` target, every relational sentence ("the same as", "the opposite of", "its siblings",
  "unlike"), every bare numeral and every `always`/`never`/`only`/`all`/`most`/`exactly`, and resolve
  each against **the target's** body. That is what found the last 8, including a `&&` shipping
  broken JS. Corollary rule for the writers: **"the overload next to it does X" is not evidence
  about "this one does Y"** — that exact carry-over produced two separate defects.

  **`node --check` IS NOT SUFFICIENT — a new blind spot.** A deleted `&&` leaves
  `if (aimed !== null aimed.getX() === 10 …)`, which is **still valid JavaScript that means
  something entirely different**, so `node --check` passes it. A green build, a clean type-check
  and a clean `node --check` all passed while broken JS shipped. **The byte count is the only
  reliable check** for this defect class.

  **TWO VALIDATOR FINDINGS WERE FALSE — I DISPROVED BOTH FROM BYTECODE BEFORE THEY COULD BE
  "FIXED". This is the batch's most important process lesson.**
  - **DyeColorHelper colour splits (round 5):** reported inverted (firework smaller for 4/16, sign
    for 5/16). Wrong. The validator's `ldc`-only parse **silently dropped `BLUE` and `BLACK`**, whose
    third constants are `sipush 255` and `iconst_0`. Correct: firework lower for **11/16**, sign
    lower for **4/16** = exactly `{blue, black, cyan, green}` — which is what the docs said.
  - **DyeColorHelper alpha byte (round 6):** reported as a single blocking defect claiming there is
    no alpha byte at all. Wrong. `ARGB.opaque(int)` is `ldc -16777216` + `ior`, and the **constructor**
    applies it to `textureDiffuseColor` and `textColor` but not to `fireworkColor`; the three
    getters return those fields directly. The validator read the **static-init literals** (all
    ≤ `0xFFFFFF`) and missed the constructor. Worked proof: WHITE `0xF9FFFE` → `0xFFF9FFFE`;
    `/65536` = 65529 vs the true red byte 249 — off by exactly `0xFF00` = 65280, which is the
    number the doc already gave.
  **Shared root cause: when reading a static initializer, small constants appear as
  `iconst_*`/`bipush`/`sipush`, not only `ldc`; and `PacketByteBufferHelper` uses `ldc_w`
  exclusively, so an `ldc`-only parse finds 0 keys.** A tally that comes out short means the parse
  dropped entries, not that the data is smaller. **Both files were re-derived twice, independently,
  and the docs were right both times.** Had I trusted the reports, two correct files would have been
  corrupted.

  **Round 4's inverted audit found 8 more** (7 blocking + 1 whose premise was false and which the
  writer correctly **refused** to "fix" — `VehicleMoveS2CPacket` is put once; the second line is a
  different name for a different class, and the compiled map has 200 distinct keys → 200 classes).
  That refusal is the correct instinct and worth keeping.

  **Source defects found (not fixed — code changes, routed to a maintainer).** Batch-05's
  `BUFFER_TO_PACKET` finding is confirmed and now precisely documented: the map is read and never
  written, and **3 further maps in the same class (`PACKET_IDS`, `PACKET_STATES`, `PACKET_SIDES`)
  are populated only by a commented-out block**, so `getPacketId()`/`getNetworkStateId()` always
  answer `0`, `isClientbound()` always `false` and `isServerbound()` always `true`. Also new:
  `OptionsHelper.AccessibilityOptionsHelper.setFovEffect(boolean)` writes `hideLightningFlash`
  (batch-10 corrected the **documentation**; the code is still wrong); `DyeColor`'s three shade
  constants are independent per-colour values, not a derived lightening; `SuggestionsBuilderHelper
  .suggestPositions` reads `split[0..2]` unconditionally so a 2-part string throws
  `ArrayIndexOutOfBoundsException`; `FormattingHelper.getColorValue()` NPEs on any of the 5 modifiers
  and on `RESET`, and `isModifier()` is **not** the complement of `isColor()` because `RESET` answers
  false to both; `StatusEffectHelper.isPermanent()` is hard-coded `false`; `StatsHelper`'s keyed
  stat calls key on a *category*, so `getRawStatMap`/`getFormattedStatMap` collapse to one entry per
  category; `BlockPredicate` has a 4th `components` part that `test()` cannot reach; and 6 of 1520
  vanilla advancements are in the minority shapes the docs had claimed were the majority.

  **Accepted / deliberate:** `DyeColorHelper` has **0 examples** — validated as correct, because
  `docs/typescript/headers/McIdsAndEnums.d.ts:15` is `type EntityTypeFromId<E> = EntityHelper;`,
  discarding `E`, so *every* route to a `DyeColorHelper` is a `tsc` error. The class doc says so.
  `OptionsHelper`/`InteractionManagerHelper`/`PacketByteBufferHelper` got 3/6/6 examples for
  233/51/148 members — thin but each example is dense and the per-member `@param`/`@return` are
  complete. **This is the recorded example backlog for this package.**

  **Known docgen defects newly characterised:** `pydoclet` emits field *declarations* but never
  field *javadoc*, so the 3 examples on `CommandNodeHelper.fabric` and `OptionsHelper.parent` are
  **invisible to Python readers** (unfixable in javadoc). `pydoclet` also has **no `@throws`
  support at all** (0 `Raises:` across 632 generated files vs 47 Java files using `@throws`).
  `tsdoclet` and `pydoclet` **disagree on `char` returns** — `number` vs `str`; the pydoclet is
  correct, since Truffle exports `Character` as a string.
- [ ] batch-11 (client.api.classes.worldscanner.filter.**, 17) — pending
- [x] batch-11 (client.api.classes.worldscanner.filter.**, 17) — done: the package the plan called
  the worst-documented in the codebase (**31 methods, 1 documented, 0 examples**) is now **59
  documented members and 48 `example:` blocks**, with all 17 class docs present. Build green,
  **0/17** files with non-comment changes, **0 `&`/`<` bytes inside any example** across the whole
  docset, 48/48 `node --check`, `tsc` 0 errors with 48 files compiled (non-vacuous), and the shipped
  `.d.ts` byte-identical. 3 validation rounds.

  **The structural fact that shaped the whole batch: none of these 17 classes is in the shipped
  `.d.ts`** (0 occurrences each) — `tsdoclet`'s whitelist covers only `client.api.helper.` and
  `client.api.classes.inventory.`, and `WorldScannerBuilder` is in neither, so nothing drags them in.
  They are `WorldScannerBuilder`'s **internal construction detail**: a script never names one. So
  **no `Java.type(...)` example is possible** (it types `unknown` and fails `tsc`), and every example
  goes on the `World.getWorldScanner()` route instead. Same situation as batch-10's `DyeColorHelper`,
  and the validator confirmed that handling was right.

  **The defect class again — and it is now a confirmed pattern across two batches.** All 6 blocking
  defects were confident, specific, plausible claims contradicted by the body one screen away, and
  **not one was visible to `gradlew`, `tsc` or `node --check`**: a mistyped operator said to be
  refused "when the filter is built" (it is refused at **test time** — the constructor never inspects
  it, while the string filters genuinely do resolve in theirs, which is exactly how the error was
  copied across); `>` described as tolerance-compared (only `==`/`!=` get `DoubleMath.fuzzyEquals`);
  a claim about a **document that does not exist** (`XorFilter`'s own class javadoc is bare at HEAD,
  so the sentence referred to itself); `remove(List)` said to remove "at most once" when it calls
  `List.removeAll` and removes **every** occurrence (the correct text of the adjacent
  `remove(IFilter)` copied onto a method with different semantics); an example comment contradicting
  its own `Chat.log`; and `notBlockFilter()` said to throw "when the scanner is built" when
  `composeFilters(null)` throws **inside the `not` call itself**, before `build()` is reached.

  **THE METHOD THAT FINALLY WORKED — add these to the audit.** Pairing `@return` with its own body
  (batch-10 rounds 1–4) does not find any of these, because none of them is about the method's own
  return. The inverted pass does, **provided two extra dimensions are added**:
  > 1. **Timing claims** — built vs constructed vs test time vs per-test vs once vs cached.
  > 2. **Cardinality claims** — each / all / every / only / never / exactly N / at most once.
  > 3. **Every sentence naming another class's documentation** — confirm that document exists and
  >    says that. (Blocker 3 was a fabricated discrepancy.)
  With those, the writer found 5 more errors in its own prose in one pass, and the validator found
  one more it had missed. **Do not re-open the fixed ones.**

  **A real runtime defect found in 5 examples, invisible to every gate:** `.is(15)` throws
  `ClassCastException` at build time, because `ClassWrapperFilter` does
  `new NumberCompareFilter((String) args[0], args[1])` — a numeric method needs an **operator and a
  number**. All five fixed to `.is("==", 15)` (the 14 surviving single-arg `.is(x)` calls are on
  boolean methods, which is correct).

  **A WRITER CLAIM DISPROVEN BY THE VALIDATOR — do not record it as a quirk.** The writer reported
  that a `{@code}` body **starting with `>` loses that `>`** (`{@code >=}` shipping as `=`), proved it
  with a probe, and switched to `{@code ">="}`. The validator ran the real compiled pydoclet
  (`buildSrc/build/libs/buildSrc.jar`) on **JDK 21, 25 and 26** and found `>` **preserved on all
  three** — the quirk does not exist. The quoting was left in place (harmless) and no file describes
  the loss. **What is real, and different: HTML entities in javadoc are mangled by the doclets** —
  prose `a &gt;= b` → `a = b`; example `1 &gt;= 2` → `1 = 2`; `1 &amp;&amp; 2` → `1 2` (the whole
  entity lost); `{@code &gt;=}` is not decoded at all; **raw `>=` / `&&` survive fine.** The rule is
  *"never HTML-escape inside javadoc; write the raw character"*, and it hits `&amp;` harder than
  `&gt;`. The 17 files have 0 entities.

  **Dead code documented, not fixed (code changes — routed to a maintainer).** All confirmed by the
  validator: `XorFilter` and the whole xor path are unreachable (no `xor*` builder method, so
  `Operation.XOR` is never set and `case XOR:` at `WorldScannerBuilder.java:112` is dead); `GroupFilter`
  and all four nested classes have **zero** construction sites repo-wide; `CharCompareFilter` is
  unreachable (neither helper declares a `char`-returning method); all six `compare*` methods throw a
  message listing `"=>"` while no switch anywhere has a `case "=>"` (they have `case ">="`), so the
  error tells the user to type an operator the code rejects; `notStateFilter()`/`notBlockFilter()`
  throw unless that category already has a filter; and `WorldScannerBuilder.java:273` instantiates
  `new StringifyFilter<BlockStateFilter>` where the field is `IAdvancedFilter<BlockStateHelper>`
  (the block branch gets it right) — compiling only via an unchecked cast. Also noted: inherited
  helper methods are unreachable because `getPublicNoParameterMethods` uses `getDeclaredMethods()`,
  so `BaseHelper.getRaw()` is invisible.

  **A pre-existing doc defect in an out-of-scope file, recorded not fixed:** the shipped
  `FWorld.getWorldScanner()` and `WorldScannerBuilder` examples use `.contains("chest", "barrel")`
  and `.endsWith("ore")`, which **do not type-check** — `Contains`/`EndsWith` collapse to the whole
  `toString` template. Those files are batch-12's; the writer correctly used only `.is(` and
  `.matches(` here and did not edit them.

  **Known docgen defects extended here:** **no doclet handles `@throws` at all** (0 exception names
  across all 22 pages *and* all `.py` stubs) — so this extends to `webdoclet`, not just `pydoclet`;
  17 source `<pre>` blocks emit only 47 `example:` blocks, because one pre-existing
  `StringCompareFilter` example renders without the marker; and the 17 pages carry 20 degenerate
  `href="#"` links from the doclet's own generic `@return` type links (1236 site-wide).

- [ ] batch-12 (worldscanner + client.api.classes core, 7) — pending
- [x] batch-12 (worldscanner + client.api.classes core, 7) — done: `WorldScannerBuilder` **0 → 25**
  documented, `FakeServerCommandSource` 0 → 13, `InteractionProxy` 0 → 1, plus the `CustomImage`
  field and 4 small gaps; **169 `example:` blocks** (the package had zero). Build green, **0/7**
  non-comment changes, 0 `&`/`<` inside any example, `tsc` clean over the examples. 3 rounds.
  `InteractionProxy`'s `toString()` left bare.

  **The first pass was a REGRESSION and had to be repaired — the most important thing in this
  entry.** The validator found **6 blocking defects, 3 of them mechanical corruption of the work
  itself**:
  - **11 lines of correct pre-existing documentation deleted** from `InteractionProxy`, leaving
    *"Can be one of the following reason:"* with **nothing following** and a dangling "for example"
    with no antecedent — while the `@DocletDeclareType` still emitted all 11 reason literals, so
    readers saw 11 opaque strings. **Restored verbatim from HEAD.** This is the "a tag may be
    replaced, never silently dropped" rule generalised to *prose*, and it is now a standing check.
  - **27 sentences truncated mid-clause** (0 at HEAD), e.g. `* Without this the` followed by empty
    `* ` lines — 11 in `FakeServerCommandSource`, 16 in `RegistryHelper`, leaving 121 stray
    whitespace-only lines. All 27 completed from the bodies.
  - **83 description paragraphs misfiled *after* their block tags** (0 at HEAD), so in the shipped
    web doc the **version badge carried the whole paragraph and the example** with the real
    one-line summary stranded after them. 74 blocks reordered to the canonical
    **description → `example:` → block tags**; the shipped HTML was checked afterwards.

  **The 3 factual defects were the usual class:** `WorldScanner`'s map keys were described wrongly on
  **both** branches — `ignoreState=true` gives `Block{minecraft:stone}` (not `minecraft:stone`, it is
  `Block.toString()`'s recipe) and `ignoreState=false` was claimed to be "the id with its property
  list" when the fix-writer had to check `StateHolder`; `CustomImage.IMAGES` keys documented as
  `jsmimage/overlay` when `ResourceLocation.parse` puts them in the **default namespace**, so the
  example could never match (and its `if (found !== undefined)` guard did not narrow, because the
  typings declare `Map.get` as returning `V | null` — **2 real `TS18047` errors**); and `TextBuilder`
  documented as finishing a nested builder when `BaseHelper` stores the reference, so it is **live**.

  **A writer claim about the tsc baseline was an ARTIFACT and I nearly accepted it.** The fix-writer
  reported "58 `Cannot find name 'Java'` errors" and could not reproduce the 532 baseline. I ran the
  check myself: `Graal.d.ts` alone makes every `Java.type(...)` return `unknown` and leaves `Chat`
  undeclared, so **58 of those were the missing-headers trap, not real defects.** With the generated
  `.d.ts` **combined with** `docs/typescript/headers/*.d.ts`, all 56 `Java.type` examples produce
  **2** errors — both the `TS18047` above, now fixed. The correct invocation compiles **233 files**;
  a headers-less run reports tens of thousands of errors and is meaningless. **This is now gate 6
  in docs-plan.md, and the "532" figure only means anything with the full header set.**

  **The one error left is a typings gap, not a bad example:** `FakeServerCommandSource`'s
  `customSuggestion` example gets `TS7006` because the class is effectively **absent from the shipped
  `.d.ts`** (2 mentions, no class block), so `getSource()` is `any` and the callback is untyped.
  Left as-is deliberately — annotating would trade it for `TS2304` plus another parameter
  annotation.

  **2 pre-existing broken examples fixed (this is BATCH-12's own file, as batch-11 flagged).**
  `WorldScannerBuilder`'s `.contains("stone")` does **not** type-check: `Contains<BlockToString, S>`
  collapses to the whole `` `BlockHelper:{"id": "${string}"}` `` template, so the argument is not a
  substring search at all. Replaced with `matches(".*_ore.*")`, verified by reproducing the exact
  `TS2345` first. The same forms in `FWorld.java` are batch-09 and **committed — left untouched**;
  this needs a follow-up on a branch that owns that file.

  **Pre-existing false claims corrected:** `toString` was documented as giving `{minecraft:stone}`
  (it is `BlockHelper:{"id": "minecraft:stone"}`); `getChunkRange`'s `@return` said "a list of all
  matching block positions" (it is a list of `ChunkPos`); `getClipBounds` said "an array" (a
  `Rectangle`); and a new line the writer wrote itself — "0 to 10 is eleven blocks wide" — was
  **ten**, because the exclusive overload passes `x2-1` into an inclusive loop. That last one is the
  method both earlier batches used to trust: **run your own arithmetic, do not assume your prose
  matches your probe.**

  **Source defects found (documented, not fixed).** `CustomImage.drawImage(9-arg)` passes `image`
  where `img` is meant, so it draws the wrong image; `CustomImage.saveImage` calls `mkdirs()` on a
  path ending `.png`; `CustomImage.createWidget(String,String)` NPEs because `ImageIO.read` returns
  `null` silently, so the `@Nullable` only covers the `IOException`; `WorldScannerBuilder.java:273`
  instantiates `StringifyFilter<BlockStateFilter>` against a `BlockStateHelper` field (compiles only
  via an unchecked cast, while the block branch is right); the dead `case XOR:`; and the `char`
  branch of `ClassWrapperFilter.getFilter`, unreachable because no helper declares a `char`-returning
  method. Four `@see` on `CustomImage.loadImage` were removed — they were leaking raw into the
  shipped `.d.ts` — with the information carried into prose and examples.

  **A proven docgen win, worth keeping:** the six-operator list in `WorldScannerBuilder` used a
  pre-existing `&lt;` that pydoclet **deletes**, so Python readers saw only four operators and
  `&lt;=` rendered as `' ='` — actively misleading. `{@code "<"}` makes the bare `<` survive and all
  six now render.

- [ ] batch-13 (client.api.classes.render + components3d, 12) — pending
- [x] batch-13 (client.api.classes.render + components3d, 12) — done: **342 `example:` blocks** (the
  package had zero), ~63 method gaps and 28 public fields closed, 178 javadoc blocks reordered to the
  canonical **description → `example:` → block tags**, and `Draw3D`'s class doc replaced (it was the
  single line `{@link Draw2D} is cool`). Build green, **0/12** non-comment changes, **0 errors across
  339 examples** under `tsc --strict` with the correct combined header set, 0 `&`/`<` in any example.
  2 rounds. The writer **could not spawn an independent validator** (subagent depth limit), which is
  why this batch got one — worth knowing for later batches.

  **The first pass shipped 17 unusable examples and a regression; both fixed in round 2.** The 9
  `IScreen` widget-builder examples called `buildAndAdd()`, which exists **only on the 6 Draw3D
  element builders** — the screen builders terminate on `build()`. 7 examples called
  `player.getInventory()`, which **exists nowhere** (only `getInventorySize()` on a horse helper), and
  1 called `button.getScreen()`, likewise absent. The round-2 writer read all 7 builder classes and
  replaced them with real methods (`.message`/`.action`/`.initially`/`.enabledTexture`/…), and while
  doing so discovered `AbstractWidgetBuilder.build()` *does* call `screen.reAddElement` — so the
  original reasoning was right and only the name was wrong.

  **A `@see` RE-POINTING that made things worse — now a standing ruling: report and leave, never
  re-point.** The first pass "fixed" 5 stale `@see` on `Draw2D.addItem` and pointed **all 12** at the
  simplest 3-arg overload, so six `ItemStackHelper` methods linked to a `String` overload — and
  amplified the known raw-`@see`-leaks-into-the-`.d.ts` defect from 12 correct values to 12 identical
  wrong ones. At HEAD there were only **6** and they were already correct and distinct. **Reverted to
  the HEAD values**; the same pass had added 16 more such tags on `addText`/`addImage`/`addRect`/
  `removeLine`, which were also reverted. Rule now in the writer brief: `@see` is a bad lever (the
  web doclet ignores it, `tsdoclet` emits it raw), so touching it buys nothing and risks exactly this.

  **`Surface`'s subdivision default was a one-number-two-truths split — the signature defect class.**
  `Surface.java:68` claimed "the default subdivision count of `200`", but `Surface.Builder` initialises
  `minSubdivisions = 1`; 200 is only what `Draw3D.addDraw2D`'s short forms pass literally. It also
  contradicted the writer's **own** text 1500 lines later ("The default here is `1`") and its own
  example ("the default is a single pixel"). Fixed, then swept all 30 field initialisers across the
  five `components3d` builders and all 74 "the default is" sentences — every other one verified
  correct, including two genuine builder-vs-instance inversions (`rotateCenter`, `renderBack`) that
  were left and are properly documented.

  **A false `@param` was "corrected" by apologising for it, which is worse.** `IScreen`'s
  `setOnScroll` said the second argument is a `Double`; the code passes a `Pos2D`. The first pass
  left the tag byte-identical to HEAD and added prose *asserting the tag was wrong* — which also
  called the tag "above" when it is below, and left the false `Double` shipping into the `.d.ts`. The
  round-2 writer fixed the tag itself and deleted the apology. The underlying finding was sound
  (`MixinScreen.java:1059` passes `new Pos2D(horiz, vert)`), and the shipped header now reads
  `BiConsumer<Pos2D, Pos2D>`.

  **THE `tsc` "532" BASELINE IS NOISE — corrected in docs-plan.md, and this cost real time.** Across
  rounds subagents reported the "same" header baseline as **528 / 532 / 549 / 153 / 10427**, because
  the number depends entirely on which files are handed to `tsc`. I could not reproduce 532 either
  (my file set gives 153). Two things are actually verifiable and both hold: the **project's own
  tsconfig** (`tsc -p build/docs/typescript/tsconfig.json --noEmit`) gives **0 errors**, and the
  **examples** type-check at **0** against the correct combined header set. The structural guarantee
  that no shipped member set changed is **zero non-comment lines**, not a `tsc` count — javadoc only
  reaches the `.d.ts` as comment text. Also note a `skipLibCheck: false` run *always* shows errors for
  any example touching a `JavaList`/`JavaMap`/`Packages` alias, because those are the dangling error
  types, so the examples' gate must be the `skipLibCheck: true` run.

  **A writer caught and corrected one of its own numbers, twice, which is the right instinct:**
  `addSlider` was first documented as "10 steps moves in tenths", then corrected to **ten positions in
  ninths** (`steps = (steps>1?steps:2)-1` with `roundValue(v)=round(v*steps)/steps`, so `steps<=1`
  becomes 2); and the unset button textures were first said to fall back to the enabled sprite, which
  is false because `ButtonWidgetHelper.java:229` uses the 4-arg canonical `WidgetSprites` record (checked
  against five MC source trees). It also found a stale `0.5`-as-waist-height claim duplicated at
  `Draw3D.java:726`.

  **The `cull` flag is genuinely the inverse of its name, in all three places, and that is now
  documented** (validator-confirmed): `Box` `seeThrough = !this.cull` (`:474`), `Line3D`
  `cull = !alwaysOnTop` (`:315`), `Surface` `(!cull) != alwaysOnTop` (`:736`, algebraically identical
  to `Draw3D.java:1435`, **not** a discrepancy).

  **Source defects found (documented, not fixed):** `Draw2D.addItem(int,int,int,…)` has an
  unconditional `return null`; `SurfaceRenderTypes.lines(boolean)` has **no caller** and its two culled
  `quads` variants are unreachable (`Rect.java:360` and `Line.java:356` both hard-code `cull=false`),
  so nothing is ever built at all; `Box.compareToSame`'s `instanceof Box` branch is always true;
  `Surface.Builder.pos(Pos3D)` hands the builder's **same** `Pos3D` to the surface it builds, so
  `surface.setPos(...)` moves the builder's position too; `Draw2D.init()` with no init function leaves
  nested overlays uninitialised (the recursive call is inside the `if (onInit != null)` branch); and
  `ScriptScreen.onClose()` with no parent traps the player (`openParent()` → `setScreen(null)`).

  **`SurfaceRenderTypes` correctly has 0 examples** — it has 0 occurrences in the shipped `.d.ts`
  (it is not referenced from any whitelisted signature), so a `Java.type(...)` example would type as
  `unknown`. The other 11 classes are all present and their examples work.

  **No TypeScript parameter annotations were added** in this batch (HEAD has 0 in all 12 files), so the
  repo-wide count of the 31 known ones is unchanged. That convention is a separate repo-wide
  decision, deliberately untouched.

- [ ] batch-14 (client.api.classes.render.components, 9) — pending
- [x] batch-14 (client.api.classes.render.components, 9) — done: **65 of 65 previously-undocumented
  public fields** documented (0 at HEAD) plus `RenderElement.mc`, all 38 method gaps closed, and
  **409 `example:` blocks** (the package had zero). Build green, **0/9** non-comment changes, **0
  `tsc` errors** across 417 examples with the correct combined header set, 0 `&`/`<` in any example.
  2 rounds.

  **THE MOST CONSEQUENTIAL DEFECT OF THE EFFORT SO FAR: a javadoc block anchored on the WRONG
  declaration.** `Line.Builder.getZIndex()`'s text — description, example *and* `@return` — was
  attached to `getScaledTop()`, which returns `Math.min(y1, y2)`. So **`getScaledTop()` shipped
  documented as returning a z-index**, in both `Line.py` and the shipped
  `JsMacrosCE-2.0.0.d.ts` (`getScaledTop(): number`), while the real `Builder.getZIndex()` sat bare
  and `Line.getZIndex()` had **no javadoc at all**. This is the misfile-after-block-tags corruption
  mode, and it is the one defect a reader is guaranteed to act on wrongly. **An anchor sweep across
  all 9 files found it was the only one** (531 blocks, 0 signal-B misfiles remaining). *Why the
  writer's own "0 misfiled" check missed it:* it only tested **adjacency** — was prose after a block
  tag — which cannot see a block that is well-formed but attached to the wrong member.

  **The fix-writer DROPPED a clause rather than relocating it, and that was right.** The misfiled
  block also contained *"and the built line folds the rotation down where this builder does not"* — a
  rotation fact in a z-index doc. It verified the statement already appears at **eight** sites
  including `Builder.rotation`, so relocating would be pure duplication. Recorded because "never
  delete prose" must not become "never remove a misplaced sentence": drop it, report it, and show
  the information already exists elsewhere.

  **A false claim the writer got right in three places and backwards in the two it added.** `Item`:
  *"turning this on with no text shows the count when the stack is more than one."* False for any
  **builder-made** icon, which is what both attached examples build — `Item.Builder.ovText`
  initialises to `""` (`:1136`), and both the 2D and 3D paths test `ovText != null`, so `""` wins and
  a **zero-length string is drawn and no count appears** (probed: builder default with count 5 draws
  nothing on both paths; the constructor default `null` draws `"5"`). The writer's own prose at
  `:116-120`, `:1364-1366` and `:1372-1375` states the distinction correctly. The round-2 sweep then
  found the **same false claim a third time** at `Builder.item(String,int)`, plus a `@param` and
  `shouldShowOverlay`. **Five sites fixed.** The contrast that now carries it: `Draw2D.addItem` routes
  through the 7-arg constructor, so *those* methods really do show the count.

  **B1, B3, B4 from round 1, all closed and independently verified:** `Alignable.parsePercentage`'s
  prose said a decimal or leading space gives `-1` while its own `@throws` said a non-whole-number
  throws (it does — probed `50.5%` and `" 50%"` both throw); the "flatten that depth" claim, which was
  false on 1.21.8 where **both `is3dRender` branches make the identical `drawContext.renderItem(item,
  x, y)` call** and the ≤1.21.5 `matrices.scale(…)` that did the flattening is dead (the flattening is
  real on the `PoseStack` variant, which is what was conflated); and a false exclusive enumeration
  that missed `Text` and `Item` overriding `render3D`.

  **The writer corrected MY brief twice, both times correctly.** I told it `Builder.scale()` throws on
  `scale <= 0` so two "rounds down" sites were fine — true for `Text`/`Item`, but `Draw2DElement`'s
  builder is unguarded, so it qualified those two as well. And I listed 4 "rounds down" sites when
  only 2 needed it, because the builder copies really do throw. It also **partly disagreed** rather
  than complying: `Draw2D.setScale` *does* refuse non-positive values, so only the builder path is
  reachable, and it said so.

  **BOTH ROLES SHIPPED VACUOUS CHECKERS IN THIS BATCH — now a standing rule in docs-plan.md.** The
  writer found its truncation/misfiling checker was "silently examining nothing" (its line filter
  rejected every line, so "0 across 524 blocks" was vacuous). The **validator** then reported that
  **three of its own checkers were vacuous** — including a word-set prose diff that reported "0
  deleted prose" for an injected sentence whose words all recur elsewhere, which is impossible for a
  set to detect — and **declined to certify its misfile checker at all**, falling back to a manual
  read. The round-2 fix-writer found three more bugs in its own instruments, including a comment-strip
  lexer whose block-comment opener test compared against `'"'` instead of `'/'`. The rule now recorded:
  **prove every checker by injecting a known defect into a copy and requiring both that it fires and
  that it reports 0 on pristine; and if a checker cannot be made reliable, say so rather than quoting a
  green number from it.** A checker that examines nothing says "0" in exactly the same words as one
  that finds nothing.

  **The project's own tsconfig is a WEAK check, not a gate — corrected in docs-plan.md.** It compiles
  **one zero-byte** `scripts/test.ts` plus the header under `skipLibCheck: true`, so it exercises not a
  single example. Useful as a smoke test; never quote it as evidence the examples are sound. The
  meaningful gate is the per-example run with the combined header set, **with a positive control and a
  stated compiled-file count**.

  **A live defect the byte-checker's positive control caught:** `pydoclet` silently deletes a bare
  `&`, so three of the writer's own examples shipped as `icon.getColor() 0x00FFFFFF` and
  `Chat.log(… 0x123456)` — valid-looking JavaScript, wrong code. All rewritten. This is the third
  time that checker has earned its keep.

  **Source defects found (documented, not fixed):** `Mth.wrapDegrees` is applied **inconsistently**
  across the six elements (`Rect` wraps in ctor and setter, `Text` in both, `Line` ctor only,
  `Item`/`Image` setter only, `Draw2DElement` never); `Draw2DElement`'s constructor binds
  `draw2D.widthSupplier` to the builder's suppliers **by value**, so a later `width`/`size` call
  resizes the element but leaves the nested overlay reporting the outer overlay's size; `Line`'s stroke
  band is `(int)-halfWidth`…`(int)halfWidth`, so a thickness of 1 gives `-0.5`→0 and `0.5`→0;
  `Alignable.split("on")` with no `on` throws `ArrayIndexOutOfBoundsException`, and `parsePercentage` is
  handed the whole string, so **every** percentage in the two-element form throws; and
  `Line.Builder.moveTo` rebuilds from `Math.abs` extents, so magnitudes survive but **signs do not**
  and a right-to-left line comes out mirrored.

- [x] batch-15 (client.api.classes.inventory, 10) — done: **55 script-visible method gaps closed**,
  `CommandManager`'s undocumented public field documented, and **56 `example:` blocks** added (the
  package had zero). Build green, **0/10** non-comment changes, 0 `tsc` errors, 0 `&`/`<` in any
  example, **0 documented `toString`** (8 exist in these files and must stay bare). 2 rounds.
  *(My brief said "11 files" but enumerated 10; the writer flagged the discrepancy rather than
  inventing an eleventh.)*

  **THE SECOND SILENT VOID, and the same root cause as batch-14's worst defect: ANCHORING, NOT
  ADJACENCY.** The writer put 12 javadoc blocks **between** `@Override` and the declaration
  (`CraftingInventory` ×4, `FurnaceInventory` ×4, `PlayerInventory` ×4). Measured: **268** methods
  repo-wide use the normal `/** */`→`@Override` order and exactly **12** used the anomalous one — all
  12 from this batch. The effect is that the method ships with an **empty docstring** and the prose
  appears in **zero** of the three output trees (`.py`, `.html` **and** `.d.ts`), while the normally
  anchored control `FiltererModulus.test` ships fine. **~200 lines of prose and 6 of the batch's 56
  examples were void** — the largest silent loss in the effort. The writer's own anchor sweep reported
  "8 findings, 0 genuine"; the validator's found **13, all 13 genuine, all 13 created by this batch**.
  **Lesson: an adjacency check — "is prose after a block tag?" — structurally cannot see either of
  this effort's two worst defects. You need an anchor check: does the member named in the first
  sentence, the example, and `@return` match the declaration immediately below?**

  **The writer's stated cause for the above was wrong, and worth recording:** it concluded *"`pydoclet`
  drops `@Override` docstrings, so 6 examples are `.html`/`.d.ts`-only."* **All three** doclets drop
  them — they reach **none** of the outputs. That is a pre-existing docgen behaviour, not something
  javadoc can work around, and misreading it is what hid the placement bug.

  **B2 was the batch's own central mechanic, documented three different wrong ways.** `or()` **pops
  two and pushes one merged node**, so calling it after a *multi-level* branch leaves the next argument
  **under that branch's innermost node** (`/count/set/amount/show`, not `/count/set/show`). The class
  doc, the `or()` example and the `or(2)` example each asserted a flatter shape, and `or()`'s own prose
  (*"build a branch to its end, call this, then build the next one"*) is precisely the sequence that
  produces the shape its own example comment **denied** — the file was self-contradictory about its
  central mechanic. All three re-derived and fixed, the `or()` example now really produces
  `/count show` and the `or(2)` example correctly says the next literal lands *under* `set`.

  **Three more single wrong facts, all the signature class:** `CommandManager` was described as
  *"owning two things"* when `CommandManagerFabric`/`Forge` **have no fields at all** (the command list
  is read live from the connection and the builder map is `private static` on `CommandBuilder*`);
  `PlayerInventory` said `getInputSize()` "works" when it is `getCraftingHeight() * getCraftingWidth()`
  = `0 * 0` = `0`, **contradicting the writer's own correct text 30 lines later**; and `CommandBuilder`
  claimed the slot-name set was "not fully enumerable from the class file" when
  `javap -c` prints `SlotRanges.method_58084` **in full**.

  **Writer self-caught, and the audit did its job:** its 4-part audit found **7 errors in its own new
  prose** (including an *inverted* `literalArg`/`suggests` claim and a class doc saying a builder "can
  be registered again" when `register()` pops the stack — probe: second call throws
  `EmptyStackException`). It also filled 4 pre-existing **empty** `@param` tags in `Inventory`,
  repaired a pre-existing misfiled block in `unregister()` that sat after `@since`, and removed its own
  **`* NOTE for the validator:` scaffolding that was shipping to end users in 6 generated artifacts** —
  which the validator caught, the writer had not noticed.

  **A tooling failure that passed two gates.** The writer's helper script wrote example `if`/`}` lines
  with **no leading `*`** — 14 malformed javadoc lines — and **the build passed and the non-comment
  gate passed**, because the text is still lexically inside the comment; only the doclets' per-line
  `*`-stripping was affected. It wrote a checker to find it (proven on an injection), fixed it, then
  found its repair had flattened nested indentation and rebuilt the blocks from brace depth. The
  validator confirmed **0** malformed lines remain across 1692 inner-comment lines.

  **Source defects found (documented, not fixed):** **`CommandBuilder.or()` is loader-dependent** —
  Fabric no-ops at the head (`if (size>1){…}`, no `else`) while NeoForge throws
  `AssertionError("Can't use or() on the head of the command")`; documented honestly rather than
  picking one. `itemSlotArg` parses a **named slot range that must cover exactly one slot**
  (`ERROR_UNKNOWN_SLOT` / `ERROR_ONLY_SINGLE_SLOT_ALLOWED`), and the accepted names are prefixes —
  bare `armor`/`player`/`hotbar` do not parse but `hotbar.*` does.

- [x] batch-16 (client.api.classes.inventory, remaining 11) — done: **83 `example:` blocks** (the
  package had zero) plus a class-level slot table for every class. **0 documentable method gaps** — the
  22 undocumented members are 11 constructors and 10 `toString` overrides, both categories that must
  stay bare. Build green, **0/11** non-comment changes, 0 `&`/`<` in any example, 0 documented
  `toString`, 0 T2 anchoring problems across 82 blocks. 2 rounds.

  **THE BATCH'S HEADLINE: 38 OF THE WRITER'S OWN 83 EXAMPLES SHIPPED BROKEN, and its primary gate
  was "passing vacuously".** `pydoclet` deletes every `&&` and every `<`:
  `if (inv.is("Anvil") && inv.getLeftInput()…)` shipped as `if (inv.is("Anvil") inv.getLeftInput()…)`.
  The count-based gate (`&`=0, `<`=0 in the `.py`) **passed** — because after deletion there is
  nothing left to count. What caught it was a **source↔`.py` token diff** (0 of 83 examples lose a
  token, after the fix; 38 before). This is the single most important methodological finding of the
  batch: **counting the bytes in the output is not enough — you must compare the output against the
  source**, because the failure mode is a *deletion*.

  **A duplicated `example:` block was the sole discrepancy in that token diff and it was not surfaced.**
  Two byte-identical examples sat back to back in one `BeaconInventory` javadoc, shipping to all three
  trees and concatenating onto one line in the `.py` (invalid JavaScript). The diff compared **83 java
  blocks against 82 `.py` blocks** — the cheapest possible red flag. **So a source↔output comparison
  must assert the counts are EQUAL before it checks token loss.** Now a standing rule.

  **A claim built on half the evidence, in five places, and the fix was found in the decompiled source
  rather than the bytecode.** `BEACON_EFFECTS` has **FOUR** tiers (`List.of(Object,Object,Object,
  Object)`: SPEED/HASTE, RESISTANCE/JUMP_BOOST, STRENGTH, **REGENERATION**). The writer's evidence was
  the `i <= 2` loop in `BeaconScreen.init()` — but there is a **second** loop there that reads
  `BEACON_EFFECTS.get(3)`. Four shipped claims were wrong, the worst being *"the regeneration special
  case cannot be reached, because regeneration is not one of the effects a beacon offers"* — it is,
  and the branch is reachable on any level-≥4 beacon. The `IndexOutOfBoundsException` is real but only
  reachable at **level 5**, which vanilla never produces (`MAX_LEVELS = 4`). The fix-writer found a
  **fifth** site from the same re-derivation. **Lesson: one loop is not a bound on the data it loops
  over — read the whole initialiser.**

  **8 pre-existing false claims corrected**, the sharpest being **`SmithingInventory.getOutput()`**:
  documented as "the expected output item" when slot 2 is `SmithingMenu.ADDITIONAL_SLOT` and the
  smithed result is **slot 3** — so the method name *and* the slot both lie. The validator confirmed it
  twice, from `createInputSlotDefinitions` and independently from `getMap()["output"]` = `{3}`. Also:
  grindstone max is `2*simulateXp() − 1` (not ×2, because `getExperienceAmount` returns
  `i + random.nextInt(i)` with `i = ceil(xp/2)`); anvil repair cost is the `minecraft:repair_cost`
  component, not a quantity; `getMerchantRewardedExperience()` is `activeOffer.getXp()` (the selected
  trade's reward), not a running total; the cartography material slot accepts **paper, map or glass
  pane**; a horse's `getInventorySize()` is **15 or 0**, never 2 or 3.

  **Every slot index was re-derived from bytecode, and the validator checked them by a second,
  independent path** — `Inventory.getMapInternal()`, which computes each section from
  `getTotalSlots()`. Both agree on all 11, including `LoomMenu` banner 0 / dye 1 / **pattern 2** /
  result 3 and `SmithingMenu` template 0 / base 1 / additional 2 / result 3.

  **TWO TOOLING CORRECTIONS THAT MATTER FOR THE REST OF THE EFFORT.**
  - **A `node --check` rule in `docs-plan.md` was wrong, and I had written it from an unverified
    claim.** It said `node --check` *passes* deleted-`&&` damage. The batch-16 validator tested 9 shapes
    and it **rejects every one** — `pydoclet` leaves the surrounding whitespace, so the operands stay
    two separate tokens. `node --check` **is** a real gate here. **Corrected in docs-plan.md** (quirk
    17), which had also been contradicted in-session by a writer that could not reproduce it.
  - **`tsc` was reported as giving "near-zero coverage of member names" on these classes** (because
    `inv.is("Beacon")` was thought not to narrow). The fix-writer injected `inv.__nope__()` into
    **all 82** guarded examples and `tsc` caught **82/82**: `Inventory.is()` is declared as a **type
    predicate** (`is<T extends ScreenName>(…): this is T extends keyof InvNameToTypeMap ? …`), so it
    does narrow. **`tsc` is a real member-existence gate** — though it still cannot see a class absent
    from the header tree, so cross-checking symbols against source remains necessary.
  - Also worth carrying: that writer's **first `tsc` run was silently vacuous** — invoked from the
    wrong cwd it printed `TS5058: path does not exist` and the error-parser reported "0 errors". Its
    harness now hard-fails on `TS5058`/`TS6046` before counting. **A type-check that never ran reports
    0 errors, in the same words as one that passed.**

  **Source defects found (documented, not fixed):** `EnchantInventory`'s three option readers call
  `orElseThrow()`, so they throw `NoSuchElementException` whenever the table offers fewer than three
  enchantments (an empty slot leaves the clue at −1 and `MappedRegistry.get(int)` returns empty for a
  negative id) — and `:109`'s `if ((enchantment) != null)` is dead code; `LoomInventory`'s guard is
  `index <= patterns.size()` where `clickMenuButton` needs `< size`; `RecipeInventory` declares
  `throws InterruptedException` and `@Nullable` where nothing can throw or return null;
  `VillagerInventory.getMerchantRewardedExperience()` is misnamed.

- [ ] batch-17 (client.api.helper.world + helper.inventory, 20) — pending
- [ ] batch-16 (client.api.classes.inventory, 11) — pending
- [ ] batch-17 (client.api.helper.world + helper.inventory, 20) — pending
- [ ] batch-18 (client.api.helper.world.entity + helper.screen, 21) — pending
- [ ] batch-19 (entity.specialized boss/decoration/display/other/projectile/vehicle, 21) — pending
- [ ] batch-20 (entity.specialized.mob, 21) — pending
- [ ] batch-21 (entity.specialized.passive, 30) — pending

## In-game test queue (examples that could not be fully verified)

Batch-10 (all runtime behaviour; everything static is bytecode-verified):

1. `PacketByteBufferHelper` — that `toPacket()` returns `null` for a fresh `Client.createPacketByteBuffer()`
   and throws `NullPointerException` for an event-supplied one; and that `PACKET_IDS`/`PACKET_STATES`/
   `PACKET_SIDES` really do stay empty through the live mixin/init path (bytecode-verified, but that
   is a runtime invariant). Confirms the corrected 200-entry census at runtime.
2. `FormattingHelper` — that `getColorValue()` throws for all 5 modifiers and `RESET`, and that
   `getCode()` arrives as a one-character **string** so `"§" + code` renders a real code.
3. `DyeColorHelper` — the three shade values against real dyed items, signs and firework stars; and
   in particular that `getColorValue().toString(16)` prints a leading `ff`, which is the single most
   consequential correction in the batch.
4. `StyleHelper.getClickAction()` — that a **server-sent** `clickEvent: {action: "custom"}` yields
   `CUSTOM` (uppercase), distinct from the hard-coded lowercase `"custom"` for a JsMacros-registered
   click.
5. `SuggestionsBuilderHelper.suggestPositions("0 64")` — bytecode says `split[2]` is read
   unconditionally, so this must throw `ArrayIndexOutOfBoundsException`. One call confirms it.
6. `StatsHelper.getRawStatMap()` / `getFormattedStatMap()` — the per-category collapse, and the
   `stat_type.minecraft.*` key shape, against a live server that has sent statistics.
7. `NbtPredicateHelper` / `StatePredicateHelper` / `BlockPredicateHelper` examples — all key off
   `elytra.getDestroyRestrictions()` and need a real elytra plus live block-entity state.
8. Everything gated on being in a world: all examples using `Player.getPlayer()`,
   `World.isWorldLoaded()`/`getBlock`/`getEntities`, `Client.getGameOptions()`, item NBT, and the
   `InteractionManagerHelper` block-breaking examples. Also `JavaWrapper.methodToJava` callback
   threading for `breakBlockAsync`.
