package com.jsmacrosce.jsmacros.client.mixin.events;

import com.jsmacrosce.jsmacros.client.util.InputCompat;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
//? if >1.21.8 {
/*import net.minecraft.client.input.KeyEvent;
*///?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.api.event.impl.EventKey;

@Mixin(KeyboardHandler.class)
class MixinKeyboard {

    @Shadow
    @Final
    private Minecraft minecraft;

    //? if >1.21.8 {
    /*@Inject(at = @At("HEAD"), method = "keyPress", cancellable = true)
    private void keyPress(long window, int action, KeyEvent keyEvent, CallbackInfo info) {
        if (window != minecraft.getWindow().handle()) {
            return;
        }
        if (InputConstants.getKey(keyEvent) == InputConstants.UNKNOWN
                || (action != InputConstants.PRESS && action != InputConstants.RELEASE)) {
            return;
        }
        if (EventKey.parse(InputConstants.getKey(keyEvent), action, InputCompat.modifiers(keyEvent.modifiers()))) {
            info.cancel();
        }
    }
    *///?} else {
    @Inject(at = @At("HEAD"), method = "keyPress", cancellable = true)
    private void onKey(long window, int key, int scancode, int action, int mods, final CallbackInfo info) {
        if (window != minecraft.getWindow().getWindow()) {
            return;
        }
        if (key == -1 || action == 2) {
            return;
        }
        if (EventKey.parse(key, scancode, action, mods)) {
            info.cancel();
        }
    }
    //?}
}
