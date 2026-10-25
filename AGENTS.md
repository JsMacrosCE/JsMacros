# Comments and descriptions

- Explain purpose before mechanics: what the code lets someone do, what problem
  it solves, or why it exists. Write for a developer who is new to this area.
- Use plain language and a small concrete example when it helps. Introduce
  implementation terms only after the reader understands the point; do not lead
  with a restatement of Java declarations or framework jargon.
- Keep the useful technical details and verified caveats. Clearer wording must
  not weaken the contract or invent behavior.
- Implementation comments should explain a design choice or a problem being
  avoided, not narrate obvious code. Straightforward members do not need filler.
- Before finishing, reread descriptions without relying on the implementation:
  can a new reader tell what this is for? See `docs/STYLE.md` for API documentation.

# Documentation work

When writing or revising Javadoc, doclet annotations, or generated-documentation
code, read `docs/STYLE.md` and `docs/README.md` first.

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
