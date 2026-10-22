package com.jsmacrosce.jsmacros.client.access;

import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

@DocletIgnore
public interface IFontManager {
    Set<ResourceLocation> jsmacros_getFontList();

}
