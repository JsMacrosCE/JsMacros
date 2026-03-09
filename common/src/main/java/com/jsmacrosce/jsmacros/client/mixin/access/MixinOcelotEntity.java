package com.jsmacrosce.jsmacros.client.mixin.access;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.feline.Ocelot;
*///? } else {
import net.minecraft.world.entity.animal.Ocelot;
//? }

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(Ocelot.class)
public interface MixinOcelotEntity {

    @Invoker
    boolean invokeIsTrusting();

}
