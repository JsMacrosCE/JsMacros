import org.gradle.api.file.DuplicatesStrategy

plugins {
    alias(libs.plugins.architectury.plugin)
    alias(libs.plugins.architectury.loom)
    id("java")
    id("maven-publish")
}

architectury {
    common("fabric", "neoforge")
}

loom {
    accessWidenerPath.set(file("src/main/resources/jsmacros.accesswidener"))

    mixin {
        defaultRefmapName.set("jsmacros.refmap.json")
        useLegacyMixinAp.set(true)
    }
}


dependencies {
    // Minecraft dependencies (platform-agnostic)
    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())

    // Shared dependencies
    compileOnly(libs.mixin)
    compileOnly(libs.mixin.extra)
    implementation(libs.bundles.minecraft.libs)
    implementation(libs.bundles.scripting.libs)
    implementation(libs.asm.tree)

    // Additional dependencies for standalone compilation
    implementation(libs.bundles.common.libs)
    implementation(libs.commons.codec)
    implementation(libs.commons.io)
    implementation(libs.commons.lang3)
    implementation(libs.joml)
    implementation(libs.jb.annotations)

    annotationProcessor("org.spongepowered:mixin:0.8.7:processor")
}

java {
    withSourcesJar()
}

tasks.processResources {
    dependsOn(tasks.named("compileJava"))

    val refmap = layout.buildDirectory.file("classes/java/main/jsmacros.refmap.json")
    from(refmap)

    filesMatching("jsmacros.extension.json") {
        expand("dependencies" to "")
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}