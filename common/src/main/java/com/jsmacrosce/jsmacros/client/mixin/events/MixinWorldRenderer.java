package com.jsmacrosce.jsmacros.client.mixin.events;

import com.google.common.collect.ImmutableSet;

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
import org.joml.Matrix4fc;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jsmacrosce.jsmacros.client.api.classes.render.Draw3D;
import com.jsmacrosce.jsmacros.client.api.library.impl.FHud;

// 1.21.11 introduced the Gizmos API. 3D components emit gizmos, so we collect them here and
// render the standard/always-on-top groups ourselves. Injecting at addLateDebugPass HEAD puts the
// pass immediately before the vanilla late_debug pass, i.e. after weather and before the depth
// clear for vanilla always-on-top gizmos.
//? if >=1.21.11 {
/*import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
*///? }

//? if >=26.1 {
/*import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
*///? } else if >1.21.8 {
/*import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
*///?}

//? if >1.21.5 {
import com.mojang.blaze3d.buffers.GpuBufferSlice;
//?}

//? if <=1.21.5 {
/*import net.minecraft.client.renderer.FogParameters;
*///?}

@Mixin(LevelRenderer.class)
public class MixinWorldRenderer {

    @Final
    @Shadow
    private LevelTargetBundle targets;

    //? if >=26.1 {
    /*@Inject(method = "addLateDebugPass", at = @At("HEAD"))
    private void onAddLateDebugPass(
            FrameGraphBuilder frameGraphBuilder,
            CameraRenderState cameraState,
            GpuBufferSlice shaderFog,
            Matrix4fc modelViewMatrix,
            CallbackInfo ci
    ) {
        jsmacrosce_addGizmoPass(frameGraphBuilder, cameraState, modelViewMatrix);
    }
    *///? } else if >=1.21.11 {
    /*@Inject(method = "addLateDebugPass", at = @At("HEAD"))
    private void onAddLateDebugPass(
            FrameGraphBuilder frameGraphBuilder,
            CameraRenderState cameraState,
            GpuBufferSlice shaderFog,
            Matrix4f modelViewMatrix,
            CallbackInfo ci
    ) {
        jsmacrosce_addGizmoPass(frameGraphBuilder, cameraState, modelViewMatrix);
    }
    *///? } else if >1.21.8 {
    /*@Inject(method = "addMainPass", at = @At("TAIL"))
    private void onRenderMain(
            FrameGraphBuilder frameGraphBuilder,
            Frustum frustum,
            Matrix4f frustumMatrix,
            GpuBufferSlice shaderFog,
            boolean renderBlockOutline,
            LevelRenderState levelRenderState,
            DeltaTracker deltaTracker,
            ProfilerFiller profiler,
            CallbackInfo ci
    ) {
        jsmacrosce_renderDirect3D(frameGraphBuilder, deltaTracker, profiler);
    }
    *///?} else if >1.21.5 {
    @Inject(method = "addMainPass", at = @At("TAIL"))
    private void onRenderMain(
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
    ) {
        jsmacrosce_renderDirect3D(frameGraphBuilder, deltaTracker, profiler);
    }
    //?} else {
    /*@Inject(method = "addMainPass", at = @At("TAIL"))
    private void onRenderMain(
            FrameGraphBuilder frameGraphBuilder,
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
    ) {
        jsmacrosce_renderDirect3D(frameGraphBuilder, deltaTracker, profiler);
    }
    *///?}

    //? if >=1.21.11 {
    /*@Unique
    private void jsmacrosce_addGizmoPass(FrameGraphBuilder frameGraphBuilder, CameraRenderState cameraState, Matrix4fc modelViewMatrix) {
        if (this.targets == null) {
            return;
        }
        FramePass framePass = frameGraphBuilder.addPass("jsmacrosce_draw3d");
        LevelTargetBundle frameBufferSet = this.targets;
        frameBufferSet.main = framePass.readsAndWrites(frameBufferSet.main);

        framePass.executes(() -> {
            ProfilerFiller profiler = Profiler.get();
            profiler.push("jsmacrosce_d3d");

            try {
                MultiBufferSource.BufferSource consumers = Minecraft.getInstance().renderBuffers().bufferSource();
                float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
                PoseStack matrixStack = new PoseStack();

                DrawableGizmoPrimitives standardGizmos = new DrawableGizmoPrimitives();
                DrawableGizmoPrimitives alwaysOnTopGizmos = new DrawableGizmoPrimitives();
                SimpleGizmoCollector frameGizmos = new SimpleGizmoCollector();

                try (Gizmos.TemporaryCollection ignored = Gizmos.withCollector(frameGizmos)) {
                    for (Draw3D d : ImmutableSet.copyOf(FHud.renders)) {
                        d.render(matrixStack, consumers, tickDelta);
                    }
                }

                consumers.endBatch();

                long now = Util.getMillis();
                for (SimpleGizmoCollector.GizmoInstance instance : frameGizmos.drainGizmos()) {
                    DrawableGizmoPrimitives target = instance.isAlwaysOnTop() ? alwaysOnTopGizmos : standardGizmos;
                    instance.gizmo().emit(target, instance.getAlphaMultiplier(now));
                }

                // 1.21.11's DrawableGizmoPrimitives.render takes Matrix4f, 26.1's takes Matrix4fc.
                Matrix4f viewMatrix = new Matrix4f(modelViewMatrix);

                RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
                RenderSystem.outputColorTextureOverride = mainTarget.getColorTextureView();
                RenderSystem.outputDepthTextureOverride = mainTarget.getDepthTextureView();
                try {
                    if (!standardGizmos.isEmpty()) {
                        standardGizmos.render(matrixStack, consumers, cameraState, viewMatrix);
                        consumers.endLastBatch();
                    }

                    if (!alwaysOnTopGizmos.isEmpty()) {
                        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mainTarget.getDepthTexture(), 1.0);
                        alwaysOnTopGizmos.render(matrixStack, consumers, cameraState, viewMatrix);
                        consumers.endLastBatch();
                    }
                } finally {
                    RenderSystem.outputColorTextureOverride = null;
                    RenderSystem.outputDepthTextureOverride = null;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

            profiler.pop();
        });
    }
    *///? }

    //? if <1.21.11 {
    @Unique
    private void jsmacrosce_renderDirect3D(FrameGraphBuilder frameGraphBuilder, DeltaTracker deltaTracker, ProfilerFiller profiler) {
        if (this.targets == null) {
            return;
        }
        FramePass framePass = frameGraphBuilder.addPass("jsmacrosce_draw3d");
        LevelTargetBundle frameBufferSet = this.targets;
        frameBufferSet.main = framePass.readsAndWrites(frameBufferSet.main);

        framePass.executes(() -> {
            profiler.push("jsmacrosce_d3d");

            try {
                MultiBufferSource.BufferSource consumers = Minecraft.getInstance().renderBuffers().bufferSource();
                float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
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
    //? }
}
