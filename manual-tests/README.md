# JsMacrosCE manual client suite

This directory contains small, self-contained client checks for behavior that
must remain consistent across supported Minecraft versions and both loaders.
They are executable examples of the public scripting API as well as regression
checks.

## Test contract

- `suite.js` is the single entry point. Each registry entry tests one
  user-visible behavior; its identifier, display title, and documentation use
  functional names, not Minecraft-version names.
- A test uses only public JsMacros APIs. Version-specific behavior belongs in
  the implementation, not in a test macro unless the version difference is the
  behavior being checked.
- A test must be harmless in a disposable client profile: no servers, commands,
  save edits, network actions, or unrelated user macros.
- A screen test schedules setup with `Client.runOnMainThread(JavaWrapper.methodToJava(...))`.
  It adds elements from `setOnInit`, because screen initialization clears prior
  elements.
- Each behavior area has a matching Markdown specification with scope, run
  steps, expected results, and exclusions. Keep observations outside the test
  script.

## Adding a test

1. Add a row to [catalog.md](catalog.md) with a stable identifier.
2. Add a registry entry to `suite.js`, a catalog row, and update the relevant
   behavior-area specification.
3. Run the same script against every relevant `(Minecraft version, loader)`
   tuple. Record each result using [results-template.md](results-template.md).
4. Keep labels descriptive of behavior. For example, use `Text Hover`, not
   `JsMacrosCE 26.1 text hover smoke test`.

The suite can run all registered tests in sequence, a category, or a selected
test. Results are kept only for the current suite session; use the result
template for durable version/loader records.
