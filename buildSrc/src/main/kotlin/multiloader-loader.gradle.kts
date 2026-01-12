import org.gradle.api.tasks.compile.JavaCompile
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

// Get this project's Stonecutter-generated sources directory
val selfStonecutterJava = layout.buildDirectory.dir("generated/stonecutter/main/java")
val selfStonecutterResources = layout.buildDirectory.dir("generated/stonecutter/main/resources")

// Ensure stonecutterGenerate runs before compiling for both common and this project
val ensureStonecutterGenerate = tasks.register("ensureStonecutterGenerate") {
    dependsOn(":common:${minecraft_version}:stonecutterGenerate")
    dependsOn("stonecutterGenerate")
}

// Add common's Stonecutter-generated sources to the source set
sourceSets {
    main {
        java {
            srcDir(commonStonecutterJava)
        }
        resources {
            srcDir(commonStonecutterResources)
        }
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(ensureStonecutterGenerate)
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(ensureStonecutterGenerate)
}

tasks.named<Javadoc>("javadoc") {
    dependsOn(ensureStonecutterGenerate)
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(ensureStonecutterGenerate)
}

// Exclude access wideners folder from the jar (each loader handles this separately)
tasks.named<Jar>("jar") {
    exclude("accesswideners/**")
}
