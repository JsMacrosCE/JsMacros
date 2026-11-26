# Developing extensions

Standalone language and library extensions can compile against the Java 21
JsMacrosCE core API in their own Gradle project:

```kotlin
repositories {
    mavenCentral()
    maven("https://maven.jsmacrosce.com/releases") {
        content { includeModule("com.jsmacrosce.jsmacros", "extension-api") }
    }
}

dependencies {
    compileOnly("com.jsmacrosce.jsmacros:extension-api:<JsMacrosCE version>")
}
```

Use a published version of JsMacrosCE that you intend to support. The API
JAR covers the loader-independent core. Code that imports Minecraft or
loader types needs a build for that target. Keep the API JAR out of your
extension package: the installed JsMacrosCE mod supplies those classes.
For integration tests that use built-in engines or client libraries, use a
matching mod JAR on the test runtime classpath.

The `extension` directory in this repository contains JsMacrosCE's shipped
Graal adapters and extension loader tests.

## Publishing the API

The stable **Release Build** workflow publishes `extension-api` after the mod
release job succeeds. It uses the same source revision and version as the mod
release. The beta and dev workflows do not deploy Maven artifacts.

In the GitHub repository, open **Settings → Environments → New environment**
and name it `maven-releases`. Restrict deployment to your trusted release
branch (normally `main`) and add these **environment secrets**:

| Secret | Value |
| --- | --- |
| `MAVEN_NAME` | The Reposilite token name, e.g. `jsmacrosce-ci`. This is the name passed to `token-generate`, used as the HTTP Basic username. |
| `MAVEN_SECRET` | The generated Reposilite token secret, used as the password. |

The token needs write access on `/releases/com/jsmacrosce/jsmacros/extension-api/`
or its parent route `/releases/com/jsmacrosce/`. Consumers need anonymous read
access. If only the Maven job fails, rerun that job from the original release
run so it uses the same version and commit. If Reposilite already received
some of the version's files, inspect the coordinate before retrying: Reposilite
can reject redeployments of existing files.

To verify the publication locally without any credentials:

```sh
./gradlew :extension-api:publishToMavenLocal \
  "-Dmaven.repo.local=$PWD/build/test-maven"
```
