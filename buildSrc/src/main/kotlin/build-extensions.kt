import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

val Project.mod: ModData get() = ModData(this)
fun Project.prop(key: String): String? = findProperty(key)?.toString()

val Project.stonecutterBuild get() = extensions.getByType<StonecutterBuildExtension>()
val Project.stonecutterController get() = extensions.getByType<StonecutterControllerExtension>()

val Project.common get() = requireNotNull(stonecutterBuild.node.sibling("common")) {
    "No common project for $project"
}
val Project.commonProject get() = rootProject.project(stonecutterBuild.current.project)
val Project.commonMod get() = commonProject.mod

val Project.loader: String? get() = prop("loader")

/**
 * Name of the task producing `loader`'s mod jar.
 *
 * Loom only registers `remapJar` while Minecraft is obfuscated; 26.1+ ships unobfuscated and
 * exposes a bare `jar` instead. Ask Loom which one exists rather than re-deriving the version
 * predicate in every build script, in the controller and in CI.
 */
fun Project.modJarTaskName(loader: String): String =
    if (loader == "fabric" && tasks.findByName("remapJar") != null) "remapJar" else "jar"

@JvmInline
value class ModData(private val project: Project) {
    /**
     * The exact Minecraft version this node targets.
     *
     * The value declared in `versions/<v>/gradle.properties` must agree with the Stonecutter
     * version. If the two ever drift, version-dependent build logic (access wideners, mappings,
     * packaging) silently starts describing a different Minecraft than the node compiles against.
     */
    val mc: String
        get() {
            val declared = prop("minecraft_version")
            val stonecutter = project.stonecutterBuild.current.version
            check(declared == stonecutter) {
                "minecraft_version ($declared) does not match the Stonecutter version ($stonecutter)"
            }
            return declared
        }

    val modId: String get() = prop("mod_id")

    /**
     * File name of the access widener the Fabric loader consumes, relative to
     * `common/src/main/resources/accesswideners/`.
     *
     * Minecraft ships unobfuscated from 26.1, where Loom applies the widener with
     * `readOfficial` and rejects any namespace other than `official`. Older versions are remapped
     * and need `named`. The namespace is declared per version as `access_widener_namespace`
     * rather than inferred here, because Fletching-Table's AW lexer only understands
     * `named`/`intermediary`: the AW->AT conversion feeding NeoForge still needs its own `named`
     * copy, so the two files cannot be merged.
     */
    val fabricAccessWidener: String
        get() {
            val namespace = propOrNull("access_widener_namespace") ?: "named"
            val suffix = if (namespace == "named") "" else "-$namespace"
            return "$mc-${prop("mod_id")}$suffix.accesswidener"
        }

    fun propOrNull(key: String): String? = project.findProperty(key)?.toString()
    fun prop(key: String): String = requireNotNull(propOrNull(key)) { "Missing property '$key'" }
}
