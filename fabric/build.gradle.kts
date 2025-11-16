plugins {
    alias(libs.plugins.architectury.plugin)
    alias(libs.plugins.architectury.loom)
    id("java")
    id("maven-publish")
    alias(libs.plugins.shadow)
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    accessWidenerPath.set(file("src/main/resources/jsmacros.accesswidener"))
}

dependencies {
    minecraft("com.mojang:minecraft:${libs.versions.minecraft.get()}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${libs.versions.fabric.loader.get()}")

    implementation(project(":common"))
    include(project(":common"))

    modImplementation("net.fabricmc.fabric-api:fabric-api:${libs.versions.fapi.get()}")
    modImplementation("com.terraformersmc:modmenu:${libs.versions.modmenu.get()}")

    // Implementation dependencies
    implementation(libs.prism4j)
    implementation(libs.nv.websocket.client)
    implementation(libs.javassist)
    implementation(libs.joor)

    // Include dependencies in jar for runtime
    include(libs.prism4j)
    include(libs.nv.websocket.client)
    include(libs.javassist)
    include(libs.joor)

    // Extensions
    include(project(":extension:graal"))
    include(project(":extension:graal:js"))

    // Include shared dependencies once in main jar
    include(libs.graal.sdk)
    include(libs.truffle.api)
    include(libs.graal.js)
    include(libs.graal.regex)

    // Add all extension subprojects for runtime
    for (file in file("../extension").listFiles() ?: emptyArray()) {
        if (!file.isDirectory || file.name in listOf("build", "src", ".gradle", "gradle")) continue

        runtimeOnly(project(":extension:${file.name}"))
        if (file.resolve("subprojects.txt").exists()) {
            for (subproject in file.resolve("subprojects.txt").readLines()) {
                if (file.name == "graal" && subproject.trim() == "python") {
                    // TODO: Graal Python brings in massive non-Minecraft-safe dependencies and
                    // currently explodes Architectury Transformer. Skip it for Fabric runtime.
                    continue
                }
                runtimeOnly(project(":extension:${file.name}:$subproject"))
                include(project(":extension:${file.name}:$subproject"))
            }
        } else {
            include(project(":extension:${file.name}"))
        }
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }

    // Include resources from common project
    from(project(":common").tasks.processResources) {
        exclude("jsmacros.accesswidener") // Exclude duplicate accesswidener
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

tasks.jar {
    archiveClassifier.set("fabric-dev")
}

tasks.remapJar {
    archiveClassifier.set("fabric")
}