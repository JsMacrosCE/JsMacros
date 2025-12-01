plugins {
    java
}

repositories {
    mavenLocal()
    maven("https://maven.wagyourtail.xyz/releases")
    maven("https://maven.wagyourtail.xyz/snapshots")
    maven("https://maven.neoforged.net/releases")
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.toVersion(21)
    targetCompatibility = JavaVersion.toVersion(21)
}

dependencies {
    implementation(gradleApi())
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("commons-io:commons-io:2.7")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}