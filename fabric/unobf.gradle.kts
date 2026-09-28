import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    id("net.fabricmc.fabric-loom")
    id("multiloader-loader")
    alias(libs.plugins.fletching.fabric)
}

val mod_id = commonMod.prop("mod_id")
val minecraft_version = commonMod.prop("minecraft_version")
val fabric_minecraft_version_range = commonMod.propOrNull("fabric_minecraft_version_range") ?: minecraft_version
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

    implInclude(libs.prism4j.get())
    implInclude(libs.joor.get())
    implInclude(libs.nv.websocket.get())
    implInclude(libs.javassist.get())

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
                "minecraft_version" to minecraft_version,
                "fabric_minecraft_version_range" to fabric_minecraft_version_range
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
