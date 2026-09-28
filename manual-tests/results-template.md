# Manual client suite results

Copy this file for each run or release and record one row per `(test, Minecraft
version, loader)` tuple. Keep observations here, not in the test scripts.

## Environment

- JsMacrosCE version:
- Date:
- Profile / world:
- Other mods or notes:

## Results

Outcome is one of `Passed`, `Failed`, or `Skipped`. Record the mode
(`auto`/`screen`/`observe`/`event`) and the automatic count exactly as shown by
the suite (`passed/total`). The suite keeps results for the current session
only, so this file is the durable record.

| Test ID | Mode | Minecraft | Loader | Automatic | Outcome | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| | | | | | | |

## Regression comparison

List each test ID whose outcome changed between tuples or releases, with the
reason and any log/screenshot reference:

- 

## Known target-specific exceptions applied

Quote the documented exception for every test marked `Skipped` (for example the
Fabric-only debug-screen overlay, or the pre-26.1 pick hook):

- 
