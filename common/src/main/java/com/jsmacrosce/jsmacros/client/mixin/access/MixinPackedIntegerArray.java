package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.util.SimpleBitStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.jsmacrosce.jsmacros.client.access.IPackedIntegerArray;

@Mixin(SimpleBitStorage.class)
public class MixinPackedIntegerArray implements IPackedIntegerArray {
    @Shadow
    @Final
    private long mask;

    @Shadow
    @Final
    private int valuesPerLong;

    @Shadow
    @Final
    private int divideMul;

    @Shadow
    @Final
    private int divideAdd;

    @Shadow
    @Final
    private int divideShift;

    @Override
    public long jsmacros_getMaxValue() {
        return mask;
    }

    @Override
    public int jsmacros_getElementsPerLong() {
        return valuesPerLong;
    }

    @Override
    public int jsmacros_getIndexScale() {
        return divideMul;
    }

    @Override
    public int jsmacros_getIndexOffset() {
        return divideAdd;
    }

    @Override
    public int jsmacros_getIndexShift() {
        return divideShift;
    }

}
