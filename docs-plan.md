# JsMacrosCE — Script-Exposed Java API Javadoc Plan

**Deliverable:** improved javadoc comments (plus doclet annotations) on the script-exposed Java
API. Web docs / TypeScript `.d.ts` / Python stubs are generated from these by the custom doclets
in `buildSrc` (`pydoclet`, `tsdoclet`, `webdoclet`) via the `generatePyDoc` / `generateTSDoc` /
`generateWebDoc` Gradle tasks. **No parallel markdown docs.**

## Doc source set

`build.gradle.kts` (and `stonecutter.gradle.kts`) define:

```kotlin
val documentationSources = files(mainSourceSets.map { it.allJava })   // :common + :extension main
```

i.e. **all** of `common/src/main/java` (534 files) and `extension/*/src/main/java` (10 files) are
fed to the doclets. The doclets render **every** type in that set; classes are grouped into the web
doc by annotation: `@Library("…")` → group *Library*, `@Event("…")` → group *Event*, everything
else → group *Class*.

Script-exposed surface for this effort (everything reachable from a script's globals / event
payloads), which is what the batches below cover:

- `com.jsmacrosce.jsmacros.api.*` — `PlayerInput`, `api.library.F*` (`@Library`), `api.helper.*`, `api.math.*`
- `com.jsmacrosce.jsmacros.core.library.*` / `core.library.impl.*` — `@Library` entry points
- `com.jsmacrosce.jsmacros.core.event.*` / `core.event.impl.*` — event/listener/filterer infrastructure
- `com.jsmacrosce.jsmacros.core.service.*` — `Service` `@Library`
- `com.jsmacrosce.jsmacros.client.api.library.impl.*` — `Chat`/`Client`/`Hud`/`KeyBind`/`Player`/`PositionCommon`/`World`
- `com.jsmacrosce.jsmacros.client.api.event.impl.**` — all 51 script events
- `com.jsmacrosce.jsmacros.client.api.classes.**` — inventory / render / worldscanner / filters
- `com.jsmacrosce.jsmacros.client.api.helper.**` — `*Helper` payload wrappers

Out of scope (internal implementation, never exposed to scripts): `client/mixin/**`,
`client/access/**`, `access/**`, `client/gui/**`, `core/config`, `core/extensions`,
`core/language`, `client/config`, `client/util`, `core/threads`, `util/**`, `extension/graal/**`
(10 files, internal Graal plumbing).

### The active build target is **26.1.2**, not 1.21.8 (verified in batch-17)

`stonecutter.gradle.kts:141` reads `val mcVersionsToBuild = if (IS_CI) supportedVersions else
listOf("26.1.2")`. The `stonecutter.active` file says `1.21.8`, but that is the *backport* marker —
the code that is actually compiled, doclet-processed and shipped is 26.1.2. Confirmed three
independent ways: `mcVersionsToBuild`; the presence of `getCapeUrl` in the generated
`JsMacrosCE-2.0.0.d.ts` (a `//? if >1.21.8` branch); and `version=26.1.2` in the mapping-viewer URLs
the web doclet emits into every page.

**This is the verification baseline for every batch, and getting it wrong is not a small error.**
Two of batch-17's five blocking findings were false precisely because they were derived against
**1.21-era conventions** — `level` running `0..7` rather than `1..8`. Read behaviour from the
**generated/active** sources or the **compiled class**, never from memory of an older version.

A Stonecutter-free decompiled sources jar for vanilla 26.1.2 is available for vanilla classes:
`neoforge/versions/26.1.2/build/moddev/artifacts/minecraft-patched-26.1.2.109-sources.jar`
(`unzip -p <jar> net/minecraft/…`). Prefer it over `//?`-gated source for anything behavioural, and
`javap -c` for anything numeric.

## Current state (audit baseline, `.hermes-audit/audit.py`)

Whole `com/jsmacrosce/jsmacros` tree: **495 classes, 3550 public methods, 2418 documented,
only 1 method carrying an `example:` block**, 446 public fields of which 39 documented,
313/495 classes with a class-level javadoc.

**The dominant gap is `example:` blocks — there is effectively one in the entire codebase**
(`FJsMacros.assertEvent`). The second gap is the event classes' public **fields** (the payload
members script users actually read) and several near-untouched packages
(`client/api/classes/worldscanner/filter/**`, `api/math`, `api`).

Coverage note legend used in the batch tables:

- `complete` — class + all public members documented, has examples
- `has-javadoc-but-no-example` — documented, no `example:` block
- `partial` — some members documented, gaps remain
- `undocumented` — no usable class-level javadoc

## Cross-branch constraint

`origin/backports/1.21.8-vitepress` carries a future markdown doclet that consumes the same
javadoc. Three files already have doc work on that branch and must stay consistent with it
(edits there must be **minimal and additive**):

- `common/.../client/api/event/impl/EventRecvMessage.java`
- `common/.../client/api/event/impl/world/EventPlayerJoin.java`
- `common/.../client/api/event/impl/world/EventPlayerLeave.java`

That branch uses a `@DocletCategory` annotation that **does not exist on this branch** — do not
introduce it. Reuse its *prose* for the fields it documented, nothing more. See batch-04.

### Verified 2026-09-28 (before batch-09 / batch-22): the overlap is much wider than 3 files, but carries no prose

`git diff --name-only HEAD origin/backports/1.21.8-vitepress` lists 417 files, and the branch's heavy
refactor does touch **all 7** `client.api.library.impl` classes (batch-09) and 2 of batch-22's 4
classes. **None of them constrains us**, because the branch adds no javadoc there. Comparing the
*comment lines only* between HEAD and the branch:

| file | comment-line deltas | verdict |
|---|---|---|
| FChat | 1 | branch merely **dropped** a `@throws JsonParseException` (refactor changed the exception) |
| FHud | 2 | a `@param` **rename only** (`showBackground` on HEAD vs `dirtBG` on the branch) |
| FWorld | 35 | the refactor **removed methods**, taking their javadoc with them — HEAD is a superset |
| FClient, FKeyBind, FPlayer, FPositionCommon, BaseEvent, BaseEventRegistry, IEventListener, EventListener | 0 | byte-identical comments |

So the rule for every remaining batch is: **the branch's *code* churn is irrelevant, only its *new or
rewritten prose* matters.** Before writing a batch, run the comment-only diff above for its files. If
the delta is 0, the batch is unconstrained and can be written normally. If it is non-zero, read only
the changed comment lines and reuse prose that is genuinely *new*.

---

## Batches

