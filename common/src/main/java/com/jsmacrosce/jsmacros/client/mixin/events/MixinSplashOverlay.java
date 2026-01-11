package com.jsmacrosce.jsmacros.client.mixin.events;

import net.minecraft.client.gui.screens.LoadingOverlay;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.api.event.impl.EventResourcePackLoaded;

@Mixin(LoadingOverlay.class)
public class MixinSplashOverlay {
    @Shadow
    @Final
    private boolean fadeIn;

    @Inject(
            //? if >1.21.8 {
            /*method = "tick",
            *///?} else {
            method = "render",
            //?}
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;fadeOutStart:J", opcode = Opcodes.PUTFIELD))
    private void onReloadComplete(CallbackInfo ci) {
        new EventResourcePackLoaded(!fadeIn).trigger();
    }

}
