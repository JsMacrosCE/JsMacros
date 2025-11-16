plugins {
    alias(libs.plugins.architectury.plugin)
    alias(libs.plugins.architectury.loom)
}

architectury {
    platformSetupLoomIde()
    forge()
}

loom {
    accessWidenerPath.set(file("src/main/resources/jsmacros.accesswidener"))
}

dependencies {
    forge("net.minecraftforge:forge:1.21.8-52.0.19")

    implementation(project(":common"))

    // Mixin dependencies
    compileOnly(libs.mixin)
    compileOnly(libs.mixin.extra)

    // Include dependencies in jar
    implementation(libs.prism4j)
    implementation(libs.joor)
    implementation(libs.nv.websocket.client)
    implementation(libs.javassist)

    // Extensions (temporarily disabled)
    // runtimeOnly(project(":extension:graal"))
    // runtimeOnly(project(":extension:graal:js"))

    // Add all extension subprojects for runtime (temporarily disabled)
    // for (file in file("../extension").listFiles() ?: emptyArray()) {
    //     if (!file.isDirectory || file.name in listOf("build", "src", ".gradle", "gradle")) continue
    //     runtimeOnly(project(":extension:${file.name}"))
    //
    //     if (file.resolve("subprojects.txt").exists()) {
    //         for (subproject in file.resolve("subprojects.txt").readLines()) {
    //             runtimeOnly(project(":extension:${file.name}:$subproject"))
    //         }
    //     }
    // }
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", "1.21.8")
    inputs.property("forge_version", "52.0.19")

    filesMatching("META-INF/mods.toml") {
        expand(
            "version" to project.version,
            "minecraft_version" to "1.21.8",
            "forge_version" to "52.0.19"
        )
    }

    // Include resources from common project
    from(project(":common").tasks.processResources)
}

tasks.jar {
    archiveClassifier.set("forge")
    manifest {
        attributes(
            "MixinConfigs" to "jsmacros-common.mixins.json,jsmacros-client.mixins.json,jsmacros-forge.mixins.json",
            "FMLModType" to "MOD"
        )
    }
}