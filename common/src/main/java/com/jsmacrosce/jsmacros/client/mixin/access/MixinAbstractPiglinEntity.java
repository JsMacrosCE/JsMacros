package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(AbstractPiglin.class)
public interface MixinAbstractPiglinEntity {

    @Invoker
    boolean invokeIsImmuneToZombification();

}