### batch-01 — `client.api.event.impl.world` (14 classes)
Highest-value gap: world events, **0/16 methods and 2/34 fields documented**.
`EventBlockUpdate`, `EventBossbar`, `EventChunkLoad`, `EventChunkUnload`, `EventDimensionChange`,
`EventDisconnect`, `EventEntityDamaged`, `EventEntityHealed`, `EventEntityLoad`,
`EventEntityUnload`, `EventJoinServer`, `EventNameChange`, `EventSound`, `EventTick`.
(`EventPlayerJoin` / `EventPlayerLeave` deliberately deferred to batch-04.)

| class | methods doc | fields doc | class doc | example |
|---|---|---|---|---|
| EventBlockUpdate | 0/1 | 0/2 | yes | no |
| EventBossbar | 0/1 | 0/3 | yes | no |
| EventChunkLoad | 0/1 | 0/3 | yes | no |
| EventChunkUnload | 0/1 | 0/2 | yes | no |
| EventDimensionChange | 0/1 | 0/1 | yes | no |
| EventDisconnect | 0/1 | 1/1 | yes | no |
| EventEntityDamaged | 0/1 | 1/3 | **no** | no |
| EventEntityHealed | 0/1 | 0/3 | yes | no |
| EventEntityLoad | 0/1 | 0/1 | **no** | no |
| EventEntityUnload | 0/1 | 0/2 | **no** | no |
| EventJoinServer | 0/1 | 0/2 | yes | no |
| EventNameChange | 0/1 | 0/3 | yes | no |
| EventSound | 0/1 | 0/4 | yes | no |
| EventTick | 0/1 | 0/0 | yes | no |

### batch-02 — `client.api.event.impl.player` (18 classes)
1/19 methods, 5/48 fields documented; 4 classes have no class-level javadoc.
`EventAirChange`, `EventArmorChange`, `EventAttackBlock`\*, `EventAttackEntity`\*,
`EventDamage`, `EventDeath`, `EventEXPChange`, `EventFallFlying`\*, `EventHeal`,
`EventHealthChange`, `EventHeldItemChange`, `EventHungerChange`, `EventInteractBlock`,
`EventInteractEntity`\*, `EventOpenScreen`, `EventRiding`, `EventSignEdit`,
`EventStatusEffectUpdate` — \* = no class javadoc.

### batch-03 — `client.api.event.impl.inventory` (7) + `client.api.event.filterer` (3)
4/26 methods, 3/23 fields documented. `EventClickSlot`, `EventContainerUpdate`\*,
`EventDropSlot`, `EventItemDamage`, `EventItemPickup`, `EventOpenContainer`, `EventSlotUpdate`;
`FiltererBlockUpdate` (1/10 m), `FiltererRecvPacket` (0/3), `FiltererSendPacket` (0/3).
\* = no class javadoc.

### batch-04 — vitepress-overlap set + `core.event.impl` remainder (7 classes)
**Minimal additive edits only**, prose kept consistent with
`git show origin/backports/1.21.8-vitepress:<path>`. Do not add `@DocletCategory`.
`EventRecvMessage`, `EventPlayerJoin`, `EventPlayerLeave`, `EventWrappedScript` (0/6 m, 0/3 f),
`EventProfileLoad` (0/1 m, 0/1 f), `FiltererInverted` (0/4 m), `FiltererModulus` (0/3 m).

### batch-05 — `client.api.event.impl` top-level (9) + `CommandContextHelper` (1)
`EventKey` (2/4 m), `EventLaunchGame`, `EventMouseScroll`, `EventQuitGame`, `EventRecvPacket`,
`EventResourcePackLoaded`, `EventSendMessage`, `EventSendPacket`, `EventTitle`,
`client/api/helper/CommandContextHelper` (1/7 m, and it is itself an `@Event("CommandContext")`).

### batch-06 — `api.math` (5) + `api` + `api.helper` (7 classes)
41/102 methods, **0/20 fields** documented. `Pos2D` (12/22 m, 0/3 f), `Pos3D` (15/24, 0/2),
`Vec2D` (5/23, 0/4), `Vec3D` (9/27, 0/2), `Plane3D` (0/6, 0/9, no method docs at all),
`PlayerInput` (3/5 m, 0/7 f), `ModContainerHelper` (7/8 m).

### batch-07 — `api.library` + `core.library.impl` (8 classes)
Well documented (28/31, 0 examples). Work is **examples + closing the last gaps**.
`FUtils` (9/9), `FJavaUtils` (9/9), `FJsMacros` (44/44, 1 example), `FReflection` (23/25),
`FFS` (20/20), `FGlobalVars` (17/18, 0/1 f), `FRequest` (7/7), `FTime` (2/2).

### batch-08 — `core.library.impl.classes` (7) + `core.library` core (2)
`WrappedScript` (**0/9 m, 0/2 f, no class doc**), `ClassBuilder` (3/9), `FileHandler` (8/10),
`HTTPRequest` (11/11, 0/4 f), `LibraryBuilder` (1/2), `ProxyBuilder` (4/4, 0/3 f),
`Websocket` (5/5, 5/5 f), `core/library/LibraryRegistry` (**0/4 m, 0/4 f, no class doc**),
`core/library/BaseLibrary` (0/1 f, no class doc).

### batch-09 — `client.api.library.impl` (7 classes)
203/207 methods documented, 0 examples. Biggest *example* value in the codebase:
`FChat` (32), `FClient` (34, 1/1 f), `FHud` (23, 2/2 f), `FKeyBind` (10),
`FPlayer` (35/37), `FPositionCommon` (7), `FWorld` (62/64, 4/4 f).

### batch-10 — `client.api.helper` top-level (19 classes)
1277/1411 documented; concentrated gaps: `CommandContextHelper` (handled in batch-05),
`StatsHelper` (11/18, no class doc), `StyleHelper` (5/20), `SuggestionsBuilderHelper` (8/16),
`OptionsHelper` (46/46 but **0/6 fields**), `TextHelper` (10/11, 0/1 f),
`InteractionManagerHelper` (49/49), `PacketByteBufferHelper` (141/145),
`FormattingHelper` (6/7), `StatusEffectHelper` (12/13), `NBTElementHelper` (15/16),
`AdvancementProgressHelper` (12/13), `AdvancementHelper` (10/11),
`AdvancementManagerHelper` (10/11), `BlockPredicateHelper` (4/4), `DyeColorHelper` (5/5),
`NbtPredicateHelper` (3/3), `StatePredicateHelper` (2/2), `CommandNodeHelper` (no class doc).

