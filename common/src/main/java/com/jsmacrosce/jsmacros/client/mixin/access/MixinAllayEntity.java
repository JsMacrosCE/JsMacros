package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.world.entity.animal.allay.Allay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(Allay.class)
public interface MixinAllayEntity {

    @Invoker
    boolean invokeCanDuplicate();

}
