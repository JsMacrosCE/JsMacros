package com.jsmacrosce.jsmacros.fabric.client.mixins.access;

import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.access.IScreenInternal;
import com.jsmacrosce.jsmacros.client.api.classes.InteractionProxy;
import com.jsmacrosce.jsmacros.client.api.classes.render.ScriptScreen;

@MixinEnvironment("fabric")
@Mixin(value = GameRenderer.class)
public class MixinGameRenderer {
    @Shadow
    @Final
    private Minecraft minecraft;

//? if >=26.1 {
    /*@Redirect(method = "extractGui", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void onRender(Screen instance, GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
        instance.extractRenderStateWithTooltipAndSubtitles(drawContext, mouseX, mouseY, delta);
        if (!(minecraft.screen instanceof ScriptScreen)) {
            ((IScreenInternal) instance).jsmacros_render(drawContext, mouseX, mouseY, delta);
        }
    }
    *///?} else if >1.21.8 {
    /*@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void onRender(Screen instance, GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        instance.renderWithTooltipAndSubtitles(drawContext, mouseX, mouseY, delta);
        if (!(minecraft.screen instanceof ScriptScreen)) {
            ((IScreenInternal) instance).jsmacros_render(drawContext, mouseX, mouseY, delta);
        }
    }
    *///?} else {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void onRender(Screen instance, GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        instance.renderWithTooltip(drawContext, mouseX, mouseY, delta);
        if (!(minecraft.screen instanceof ScriptScreen)) {
            ((IScreenInternal) instance).jsmacros_render(drawContext, mouseX, mouseY, delta);
        }
    }
    //?}

    //? if <26.1 {
    @Inject(at = @At("HEAD"), method = "pick(F)V", cancellable = true)
    public void onTargetUpdate(float tickDelta, CallbackInfo ci) {
        if (InteractionProxy.Target.onUpdate(tickDelta)) {
            ci.cancel();
        }
    }
    //?}
}
