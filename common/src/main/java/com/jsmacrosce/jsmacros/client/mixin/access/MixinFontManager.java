package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.jsmacrosce.jsmacros.client.access.IFontManager;

import java.util.Map;
import java.util.Set;

@Mixin(FontManager.class)
public class MixinFontManager implements IFontManager {
    @Shadow
    @Final
    private Map<ResourceLocation, FontSet> fontSets;

    @Override
    public Set<ResourceLocation> jsmacros_getFontList() {
        return fontSets.keySet();
    }

}
