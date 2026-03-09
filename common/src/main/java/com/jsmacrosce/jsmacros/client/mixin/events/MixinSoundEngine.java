package com.jsmacrosce.jsmacros.client.mixin.events;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.jsmacrosce.jsmacros.client.api.event.impl.world.EventSound;

@Mixin(SoundEngine.class)
public class MixinSoundEngine {
    //? if >1.21.5 {
    @Inject(at = @At("HEAD"), method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", cancellable = true)
    public void onPlay(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> info) {
    //?} else {
    /*@Inject(at = @At("HEAD"), method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", cancellable = true)
    public void onPlay(SoundInstance instance, CallbackInfo info) {
    *///?}
        String id = null;
        try {
            id = instance
                    //? if >=1.21.11 {
                    /*.getIdentifier()
                    *///? } else {
                    .getLocation()
                    //? }
                    .toString();
        } catch (NullPointerException ignored) {
        }
        float volume = 1.0F;
        float pitch = 1.0F;
        try {
            volume = instance.getVolume();
            pitch = instance.getPitch();
        } catch (NullPointerException ignored) {
        }

        EventSound ev = new EventSound(id, volume, pitch, instance.getX(), instance.getY(), instance.getZ());
        ev.trigger();
        if (ev.isCanceled()) {
            info.cancel();
        }
    }

}
