plugins {
    id("java")
    id("maven-publish")
}

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net/")
    maven("https://jitpack.io")
}

dependencies {
    // Shared dependencies
    compileOnly(libs.mixin)
    compileOnly(libs.mixin.extra)
    implementation(libs.asm)
    implementation(libs.prism4j)
    implementation(libs.joor)
    implementation(libs.nv.websocket.client)
    implementation(libs.javassist)

    // Additional dependencies for standalone compilation
    implementation(libs.guava)
    implementation(libs.gson)
    implementation(libs.slf4j.api)
    implementation(libs.fastutil)
    implementation(libs.commons.codec)
    implementation(libs.commons.io)
    implementation(libs.commons.lang3)
    implementation(libs.joml)
    implementation(libs.jb.annotations)
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    filesMatching("jsmacros.extension.json") {
        expand("dependencies" to "")
    }
}