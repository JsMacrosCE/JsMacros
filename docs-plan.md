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
47/52 documented; 5 gaps + examples.

### batch-21 — `client.api.helper.world.entity.specialized.passive` (30)
122/122 documented; work is examples.

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
   `function (event) { ... }`. The same applies to `>` in prose inside `{@code}`.
2. **Never add `@see`.** The web doclet ignores `@see` entirely — it renders nowhere in the web
   docs and leaks as raw `@see` lines into the shipped `.d.ts`. Use inline `{@link}` instead.
3. **`{@link}` silently resolves to the wrong overload** if the parameter type is not imported or
   fully qualified — the doclet falls back to matching on the simple name. Fully qualify the
   parameter type, or add the import. Verify the emitted anchor in the generated HTML.
4. **No bare `&` or `<` in javadoc character data.** `XMLBuilder` does no escaping at all, so a
   stray `&`/`<` ships malformed XML. Build passing is NOT sufficient — inspect the generated HTML.
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

## Validation (every batch)

1. `./gradlew generatePyDoc generateTSDoc generateWebDoc` must succeed — javadoc parse errors
   are a hard failure.
2. Every new/modified `example:` resolved symbol-by-symbol against the actual Java source.
3. Where feasible, type-check examples against the generated TypeScript definitions.
4. Flag (never silently drop) unverifiable examples; list them for the in-game test queue.
