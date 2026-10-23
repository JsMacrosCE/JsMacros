import dev.kikugie.stonecutter.build.StonecutterBuildExtension

// Share symbol/package renames. Ownership changes use typed Java compat helpers.
fun StonecutterBuildExtension.minecraftReplacements() {
    replacements.string(current.parsed >= "1.21.11") {
        replace("ResourceLocation", "Identifier")
        replace("net.minecraft.Util", "net.minecraft.util.Util")
        replace("net.minecraft.advancements.critereon", "net.minecraft.advancements.criterion")

        // Preserve script-facing names and unrelated identifiers containing the renamed type.
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
        // NeoForge's ScreenEvent.Render.Post accessor retains the old name.
        replace("getGuiGraphics", "getGuiGraphics")
    }

    replacements.string(current.parsed >= "26.2") {
        replace("net.minecraft.advancements.criterion", "net.minecraft.advancements.predicates")
        replace("net.minecraft.world.entity.monster.Slime", "net.minecraft.world.entity.monster.cubemob.Slime")
    }

    // 0.8.4 supports opt-in file headers, not local replacement scopes. Only MacroContainer
    // opts into this overload adaptation; do not rewrite unrelated OpenGL calls globally.
    replacements.string("macro_icon_blend", current.parsed >= "26.2") {
        replace("GlStateManager._enableBlend()", "GlStateManager._enableBlend(0)")
        replace("GlStateManager._disableBlend()", "GlStateManager._disableBlend(0)")
    }
}
