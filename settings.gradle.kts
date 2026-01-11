pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()

        exclusiveContent {
            forRepository {
                maven {
                    name = "Fabric"
                    url = uri("https://maven.fabricmc.net")
                }
            }
            filter {
                includeGroupAndSubgroups("net.fabricmc")
                includeGroupAndSubgroups("fabric-loom")
            }
        }

        exclusiveContent {
            forRepository {
                maven {
                    name = "Sponge"
                    url = uri("https://repo.spongepowered.org/repository/maven-public")
                }
            }
            filter {
                includeGroupAndSubgroups("org.spongepowered")
            }
        }

        exclusiveContent {
            forRepository {
                maven {
                    name = "NeoForge"
                    url = uri("https://maven.neoforged.net/releases")
                }
            }
            filter {
                includeGroupAndSubgroups("net.neoforged")
            }
        }
    }

    plugins {
        // see https://fabricmc.net/develop/ for new versions
        id("fabric-loom") version "1.13-SNAPSHOT" apply false
        // see https://projects.neoforged.net/neoforged/moddevgradle for new versions
        id("net.neoforged.moddev") version "2.0.139" apply false
    }
}

plugins {
	id("dev.kikugie.stonecutter") version "0.7.11"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

// This should match the folder name of the project, or else IDEA may complain (see https://youtrack.jetbrains.com/issue/IDEA-317606)
rootProject.name = "JsMacrosCE"

include("common")
include("fabric")
include("neoforge")
include("extension")
include("extension:graal")
include("extension:graal:js")
include("extension:graal:python")

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(rootProject) {
        versions("1.21.5", "1.21.8", "1.21.10")

        branch("common") {
            versions("1.21.5", "1.21.8", "1.21.10")
        }
        branch("fabric") {
            versions("1.21.5", "1.21.8", "1.21.10")
        }
        branch("neoforge") {
            versions("1.21.5", "1.21.8", "1.21.10")
        }
    }
}