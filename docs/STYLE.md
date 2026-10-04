# API documentation style

This guide is for Javadocs that describe JsMacrosCE's script-facing API. Those
comments are source material for four generated references: TypeScript
declarations, Python stubs, legacy HTML, and the VitePress site. Write one
accurate contract that survives all four; do not write four renderer-specific
versions in the comment.

## 1. Write the contract, not a tutorial

Lead with one direct sentence saying what the class or member is or does. Add
only details that change a caller's understanding or use of it, such as:

- units, coordinate systems, index bases, defaults, or the meaning of a value;
- null, sentinel, empty-result, or failure behavior;
- side effects, state changes, lifecycle, thread, or cancellation behavior;
- a meaningful distinction between overloads;
- version-dependent behavior or a non-obvious limitation.

Prefer the shortest wording that preserves those facts. Remove repeated
introductions, restatements of names and signatures, obvious implications,
hypothetical use cases, rhetorical warnings, and advice not supported by the
implementation. Do not add prose merely to make a comment look complete. There
is no sentence or word quota: a complicated contract can need more explanation
than a simple getter.

Descriptions are not a place to say “Don't modify” or repeat a type constraint.
Use the appropriate metadata and generated typing contract described below.
Never infer behavior from a method name, a Minecraft convention, or the previous
Javadoc. Check the implementation and, for a Minecraft-facing claim, the
matching target's source snapshot. If behavior cannot be established, narrow the
claim or flag it for investigation instead of presenting a guess as fact.

### Class documentation

Describe the class's role and, when relevant, how scripts obtain or use it.
`@Library("World")` and `@Event("Tick")` declare script-facing aliases; state
the global name when it helps orient the reader. Explain only class-wide facts
that members cannot explain clearly on their own. A nested/supporting type is
not automatically a separate global library.

Keep a compact class-level example for script-facing API classes. Show a common
task that demonstrates the API's shape, including necessary guards or callback
wrapping. Avoid turning the class overview into an index of every method or a
long catalogue of examples.

### Members and overloads

- Methods: describe the action or returned value, then state non-obvious
  conditions and effects. A method name and rendered signature already convey
  many basics; don't repeat them in a paragraph.
- Parameters: document meaning, accepted form, units, coordinate space, or
  special cases in `@param`. Keep each description about that parameter.
- Returns: use `@return` for the result's meaning and important states; don't
  restate its Java type. State null/sentinel behavior explicitly when it is not
  obvious. Keep prose consistent with `@Nullable` and any doclet type override.
- Fields: describe what the value represents and units/state if relevant. Mark
  script read-only fields with `@DocletReadOnly`; Java `final` fields are
  recognized automatically. Do not mistake read-only for constant, frozen, or
  deeply immutable.
- Constructors: explain required setup or lifecycle effects, not that the
  constructor “creates an instance.” Document parameters as usual.
- Overloads: explain the meaningful difference once where possible, but make
  each overload's own contract understandable. Do not copy the same long
  caveat/example into every overload.

Event documentation should identify when the event fires, what its payload
means, and any important cancellation or thread behavior. Verify these against
the event dispatch/injection path. For cancellable events, distinguish
cancelling the event from changing an underlying action; do not claim
cancellability merely because the event name suggests it.

## 2. Javadoc structure and supported tags

Use a normal Javadoc body followed by block tags. Keep examples and all
description paragraphs **before the first block tag**: text after `@param`,
`@return`, or another block tag belongs to that tag, not to the main
description. This is especially important for examples; otherwise a renderer
may place them in a parameter/return description or omit them.

Supported/appropriate tags:

- `@param name ...`: one non-empty tag for each meaningful parameter. For a
  type parameter, use standard `@param <T> ...` syntax.
- `@return ...`: one tag for a meaningful non-void result. Explain semantics,
  not the return type repeated in words.
- `@since x.y.z`: the **JsMacrosCE release** that introduced this API, not the
  Minecraft version. Preserve known historical values; do not guess or leave
  `[citation needed]` in polished user-facing docs. Investigate uncertain
  history separately.
- `@deprecated explanation`: say why it is deprecated and what to use instead
  when known. Keep this consistent with Java's `@Deprecated` annotation.

Do not add `@see`: its output is inconsistent (`webdoclet` ignores it and it can
leak raw into TypeScript). Use an inline `{@link ...}` in the relevant sentence
instead. Do not assume standard tags such as `@throws`, `@author`, or arbitrary
custom tags are faithfully represented in all outputs. Before introducing one,
prove its behavior across the actual generated artifacts; otherwise put a
verified exception/failure contract in the description or `@return`.

Avoid duplicate block tags. The Python renderer keeps the last duplicate
`@param` and the first `@return`; other formats can behave differently. Duplicate
tags can silently discard prose or an example. Check parameter names against the
Java declaration and ensure all tags precede no further body text.

## 3. Examples

Write examples as `<pre>` blocks in the main description, without an `example:`
label:

```java
/** Returns whether a world is loaded.
 * <pre>
 * if (World.isWorldLoaded()) {
 *   Chat.log(`in ${World.getDimension()}`);
 * }
 * </pre>
 * @return {@code true} when a world is loaded; {@code false} otherwise
 */
```

Place the example in the main description, before all block tags. Keep examples
short, executable in the documented script context, and useful enough to teach
something beyond the obvious call. Include required null/world checks,
`JavaWrapper.methodToJava` callback conversion, and event filterers when those
are part of the real usage contract. Do not invent helper names or signatures.
The class example is the default example users copy; member examples are
optional for trivial getters and setters.

