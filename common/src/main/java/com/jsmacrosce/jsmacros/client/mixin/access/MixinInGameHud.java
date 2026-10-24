package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;

import net.minecraft.client.DeltaTracker;
//? if >=26.2 {
/*import net.minecraft.client.gui.Hud;
*///?} else {
import net.minecraft.client.gui.Gui;
//?}
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.api.classes.render.IDraw2D;
import com.jsmacrosce.jsmacros.client.api.library.impl.FHud;

import java.util.Comparator;

//? if >=26.2 {
/*@Mixin(Hud.class)
*///?} else {
@Mixin(Gui.class)
//?}
@DocletIgnore
public class MixinInGameHud {
    //? if >=26.1 {
    /*@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"))
    private void onExtractHud(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
    *///? } else {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderHud(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
    //? }
        FHud.overlays.stream()
                .sorted(Comparator.comparingInt(IDraw2D::getZIndex))
                .forEach(overlay -> overlay.render(context));
    }
}
