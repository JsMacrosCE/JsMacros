import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import me.modmuss50.mpp.PublishModTask
import me.modmuss50.mpp.ReleaseType
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.CoreJavadocOptions
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.internal.extensions.stdlib.capitalized
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

plugins {
    id("me.modmuss50.mod-publish-plugin") version "1.1.0"
}

// Check if this is a Stonecutter versioned project by looking at the project path
// Versioned projects have paths like ":1.21.10", ":common:1.21.10", etc.
val stonecutterExt = extensions.findByType(StonecutterBuildExtension::class.java)
val isVersionedProject = stonecutterExt != null && project.path != ":" && project.path.matches(Regex(".*:\\d+\\.\\d+.*"))

// Root-level directory setup
val distDir = layout.projectDirectory.dir("dist")
val distDirFile = distDir.asFile
val docsBuildDir = layout.buildDirectory.dir("docs").get().asFile
val docletJarFile = layout.projectDirectory.file("buildSrc/build/libs/buildSrc.jar").asFile

// Root-level properties (available in root gradle.properties)
val modIdProvider = providers.gradleProperty("mod_id")
val channelProvider = providers.gradleProperty("channel").orElse("release")
val modVersionBaseProvider = providers.gradleProperty("mod_version").orElse(providers.gradleProperty("version"))
val betaNumberProvider = providers.gradleProperty("beta_number")
val alphaDateProvider = providers.gradleProperty("alpha_date")
val buildShaProvider = providers.gradleProperty("build_sha")

val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
val computedAlphaDateProvider = providers.provider {
    alphaDateProvider.orElse(
        providers.provider {
            ZonedDateTime.now(ZoneId.of("America/New_York")).format(dateFormatter)
        }
    ).get()
}

val computedBuildShaProvider = providers.provider {
    val sha = buildShaProvider.orElse(
        providers.provider { System.getenv("GITHUB_SHA") ?: "local" }
    ).get()
    sha.take(7)
}

val computedVersionProvider = providers.provider {
    val base = modVersionBaseProvider.get()
    when (channelProvider.get()) {
        "release" -> base
        "beta" -> {
            val betaNum = betaNumberProvider.orElse("1").get()
            "$base-beta.$betaNum"
        }
        "alpha" -> "$base-alpha.${computedAlphaDateProvider.get()}"
        else -> "$base-dev-${computedBuildShaProvider.get()}"
    }
}

// These are safe to access at the root level (from root gradle.properties)
val modId = modIdProvider.get()
val channel = channelProvider.get()
version = computedVersionProvider.get()

val loaders = listOf("fabric", "neoforge")

data class ExtensionSpec(val path: String, val extId: String)
val jsmExtensions: List<ExtensionSpec> = listOf(
    ExtensionSpec(path = ":extension:graal:python", extId = "graalpy")
)

// Only access version-specific properties if we're in a versioned subproject AND minecraft_version is available
val mcVersionProvider = providers.gradleProperty("minecraft_version")
val hasMinecraftVersion = mcVersionProvider.isPresent

