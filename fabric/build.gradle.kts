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

base {
    archivesName.set("jsmacros")
}

loom {
    accessWidenerPath.set(project(":common").file("src/main/resources/jsmacros.accesswidener"))

    mixin {
        defaultRefmapName.set("jsmacros-fabric.refmap.json")
        useLegacyMixinAp.set(true)
    }
}

val common by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
val shadowBundle by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

configurations["compileClasspath"].extendsFrom(common)
configurations["runtimeClasspath"].extendsFrom(common)
configurations["developmentFabric"].extendsFrom(common)

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabric.loader)

    add(common.name, project(":common", "namedElements")) {
        isTransitive = false
    }
    add(shadowBundle.name, project(":common", "transformProductionFabric"))

    modImplementation(libs.fabric.api)
    modImplementation(libs.modmenu)

    // Implementation dependencies
    implementation(libs.bundles.scripting.libs)
    implementation(libs.javassist)

    // Include dependencies in jar for runtime
    include(libs.bundles.scripting.libs)
    include(libs.javassist)

    // Extensions
    include(project(":extension:graal"))
    include(project(":extension:graal:js"))

    // Include shared dependencies once in main jar
    include(libs.bundles.graal.core)

    // Add all extension subprojects (embed in JAR, not runtime classpath)
    for (file in file("../extension").listFiles() ?: emptyArray()) {
        if (!file.isDirectory || file.name in listOf("build", "src", ".gradle", "gradle")) continue

        if (file.resolve("subprojects.txt").exists()) {
            for (subproject in file.resolve("subprojects.txt").readLines()) {
                if (file.name == "graal" && subproject.trim() == "python") {
                    // TODO: Graal Python brings in massive non-Minecraft-safe dependencies and
                    // currently explodes Architectury Transformer. Skip it for Fabric runtime.
                    continue
                }
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

tasks.shadowJar {
    exclude("architectury.common.json")
    configurations = listOf(project.configurations["shadowBundle"])

    // Extract javassist classes to make them available on main classpath
    dependencies {
        include(dependency("org.javassist:javassist:.*"))
    }

    archiveClassifier.set("dev-shadow")
}

// Extension system configuration for development
val devExtensionsDir = layout.projectDirectory.dir("run/config/jsMacros/Extensions")

val copyDevExtensions = tasks.register<Copy>("copyDevExtensions") {
    dependsOn(":extension:graal:jar", ":extension:graal:js:jar")
    from(project(":extension:graal").tasks.named("jar"))
    from(project(":extension:graal:js").tasks.named("jar"))
    into(devExtensionsDir)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.named<JavaExec>("runClient") {
    dependsOn(copyDevExtensions)
}

tasks.remapJar {
    inputFile.set(tasks.shadowJar.get().archiveFile)
    injectAccessWidener.set(true)
    archiveClassifier.set("fabric")
}