plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version "2.2.0"
}

repositories {
    maven("https://maven.neoforged.net/releases")
    maven("https://maven.kikugie.dev/snapshots")
    mavenCentral()
    gradlePluginPortal()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(gradleApi())
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("commons-io:commons-io:2.7")
    implementation("dev.kikugie:stonecutter:0.8.3")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
}

val testDocBodyRenderer by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Checks example language rendering without a Minecraft classpath"
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.jsmacrosce.doclet.core.util.DocBodyRendererTest")
}

tasks.named("check") {
    dependsOn(testDocBodyRenderer)
}