### batch-11 — `client.api.classes.worldscanner.filter.**` (17 classes)
**Worst-documented package in the codebase: 1/51 methods, 2/31 class docs.**
`filter/` `BasicFilter` (0/4), `ClassWrapperFilter` (1/2), `GroupFilter` (0/5);
`filter/api/` `IAdvancedFilter`, `ICompare`\*\*, `IFilter`;
`filter/compare/` `BooleanCompareFilter` (0/1), `CharCompareFilter`\* (0/1),
`NumberCompareFilter` (0/1), `StringCompareFilter` (0/1);
`filter/impl/` `BlockFilter`, `BlockStateFilter`, `StringifyFilter` (0/5);
`filter/logical/` `AndFilter` (0/3), `NotFilter` (0/2), `OrFilter` (0/3), `XorFilter` (0/3).
(\*\* = no class javadoc.)

### batch-12 — `client.api.classes.worldscanner` (2) + `client.api.classes` core (5)
`WorldScanner` (22/22 — needs examples), `WorldScannerBuilder` (**0/24**),
`CustomImage` (41/43, 0/1 f), `RegistryHelper` (41/43), `TextBuilder` (12/13),
`FakeServerCommandSource` (**0/12**), `InteractionProxy` (0/1).

### batch-13 — `client.api.classes.render` (5) + `components3d` (7)
`Draw2D` (32/66, 2/6 f), `Draw3D` (53/55), `IDraw2D`, `IScreen`, `ScriptScreen` (2/6, 2/3 f);
`Box` (12/16, 0/5 f), `EntityTraceLine` (2/3, 0/4 f), `Line3D` (11/15, 0/3 f),
`RenderElement3D` (no class doc), `Surface` (14/31, 1/9 f), `SurfaceRenderTypes` (0/3),
`TraceLine` (10/14).

### batch-14 — `client.api.classes.render.components` (9)
`Draw2DElement` (18/28, 0/10 f), `Image` (22/33, 0/15 f), `Item` (19/32, 0/10 f),
`Line` (23/34, 0/10 f), `Rect` (26/37, 0/9 f), `Text` (21/33, 0/11 f), `Alignable`,
`RenderElement`, `RenderElementBuilder` (2/2). Zero field docs across the whole package.

### batch-15 — `client.api.classes.inventory` (11 of 21)
`Inventory` (40/44), `CommandBuilder` (**13/50**), `CreativeInventory` (21/24),
`PlayerInventory` (7/12), `FurnaceInventory` (12/17), `BrewingStandInventory` (16/17),
`CraftingInventory` (**1/6**), `ChatHistoryManager` (19/19), `CommandManager` (5/5, 0/1 f),
`ContainerInventory` (1/2).

### batch-16 — `client.api.classes.inventory` (remaining 10)
`AnvilInventory` (8/9), `BeaconInventory` (6/7), `CartographyInventory` (3/4),
`EnchantInventory` (8/9), `GrindStoneInventory` (4/5), `HorseInventory` (9/10),
`LoomInventory` (4/5), `RecipeInventory` (13/13), `SmithingInventory` (3/4),
`StoneCutterInventory` (6/7), `VillagerInventory` (7/8).

### batch-17 — `client.api.helper.world` (14) + `client.api.helper.inventory` (6)
763/763 world + 133/140 inventory methods documented; work is examples + 7 inventory gaps.

### batch-18 — `client.api.helper.world.entity` (11) + `client.api.helper.screen` (10)
449/462 and 82/111 documented. `ScreenHelper` subclasses carry the larger screen-side gap.

### batch-19 — `client.api.helper.world.entity.specialized` — boss/decoration/display/other/projectile/vehicle (21)
Fully documented (90/90); work is examples.

### batch-20 — `client.api.helper.world.entity.specialized.mob` (21)
47/52 documented; 5 gaps + examples. **Sibling-pair sweep is the headline** (see audit step 4) — it
found 2 sense-inversions on its first pass in batch-19 and 7 more on a follow-up sweep in batch-18.

### batch-21 — `client.api.helper.world.entity.specialized.passive` (30)
122/122 documented; work is examples.

### batch-22 — core event cancellation semantics (cross-cutting, 4 classes) — NEW, from batch-05
**Every cancellable event's docs currently promise that `cancel()` works, and it does not.**
`FJsMacros.ScriptEventListener.trigger` builds the `EventContainer`, hands `callback.accept(e, p)`
to `runner.threadPool.runTask(...)` and **returns immediately** — while every mixin reads
`event.isCanceled()` on the *next line*. So a script's `event.cancel()` is a race it almost always
loses. This contradicts the generated `Events.Cancellable` interface, the `cancel()` name, and
roughly 45 classes of prose written in batches 01-05.

State the rule **once, centrally** — do NOT scatter a hedge across the 45 event classes, which would
create exactly the inconsistency batches 01-05 avoided.

- `common/src/main/java/com/jsmacrosce/jsmacros/core/event/BaseEvent.java` (0/6 methods, no class doc)
- `common/src/main/java/com/jsmacrosce/jsmacros/core/event/BaseEventRegistry.java` (9/13 m, 0/5 f)
- `common/src/main/java/com/jsmacrosce/jsmacros/core/event/IEventListener.java` (no class doc)
- `common/src/main/java/com/jsmacrosce/jsmacros/core/event/EventListener.java` (1 m, no class doc)

Cover: what `joinable` events do differently; whether `BaseProfile` dispatches synchronously for some
event kinds; and cross-link from `FJsMacros.on` if its own doc needs it. Name
`JsMacrosThreadPool` (`core/threads/JsMacrosThreadPool.java`, undocumented) as the mechanism.

**Alternative for a maintainer:** make `trigger()` synchronous for `cancellable` events, which would
make all 45 classes' existing docs correct as written. That is a code fix, out of scope here.

---

## Known docgen defects — DEFERRED to the docgen rework (not this effort's to fix)

All four live in `buildSrc` (`pydoclet` / `tsdoclet`) and are **deliberately not fixed here**: the
docgen is being reworked on another branch. Recorded so the work is not lost and so no later batch
tries to work around them in javadoc. Verified empirically in batch-09 by running all three doclets
and diffing the artefacts — not inferred from source, which was wrong three separate times.

1. **`<br>` in prose is silently dropped by `pydoclet`.** All three doclets switch on `DocTree.Kind`;
   `pydoclet` has no arm for it, and this JDK's `DocTree.Kind` has **no `BR` constant at all**
   (verified: `ATTRIBUTE AUTHOR CODE … TEXT THROWS …`), so `<br>` falls through to nothing.
   `webdoclet` emits it, which is why the pervasive house style has never looked broken.
   *Impact:* prose line breaks run together in the Python stubs. **~100 instances across batch-09's
   7 files alone**, plus every earlier batch. **Cosmetic** — a lost line break, not lost content.
   *Not repaired* by rewriting ~100 prose sites; that is a worse risk than the defect.

