import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File

plugins {
    `multiloader-loader`
    id("net.neoforged.moddev")
}

val mod_id = commonMod.prop("mod_id")
val minecraft_version = commonMod.prop("minecraft_version")
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

    // Automatically enable neoforge AccessTransformers if the file exists
    val at = project(":common").file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }

    val parchment_minecraft = commonMod.prop("parchment_minecraft")
    val parchment_version = commonMod.prop("parchment_version")

    parchment {
        minecraftVersion = parchment_minecraft
        mappingsVersion = parchment_version
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
    implementation("io.noties:prism4j:2.0.0")
    jarJar("io.noties:prism4j:[2.0.0,2.1.0)")

    implementation("org.jooq:joor:0.9.15")
    jarJar("org.jooq:joor:[0.9.15,0.10.0)")

    implementation("com.neovisionaries:nv-websocket-client:2.14")
    jarJar("com.neovisionaries:nv-websocket-client:[2.14,2.15.0)")

    implementation("org.javassist:javassist:3.30.2-GA")
    jarJar("org.javassist:javassist:[3.30.2-GA,3.31.0)")

    // For NeoForge < 1.21.9, external libraries need to be added to additionalRuntimeClasspath
    // to be loaded by the modular classloader during dev runs.
    // For 1.21.9+, this is handled automatically.
    if (stonecutterBuild.eval(minecraft_version, "<1.21.9")) {
        // Exclude annotations-java5 from prism4j as it has an invalid Java module name
        configurations.named("additionalRuntimeClasspath").configure {
            exclude(group = "org.jetbrains", module = "annotations-java5")
        }
        "additionalRuntimeClasspath"("io.noties:prism4j:2.0.0")
        "additionalRuntimeClasspath"("org.jooq:joor:0.9.15")
        "additionalRuntimeClasspath"("com.neovisionaries:nv-websocket-client:2.14")
        "additionalRuntimeClasspath"("org.javassist:javassist:3.30.2-GA")
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
            "minecraft_version" to minecraft_version
        ))
    }
}

sourceSets.main {
    resources.srcDir("src/generated/resources")
}

tasks {
    processResources {
        exclude("$mod_id.accesswidener")
        exclude("accesswideners/**")
    }
}

tasks.named("createMinecraftArtifacts") {
    dependsOn(":neoforge:$minecraft_version:processResources")
}
