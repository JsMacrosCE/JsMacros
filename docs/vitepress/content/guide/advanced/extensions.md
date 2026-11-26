---
description: "Install a JsMacrosCE extension, or build your own script library or language from the example project."
outline: deep
---
# Extensions

JavaScript is built into the mod. An extension can teach JsMacrosCE to run
another kind of script, or give scripts a new global library to call. You can
write one in an ordinary Java project using the published JsMacrosCE extension
API.

## Install an extension

Put the extension JAR in `config/jsMacros/Extensions/` in your Minecraft instance
and restart the client. The folder name uses a capital `E`. The JsMacrosCE mod
goes in `mods/`; extensions go in `Extensions/`. Keep your scripts in
`config/jsMacros/Macros/`.

For Python, download `jsmacrosce-ext-graalpy-<release>.jar` from the
[GitHub release](https://github.com/JsMacrosCE/JsMacros/releases) that matches
your installed JsMacrosCE version.

## Build the example

Start with the [Example Extension](https://github.com/JsMacrosCE/JsMacrosCE-Example-Extension).
It adds `.jexl` scripts and a global library called `ExampleGreeting`, so you can
see both kinds of extension in one project.

1. Clone the example, install JDK 21, and run `./gradlew build` from the example
   directory (`gradlew.bat build` on Windows).
2. Install `build/libs/jsmacrosce-example-extension-1.0.0.jar` as described above.
   This example currently targets JsMacrosCE 2.1.0.
3. Copy [`hello.jexl`](https://github.com/JsMacrosCE/JsMacrosCE-Example-Extension/blob/main/examples/hello.jexl)
   into `config/jsMacros/Macros/` and use **Run** in the JsMacros screen to run
   it once. You should see three greetings in chat, followed by
   `[PASS] 3 greetings; trigger: no event`.

`./gradlew test` loads the packaged JAR and runs JEXL with a stand-in for the
`Chat` library. You can run those tests without starting Minecraft.

## Use the extension API in your build

The example already has this set up. If you have your own Gradle project, add
the Maven repository and the dependency to `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven("https://maven.jsmacrosce.com/releases")
        }
        filter { includeModule("com.jsmacrosce.jsmacros", "extension-api") }
    }
}

dependencies {
    compileOnly("com.jsmacrosce.jsmacros:extension-api:2.1.0")
}
```

Use the oldest published JsMacrosCE release that has the API you need and that
you intend to support. Keep `extension-api` out of your extension JAR: the
installed mod supplies those classes at runtime. The published API targets Java
21 and covers the loader-independent core. An extension that uses only this API
is not tied to a Minecraft version or loader, so one JAR can serve every
supported target. If you import Minecraft or loader classes, build and test for
the Minecraft versions and loaders you intend to support.

## Make it your own

Pick a Java package and a unique ID for `getExtensionName()`. Under
`src/main/resources/META-INF/services/`, keep the filename
`com.jsmacrosce.jsmacros.core.extensions.Extension` and change its contents to
the fully qualified name of your provider class. JsMacrosCE uses this file to
find the provider, so give the class a public no-argument constructor. Set your
project and JAR name in `settings.gradle.kts` so the build output is easy to
identify.

### Add a library

Keep `LibraryExtension` on the provider and return your library classes from
`getLibraries()`. The example's `ExampleGreetingLibrary` extends `BaseLibrary`,
has a public constructor that takes `Core<?, ?>`, and uses
`@Library("ExampleGreeting")` to give scripts a global name. With the extension
installed, a JavaScript macro can call it like any other library:

```js
Chat.log(ExampleGreeting.greet("Ada"));
```

A `BaseLibrary` instance is shared between scripts. If each run needs its own
state, use `PerExecLibrary` with a public constructor that takes
`BaseScriptContext<?>`. For a library-only extension, remove `LanguageExtension`,
the JEXL adapter classes, and the JEXL dependency. You can leave `init` empty
if the provider has nothing to set up.

### Add a language

Implement `LanguageExtension` and choose a file suffix that other installed
languages do not use. The suffix is separate from the extension ID: the example
uses `jexl` for scripts and `examplejexl` for the ID. Follow `JexlLanguage` for
how to give both file and inline runs the same libraries and the `event`, `file`,
and `context` globals. An inline run may have no `file`, and either kind of run
may have no `event`.

Add your engine and its runtime dependencies to the example build's `embedded`
configuration. The build puts those JARs in `META-INF/jsmacroscedeps/` and lists
them in `jsmacrosce.ext.<id>.json` at the root of the extension JAR. The example
generates that descriptor from the same resolved JARs it packages, so it stays in
sync when you add a dependency. Change the filename in the build to match
`getExtensionName()`. If you have no dependencies to bundle, you can omit the
descriptor.

Bundled dependencies share a class loader with other extensions. Do not bundle
JsMacrosCE, Minecraft, or loader classes, and check for version conflicts when
another extension bundles the same library.

JsMacrosCE creates the provider before those nested JARs are available. Keep the
provider's constructor and static initializers independent of the engine
classes, and start the engine in `init`, after the loader has added them. If your
setup depends on another extension's registered libraries, put that part in
`onExtensionsLoaded`.

## Related pages

- [How a script runs](/guide/concepts/how-scripts-run)
- [Example Extension README](https://github.com/JsMacrosCE/JsMacrosCE-Example-Extension#readme)
- API reference: [`LibraryExtension`](/api/classes/com/jsmacrosce/jsmacros/core/extensions/LibraryExtension),
  [`LanguageExtension`](/api/classes/com/jsmacrosce/jsmacros/core/extensions/LanguageExtension)
