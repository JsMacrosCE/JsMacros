import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

repositories {
    mavenLocal()
    mavenCentral()

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

val channel = providers.gradleProperty("channel").orElse("release").get()
val baseVersion = providers.gradleProperty("mod_version").orElse(providers.gradleProperty("version")).get()
version = when (channel) {
    "release" -> baseVersion
    "beta" -> "$baseVersion-beta.${providers.gradleProperty("beta_number").orElse("1").get()}"
    "alpha" -> {
        val date = providers.gradleProperty("alpha_date").orElse(providers.provider {
            ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        }).get()
        "$baseVersion-alpha.$date"
    }
    else -> {
        val sha = providers.gradleProperty("build_sha")
            .orElse(providers.environmentVariable("GITHUB_SHA")).orElse("local").get().take(7)
        "$baseVersion-dev-$sha"
    }
}

// The controller owns release packaging and publishing. Version nodes must not
// produce another copy of shared extensions or a second devkit/release definition.
if (project != rootProject) {
    val mcVersion = stonecutter.current.version
    listOf("createDist", "createDistDocs", "createDistSite", "createDistExtensions", "packageDevkit").forEach { name ->
        tasks.register(name) {
            group = "distribution"
            description = "Delegates to the controller's $name task"
            dependsOn(":$name")
        }
    }
    tasks.register("createDistMods") {
        group = "distribution"
        description = "Packages both loader jars for Minecraft $mcVersion"
        dependsOn(listOf("Fabric", "Neoforge").map { ":package${it}ModJar${mcVersion.replace(".", "")}" })
    }
    tasks.register("printVersion") {
        group = "distribution"
        doLast { println(project.version) }
    }
}

stonecutter {
    minecraftReplacements()
}
