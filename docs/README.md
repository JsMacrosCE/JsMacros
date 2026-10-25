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
Minecraft version (just the version number). It discovers only generated targets,
not an assumed list of pages. Outside an API reference, the selector shows the
latest generated Minecraft version. A snapshot overview states its Minecraft
target in the page; every page of a reference states it in the selector.

Indexes and sidebars use short names and nest supporting types under their owners;
fully qualified Java names remain on individual pages. A nested type in a library
is not itself a global library: only the enclosing named `@Library` object gets a
global-access notice. Supporting types retain their own pages and constructors,
and existing URLs are preserved. An otherwise unclassified index has no extra
"Uncategorized" heading; mixed indexes use an "Other classes/libraries/events"
section instead.

A member page shows the branch of the tree it belongs to, not the whole tree:
the group it is in, the section, and the types that hold it, plus links to the
other groups. The group pages list every member, so the full tree is one click
away. A search answers for the snapshot the reader is on: the index holds every
snapshot, but results from other Minecraft versions and other mod versions are
dropped.

The sidebar is browser-only. API prose, signatures, examples and index-page
links are still server-rendered; visitors without JavaScript and HTTP-only agents
do not see sidebar links. The browser fetches one hashed JSON navigation asset
for the selected snapshot, indexes its routes, and reuses successful loads across
page changes and version switches. Loading reserves the normal sidebar column;
a failed request offers Retry and a link to the server-rendered API index.
The default theme still supplies the sidebar appearance and mobile controls.

Stonecutter decides which declarations exist and which names/signatures apply.
The doclet does not interpret Stonecutter comments a second time. A method that
exists as a compatibility stub is still documented on that target: generation
cannot infer useful behavior from method presence. Its Javadoc must explain
version-dependent behavior. A member that only makes sense on some of the
targets can instead be guarded out in the shared source, and is then documented
only on the targets that have it, so its Javadoc does not have to describe the
others. `@since` continues to mean the JsMacros version, not the Minecraft
version.

## Commands

Run from the repository root. The familiar root tasks select the distribution's
default Minecraft targets (latest locally, all supported targets in CI).
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

`./gradlew buildVitepressSite` installs the site dependencies and builds the
static site. To run the steps directly:

```sh
cd build/docs/vitepress
npm ci
npm test
npm run build
```

For a live preview while editing the site, regenerate and copy the content, then
start the dev server in the generated directory:

```sh
./gradlew copyVitepressDoc
cd build/docs/vitepress
npm ci
npm run dev
```

The website build allows Node a 12 GiB heap, bounds VitePress build concurrency
to four pages instead of its default 64, and disables the Markdown render cache.
This is a heap ceiling, not a reservation; use a suitably sized machine when
building every target together. Minification and local search remain enabled.
Before the browser-only sidebar, a measured five-target build used about 10.8 GiB
peak resident memory and took about seven minutes; that is not a current
performance estimate. On smaller machines, generate and build a selected subset
of targets instead.

`copyVitepressDoc` synchronizes only the generated site directory; source files in
`docs/vitepress` are inputs and are never rewritten. Installed site dependencies
and VitePress caches are preserved. A doclet regeneration clears only its own
isolated generated destination so removed classes cannot survive as stale pages.
Do not edit generated files as a way to fix documentation.

The combined devkit consumes the matching `build/docs/targets/<Minecraft>` trees,
not one shared typing/HTML tree for every version. Legacy HTML can be served
independently from each target's `web` directory, with its existing mod-version
paths and assets.

## Distribution packaging

`createDist` produces one mod JAR per Minecraft/loader pair, one shared GraalPython
JAR, and one `jsmacrosce-devkit-<release>.zip`. A seven-target release therefore
publishes 16 assets. GitHub receives explicit task outputs, not a glob of old and
new files in `dist`. Per-version build nodes delegate shared packaging to the
controller; they do not register another publisher or extension/devkit copies.

The devkit ZIP contains:

```text
README.md
serve-docs.py
1.21.5/{typescript,python,web}/
1.21.8/{typescript,python,web}/
...
vitepress/{index.html,assets,...}
```

The Minecraft folders contain independent API snapshots. The `vitepress/`
directory contains one **rendered**, multi-target website, not a Node project or
one copy per Minecraft version. After extraction, `python3 serve-docs.py` serves
it on <http://127.0.0.1:8000/> with clean-URL support and local search. Users do
not need to install Node or rebuild the site.
Sidebar JSON assets are included in `vitepress/assets/`; they are fetched from
this local server, so browser navigation works offline too. Opening the HTML
directly with `file://` is not supported.

`packageDevkit` builds only the devkit and its documentation dependencies; it
does not compile mod JARs or package Python. It follows the documentation target
selection above (latest locally, all in CI, or an explicit override). A complete
distribution must include documentation for every packaged mod target.
`verifyDistribution` checks the release asset count, required devkit snapshots,
rendered site and shared extension bytecode; publishing depends on these checks.

