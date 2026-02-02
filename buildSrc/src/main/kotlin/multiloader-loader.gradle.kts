import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.bundling.Jar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("multiloader-common")
}

// Use Stonecutter-aware property access
val mod_id = commonMod.prop("mod_id")
val minecraft_version = commonMod.mc

// Get the common project reference
val commonProject = project(":common:${minecraft_version}")

// Get the Stonecutter-generated sources directory from common
val commonStonecutterJava = commonProject.layout.buildDirectory.dir("generated/stonecutter/main/java")
val commonStonecutterResources = commonProject.layout.buildDirectory.dir("generated/stonecutter/main/resources")

// Common main sources/resources (non-generated)
val commonMainJava = commonProject.layout.projectDirectory.dir("src/main/java")
val commonMainResources = commonProject.layout.projectDirectory.dir("src/main/resources")

// Get this project's Stonecutter-generated sources directory
val selfStonecutterJava = layout.buildDirectory.dir("generated/stonecutter/main/java")
val selfStonecutterResources = layout.buildDirectory.dir("generated/stonecutter/main/resources")

sourceSets.named("main") {
    java.srcDir(commonStonecutterJava)
    java.srcDir(commonMainJava)
    resources.srcDir(commonStonecutterResources)
    resources.srcDir(commonMainResources)
}

// Ensure stonecutterGenerate runs before compiling for both common and this project
val ensureStonecutterGenerate = tasks.register("ensureStonecutterGenerate") {
    dependsOn(":common:${minecraft_version}:stonecutterGenerate")
    dependsOn("stonecutterGenerate")
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(ensureStonecutterGenerate)
    // Add common's Stonecutter-generated sources
    source(commonStonecutterJava)
    // Add common's main sources
    source(commonMainJava)
}

tasks.matching { it.name == "kspKotlin" }.configureEach {
    dependsOn(ensureStonecutterGenerate)
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(ensureStonecutterGenerate)
    // Include common's Stonecutter-generated resources
    from(commonStonecutterResources)
    // Include common's main resources
    from(commonMainResources)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<Javadoc>("javadoc") {
    dependsOn(ensureStonecutterGenerate)
    source(commonStonecutterJava)
    source(commonMainJava)
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(ensureStonecutterGenerate)
    from(commonStonecutterJava)
    from(commonStonecutterResources)
    from(commonMainJava)
    from(commonMainResources)
}

pluginManager.withPlugin("net.neoforged.moddev") {
    tasks.named<Jar>("jar") {
        exclude("accesswideners/**")
    }
}
