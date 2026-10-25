import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.tools.ToolProvider

private val versions = listOf("1.21.5", "1.21.8", "1.21.10", "1.21.11", "26.1.2", "26.2", "26.3")
private val loaders = listOf("fabric", "neoforge")

fun main(args: Array<String>) {
    val output = File(args[0]).apply { mkdirs() }
    val repository = File(args[1])
    var checks = 0
    for (release in listOf("2.0.0", "2.0.0-beta.3", "2.0.0-dev-abcdef0")) {
        val fixture = Files.createTempDirectory(output.toPath(), "$release-").toFile()
        writeFixture(fixture, repository, release)
        val result = GradleRunner.create()
            .withProjectDir(fixture)
            .withPluginClasspath()
            .withArguments("dumpReleaseAssets", "--offline", "--console=plain", "--max-workers=2", "--stacktrace",
                "-Dorg.gradle.jvmargs=-Xmx512m", "-Dorg.gradle.configuration-cache=false")
            .build()
        check(result.output.contains("Verified 16 release assets")) { result.output }
        val expected = versions.flatMap { mc -> loaders.map { "jsmacrosce-$mc-$it-$release.jar" } }.toSet() +
            setOf("jsmacrosce-devkit-$release.zip", "jsmacrosce-ext-graalpy-$release.jar")
        check(File(fixture, "assets.txt").readLines().toSet() == expected)
        check(File(fixture, "dist").walkTopDown().filter(File::isFile).map(File::getName).toSet() == expected)
        val devkit = File(fixture, "dist/jsmacrosce-devkit-$release.zip")
        ZipFile(devkit).use { zip ->
            check(zip.entries().asSequence().none { it.name.startsWith("99.0/") || it.name.startsWith("vitepress/99.0/") })
            versions.forEach { mc ->
                for (relative in listOf("web/index.html", "typescript/headers/JsMacros-$release.d.ts", "python/JsMacrosAC/Core.py")) {
                    check(zip.getInputStream(zip.getEntry("$mc/$relative")).readBytes().contentEquals(
                        File(fixture, "build/docs/targets/$mc/$relative").readBytes()))
                }
            }
        }
        val extension = File(fixture, "dist/extensions/jsmacrosce-ext-graalpy-$release.jar")
        check(extension.readBytes().contentEquals(File(fixture, "extension/graal/python/build/libs/python.jar").readBytes()))
        checks += 5

        if (release == "2.0.0") {
            val incompleteSelection = GradleRunner.create()
                .withProjectDir(fixture)
                .withPluginClasspath()
                .withArguments("verifyDistribution", "-PfixtureDocumentationTargets=26.3", "--offline", "--console=plain",
                    "--max-workers=2", "-Dorg.gradle.jvmargs=-Xmx512m", "-Dorg.gradle.configuration-cache=false")
                .buildAndFail()
            check(incompleteSelection.output.contains("Distribution documentation must cover every packaged Minecraft target"))
            check(incompleteSelection.task(":prepareDist") == null) { "Invalid target selection must fail before deleting dist" }
            checks += 2
            rejectArchive(devkit, File(fixture, "missing-site.zip"), { it == "vitepress/index.html" }) {
                verifyDevkit(it, versions, release)
            }
            rejectArchive(devkit, File(fixture, "missing-site-assets.zip"), { it.startsWith("vitepress/assets/") }) {
                verifyDevkit(it, versions, release)
            }
            rejectArchive(devkit, File(fixture, "missing-target.zip"), { it == "vitepress/$release/26.3/index.html" }) {
                verifyDevkit(it, versions, release)
            }
            rejectArchive(devkit, File(fixture, "missing-python.zip"), { it.startsWith("1.21.5/python/JsMacrosAC/") }) {
                verifyDevkit(it, versions, release)
            }
            rejectArchive(devkit, File(fixture, "missing-ts-support.zip"), { it == "26.3/typescript/headers/Graal.d.ts" }) {
                verifyDevkit(it, versions, release)
            }
            rejectArchive(extension, File(fixture, "missing-service.jar"), { it.startsWith("META-INF/services/") }) {
                verifySharedExtension(it)
            }
            val tooNew = File(fixture, "java25.jar")
            rewriteArchive(extension, tooNew) { name, bytes ->
                if (name.endsWith(".class")) bytes.copyOf().apply { this[7] = 69 } else bytes
            }
            expectFailure { verifySharedExtension(tooNew) }
            checks += 7
        }
    }
    println("Distribution fixtures: $checks checks passed (release, beta and dev; seven targets, two loaders)")
}

