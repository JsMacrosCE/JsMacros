import java.io.FilterReader
import java.nio.file.Path

plugins {
    id("multiloader-common")
    id("net.neoforged.moddev")
    alias(libs.plugins.fletching.common)
}

val mod_id = commonMod.prop("mod_id")
val minecraft_version = commonMod.prop("minecraft_version")

neoForge {
    commonMod.propOrNull("neo_form_version")?.let { neoFormVersion = it }

    accessTransformers.from(
        layout.buildDirectory.file("generated/access-transformer/accesstransformer.cfg")
    )

    val supportsParchment = stonecutterBuild.eval(stonecutterBuild.current.version, "<26.1")
    val parchmentMinecraft = commonMod.propOrNull("parchment_minecraft")
    val parchmentVersion = commonMod.propOrNull("parchment_version")

    if (supportsParchment && parchmentMinecraft != null && parchmentVersion != null) {
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

stonecutter {
    replacements.string(current.parsed >= "1.21.11") {
        replace("ResourceLocation", "Identifier")

        replace("net.minecraft.Util", "net.minecraft.util.Util")
        replace("net.minecraft.advancements.critereon", "net.minecraft.advancements.criterion")

        // Conflicts
        replace("parseIdentifier", "parseIdentifier")
        replace("getAdvancementsForIdentifiers", "getAdvancementsForIdentifiers")
        replace("suggestIdentifier", "suggestIdentifier")
        replace("mapIdentifiers", "mapIdentifiers")
        replace("getIdentifier", "getIdentifier")
        replace("writeIdentifier", "writeIdentifier")
        replace("readIdentifier", "readIdentifier")
        replace("getWorldIdentifier", "getWorldIdentifier")
        replace("base.readResourceLocation", "base.readIdentifier")
        replace("base.writeResourceLocation", "base.writeIdentifier")
        replace("@return the raw minecraft Identifier.", "@return the raw minecraft Identifier.")
    }

    replacements.string(current.parsed >= "26.1") {
        replace("GuiGraphics", "GuiGraphicsExtractor")

        // Conflicts
        // NeoForge's ScreenEvent.Render.Post accessor, which is still spelled this way
        replace("getGuiGraphics", "getGuiGraphics")
    }
}