`createDistSite` also copies the rendered site to `dist/vitepress/`, and
`createDistDocs` copies the per-target trees to `dist/docs/<Minecraft>/` for CI
consumers. `buildVitepressSite` runs `npm ci` and `npm run build` in
`build/docs/vitepress`. All release workflows install Node for this reason.
Skipping `createDistSite` alone no longer skips the site build, because the
devkit itself includes the site. For a documentation-only local check, select
one target and run `packageDevkit`; the complete site can be large (see below).

Distribution tasks recreate their output directory. Use
`-PdistDirectory=build/packaging-check` to validate without replacing `dist/`.
Only `dist/` or a subdirectory of `build/` is accepted, to protect source trees
from the distribution cleanup.

The shared language adapters target Java 21 and compile against the 1.21.5 core,
independently of `stonecutter.active`. Graal infrastructure and JavaScript remain
embedded in every mod JAR. Optional Python is published once as
`jsmacrosce-ext-graalpy-<release>.jar`, installed in `config/jsMacros/Extensions/`
(capital `E`). There are no duplicate extension ZIPs. Future extensions that use
version-specific Minecraft or loader APIs need their own target builds; they
must not be added to the shared artifact list merely by renaming a JAR.

## Site size

Navigation is shared rather than repeated on every generated page:

- `metaChunk` writes the site data and the page hash map into one shared chunk.
  Inlined, they were repeated on every page and grew with the page count, which
  made a five-target site larger than any static host accepts.
- Global site data contains only a small snapshot manifest, not the expanded
  per-page sidebar table. This also avoids the default theme traversing every
  snapshot's navigation during each page's server render.
- Snapshot trees are separate static JSON assets loaded on demand in the browser.
  A member page constructs only its owner branch; overview and group pages show
  the full relevant trees. No sidebar links are duplicated in server HTML.

## Minecraft class links

Both website renderers use `ExternalTypeLinks`:

- Pre-26.1: `https://mappings.dev/<Minecraft>/<binary-class-path>.html`
- 26.1+: `https://mcsrc.dev/2/<Minecraft>/<binary-class-path>`

These are class-level links. No guessed member anchors or decompiler line numbers
are emitted. Nested Minecraft classes use `$`; external Javadoc uses its dotted
nested-class filenames. Configured external Javadoc takes precedence. Only
Minecraft's own `net.minecraft`, Blaze3D, RenderPearl, math and Realms packages get the game
fallback; Authlib, Brigadier and DataFixerUpper are separate libraries.
JDK Javadoc is selected using the target's Java toolchain version, and external
`element-list` module prefixes are preserved rather than using legacy framed URLs.

Configured external Javadoc indexes are fetched while generating with bounded
timeouts and retries. Missing, malformed and permanent HTTP-error indexes fall
back immediately from `element-list` to `package-list`; timeouts, HTTP 408/429 and
server errors retain bounded retries. If neither format provides a valid index,
generation fails and names the URL and cause, so a release does not publish
documentation whose external links silently went missing.

The provider rule currently accepts release versions. An unknown/snapshot version
does not acquire a guessed Minecraft URL. New supported targets should verify
their provider and class URLs before publication.

## Regression checks

`./gradlew -p buildSrc testDistribution` packages offline seven-target fixtures
for release, beta and dev names, verifies the 16 explicit assets and rejects
incomplete devkits and Java-25-only shared extensions. Run
`python3 buildSrc/src/test/python/test_devkit_server.py` for the bundled preview
server's clean-URL and path-isolation checks. CI also builds a real two-target
devkit ZIP, including all four generated formats and the rendered site.

Reference prose and field contracts follow [the documentation style guide](STYLE.md).
Use `@DocletReadOnly` for fields maintained by Java that scripts should not assign;
Java `final` fields are recognized automatically. This generates TypeScript
`readonly` class/interface members, Python `Final[T]` hints, and read-only website
badges. These are static typing/documentation constraints, not runtime access
control or deep immutability. Values may still change when Java updates them.
Static library fields remain on the Java class declaration; the global library
namespace exposes instance methods, so use its getters (e.g. `World.getServer15MAverageTPS()`).

```sh
./gradlew :buildSrc:testExternalTypeLinks :buildSrc:testDocletLinks \
  :buildSrc:testDocBodyRenderer :buildSrc:testTsRenderer :buildSrc:testMarkdownWriter \
  :buildSrc:testExternalJavadocIndexes
npm --prefix docs/vitepress test
```

Browser regressions require a built site with at least two snapshots. Playwright
Core is a test-only dependency; install its browser explicitly:

```sh
./gradlew :buildVitepressSite -PdocsMinecraftVersions=1.21.5,26.3
cd build/docs/vitepress
npx playwright-core install chromium
npm run test:browser
```

The suite checks no-JS content, delayed loading/layout stability, direct and SPA
navigation, history, version switching, caching, failed/malformed requests, retry,
stale responses and mobile controls, including hydration/console errors. Optional
environment variables: `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` selects an existing
Chromium executable; `DOCS_SCREENSHOT_DIR` saves desktop/mobile screenshots;
`DOCS_SITE_DIR` selects another rendered site; `DOCS_TEST_URL` tests an existing
server (for example the extracted devkit); `DOCS_BASE_PATH` matches a site built
for a subdirectory deployment (including leading/trailing slashes).

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
