package com.jsmacrosce.jsmacros.client.mixin.access;

//? if >=26.3 {
/*import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.client.access.IOverlayPreparedFrame;
import com.jsmacrosce.jsmacros.client.api.classes.render.OverlaySubmitNodeCollection;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
import java.util.Map;

@DocletIgnore
@Mixin(FeatureRenderDispatcher.PreparedFrame.class)
public abstract class MixinOverlayPreparedFrame implements IOverlayPreparedFrame {
    @Shadow private FeatureFrameContext context;
    @Shadow private SubmitNodeStorage submitNodeStorage;
    @Shadow @Final private Map<FeatureRenderPhase<?>, List<?>> groupsByPhase;

    @Shadow
    private void executePhase(FeatureRenderPhase<?> phase, FeatureFrameContext context, RenderPass pass) {
        throw new AssertionError();
    }

    @Override
    public boolean jsmacros_hasOverlays(boolean top) {
        for (var layer : submitNodeStorage.getSubmitsPerOrder().values()) {
            if (top && !groupsByPhase.getOrDefault(layer.alwaysOnTopGizmos, List.of()).isEmpty()) return true;
            if (layer instanceof OverlaySubmitNodeCollection overlay
                    && !groupsByPhase.getOrDefault(top ? overlay.overlayTop : overlay.overlayDepth, List.of()).isEmpty()) return true;
        }
        return false;
    }

    @Override
    public void jsmacros_renderOverlays(RenderPass pass, boolean top) {
        for (var layer : submitNodeStorage.getSubmitsPerOrder().values()) {
            if (layer instanceof OverlaySubmitNodeCollection overlay) {
                executePhase(top ? overlay.overlayTop : overlay.overlayDepth, context, pass);
            }
        }
    }

    @Inject(method = "executeAlwaysOnTop", at = @At("TAIL"))
    private void jsmacros_renderTop(RenderPass pass, CallbackInfo ci) {
        jsmacros_renderOverlays(pass, true);
    }
}
*///?}
