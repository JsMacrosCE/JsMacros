import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
}

base {
    archivesName.set("${property("mod_id")}-graal")
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

// Configuration for runtime dependencies to embed in the extension jar
val embedDeps by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    // Depends on extension module
    implementation(project(":extension"))

    // Compile against shared common code
    compileOnly(project(":common:${minecraftVersion}"))

    // Graal core dependencies - these get embedded
    api("org.graalvm.sdk:graal-sdk:24.0.1")
    implementation("org.graalvm.truffle:truffle-api:24.0.1")
    implementation("org.graalvm.regex:regex:24.0.1")
    implementation("org.graalvm.polyglot:polyglot:24.0.1")

    // Common library dependencies, google deps must align with neoforged
    implementation("com.google.guava:guava:31.1-jre")
    implementation("com.google.code.gson:gson:2.10")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("it.unimi.dsi:fastutil:8.5.15")

    // Embed GraalVM dependencies
    add(embedDeps.name, "org.graalvm.sdk:graal-sdk:24.0.1")
    add(embedDeps.name, "org.graalvm.regex:regex:24.0.1")
    add(embedDeps.name, "org.graalvm.truffle:truffle-api:24.0.1")
    add(embedDeps.name, "org.graalvm.polyglot:polyglot:24.0.1")
    add(embedDeps.name, "org.graalvm.sdk:collections:24.0.1")
    add(embedDeps.name, "org.graalvm.sdk:nativeimage:24.0.1")
    add(embedDeps.name, "org.graalvm.sdk:word:24.0.1")

    // Chrome Inspector and Profiler tools
    implementation("org.graalvm.tools:chromeinspector-tool:24.0.1")
    implementation("org.graalvm.tools:profiler-tool:24.0.1")

    // Embed them
    add(embedDeps.name, "org.graalvm.tools:chromeinspector-tool:24.0.1")
    add(embedDeps.name, "org.graalvm.tools:profiler-tool:24.0.1")

    // Test dependencies
    testImplementation(project(":extension"))
    testImplementation(project(":common:${minecraftVersion}"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.jetbrains:annotations:20.1.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

// Collect embedded dependency paths for the json file
fun getEmbeddedDepPaths(): String =
    embedDeps.files.joinToString(", ") { file ->
        "\"META-INF/jsmacroscedeps/${file.name}\""
    }

// Process resources to expand dependencies placeholder
tasks.named<ProcessResources>("processResources") {
    filesMatching("jsmacrosce.ext.graal.json") {
        expand(mapOf("dependencies" to getEmbeddedDepPaths()))
    }
}

// Embed dependencies into the extension jar
tasks.named<Jar>("jar") {
    dependsOn(embedDeps)
    from(embedDeps) {
        into("META-INF/jsmacroscedeps")
    }
}

tasks.test {
    useJUnitPlatform()
}
