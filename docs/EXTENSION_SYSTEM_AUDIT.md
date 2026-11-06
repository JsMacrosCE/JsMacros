# Extension system: design review and 3.0.0 removal plan

This is an internal engineering note, not script-facing documentation. It records
what the extension system is worth keeping, where its design will hurt later, and
a concrete list of what to delete when the 3.0.0 breaking window opens.

The review is of the extension system as it stands after
`515ef511 Load extension archives from memory and find their translations`. That
commit is a real improvement over what it replaced, and several findings below
are inherited rather than introduced. They are marked where it matters.

## Scope

Reviewed: `common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/` (6 types,
1073 lines), the library registration path it feeds
(`core/library/LibraryRegistry.java`, `core/library/impl/classes/LibraryBuilder.java`),
the consumers (`Core.exec`, `MixinTranslationStorage`, `AutoCompleteSuggester`,
`FileChooser`, `FGlobalVars`), the bundled adapters
(`extension/graal/**`), the build wiring that produces the nested jars
(`extension/*/build.gradle.kts`, `fabric/build.gradle.kts`, `neoforge/build.gradle.kts`),
and the tests under `extension/src/test/`.

The extension test suite was run and its output read; results are cited where they
support a finding. Minecraft was not launched and the bundled extension jars were
not exercised at runtime, so runtime-only claims below are stated as risks rather
than observations.

## Status of this pass

The section 5.1 list (drop now) and the five correctness defects in section 4 are
implemented, together with the missing integration test and CI coverage:

- removed `minCoreVersion()` / `maxCoreVersion()`, the bodyless `addURL`
  override, and the primitive dependency form;
- dependency descriptors are array-only, validated on read, and resolved in
  declaration order;
- `InMemoryJar` now rejects truncation, size mismatches, and checksum mismatches,
  bounds each entry, and refuses a local header whose name differs from the entry
  being read;
- extension translations merge in loader order instead of hash-set order;
- `Core.exec` raises a named error instead of a `NullPointerException` when no
  language matches;
- `ExtensionLoaderIntegrationTest` builds real jars (including one with a nested
  dependency archive) and covers discovery, ordering, duplicate names, and
  per-extension failure isolation;
- the loader tests no longer share a static `Core`, and the vacuous
  "does not load twice" test is replaced by one that can fail;
- CI runs `:extension:test`, `:extension:graal:test`, `:extension:graal:js:test`
  and `:extension:graal:python:test` instead of compiling three of them;
- the Graal language's process-wide engine is documented as an invariant rather
  than left as an unstated assumption.

Three deviations from the plan above are recorded in the relevant sections:

1. **Ordered dependencies are a `LinkedHashSet`, not a `List`.** Changing the
   return type would be a signature break; keeping the order inside a `Set` fixes
   the hazard now and leaves the type change for 3.0.0.
2. **A missing declared dependency warns and is skipped instead of throwing.** A
   flat classpath (a development run, or a build that puts the libraries on the
   classpath rather than embedding them) can legitimately satisfy the dependency
   without the archive existing at the declared path. Descriptor *shape* errors
   still throw. The warning names the descriptor and the path.
3. **The background pre-load was removed rather than made awaitable.** It created
   its own engine instead of the one scripts use, so it warmed only the classes
   both engines share while costing a thread and a second engine. The test harness
   now warms a language by running one no-op script through the real path, which
   replaced the fixed five-second sleep.
4. **A pre-existing test assertion was made sound.** `PyTest.test2` and
   `JsTest.test2` required one of two tasks that are queued together to append
   first, then asserted the full interleaving across a 10 ms scheduling gap. It
   failed in roughly half of six runs once the timing changed, and would have made
   the Python suite unusable in CI. The assertion now checks what the script
   guarantees: each task's own counter stays in order and the main thread resumes
   only after both finish.

## 1. How it works today

A `Core` owns one `ExtensionLoader`. Nothing loads extensions eagerly: the first
query on the loader (including one arriving from a mixin during a resource reload)
runs discovery.

Discovery builds one `ExtensionClassLoader` from two sources:

- user archives in `<config>/Extensions`, sorted by file name;
- the extension archives named by `jsmacrosce.extension.json`, which the build
  expands from `META-INF/jsmacroscedeps/*.jar` (the Graal adapters, ~29 MB today).

The second group cannot be opened by the JDK because they are jars inside the mod
jar, so `InMemoryJar` parses their central directory and `ExtensionClassLoader`
serves their entries from memory before falling back to its file path.

Providers come from `ServiceLoader`. Duplicates by extension name keep the first
one found. Each survivor loads its own dependency jars into the same loader,
`init(core)`s, and then registers libraries. Any failure is reported to
stdout/stderr and that extension is skipped.

## 2. What is worth keeping

- **The replacement of the `Extensions/tmp` extraction.** Loading from memory
  removes a writable-temp dependency, removes stale unpacked copies, and removes a
  whole class of "the old jar is still there" bugs. This is the right direction.
- **Determinism and isolation that did land.** Name-sorted local archives,
  per-provider failure containment, and the duplicate-name report are all
  improvements over a `HashSet` and a fail-fast loop.
