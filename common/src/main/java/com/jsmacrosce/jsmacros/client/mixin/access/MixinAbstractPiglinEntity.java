package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;

import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Invokes {@link AbstractPiglin}'s protected zombification-immunity check.
 *
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(AbstractPiglin.class)
@DocletIgnore
public interface MixinAbstractPiglinEntity {

    @Invoker
    boolean invokeIsImmuneToZombification();

}
