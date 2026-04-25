pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }

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
        // see https://fabricmc.net/develop/ for new versions.
        // fabric-loom is the legacy alias (LoomGradlePlugin, requires mappings) used for 1.21.x.
        // net.fabricmc.fabric-loom is the non-obfuscated plugin (LoomNoRemapGradlePlugin, skips
        // mappings) required for MC 26.1+ which ships deobfuscated. Both IDs resolve to the same
        // plugin JAR.
        id("fabric-loom") version "1.15.4" apply false
        id("net.fabricmc.fabric-loom") version "1.15.4" apply false
        // see https://projects.neoforged.net/neoforged/moddevgradle for new versions
        id("net.neoforged.moddev") version "2.0.139" apply false
    }
}

plugins {
	id("dev.kikugie.stonecutter") version "0.8.3"
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

val fabricVersions = listOf("1.21.5", "1.21.8", "1.21.10", "1.21.11", "26.1.2")
val commonVersions = fabricVersions
// NeoForge 26.1.2.22-beta is available but requires its own adaptation; deferred to a follow-up PR.
// TODO(26.1): include "26.1.2" here once the neoforge branch is wired up.
val neoforgeVersions = listOf("1.21.5", "1.21.8", "1.21.10", "1.21.11")

// Expose to stonecutter.gradle.kts so version lists aren't duplicated.
gradle.extra["fabricVersions"] = fabricVersions
gradle.extra["neoforgeVersions"] = neoforgeVersions

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(rootProject) {
        versions(*fabricVersions.toTypedArray())

        branch("common") {
            versions(*commonVersions.toTypedArray())
        }
        branch("fabric") {
            versions(*fabricVersions.toTypedArray())
        }
        branch("neoforge") {
            versions(*neoforgeVersions.toTypedArray())
        }
    }
}
