import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("jsmacros-extension")
}

val minecraftVersion = EXTENSION_CORE_TARGET

base {
    archivesName.set("${property("mod_id")}-graal-js")
}

val extensionTestOutput = project(":extension")
    .extensions
    .getByType(SourceSetContainer::class.java)
    .named("test")
    .get()
    .output

// Configuration for runtime dependencies to embed in the extension jar
val embedDeps by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

// Dynamically exclude anything already embedded by :extension:graal to avoid duplicates
val parentEmbedDeps = project(":extension:graal").configurations.getByName("embedDeps")

val filteredEmbedFiles = providers.provider {
    val parentNames: Set<String> =
        parentEmbedDeps.resolvedConfiguration.resolvedArtifacts
            .map(ResolvedArtifact::getFile)
            .map { it.name }
            .toSet()

    embedDeps.resolvedConfiguration.resolvedArtifacts
        .map(ResolvedArtifact::getFile)
        .filter { it.name !in parentNames }
}

dependencies {
    // Depends on graal module
    implementation(project(":extension:graal"))
    implementation(project(":extension"))

    // Compile against shared common code
    compileOnly(project(":common:${minecraftVersion}"))

    // Graal JS specific dependencies
    implementation(libs.graal.js.runtime)

    // Embed GraalJS dependencies
    add(embedDeps.name, libs.graal.truffle.enterprise)
    add(embedDeps.name, libs.graal.js.language)
    add(embedDeps.name, libs.graal.truffle.runtime)
    add(embedDeps.name, libs.graal.truffle.compiler)
    add(embedDeps.name, libs.graal.nativebridge)
    add(embedDeps.name, libs.graal.jniutils)

    // Test dependencies
    testImplementation(project(":extension"))
    testImplementation(project(":common:${minecraftVersion}"))
    testImplementation(extensionTestOutput)
    testImplementation(libs.junit.api)
    testImplementation(libs.jetbrains.annotations)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Collect embedded dependency paths for the json file
fun getEmbeddedDepPaths(): String =
    filteredEmbedFiles.get().joinToString(", ") { file ->
        "\"META-INF/jsmacroscedeps/${file.name}\""
    }

// Process resources to expand dependencies placeholder
tasks.named<ProcessResources>("processResources") {
    inputs.files(filteredEmbedFiles)
    filesMatching("jsmacrosce.ext.graaljs.json") {
        expand(mapOf("dependencies" to getEmbeddedDepPaths()))
    }
}

// Embed dependencies into the extension jar
tasks.named<Jar>("jar") {
    dependsOn(embedDeps)
    from(filteredEmbedFiles) {
        into("META-INF/jsmacroscedeps")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
}