- **Null-safe descriptor reading.** A missing descriptor now means "no
  dependencies" instead of an NPE. Small, but it makes a bare extension possible.
- **The class loader shape.** Parent-first delegation with the core's own classes
  as the contract is correct and should not change. Extension code must use the
  core's classes, not its own copy of them.
- **The tests that were added.** `InMemoryJarTest` and `ExtensionClassLoaderTest`
  are meaningful: they build real archives with Javassist and exercise the
  nested-jar chain. They are the only reason the new code can be trusted at all.
- **`LibraryRegistry`'s explicit language list.** Passing resolved language
  classes instead of writing them into a generated annotation is a correct fix for
  a real class-loader problem (a class reference in an annotation resolves against
  the loader that defined the annotated class).

## 3. Design concerns, most consequential first

Which of these the commit introduced, and which it merely inherited, matters for
deciding what to fix now:

- **Newly load-bearing in `515ef511`:** the single flat namespace (C) — the
  bundled archives only started sharing it with user archives once they moved
  in-memory; the two-namespace translation lookup and its merge (H, §4.4); the
  determinism claim that the code does not fully deliver (§4.10); and the "first
  extension with this name wins" policy, which is what makes user overrides
  impossible.
- **Inherited:** the fat interface (A), the dead version bounds (B), the live
  mutable sets (E), file-suffix matching and `int` priority (F), the two library
  registration paths (I), the missing lifecycle and structured results (J), and
  the lazy getters plus the un-restored context class loader (D). These are listed
  here because the 3.0.0 window is the cheapest chance to absorb them; fixing only
  the new code would leave the expensive parts in place.

### A. The SPI is one fat interface that also performs I/O

`Extension` currently carries identity, two version bounds, `init`,
dependency-descriptor parsing, and translation lookup
([Extension.java](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/Extension.java)).
`LanguageExtension` adds six unrelated responsibilities: selection priority, file
matching, default suffix, language construction, exception wrapping, and guest
object detection. `LibraryExtension` adds a reflection-shaped library list.

Three problems compound:

1. **Every capability change is an SPI change.** Adding, say, "this extension
   contributes a settings page" or "this extension can render a guest value"
   means editing the interface every third-party extension implements.
2. **The interface does classpath I/O in default methods.** `getDependencies()`
   and `getTranslations()` hardcode file formats, directory layouts, and
   `getClass().getResource` lookups. They also expose the parsing helpers as public
   static API (`getDependenciesInternal`, `getTranslationsInternal`), so the
   layout is now part of the contract rather than an implementation detail of the
   loader.
3. **`init(Core<?, ?>)` hands an extension the whole core.** Extensions reach
   into `core.config.addOptions(...)` and `core.libraryRegistry` directly (see
   `GraalExtension.init`). That drags every Minecraft-versioned type reachable from
   `Core` into anything that implements the interface, including the test fixtures,
   and it makes the loader impossible to test without a core.

**Why it matters later.** This is the interface that third parties compile
against, and it lives in a per-Minecraft-version module
(`common`, stonecuttered seven times) with no binary-compatibility check. Every
future capability is a choice between "edit the fat interface" and "add a
parallel mechanism". That is exactly the kind of pressure that produces a fourth
and fifth ad-hoc hook by 3.2.

**Direction.** Split into capabilities and pass a narrow context:
`ExtensionContext` (config registration, library registration, logger, extension
name, config directory) instead of `Core`; separate `LanguageProvider`,
`FileAssociation`, `LibraryProvider`, `Translations`, `GuestValueAdapter`. Keep
`init` as the only lifecycle method, on the new context. See §6.

### B. Declared compatibility is not enforced, and cannot be

