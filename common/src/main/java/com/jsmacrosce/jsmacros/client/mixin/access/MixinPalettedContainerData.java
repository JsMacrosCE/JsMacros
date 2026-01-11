package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.util.BitStorage;
import net.minecraft.world.level.chunk.Palette;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.jsmacrosce.jsmacros.client.access.IPalettedContainerData;

@Mixin(targets = "net.minecraft.world.level.chunk.PalettedContainer$Data")
public class MixinPalettedContainerData<T> implements IPalettedContainerData<T> {

    @Shadow
    @Final
    private BitStorage storage;

    @Shadow
    @Final
    private Palette<T> palette;

    @Override
    public BitStorage jsmacros_getStorage() {
        return storage;
    }

    @Override
    public Palette<T> jsmacros_getPalette() {
        return palette;
    }

}
