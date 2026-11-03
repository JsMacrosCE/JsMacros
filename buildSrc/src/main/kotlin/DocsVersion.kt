/**
 * The directory and route version a documentation snapshot publishes under.
 *
 * A development snapshot changes on every commit, so `2.0.0-dev-abc1234`
 * publishes under the stable name `dev` and published links keep working.
 * Released versions keep their own name so several releases can be served
 * side by side.
 */
fun docsDirectoryName(modVersion: String): String =
    if (modVersion.contains("-dev-")) "dev" else modVersion
