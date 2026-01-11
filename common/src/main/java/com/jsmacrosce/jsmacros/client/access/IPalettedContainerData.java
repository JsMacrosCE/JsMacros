package com.jsmacrosce.jsmacros.client.access;

import net.minecraft.util.BitStorage;
import net.minecraft.world.level.chunk.Palette;

public interface IPalettedContainerData<T> {

    BitStorage jsmacros_getStorage();

    Palette<T> jsmacros_getPalette();

}
