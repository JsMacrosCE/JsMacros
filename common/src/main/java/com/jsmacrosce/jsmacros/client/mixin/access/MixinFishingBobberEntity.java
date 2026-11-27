package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(FishingHook.class)
@ApiStatus.Internal
@DocletIgnore
public interface MixinFishingBobberEntity {

    @Accessor("biting")
    boolean getCaughtFish();

}
