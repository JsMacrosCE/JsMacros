package com.jsmacrosce.jsmacros.client.access;

import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.Set;

@ApiStatus.Internal
@DocletIgnore
public interface IFontManager {
    Set<ResourceLocation> jsmacros_getFontList();

}
