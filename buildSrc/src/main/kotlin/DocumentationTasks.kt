import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.CoreJavadocOptions
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.*
import java.io.File

/** Called after evaluation: one source/classpath/toolchain tuple per Minecraft snapshot. */
fun Project.registerDocumentationTasks(supportedVersions: List<String>, defaultTargets: List<String>) {
    val singular = providers.gradleProperty("docsMinecraftVersion").orNull
    val plural = providers.gradleProperty("docsMinecraftVersions").orNull
    if (singular != null && plural != null) {
        throw GradleException("Use either docsMinecraftVersion or docsMinecraftVersions, not both")
    }
    val requested = singular ?: plural
    val targets = when (requested) {
        null -> defaultTargets
        "all" -> supportedVersions
        else -> requested.split(',').map(String::trim).distinct()
    }
    if (targets.isEmpty() || targets.any { it !in supportedVersions }) {
        throw GradleException("Unsupported documentation targets $targets; choose from $supportedVersions")
    }
    val docsRoot = layout.buildDirectory.dir("docs").get().asFile
    val modVersion = version.toString()
    val docletJar = rootProject.file("buildSrc/build/libs/buildSrc.jar")
    val extensionProjects = rootProject.allprojects.filter {
        it.path.startsWith(":extension") && it.extensions.findByType<SourceSetContainer>() != null
    }
    val extensionSources = files(extensionProjects.map { it.extensions.getByType<SourceSetContainer>()["main"].allJava })
    val kinds = linkedMapOf("Py" to "pydoclet", "TS" to "tsdoclet", "Web" to "webdoclet", "Vitepress" to "mddoclet")

    supportedVersions.forEach { minecraft ->
        val suffix = minecraft.replace(".", "")
        val common = rootProject.project(":common:$minecraft")
        val commonMain = common.extensions.getByType<SourceSetContainer>()["main"]
        val stonecutter = common.extensions.getByType<StonecutterBuildExtension>()
        val java = common.extensions.getByType<JavaPluginExtension>()
        val tools = common.extensions.getByType<JavaToolchainService>()
        val docletLibraries = common.configurations.detachedConfiguration(
            common.dependencies.create("com.google.code.gson:gson:2.14.0"))
        // Extensions compile against active common in ordinary builds. Only their external
        // libraries belong here: all extension/common project types are supplied as sources.
        val extensionLibraries = common.configurations.create("documentationLibraries") {
            isCanBeResolved = true
            isCanBeConsumed = false
            attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
            attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, java.toolchain.languageVersion.get().asInt())
        }
        extensionProjects.forEach { extension ->
            extension.configurations.getByName("compileClasspath").allDependencies
                .filterNot { it is ProjectDependency }
                .forEach { common.dependencies.add(extensionLibraries.name, it) }
        }
        val targetRoot = File(docsRoot, "targets/$minecraft")
        kinds.forEach { (kind, doclet) ->
            val generateName = "generate${kind}Doc$suffix"
            val output = when (kind) {
                "Py" -> "python/JsMacrosAC"
                "TS" -> "typescript/headers"
                "Web" -> "web"
                else -> "vitepress"
            }
            // Resolve compile configurations from their owning project. Gradle 9 rejects
            // a root task resolving a sibling's configuration without its project lock.
            val generate = common.tasks.register<Javadoc>("generate${kind}Doc") {
                group = "documentation"
                description = "Generates $kind documentation for Minecraft $minecraft"
                // Always use the generated view, including when this target is active.
                source(fileTree(stonecutter.tasks.generatedSourcesDir.dir("main/java")))
                source(extensionSources)
                classpath = commonMain.compileClasspath + extensionLibraries
                dependsOn(stonecutter.tasks.generate.getOrThrow("main"), common.tasks.named("createMinecraftArtifacts"))
                javadocTool.set(tools.javadocToolFor(java.toolchain))
                destinationDir = File(targetRoot, output)
                inputs.property("minecraftVersion", minecraft)
                inputs.property("modVersion", modVersion)
                options.doclet = "com.jsmacrosce.doclet.core.$doclet.Main"
                options.docletpath = (listOf(docletJar) + docletLibraries.files).toMutableList()
                (options as CoreJavadocOptions).addStringOption("v", modVersion)
                if (kind == "Web" || kind == "Vitepress") {
                    (options as CoreJavadocOptions).addStringOption("mcv", minecraft)
                    (options as StandardJavadocDocletOptions).links(
                        "https://docs.oracle.com/en/java/javase/${java.toolchain.languageVersion.get().asInt()}/docs/api/",
                        "https://www.slf4j.org/apidocs-2.0.17/",
                        "https://takahikokawasaki.github.io/nv-websocket-client/"
                    )
                }
                // This destination contains only this task's generated artifacts. Removed
                // classes must not survive a successful regeneration as stale API pages.
                doFirst { project.delete(destinationDir) }
            }
            tasks.register(generateName) {
                group = "documentation"
                description = "Generates $kind documentation for Minecraft $minecraft"
                dependsOn(generate)
            }
            if (kind != "Vitepress") {
                val directory = when (kind) { "Py" -> "python"; "TS" -> "typescript"; else -> "web" }
                tasks.register<Copy>("copy${kind}Doc$suffix") {
                    group = "documentation"
                    description = "Copies $kind support files for Minecraft $minecraft"
                    dependsOn(generateName)
                    from(rootProject.file("docs/$directory"))
                    into(File(targetRoot, directory))
                    if (kind == "Web") {
                        inputs.property("modVersion", modVersion)
                        inputs.property("minecraftVersion", minecraft)
                        filesMatching("index.html") { expand(mapOf("version" to modVersion)) }
                    }
                }
            }
        }
        tasks.register("generateDocs$suffix") {
            group = "documentation"
            description = "Generates all four documentation formats for Minecraft $minecraft"
            dependsOn(kinds.keys.map { "generate${it}Doc$suffix" })
        }
        tasks.register("copyDocs$suffix") {
            group = "documentation"
            dependsOn("generateVitepressDoc$suffix", "copyPyDoc$suffix", "copyTSDoc$suffix", "copyWebDoc$suffix")
        }
    }
    kinds.keys.forEach { kind ->
        tasks.register("generate${kind}Doc") {
            group = "documentation"
            description = "Generates $kind documentation for the selected Minecraft targets"
            dependsOn(targets.map { "generate${kind}Doc${it.replace(".", "")}" })
        }
        if (kind != "Vitepress") {
            tasks.register("copy${kind}Doc") {
                group = "documentation"
                dependsOn(targets.map { "copy${kind}Doc${it.replace(".", "")}" })
            }
        }
    }
    tasks.register<Sync>("copyVitepressDoc") {
        group = "documentation"
        description = "Assembles a VitePress site with a selector for the selected Minecraft targets"
        dependsOn("generateVitepressDoc")
        from(rootProject.file("docs/vitepress")) {
            exclude("node_modules/**", ".vitepress/cache/**", ".vitepress/dist/**", "content/*/sidebar-data.json")
        }
        targets.forEach { minecraft ->
            from(File(docsRoot, "targets/$minecraft/vitepress/content")) { into("content") }
        }
        into(File(docsRoot, "vitepress"))
        // Keep installed dependencies/build caches between site assemblies.
        preserve { include("node_modules/**", ".vitepress/cache/**", ".vitepress/dist/**") }
    }
    tasks.register("generateDocsAll") {
        group = "documentation"
        description = "Generates isolated documentation snapshots for all supported Minecraft versions"
        dependsOn(supportedVersions.map { "generateDocs${it.replace(".", "")}" })
    }
}