2. **`JavaList` (and `JavaArray`, `JavaSet`, `JavaMap`, `JavaCollection`, `JavaClass`, `JavaObject`) are
   error types.** `tsdoclet/PackageTree.java:53-55` **deliberately skips** `predefinedClasses`
   (`java.util.List`, `java.lang.Collection`, `java.util.Map`, `java.util.Set`, `java.lang.Class`,
   `java.lang.Object`, …) when building the `Packages` tree, assuming a static header declares them.
   `Graal.d.ts:689-694` then aliases to them (`type JavaList<T> = Packages.java.util.List<T>`), and
   `java.lang.Array` is neither in `predefinedClasses` nor emitted. Nothing declares any of them, so
   every alias dangles and the **~296 `JavaList<...>` uses in the shipped `.d.ts` resolve to nothing**.
   *Impact:* invisible only because the shipped tsconfig sets `skipLibCheck: true`; with it off,
   `Graal.d.ts` alone reports **532 errors**. In practice `Array.from(someJavaList)` degrades to
   `unknown[]`, so `Player.addInputs(PlayerInput[])` fails with `TS2345` — which is why the
   `createPlayerInputsFromCsv` example uses the `.size()`/`.get(i)` idiom.

3. **A duplicated `@param` silently discards a description.** `pydoclet`'s `getParamDescriptions` uses
   `paramMap.put`, so the **last** wins — and javadoc attributes everything after a block tag to that
   tag, so the prose *and* the `example:` following a first `@param` vanish from the Python stub
   while the `.d.ts` and `.html` stay correct. `@return` uses `findFirst()`, so the **first** wins;
   `tsdoclet` emits both when non-empty. Found 36 such duplicates in batch-09, all introduced by it.

4. **52 of 632 generated Python stubs fail `ast.parse`.** `pydoclet`'s `getImports()` does
   `.replace(".", "_")` on a `TypeMirror` that renders annotated types as `pkg.@ann Type`, leaving
   spaces inside the identifier: `net_minecraft_client_multiplayer_@org_jetbrains_annotations_Nullable
   ClientLevel = TypeVar(...)`. Pre-existing, long-standing, and unrelated to any doc text.

**Consequence for the remaining batches:** a green build and a clean `tsc` pass do **not** establish
that a javadoc change is correct, because `pydoclet` damage exists only in the generated `.py` — which
still parses, so `ast.parse` does not catch it either. **Diff the generated `.py` against the source
javadoc**; that is the only check covering the class of defect that matters most here.

---

## Conventions (applies to every batch)

- Edit **only** javadoc comments and the `com.jsmacrosce.doclet` annotations
  (`@DocletDeclareType`, `@DocletIgnore`, `@DocletReplaceParams`, `@DocletReplaceReturn`,
  `@DocletReplaceTypeParams`). No code, signature, or annotation-semantics changes.
- Every documented method: description derived from reading the implementation (never guessed),
  `@param`, `@return`, `@throws` where relevant, and an `example:` block using `<pre>`-fenced JS
  in the existing house style (see `FJsMacros.assertEvent`).
- Examples must use only APIs that exist — cross-check every symbol against source.
- Plain javadoc only: `@param/@return/@throws`, `{@link}`, `{@code}`, `{@literal}`, and
  `example:` blocks with `<pre>` fencing. No exotic HTML, no tables, no custom inline markup.
- Additive only: never delete or wholesale-rewrite existing javadoc.
- If behaviour is unclear, say so in a comment the validator can flag rather than guessing.

### Doclet quirks learned in batch-01 (hard constraints for all later batches)

1. **Never write `=>` in an example.** The pydoclet drops the `&gt;` entity, so `=>` renders as a
   bare `=` in the shipped Python stubs (`methodToJava((event) = {`). Use
   `function (event) { ... }`.
   **Corrected in batch-09:** this item used to add *"The same applies to `>` in prose inside
   `{@code}`"*, which contradicted item 4. Both halves advised against writing `&gt;` inside
   `{@code}`, but for **different reasons**: in ordinary prose the entity is **deleted**, whereas
   inside `{@code}` it **survives verbatim** as the literal characters (item 4). The advice stands; the
   stated reason was wrong, and the two items no longer contradict each other.
   **Note:** this is about *example code*. `<br>` in prose is the pervasive house style and the **web**
   doclet emits it deliberately, so it is correct there — but see item 6: **`pydoclet` drops it**, so
   it is lost in the Python stubs (cosmetic: a lost line break, not lost content). Writing `<br>` is
   still right; just do not expect the Python stub to show the break.
2. **Never add `@see`.** The web doclet ignores `@see` entirely — it renders nowhere in the web
   docs and leaks as raw `@see` lines into the shipped `.d.ts`. Use inline `{@link}` instead.
3. **`{@link}` silently resolves to the wrong overload** if the parameter type is not imported or
   fully qualified — the doclet falls back to matching on the simple name. Fully qualify the
   parameter type, or add the import. Verify the emitted anchor in the generated HTML.
