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
import com.jsmacrosce.jsmacros.client.api.classes.render.components3d.Surface;
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

//? if <1.21.11 {
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.phys.Vec3;
//?}

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
    */    //? } else if >1.21.8 {
    /*// Inject after the cloud/weather passes so clouds do not draw over surfaces.
    @Inject(method = "addLateDebugPass", at = @At("HEAD"))
    private void onRenderMain(
            FrameGraphBuilder frameGraphBuilder,
            Vec3 cameraPos,
            GpuBufferSlice shaderFog,
            Frustum frustum,
            CallbackInfo ci
    ) {
        jsmacrosce_renderDirect3D(frameGraphBuilder);
    }
    *///?} else if >1.21.5 {
    @Inject(method = "addLateDebugPass", at = @At("HEAD"))
    private void onRenderMain(
            FrameGraphBuilder frameGraphBuilder,
            Vec3 cameraPos,
            GpuBufferSlice shaderFog,
            CallbackInfo ci
    ) {
        jsmacrosce_renderDirect3D(frameGraphBuilder);
    }
    //?} else {
    /*@Inject(method = "addLateDebugPass", at = @At("HEAD"))
    private void onRenderMain(
            FrameGraphBuilder frameGraphBuilder,
            Vec3 cameraPos,
            FogParameters fogParameters,
            CallbackInfo ci
    ) {
        jsmacrosce_renderDirect3D(frameGraphBuilder);
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

                    // Depth-tested surface elements (cull=true) draw while the world
                    // depth buffer is still intact.
                    for (Draw3D d : ImmutableSet.copyOf(FHud.renders)) {
                        d.renderDirect(matrixStack, consumers, tickDelta, false);
                    }
                    consumers.endBatch();

                    // Always-on-top elements need a cleared depth buffer, and the
                    // direct surfaces (cull=false) need it too, not just the gizmos.
                    boolean alwaysOnTopSurface = false;
                    for (Draw3D d : ImmutableSet.copyOf(FHud.renders)) {
                        for (Surface s : d.getDraw2Ds()) {
                            if (!s.cull) {
                                alwaysOnTopSurface = true;
                                break;
                            }
                        }
                        if (alwaysOnTopSurface) {
                            break;
                        }
                    }

                    if (!alwaysOnTopGizmos.isEmpty() || alwaysOnTopSurface) {
                        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mainTarget.getDepthTexture(), 1.0);
                        if (!alwaysOnTopGizmos.isEmpty()) {
                            alwaysOnTopGizmos.render(matrixStack, consumers, cameraState, viewMatrix);
                            consumers.endLastBatch();
                        }
                    }

                    // Always-on-top surface elements (cull=false) draw after the clear.
                    for (Draw3D d : ImmutableSet.copyOf(FHud.renders)) {
                        d.renderDirect(matrixStack, consumers, tickDelta, true);
                    }
                    consumers.endBatch();
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
    private void jsmacrosce_renderDirect3D(FrameGraphBuilder frameGraphBuilder) {
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

                for (Draw3D d : ImmutableSet.copyOf(FHud.renders)) {
                    d.renderDepthPass(matrixStack, consumers, tickDelta);
                }
                consumers.endBatch();

                // Always-on-top surfaces (cull=false) must draw over the world, so
                // clear depth before their group.
                boolean alwaysOnTopSurface = false;
                for (Draw3D d : ImmutableSet.copyOf(FHud.renders)) {
                    for (Surface s : d.getDraw2Ds()) {
                        if (!s.cull) {
                            alwaysOnTopSurface = true;
                            break;
                        }
                    }
                    if (alwaysOnTopSurface) {
                        break;
                    }
                }
                if (alwaysOnTopSurface) {
                    RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
                    RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mainTarget.getDepthTexture(), 1.0);
                    for (Draw3D d : ImmutableSet.copyOf(FHud.renders)) {
                        d.renderAlwaysOnTopSurfaces(matrixStack, consumers, tickDelta);
                    }
                    consumers.endBatch();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

            profiler.pop();
        });
    }
    //? }
}
