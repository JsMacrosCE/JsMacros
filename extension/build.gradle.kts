plugins {
    id("java")
    id("com.gradleup.shadow")
}

val archives_base_name: String by project.properties

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net/")
}

dependencies {
    implementation(project(":common"))

    // Additional dependencies for testing
    implementation(libs.guava)
    implementation(libs.slf4j.api)

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.jetbrains:annotations:20.1.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

java {
    sourceCompatibility = JavaVersion.toVersion(rootProject.libs.versions.java.get().toInt())
    targetCompatibility = JavaVersion.toVersion(rootProject.libs.versions.java.get().toInt())

    toolchain {
        languageVersion = JavaLanguageVersion.of(rootProject.libs.versions.java.get().toInt())
    }
}

base {
    archivesName.set(archives_base_name + "-extension")
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.gradleup.shadow")

    base {
        archivesName.set(archives_base_name + "-${project.name}-extension")
    }

    java {
        sourceCompatibility = JavaVersion.toVersion(rootProject.libs.versions.java.get().toInt())
        targetCompatibility = JavaVersion.toVersion(rootProject.libs.versions.java.get().toInt())

        toolchain {
            languageVersion = JavaLanguageVersion.of(rootProject.libs.versions.java.get().toInt())
        }
    }

    // Create configuration for extension dependencies
    val jsmacrosExtensionDeps by configurations.creating {
        extendsFrom(configurations.implementation.get())
    }

    repositories {
        mavenCentral()
        maven("https://libraries.minecraft.net/")
    }

    dependencies {
        // Depend on parent extension project if exists
        if (parent != null) {
            implementation(parent!!)
        }

        // Depend on common module
        implementation(project(":common"))

        testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
        testImplementation("org.jetbrains:annotations:20.1.0")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    }

    afterEvaluate {
        val dependencyFiles = jsmacrosExtensionDeps
            .resolve()
            .filter { it.extension.equals("jar", ignoreCase = true) }
            .distinctBy { it.name }

        tasks.jar {
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
            dependsOn(":common:remapJar")

            if (dependencyFiles.isNotEmpty()) {
                from(dependencyFiles) {
                    into("META-INF/jars")
                    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
                }
            }
        }

        tasks.processResources {
            val dependencyPaths = dependencyFiles.map { "META-INF/jars/${it.name}" }
            filesMatching("jsmacros.ext.*.json") {
                expand("dependencies" to dependencyPaths.joinToString("\", \""))
            }
        }
    }
}