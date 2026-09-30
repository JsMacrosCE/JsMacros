import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
}

// Get minecraft version from stonecutter.active file
val minecraftVersion = rootProject.file("stonecutter.active").readText().trim()
val versionProject = project(":${minecraftVersion}")
val targetJavaVersion = versionProject.property("java_version").toString().toInt()

configurations.configureEach {
    if (isCanBeResolved) {
        attributes.attribute(org.gradle.api.attributes.java.TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, targetJavaVersion)
    }
}

base {
    archivesName.set("${property("mod_id")}-graal")
}

java {
    sourceCompatibility = JavaVersion.toVersion(targetJavaVersion)
    targetCompatibility = JavaVersion.toVersion(targetJavaVersion)
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
}

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
    api(libs.graal.sdk)
    implementation(libs.graal.truffle.api)
    implementation(libs.graal.regex)
    implementation(libs.graal.polyglot)

    // Common library dependencies, google deps must align with neoforged
    implementation(libs.guava.extension)
    implementation(libs.gson)
    implementation(libs.slf4j)
    implementation(libs.fastutil)

    // Embed GraalVM dependencies
    add(embedDeps.name, libs.graal.sdk)
    add(embedDeps.name, libs.graal.regex)
    add(embedDeps.name, libs.graal.truffle.api)
    add(embedDeps.name, libs.graal.polyglot)
    add(embedDeps.name, libs.graal.collections)
    add(embedDeps.name, libs.graal.nativeimage)
    add(embedDeps.name, libs.graal.word)

    // Chrome Inspector and Profiler tools
    implementation(libs.graal.chromeinspector)
    implementation(libs.graal.profiler)

    // Embed them
    add(embedDeps.name, libs.graal.chromeinspector)
    add(embedDeps.name, libs.graal.profiler)

    // Test dependencies
    testImplementation(project(":extension"))
    testImplementation(project(":common:${minecraftVersion}"))
    testImplementation(libs.junit.api)
    testImplementation(libs.jetbrains.annotations)
    testRuntimeOnly(libs.junit.engine)
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