private fun writeFixture(root: File, repository: File, release: String) {
    fun write(path: String, content: String) {
        File(root, path).apply { parentFile.mkdirs(); writeText(content) }
    }
    val projects = versions.flatMap { mc -> loaders.map { ":$it:$mc" } } + ":extension:graal:python"
    projects.forEach { File(root, it.removePrefix(":").replace(':', '/')).mkdirs() }
    write("settings.gradle.kts", "rootProject.name = \"distribution-fixture\"\ninclude(${projects.joinToString { "\"$it\"" }})\n")
    // The convention must not use a newer IDE source state to raise its Java baseline.
    write("stonecutter.active", "26.3\n")
    write("build.gradle.kts", """
        import org.gradle.api.tasks.bundling.Jar
        plugins { id("jsmacros-extension") }
        version = "$release"
        check(java.toolchain.languageVersion.get().asInt() == 21)
        check(tasks.named<JavaCompile>("compileJava").get().options.release.get() == 21)
        check(EXTENSION_CORE_TARGET == "1.21.5")
        subprojects {
            if (path.startsWith(":fabric:") || path.startsWith(":neoforge:") || path == ":extension:graal:python") {
                tasks.register<Jar>("jar") {
                    archiveFileName.set(if (project.path == ":extension:graal:python") "python.jar" else "mod.jar")
                    destinationDirectory.set(layout.buildDirectory.dir("libs"))
                    from(rootProject.file("fixtures/extension"))
                }
                if (path.startsWith(":fabric:1.")) {
                    tasks.register<Jar>("remapJar") {
                        archiveFileName.set("remapped.jar")
                        destinationDirectory.set(layout.buildDirectory.dir("libs"))
                        from(rootProject.file("fixtures/extension"))
                    }
                }
            }
        }
        val targets = listOf(${versions.joinToString { "\"$it\"" }})
        val documentationTargets = providers.gradleProperty("fixtureDocumentationTargets").orNull?.split(',') ?: targets
        tasks.register("prepareDist") { doLast { delete(layout.projectDirectory.dir("dist")) } }
        tasks.register("buildVitepressSite")
        tasks.register("createDistDocs")
        tasks.register("createDistSite")
        targets.forEach { tasks.register("copyDocs" + it.replace(".", "")) }
        val distribution = registerDistributionTasks(
            "jsmacrosce", targets, targets, documentationTargets, listOf("fabric", "neoforge"),
            listOf(SharedExtensionArtifact(":extension:graal:python", "graalpy")), layout.projectDirectory.dir("dist")
        )
        tasks.register("dumpReleaseAssets") {
            dependsOn(distribution.verify)
            doLast { file("assets.txt").writeText(distribution.releaseFiles.files.map { it.name }.sorted().joinToString("\n")) }
        }
    """.trimIndent())
    File(repository, "docs/devkit").copyRecursively(File(root, "docs/devkit"), overwrite = true)
    write("fixtures/extension/META-INF/services/com.jsmacrosce.jsmacros.core.extensions.Extension", "FixtureExtension\n")
    write("fixtures/FixtureExtension.java", "public class FixtureExtension {}\n")
    check(ToolProvider.getSystemJavaCompiler().run(null, null, null,
        "--release", "21", "-d", File(root, "fixtures/extension").path,
        File(root, "fixtures/FixtureExtension.java").path) == 0)
    for (mc in versions) {
        val base = "build/docs/targets/$mc"
        write("$base/web/index.html", "<html>$mc legacy reference</html>")
        write("$base/typescript/tsconfig.json", "{\"include\":[\"headers/*.d.ts\"]}")
        write("$base/typescript/headers/Graal.d.ts", "// Graal support headers\n")
        write("$base/typescript/headers/JsMacros-$release.d.ts", "// $mc typings\n")
        write("$base/python/README.md", "$mc Python stub instructions\n")
        write("$base/python/JsMacrosAC/Core.py", "\"\"\"$mc Python stubs\"\"\"\n")
        write("build/docs/vitepress/.vitepress/dist/$release/$mc/index.html", "<html>$mc VitePress reference</html>")
        // Raw site sources should never enter the devkit.
        write("$base/vitepress/content/unused.md", "not rendered")
    }
    write("build/docs/vitepress/.vitepress/dist/index.html", "<html>Docs home</html>")
    write("build/docs/vitepress/.vitepress/dist/assets/app.js", "// compiled site assets\n")
    write("build/docs/targets/99.0/typescript/headers/old.d.ts", "stale, unselected target")
    write("dist/jsmacrosce-extensions-1.21.5-$release.zip", "obsolete extension pack")
}

private fun rejectArchive(source: File, destination: File, drop: (String) -> Boolean, verify: (File) -> Unit) {
    rewriteArchive(source, destination) { name, bytes -> if (drop(name)) null else bytes }
    expectFailure { verify(destination) }
}

private fun rewriteArchive(source: File, destination: File, transform: (String, ByteArray) -> ByteArray?) {
    ZipFile(source).use { input ->
        ZipOutputStream(Files.newOutputStream(destination.toPath())).use { output ->
            for (entry in input.entries()) {
                if (entry.isDirectory) continue
                val bytes = transform(entry.name, input.getInputStream(entry).use { it.readAllBytes() }) ?: continue
                output.putNextEntry(ZipEntry(entry.name))
                output.write(bytes)
                output.closeEntry()
            }
        }
    }
}

private fun expectFailure(action: () -> Unit) {
    check(runCatching(action).exceptionOrNull() is IllegalStateException) { "Malformed artifact was accepted" }
}
