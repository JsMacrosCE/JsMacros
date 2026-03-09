package com.jsmacrosce.jsmacros.client.mixin.access;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.equine.AbstractHorse;
*///? } else {
import net.minecraft.world.entity.animal.horse.AbstractHorse;
//? }

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(AbstractHorse.class)
public interface MixinAbstractHorseEntity {

    @Invoker
    int invokeGetInventorySize();

}