`minCoreVersion()` and `maxCoreVersion()` are declared defaults
([Extension.java:45](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/Extension.java#L45),
[:55](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/Extension.java#L55))
and **nothing in the repository calls either of them**. Their defaults are the
literals `"2.0.0"` and `"2.1.0"`.

So an extension can declare a range, the loader ignores it, and the defaults
silently become wrong the moment the version moves — the 3.0.0 build will still
claim every extension is compatible with `2.0.0`–`2.1.0`. A third-party author
gets no protection from the mechanism the API advertises, which is worse than not
offering it: the declaration reads like a verified contract in generated docs.

**Why it matters later.** Once 3.0.0 ships and someone writes an extension against
it, the *next* breaking release needs a working gate. Retrofitting enforcement
after extensions exist is harder than doing it in the same window as the break.

**Direction.** Delete both. Replace with one machine-readable API level in the
manifest (an integer or a semantic version, not a `String`) and enforce it during
discovery: too old → explicit "requires API N, this build is M" result, not a
`ClassCastException` deep in a script. Enforcement must live next to the version
constant so drift is impossible.

### C. One class loader means one flat namespace across all extensions

`ExtensionClassLoader` is a single `URLClassLoader` plus one ordered list of
memory jars ([ExtensionClassLoader.java:40](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/ExtensionClassLoader.java#L40)).
Every extension and every extension dependency lands in it, and `findClass` walks
the list in insertion order and defines the first match
([:107](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/ExtensionClassLoader.java#L107)).

Consequences that will bite:

- Two extensions that need different versions of the same library (Guava, Gson,
  ASM, a Graal component) silently share one copy. The first archive wins, and the
  second extension gets version skew with no diagnostic — `findInMemoryClass`
  returns the first hit without reporting the shadowing.
- A user-dropped jar can shadow a bundled dependency simply by landing earlier in
  the loader's list, and can shadow a bundled *class* the same way. The bundled
  part of that list is itself in `HashSet` order over resource URLs (see §4.10),
  so which of two bundled archives supplies a duplicated class is arbitrary.
- The parent-first rule means a class that also exists on the mod classpath (a dev
  run, a flattened packaging mistake) is silently taken from there instead, so the
  dev path and the shipped path differ.

There is a reason for the flattening — `@Library` classes are generated by the
core's loader and need to see extension types — but that is solved by the
redirecting loader in the Graal adapter, not by sharing one namespace.

**Why it matters later.** Isolation is the difference between "extension A broke
my game" and "extension A's copy of Gson broke extension B's". It is also the
prerequisite for ever unloading or reloading an extension.

**Direction.** One child loader per extension archive, parent-first into a shared
"extension platform" loader that holds the core contract. Report shadowed class
names as a load warning. This does not have to wait for 3.0.0, but it is much
easier to do while the loader is small.

### D. Loading is triggered by getters, and it mutates the calling thread

Every query method starts with `if (notLoaded()) loadExtensions()`
([ExtensionLoader.java:164](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/ExtensionLoader.java#L164)).
There is no explicit initialization in the mod bootstrap: `clientCore` is built in
a static initializer and nothing calls `loadExtensions()`.

That means the first extension load happens wherever the first query lands —
today including `MixinTranslationStorage` during a client resource reload. That
work includes reading ~29 MB of nested jars, parsing JSON descriptors, and
running third-party `init` code, all synchronously on whatever thread asked. The
pre-load threads those `init` methods start are fire-and-forget, which is why
`BaseTest.init` has to sleep 5 seconds before it can rely on them.

Worse, the loader leaves the extension class loader installed as the current
thread's context class loader and never restores it
([ExtensionLoader.java:181](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/ExtensionLoader.java#L181)).
`JsMacrosThreadPool.PoolThread.runTask` copies the caller's context class loader
onto worker threads, so script threads inherit whichever thread happened to
trigger the load. That is silent, order-dependent, and global.

**Why it matters later.** Lazy-init-from-a-getter is unrecoverable once more
call sites depend on it, and it prevents ever returning a structured load result,
initializing before the UI enumerates languages, or scheduling the load off the
render thread.

**Direction.** Make loading explicit: `loadExtensions()` (or `load()`) called once
from the mod initializer, with results returned rather than printed. Then remove
the lazy triggers from every getter. Scope the context-class-loader change to a
try/finally around extension work, and give the thread pool an explicit parent
loader instead of inheriting.

### E. Public API hands out the loader's live mutable state

`getAllExtensions()`, `getAllLanguageExtensions()` and
`getAllLibraryExtensions()` return the *backing* sets
([:81](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/ExtensionLoader.java#L81)).
The class Javadoc states this and admits the damage ("mutating one does not
reconcile the others or registered libraries").

This already has one wrong-answer bug in it: `getHighestPriorityExtension()` caches
the winner and is never invalidated when the live set changes, so external
mutation produces a stale selection indefinitely. It is also the reason the
"independent live backing sets" caveat has to be repeated in four places — a
caveat that exists only because the internal collection was made public.

**Direction.** Return immutable snapshots (`List<Extension>`) and keep mutation
behind the loader. At 3.0.0 this is a cheap break because nothing in the tree
mutates them; the only consumer iterates.

### F. A language is identified by a file extension and an `int` priority

`LanguageExtension.extensionMatch(File)` returns a tri-state, and
`getExtensionForFile` implements a two-pass selection: if more than one extension
matches and any returned `MATCH_WITH_NAME`, only those are considered, then the
highest `int` priority wins
([ExtensionLoader.java:113](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/ExtensionLoader.java#L113)).
`MATCH_WITH_NAME` means "the file name contains the extension's name" — the
Graal adapter uses it to decide that `foo.graal.js` belongs to Graal rather than
to a hypothetical other JS provider.

The contract is under-specified and the implementation is over-clever:

- Ties have "no guaranteed selection", but `List.sort` is stable and the backing
  set is insertion-ordered, so in practice discovery order decides. The documented
  absence of a rule is false, and it invites an ordering bug later.
- `Integer.MIN_VALUE` overflows under the negation and sorts as the *highest*
  priority; this is documented rather than fixed.
- `Core.exec(String lang, ...)` synthesizes `new File("." + lang)` to reuse file
  matching ([Core.java:160](../common/src/main/java/com/jsmacrosce/jsmacros/core/Core.java#L160)) —
  so "language id" and "file suffix" are the same namespace, and a language
  cannot have an id that is not a plausible suffix.
- Selecting a language costs a full match probe per language on every script run
  and every editor autocomplete request; the Graal probe touches
  `Source.findLanguage` and the engine.

**Why it matters later.** Every future language (Lua, WASM, a typed JS variant)
adds a provider whose conflicts must be reasoned about in this tri-state. The
second provider for an existing suffix is where this becomes unmaintainable.

**Direction.** Declare file associations (`Set<String> suffixes` plus optional
patterns) and a stable `String` language id, with a documented conflict policy
(explicit user preference, then declaration order, then error). Cache the
association table instead of re-probing on every call.

### G. `InMemoryJar` is a hand-rolled ZIP reader with no validation and no bounds

`InMemoryJar` replaces 20 lines of `ZipInputStream` with a 176-line parser. It is
competently written for the happy path, but it trusts the central directory
completely and has no integrity story:

- no CRC check and no bounds check on declared sizes;
- no cap on inflated output, so a small crafted archive can exhaust the heap
  during class loading (`uncompressedSize` is only used as an initial capacity,
  [:130](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/InMemoryJar.java#L130));
- the local header's file name is never compared to the name being read, so a
  crafted archive can serve one entry's bytes under another entry's name;
- ZIP64 and encryption are unsupported (documented), and unknown methods throw
  (good).

This is a load-bearing security boundary in one direction only: extension jars are
user-installed, so this is not remote code execution, but "users install
extensions" is exactly the population that will install a repackaged jar.

**Direction.** Prefer a correctly-scoped library over a bespoke parser. Two
options that keep the no-disk property: `ZipInputStream` over the in-memory bytes
for streaming reads (correct, minimal, no random access), or `FileSystems`/`ZipFile`
against a single temp file per bundled archive if random access is required. If the
parser stays, add a declared-size bound, a CRC check, and a name comparison; a
malformed archive must fail loudly rather than silently truncate (see §4.1).

#### The reader is a stopgap, not a destination

`InMemoryJar` exists only because the JDK cannot open a jar that is nested inside a
jar. That is a one-call problem, and the reader pays for solving it by hand:

- 255 lines of format parsing in a scripting mod, every line of it a place where a
  future bug can hide;
- a private `jsmacros-extension:` URL scheme instead of `jar:`, so
  `getProtectionDomain().getCodeSource()`, `JarURLConnection`, and libraries that
  inspect their own location see something they do not expect;
- a permanent heap copy of the compressed archives — about 28 MB for the current
  bundled set (21.2 MB + 6.8 MB).

The options were measured rather than guessed:

| Option | New dependency | Disk | Lines we own | Handles for us |
| --- | --- | --- | --- | --- |
| JDK `ZipFile` over a digest-named cache file | none | ~28 MB, written once | ~40 | ZIP64, charset flags, encryption errors, real `jar:` URLs and `CodeSource` |
| `commons-compress` `ZipFile` over `SeekableInMemoryByteChannel` | 1.06 MiB, Apache-2.0 | none | ~60 | ZIP64, charset flags, encryption detection; CRC still ours |
| Keep `InMemoryJar` | none | none | 255 | nothing |

An in-memory `SeekableByteChannel` is a real option: `commons-compress` ships
`SeekableInMemoryByteChannel`, its `byte[]` constructor stores the array without
copying, and `new ZipFile(channel)` is public API. `ZipFile.getEntry` plus
`getInputStream` replace both the central-directory parser and the inflater. Note
that `ZipFile` does not verify CRC-32 on read (only its streaming sibling does), so
the checksum comparison stays in our ~40–60 lines.

**Recommendation: the cache-file option, taken together with the 3.0.0 per-archive
class loaders.** Extracting the bytes is a JDK `ZipFile` read of the outer jar;
naming the cache entry by the archive's SHA-256 makes it write-once and lets a
version change replace it cleanly. It also removes the dev/shipped divergence: a
development run already has the archives on disk (`build/resources/main/META-INF/
jsmacroscedeps/`), so it can add them directly and only a packaged mod needs the
extraction. It is the only option here that makes `jar:` URLs real again, which is
what the per-archive loaders want anyway.

The old `<config>/Extensions/tmp` was bad because it was extracted on every launch,
never cleaned, had no identity, and sat where users expect their own extensions.
A digest-named cache under a directory the mod owns fixes all four; the reason to
avoid disk at all is not the disk, it is the ownership of the format.

The in-memory option is preferable only if "never write outside the mod jar" is a
hard rule. It then needs `commons-compress` relocated into the mod's own package: a
second mod bundling a different version would collide in the shared mod class
loader, and neither `jarJar` nor Loom's `include` relocates by itself.

Until either option lands, the checks added in this pass are the minimum bar. If
the reader stays another release, close these two gaps first:

- the general-purpose encryption bit is not checked, so an encrypted entry is
  reported as corrupt rather than as encrypted;
- entry names are always decoded as UTF-8, so an archive written with the legacy
  code page yields mangled names, which surfaces as a service file that cannot be
  found.

### H. Conventions are overloaded strings, and they disagree with each other

One feature currently spans four independent naming conventions:

| Concern | Current convention |
| --- | --- |
| Dependency descriptor | `jsmacrosce.ext.<extensionName>.json` (derived from the name) |
| Dependency directory | `META-INF/jsmacroscedeps/` (array form) |
| Legacy dependency directory | `META-INF/jsmacrosdeps/` (primitive form) |
| Translation directory | `assets/jsmacros/<name>/lang/` *or* `assets/jsmacrosce/<name>/lang/` |
| Bundled-archive manifest | `jsmacrosce.extension.json`, a JSON template containing `${dependencies}` |

Four of those are legacy or inconsistent:

- The primitive dependency form prefixes `META-INF/jsmacrosdeps/`
  ([Extension.java:116](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/Extension.java#L116))
  while every build script writes `META-INF/jsmacroscedeps/`. The primitive form
  can therefore never address the artifacts the build actually produces.
- The `${dependencies}` special case exists only in the primitive branch. The array
  branch — the one the build uses — does not special-case it, which is why the test
  fixture logs `Could not find dependency: ${dependencies}` on every run.
- `common/src/main/resources/jsmacrosce.extension.json` contains
  `"dependencies": [${dependencies}]`, which is **not valid JSON** until a
  `processResources` expansion rewrites it. Only the loader modules expand it. Any
  future packaging path that copies the common resources without expanding turns a
  build template into a fatal `RuntimeException` during extension loading.
- Two translation namespaces are searched in order, with the legacy one first.
  That ordering means a legacy namespace wins over the current one when both are
  present.

**Why it matters later.** Every one of these is a compatibility decision that is
cheap only while the major version is open. They are also the first thing a new
extension author has to learn and will get wrong, because none of it is written
down for third parties — `extension/README.md` is four lines saying "make a folder
here".

**Direction.** One manifest format, declared in the extension jar, validated with a
schema and a clear error message, with no text substitution and no name-derived
file paths. Generate it as a proper resource at build time. Drop the second asset
namespace at 3.0.0.

### I. Library registration has two paths and a weak collision policy

There are two ways a library enters `LibraryRegistry`: a class with `@Library`
via `LibraryExtension.getLibraries()`, and a generated class via
`LibraryBuilder.finishBuildAndFreeze()` which passes its language list
out-of-band. The annotation's own `languages()` element is now half-dead: generated
libraries do not populate it, and the lookup for annotated libraries compares
language classes with `equals` while the per-language maps use
`isAssignableFrom`
([LibraryRegistry.java:86](../common/src/main/java/com/jsmacrosce/jsmacros/core/library/LibraryRegistry.java#L86)
vs
[:127](../common/src/main/java/com/jsmacrosce/jsmacros/core/library/LibraryRegistry.java#L127)).
So `@Library(languages = {BaseLanguage.class})` matches nothing, while the
equivalent registration through the builder matches everything.

Collisions are worse than undocumented: `globalOwners` warns on a duplicate global
name but keeps the *first* owner, while the actual map `put` lets the *last*
registration win — and the warning text says "the last one registered is used".
Registration order is discovery order, which is parent-classpath, then memory jars,
then name-sorted user jars. A user extension can therefore silently replace a
core library's global, or another extension's, and the message that explains it
says the opposite of what happens.

**Direction.** One registration path (builders), one language-matching rule
(assignable), and an explicit precedence: first registration wins outright with a
recorded conflict surfaced to the user, or a declared override with a warning.
Delete `Library.languages()` in favour of the explicit list.

### J. There is no lifecycle, no ownership, and no structured result

- **No shutdown.** There is no `close()`/`shutdown()` on `Extension` and no close
  on the loader, so extension engines, threads, and libraries live for the process.
  `loadExtensions` closes the class loader only on the failure path. Nothing can
  ever reload, and `highestPriorityExtension` caches across any change.
  (`JsExtension` and `PythonExtension` each start a bare `Thread` and never join
  it; `GraalLanguageDefinition.engine` is a static singleton.)
- **No config namespace.** An extension calls `config.addOptions("graal", ...)`
  with a hand-picked key. A second extension choosing the same key fails its
  `init` with `IllegalStateException`, which the loader reports as a generic
  "failed to initialize" and skips. The key should be derived from or validated
  against the extension name.
- **No result object.** `loadExtensions()` returns `void` and reports through
  `System.out`/`System.err`, including stack traces for expected third-party
  failures. A user with a broken extension gets "Loaded 3 extensions" and nothing
  queryable. `loadingDone` becomes `true` even when every extension failed, so
  there is no way to tell "no extensions installed" from "all extensions broke".
- **Partial state is left behind.** `initialize()` registers libraries before
  adding the extension to the registry, so a `getLibraries()` that throws halfway
  leaves libraries registered for an extension the loader considers absent. There
  is no rollback and no record.
- **Re-entrancy misbehaves.** `loadExtensions()` is `synchronized` but reentrant on
  the same thread, so an extension that queries the loader from its own `init` gets
  "Extensions already loaded" printed and a partially populated set.

**Direction.** `AutoCloseable` loader plus `Extension.close()`; a
`LoadResult`/`ExtensionStatus` per extension (loaded / skipped / failed with
cause) returned from an explicit `load()`; registration ordered so nothing is
observable until the extension is fully registered; one namespaced config entry per
extension.

### K. The tests do not exercise the shipped path, and CI does not run half of them

`ExtensionLoaderTest` looks like integration coverage but is not: the providers
come from `src/test/resources/META-INF/services/`, which is on the *parent* test
classpath, so `ServiceLoader` finds them through parent delegation and
`addMemoryJar` is never used. The nested-jar path is only covered a level lower in
`ExtensionClassLoaderTest`, which builds archives by hand.

Two consequences:

- The "dedupe by name" behavior has no test at all.
- `doesNotLoadTheSameExtensionTwice` asserts nothing. `CoreInstanceCreator` caches
  one static `Core`, so the second `loadExtensions()` hits the
  `classLoader != null` early return and prints "Extensions already loaded" — which
  is exactly what the test output shows, three times. It verifies the no-op guard,
  not deduplication.

CI compiles the extension modules, then runs `:extension:test` and
`:extension:graal:js:test` only. `:extension:graal:test` (`ThrowableFormatterTest`,
`GuestExceptionSimplifierTest`, `ChromeInspectorPatchTest`) and
`:extension:graal:python:test` (`PyTest`, `StopScriptTest`, `GraalScriptTypesTest`)
are compiled but never executed in CI.

**Direction.** Add one integration test that builds a real outer jar containing a
nested extension jar plus its dependency jar, points the loader at it, and asserts
discovered names, order, duplicate handling, and isolation of an `init` failure.
Give each test its own `Core` (the singleton makes test order significant). Run all
extension test tasks in CI.

## 4. Defects found along the way

These are bugs, listed separately from the design concerns.

1. **Fixed: truncated deflate entries returned partial bytes silently.**
   `InMemoryJar.read` breaks out of the inflate loop when the inflater needs input
   and returns whatever it has, without checking `inflater.finished()`
   ([:135](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/InMemoryJar.java#L135)).
   A truncated class file surfaces later as a confusing `ClassFormatError`; a
   truncated translation file just loses entries.

2. **Fixed: stored entries copied the wrong length.** The stored branch allocates
   `uncompressedSize` and then `System.arraycopy(..., out.length)`
   ([:121](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/InMemoryJar.java#L121)).
   If a malformed archive declares an uncompressed size larger than the stored
   data, this reads past the entry (into the next entry, or out of bounds near the
   end of the file). It should copy `compressedSize` and validate that the two
   agree for stored entries.

3. **Fixed: no inflate bound.** A crafted or corrupt archive can inflate without limit
   and exhaust the heap at class-load time. Related: `findClass` swallows
   `IOException` and treats a damaged entry as "not in this jar"
   ([ExtensionClassLoader.java:113](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/ExtensionClassLoader.java#L113)),
   so corruption degrades to "class not found in an unexpected loader".

4. **Fixed: extension translation precedence was nondeterministic.**
   `MixinTranslationStorage` collects every extension's map into a
   `Collectors.toSet()` and then merges that set into a `LinkedHashMap`
   ([MixinTranslationStorage.java:28](../common/src/main/java/com/jsmacrosce/jsmacros/client/mixin/access/MixinTranslationStorage.java#L28)).
   The set destroys the loader's stable order, so when two extensions define the
   same key the winner depends on `HashMap` iteration. The `putIfAbsent` afterwards
   also means an extension can never override a key the resource packs already
   defined, which is a defensible rule but is not documented anywhere. This call also triggers lazy extension loading
   during a resource reload.

5. **Fixed: `Core.exec` threw `NullPointerException` when no language matches.**
   `exec(String lang, ...)` relies on `assert l != null`
   ([Core.java:161](../common/src/main/java/com/jsmacrosce/jsmacros/core/Core.java#L161)),
   which is disabled by default; `exec(ScriptTrigger, ...)` dereferences the
   fallback without a null check. Both should raise a clear "no language extension
   matched" error.

6. **Documented, not fixed: `GraalExtension` and `GraalLanguageDefinition` share static
   singletons that ignore the `Core` argument.** `GraalExtension.getLanguage` is documented as
   "creation is unsynchronized, so concurrent calls can construct competing
   definitions", and the cached definition keeps the *first* core it saw. A second
   `Core` silently gets a language bound to the first one's profile and
   libraries. The invariant is now stated on `GraalExtension`; making the language
   per-core belongs to the 3.0.0 language contract.

7. **Fixed: the `ExtensionClassLoader.addURL` override documented behavior it did not
   implement.** The body is `super.addURL(url)`
   ([:56](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/ExtensionClassLoader.java#L56));
   the Javadoc claims null, duplicate, and post-close additions are ignored. The
   override exists only to attach a contract that then drifts. Delete the override
   (the method is still inherited) or implement the filtering.

8. **The `Library` collision warning states the opposite of what happens.**
   Message: "the last one registered is used"
   ([LibraryRegistry.java:213](../common/src/main/java/com/jsmacrosce/jsmacros/core/library/LibraryRegistry.java#L213));
   `globalOwners` keeps the first owner. Both facts can be true simultaneously for
   different maps, which is precisely why the message is misleading.

9. **Fixed: the test fixture left a spurious error in every run.** The test
   `jsmacrosce.extension.json` uses the array form with a literal
   `${dependencies}`, so every `ExtensionLoaderTest` run logs
   `[JsMacrosExtensionManager] Could not find dependency: ${dependencies}`.

10. **Partly fixed: archive ordering was not what the documentation said, and the
    bundled set was not ordered at all.** `loadExtensions`'s Javadoc says "archives are visited in
    name order", but only *local* archives are name-sorted
    ([ExtensionLoader.java:206](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/ExtensionLoader.java#L206)).
    `getDependenciesInternal` returns a plain `HashSet<URL>`
    ([Extension.java:126](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/Extension.java#L126)),
    and both the bundled archives and each extension's dependencies are added to
    the loader by iterating it
    ([ExtensionLoader.java:243](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/ExtensionLoader.java#L243),
    [:346](../common/src/main/java/com/jsmacrosce/jsmacros/core/extensions/ExtensionLoader.java#L346)).
    Because the loader defines a class from the first archive that has it, that
    order matters — and it is `HashSet` iteration order over resource URLs: stable
    between runs, but unrelated to the descriptor, the file name, or any declared
    intent.
    On top of that, `ServiceLoader` sees the parent classpath first, then memory
    jars, then local files, so the effective precedence is parent → bundled → user.
    That is a defensible policy, but it is not the documented one, and it means
    **a user can never override a bundled extension by dropping a newer jar** —
    their jar is silently ignored with one stdout line. The dependency set is now a
    `LinkedHashSet`, so both bundled archives and per-extension dependencies are
    added in declaration order, and the loader's Javadoc describes the real
    precedence (parent, bundled, user). The stdout diagnostics remain a §J issue.

## 5. The 3.0.0 removal plan

The point of a major version is to spend the breaking window on the things that
are expensive to change later. Below, "Now" means nothing in this repository
depends on it and the blast radius on third parties is either zero or named in the
row; "3.0.0" means it is a compatibility decision that needs the major.

### 5.1 Drop now (implemented in this pass)

| Remove | Where | Why it was safe now | Replacement |
| --- | --- | --- | --- |
| `minCoreVersion()` / `maxCoreVersion()` | `Extension` | No caller anywhere in the tree. Implementors overriding them are unaffected. | Nothing until the manifest field lands; do not ship a second unused declaration. |
| `ExtensionClassLoader.addURL` override | `ExtensionClassLoader` | The method is inherited; the body adds nothing and the Javadoc was wrong. | Deleted; callers use the inherited method. |
| Primitive `META-INF/jsmacrosdeps/` dependency form and the `${dependencies}` literal special case | `Extension.getDependenciesInternal` | No build here emits it; every one writes the array form into `jsmacroscedeps`. | Array-only descriptors, validated on read. Ordered as a `LinkedHashSet` for now; the type change to `List` is a 3.0.0 item. |
| Spurious test fixture `${dependencies}` | `extension/src/test/resources/jsmacrosce.extension.json` | Test-only; removes noise from every run. | `{"dependencies": []}`. |
| `BaseTest`'s 5-second sleep | `extension/src/test` | It waits for the fire-and-forget engine pre-load that `init` starts; nothing can await it. | Remove the pre-load, or make it explicitly awaitable. |

### 5.2 Drop at 3.0.0

| Remove | Why it must go | Replacement |
| --- | --- | --- |
| The `jsmacros` translation namespace fallback | Two namespaces with legacy-first ordering is a permanent ambiguity, and a 3.0.0 consumer base is the only clean reset. | One namespace, declared in the manifest; missing keys fall back to the core only. |
| Live mutable sets from the loader | It is why the "mutating one does not reconcile the others" caveat exists, and why the priority cache can go stale. | Immutable `List` snapshots. |
| `LanguageExtension.getPriority()` + `ExtMatch` tri-state | Unstable tie-breaks, integer negation overflow, and portfolio-wide reasoning for every new language. | Declared file associations + stable language id + documented conflict policy. |
| `LanguageExtension.isGuestObject(Object)` | A boolean type test cannot express "render this guest value as text/JSON", and Graal's `instanceof Value` answers true for other languages' values. | `GuestValueAdapter` (recognize / display / serialize), or an optional capability interface. |
| `LibraryExtension.getLibraries()` returning annotation classes | Second registration path, reflection-shaped, half-dead `languages()` annotation element. | Builder-based registration only; explicit language lists; one matching rule. |
| `Library.languages()` | Exact-class matching for annotated libraries vs assignable matching for builder libraries; generated libraries no longer populate it. | Explicit registration list; delete the element. |
| Descriptor/translation I/O in `Extension` default methods | Layout is baked into the ABI and the parsing helpers are public API. | Loader-owned manifest reader; extensions only answer `getExtensionName()`. |
| Lazy `loadExtensions()` from every getter | Hidden startup cost, render-thread I/O, TCCL leakage, no structured result. | Explicit `load()` at bootstrap returning `List<ExtensionStatus>`. |
| `init(Core<?, ?>)` | Drags the whole versioned core into the SPI and makes the loader untestable without Minecraft. | `init(ExtensionContext)`. |
| `${dependencies}` text-substituted JSON | Invalid JSON in the source tree; a single unexpanded copy breaks loading for everyone. | Generated manifest resource, no text substitution. |
| The whole SPI living in `common` | Seven stonecuttered copies of the contract with no binary-compatibility gate. | Standalone `api` module (see §6), published and versioned independently. |
| `getDependencies()` / `getDependenciesInternal` returning `Set<URL>` | Order matters to the loader but the type says it does not; `LinkedHashSet` fixes the behavior but not the signature. | `List<URL>`, or a manifest object that also carries the descriptor's identity. |

### 5.3 Keep, but fix before the window closes

- Per-extension class-loader isolation (§C) — not an API break, but much cheaper
  while the loader is small.
- Validation and bounds in the archive reader (§G, §4.1–4.3) — these are correctness
  fixes, not compatibility decisions.
- Structured load results and a `close()` lifecycle (§J) — additive once the
  explicit `load()` exists, which is the 3.0.0 break.
- Deterministic translation merge and per-extension config namespacing (§H, §I) —
  the merge is done; the config namespace is still open.

## 6. Target shape

The single highest-value structural change, and the one that makes the rest easy:

> **Move the extension SPI out of `common` into a standalone `api` module that has
> no Minecraft dependency and is published as its own artifact.**

That one move:

- gives third parties an artifact to compile against without a Minecraft toolchain
  or a Stonecutter target;
- makes binary compatibility checkable in CI (`japicmp` or equivalent) against the
  last release, so "is this breaking?" stops being a judgement call;
- removes seven copies of the same interface and the "compile against 1.21.5"
  workaround;
- makes the loader testable as plain Java, which is what the current tests are
  already pretending to do.

On top of that, the shape to aim for:

```text
ExtensionContext        // narrow: name, config registration, library registration, logger, paths
Extension               // identity + init(ctx) + close()          (only required capability)
LanguageProvider        // language id, associations, language factory, exception wrapping
LibraryProvider         // library classes (builders)
Translations            // optional: map for a language
GuestValueAdapter       // optional: recognize / display / serialize

ExtensionLoader.load()  // explicit, returns List<ExtensionStatus>, no lazy triggers
ExtensionClassLoader    // one child loader per archive, parent-first
```

Rules that keep it from re-accreting:

1. **Capabilities over one interface.** An extension implements only what it
   provides; the loader discovers capabilities by `instanceof`, not by
   `default`-method no-ops.
2. **No I/O in the SPI.** The loader owns formats, paths, and parsing. An
   extension answers questions; it does not read resources on the loader's behalf.
3. **Nothing silent.** Every skip, shadow, conflict, and failure becomes a typed
   status entry, surfaced in the log *and* the UI.
4. **One manifest, one schema, validated.** No name-derived paths, no text
   substitution, one namespace.
5. **Explicit lifecycle.** Load once at a known point; close on shutdown;
   everything created by an extension is owned by that extension.

## 7. Suggested sequencing

1. **Now, before 3.0.0 — done in this pass:** §5.1 deletions, §4.1–4.5, the
   nested-jar integration test, all extension suites in CI, and the Graal
   singleton invariant. What remains in this phase is small and independent:
   - correct the `Library` collision message and pick a real precedence rule
     (§4.8, §I);
   - per-archive class loaders with shadow reporting (§C), which is cheaper now
     than after 3.0.0;
   - decide whether the hand-rolled archive reader stays; if it does, keep the
     checks added here and add the ZIP64/encryption rejection tests.
2. **In the 3.0.0 window (one PR series, in this order):** extract `api` module
   and add a binary-compatibility gate → explicit `load()` + `ExtensionContext` →
   immutable snapshots → split `LanguageProvider` (associations, guest values) →
   split `LibraryProvider` and remove `Library.languages()` → one manifest and one
   namespace → drop the legacy translation namespace.
3. **After 3.0.0, no API break needed:** per-archive class loaders and shadow
   reporting; archive-reader hardening or replacement; `close()` plumbing;
   translation caching; settings-page capability.

Doing step 2 in a single window matters more than doing it in one release: these
changes are individually small but collectively they are the difference between an
SPI that survives the next three years and one that grows a second and third
"internal" interface beside it.

## 8. Deliberately not changed

- **Parent-first delegation** with the core's classes as the contract. Correct;
  extension code must see the core's types.
- **`ServiceLoader` for discovery.** It is the standard mechanism and it works;
  the complaints above are about *when* and *in what order* it is used, not about
  using it.
- **The nested-jar requirement itself.** Shipping the Graal adapters inside the mod
  jar is a legitimate packaging choice; the problem is the reader's robustness and
  the loader's flat namespace, not the nesting.
- **The library lifetime model** (`BaseLibrary`, per-exec, per-language,
  per-exec-language). It is a reasonable shape for this problem; only the
  registration path and the language-matching rule need to converge.
