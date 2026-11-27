package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(Creeper.class)
@ApiStatus.Internal
@DocletIgnore
public interface MixinCreeperEntity {

    @Accessor("swell")
    int getFuseTime();

    @Accessor("maxSwell")
    int getMaxFuseTime();

}
