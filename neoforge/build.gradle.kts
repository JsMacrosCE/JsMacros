import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File

plugins {
    id("multiloader-loader")
    id("net.neoforged.moddev")
}

base {
    archivesName.set("${property("mod_id")}-${property("minecraft_version")}-neoforge-${project.version}")
}

// Configuration for embedding extension jars
val extensionJars by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

// TODO: Look into https://github.com/neoforged/ModDevGradle?tab=readme-ov-file#disabling-decompilation-and-recompilation in CI
neoForge {
    version = property("neoforge_version").toString()

    // Automatically enable neoforge AccessTransformers if the file exists
    val at = project(":common").file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }

    parchment {
        minecraftVersion = property("parchment_minecraft").toString()
        mappingsVersion = property("parchment_version").toString()
    }

    runs {
        configureEach {
            systemProperty("neoforge.enabledGameTestNamespaces", property("mod_id").toString())
            ideName.set("NeoForge ${name.replaceFirstChar { it.uppercase() }} (${project.path})")
        }

        register("client") {
            client()
        }

        register("data") {
            // matches your Groovy: "data" run name, but uses client lifecycle datagen
            clientData()

            programArguments.apply {
                addAll("--mod", property("mod_id").toString())
                add("--all")
                addAll("--output", file("src/generated/resources/").absolutePath)
                addAll("--existing", file("src/main/resources/").absolutePath)
            }
        }
    }

    mods {
        create(property("mod_id").toString()) {
            sourceSet(sourceSets["main"])
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

    // Also add as runtimeOnly to ensure they're available during dev runs
    runtimeOnly("io.noties:prism4j:2.0.0")
    runtimeOnly("org.jooq:joor:0.9.15")
    runtimeOnly("com.neovisionaries:nv-websocket-client:2.14")
    runtimeOnly("org.javassist:javassist:3.30.2-GA")

    // Extension jars to embed
    add(extensionJars.name, project(mapOf("path" to ":extension:graal", "configuration" to "archives")))
    add(extensionJars.name, project(mapOf("path" to ":extension:graal:js", "configuration" to "archives")))

    // Extension runtime dependencies for dev mode
    runtimeOnly(project(":extension:graal"))
    runtimeOnly(project(":extension:graal:js"))
}

// Collect extension jar names for dependencies property
fun getExtensionJarPaths(): String =
    extensionJars.files.joinToString(", ") { file ->
        "\"META-INF/jsmacrosdeps/${file.name}\""
    }

tasks.named<ProcessResources>("processResources") {
    // Embed extension jars into the final jar
    dependsOn(extensionJars)
    from(extensionJars) {
        into("META-INF/jsmacrosdeps")
    }

    // Add dependencies expansion for jsmacros.extension.json
    filesMatching("jsmacros.extension.json") {
        expand(mapOf("dependencies" to getExtensionJarPaths()))
    }
}

sourceSets.named("main") {
    resources.srcDir("src/generated/resources")
}
