plugins {
    id("multiloader-common")
    id("net.neoforged.moddev")
}

neoForge {
    neoFormVersion = property("neo_form_version").toString()

    // Automatically enable AccessTransformers if the file exists
    val at = file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }

    parchment {
        minecraftVersion = property("parchment_minecraft").toString()
        mappingsVersion = property("parchment_version").toString()
    }
}

dependencies {
    compileOnly("org.spongepowered:mixin:0.8.5")

    // fabric and neoforge both bundle mixinextras, so it is safe to use it in common
    compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")

    // ASM for bytecode manipulation
    compileOnly("org.ow2.asm:asm-tree:9.6")

    // Common library dependencies
    compileOnly("io.noties:prism4j:2.0.0")
    compileOnly("org.jooq:joor:0.9.15")
    compileOnly("com.neovisionaries:nv-websocket-client:2.14")
    compileOnly("org.javassist:javassist:3.30.2-GA")
}

val commonJava by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val commonResources by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

artifacts {
    sourceSets["main"].java.srcDirs.forEach { dir ->
        add(commonJava.name, dir)
    }
    sourceSets["main"].resources.srcDirs.forEach { dir ->
        add(commonResources.name, dir)
    }
}
