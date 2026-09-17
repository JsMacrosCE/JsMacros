package com.jsmacrosce.jsmacros.fabric.client.mixins.access;

import com.google.common.collect.ImmutableSet;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.api.classes.render.IDraw2D;
import com.jsmacrosce.jsmacros.client.api.library.impl.FHud;

import java.util.Comparator;

//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///? } else {
import net.minecraft.client.gui.GuiGraphics;
//?}

@MixinEnvironment("fabric")
@Mixin(DebugScreenOverlay.class)
class MixinDebugHud {

    //? if >=26.1 {
    /*@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", at = @At("TAIL"))
    private void afterExtractRenderState(GuiGraphicsExtractor context, CallbackInfo ci) {
        jsmacrosce_renderOverlays(context);
    }
    *///? } else if >1.21.8 {
    /*@Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("TAIL"))
    private void afterRender(GuiGraphics context, CallbackInfo ci) {
        jsmacrosce_renderOverlays(context);
    }
    *///?} else {
    @Inject(method = "drawGameInformation(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("TAIL"))
    private void afterDrawLeftText(GuiGraphics context, CallbackInfo ci) {
        jsmacrosce_renderOverlays(context);
    }
    //?}

    @Unique
    private void jsmacrosce_renderOverlays(
            //? if >=26.1 {
            /*GuiGraphicsExtractor context
            *///? } else {
            GuiGraphics context
            //? }
    ) {
        DebugScreenOverlay self = (DebugScreenOverlay) (Object) this;
        if (!self.showDebugScreen()) return;

        ImmutableSet.copyOf(FHud.overlays).stream()
                .sorted(Comparator.comparingInt(IDraw2D::getZIndex))
                .forEachOrdered(hud -> hud.render(context));
    }
}
