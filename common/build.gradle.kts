import java.io.FilterReader
import java.nio.file.Path
import org.gradle.api.tasks.testing.Test

plugins {
    id("multiloader-common")
    id("net.neoforged.moddev")
    alias(libs.plugins.fletching.common)
}

val mod_id = commonMod.modId
val minecraft_version = commonMod.mc

neoForge {
    commonMod.propOrNull("neo_form_version")?.let { neoFormVersion = it }

    accessTransformers.from(
        layout.buildDirectory.file("generated/access-transformer/accesstransformer.cfg")
    )

    // Only declared for obfuscated versions; 26.1+ is unobfuscated and has no Parchment release.
    val parchmentMinecraft = commonMod.propOrNull("parchment_minecraft")
    val parchmentVersion = commonMod.propOrNull("parchment_version")

    if (parchmentMinecraft != null && parchmentVersion != null) {
        parchment {
            minecraftVersion = parchmentMinecraft
            mappingsVersion = parchmentVersion
        }
    }
}

dependencies {
    compileOnly(libs.mixin)

    // fabric and neoforge both bundle mixinextras, so it is safe to use it in common
    compileOnly(libs.mixinextras)
    annotationProcessor(libs.mixinextras)

    // ASM for bytecode manipulation
    compileOnly(libs.asm.tree)
    // The adapter checks also inspect Java 25 class files; production ASM is unchanged.
    testImplementation("org.ow2.asm:asm-tree:9.10.1")

    // Common library dependencies
    compileOnly(libs.prism4j)
    compileOnly(libs.joor)
    compileOnly(libs.nv.websocket)
    compileOnly(libs.javassist)
}

val commonJava by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val commonResources by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

// Get the Stonecutter-generated sources directory
val stonecutterGeneratedJava = layout.buildDirectory.dir("generated/stonecutter/main/java")
val stonecutterGeneratedResources = layout.buildDirectory.dir("generated/stonecutter/main/resources")

artifacts {
    // Use the Stonecutter-generated sources after processing
    add(commonJava.name, stonecutterGeneratedJava) {
        builtBy("stonecutterGenerate")
    }
    add(commonResources.name, stonecutterGeneratedResources) {
        builtBy("stonecutterGenerate")
    }
}

tasks.named("createMinecraftArtifacts") {
    val mcVersion = project.name  // The project name is the minecraft version (e.g., "1.21.8")
    dependsOn(":common:${mcVersion}:stonecutterGenerate")
}

val accessTransformerFile = layout.buildDirectory.file("generated/access-transformer/accesstransformer.cfg")
val accessWidenerFile = rootProject.file(
    "common/src/main/resources/accesswideners/$minecraft_version-$mod_id.accesswidener"
)
val generateAccessTransformer by tasks.registering(Copy::class) {
    from(accessWidenerFile)
    into(accessTransformerFile.map { it.asFile.parentFile })
    rename { "accesstransformer.cfg" }
    val transformerClass = Class.forName(
        "dev.kikugie.fletching_table.transformer.Aw2AtFileTransformer"
    ) as Class<out FilterReader>
    val argsClass = Class.forName(
        "dev.kikugie.fletching_table.transformer.Aw2AtFileTransformer\$TransformArgs"
    )
    val args = argsClass.getDeclaredConstructor(Path::class.java)
        .newInstance(accessWidenerFile.toPath())
    filter(mapOf("args" to args), transformerClass)
}

tasks.named("createMinecraftArtifacts") {
    dependsOn(generateAccessTransformer)
}

fletchingTable {
    accessConverter.register(sourceSets.main) {
        add("accesswideners/$minecraft_version-$mod_id.accesswidener")
    }
}

val testStringHashTrie by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Checks StringHashTrie contents, lookups and internal shape against a reference set"
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.jsmacrosce.StringHashTrieTest")
}

tasks.named<Test>("test") {
    // the JUnit platform is not on this module's classpath; the check above is the test task here,
    // which is why this one would otherwise fail with "no tests to execute"
    enabled = false
}

tasks.named("check") {
    dependsOn(testStringHashTrie)
}

