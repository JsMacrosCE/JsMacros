import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
}

base {
    archivesName.set("${property("mod_id")}-ruby")
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
val minecraftVersion = rootProject.file("stonecutter.active").takeIf { it.exists() }
    ?.readText()?.trim()?.ifEmpty { null }
    ?: throw GradleException("stonecutter.active is empty; set an active version first")

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
    compileOnly("org.jetbrains:annotations:20.1.0")

    // JRuby runtime - embedded into the extension jar so users don't need it on the classpath
    implementation("org.jruby:jruby-complete:9.4.5.0")
    add(embedDeps.name, "org.jruby:jruby-complete:9.4.5.0")

    // Common library dependencies, google deps must align with neoforged
    implementation("com.google.guava:guava:31.1-jre")
    implementation("com.google.code.gson:gson:2.10")
    implementation("org.slf4j:slf4j-api:2.0.16")

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
    filesMatching("jsmacrosce.ext.jruby.json") {
        expand(mapOf("dependencies" to getEmbeddedDepPaths()))
    }
}

// Embed dependencies into the extension jar
tasks.named<Jar>("jar") {
    dependsOn(embedDeps)
    from(embedDeps) {
        into("META-INF/jsmacroscedeps")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
}
