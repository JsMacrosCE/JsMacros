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
    testRuntimeOnly(libs.junit.engine)
}

tasks.test {
    useJUnitPlatform()
    enabled = false
}
