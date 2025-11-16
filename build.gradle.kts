plugins {
    alias(libs.plugins.architectury.plugin)
    alias(libs.plugins.architectury.loom) apply false
    alias(libs.plugins.shadow) apply false
}

architectury {
    minecraft = libs.versions.minecraft.get()
}

version = "2.0.0"
group = "xyz.wagyourtail"

// Subproject configuration for all modules
subprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.terraformersmc.com/releases/")
        maven("https://jitpack.io")
    }

    // Apply Java toolchain consistently
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

// Build coordination tasks
tasks.register("buildAll") {
    group = "build"
    description = "Build all available modules"

    // Only build modules that are currently enabled
    dependsOn(":common:build")
    dependsOn(":extension:build")
    dependsOn(":site:build")
}

tasks.register("cleanAll") {
    group = "build"
    description = "Clean all modules"

    dependsOn(tasks.named("clean"))
    dependsOn(":common:clean")
    dependsOn(":site:clean")
}

// Extension system configuration
 val jsmacrosExtensionInclude by configurations.creating

 dependencies {
     jsmacrosExtensionInclude(project(":extension:graal")) { isTransitive = false }
     jsmacrosExtensionInclude(project(":extension:graal:js")) { isTransitive = false }
 }

// Enhanced clean task
tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
    delete(file("dist"))
}

// Distribution task
tasks.register<Copy>("createDist") {
    group = "build"
    description = "Creates all files for the distribution of the project"

    // Clean dist directory first
    doFirst {
        delete(file("dist"))
        file("dist").mkdirs()
    }

    // Collect extension JARs
    from(project(":extension:graal").tasks.named("jar")) {
        into("extensions")
        rename { "${project.name}-${it}" }
    }
    from(project(":extension:graal:js").tasks.named("jar")) {
        into("extensions")
        rename { "${project.name}-${it}" }
    }
    from(project(":extension:graal:python").tasks.named("jar")) {
        into("extensions")
        rename { "${project.name}-${it}" }
    }

    // Copy TypeScript definitions for documentation
    from("docs/typescript") {
        into("typescript")
        include("**/*.d.ts")
    }

    // Copy gradle.properties for CI/CD
    from("gradle.properties")

    into(layout.projectDirectory.dir("dist"))

    doLast {
        println("Distribution created in ./dist/")
        println("Contents:")
        file("dist").listFiles()?.forEach { file ->
            if (file.isDirectory()) {
                println("  ${file.name}/")
                file.listFiles()?.forEach { subFile ->
                    println("    - ${subFile.name}")
                }
            } else {
                println("  - ${file.name}")
            }
        }
    }
}

// Add createDist to buildAll task
tasks.named("buildAll") {
    dependsOn("createDist")
}