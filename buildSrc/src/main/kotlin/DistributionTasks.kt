import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.*
import java.io.File
import java.util.zip.ZipFile

abstract class PackageReleaseJar : DefaultTask() {
    @get:InputFile
    abstract val sourceFile: RegularFileProperty

    @get:OutputFile
    abstract val destinationFile: RegularFileProperty

    @TaskAction
    fun copyJar() {
        val destination = destinationFile.get().asFile
        destination.parentFile.mkdirs()
        sourceFile.get().asFile.copyTo(destination, overwrite = true)
    }
}

// Only extensions with a shared binary belong here. MC/loader-specific extensions
// must declare their own target artifacts instead of renaming this shared JAR.
data class SharedExtensionArtifact(val path: String, val id: String)

data class DistributionArtifacts(
    val releaseFiles: ConfigurableFileCollection,
    val verify: TaskProvider<Task>
)

fun Project.registerDistributionTasks(
    modId: String,
    supportedVersions: List<String>,
    minecraftVersions: List<String>,
    documentationVersions: List<String>,
    loaders: List<String>,
    extensions: List<SharedExtensionArtifact>,
    distDirectory: Directory
): DistributionArtifacts {
    require(supportedVersions.map { it.replace(".", "") }.distinct().size == supportedVersions.size) {
        "Minecraft targets must have distinct packaging task names"
    }
    require(minecraftVersions.distinct().size == minecraftVersions.size && supportedVersions.containsAll(minecraftVersions)) {
        "Distribution targets must be distinct supported Minecraft versions"
    }
    require(loaders.distinct().size == loaders.size) { "Duplicate distribution loaders" }
    require(extensions.map { it.id.lowercase() }.distinct().size == extensions.size) { "Duplicate shared extension IDs" }
    val releaseVersion = version.toString()
    val docsDirectory = layout.buildDirectory.dir("docs")
    val prepareDist = tasks.named("prepareDist")

    // Explicit output files avoid overlapping Copy-task outputs and stale dist globs.
    val modTasks = supportedVersions.associateWith { minecraft ->
        loaders.map { loader ->
            val loaderProject = project(":$loader:$minecraft")
            val jar = loaderProject.tasks.named<AbstractArchiveTask>(loaderProject.modJarTaskName(loader))
            tasks.register<PackageReleaseJar>("package${loader.replaceFirstChar(Char::uppercase)}ModJar${minecraft.replace(".", "")}") {
                group = "distribution"
                description = "Packages $loader mod jar for $minecraft into dist"
                dependsOn(prepareDist)
                sourceFile.set(jar.flatMap { it.archiveFile })
                destinationFile.set(distDirectory.file("$modId-$minecraft-$loader-$releaseVersion.jar"))
            }
        }
    }
    val selectedModTasks = minecraftVersions.flatMap { modTasks.getValue(it) }

    val extensionTasks = extensions.map { extension ->
        val jar = project(extension.path).tasks.named<AbstractArchiveTask>("jar")
        tasks.register<PackageReleaseJar>("package${extension.id.replaceFirstChar(Char::uppercase)}Extension") {
            group = "distribution"
            description = "Packages the shared ${extension.id} extension"
            dependsOn(prepareDist)
            sourceFile.set(jar.flatMap { it.archiveFile })
            destinationFile.set(distDirectory.file("extensions/$modId-ext-${extension.id}-$releaseVersion.jar"))
        }
    }

    val devkit = tasks.register<Zip>("packageDevkit") {
        group = "distribution"
        description = "Packages versioned typings, legacy HTML and the rendered VitePress site"
        dependsOn(prepareDist, "buildVitepressSite", documentationVersions.map { "copyDocs${it.replace(".", "")}" })
        destinationDirectory.set(distDirectory)
        archiveFileName.set("$modId-devkit-$releaseVersion.zip")
        duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.FAIL
        from(rootProject.file("docs/devkit")) {
            include("README.md", "serve-docs.py")
        }
        documentationVersions.forEach { minecraft ->
            from(docsDirectory.map { it.dir("targets/$minecraft") }) {
                into(minecraft)
                include("web/**", "typescript/**", "python/**")
            }
        }
        from(docsDirectory.map { it.dir("vitepress/.vitepress/dist") }) {
            into("vitepress")
        }
        doLast {
            verifyDevkit(archiveFile.get().asFile, documentationVersions, releaseVersion)
        }
    }

    tasks.register("createDistMods") {
        group = "distribution"
        description = "Packages loader-specific mod jars into dist"
        dependsOn(selectedModTasks)
    }
    tasks.register("createDistExtensions") {
        group = "distribution"
        description = "Packages standalone shared extensions into dist"
        dependsOn(extensionTasks)
    }

    val releaseFiles = objects.fileCollection().from(
        selectedModTasks.map { task -> task.flatMap { it.destinationFile } },
        extensionTasks.map { task -> task.flatMap { it.destinationFile } },
        devkit.flatMap { it.archiveFile }
    )
    val verify = tasks.register("verifyDistribution") {
        group = "verification"
        description = "Checks release assets, complete devkit snapshots and shared extension bytecode"
        dependsOn(selectedModTasks, extensionTasks, devkit)
        inputs.files(releaseFiles)
        doLast {
            val expectedCount = minecraftVersions.size * loaders.size + extensions.size + 1
            check(releaseFiles.files.size == expectedCount) { "Expected $expectedCount distinct release assets" }
            check(releaseFiles.files.map(File::getName).distinct().size == expectedCount) { "Duplicate release asset names" }
            releaseFiles.forEach { check(it.isFile && it.length() > 0) { "Missing release asset: $it" } }
            verifyDevkit(devkit.get().archiveFile.get().asFile, documentationVersions, releaseVersion)
            extensionTasks.forEach { verifySharedExtension(it.get().destinationFile.get().asFile) }
            logger.lifecycle("Verified $expectedCount release assets for ${minecraftVersions.joinToString()}")
        }
    }
    tasks.register("createDist") {
        group = "distribution"
        description = "Assembles mods, shared extensions and one combined devkit into dist"
        dependsOn("createDistDocs", "createDistSite", verify)
    }
    // Documentation-only packaging may select a subset, but a full distribution
    // must fail before generation/compilation if that subset omits a mod target.
    gradle.taskGraph.whenReady {
        if (hasTask(verify.get())) {
            check(documentationVersions.containsAll(minecraftVersions)) {
                "Distribution documentation must cover every packaged Minecraft target: $minecraftVersions"
            }
        }
    }
    return DistributionArtifacts(releaseFiles, verify)
}

