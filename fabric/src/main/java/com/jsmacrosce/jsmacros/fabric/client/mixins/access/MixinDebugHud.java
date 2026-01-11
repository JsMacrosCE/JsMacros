package com.jsmacrosce.jsmacros.fabric.client.mixins.access;

import com.google.common.collect.ImmutableSet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.api.classes.render.IDraw2D;
import com.jsmacrosce.jsmacros.client.api.library.impl.FHud;

import java.util.Comparator;

@Mixin(DebugScreenOverlay.class)
class MixinDebugHud {
    // TODO: I didn't want to find the mixin reference, changed in 1.21.9 or 1.21.10
    
    //? if >1.21.8 {
    //?} else {
    @Inject(
            method = "drawGameInformation(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At("TAIL")
    )
    private void afterDrawLeftText(GuiGraphics context, CallbackInfo ci) {
        DebugScreenOverlay self = (DebugScreenOverlay) (Object) this;
        if (!self.showDebugScreen()) return;

        ImmutableSet.copyOf(FHud.overlays).stream()
                .sorted(Comparator.comparingInt(IDraw2D::getZIndex))
                .forEachOrdered(hud -> hud.render(context));
    }
    //?}
}
