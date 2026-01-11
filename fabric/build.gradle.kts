import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("fabric-loom")
    id("multiloader-loader")
}

val mod_id = commonMod.prop("mod_id")
val minecraft_version = commonMod.prop("minecraft_version")
var mod_version = project.version.toString()

base {
    archivesName.set("$mod_id-$minecraft_version-fabric-$mod_version")
}

// Configuration for embedding extension jars
val extensionJars by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

// Gradle is stupid and will throw a `Type mismatch: inferred type is Dependency? but Any was expected` otherwise
fun DependencyHandlerScope.implInclude(notation: Any) {
    add("implementation", requireNotNull(include(notation)))
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")

    mappings(loom.layered {
        val parchment_minecraft = commonMod.prop("parchment_minecraft")
        val parchment_version = commonMod.prop("parchment_version")

        officialMojangMappings()
        parchment(
            "org.parchmentmc.data:parchment-$parchment_minecraft:$parchment_version@zip"
        )
    })

    val fabric_loader_version = commonMod.prop("fabric_loader_version")
    val fabric_version = commonMod.prop("fabric_version")

    modImplementation("net.fabricmc:fabric-loader:$fabric_loader_version")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")

    // ModMenu integration
    val mod_menu_version = commonMod.prop("mod_menu_version");
    modImplementation("com.terraformersmc:modmenu:$mod_menu_version")

    // Common library dependencies - include for bundling in jar
    implInclude("io.noties:prism4j:2.0.0")
    implInclude("org.jooq:joor:0.9.15")
    implInclude("com.neovisionaries:nv-websocket-client:2.14")
    implInclude("org.javassist:javassist:3.30.2-GA")

    // Extension jars to embed
    add(extensionJars.name, project(mapOf("path" to ":extension:graal", "configuration" to "archives")))
    add(extensionJars.name, project(mapOf("path" to ":extension:graal:js", "configuration" to "archives")))
}

// Collect extension jar names for dependencies property
fun getExtensionJarPaths(): String =
    extensionJars.files.joinToString(", ") { file ->
        "\"META-INF/jsmacroscedeps/${file.name}\""
    }

tasks.named<ProcessResources>("processResources") {
    // Embed extension jars into the final jar
    dependsOn(extensionJars)
    from(extensionJars) {
        into("META-INF/jsmacroscedeps")
    }

    // Add dependencies expansion for jsmacros.extension.json
    filesMatching("jsmacrosce.extension.json") {
        expand(mapOf("dependencies" to getExtensionJarPaths()))
    }
    
    // Expand fabric.mod.json with minecraft version
    filesMatching("fabric.mod.json") {
        expand(mapOf(
            "version" to mod_version,
            "minecraft_version" to minecraft_version
        ))
    }
}

// Copy the version-specific access widener and rename it for the jar
tasks.named<ProcessResources>("processResources") {
    val awFile = project(":common").file("src/main/resources/accesswideners/$minecraft_version-$mod_id.accesswidener")
    
    from(awFile.parentFile) {
        include(awFile.name)
        rename(awFile.name, "$mod_id.accesswidener")
        into("")
    }
}

loom {
    // Use the version-specific access widener
    accessWidenerPath.set(project(":common").file("src/main/resources/accesswideners/$minecraft_version-$mod_id.accesswidener"))

    mixin {
        defaultRefmapName.set("$mod_id.refmap.json")
    }
}