if (isVersionedProject && hasMinecraftVersion) {
    val mcVersion = mcVersionProvider.get()
    val artifactBaseName = providers.provider { "$modId-$mcVersion-$channel-$version" }

    gradle.projectsEvaluated {
        val docsProjects = allprojects
            .filter { it.path.startsWith(":common") || it.path.startsWith(":extension") }
            .mapNotNull { p ->
                val ss = p.extensions.findByType(SourceSetContainer::class.java)
                if (ss == null) null else p
            }

        val mainSourceSets = docsProjects.map { p ->
            p.extensions.getByType(SourceSetContainer::class.java).named("main").get()
        }

        val documentationSources = files(mainSourceSets.map { it.allJava })
        val documentationClasspath = files(mainSourceSets.map { it.compileClasspath })

        tasks.register("generatePyDoc", Javadoc::class.java) {
            group = "documentation"
            description = "Generates the python documentation for the project"
            source(documentationSources)
            classpath = documentationClasspath
            destinationDir = File(docsBuildDir, "python/JsMacrosAC")
            options.doclet = "com.jsmacrosce.doclet.pydoclet.Main"
            options.docletpath = mutableListOf(docletJarFile)
            (options as CoreJavadocOptions).addStringOption("v", project.version.toString())
        }

        tasks.register("copyPyDoc", Copy::class.java) {
            group = "documentation"
            description = "Copies the python documentation to the build folder"
            dependsOn("generatePyDoc")
            from(rootProject.file("docs/python"))
            into(File(docsBuildDir, "python"))
        }

        tasks.register("generateTSDoc", Javadoc::class.java) {
            group = "documentation"
            description = "Generates the typescript documentation for the project"
            source(documentationSources)
            classpath = documentationClasspath
            destinationDir = File(docsBuildDir, "typescript/headers")
            options.doclet = "com.jsmacrosce.doclet.tsdoclet.Main"
            options.docletpath = mutableListOf(docletJarFile)
            (options as CoreJavadocOptions).addStringOption("v", project.version.toString())
        }

        tasks.register("copyTSDoc", Copy::class.java) {
            group = "documentation"
            description = "Copies the typescript files to the build folder"
            dependsOn("generateTSDoc")
            from(rootProject.file("docs/typescript"))
            into(File(docsBuildDir, "typescript"))
        }

        tasks.register("generateWebDoc", Javadoc::class.java) {
            group = "documentation"
            description = "Generates the web documentation for the project"
            source(documentationSources)
            classpath = documentationClasspath
            destinationDir = File(docsBuildDir, "web")
            options.doclet = "com.jsmacrosce.doclet.webdoclet.Main"
            options.docletpath = mutableListOf(docletJarFile)
            (options as CoreJavadocOptions).addStringOption("v", project.version.toString())
            (options as CoreJavadocOptions).addStringOption("mcv", mcVersion)
            (options as StandardJavadocDocletOptions).links(
                "https://docs.oracle.com/javase/8/docs/api/",
                "https://www.javadoc.io/doc/org.slf4j/slf4j-api/1.7.30/",
                "https://javadoc.io/doc/com.neovisionaries/nv-websocket-client/latest/"
            )
        }

        tasks.register("copyWebDoc", Copy::class.java) {
            group = "documentation"
            description = "Copies the web documentation to the build folder"
            dependsOn("generateWebDoc")
            from(rootProject.file("docs/web"))
            into(File(docsBuildDir, "web"))
            inputs.property("version", project.version.toString())
            filesMatching("index.html") {
                expand(mapOf("version" to project.version.toString()))
            }
        }

        tasks.register("createDistDocs", Copy::class.java) {
            group = "distribution"
            description = "Packages generated documentation into the dist directory"
            dependsOn("prepareDist", "copyPyDoc", "copyTSDoc", "copyWebDoc")
            from(docsBuildDir)
            into(distDirFile)
        }

        val baseJarTasks: Map<String, org.gradle.api.tasks.TaskProvider<Copy>> =
            loaders.associateWith { loader ->
                val loaderProject = project(":$loader")
                val sourceTaskName = if (loader == "fabric") "remapJar" else "jar"
                val taskName = "package${loader.replaceFirstChar { it.uppercase() }}ModJar"

                tasks.register(taskName, Copy::class.java) {
                    group = "distribution"
                    description = "Packages $loader mod jar into dist"
                    dependsOn("prepareDist", loaderProject.tasks.named(sourceTaskName))

                    val jarFile = loaderProject.tasks.named(sourceTaskName).flatMap {
                        (it as org.gradle.api.tasks.bundling.AbstractArchiveTask).archiveFile
                    }

                    from(jarFile)
                    rename { "$modId-$mcVersion-$loader-${project.version}.jar" }
                    into(distDirFile)
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                }
            }

        val extensionJarTasks = jsmExtensions.map { ext: ExtensionSpec ->
            tasks.register("package${ext.extId.replaceFirstChar { ch -> ch.uppercase() }}Extension", Copy::class.java) {
                group = "distribution"
                description = "Packages ${ext.extId} extension"

                val extJar = project(ext.path).tasks.named("jar").flatMap {
                    (it as org.gradle.api.tasks.bundling.AbstractArchiveTask).archiveFile
                }

                dependsOn("prepareDist", extJar)
                from(extJar)
                rename { "$modId-ext-${ext.extId}-$mcVersion-${project.version}.jar" }
                into(File(distDirFile, "extensions"))
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }
        }

        val devkitTasks = listOf(
            tasks.register("packageDevkit", Zip::class.java) {
                group = "distribution"
                description = "Packages devkit bundle"
                dependsOn("prepareDist", "copyPyDoc", "copyTSDoc", "copyWebDoc")
                destinationDirectory.set(distDir)
                archiveFileName.set("$modId-devkit-$mcVersion-${project.version}.zip")
                from(docsBuildDir) {
                    include("web/**")
                    include("typescript/**")
                    include("python/**")
                }
            }
        )

        val extensionPackTasks = listOf(
            tasks.register("packageExtensionsPack", Zip::class.java) {
                group = "distribution"
                description = "Bundles all extensions into the config/jsMacros/extensions layout"
                dependsOn(
                    "prepareDist",
                    extensionJarTasks,
                    "createDistExtensions",
                    "createDistDocs",
                    baseJarTasks.values
                )
                destinationDirectory.set(distDir)
                archiveFileName.set("$modId-extensions-$mcVersion-${project.version}.zip")
                into("config/jsMacros/extensions") {
                    from(File(distDirFile, "extensions")) {
                        include("*-${project.version}.jar")
                    }
                }
            }
        )

        tasks.register("createDistMods") {
            group = "distribution"
            description = "Packages loader specific jars into the dist directory"
            dependsOn(baseJarTasks.values)
        }

        tasks.register("createDistExtensions") {
            group = "distribution"
            description = "Packages standalone extensions into the dist directory"
            dependsOn(extensionJarTasks)
        }

        tasks.register("createDist") {
            group = "distribution"
            description = "Assembles documentation, mods, extensions, devkits, and sources into dist/"
            dependsOn(
                "createDistDocs",
                "createDistMods",
                "createDistExtensions",
                devkitTasks,
                extensionPackTasks
            )
        }

        tasks.register("printArtifactName") {
            group = "distribution"
            description = "Prints the canonical artifact name for CI workflows"
            doLast {
                println(artifactBaseName.get())
            }
        }

        tasks.register("printMinecraftVersion") {
            group = "distribution"
            description = "Prints the targeted Minecraft version for CI workflows"
            doLast {
                println(mcVersion)
            }
        }

        val releaseType = when (channel) {
            "release" -> ReleaseType.STABLE
            "beta" -> ReleaseType.BETA
            else -> ReleaseType.ALPHA
        }

        val modrinthProjectId = providers.gradleProperty("modrinth_id")
            .orElse(providers.environmentVariable("MODRINTH_PROJECT"))
        val modrinthToken = providers.gradleProperty("modrinth_token")
            .orElse(providers.environmentVariable("MODRINTH_TOKEN"))
        val githubRepo = providers.gradleProperty("github_repository")
            .orElse(providers.environmentVariable("GITHUB_REPOSITORY"))
            .orElse("JsMacrosCE/JsMacros")
        val githubToken = providers.environmentVariable("GITHUB_TOKEN")
        val githubCommitish = providers.environmentVariable("GITHUB_SHA").orElse("main")
        val githubTagName = providers.provider { "v${project.version}" }
        val targetMcVersion = mcVersion

        fun modrinthChangelog(loader: String): String = """
            JsMacrosCE ${project.version} for $loader on Minecraft $targetMcVersion.
            Source: https://github.com/JsMacrosCE/JsMacros
        """.trimIndent()

        fun githubChangelog(): String = """
            ${releaseType.toString().lowercase(Locale.getDefault()).capitalized()} Release for JsMacrosCE ${project.version}.
            Built game version: $targetMcVersion
            Alpha, beta, and release builds are available on Modrinth: https://modrinth.com/mod/jsmacrosce/versions
        """.trimIndent()


        publishMods {
            val publishModrinth = modrinthToken.isPresent && channel != "dev"

            if (publishModrinth) {
                loaders.forEach { loader ->
                    val platformName = "modrinth${loader.replaceFirstChar { it.uppercase() }}${targetMcVersion.replace(".", "")}"
                    val sourceTaskName = if (loader == "fabric") "remapJar" else "jar"
                    val loaderProject = project(":$loader")

                    modrinth(platformName) {
                        projectId.set(modrinthProjectId)
                        accessToken.set(modrinthToken)
                        minecraftVersions.add(targetMcVersion)
                        modLoaders.set(listOf(loader))

                        version.set("${project.version}+$targetMcVersion-$loader")
                        displayName.set("JsMacrosCE ${project.version} ($loader $targetMcVersion)")
                        changelog.set(modrinthChangelog(loader))
                        type.set(releaseType)
                        file.set(
                            loaderProject.tasks.named(sourceTaskName, AbstractArchiveTask::class.java)
                                .flatMap { it.archiveFile }
                        )
                    }
                }
            }

            github("githubRelease") {
                accessToken.set(githubToken)
                repository.set(githubRepo)
                commitish.set(githubCommitish)
                tagName.set(githubTagName)
                displayName.set("JsMacrosCE ${project.version}")
                changelog.set(githubChangelog())
                type.set(releaseType)
                allowEmptyFiles.set(true)
                additionalFiles.from(
                    providers.provider {
                        distDir.asFileTree.matching {
                            include("jsmacrosce-*-fabric-${project.version}.jar")
                            include("jsmacrosce-*-neoforge-${project.version}.jar")
                            include("jsmacrosce-devkit-*-${project.version}.zip")
                            include("jsmacrosce-extensions-*-${project.version}.zip")
                            include("extensions/jsmacrosce-ext-*-${project.version}.jar")
                        }
                    }
                )
            }
        }

        tasks.named("publishMods") {
            dependsOn("createDist")
        }

        tasks.withType(PublishModTask::class.java).configureEach {
            dependsOn("createDist")
        }
    }
}

// Register root-level tasks unconditionally
tasks.register("prepareDist") {
    group = "distribution"
    description = "Cleans and recreates the dist directory"
    doLast {
        project.delete(distDirFile)
        distDirFile.mkdirs()
    }
}

tasks.register("printVersion") {
    group = "distribution"
    description = "Prints the computed project version for CI workflows"
    doLast {
        println(project.version)
    }
}
