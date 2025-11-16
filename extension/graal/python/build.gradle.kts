plugins {
    java
}

// Get reference to the extension project for test dependencies
val extensionProject = project(":extension")

val graal_version: String by project.properties

dependencies {
    implementation(libs.graal.python)
    implementation(libs.graal.llvm)

    // Inherit test dependencies from grandparent extension module
    testImplementation(extensionProject)
    testImplementation(extensionProject.sourceSets.test.get().output)

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.jetbrains:annotations:20.1.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.test {
    useJUnitPlatform()
}