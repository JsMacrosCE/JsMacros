import me.modmuss50.mpp.PublishModTask
import me.modmuss50.mpp.ReleaseType
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Zip
import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.api.GradleException
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

plugins {
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") apply false
    id("fabric-loom") apply false
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

val IS_CI = System.getenv("CI") == "true"

if (IS_CI) stonecutter active null
else stonecutter active file("stonecutter.active") /* [SC] DO NOT EDIT */

// Root-level distribution setup
val distDir = layout.projectDirectory.dir("dist")
val distDirFile = distDir.asFile
val docsBuildDir = layout.buildDirectory.dir("docs").get().asFile

repositories {
    mavenCentral()
}

// Root-level properties
val modIdProvider = providers.gradleProperty("mod_id")
val channelProvider = providers.gradleProperty("channel").orElse("release")
val modVersionBaseProvider = providers.gradleProperty("mod_version").orElse(providers.gradleProperty("version"))
val betaNumberProvider = providers.gradleProperty("beta_number")
val alphaDateProvider = providers.gradleProperty("alpha_date")
val buildShaProvider = providers.gradleProperty("build_sha")

val isDevChannel = channelProvider.get() == "dev"

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

// Safe root-level access
val modId = modIdProvider.get()
val channel = channelProvider.get()
version = computedVersionProvider.get()

val supportedVersions = listOf("1.21.5", "1.21.8", "1.21.10", "1.21.11", "26.1.2")
val mcVersionsToBuild = if (IS_CI) supportedVersions else listOf("26.1.2")
val mcVersion = mcVersionsToBuild.first() // for backward compatibility

val loaders = listOf("fabric", "neoforge")

data class ExtensionSpec(val path: String, val extId: String)
val jsmExtensions: List<ExtensionSpec> = listOf(
    ExtensionSpec(path = ":extension:graal:python", extId = "graalpy")
)

val artifactBaseName = providers.provider { "$modId-$mcVersion-$channel-$version" }

// Register distribution tasks
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

if (project == rootProject) {
    val activeStonecutterVersion = providers.fileContents(layout.projectDirectory.file("stonecutter.active"))
        .asText
        .map { text ->
            text.trim().ifEmpty {
                throw GradleException("stonecutter.active is empty; set an active version first")
            }
        }

    val activeVersion = activeStonecutterVersion.get()
    val fabricRunClientPath = ":fabric:$activeVersion:runClient"
    val neoforgeRunClientPath = ":neoforge:$activeVersion:runClient"

    tasks.register("runFabricClient") {
        group = "run"
        description = "Runs the Fabric client for the active Stonecutter version"
        dependsOn(fabricRunClientPath)
    }

    tasks.register("runNeoforgeClient") {
        group = "run"
        description = "Runs the NeoForge client for the active Stonecutter version"
        dependsOn(neoforgeRunClientPath)
    }
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
    description = "Prints the targeted Minecraft version(s) for CI workflows"
    doLast {
        println(mcVersionsToBuild.joinToString(","))
    }
}

// Configure distribution tasks after projects are evaluated
gradle.projectsEvaluated {
    registerDocumentationTasks(supportedVersions, mcVersionsToBuild)

    tasks.register("createDistDocs", Copy::class.java) {
        group = "distribution"
        description = "Packages generated documentation into the dist directory"
        dependsOn("prepareDist", "copyPyDoc", "copyTSDoc", "copyWebDoc", "copyVitepressDoc",
            mcVersionsToBuild.map { "copyDocs${it.replace(".", "")}" })
        mcVersionsToBuild.forEach { minecraft ->
            from(File(docsBuildDir, "targets/$minecraft")) { into("docs/$minecraft") }
        }
        from(File(docsBuildDir, "vitepress")) {
            into("vitepress")
            exclude("node_modules/**", ".vitepress/cache/**", ".vitepress/dist/**")
        }
        into(distDirFile)
    }

    // Package loader-specific jars
    val baseJarTasks: Map<String, org.gradle.api.tasks.TaskProvider<Copy>> =
        loaders.flatMap { loader ->
            mcVersionsToBuild.map { version ->
                val loaderProject = project(":$loader:$version")
                val sourceTaskName = loaderProject.modJarTaskName(loader)
                val taskName = "package${loader.replaceFirstChar { it.uppercase() }}ModJar${version.replace(".", "")}"

                tasks.register(taskName, Copy::class.java) {
                    group = "distribution"
                    description = "Packages $loader mod jar for $version into dist"
                    dependsOn("prepareDist", loaderProject.tasks.named(sourceTaskName))

                    val jarFile = loaderProject.tasks.named(sourceTaskName).flatMap {
                        (it as org.gradle.api.tasks.bundling.AbstractArchiveTask).archiveFile
                    }

                    from(jarFile)
                    rename { "$modId-$version-$loader-${project.version}.jar" }
                    into(distDirFile)
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                }
            }
        }.associateBy { it.name }

    // Package extensions
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

    val devkitTasks = mcVersionsToBuild.map { version ->
        tasks.register("packageDevkit${version.replace(".", "")}", Zip::class.java) {
            group = "distribution"
            description = "Packages devkit bundle for $version"
            dependsOn("prepareDist", "copyDocs${version.replace(".", "")}")
            destinationDirectory.set(distDir)
            archiveFileName.set("$modId-devkit-$version-${project.version}.zip")
            from(File(docsBuildDir, "targets/$version")) {
                include("web/**")
                include("typescript/**")
                include("python/**")
            }
        }
    }

    val extensionPackTasks = mcVersionsToBuild.map { version ->
        tasks.register("packageExtensionsPack${version.replace(".", "")}", Zip::class.java) {
            group = "distribution"
            description = "Bundles all extensions into the config/jsMacros/extensions layout for $version"
            dependsOn(
                "prepareDist",
                extensionJarTasks,
                "createDistExtensions",
                "createDistDocs",
                baseJarTasks.values
            )
            destinationDirectory.set(distDir)
            archiveFileName.set("$modId-extensions-$version-${project.version}.zip")
            into("config/jsMacros/extensions") {
                from(File(distDirFile, "extensions")) {
                    include("*-${project.version}.jar")
                }
            }
        }
    }

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

    fun modrinthChangelog(loader: String, targetMcVersion: String): String = """
        JsMacrosCE ${project.version} for $loader on Minecraft $targetMcVersion.
        Source: https://github.com/JsMacrosCE/JsMacros
    """.trimIndent()

    fun githubChangelog(): String = """
        ${releaseType.toString().lowercase(Locale.getDefault()).capitalized()} Release for JsMacrosCE ${project.version}.
        Supported Minecraft versions: ${mcVersionsToBuild.joinToString(", ")}.
        Alpha, beta, and release builds are available on Modrinth: https://modrinth.com/mod/jsmacrosce/versions
    """.trimIndent()

    publishMods {
        val publishModrinth = modrinthToken.isPresent && channel != "dev"

        if (publishModrinth) {
            mcVersionsToBuild.forEach { targetMcVersion ->
                val mcSegment = targetMcVersion.replace(".", "")
                loaders.forEach { loader ->
                    val platformName = "modrinth${loader.replaceFirstChar { it.uppercase() }}$mcSegment"
                    val loaderProject = project(":$loader:$targetMcVersion")
                    val sourceTaskName = loaderProject.modJarTaskName(loader)

                    modrinth(platformName) {
                        projectId.set(modrinthProjectId)
                        accessToken.set(modrinthToken)
                        minecraftVersions.add(targetMcVersion)
                        modLoaders.set(listOf(loader))

                        version.set("${project.version}+$targetMcVersion-$loader")
                        displayName.set("JsMacrosCE ${project.version} ($loader $targetMcVersion)")
                        changelog.set(modrinthChangelog(loader, targetMcVersion))
                        type.set(releaseType)
                        file.set(
                            loaderProject.tasks.named(sourceTaskName, AbstractArchiveTask::class.java)
                                .flatMap { it.archiveFile }
                        )
                    }
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
