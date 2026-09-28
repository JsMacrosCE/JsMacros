# JsMacrosCE manual client suite

This directory contains small, self-contained client checks for behavior that
must remain consistent across supported Minecraft versions and both loaders.
They are executable examples of the public scripting API as well as regression
checks.

See [harness.md](harness.md) for the runner architecture and how future
`event` tests are expected to plug in.

## Test contract

- `suite.js` is the single entry point. Each registry entry tests one
  user-visible behavior; its identifier, display title, and documentation use
  functional names, not Minecraft-version names.
- Every test declares a `mode`:
  - `auto` — programmatic checks only. Runs headlessly and reports in chat; no
    page is shown.
  - `screen` — needs the suite screen open for input (typing, clicking). The
    page carries `Passed`/`Failed`/`Previous`/`Next`/`Back`.
  - `observe` — needs the world visible. The runner closes the suite screen,
    prints instructions plus clickable `[Pass] [Fail] [Skip] [Retry] [Menu]` in
    chat, and shows a small HUD hint while it runs. The chat screen must be open
    (`T`) to click.
  - `event` — reserved for future listener-based tests. Follow the `observe`
    contract, register `JsMacros.on(...)` in `prepare`, and optionally provide
    `done(state)`/`timeoutSeconds`.
- A test uses only public JsMacros APIs. Version-specific behavior belongs in
  the implementation, not in a test macro unless the version difference is the
  behavior being checked.
- A test must be harmless in a disposable client profile: no servers, commands,
  save edits, network actions, or unrelated user macros.
- A screen test schedules setup with
  `Client.runOnMainThread(JavaWrapper.methodToJava(...))`. It adds elements from
  `setOnInit`, because screen initialization clears prior elements.
- Each behavior area has a matching Markdown specification with scope, run
  steps, expected results, and exclusions. Keep observations outside the test
  script.

## Control surface

`suite.js` runs a client-tick loop so the script context stays alive while the
suite screen is closed. It posts a chat control panel:

- `[Run All]` runs the automated batch, then the visual batch, then opens the
  screen test sequence.
- `[Run Automated]` runs every `auto` test headlessly and posts a pass/fail
  summary in chat.
- `[Run Visual]` runs the `observe`/`event` tests in sequence via chat
  instructions and clickable verdicts.
- `[Run Screen]` opens the first `screen` test with Previous/Next/Back controls;
  Passed/Failed record and advance.
- `[Menu]` opens the browse screen (categories, individual tests, results).
- `[Results]` posts the current results in chat.
- Press `H` with no screen open to reopen the browse screen. `EventKey` is
  suppressed while a JsMacros screen is open, so the hotkey only fires when the
  suite is closed, which is the intended case.

The browse screen mirrors the same actions and labels each test with its mode.

## Adding a test

1. Add a row to [catalog.md](catalog.md) with a stable identifier and its mode.
2. Add a registry entry to `suite.js` (`run` for `auto`, `setup` for `screen`,
   `prepare`/`instructions`/`verify`/`teardown` for `observe`/`event`), a
   catalog row, and update the relevant behavior-area specification.
3. Run the same script against every relevant `(Minecraft version, loader)`
   tuple. Record each result using [results-template.md](results-template.md).
4. Keep labels descriptive of behavior. For example, use `Text Hover`, not
   `JsMacrosCE 26.1 text hover smoke test`.

Automated results are kept for the current suite session only; use the result
template for durable version/loader records.
