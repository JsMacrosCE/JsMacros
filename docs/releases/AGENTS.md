# Release notes style guide

This guide is for `docs/releases/<version>.md`. These files are embedded in the
GitHub and Modrinth release bodies, so they are read by players and script
authors, not by maintainers reading a commit log.

Read `docs/releases/2.1.0.md` and `docs/releases/2.2.0.md` as reference examples.
For API Javadoc, use `docs/STYLE.md` instead; this file only covers release notes.

## Start from the right baseline

Release notes describe changes since the previous published build, not every
change that exists in the repository.

- Identify the baseline first: the previous stable release, or the last beta when
  the release follows a beta series. The git branch, `stonecutter.active`, and the
  current mod version are not the release baseline; use tags and published releases.
- Compare the baseline tag with the release tag and, when asked, the current
  worktree. Include staged and unstaged changes only when the user says they are
  part of the release.
- Use `git log --oneline <baseline>..<target>` to find candidates, then read the
  diff and the source. A commit subject is not enough to describe user-visible
  behavior.
- Check `@since` and `@DocletIgnore`. Do not announce a method as a new script API
  if it is hidden from generated docs, or if it already exists in the baseline.
- Do not reset, stash, stage, commit, or reformat user changes while gathering
  evidence.

If a feature already exists in the baseline, do not call it new. If a change is
only internal, CI, tests, or build plumbing, leave it out unless it changes what a
user or extension author can do.

## Voice and audience

Write for someone who uses JsMacrosCE and wants to know what changed. Assume they
know how to write a script, but not that they have read the source or the commit
log.

- Be direct, calm, and specific. State the effect, not the effort behind it.
- Explain why a change matters when the effect is not obvious, especially in
  Upgrading. Do not invent a reason; if the reason is unknown, state the effect.
- Use present tense for current behavior and "now" for a change. Use "gained" or
  "adds" sparingly, when it reads naturally.
- Prefer plain English over implementation vocabulary. Mention Java classes,
  internal methods, or Minecraft internals only when the user needs them.
- Use one term consistently: "event lock", "joined handling", "read-only
  delivery", "script type namespaces".
- Do not use marketing language: "powerful", "seamless", "huge", "exciting",
  "under the hood", "various", "misc", "simply", "just", or "obviously".
- Do not use "we" or address the maintainers. Do not use exclamation marks or
  emoji.
- Do not promise future behavior. Version-gate claims when a feature exists only
  on some Minecraft targets.

Be concrete enough that the reader can find the change without searching the
guide. Name the method, event, setting, option, or config path. Do not turn a
release note into a tutorial, and do not list every internal fix.

## File shape

Release notes are Markdown fragments. The build embeds these files only for a
stable `x.y.z` release and adds the release title and header, so the file starts
with a section, not an H1.

Use this order and omit empty sections:

```markdown
## Highlights

## Added

**Group name**

## Changed

## Fixed

## Upgrading

## Quick examples
```

`Quick examples` is optional and always goes last.

- Use `##` for sections. Use bold text, not headings, for groups inside a section.
- Put backticks around API names, methods, events, options, and paths.
- Use absolute links (`https://jsmacrosce.com/...`) because the notes are mirrored
  outside the docs site. Do not use relative links.
- Keep the file usually around 40-75 lines without Quick examples. That section
  may add roughly 25 lines. Length follows the size of the release; this is not a
  quota.
- Use a table for a repeated old-to-new mapping. Outside Quick examples, use a
  short code block only when a signature or migration is clearer that way; avoid
  full tutorials.
- Do not repeat the same detail in several sections. Highlights may summarize
  something that Added explains once.

## Section rules

### Highlights

Four to six bullets for the changes a reader should see first. Good candidates
are new Minecraft targets, a new scripting capability, a behavior change that
requires attention, a major documentation change, or a new extension workflow.

Make each bullet a complete idea. Do not write an exhaustive table of contents.
If the release is mostly one large change, use the highlights to explain its
shape and put the details in Added and Upgrading.

### Added

List actual new capabilities, grouped by area: scripting and game APIs, rendering,
packets and networking, documentation, extensions, and so on.

Name what was added. Avoid vague summaries:

Bad:

> Rendering additions include `Draw2D.clear()`, visibility and element controls
> for `Draw3D`, surface lighting controls, and the `Color` library.

Better:

> - The `Color` library creates and inspects packed ARGB colors with
>   `Color.rgb(...)`, `Color.argb(...)`, `Color.fromPacked(...)`, and
>   `Color.withAlpha(...)`.
> - `Draw2D.clear()` removes an overlay's elements without unregistering it.
> - `Draw3D` gained `visible`/`setVisible()`/`isVisible()`, `getElements()`, and
>   `removeElement()`.

