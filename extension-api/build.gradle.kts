import org.gradle.api.credentials.PasswordCredentials
import org.gradle.authentication.http.BasicAuthentication

plugins {
    id("jsmacros-extension")
    `maven-publish`
}

group = rootProject.group
version = rootProject.version

// Compile the same core sources as the oldest supported host, without a game or loader
// classpath. Accidental Minecraft dependencies then fail here instead of reaching consumers.
val common = project(":common:$EXTENSION_CORE_TARGET")
val coreSources = files(common.layout.buildDirectory.dir("generated/stonecutter/main/java"))
    .builtBy("${common.path}:stonecutterGenerate")

sourceSets.main {
    java.srcDir(coreSources)
    java.include(
        "com/jsmacrosce/jsmacros/core/**",
        "com/jsmacrosce/doclet/**",
        "com/jsmacrosce/Pair.java",
        "com/jsmacrosce/SynchronizedWeakHashSet.java",
        "com/jsmacrosce/Util.java",
        "com/jsmacrosce/jsmacros/util/ColorMath.java"
    )
}

dependencies {
    // Extension authors can reference these types in core signatures. The host supplies them
    // at runtime, so the publication exposes them only on the Gradle compile variant.
    compileOnlyApi(libs.gson)
    compileOnlyApi(libs.slf4j)
    compileOnlyApi(libs.fastutil)
    compileOnlyApi(libs.jetbrains.annotations)
    compileOnlyApi(libs.javassist)
    compileOnlyApi(libs.joor)
    compileOnlyApi(libs.nv.websocket)

    compileOnly(libs.guava.extension)
    compileOnly("commons-io:commons-io:2.17.0")
}

tasks.withType<Jar>().configureEach {
    from(rootProject.layout.projectDirectory.file("LICENSE")) { into("META-INF") }
}

val sourceRepository = "https://github.com/JsMacrosCE/JsMacros"
val releaseChannel = providers.gradleProperty("channel").orElse("release").get()

publishing {
    publications {
        create<MavenPublication>("extensionApi") {
            from(components["java"])
            pom {
                name.set("JsMacrosCE Extension API")
                description.set("Compile-time core API for standalone JsMacrosCE extensions")
                url.set(sourceRepository)
                licenses {
                    license {
                        name.set("Mozilla Public License 2.0")
                        url.set("https://www.mozilla.org/MPL/2.0/")
                    }
                }
                scm { url.set(sourceRepository) }
            }
        }
    }

    repositories {
        // Beta and dev builds can still be published locally for testing.
        if (releaseChannel == "release") {
            maven {
                name = "reposilite"
                url = uri("https://maven.jsmacrosce.com/releases")
                credentials(PasswordCredentials::class)
                authentication { create<BasicAuthentication>("basic") }
            }
        }
    }
}
