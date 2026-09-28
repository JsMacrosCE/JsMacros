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
