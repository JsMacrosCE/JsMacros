import com.google.gson.JsonParser
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile

fun main(args: Array<String>) {
    val output = File(args[0]).apply { mkdirs() }
    var checks = 0
    for (resources in listOf(false, true)) {
        val fixture = Files.createTempDirectory(output.toPath(), "manifest-$resources-").toFile()
        fun write(path: String, text: String) = File(fixture, path).apply {
            parentFile.mkdirs()
            writeText(text)
        }
        write("settings.gradle.kts", "rootProject.name = \"manifest-fixture\"\n")
        write("deps/first.jar", "first")
        write("deps/quoted-\"é.jar", "quoted")
        write("deps/added.jar", "added")
        write("other/first.jar", "collision")
        write("patch/marker.txt", "patched dependency")
        write("extra/META-INF/jsmacroscedeps/undeclared.jar", "undeclared")
        write("build.gradle.kts", """
            import org.gradle.api.tasks.bundling.Jar
            plugins { id("jsmacros-extension") }
            val patched = tasks.register<Jar>("patchedJar") {
                archiveFileName.set("patched.jar")
                destinationDirectory.set(layout.buildDirectory.dir("patched"))
                from("patch")
            }
            val selection = providers.gradleProperty("selection").getOrElse("normal")
            val selected = when (selection) {
                "empty" -> emptyList<String>()
                "only" -> listOf("deps/first.jar")
                "added" -> listOf("deps/added.jar", "deps/first.jar")
                "duplicate" -> listOf("deps/first.jar", "other/first.jar")
                "missing" -> listOf("deps/missing.jar")
                else -> listOf("deps/first.jar", "deps/quoted-\"é.jar")
            }.let { if (providers.gradleProperty("reverse").isPresent) it.reversed() else it }
            val embedded = files(selected)
            if (selection == "normal") embedded.from(patched.flatMap { it.archiveFile })
            if ($resources) {
                tasks.register<Jar>("remapJar") {
                    archiveFileName.set("remapped.jar")
                    from(tasks.named<Jar>("jar").map { zipTree(it.archiveFile) })
                }
            }
            registerEmbeddedDependencies("jsmacrosce.ext.fixture.json", embedded, embedInResources = $resources,
                packagedJarTask = if ($resources) "remapJar" else "jar")
            tasks.named<Jar>("jar") {
                when (providers.gradleProperty("broken").orNull) {
                    "missing" -> exclude("**/first.jar", "first.jar")
                    "manifest" -> exclude("jsmacrosce.ext.fixture.json")
                    "extra" -> from("extra")
                    "content" -> eachFile {
                        if (name == "first.jar") filter { _: String -> "wrong dependency content" }
                    }
                }
            }
        """.trimIndent())

        fun run(vararg arguments: String, fail: Boolean = false): org.gradle.testkit.runner.BuildResult {
            val runner = GradleRunner.create().withProjectDir(fixture).withPluginClasspath()
                .withArguments(*arguments, "--offline", "--no-configuration-cache", "--no-parallel",
                    "--console=plain", "--max-workers=2", "-Dorg.gradle.jvmargs=-Xmx512m", "--stacktrace")
            return if (fail) runner.buildAndFail() else runner.build()
        }
        fun paths(): List<String> = JsonParser.parseString(
            File(fixture, "build/generated/dependency-manifest/jsmacrosce.ext.fixture.json").readText()
        ).asJsonObject.getAsJsonArray("dependencies").map { it.asString }
        fun expected(vararg names: String) = names.map { "META-INF/jsmacroscedeps/$it" }
        fun packagedContent(name: String): String {
            val jar = File(fixture, "build/libs/" + if (resources) "remapped.jar" else "manifest-fixture.jar")
            return ZipFile(jar).use { zip -> zip.getInputStream(zip.getEntry(name)).bufferedReader().use { it.readText() } }
        }

        val initial = run("verifyDependencyManifest")
        check(initial.task(":patchedJar")?.outcome == TaskOutcome.SUCCESS)
        check(initial.tasks.indexOfFirst { it.path == ":patchedJar" } <
            initial.tasks.indexOfFirst { it.path == ":processResources" })
        check(paths() == expected("first.jar", "quoted-\"é.jar", "patched.jar"))
        check(packagedContent("META-INF/jsmacroscedeps/quoted-\"é.jar") == "quoted")
        checks += 4

        val repeat = run("verifyDependencyManifest")
        check(repeat.task(":generateDependencyManifest")?.outcome == TaskOutcome.UP_TO_DATE)
        check(repeat.task(":jar")?.outcome == TaskOutcome.UP_TO_DATE)
        checks += 2

        val reversed = run("verifyDependencyManifest", "-Preverse")
        check(reversed.task(":generateDependencyManifest")?.outcome == TaskOutcome.SUCCESS)
        check(paths() == expected("quoted-\"é.jar", "first.jar", "patched.jar"))
        checks += 2

        write("deps/first.jar", "changed content")
        val changed = run("verifyDependencyManifest", "-Preverse")
        check(changed.task(":generateDependencyManifest")?.outcome == TaskOutcome.SUCCESS)
        check(packagedContent("META-INF/jsmacroscedeps/first.jar") == "changed content")
        checks += 2

        run("verifyDependencyManifest", "-Pselection=only")
        check(paths() == expected("first.jar"))
        run("verifyDependencyManifest", "-Pselection=added")
        check(paths() == expected("added.jar", "first.jar"))
        run("verifyDependencyManifest", "-Pselection=empty")
        check(paths().isEmpty())
        checks += 3

        val collision = run("generateDependencyManifest", "-Pselection=duplicate", fail = true)
        check(collision.output.contains("Embedded dependency filenames collide"))
        run("generateDependencyManifest", "-Pselection=missing", fail = true)
        checks += 2

        for (broken in listOf("missing", "manifest", "extra", "content")) {
            val result = run("verifyDependencyManifest", "-Pbroken=$broken", fail = true)
            check(result.task(":verifyDependencyManifest")?.outcome == TaskOutcome.FAILED)
            checks++
        }
    }
    println("Dependency manifest fixtures: $checks checks passed (jar and resource embedding)")
}
