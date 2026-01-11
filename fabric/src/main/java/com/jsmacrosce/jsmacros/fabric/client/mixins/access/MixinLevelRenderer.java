package com.jsmacrosce.jsmacros.fabric.client.mixins.access;

import com.google.common.collect.ImmutableSet;
//? if >1.21.5 {
import com.mojang.blaze3d.buffers.GpuBufferSlice;
//?}
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
//? if >1.21.8 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.LevelRenderState;
*///?}
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw3D;
import com.jsmacrosce.jsmacros.client.api.library.impl.FHud;

@Mixin(value = LevelRenderer.class)
public class MixinLevelRenderer {

    @Shadow
    @Final
    private RenderBuffers renderBuffers;
    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Inject(method = "addMainPass", at = @At("TAIL"))
    private void onRenderMain(
            //? if >1.21.8 {
            /*FrameGraphBuilder frameGraphBuilder,
            Frustum frustum,
            Matrix4f frustumMatrix,
            GpuBufferSlice shaderFog,
            boolean renderBlockOutline,
            LevelRenderState levelRenderState,
            DeltaTracker deltaTracker,
            ProfilerFiller profiler,
            CallbackInfo ci
            *///?} else if >1.21.5 {
            FrameGraphBuilder frameGraphBuilder,
            Frustum frustum,
            Camera camera,
            Matrix4f frustumMatrix,
            GpuBufferSlice shaderFog,
            boolean renderBlockOutline,
            boolean renderEntityOutline,
            DeltaTracker deltaTracker,
            ProfilerFiller profiler,
            CallbackInfo ci
            //?} else {
            /*FrameGraphBuilder frameGraphBuilder,
            Frustum frustum,
            Camera camera,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            FogParameters fogParameters,
            boolean renderBlockOutline,
            boolean renderEntityOutline,
            DeltaTracker deltaTracker,
            ProfilerFiller profiler,
            CallbackInfo ci
            *///?}
    ) {
        if (this.targets == null) {
            return;
        }
        FramePass framePass = frameGraphBuilder.addPass("jsmacrosce_draw3d");
        LevelTargetBundle frameBufferSet = this.targets;
        frameBufferSet.main = framePass.readsAndWrites(frameBufferSet.main);

        framePass.executes(() -> {
            profiler.push("jsmacrosce_d3d");

            try {
                MultiBufferSource.BufferSource consumers = renderBuffers.crumblingBufferSource();

                float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);

                PoseStack matrixStack = new PoseStack();
                matrixStack.pushPose();
                for (Draw3D d : ImmutableSet.copyOf(FHud.renders)) {
                    d.render(matrixStack, consumers, tickDelta);
                }
                matrixStack.popPose();
                consumers.endBatch();
            } catch (Throwable e) {
                e.printStackTrace();
            }

            profiler.pop();
        });
    }

}
