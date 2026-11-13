import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.InputStream
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.zip.ZipFile

private const val DEPENDENCY_DIRECTORY = "META-INF/jsmacroscedeps/"

// Lists exactly the files selected for embedding, without a JSON template in the source resources.
abstract class GenerateDependencyManifest : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val archives: ConfigurableFileCollection

    // File inputs alone do not track order, but order decides which dependency supplies a class.
    @get:Input
    val dependencyPaths: List<String>
        get() = archives.files.map { DEPENDENCY_DIRECTORY + it.name }

    @get:OutputFile
    abstract val manifestFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val paths = dependencyPaths
        val duplicates = paths.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        check(duplicates.isEmpty()) { "Embedded dependency filenames collide: $duplicates" }
        check(archives.files.all { it.isFile }) { "Embedded dependencies must be files" }
        val dependencies = JsonArray().apply { paths.forEach { add(it) } }
        val manifest = JsonObject().apply { add("dependencies", dependencies) }
        val output = manifestFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(GsonBuilder().setPrettyPrinting().create().toJson(manifest) + "\n", Charsets.UTF_8)
    }
}

// Checks the packaged resource as well as its entries: copying the wrong manifest must fail check.
abstract class VerifyDependencyManifest : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val archives: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val archiveFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val manifestFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val manifest = manifestFile.get().asFile
        val expected = manifest.readText(Charsets.UTF_8)
        val paths = JsonParser.parseString(expected).asJsonObject.getAsJsonArray("dependencies")
            .map { it.asString }
        ZipFile(archiveFile.get().asFile).use { zip ->
            val entries = zip.entries().asSequence().filterNot { it.isDirectory }.toList()
            check(entries.count { it.name == manifest.name } == 1) {
                "Expected exactly one ${manifest.name} in ${archiveFile.get().asFile}"
            }
            val packaged = zip.getInputStream(zip.getEntry(manifest.name)).bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            check(packaged == expected) { "Packaged ${manifest.name} differs from the generated manifest" }
            val embedded = entries.filter { it.name.startsWith(DEPENDENCY_DIRECTORY) }.map { it.name }
            check(embedded.size == paths.size && embedded.toSet() == paths.toSet()) {
                "Packaged dependencies do not match ${manifest.name}: declared=$paths, embedded=$embedded"
            }
            for (archive in archives.files) {
                val path = DEPENDENCY_DIRECTORY + archive.name
                check(dependencyDigest(archive.inputStream()).contentEquals(
                    dependencyDigest(zip.getInputStream(zip.getEntry(path)))
                )) { "Packaged dependency $path differs from the selected file $archive" }
            }
        }
    }
}

private fun dependencyDigest(input: InputStream): ByteArray =
    DigestInputStream(input, MessageDigest.getInstance("SHA-256")).use { stream ->
        stream.transferTo(OutputStream.nullOutputStream())
        stream.messageDigest.digest()
    }

// Both the manifest and the copy use this single file selection, including any filtering or patches.
fun Project.registerEmbeddedDependencies(
    manifestName: String,
    archives: Any,
    embedInResources: Boolean = false,
    packagedJarTask: String = "jar"
): TaskProvider<GenerateDependencyManifest> {
    val embeddedFiles = files(archives)
    val generate = tasks.register("generateDependencyManifest", GenerateDependencyManifest::class.java) {
        group = "build"
        description = "Generates $manifestName from the embedded dependencies"
        this.archives.from(embeddedFiles)
        manifestFile.set(layout.buildDirectory.file("generated/dependency-manifest/$manifestName"))
    }
    tasks.named("processResources", ProcessResources::class.java) {
        from(generate.flatMap { task -> task.manifestFile })
        if (embedInResources) {
            from(embeddedFiles) { into(DEPENDENCY_DIRECTORY) }
        }
    }
    tasks.named("jar", Jar::class.java) {
        if (!embedInResources) {
            from(embeddedFiles) { into(DEPENDENCY_DIRECTORY) }
        }
    }
    val packagedJar = tasks.named(packagedJarTask, AbstractArchiveTask::class.java)
    val verify = tasks.register("verifyDependencyManifest", VerifyDependencyManifest::class.java) {
        group = "verification"
        description = "Checks $manifestName and its dependency entries in the jar"
        this.archives.from(embeddedFiles)
        archiveFile.set(packagedJar.flatMap { task -> task.archiveFile })
        manifestFile.set(generate.flatMap { task -> task.manifestFile })
    }
    tasks.named("check") { dependsOn(verify) }
    return generate
}
