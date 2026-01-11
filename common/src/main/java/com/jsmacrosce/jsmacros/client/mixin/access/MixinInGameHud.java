package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw2D;
import com.jsmacrosce.jsmacros.client.api.classes.render.IDraw2D;
import com.jsmacrosce.jsmacros.client.api.library.impl.FHud;

@Mixin(Gui.class)
public class MixinInGameHud {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderHud(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (!FHud.overlays.isEmpty()) {
            for (IDraw2D<Draw2D> overlay : FHud.overlays) {
                overlay.render(context);
            }
        }
    }
}
