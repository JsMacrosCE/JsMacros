package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.world.entity.animal.Ocelot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(Ocelot.class)
public interface MixinOcelotEntity {

    @Invoker
    boolean invokeIsTrusting();

}
