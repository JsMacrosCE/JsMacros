package xyz.wagyourtail.jsmacros.client.mixin.events;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.wagyourtail.jsmacros.client.api.classes.render.Draw3D;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FHud;

@Mixin(LevelRenderer.class)
public class MixinWorldRenderer {

    @Shadow
    private LevelTargetBundle targets;

    @Inject(method = "addMainPass", at = @At("TAIL"))
    private void onRenderMain(
            FrameGraphBuilder frameGraphBuilder,
            Frustum frustum,
            Camera camera,
            Matrix4f positionMatrix,
            GpuBufferSlice fog,
            boolean renderBlockOutline,
            boolean renderEntityOutline,
            DeltaTracker tickCounter,
            ProfilerFiller profiler,
            CallbackInfo ci
    ) {
        if (this.targets == null) {
            return;
        }
        FramePass framePass = frameGraphBuilder.addPass("jsmacros_draw3d");
        LevelTargetBundle frameBufferSet = this.targets;
        frameBufferSet.main = framePass.readsAndWrites(frameBufferSet.main);

        framePass.executes(() -> {
            profiler.push("jsmacros_d3d");

            try {
                MultiBufferSource.BufferSource consumers = Minecraft.getInstance().renderBuffers().bufferSource();

                float tickDelta = tickCounter.getGameTimeDeltaPartialTick(true);

                PoseStack matrixStack = new PoseStack();

                for (Draw3D d : ImmutableSet.copyOf(FHud.renders)) {

                    d.render(matrixStack, consumers, tickDelta);
                }

                consumers.endBatch();

            } catch (Throwable e) {
                e.printStackTrace();
            }

            profiler.pop();
        });
    }
}