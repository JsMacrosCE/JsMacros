package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(Creeper.class)
public interface MixinCreeperEntity {

    @Accessor("swell")
    int getFuseTime();

    @Accessor("maxSwell")
    int getMaxFuseTime();

}
