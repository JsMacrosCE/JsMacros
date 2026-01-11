package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(AbstractHorse.class)
public interface MixinAbstractHorseEntity {

    @Invoker
    int invokeGetInventorySize();

}