// These checks inspect compiled adapters and native render state without starting a client.
sourceSets.named("test") {
    compileClasspath += sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().compileClasspath
}
val testSurfaceRenderTypes = tasks.register<JavaExec>("testSurfaceRenderTypes") {
    group = "verification"
    description = "Checks overlay shader inputs, vertex layouts and render state"
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.jsmacrosce.SurfaceRenderTypesTest")
}
tasks.named("check") {
    dependsOn(testSurfaceRenderTypes)
}

val testClientCompat = tasks.register<JavaExec>("testClientCompat") {
    group = "verification"
    description = "Checks version-specific GUI/render adapter dispatch and scoped replacements"
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.jsmacrosce.ClientCompatTest")
}
tasks.named("check") {
    dependsOn(testClientCompat)
}

val testFormattingCompat = tasks.register<JavaExec>("testFormattingCompat") {
    group = "verification"
    description = "Checks legacy formatting IDs and color lookup boundaries"
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.jsmacrosce.FormattingCompatTest")
}
tasks.named("check") {
    dependsOn(testFormattingCompat)
}

val testColorMath = tasks.register<JavaExec>("testColorMath") {
    group = "verification"
    description = "Checks packed-ARGB packing, validation and the render alpha policy"
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.jsmacrosce.ColorMathTest")
}
tasks.named("check") {
    dependsOn(testColorMath)
}

val testDraw3DCollection = tasks.register<JavaExec>("testDraw3DCollection") {
    group = "verification"
    description = "Checks Draw3D element snapshots, removal, clearing and visibility state"
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.jsmacrosce.Draw3DCollectionTest")
}
tasks.named("check") {
    dependsOn(testDraw3DCollection)
}

// Bootstrap on 1.21.x expects the client language assets, which this test classpath does not
// carry. The 1.21.x targets are covered by compilation; runtime checks run on 26.1+.
if (stonecutter.current.parsed >= "26.1") {
    val testBlockStateHelper = tasks.register<JavaExec>("testBlockStateHelper") {
        group = "verification"
        description = "Checks the legacy movement-blocking predicate without starting a client"
        dependsOn(tasks.named("testClasses"))
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("com.jsmacrosce.BlockStateHelperTest")
    }
    tasks.named("check") {
        dependsOn(testBlockStateHelper)
    }
}

listOf("InputCompat", "PacketBufferCompat", "ScriptTypeRegistry").forEach { checkName ->
    val adapterCheck = tasks.register<JavaExec>("test$checkName") {
        group = "verification"
        description = "Checks $checkName compatibility contracts without starting a client"
        dependsOn(tasks.named("testClasses"))
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("com.jsmacrosce.${checkName}Test")
    }
    tasks.named("check") { dependsOn(adapterCheck) }
}

if (stonecutter.current.parsed >= "26.2") {
    val testRenderSink = tasks.register<JavaExec>("testRenderSink") {
        group = "verification"
        description = "Checks native overlay submission order, depth phases and pose snapshots"
        dependsOn(tasks.named("testClasses"))
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("com.jsmacrosce.RenderSinkTest")
    }
    tasks.named("check") {
        dependsOn(testRenderSink)
    }
}

if (stonecutter.current.parsed >= "26.3") {
    val mixinCheck = tasks.register<JavaExec>("testMixinBindings") {
        group = "verification"
        description = "Checks common Mixin member descriptors and injection selectors against the exact target"
        dependsOn(tasks.named("testClasses"))
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("com.jsmacrosce.MixinBindingsTest")
        args(sourceSets.main.get().output.classesDirs.files.map { it.absolutePath })
    }
    val defaultsCheck = tasks.register<JavaExec>("testVanillaDataDefaults") {
        group = "verification"
        description = "Checks vanilla brewing and fuel fallback data without starting a client or server"
        dependsOn(tasks.named("testClasses"))
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("com.jsmacrosce.VanillaDataDefaultsTest")
    }
    tasks.named("check") { dependsOn(defaultsCheck, mixinCheck) }
}

stonecutter {
    minecraftReplacements()
}
