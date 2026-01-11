package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.world.entity.animal.horse.Horse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(Horse.class)
public interface MixinHorseEntity {

    @Invoker("getTypeVariant")
    int invokeGetHorseVariant();

}
