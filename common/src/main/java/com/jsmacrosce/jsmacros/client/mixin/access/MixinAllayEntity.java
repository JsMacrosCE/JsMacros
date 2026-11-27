package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;

import net.minecraft.world.entity.animal.allay.Allay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(Allay.class)
@ApiStatus.Internal
@DocletIgnore
public interface MixinAllayEntity {

    @Invoker
    boolean invokeCanDuplicate();

}