fun verifyDevkit(archive: File, minecraftVersions: List<String>, releaseVersion: String) {
    ZipFile(archive).use { zip ->
        fun requireEntry(name: String) {
            check(zip.getEntry(name)?.let { !it.isDirectory && it.size > 0 } == true) {
                "Incomplete devkit: missing $name in ${archive.name}"
            }
        }
        requireEntry("README.md")
        requireEntry("serve-docs.py")
        requireEntry("vitepress/index.html")
        check(zip.entries().asSequence().any { it.name.startsWith("vitepress/assets/") && !it.isDirectory }) {
            "Incomplete devkit: missing rendered VitePress assets"
        }
        minecraftVersions.forEach { minecraft ->
            requireEntry("$minecraft/web/index.html")
            requireEntry("$minecraft/typescript/tsconfig.json")
            requireEntry("$minecraft/typescript/headers/Graal.d.ts")
            requireEntry("$minecraft/typescript/headers/JsMacros-$releaseVersion.d.ts")
            requireEntry("$minecraft/python/README.md")
            check(zip.entries().asSequence().any {
                it.name.startsWith("$minecraft/python/JsMacrosAC/") && it.name.endsWith(".py") && it.size > 0
            }) { "Incomplete devkit: missing Python stubs for $minecraft" }
            requireEntry("vitepress/$releaseVersion/$minecraft/index.html")
        }
        check(zip.entries().asSequence().none {
            it.name.contains("node_modules/") || it.name.contains(".vitepress/") || it.name.endsWith(".jar")
        }) { "Devkits must contain rendered docs, not build caches, site sources or extension jars" }
    }
}

fun verifySharedExtension(archive: File) {
    ZipFile(archive).use { zip ->
        check(zip.getEntry("META-INF/services/com.jsmacrosce.jsmacros.core.extensions.Extension") != null) {
            "Missing extension service descriptor in ${archive.name}"
        }
        val classes = zip.entries().asSequence().filter { it.name.endsWith(".class") }.toList()
        check(classes.isNotEmpty()) { "No extension classes in ${archive.name}" }
        classes.forEach { entry ->
            zip.getInputStream(entry).use { stream ->
                val header = stream.readNBytes(8)
                check(header.size == 8 && header.take(4) == listOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte())) {
                    "Invalid class file ${entry.name} in ${archive.name}"
                }
                val major = ((header[6].toInt() and 255) shl 8) or (header[7].toInt() and 255)
                check(major <= 65) { "Shared extension ${archive.name} requires newer than Java 21: ${entry.name} (major $major)" }
            }
        }
    }
}
