rootProject.name = "jsmacros"

pluginManagement {
    repositories {
        mavenLocal()
        maven("https://maven.wagyourtail.xyz/releases")
        maven("https://maven.wagyourtail.xyz/snapshots")
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.fabricmc.net/")
        maven("https://files.minecraftforge.net/maven/")
        maven("https://maven.architectury.dev/")
        mavenCentral()
        maven("https://jitpack.io")
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include("common")
include("fabric")
include("neoforge")

include("site")

include("extension")
for (file in file("extension").listFiles() ?: emptyArray()) {
    if (!file.isDirectory || file.name in listOf("build", "src", ".gradle", "gradle")) continue
    include("extension:${file.name}")

    if (file.resolve("subprojects.txt").exists()) {
        for (subproject in file.resolve("subprojects.txt").readLines()) {
            include("extension:${file.name}:$subproject")
        }
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        for (file in file("extension").listFiles() ?: emptyArray()) {
            if (!file.isDirectory || file.name in listOf("build", "src", ".gradle", "gradle")) continue
            val extensionName = file.name
            val libPath = "extension/$extensionName/gradle/$extensionName.versions.toml"

            if (file(libPath).exists()) {
                create("${extensionName}Libs") {
                    from(files(libPath))
                }
            }
        }
    }
}
