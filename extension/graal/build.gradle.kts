
dependencies {
    // Graal core dependencies
    implementation(libs.bundles.graal.core)
    implementation(libs.graal.js)

    // Common library dependencies
    implementation(libs.bundles.common.libs)
}

var projectPath = project.path

subprojects {
    dependencies {
        implementation(project(projectPath))

        // Inherit parent dependencies
        parent!!.configurations.implementation.get().dependencies.forEach { dependency ->
            implementation(dependency)
        }

        testImplementation(parent!!.sourceSets.test.get().output)
        for (dependency in parent!!.configurations.testImplementation.get().dependencies) {
            testImplementation(dependency)
        }
    }
}

tasks.test {
    useJUnitPlatform()
}