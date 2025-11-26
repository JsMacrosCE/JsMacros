import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    alias(libs.plugins.architectury.plugin)
    alias(libs.plugins.architectury.loom)
    id("java")
    id("maven-publish")
    alias(libs.plugins.shadow)
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

val embeddedDependencyPrefixes = listOf(
    "joor",
    "javassist",
    "nv-websocket-client",
    "prism4j"
)

loom {
    accessWidenerPath.set(project(":common").file("src/main/resources/jsmacros.accesswidener"))

    mixin {
        defaultRefmapName.set("jsmacros-neoforge.refmap.json")
        useLegacyMixinAp.set(true)
    }

    runs.configureEach {
        mods {
            maybeCreate("jsmacros").apply {
                sourceSet("main")
                sourceSet("main", project(":common"))
            }
        }

    }

}

version = "2.0.0"
group = "xyz.wagyourtail"

// NeoForge dependencies
dependencies {
    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())
    neoForge(libs.neoforge)

    implementation(project(":common"))

    // Implementation dependencies
    implementation(libs.bundles.scripting.libs)
    implementation(libs.javassist)

    // Bundle runtime dependencies into the mod jar so the development run matches production
    include(libs.bundles.scripting.libs)
    include(libs.javassist)
}



// Extension system configuration
val jsmacrosExtensionInclude by configurations.creating

dependencies {
    jsmacrosExtensionInclude(project(":extension:graal")) { isTransitive = false }
    jsmacrosExtensionInclude(project(":extension:graal:js")) { isTransitive = false }
    jsmacrosExtensionInclude(project(":extension:graal:python")) { isTransitive = false }
}

val devExtensionsDir = layout.projectDirectory.dir("run/config/jsMacros/Extensions")

val copyDevExtensions = tasks.register<Copy>("copyDevExtensions") {
    dependsOn(":extension:graal:jar", ":extension:graal:js:jar", ":extension:graal:python:jar")
    from(jsmacrosExtensionInclude)
    into(devExtensionsDir)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.named<JavaExec>("runClient") {
    dependsOn(copyDevExtensions, copyEmbeddedDependencyJarsToClasses, writeJarJarMetadata)

    classpath += project.files({
        project.configurations.named("runtimeClasspath").get().resolve().filter { file ->
            embeddedDependencyPrefixes.any { prefix -> file.name.startsWith(prefix) }
        }
    })

    // Force Sponge Mixin to remap Fabric intermediary names to Mojmap when running NeoForge
    jvmArgs(
        "-Dmixin.env.remapRefMap=true",
        "-Dmixin.env.refMapRemappingEnv=intermediary",
        "-Dmixin.env.refMapRemappingDestinationEnv=named",
        "-Dmixin.env.disableRefMap=true"
    )

    doFirst {
        val embeddedPaths = classpath.files.filter { file ->
            embeddedDependencyPrefixes.any { prefix -> file.name.startsWith(prefix) }
        }
        if (embeddedPaths.isEmpty()) {
            logger.lifecycle("[runClient] No embedded dependency jars detected on classpath")
        } else {
            val formattedPaths = embeddedPaths.joinToString(separator = System.lineSeparator()) { it.absolutePath }
            logger.lifecycle("[runClient] Embedded dependency jars on classpath:\n$formattedPaths")
        }
    }
}



val commonMain = project(":common").extensions.getByType(SourceSetContainer::class.java).getByName("main")

val commonClassesDir = layout.buildDirectory.dir("generated/commonMain")
val syncCommonMainClasses = tasks.register("syncCommonMainClasses", Sync::class.java) {
    from(commonMain.output)
    into(commonClassesDir)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

val embeddedDependencyFiles = project.provider {
    configurations.runtimeClasspath.get().filter { file ->
        embeddedDependencyPrefixes.any { prefix -> file.name.startsWith(prefix) }
    }.toSet()
}

val embeddedDependencyArtifacts = project.provider {
    configurations.runtimeClasspath.get()
        .incoming
        .artifacts
        .artifacts
        .filter { artifact ->
            embeddedDependencyPrefixes.any { prefix -> artifact.file.name.startsWith(prefix) }
        }
        .sortedBy { artifact -> artifact.file.name }
}

val javaClassesDir = tasks.named<JavaCompile>("compileJava").flatMap { it.destinationDirectory }
val copyEmbeddedDependencyJarsToClasses = tasks.register("copyEmbeddedDependencyJarsToClasses", Sync::class.java) {
    dependsOn(tasks.named("compileJava"))
    from(embeddedDependencyFiles)
    into(javaClassesDir.map { it.dir("META-INF/jarjar") })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val writeJarJarMetadata = tasks.register("writeJarJarMetadata") {
    dependsOn(copyEmbeddedDependencyJarsToClasses)
    outputs.file(javaClassesDir.map { it.file("META-INF/jarjar/metadata.json") })

    doLast {
        val classesDirFile = javaClassesDir.get().asFile
        val metadataFile = classesDirFile.resolve("META-INF/jarjar/metadata.json")
        metadataFile.parentFile.mkdirs()

        val entries = embeddedDependencyArtifacts.get().mapNotNull { artifact ->
            val moduleId = artifact.id.componentIdentifier as? ModuleComponentIdentifier ?: return@mapNotNull null
            buildString {
                appendLine("{")
                appendLine("  \"identifier\": {")
                appendLine("    \"group\": \"${moduleId.group}\",")
                appendLine("    \"artifact\": \"${moduleId.module}\"")
                appendLine("  },")
                appendLine("  \"version\": {")
                appendLine("    \"range\": \"[${moduleId.version},)\",")
                appendLine("    \"artifactVersion\": \"${moduleId.version}\"")
                appendLine("  },")
                appendLine("  \"path\": \"META-INF/jarjar/${artifact.file.name}\",")
                appendLine("  \"isObfuscated\": false")
                append("}")
            }
        }

        val content = buildString {
            appendLine("{")
            appendLine("  \"jars\": [")
            if (entries.isNotEmpty()) {
                appendLine(entries.joinToString(",\n") { entry -> entry.prependIndent("    ") })
            }
            appendLine("  ]")
            appendLine("}")
        }

        metadataFile.writeText(content)
    }
}

extensions.getByType(SourceSetContainer::class.java).named("main") {
    val output = this.output
    output.dir(mapOf("builtBy" to syncCommonMainClasses), commonClassesDir)
}

tasks.named("classes") {
    dependsOn(syncCommonMainClasses, writeJarJarMetadata)
}

tasks.jar {
    dependsOn(writeJarJarMetadata)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(commonMain.output)
    from(commonClassesDir)
    from({ embeddedDependencyFiles.get() }) {
        into("META-INF/jarjar")
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }

    from(project(":common").tasks.processResources) {
        exclude("jsmacros.accesswidener")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}