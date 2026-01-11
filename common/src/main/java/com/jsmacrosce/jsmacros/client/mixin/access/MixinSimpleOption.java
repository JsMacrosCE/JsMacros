package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OptionInstance.class)
public interface MixinSimpleOption {
    @Accessor("value")
    <T> void forceSetValue(T value);

}
