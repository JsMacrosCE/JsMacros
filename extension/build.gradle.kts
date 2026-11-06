plugins {
    id("jsmacros-extension")
}

val minecraftVersion = EXTENSION_CORE_TARGET

base {
    archivesName.set("${property("mod_id")}-extension")
}

dependencies {
    // Extension system dependencies
    implementation(libs.slf4j)
    implementation(libs.guava.extension)

    // Test dependencies
    testImplementation(project(":common:${minecraftVersion}"))
    testImplementation(libs.junit.api)
    testImplementation(libs.jetbrains.annotations)
    testImplementation(libs.javassist)
    // The core reads its dependency and translation descriptors with gson, and its task queues
    // with fastutil, neither of which a java-library module passes on to its consumers.
    testImplementation(libs.gson)
    testImplementation(libs.fastutil)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