`example:` is not a Javadoc tag or doclet marker; it is emitted as ordinary
prose (notably in Python docstrings). Omit it in new or revised comments. Remove
it when editing nearby examples, but do not make a broad comment-only migration
solely to delete the label without reviewing the example itself.

Examples are documentation too: verify every JsMacros symbol and overload
against source. Use the script's actual JavaScript syntax; do not put Java
declarations or TypeScript-only syntax in a JavaScript example. A
`<pre class="language-typescript">` block is available when the example truly
requires TypeScript. Check that the rendered example appears in all four
outputs, and type-check examples where the harness supports them.

## 4. Inline markup, code, and links

- Use `{@code ...}` for identifiers, signatures, literal values, syntax, and
  text that must remain literal—especially `<`, `>`, or `&`. It is the safest
  inline form across the doclets (Python renders it quoted). Avoid bare HTML
  entities or raw angle brackets for code.
- Use `{@link Type}` or `{@link #method(...)}` for a useful source link. Use
  `{@code memberName}` instead for primitive-typed fields; the legacy HTML
  renderer cannot resolve those links reliably.
- For overloaded methods, supply a resolvable parameter signature. Fully
  qualify or import parameter types where needed; a short unqualified type can
  resolve to the wrong overload. Verify the generated target/anchor.
- Link labels are not uniform across formats: the legacy web renderer drops
  labels. Do not depend on a label to preserve essential meaning.
- Use restrained standard markup for paragraphs and lists. Avoid raw HTML as a
  formatting workaround; renderers handle HTML differently. In particular,
  Python may drop entities and some HTML nodes. If exact output matters, inspect
  generated artifacts rather than reasoning from markup.

## 5. Script-facing typing and doclet annotations

Java declarations remain the baseline, but script APIs sometimes need explicit
presentation metadata. These annotations change documentation/types, not
runtime behavior; do not use them to conceal an implementation mismatch.

- `@DocletReplaceParams("...")`: replace the generated script parameter list
  when Java types do not express the supported script-facing signature. Keep
  names, optionality, nullability, overloads, and declarations accurate. Some
  overrides contain complete TypeScript overload declarations; review their
  output rather than assuming they are simple type names.
- `@DocletReplaceReturn("...")`: replace the generated script return type.
  Preserve the actual value shape and nullable behavior.
- `@DocletReplaceTypeParams("...")`: override generated generic constraints
  when required by the scripting API.
- `@DocletReadOnly`: mark a field scripts may read but should not reassign.
  This yields TypeScript `readonly`, Python `Final[T]`, and website read-only
  indicators; it is not runtime enforcement or object immutability. Check
  whether scripts are intended to assign the field before annotating it.
- `@DocletDeclareType(name = ..., type = ...)`: declare a script typing alias
  where required. Confirm the generated declaration and its use sites.
- `@DocletCategory("...")`: organize generated reference indexes; it does not
  describe runtime behavior.
- `@Library` and `@Event`: define script API grouping/aliases. Do not change
  them as a prose edit.

Keep duplicate annotation declarations under `common` and `buildSrc` identical
where the project mirrors an annotation for source compilation/doclet use.
Static fields on a Java library class are not necessarily properties of the
global library namespace; check the generated declaration and direct users to
the script-facing getter when that is the exposed API.

## 6. Cross-format constraints

All formats consume the same Javadoc, but not all markup/tags render the same
way. Treat the current implementation and generated artifacts as the authority;
this summary is a set of authoring guardrails, not a promise that arbitrary
Javadoc syntax is portable.

| Source construct | Guidance |
| --- | --- |
| Plain prose | Keep it format-neutral and concise. |
| `{@code ...}` | Preferred literal/code form; inspect Python's quoted rendering when exact appearance matters. |
| `{@link ...}` | Resolve/label behavior differs. Verify links and overload anchors in generated HTML/Markdown and typings. |
| `<pre>` example | Keep before block tags; inspect the actual `.py` docstring and each output in which the example is intended to appear. |
| `@param`, `@return`, `@since`, `@deprecated` | Supported contract tags; use once and in the correct place. |
| `@see`, arbitrary HTML/entities/custom tags | Avoid unless a fixture proves correct behavior in all affected formats. |

The Python stub can lose content while still parsing: damaged text lives in
docstrings. A successful build, `ast.parse`, or TypeScript check does not prove
the rendered documentation is correct. For any Javadoc edit, compare the
generated Python docstring/output against the source. Check the other formats
where the changed content is expected to appear. Inspect all four when adding
markup/tags, changing doclet metadata, or claiming the construct works across
all formats; inspect fields as well as method docstrings.

## 7. Editing and review checklist

Before editing, read neighboring overloads and trace every behavior claim to the
implementation. For Minecraft behavior, inspect the exact target-matching
snapshot before using that API as evidence. Edit shared source, not generated
Stonecutter views or `build/docs` output. Preserve unrelated user edits and
public names/signatures/annotations.

For a batch, use `/docs-prune`. Keep batches coherent and bounded; assign
parallel editors disjoint files. A separate reviewer should look for both lost
contracts and unsupported claims. Generate the affected documentation
target(s), compare Python output with the edited source, and inspect each
affected format. For new markup/tags, script typings, or custom doclet
annotations, inspect all four formats and run focused fixtures/checks for
positive and negative cases.

Before accepting a change, ask:

1. Is each sentence useful, accurate, and supported by source?
2. Can a caller determine arguments, result/null behavior, side effects, and
   important restrictions without relying on an unsupported inference?
3. Are overloads distinct without repeating the same tutorial?
4. Are every link, example, tag, and annotation intentional and correctly placed?
5. Does generated Python preserve the prose, and does each affected format
   express the intended API contract? If the change is intended to work across
   the pipeline, have all four formats been checked?
