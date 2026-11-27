package com.jsmacrosce.jsmacros.client.access;

import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.chunk.Palette;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@DocletIgnore
public interface IPalettedContainerData<T> {

    BitStorage jsmacros_getStorage();

    Palette<T> jsmacros_getPalette();

}
