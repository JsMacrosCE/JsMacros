# Build version sources

- `libs.versions.toml` is for shared Java libraries and repeated project plugin
  versions. Its entries are used by the module builds; there are no placeholder
  aliases. Keep the same alias for a library used with different configurations
  (for example, `compileOnly` in common and `include` on Fabric).
- `versions/<minecraft>/gradle.properties` is authoritative for Minecraft,
  NeoForm/Parchment, Fabric Loader/API, NeoForge, Mod Menu and the Java toolchain
  for that target. Do not put these target-specific coordinates in the catalog.
- `settings.gradle.kts` owns the settings plugins and the Loom/ModDevGradle
  plugin versions. The Stonecutter controller inherits Loom/ModDevGradle
  versions from plugin management; its build script uses the catalog for the
  shared publishing plugin. `buildSrc` is a separate Gradle build with its own
  plugin classpath; its Kotlin and Stonecutter dependencies are pinned there.
- Keep intentional differences: the extensions use Guava 31.1-jre to align
  with NeoForge, not a newer generic Guava version. NeoForge's `jarJar` upper
  compatibility bounds remain explicit, while their lower bounds follow the
  selected catalog versions. Graal artifacts share one version but preserve
  their per-extension runtime and embed configurations.

When upgrading a shared library, check all its configurations and the Fabric
include / NeoForge jarJar packaging, not only the compile classpath. Keep
`stonecutter.active` unchanged when validating explicit version tasks.
