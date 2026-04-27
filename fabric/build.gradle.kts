import org.gradle.language.jvm.tasks.ProcessResources
import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    kotlin("jvm") version "2.2.10"
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
    // `apply false` puts loom on the classpath without applying it; the actual
    // plugin (obfuscated vs deobfuscated) is picked per-version below. Typed DSL
    // accessors are not generated under `apply false`, so loom-specific config in
    // this file uses the string-based DSL (`"minecraft"(...)`, `configure<LoomGradleExtensionAPI>`).
    id("fabric-loom") apply false
    id("net.fabricmc.fabric-loom") apply false
    id("multiloader-loader")
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.22"
}

val mod_id = commonMod.prop("mod_id")
val minecraft_version = commonMod.prop("minecraft_version")
var mod_version = project.version.toString()

// MC 26.1 ships deobfuscated, so Loom 1.15+ skips the mappings layer and the
// remap step. The non-obfuscated plugin `net.fabricmc.fabric-loom` routes to
// `LoomNoRemapGradlePlugin`; the legacy `fabric-loom` alias still targets the
// obfuscated-MC plugin used for 1.21.x. Both IDs come from the same JAR and
// cannot be conditionalised inside the `plugins { }` block, so we apply the
// correct one here.
val isDeobfuscatedMc = minecraft_version.startsWith("26.")
apply(plugin = if (isDeobfuscatedMc) "net.fabricmc.fabric-loom" else "fabric-loom")

val loom = extensions.getByType(LoomGradleExtensionAPI::class.java)

base {
    archivesName.set("$mod_id-$minecraft_version-fabric-$mod_version")
}

// Configuration for embedding extension jars
val extensionJars by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

// Gradle is stupid and will throw a `Type mismatch: inferred type is Dependency? but Any was expected` otherwise
fun DependencyHandlerScope.implInclude(notation: Any) {
    val dep = requireNotNull(add("include", notation))
    add("implementation", dep)
}

dependencies {
    "minecraft"("com.mojang:minecraft:$minecraft_version")

    if (!isDeobfuscatedMc) {
        "mappings"(loom.layered(Action {
            val parchment_minecraft = commonMod.prop("parchment_minecraft")
            val parchment_version = commonMod.prop("parchment_version")

            officialMojangMappings()
            // Parchment does not yet ship mappings for 26.1.x; skip the layer when
            // either property is blank. TODO(26.1): re-enable once parchment publishes.
            if (parchment_minecraft.isNotBlank() && parchment_version.isNotBlank()) {
                parchment(
                    "org.parchmentmc.data:parchment-$parchment_minecraft:$parchment_version@zip"
                )
            }
        }))
    }

    val fabric_loader_version = commonMod.prop("fabric_loader_version")
    val fabric_version = commonMod.prop("fabric_version")
    val mod_menu_version = commonMod.prop("mod_menu_version")

    val modOrPlain = if (isDeobfuscatedMc) "implementation" else "modImplementation"
    add(modOrPlain, "net.fabricmc:fabric-loader:$fabric_loader_version")
    add(modOrPlain, "net.fabricmc.fabric-api:fabric-api:$fabric_version")
    add(modOrPlain, "com.terraformersmc:modmenu:$mod_menu_version")

    // Common library dependencies - include for bundling in jar
    implInclude("io.noties:prism4j:2.0.0")
    implInclude("org.jooq:joor:0.9.15")
    implInclude("com.neovisionaries:nv-websocket-client:2.14")
    implInclude("org.javassist:javassist:3.30.2-GA")

    // Extension jars to embed
    add(extensionJars.name, project(mapOf("path" to ":extension:graal", "configuration" to "archives")))
    add(extensionJars.name, project(mapOf("path" to ":extension:graal:js", "configuration" to "archives")))
}

// Collect extension jar names for dependencies property
fun getExtensionJarPaths(): String =
    extensionJars.files.joinToString(", ") { file ->
        "\"META-INF/jsmacroscedeps/${file.name}\""
    }

tasks.named<ProcessResources>("processResources") {
    // Embed extension jars into the final jar
    dependsOn(extensionJars)
    from(extensionJars) {
        into("META-INF/jsmacroscedeps")
    }

    // Add dependencies expansion for jsmacros.extension.json
    filesMatching("jsmacrosce.extension.json") {
        expand(mapOf("dependencies" to getExtensionJarPaths()))
    }

    // Expand fabric.mod.json5 with minecraft version
    filesMatching("fabric.mod.json5") {
        expand(
            mapOf(
                "version" to mod_version,
                "minecraft_version" to minecraft_version
            )
        )
    }
}

// Copy the version-specific access widener and rename it for the jar
loom.apply {
    // Use the version-specific access widener
    accessWidenerPath.set(project(":common").file("src/main/resources/accesswideners/$minecraft_version-$mod_id.accesswidener"))

    mixin(Action {
        defaultRefmapName.set("$mod_id.refmap.json")
    })
}

fletchingTable {
    fabric {
        applyMixinConfig = false
    }
    mixins.register(sourceSets.main) {
        mixin("default", "jsmacrosce-common.mixins.json5") {
            env("CLIENT")
        }
        mixin("fabric", "jsmacrosce-fabric.mixins.json5") {
            env("CLIENT")
        }
    }
    j52j.register(sourceSets.main) {
        extension(
            "json",
            "fabric.mod.json5",
            "jsmacrosce-common.mixins.json5",
            "jsmacrosce-fabric.mixins.json5"
        )
    }
}

stonecutter {
    replacements.string(current.parsed >= "1.21.11") {
        replace("ResourceLocation", "Identifier")

        // Conflicts
        replace("parseIdentifier", "parseIdentifier")
        replace("getAdvancementsForIdentifiers", "getAdvancementsForIdentifiers")
        replace("suggestIdentifier", "suggestIdentifier")
        replace("mapIdentifiers", "mapIdentifiers")
        replace("getIdentifier", "getIdentifier")
        replace("writeIdentifier", "writeIdentifier")
        replace("readIdentifier", "readIdentifier")
        replace("getWorldIdentifier", "getWorldIdentifier")
        replace("base.readResourceLocation", "base.readIdentifier")
        replace("base.writeResourceLocation", "base.writeIdentifier")
        replace("@return the raw minecraft Identifier.", "@return the raw minecraft Identifier.")
    }
}