Include version-gated behavior in the bullet: "on 26.1 and later". Include the
script-facing name, not the Java implementation name, unless the Java name is
what the user must pass to `Java.type` or a config.

### Changed

Use this for behavior, defaults, return values, configuration, and compatibility
changes. State what changed and what existing code will now experience.

Good:

> - The two-argument `Hud.createScreen` now enables the world blur as well as the
>   menu background. Use the three-argument overload with `false` to turn blur off.

Do not call a behavior change a fix. Do not hide a breaking change by describing
it only in Upgrading. If existing calls keep working, say so; if they need an
edit, say that in Upgrading.

### Fixed

Group related fixes by user-visible area and describe the symptom, not the commit
message.

Good:

> - Rendering: 3D box fill alpha, surface draw order, short and zero-width line
>   drawing, and `Draw2D.addItem` returning `null`.

Avoid:

> - Fix `Box.setFillColor`, `fixAlpha`, `D3D` render order, and the line path.

Do not list every small repair. Pick the ones a user could have noticed, and use
semicolons to keep related fixes in one bullet.

### Upgrading

Upgrading is for actions, not a second changelog. Every bullet should answer:

1. Who needs to act?
2. What should they do?
3. Why do they need to do it?

Mark required and optional changes clearly. If nothing is required, say so once
and then only list the conditional cases.

Good:

> - If you use Python scripts, replace any older GraalPython extension with the
>   JAR published with 2.2.0 in `config/jsMacros/Extensions/`. The extension must
>   match the installed mod release; JavaScript is built in and needs no extension.

Bad:

> - JavaScript is built in. For Python, replace the old extension.

The second version does not say whether this affects the reader or why. Do not
put general feature descriptions in Upgrading.

Preserve migration facts exactly: old and new field names, the fact that a saved
config key is unchanged, whether an old method still works, and the version in
which it will be removed. Use a table when several fields changed at once.

### Quick examples

Optional. Add this section only when several changes are easier to understand as
usage than as prose, and place it last, after Upgrading. Use exactly one `js`
fenced block; do not scatter snippets through the other sections.

- Include 3-6 examples, roughly 25-35 lines total. If it needs more, put it in
  the guide instead.
- Pick changes where the new shape is non-obvious or where users are likely to
  keep using the old pattern. Do not demonstrate trivial additions such as
  `Draw2D.clear()`.
- Keep each example short and real. Use the actual script names and include the
  null checks or callback wrapping the API requires.
- Use `// Before:` and `// After:` pairs only when the migration is the point.
  Otherwise show the new way once.
- Label the version or context when it is not obvious, for example `// 26.1+`.
- Do not add meta-comments such as "independent snippets", "copy the one you
  need", or "example 1". Use descriptive comments for the feature being shown.
- Do not turn the block into a second changelog. One example per major theme.
- Verify every snippet against the target source and the generated script names.

## Wording patterns

- New release target: "Fabric and NeoForge builds now support Minecraft
  **26.1.2, 26.2, and 26.3**, alongside 1.21.5, ...".
- New API: "`World.getTimelines()` and `World.getClockManager()` expose ...".
- Event accessor: write `event.getMessage()`, not "the getter for the message".
  Keep the old field name only in a migration table.
- Changed default: "The two-argument form now ...".
- Compatibility: "Existing calls that ignore the result still work."
- Fix: "`Draw2D.addItem` no longer returns `null`."
- Migration: "Replace reads and writes of `event.message` with
  `event.getMessage()` and `event.setMessage(...)`."
- Conditional action: "If you use ...", "Existing profiles keep ...", "Fresh
  configurations start with ...".
- Uncertain behavior: narrow the claim or omit it. Never fill a gap with a guess.

## Accuracy checklist

Before finishing:

- The baseline is identified and the notes describe only changes since it.
- Every named API exists in the target source and is script-facing.
- Version-gated features say which Minecraft versions they apply to.
- Behavior changes are under Changed, not hidden under Fixed.
- Fixes describe symptoms, not internal class names.
- Upgrading says who must act and why; optional changes are marked optional.
- Config migration details and deprecation timelines are preserved.
- No feature is called new when it already exists in the baseline.
- Quick examples, if present, has 3-6 verified snippets and no meta-comments.
- If Quick examples is present, it is the only demonstration block; any migration
  snippet in Upgrading stays short and strictly necessary.
- No relative links, H1, marketing language, emoji, or unsupported claims.
- The file reads as a changelog a user can scan, not as a commit summary.
