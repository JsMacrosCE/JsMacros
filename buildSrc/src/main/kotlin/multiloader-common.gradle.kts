import org.gradle.api.tasks.bundling.Jar
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("java-library")
    id("idea")
}

// Use Stonecutter-aware property access
// Properties are resolved from the versioned project's gradle.properties (via commonMod)
val mod_id = commonMod.prop("mod_id")
val minecraft_version = commonMod.mc
val java_version = commonMod.prop("java_version")

val minecraft_version_range = commonMod.propOrNull("minecraft_version_range") ?: "[$minecraft_version]"
val fabric_version = commonMod.propOrNull("fabric_version") ?: ""
val fabric_loader_version = commonMod.propOrNull("fabric_loader_version") ?: ""

val mod_name = commonMod.prop("mod_name")
val mod_author = commonMod.prop("mod_author")
val license = commonMod.prop("license")
val credits = commonMod.propOrNull("credits") ?: ""

val neoforge_version = commonMod.propOrNull("neoforge_version") ?: ""
val neoforge_loader_version_range = commonMod.propOrNull("neoforge_loader_version_range") ?: "[4,)"

base {
    archivesName.set("$mod_id-${project.name}-$minecraft_version")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(java_version.toInt()))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenLocal()
    mavenCentral()

    // https://docs.gradle.org/current/userguide/declaring_repositories.html#declaring_content_exclusively_found_in_one_repository
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
        forRepositories(
            maven {
                name = "ParchmentMC"
                url = uri("https://maven.parchmentmc.org/")
            },
            maven {
                name = "NeoForge"
                url = uri("https://maven.neoforged.net/releases")
            }
        )
        filter {
            includeGroup("org.parchmentmc.data")
        }
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "TerraformersMC"
                url = uri("https://maven.terraformersmc.com/releases/")
            }
        }
        filter {
            includeGroupAndSubgroups("com.terraformersmc")
        }
    }

    maven {
        name = "BlameJared"
        url = uri("https://maven.blamejared.com")
    }
    maven {
        name = "NeoForge"
        url = uri("https://maven.neoforged.net/releases")
    }
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net")
    }
}

// Declare capabilities on the outgoing configurations.
listOf("apiElements", "runtimeElements", "sourcesElements", "javadocElements").forEach { variant ->
    configurations.named(variant).configure {
        outgoing {
            capability("${project.group}:${project.name}:${project.version}")
            capability("${project.group}:${base.archivesName.get()}:${project.version}")
            capability("${project.group}:$mod_id-${project.name}-$minecraft_version:${project.version}")
            capability("${project.group}:$mod_id:${project.version}")
        }
    }

    // Suppress Gradle metadata warnings for each published variant (only if publishing is configured)
    extensions.findByType(org.gradle.api.publish.PublishingExtension::class.java)?.let { publishing ->
        publishing.publications
            .withType(MavenPublication::class.java)
            .configureEach {
                suppressPomMetadataWarningsFor(variant)
            }
    }
}

tasks {
	processResources {
		val expandProps = mapOf(
            "version" to project.version,
            "group" to project.group,
            "minecraft_version" to minecraft_version,
            "minecraft_version_range" to minecraft_version_range,
            "fabric_version" to fabric_version,
            "fabric_loader_version" to fabric_loader_version,
            "mod_name" to mod_name,
            "mod_author" to mod_author,
            "mod_id" to mod_id,
            "license" to license,
            "description" to project.description,
            "neoforge_version" to neoforge_version,
            "neoforge_loader_version_range" to neoforge_loader_version_range,
            "credits" to credits,
            "java_version" to java_version
		)

        val jsonExpandProps: Map<String, Any?> =
            expandProps.mapValues { (_, value) ->
                if (value is String) value.replace("\n", "\\\\n") else value
            }

        filesMatching(listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml")) {
            expand(expandProps)
        }

        filesMatching(listOf("pack.mcmeta", "fabric.mod.json5", "*.mixins.json5")) {
            expand(jsonExpandProps)
        }

		inputs.properties(expandProps)
	}
}

tasks.named("processResources") {
	dependsOn(":common:${minecraft_version}:stonecutterGenerate")
}

tasks.named<Jar>("sourcesJar") {
    from(rootProject.file("LICENSE")) {
        rename { fileName -> "${fileName}_$mod_name" }
    }
}

tasks.named<Jar>("jar") {
    from(rootProject.file("LICENSE")) {
        rename { fileName -> "${fileName}_$mod_name" }
    }

    manifest {
        attributes(
            mapOf(
                "Specification-Title" to mod_name,
                "Specification-Vendor" to mod_author,
                "Specification-Version" to archiveVersion.get(),
                "Implementation-Title" to project.name,
                "Implementation-Version" to archiveVersion.get(),
                "Implementation-Vendor" to mod_author,
                "Built-On-Minecraft" to minecraft_version
            )
        )
    }
}
