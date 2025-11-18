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
    minecraft("com.mojang:minecraft:${libs.versions.minecraft.get()}")
    mappings(loom.officialMojangMappings())
    neoForge("net.neoforged:neoforge:${libs.versions.neoforge.get()}")

    implementation(project(":common"))

    // Implementation dependencies
    implementation(libs.prism4j)
    implementation(libs.nv.websocket.client)
    implementation(libs.javassist)
    implementation(libs.joor)

    // Bundle runtime dependencies into the mod jar so the development run matches production
    include(libs.prism4j)
    include(libs.nv.websocket.client)
    include(libs.javassist)
    include(libs.joor)
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
    maven("https://maven.minecraftforge.net/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.architectury.dev/")
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://jitpack.io")
}

// Subproject configuration for all modules
subprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.terraformersmc.com/releases/")
        maven("https://jitpack.io")
    }

    // Apply Java toolchain consistently
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

// Build coordination tasks
tasks.register("buildAll") {
    group = "build"
    description = "Build all available modules"

    // Only build modules that are currently enabled
    dependsOn(":common:build")
    dependsOn(":extension:build")
    dependsOn(":site:build")
    dependsOn(":fabric:build")
    dependsOn(":neoforge:build")
}

tasks.register("cleanAll") {
    group = "build"
    description = "Clean all modules"

    dependsOn(tasks.named("clean"))
    dependsOn(":common:clean")
    dependsOn(":site:clean")
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


// Distribution task
tasks.register<Sync>("createDist") {
    group = "build"
    description = "Creates all files for the distribution of the project"

    // Use built-in directory cleaning with proper up-to-date checking
    into(layout.buildDirectory.dir("dist"))

    // Collect extension JARs
    from(project(":extension:graal").tasks.named("jar")) {
        into("extensions")
        rename { "${project.name}-${it}" }
    }
    from(project(":extension:graal:js").tasks.named("jar")) {
        into("extensions")
        rename { "${project.name}-${it}" }
    }
    from(project(":extension:graal:python").tasks.named("jar")) {
        into("extensions")
        rename { "${project.name}-${it}" }
    }

    // Copy TypeScript definitions for documentation
    from("docs/typescript") {
        into("typescript")
        include("**/*.d.ts")
    }

    // Copy gradle.properties for CI/CD
    from("gradle.properties")

    doLast {
        val distDir = layout.buildDirectory.dir("dist").get().asFile
        println("Distribution created in ${distDir.absolutePath}")
        println("Contents:")
        distDir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                println("  ${file.name}/")
                file.listFiles()?.forEach { subFile ->
                    println("    - ${subFile.name}")
                }
            } else {
                println("  - ${file.name}")
            }
        }
    }
}

// Add createDist to buildAll task
tasks.named("buildAll") {
    dependsOn("createDist")
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