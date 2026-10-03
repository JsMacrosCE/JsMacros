# Known Issues
This document acts as a working list of known issues with JsMacrosCE. If you encounter a bug that is not listed here, please report it on the [GitHub Issues Page](https://github.com/JsMacrosCE/JsMacros/issues).

## General
- Some antivirus software may falsely flag JsMacrosCE as malicious.

## API
There are definitely some issues with the API, please report any bugs you find on the GitHub Issues Page.

- Merchant offers are not synchronized to client-side merchant entities. Read them through `VillagerInventory.getTrades()` while a trading screen is open; `MerchantEntityHelper.getTrades()` and `refreshTrades()` cannot retrieve or refresh offers on a client entity.
- Horse owner and some other passive-mob state is not synchronized to the client. A missing horse owner or a false client-side wetness flag on a wolf is not authoritative server state.
- `EndCrystalEntityHelper.isNatural()` exposes the crystal's *show-bottom* flag, not a provable origin. Other client-side entity predicates based on equipment or custom names are similarly heuristics.
- `EntityHelper.asServerEntity()` is available only with an integrated server and returns a live server entity. Further access to that helper must take place on the integrated-server thread. This should be gated in the Doclet somehow in the future.
- Cancellable packet event listeners execute synchronously so cancellation can take effect before network dispatch. Keep these callbacks short: a slow script can delay packet processing and hit the configured event-lock watchdog limit.

## Script Engine
To the best of my knowledge, there are no major issues with the script engine itself at this time.

## Extensions
- Missing support for Lua, Ruby, Kotlin, and WASM extensions.
- The JsMacros-Ruby extension currently has a community update that should work by @grepsedawk [here](https://github.com/grepsedawk/JsMacros-Ruby)
- Lua extension is currently WIP locally, no ETA for release. If anyone is interested in helping with development, please reach out on the Discord.

## Version Specific

### 1.21.5
- Background of screens don't render their background correctly
