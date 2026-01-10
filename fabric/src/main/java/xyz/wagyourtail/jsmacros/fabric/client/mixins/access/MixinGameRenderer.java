package xyz.wagyourtail.jsmacros.fabric.client.mixins.access;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.wagyourtail.jsmacros.client.access.IScreenInternal;
import xyz.wagyourtail.jsmacros.client.api.classes.InteractionProxy;
import xyz.wagyourtail.jsmacros.client.api.classes.render.ScriptScreen;

@Mixin(value = GameRenderer.class)
public class MixinGameRenderer {
    @Shadow
    @Final
    private Minecraft minecraft;

//? if >1.21.8 {
    /*@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
*///?} else {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
//?}
    private void onRender(Screen instance, GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        //? if >1.21.8 {
        /*instance.renderWithTooltipAndSubtitles(drawContext, mouseX, mouseY, delta);
        *///?} else {
        instance.renderWithTooltip(drawContext, mouseX, mouseY, delta);
        //?}
        if (!(minecraft.screen instanceof ScriptScreen)) {
            ((IScreenInternal) instance).jsmacros_render(drawContext, mouseX, mouseY, delta);
        }
    }

    @Inject(at = @At("HEAD"), method = "pick", cancellable = true)
    public void onTargetUpdate(float tickDelta, CallbackInfo ci) {
        if (InteractionProxy.Target.onUpdate(tickDelta)) {
            ci.cancel();
        }
    }
}
