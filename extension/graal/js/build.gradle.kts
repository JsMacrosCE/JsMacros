import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
}

base {
    archivesName.set("${property("mod_id")}-graal-js")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(property("java_version").toString().toInt()))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
}

// Get minecraft version from stonecutter.active file
val minecraftVersion = rootProject.file("stonecutter.active").readText().trim()

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
    implementation("org.graalvm.polyglot:js:24.0.1")

    // Embed GraalJS dependencies
    add(embedDeps.name, "org.graalvm.truffle:truffle-enterprise:24.0.1")
    add(embedDeps.name, "org.graalvm.js:js-language:24.0.1")
    add(embedDeps.name, "org.graalvm.truffle:truffle-runtime:24.0.1")
    add(embedDeps.name, "org.graalvm.truffle:truffle-compiler:24.0.1")
    add(embedDeps.name, "org.graalvm.sdk:nativebridge:24.0.1")
    add(embedDeps.name, "org.graalvm.sdk:jniutils:24.0.1")

    // Test dependencies
    testImplementation(project(":extension"))
    testImplementation(project(":common:${minecraftVersion}"))
    testImplementation(extensionTestOutput)
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.jetbrains:annotations:20.1.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
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
