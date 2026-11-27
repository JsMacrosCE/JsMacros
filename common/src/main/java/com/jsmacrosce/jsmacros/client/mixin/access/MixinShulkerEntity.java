package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;

import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(Shulker.class)
@ApiStatus.Internal
@DocletIgnore
public interface MixinShulkerEntity {

    @Invoker
    boolean invokeIsClosed();

}
