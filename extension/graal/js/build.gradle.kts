import org.gradle.api.artifacts.ResolvedArtifact

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

registerEmbeddedDependencies("jsmacrosce.ext.graaljs.json", filteredEmbedFiles)

tasks.named<Jar>("jar") {
    dependsOn(embedDeps)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
}
