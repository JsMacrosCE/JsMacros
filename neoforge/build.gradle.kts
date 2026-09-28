import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.FilterReader
import java.io.File
import java.nio.file.Path

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    `multiloader-loader`
    id("net.neoforged.moddev")
    alias(libs.plugins.fletching.neoforge)
}

val mod_id = commonMod.modId
val minecraft_version = commonMod.mc
val neoforge_minecraft_version_range = commonMod.propOrNull("neoforge_minecraft_version_range") ?: "[$minecraft_version]"
var mod_version = project.version.toString()
var neoforge_version = commonMod.prop("neoforge_version")

base {
    archivesName.set("$mod_id-$minecraft_version-neoforge-$mod_version")
}

// Configuration for embedding extension jars
val extensionJars by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

neoForge {
    version = neoforge_version

    accessTransformers.from(
        layout.buildDirectory.file("generated/access-transformer/accesstransformer.cfg")
    )

    // Only declared for obfuscated versions; 26.1+ is unobfuscated and has no Parchment release.
    val parchment_minecraft = commonMod.propOrNull("parchment_minecraft")
    val parchment_version = commonMod.propOrNull("parchment_version")

    if (parchment_minecraft != null && parchment_version != null) {
        parchment {
            minecraftVersion = parchment_minecraft
            mappingsVersion = parchment_version
        }
    }

    runs {
        configureEach {
            // TODO: Is this needed
            systemProperty("neoforge.enabledGameTestNamespaces", commonMod.prop("mod_id"))
            ideName.set("NeoForge ${name.replaceFirstChar { it.uppercase() }} (${project.path})")
        }

        register("client") {
            client()
        }

        // TODO: I don't think we use any data runs
        register("data") {
            // matches your Groovy: "data" run name, but uses client lifecycle datagen
            clientData()

            programArguments.apply {
                addAll("--mod", commonMod.prop("mod_id"))
                add("--all")
                addAll("--output", file("src/generated/resources/").absolutePath)
                addAll("--existing", file("src/main/resources/").absolutePath)
            }
        }
    }

    mods {
        register(commonMod.prop("mod_id")) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    // Common library dependencies - implementation for dev, jarJar for bundling in production
    implementation(libs.prism4j)
    jarJar("io.noties:prism4j:[${libs.versions.prism4j.get()},2.1.0)")

    implementation(libs.joor)
    jarJar("org.jooq:joor:[${libs.versions.joor.get()},0.10.0)")

    implementation(libs.nv.websocket)
    jarJar("com.neovisionaries:nv-websocket-client:[${libs.versions.nv.websocket.get()},2.15.0)")

    implementation(libs.javassist)
    jarJar("org.javassist:javassist:[${libs.versions.javassist.get()},3.31.0)")

    // For NeoForge < 1.21.9, external libraries need to be added to additionalRuntimeClasspath
    // to be loaded by the modular classloader during dev runs.
    // For 1.21.9+, this is handled automatically.
    if (stonecutterBuild.eval(minecraft_version, "<1.21.9")) {
        // Exclude annotations-java5 from prism4j as it has an invalid Java module name
        configurations.named("additionalRuntimeClasspath").configure {
            exclude(group = "org.jetbrains", module = "annotations-java5")
        }
        "additionalRuntimeClasspath"(libs.prism4j)
        "additionalRuntimeClasspath"(libs.joor)
        "additionalRuntimeClasspath"(libs.nv.websocket)
        "additionalRuntimeClasspath"(libs.javassist)
    }

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

    // Add dependencies expansion for jsmacrosce.extension.json
    filesMatching("jsmacrosce.extension.json") {
        expand(mapOf("dependencies" to getExtensionJarPaths()))
    }
    
    // Expand neoforge.mods.toml with version properties
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(mapOf(
            "version" to mod_version,
            "minecraft_version" to minecraft_version,
            "neoforge_minecraft_version_range" to neoforge_minecraft_version_range
        ))
    }
}

sourceSets.main {
    resources.srcDir("src/generated/resources")
}

val accessTransformerFile = layout.buildDirectory.file("generated/access-transformer/accesstransformer.cfg")
val accessWidenerFile = rootProject.file(
    "common/src/main/resources/accesswideners/$minecraft_version-$mod_id.accesswidener"
)
val generateAccessTransformer by tasks.registering(Copy::class) {
    from(accessWidenerFile)
    into(accessTransformerFile.map { it.asFile.parentFile })
    rename { "accesstransformer.cfg" }
    val transformerClass = Class.forName(
        "dev.kikugie.fletching_table.transformer.Aw2AtFileTransformer"
    ) as Class<out FilterReader>
    val argsClass = Class.forName(
        "dev.kikugie.fletching_table.transformer.Aw2AtFileTransformer\$TransformArgs"
    )
    val args = argsClass.getDeclaredConstructor(Path::class.java)
        .newInstance(accessWidenerFile.toPath())
    filter(mapOf("args" to args), transformerClass)
}

tasks.named("createMinecraftArtifacts") {
    dependsOn(generateAccessTransformer)
}

fletchingTable {
    neoforge {
        applyMixinConfig = false
    }
    mixins.register(sourceSets.main) {
        mixin("default", "jsmacrosce-common.mixins.json5") {
            env("CLIENT")
        }
        mixin("neoforge", "jsmacrosce-neoforge.mixins.json5") {
            env("CLIENT")
        }
    }
    accessConverter.register(sourceSets.main) {
        add("accesswideners/$minecraft_version-$mod_id.accesswidener")
    }
    j52j.register(sourceSets.main) {
        extension(
            "json",
            "jsmacrosce-common.mixins.json5",
            "jsmacrosce-neoforge.mixins.json5"
        )
    }
}

stonecutter {
    replacements.string(current.parsed >= "1.21.11") {
        replace("ResourceLocation", "Identifier")

        replace("net.minecraft.Util", "net.minecraft.util.Util")
        replace("net.minecraft.advancements.critereon", "net.minecraft.advancements.criterion")

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

    replacements.string(current.parsed >= "26.1") {
        replace("GuiGraphics", "GuiGraphicsExtractor")

        // Conflicts
        // NeoForge's ScreenEvent.Render.Post accessor, which is still spelled this way
        replace("getGuiGraphics", "getGuiGraphics")
    }
}
