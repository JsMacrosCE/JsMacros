import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    kotlin("jvm") version "2.2.10"
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
    id("net.fabricmc.fabric-loom")
    id("multiloader-loader")
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.22"
}

val mod_id = commonMod.prop("mod_id")
val minecraft_version = commonMod.prop("minecraft_version")
var mod_version = project.version.toString()

base {
    archivesName.set("$mod_id-$minecraft_version-fabric-$mod_version")
}

val extensionJars by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

fun DependencyHandlerScope.implInclude(notation: Any) {
    add("implementation", requireNotNull(include(notation)))
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")

    val fabric_loader_version = commonMod.prop("fabric_loader_version")
    val fabric_version = commonMod.prop("fabric_version")

    implementation("net.fabricmc:fabric-loader:$fabric_loader_version")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")

    val mod_menu_version = commonMod.prop("mod_menu_version")
    implementation("com.terraformersmc:modmenu:$mod_menu_version")

    implInclude("io.noties:prism4j:2.0.0")
    implInclude("org.jooq:joor:0.9.15")
    implInclude("com.neovisionaries:nv-websocket-client:2.14")
    implInclude("org.javassist:javassist:3.30.2-GA")

    add(extensionJars.name, project(mapOf("path" to ":extension:graal", "configuration" to "archives")))
    add(extensionJars.name, project(mapOf("path" to ":extension:graal:js", "configuration" to "archives")))
}

fun getExtensionJarPaths(): String =
    extensionJars.files.joinToString(", ") { file ->
        "\"META-INF/jsmacroscedeps/${file.name}\""
    }

tasks.named<ProcessResources>("processResources") {
    dependsOn(extensionJars)
    from(extensionJars) {
        into("META-INF/jsmacroscedeps")
    }

    filesMatching("jsmacrosce.extension.json") {
        expand(mapOf("dependencies" to getExtensionJarPaths()))
    }

    filesMatching("fabric.mod.json5") {
        expand(
            mapOf(
                "version" to mod_version,
                "minecraft_version" to minecraft_version
            )
        )
    }
}

loom {
    accessWidenerPath.set(project(":common").file("src/main/resources/accesswideners/$minecraft_version-$mod_id-official.accesswidener"))

    mixin {
        defaultRefmapName.set("$mod_id.refmap.json")
    }
}

fletchingTable {
    fabric {
        applyMixinConfig = false
    }
    mixins.register(sourceSets.main) {
        mixin("default", "jsmacrosce-common.mixins.json5") {
            env("CLIENT")
        }
        mixin("fabric", "jsmacrosce-fabric.mixins.json5") {
            env("CLIENT")
        }
    }
    j52j.register(sourceSets.main) {
        extension(
            "json",
            "fabric.mod.json5",
            "jsmacrosce-common.mixins.json5",
            "jsmacrosce-fabric.mixins.json5"
        )
    }
}

stonecutter {
    replacements.string(current.parsed >= "1.21.11") {
        replace("ResourceLocation", "Identifier")

        replace("parseIdentifier", "parseIdentifier")
        replace("getAdvancementsForIdentifiers", "getAdvancementsForIdentifiers")
        replace("suggestIdentifier", "suggestIdentifier")
        replace("mapIdentifiers", "mapIdentifiers")
        replace("getIdentifier", "getIdentifier")
        replace("writeIdentifier", "writeIdentifier")
        replace("readIdentifier", "readIdentifier")
        replace("getWorldIdentifier", "getWorldIdentifier")
        replace("base.readResourceLocation", "base.readIdentifier")
        replace("base.writeResourceLocation", "base.writeIdentifier")
        replace("@return the raw minecraft Identifier.", "@return the raw minecraft Identifier.")
    }
}
