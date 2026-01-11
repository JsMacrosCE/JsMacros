package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.jsmacrosce.jsmacros.client.access.IChunkSection;

@Mixin(LevelChunkSection.class)
public class MixinChunkSelection implements IChunkSection {

    @Shadow
    private short nonEmptyBlockCount;

    @Shadow
    private short tickingBlockCount;

    @Shadow
    private short tickingFluidCount;

    @Override
    public short jsmacros_getNonEmptyBlockCount() {
        return nonEmptyBlockCount;
    }

    @Override
    public short jsmacros_getRandomTickableBlockCount() {
        return tickingBlockCount;
    }

    @Override
    public short jsmacros_getNonEmptyFluidCount() {
        return tickingFluidCount;
    }

}
