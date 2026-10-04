# Documentation generation

JsMacrosCE uses the JDK doclet API to read Java declarations and Javadoc. Markdown,
TypeScript declarations and Python stubs share a model; the legacy HTML renderer
remains supported. VitePress is the primary website.

## Minecraft targets are separate snapshots

Every invocation of a doclet receives exactly one common project's generated
Stonecutter source view and its compile classpath and Java toolchain. Extension
sources and their external libraries are included, but extension dependencies on
the active common project's binary JAR are not. Documentation generation does not
switch `stonecutter.active` or rewrite the shared source state.

The mod version and Minecraft version are independent:

```text
build/docs/targets/1.21.8/
  python/JsMacrosAC/
  typescript/headers/
  web/2.0.0/
  vitepress/content/2.0.0/1.21.8/

build/docs/targets/26.1.2/
  ...

build/docs/vitepress/content/2.0.0/
  1.21.8/
  26.1.2/
```

The combined site has a version selector labeled with the current reference's
Minecraft version (just the version number), and a separate sidebar for
each snapshot. It discovers only generated targets, not an assumed list of pages.
Outside an API reference, the selector shows the latest generated Minecraft version.
Class pages and snapshot overviews identify their Minecraft target.

Indexes and sidebars use short names and nest supporting types under their owners;
fully qualified Java names remain on individual pages. A nested type in a library
is not itself a global library: only the enclosing named `@Library` object gets a
global-access notice. Supporting types retain their own pages and constructors,
and existing URLs are preserved. An otherwise unclassified index has no extra
"Uncategorized" heading; mixed indexes use an "Other classes/libraries/events"
section instead.

Stonecutter decides which declarations exist and which names/signatures apply.
The doclet does not interpret Stonecutter comments a second time. A method that
exists as a compatibility stub is still documented on that target: generation
cannot infer useful behavior from method presence. Its Javadoc must explain
version-dependent behavior. `@since` continues to mean the JsMacros version, not
the Minecraft version.

## Commands

Run from the repository root. The familiar root tasks select the distribution's
default Minecraft targets (currently 26.1.2 locally, all supported targets in CI).
An explicit property overrides documentation selection, without changing mod
build targets:

```sh
# A single target, regardless of the active IDE version.
./gradlew -PdocsMinecraftVersion=26.1.2 generateVitepressDoc

# Assemble one site containing multiple accurate API snapshots.
./gradlew -PdocsMinecraftVersions=1.21.8,26.1.2 copyVitepressDoc

# All supported targets, all documentation formats and their support files.
./gradlew -PdocsMinecraftVersions=all copyVitepressDoc copyWebDoc copyTSDoc copyPyDoc

# Direct target-qualified generation is also available.
./gradlew :common:26.1.2:generateVitepressDoc
# Or a root alias/all-formats convenience task:
./gradlew generateVitepressDoc2612 generateDocs1218
```

`generateDocsAll` generates all four formats for all supported Minecraft targets;
it does not assemble the combined site. Use `copyVitepressDoc` with the desired
selection to do that. The singular and plural properties are mutually exclusive;
unknown or empty targets fail rather than falling back to another version.

To build the generated site:

```sh
cd build/docs/vitepress
pnpm install --frozen-lockfile
pnpm test
pnpm build
```

The website build allows Node a 12 GiB heap, bounds VitePress build concurrency
to four pages instead of its default 64, and disables the Markdown render cache.
This is a heap ceiling, not a reservation; use a suitably sized machine when
building every target together. Minification and local search remain enabled.
A measured five-target build used about 10.7 GiB peak resident memory. On smaller
machines, generate and build a selected subset of targets instead.

`copyVitepressDoc` synchronizes only the generated site directory; source files in
`docs/vitepress` are inputs and are never rewritten. Installed site dependencies
and VitePress caches are preserved. A doclet regeneration clears only its own
isolated generated destination so removed classes cannot survive as stale pages.
Do not edit generated files as a way to fix documentation.

Devkits consume the matching `build/docs/targets/<Minecraft>` tree, not one shared
typing/HTML tree for every version. Legacy HTML can be served independently from
each target's `web` directory, with its existing mod-version paths and assets.

## Minecraft class links

Both website renderers use `ExternalTypeLinks`:

- Pre-26.1: `https://mappings.dev/<Minecraft>/<binary-class-path>.html`
- 26.1+: `https://mcsrc.dev/2/<Minecraft>/<binary-class-path>`

These are class-level links. No guessed member anchors or decompiler line numbers
are emitted. Nested Minecraft classes use `$`; external Javadoc uses its dotted
nested-class filenames. Configured external Javadoc takes precedence. Only
Minecraft's own `net.minecraft`, Blaze3D, math and Realms packages get the game
fallback; Authlib, Brigadier and DataFixerUpper are separate libraries.
JDK Javadoc is selected using the target's Java toolchain version, and external
`element-list` module prefixes are preserved rather than using legacy framed URLs.

The provider rule currently accepts release versions. An unknown/snapshot version
does not acquire a guessed Minecraft URL. New supported targets should verify
their provider and class URLs before publication.

## Regression checks

```sh
./gradlew :buildSrc:testExternalTypeLinks :buildSrc:testDocletLinks \
  :buildSrc:testDocBodyRenderer :buildSrc:testTsRenderer :buildSrc:testMarkdownWriter
node --test docs/vitepress/.vitepress/api-snapshots.test.ts
```

The offline doclet fixture compiles synthetic external types and actually runs
all four doclets for an older and an unobfuscated target. It verifies constructor
links, nested class names, external-library exclusions, and literal prose/example
preservation in the generated `.d.ts` and `.py`, not just whether those files parse.

For real-source validation, generate both sides of a relevant version boundary,
inspect the generated Javadoc options for source/classpath isolation, and compare
the actual output to the target's generated source. A green build alone does not
prove that examples, comments or Python docstrings survived correctly.

Future cross-target availability badges should be derived from these independent
snapshots. Do not union their Java sources/classpaths or assign Minecraft meaning
to JsMacros `@since` tags.
