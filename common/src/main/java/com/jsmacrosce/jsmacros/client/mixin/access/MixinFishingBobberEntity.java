package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(FishingHook.class)
public interface MixinFishingBobberEntity {

    @Accessor("biting")
    boolean getCaughtFish();

}
