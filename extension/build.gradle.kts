plugins {
    `java-library`
}

val minecraftVersion = rootProject.file("stonecutter.active").readText().trim()
val versionProject = project(":${minecraftVersion}")
val targetJavaVersion = versionProject.property("java_version").toString().toInt()

configurations.configureEach {
    if (isCanBeResolved) {
        attributes.attribute(org.gradle.api.attributes.java.TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, targetJavaVersion)
    }
}

base {
    archivesName.set("${property("mod_id")}-extension")
}

java {
    sourceCompatibility = JavaVersion.toVersion(targetJavaVersion)
    targetCompatibility = JavaVersion.toVersion(targetJavaVersion)
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
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
    enabled = false
}

tasks.named("compileTestJava") {
    enabled = false
}
