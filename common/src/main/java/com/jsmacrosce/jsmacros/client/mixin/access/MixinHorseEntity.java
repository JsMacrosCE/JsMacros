package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.equine.Horse;
*///? } else {

import net.minecraft.world.entity.animal.horse.Horse;
//? }

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(Horse.class)
@DocletIgnore
public interface MixinHorseEntity {

    @Invoker("getTypeVariant")
    int invokeGetHorseVariant();

}
