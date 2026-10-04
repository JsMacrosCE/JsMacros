# Documentation work

When writing or revising Javadoc, doclet annotations, or generated-documentation
code, read `docs/STYLE.md` and `docs/README.md` first.

For a bounded prose-pruning batch, use the `/docs-prune` command; it coordinates
the `docs-editor`, `docs-reviewer`, and `docs-validator` agents.

- Write concise API contracts, not a tutorial for every member. Keep verified
  caveats; remove filler and unsupported claims.
- Keep a compact class-level example. Member examples are optional and should
  demonstrate non-obvious usage; trivial getters do not need individual examples.
- Model script read-only fields with `@DocletReadOnly`, not “Don't modify” prose.
  Check intended script usage before annotating, and keep annotation declarations
  in `common` and `buildSrc` identical.
- Edit shared sources, never generated Stonecutter views or documentation outputs.
  Validate an explicit Minecraft target without switching the active version.
- Compare generated Python with source Javadoc, including field docs and
  docstrings. Inspect every affected output; inspect all four for markup, tags,
  typing metadata, or changes intended to work across formats. A green build or
  type-check alone does not prove prose accuracy.