4. **REWRITTEN IN BATCH-09 after a validator caught two false claims in the first rewrite. Trust the
   table below; the prose around it has been wrong twice already.** The original one-liner ("`XMLBuilder`
   does no escaping at all, so a stray `&`/`<` ships malformed XML") is **half right** — right about
   the *web* doclet, wrong about `pydoclet`. Ground truth from running all three doclets against a
   probe file:

   | source javadoc | `.py` (pydoclet) | `.html` (webdoclet) | `.d.ts` (tsdoclet) |
   |---|---|---|---|
   | `&lt;` | **deleted** → `i 3` ❌ | `&lt;` → renders `<` ✅ | `<` ✅ |
   | `&amp;` | **deleted** ✅(harmless here) | `&amp;` ✅ | decodes ✅ |
   | `&#167;` | **deleted** | `&#167;` ✅ | ✅ |
   | bare `&` (`a && b`) | **deleted** → `a b` ❌ | raw `&` → **malformed XML** ❌ | — |

   **Mechanism:** `pydoclet/parsers/ClassParser.createDescription` switches on `DocTree.Kind` with cases
   for only `TEXT`, `CODE`, `LINK`, `LINK_PLAIN`, `START_ELEMENT` and **no `default:`** — so every
   `EntityTree` (`&lt;`/`&gt;`/`&amp;`/`&#167;`) and every bare `&` is **silently dropped**. `XMLBuilder`
   does one transform (`\n`→indent) and **no escaping at all**, which is why the *web* doclet ships a
   bare `&` as malformed XML. Both mechanisms are real; they live in different doclets.

   **The consequence that matters: `&lt;` in an example ships broken JavaScript to Python users.**
   Batch-09 had 13 of them, e.g. source `for (let i = 0; i &lt; found.size(); i++)` →
   `.py` `for (let i = 0; i found.size(); i++)`. `.d.ts` and `.html` were correct in every case.

   **This survives both normal gates, which is why it needs its own check.** The build is green, and
   the example still type-checks under `tsc` — because the damage exists only in the `.py`, and the
   Python still *parses* (it is inside a docstring). **A green build and a clean type-check are
   necessary and NOT sufficient.** Diff the generated `.py` against the source, or grep it.

   **The rule — and it is NARROWER than batch-09 first wrote it.** Only **entities** are destructive.
   A **bare** `<`, `>` or `&` is ordinary TEXT and survives all three doclets intact — verified
   against 20+ live instances in batch-09 (`>> 4`, `difficulty >= 3`, `lit > 0`, `if (++ticks > 100)`,
   `is(">=", 10)` all ship correctly). So the precise rule is:

   > **Never write an HTML entity (`&lt;` `&gt;` `&amp;` `&#167;`) inside an example or in prose that
   > reaches the Python stub. Write the bare character instead.**

   Batch-09 initially told its fix-writer "no `<` and no `&` inside an example at all" and had 13
   working examples needlessly restructured to avoid bare brackets — producing an internally
   inconsistent file (bare `>=` used freely in 6 examples while 13 others were contorted) and, in one
   case, a **new type error** from an over-clever rewrite. **Do not repeat that.**

   ### THE ACTUAL RULE — and the "bare character" version is ALSO WRONG

   A fix-writer tested "just use the bare character" by building it, and it **still shipped broken**:

   | source | shipped `.py` |
   |---|---|
   | `escapes & to && since 1.9.0` | `escapes to since 1.9.0` ❌ |
   | `@return § -> &` | `§ ->` ❌ |
   | `Consumer<Boolean>,` | `Consumer Boolean >,` ❌ |

   **Cause: a bare `&` and a bare `<` are not `TEXT` in javac's doc-comment parser** — they parse as
   non-`TEXT` trees. `tsdoclet` and `webdoclet` have a `default -> append(docTree)` arm so they keep
   them, which is why the `.d.ts` and `.html` looked fine and the premise seemed true; **`pydoclet` has
   no `default:` and drops them.** So the safe set is narrower than it looks:

   | written as | `.py` | `.d.ts` / `.html` |
   |---|---|---|
   | `&amp;` `&lt;` `&gt;` `&#167;` (entity) | ❌ dropped / literal | ✅ or decoded |
   | bare `&`, bare `<` (non-TEXT) | ❌ **dropped** | ✅ kept |
   | bare `>`, bare `§` (TEXT) | ✅ kept | ✅ |
   | **`{@code X}`** | ✅ `getBody()` unmodified | ✅ unmodified |

   > **The only form that is correct in all three doclets is `{@code X}`.** `pydoclet`'s `case CODE`
   > emits `getBody()` verbatim and the other two doclets pass it through too, so a bare character
   > wrapped in `{@code}` survives everywhere. This is also the convention `FChat` already uses for
   > ampersands (`{@code \x26}`).

   **There is no representation that is simultaneously perfect everywhere** — the three doclets
   disagree by construction. Prefer `{@code X}` for any of `& < >`, accept the Python stub's quoted
   form (`'&'`, `'<'`) as the cost, and do **not** restructure working examples to avoid characters.

   **A literal `&` in an *example*** is a separate case: the `\x26` form is correct there, because the
   generated docstring is non-raw and Python decodes it to a real `&`. **But note the resulting
   inconsistency**, which is a real open question: a file can end up documenting the same literal two
   ways (`{@code &a}` in prose, `"\x26a"` in the example). `FChat` is in exactly that state — flagged,
   not resolved, because either spelling is *correct* and only one can be canonical.

6. **NEW IN BATCH-09: `<br>` in prose is silently dropped by `pydoclet`** — same root cause (it
   switches on `DocTree.Kind` with no `BR` case). Verified: `FChat.java` has **11** `<br>` in prose and
   **0** survive into `FChat.py`; the sentences simply run together. `webdoclet` *does* emit `<br>`, so
   the web docs are correct, which is why the pervasive house style has never looked broken.
   **Severity: cosmetic** — a lost paragraph break, not lost content, unlike the entity cases.
   Blast radius: **100 `<br>` across batch-09's 7 files alone**, plus every earlier batch.
   **Decision: recorded, not fixed.** Repairing it means restructuring prose in ~100 places for a
   cosmetic Python-stub gain, which is a worse risk than the defect. Do not "fix" it opportunistically
   in a later batch.

   **A literal `&` (Minecraft colour codes): `\x26` is the right answer, `&amp;` is not.**
   `pydoclet`'s `case CODE` emits `getBody()` **unmodified**, so an entity inside `{@code}` survives —
   which makes `&amp;` *look* right and is a trap: it ships the literal 5 characters `&amp;a` into
   the Python stub. `"\x26a"` is correct because the generated docstring is **non-raw**, so Python
   decodes it to a real `&` at parse time (verified by executing the generated module's docstring).
   Cost to record, not to hide: a literal `\x26` is what the reader sees in the `.d.ts` and `.html`.
   And `\x26` is a *string* escape, so it only works inside a JS string literal — `&&` in boolean
   position has no such dodge, which is exactly why the rule above is "nest the `if`s".
5. **`isCanceled()` does not exist in the generated TypeScript.** `tsdoclet/Main.java` hard-codes
   `interface Cancellable { cancel(): void; }`, so a script cannot observe cancellation state. Do
   not use `isCanceled()` in an example.
6. **The listener idiom is `JsMacros.on("EventName", JavaWrapper.methodToJava(fn))`.** There is no
   `JsMacros.addListener`, and `JsMacros.Events` is a TypeScript-only namespace, not a runtime
   object. Confirmed against `docs/web/examples/tpsservice.html` and `manual-tests/suite.js`.
7. **`@DocletReplaceReturn` type names need a matching `@DocletDeclareType`** or the type is
   dangling in the generated `.d.ts`. Do not claim a field is "narrowed to a X union" unless such a
   declaration actually exists — the pre-existing `Dimension`/`SoundId`/`EntityUnloadReason` cases
   are NOT unions (they are `type X = string` in `docs/typescript/headers/McIdsAndEnums.d.ts`).
8. **Build commands are unqualified.** The doclet tasks are registered on the *root* project via
   Stonecutter's `centralScript`: `./gradlew generatePyDoc generateTSDoc generateWebDoc`.
   `:common:1.21.8:generatePyDoc` does not exist.
9. **Never `{@link}` a field whose declared type is primitive — it renders as a dead link.**
   `ClassParser.getURL` returns `href=""` when `type.asType().getKind().isPrimitive()`, which
   fires for a primitive-typed *field* but never for a *method* (an `ExecutableElement`'s type kind
   is `EXECUTABLE`). So `{@link Foo#isEmpty()}` is fine, `{@link #someIntField}` is dead. Use
   `{@code fieldName}` for primitive fields. 14 pre-existing sites exist; batch-02 fixed 5.
10. **A `*/` cannot appear inside a javadoc comment.** So a `/** @type {...} */` TypeScript
    annotation cannot be shown in an example — the `*\/` escape renders literally in the generated
    docs. Restructure the example instead (batch-02 removed the offending variable).
11. **`@Nullable` on a generic field mangles the Python stub** — it emits a TypeVar declaration
    instead of a type. Pre-existing; not introduced by any batch.
12. **A doclet-hostile tag may be *replaced*, never silently dropped** (learned in batch-06).
    `@see` is ignored by the web doclet and leaks raw into the shipped `.d.ts`. When removing one,
    carry the information across as prose or an `{@link}`, and report every substitution.
13. **Do not document a `java.lang.Object` override** (`equals`, `hashCode`, `toString`, `clone`, …).
    `tsdoclet`'s `AbstractParser.isObjectMethod` excludes one from the shipped `.d.ts` **only while
    it has no javadoc**, so documenting one makes a brand-new member appear in the user-facing
    typings. Batches 01-06 all leave them bare for this reason. (`compareTo` is fine — not an
    Object method.)
14. **A class absent from the shipped `.d.ts` must not get a `Java.type(...)` example.**
    `Java.type` resolves through `Packages`, so a missing class types as `unknown` and cannot be
    `new`-ed — it fails `tsc` under both configs. Batch-06 removed `Plane3D`'s example for this
    reason and let the prose carry it. Root cause of such absences: `tsdoclet/Main.java:41-43`
    whitelists only `client.api.helper.` and `client.api.classes.inventory.`; everything else is
    discovered only by being referenced from a whitelisted signature.
15. **The web doclet silently drops `{@link}` labels** (found in batch-22, doclet-wide).
    `{@link Foo#bar() Bar}` renders as `<a …>Foo#bar()</a>` — the label `Bar` is discarded, so the
    reader sees the raw Java reference instead of the prose word the author intended. Not a
    correctness problem, but a labelled link is worse than an unlabelled one. **Write `{@link
    Foo#bar()}` or put the friendly word in the surrounding sentence** — do not rely on a label.
    **Re-verified in batch-19:** a source `{@link #getPos() getPos()}` renders as
    `EntityHelper#getPos()` (the target), the label gone. A writer challenged this rule citing a
    rendering that was in fact a *target*, not a label. **The rule stands.** Note, however, that
    **110 files at HEAD already use the labelled style**, so a label that merely repeats its own
    target is inert-but-harmless and **tolerated** in new prose; only a label that names something
    *different* from its target (e.g. `{@link #getLerpProgress(double) the display's own progress}`)
    is worth avoiding, because there the dropped label loses real information. Do not churn existing
    files to strip labels.
16. **`tsdoclet` emits members regardless of javadoc**, except for Object overrides. The real rule in
    `ClassParser.genTSInterface():206-215` is that every public field/constructor and every
    non-obfuscated, non-Object method is emitted unconditionally; the *only* javadoc-conditional
    filter is `AbstractParser.isObjectMethod():565-574`, which stops filtering a method once it has a
    doc comment. So documenting `equals`/`hashCode`/`toString`/`clone` injects a brand-new
    user-facing member, and **nothing else you write can change the shipped member set** — this is
    the precise form of quirk 13, and it means "did documenting this change the `.d.ts`?" usually has
    the answer "no, only descriptions changed".

17. **CORRECTED IN BATCH-16 — the earlier version of this item was WRONG.** It previously claimed
    `node --check` **passes** deleted-`&&`/`<` damage, on the strength of one writer's report. The
    batch-16 validator tested 9 shapes and it is the other way round: `node --check` **rejects every
    one** (`if (a  b) {}`, `for (let i = 0; i 3; i++) {}`, `if (c>0 c<m()) {}` …). The reason it never
    fuses: `pydoclet` deletes the `&&`/`<` but **leaves the surrounding whitespace**, so the operands
    stay two separate tokens and the result is always a parse error.
    **So `node --check` IS a real gate for this corruption class — use it, and still byte-count the
    generated `.py`**, because the byte count is what proves *which* tokens were lost, and the token
    diff against the source is what catches a **block-count mismatch** (see 18).
18. **Assert the example-block COUNT, not only that no matched pair loses a token** (batch-16). A
    duplicated `example:` inside one javadoc shipped to **all three** trees, concatenating two bodies
    onto one line in the `.py` — and it was the *sole* discrepancy in the writer's own token diff:
    **83 java blocks against 82 `.py` blocks**. A count mismatch is the cheapest possible red flag, so
    a source↔`.py` comparison must fail when the counts differ, before it looks at token loss at all.
19. **`pydoclet` emits no docstring for field members anywhere**, so field examples never reach the
    Python stubs (the web doc and `.d.ts` still get them). Unfixable in javadoc — do not contort the
    text or reach for exotic markup.
18. **`pydoclet` emits field *declarations* but never field *javadoc*** (batch-10). So an `example:`
    attached to a **field** is invisible to Python readers no matter how well written it is, because
    the comment is never emitted. The web doc and the `.d.ts` still get it. Unfixable in javadoc —
    do not contort the text or reach for exotic markup; just know field examples are
    `.html`/`.d.ts`-only.
19. **`pydoclet` has no `@throws` support at all** (batch-10): 0 `Raises:` across 632 generated
    `.py` files, against 47 Java files that use `@throws`. A `@throws` tag is still correct and still
    reaches the web docs and the `.d.ts`; it just does not reach the Python stubs.
20. **`tsdoclet` and `pydoclet` disagree on `char` returns** (batch-10): `tsdoclet` maps `char` to
    `number` (`AbstractParser.java:286-288`) and `pydoclet` to `str` (`ClassParser.java:406-408`).
    **The pydoclet is the correct one** — Truffle's `DefaultCharacterExports.isString()` is true for
    `Character.class` and no numeric export is exposed, so a `char` arrives as a one-character
    *string*. Consequences: `{@code String.fromCharCode(c)}` is wrong (`fromCharCode("c")` is `\0`);
    use `"§" + c`. **Do not "correct" prose to match the buggy `.d.ts` type.**
21. **`pydoclet` drops every inline HTML element, and leaves the whitespace behind** (batch-17).
    Verified empirically across **12 independent files**, not inferred: every one of
    `DyeColorHelper`, `FormattingHelper`, `AdvancementHelper`, `CommandNodeHelper`,
    `SuggestionsBuilderHelper`, `StyleHelper`, `PacketByteBufferHelper`,
    `InteractionManagerHelper`, `StatsHelper`, `StateHelper`, `StatusEffectHelper` and
    `EnchantmentHelper` has `<b>` in its Java and **0** in its generated `.py`. Same root cause as
    quirk 6 (`<br>`): `pydoclet` switches on `DocTree.Kind` with no arm for these elements.
    The cost is cosmetic but *visible* — `…in that list too<b>,</b>` becomes `…in that list too ,`
    with a stray space before the punctuation, which reads as a typo in the stub. `.html` and `.d.ts`
    render it correctly. `<b>` is already used in **35 files** in `common/`, so it is an established
    convention and **batch-17 accepted it** rather than treating the 3 new uses as defects. Do not
    reach for it in *new* prose where `{@code}` or sentence structure will do, but do not "fix" the
    35 existing files either — same reasoning as quirk 6.
22. **The `tsc` example gate has a silent-void trap: do NOT concatenate the headers into one file**
    (batch-17). A top-level `import`/`export` anywhere in the set makes the whole thing a *module*,
    so every global disappears — `Packages` and `Java` both report "cannot find" — and, fatally,
    **a bogus method call on a real helper does not error at all.** That configuration reports a
    clean run having checked nothing. Pass the headers as **separate `files` entries** to `tsc`. With
    that wiring **no ambient shim file is required**: `World`/`Chat`/`Client` resolve as real global
    namespaces straight from the tsdoclet header, and a fake method genuinely errors
    (`Property 'getTOTALLY_BOGUS_9' does not exist on type 'BlockDataHelper'`). This is the batch-17
    validator's finding and it is the strictest form of the "a check that never ran reports 0 errors,
    in the same words as one that passed" rule.
23. **"Stranded description" is only checkable on the RENDERED HTML, never by line position in the
    source** (batch-17). A source-level check compares the first block tag to the first following
    prose line, which fires identically on a **wrapped `@return` description**
    (`@return {@code true} if …,\n{@code false} otherwise.`) — 53 false positives on batch-17's 20
    files, against a true baseline of 0. The real signature in `build/docs/web/**.html` is a
    `<p class="description">` appearing **after** `</table>` inside a `classItem` block: the doclet
    wraps the example *inside* the description `<p>`, so a description after the params table is
    unambiguous. That check is 0 across batch-17's 466 member blocks. **If you cannot make a source
    checker reliable, fall back to the artefact rather than quoting a green number from it.**

### How to audit a batch — learned the hard way in batch-10 (6 validation rounds)

The dominant defect class in this codebase is **not** a doclet problem. It is **confident, specific,
plausible claims about Minecraft semantics that nobody checked against the implementation.** Batch-10
had ~20 of them across six rounds, and **not one was visible to `gradlew`, `tsc` or `node --check`**.
They are all wrong *facts* in shipped user-facing docs: a wrong colour code, a wrong count, an
example calling a method that exists nowhere in the repo, prose describing an overload the method
does not call.

**Audit by cross-reference, not by return value.** Rounds 1–4 of batch-10 each ran "pair every
`@return` with its own body" and each found more; that method is **structurally incapable** of
finding the defects that remained, because all of those are prose about something *other than* the
method's own return. Invert it:

> Enumerate every `{@link}` target, every relational sentence ("the same as", "the opposite of",
> "its siblings", "unlike", "whereas", "not … but"), every bare numeral, and every
> `always`/`never`/`only`/`all`/`most`/`exactly` — and resolve each against **the target's** body.

**Three dimensions that must be added, or the pass misses the commonest defects** (established in
batch-11, where all 6 blockers were one of these and the first, cross-reference-only pass found only
half of them):

> 1. **Timing claims** — *built* vs *constructed* vs *at test time* vs *per test* vs *once* vs
>    *cached* vs *memoised*. Open the body and confirm **when**, not just the outcome. A constructor
>    that merely stores a field validates nothing, and a neighbour that validates in its constructor
>    is the trap.
> 2. **Cardinality claims** — *each*, *all*, *every*, *only*, *never*, *exactly N*, *at most once*.
>    Verify the count against the implementation. `List.removeAll` removes **every** occurrence while
>    `List.remove(Object)` removes one; copying the sentence from one to the other is how this
>    happens.
> 3. **Every sentence naming another class's *documentation*** — confirm that document exists and
>    says that. One batch-11 blocker was a claim about a discrepancy in a javadoc block that was in
>    fact bare at HEAD, so the sentence referred to itself.
> 4. **EVERY SIBLING PAIR, checked member by member — established in batch-18, where this became the
>    single most productive audit in the effort.** The dominant defect is not random: it is **one
>    member's documentation carried onto its twin, sometimes with the sense inverted.** Batch-17 found
>    `getConflictingEnchantments` documented as the inverse of the *correct* `getCompatibleEnchantments`.
>    Batch-18 found `getRightInput` as the inverse of the correct `getLeftInput`, and `turnLeft` as the
>    inverse of `turnRight`; then, on sweeping the same file, it found **the same inversion in three
>    further members** that nothing had reported.
>
>    For each class, enumerate every pair sharing a stem (`getLeft`/`getRight`, `getMin`/`getMax`,
>    `getCostA`/`getCostB`, `isX`/`isNotX`, `setValue`/`getValue`, `before`/`after`) and **diff the two
>    descriptions against the two bodies.** Do not assume they agree because they read alike, and do not
>    assume they differ because the names do. Check the **class doc and any aggregate/overview method**
>    (`getInput()`, `toMap()`, `getSummary()`) for the same carry-over — in batch-18 the class doc, an
>    aggregate getter and a third method each carried a variant of one inverted claim.
>
>    The tell is **one fact stated three times in three slightly different ways**: a
>    `SliderWidgetHelper` saying "ninths" in its builder and "tenths" in two methods is not two
>    independent errors but one claim copied wrong — which means the bodies were read once and the
>    prose written from memory twice. Fix the source of the copy, not the copies.

Two rules for the writers that follow from this:

> - **"The overload next to it does X" is not evidence about "this one does Y."** This carry-over
>   produced two separate batch-10 defects (the hover-action list applied to `getClickAction()`;
>   `sendPacket()`'s prose applied verbatim to `receivePacket()`).
> - **Re-derive every numeric census from the compiled class, never from Stonecutter-gated source.**
>   Source contains `//? if` blocks whose entries do not ship in the active target; counting source
>   lines inflated batch-10's packet census from 200 to 202.

**And verify a report before acting on it — twice in batch-10 a validator's finding was false, and
"fixing" it would have corrupted correct documentation.** Both times the cause was the same parse
trap: when reading a static initializer, **small constants appear as `iconst_*`/`bipush`/`sipush` and
not only `ldc`**, and `PacketByteBufferHelper` uses **`ldc_w` exclusively**, so an `ldc`-only parse
finds **zero** keys. A dropped entry silently **flips a tally**. Related trap: a field can be
assigned from a literal in the static initializer but be **transformed in the constructor**
(`DyeColor` applies `ARGB.opaque()` to two of its three colour fields there) — read the constructor,
not just the initializer. **If a count of 16 comes out 14, suspect the parse before the data.**

## Validation (every batch)

1. `./gradlew generatePyDoc generateTSDoc generateWebDoc` must succeed — javadoc parse errors
   are a hard failure.
2. Every new/modified `example:` resolved symbol-by-symbol against the actual Java source.
3. Where feasible, type-check examples against the generated TypeScript definitions. **Run `tsc`
   against the fresh `build/docs/typescript/headers/JsMacrosCE-2.0.0.d.ts` **combined with**
   `docs/typescript/headers/*.d.ts`, and confirm a non-zero compiled-file count.** Compiling the
   headers alone, or `Graal.d.ts` alone, is meaningless: `Graal.d.ts` alone makes every
   `Java.type(...)` return `unknown` and leaves the `@Library` globals (`Chat`, `World`, …)
   undeclared, which manufactures **dozens of fake errors**. A batch-12 round reported "58
   `Cannot find name 'Java'` errors" from exactly this mistake; with the correct invocation the same
   examples produced **2**.

   > **Do NOT gate on a bare "532". Corrected in batch-13.** That number was an artefact of one
   > ad-hoc file set, and across rounds subagents variously reported **528 / 532 / 549 / 153 /
   > 10427** for the "same" check — because the result depends entirely on *which* files are handed
   > to `tsc`. The two figures that actually mean something, and which I have verified:
   >
   > - **The project's own tsconfig** — `tsc -p build/docs/typescript/tsconfig.json --noEmit` —
   >   gives **0 errors**, and is what a user or CI runs. It sets `skipLibCheck: true`; with that flag
   >   off the header tree produces a large, file-set-dependent number that means nothing on its own.
   > - **The examples, individually, against the correct combined header set** — must be **0**. Wrap
   >   each example in `{ }` in its own file so every error is attributable, and note that a
   >   `skipLibCheck: false` run will *always* show errors for any example that touches a `JavaList`
   >   / `JavaMap` / `Packages` alias, because those are the dangling error types above — so the
   >   examples' gate is the `skipLibCheck: true` run.
   >
   > **`build/docs/typescript/tsconfig.json` is a weak check, not a gate.** It compiles **one**
   > zero-byte `scripts/test.ts` plus the header, under `skipLibCheck: true` — so it verifies the
   > header parses and the program links, and **exercises not a single example**. Useful as a smoke
   > test; never quote it as evidence that the examples are sound.
   >
   > The structural guarantee that no *shipped member set* changed is **gate 5 (zero non-comment
   > lines)**, not a `tsc` count: javadoc can only reach the `.d.ts` as comment text, so untouched
   > signatures and untouched `@Doclet*` annotations mean the emitted interface is unchanged.
   >
   > **Always run a positive control** (inject a call to a non-existent method and confirm it is
   > caught) before believing a clean example count, and **state the compiled-file count** — a run
   > that compiles zero or one file is vacuous.

   **Validate your checkers, or do not report a number from them.** This has bitten in **both** roles
   in batch-14, and the failure is silent: a checker that examines nothing reports `0` in exactly the
   same words as one that finds nothing. Concrete instances worth guarding against:
   - A **method-existence** grep whose corpus **includes comments** self-matches the injected text
     and returns a vacuous pass. Use a **comment-stripped** corpus.
   - A **word-set** prose diff reports "0 deleted prose" for an injected sentence whose words all occur
     elsewhere. **Set difference cannot see that** — use a **sentence-level multiset**.
   - A line filter that rejects every line reports "0 across N blocks" having examined nothing. Prove
     each checker by injecting a known defect into a **copy** and requiring **both** that it fires on
     the injection **and** that it reports 0 on pristine.
   - A negative control that silently fails 0/9 is worse than no control — fail loudly instead.
   - If a checker cannot be made reliable, **say so and fall back to reading**, rather than quoting a
     green number from it. Both the batch-14 validator and its fix-writer did exactly this, and it is
     the behaviour to copy.
4. Flag (never silently drop) unverifiable examples; list them for the in-game test queue.
5. **Byte-level check of the generated `.py`** — `&` and `<` must be 0 inside every `example:`.
   Grepping for entities is useless (the damage is a *deletion*); and `node --check` passes the
   damaged text because it is still valid JavaScript. See quirk 17.
6. **Inverted cross-reference audit** — resolve every `{@link}` target and every relational sentence
   against the *target's* body, and re-derive every numeric census from the compiled class. This is
   the only check that finds the dominant defect class; see the audit section above.
7. **Disprove any surprising report before acting on it.** Two of batch-10's validator findings were
   false. Re-derive the number yourself; a tally that came out short means the parse dropped entries.
   **Batch-17 makes this the third batch where it fires, and in that one 2 of 5 findings were false —
   so the prior is now roughly 1-in-3, not a rarity.** Both false ones were the *same* mistake: a
   plausible-but-wrong reading of a convention, applied confidently and specifically.
   - `Direction.toYRot()` is `(data2d & 3) * 90`, and `data2d` is the **third** enum argument, not the
     first — so `DOWN` and `UP` both carry `−1` and both give `270`.
   - `BlockStateProperties.LEVEL_FLOWING` is `1..8`, **not** the pre-1.20 `0..7`.
   The tell in both: the report was *specific* and cited a real file and line, yet rested on an
   assumption nobody had checked. **A citation is not a derivation.** When a finding says "X reads
   field F", verify that F is the field being read — especially when the name is suggestive.
   **Make "verify before editing" an explicit instruction to every fix-writer**; both times in
   batch-17 the fix-writer's refusal to edit is what saved correct documentation.
   Corollary now recorded from the same batch: **a false finding can still sit next to a real
   defect.** `getLevel()`'s scale prose was right but its *example* was genuinely broken. Discard
   only the specific claim that is wrong, then keep auditing the surrounding block.
   - **A *negative* result is the most suspicious kind of finding.** In batch-18 the validator
     reported "I found **no** `instabuild`/permission check anywhere in `BaseCommandBlock` or
     `CommandBlock`" and treated the doc claim as unverified. The check was in a **third class**:
     `Player.canUseGameMasterBlocks()` at `Player.java:1920-1921`, consulted from
     `CommandBlock.java:128`. "I could not find it" is a statement about **where you looked**, never
     about whether it exists — especially for a claim that spans a call chain.
   - **Two methods with near-identical names can have opposite ranges.** `Vec3D.getYaw()` negates
     `Mth.wrapDegrees` (so `(−180, 180]`) and `EntityHelper.getYaw()` does not (so `[−180, 180)`).
     Reading one and carrying it to the other is the batch's defect class in its purest form.
