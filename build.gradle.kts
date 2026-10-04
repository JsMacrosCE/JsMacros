import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import me.modmuss50.mpp.PublishModTask
import me.modmuss50.mpp.ReleaseType
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Zip
import org.gradle.internal.extensions.stdlib.capitalized
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

plugins {
    alias(libs.plugins.mod.publish)
}

repositories {
    mavenLocal()
    mavenCentral()

    exclusiveContent {
        forRepository {
            maven {
                name = "Sponge"
                url = uri("https://repo.spongepowered.org/repository/maven-public")
            }
        }
        filter {
            includeGroupAndSubgroups("org.spongepowered")
        }
    }

    exclusiveContent {
        forRepositories(
            maven {
                name = "ParchmentMC"
                url = uri("https://maven.parchmentmc.org/")
            },
            maven {
                name = "NeoForge"
                url = uri("https://maven.neoforged.net/releases")
            }
        )
        filter {
            includeGroup("org.parchmentmc.data")
        }
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "TerraformersMC"
                url = uri("https://maven.terraformersmc.com/releases/")
            }
        }
        filter {
            includeGroupAndSubgroups("com.terraformersmc")
        }
    }

    maven {
        name = "BlameJared"
        url = uri("https://maven.blamejared.com")
    }
    maven {
        name = "NeoForge"
        url = uri("https://maven.neoforged.net/releases")
    }
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net")
    }
}

// Check if this is a Stonecutter versioned project by looking at the project path
// Versioned projects have paths like ":1.21.10", ":common:1.21.10", etc.
val stonecutterExt = extensions.findByType(StonecutterBuildExtension::class.java)
val isVersionedProject = stonecutterExt != null && project.path != ":" && project.path.matches(Regex(".*:\\d+\\.\\d+.*"))

// Root-level directory setup
val distDir = layout.projectDirectory.dir("dist")
val distDirFile = distDir.asFile

repositories {
    mavenCentral()
}

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
        // Documentation is registered once by the controller. Its target-specific tasks
        // select sibling :common:<version> nodes rather than this node's empty subtree.

        tasks.register("createDistDocs", Copy::class.java) {
            group = "distribution"
            description = "Packages generated documentation into the dist directory"
            dependsOn("prepareDist", ":copyDocs${mcVersion.replace(".", "")}")
            from(rootProject.layout.buildDirectory.dir("docs/targets/$mcVersion"))
            into(distDirFile)
        }

        val baseJarTasks: Map<String, org.gradle.api.tasks.TaskProvider<Copy>> =
            loaders.associateWith { loader ->
                val loaderProject = project(":$loader")
                val sourceTaskName = loaderProject.modJarTaskName(loader)
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
                dependsOn("prepareDist", ":copyDocs${mcVersion.replace(".", "")}")
                destinationDirectory.set(distDir)
                archiveFileName.set("$modId-devkit-$mcVersion-${project.version}.zip")
                from(rootProject.layout.buildDirectory.dir("docs/targets/$mcVersion")) {
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
                    val loaderProject = project(":$loader")
                    val sourceTaskName = loaderProject.modJarTaskName(loader)

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
