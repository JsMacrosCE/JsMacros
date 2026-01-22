plugins {
    `java-library`
}

base {
    archivesName.set("${property("mod_id")}-extension")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(property("java_version").toString().toInt()))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
}

// Get minecraft version from stonecutter.active file
val minecraftVersion = rootProject.file("stonecutter.active").readText().trim()

dependencies {
    // Compile against shared common code for tests
    testImplementation(project(":common:${minecraftVersion}"))
    
    // Extension system dependencies
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("com.google.guava:guava:31.1-jre")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.jetbrains:annotations:20.1.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.test {
    useJUnitPlatform()
}
