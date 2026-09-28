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
