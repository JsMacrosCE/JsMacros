package com.jsmacrosce.jsmacros.client.mixin.events;

import com.jsmacrosce.jsmacros.client.util.RenderCompat;
//? if >=26.3 {
/*import com.jsmacrosce.jsmacros.client.access.IOverlayPreparedFrame;
import com.jsmacrosce.jsmacros.client.api.classes.render.OverlaySubmitNodeCollection;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;
import java.util.OptionalDouble;
*///?}
import com.jsmacrosce.doclet.DocletIgnore;

import com.google.common.collect.ImmutableSet;

import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
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
import com.jsmacrosce.jsmacros.client.api.classes.render.RenderSink;
import com.jsmacrosce.jsmacros.client.api.library.impl.FHud;

//? if <26.2 {
import net.minecraft.client.renderer.MultiBufferSource;
//?}

// 1.21.11 introduced the Gizmos API. 3D components emit gizmos, so we collect them here and
// render the standard/always-on-top groups ourselves. Injecting at addLateDebugPass HEAD puts the
// pass immediately before the vanilla late_debug pass, i.e. after weather and before the depth
// clear for vanilla always-on-top gizmos.
//? if >=26.2 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
*///? } else if >=1.21.11 {
/*import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import net.minecraft.util.profiling.Profiler;
*///? }

//? if >=26.2 {
/*import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
*///? }

import net.minecraft.Util;

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
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.phys.Vec3;
//?}

@Mixin(LevelRenderer.class)
@DocletIgnore
public class MixinWorldRenderer {

    // order-independent transparency keeps transparent fragments in multiple rendertargets and
    // composites them in a separate pass. Custom alpha overlays must run after classic and OIT
    // composition, not inside those MRT passes.
    // A prepared frame groups submitted features by phase; JsMacros adds dedicated depth and top
    // phases so its overlays run with the intended depth state.
    //? if >=26.3 {
    /*@Unique private boolean jsmacros_hasTopOverlays;

    @Inject(method = "frameHasAlwaysOnTopGizmos", at = @At("RETURN"), cancellable = true)
    private void jsmacros_requireTopPass(CallbackInfoReturnable<Boolean> cir) {
        if (jsmacros_hasTopOverlays) cir.setReturnValue(true);
    }

    @Inject(method = "executeOutline", at = @At("HEAD"))
    private void jsmacros_renderDepthOverlays(FeatureRenderDispatcher.PreparedFrame features, CallbackInfo ci) {
        IOverlayPreparedFrame overlays = (IOverlayPreparedFrame) features;
        if (!overlays.jsmacros_hasOverlays(false)) return;
        var target = targets.main.get();
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "JsMacros depth overlays", target.getColorTextureView(), Optional.empty(),
                target.getDepthTextureView(), OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            overlays.jsmacros_renderOverlays(pass, false);
        }
    }
    *///?}

    @Final
    @Shadow
    private LevelTargetBundle targets;

    //? if >=26.2 {
    /*@Final
    @Shadow
    private SubmitNodeStorage submitNodeStorage;
    *///?}

    // 26.2 replaced the render-buffer API with submitted render features. This is the last
    // point with a SubmitNodeCollector for this frame.
    //? if >=26.2 {
    /*@Inject(method = "submitFeatures", at = @At("TAIL"))
    private void jsmacrosce_submitFeatures(
            LevelRenderState levelRenderState,
            SubmitNodeCollector collector,
            boolean renderOutline,
            CallbackInfo ci
    ) {
        jsmacrosce_submitDraw3d(levelRenderState, this.submitNodeStorage);
        //? if >=26.3 {
        /^jsmacros_hasTopOverlays = OverlaySubmitNodeCollection.hasTopSubmits(this.submitNodeStorage);
        ^///?}
    }

    @Unique
    private void jsmacrosce_submitDraw3d(LevelRenderState levelRenderState, SubmitNodeStorage collector) {
        try {
            float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
            PoseStack poseStack = new PoseStack();
            ImmutableSet<Draw3D> draws = ImmutableSet.copyOf(FHud.renders);

            DrawableGizmoPrimitives standardGizmos = new DrawableGizmoPrimitives();
            DrawableGizmoPrimitives alwaysOnTopGizmos = new DrawableGizmoPrimitives();
            SimpleGizmoCollector frameGizmos = new SimpleGizmoCollector();

            RenderSink depthSink = new RenderSink(collector, false);
            try (Gizmos.TemporaryCollection ignored = Gizmos.withCollector(frameGizmos)) {
                for (Draw3D d : draws) {
                    d.render(poseStack, depthSink, tickDelta);
                }
            }

            long now = Util.getMillis();
            for (SimpleGizmoCollector.GizmoInstance instance : frameGizmos.drainGizmos()) {
                DrawableGizmoPrimitives target = instance.isAlwaysOnTop() ? alwaysOnTopGizmos : standardGizmos;
                instance.gizmo().emit(target, instance.getAlphaMultiplier(now));
            }

            CameraRenderState cameraState = levelRenderState.cameraRenderState;
            standardGizmos.submit(collector, cameraState, false);

            // Depth-tested surface elements (cull=true) draw against the terrain depth buffer.
            Draw3D.renderDirectSurfaces(draws, poseStack, depthSink, tickDelta, false);

            alwaysOnTopGizmos.submit(collector, cameraState, true);

            // Non-cull surfaces join the always-on-top phase, which clears depth before drawing.
            RenderSink topSink = new RenderSink(collector, true);
            Draw3D.renderDirectSurfaces(draws, poseStack, topSink, tickDelta, true);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    *///? } else if >=26.1 {
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

    //? if >=1.21.11 <26.2 {
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
                PoseStack poseStack = new PoseStack();
                RenderSink sink = new RenderSink(consumers);

                DrawableGizmoPrimitives standardGizmos = new DrawableGizmoPrimitives();
                DrawableGizmoPrimitives alwaysOnTopGizmos = new DrawableGizmoPrimitives();
                SimpleGizmoCollector frameGizmos = new SimpleGizmoCollector();

                try (Gizmos.TemporaryCollection ignored = Gizmos.withCollector(frameGizmos)) {
                    for (Draw3D d : ImmutableSet.copyOf(FHud.renders)) {
                        d.render(poseStack, sink, tickDelta);
                    }
                }

                sink.endBatch();

                long now = Util.getMillis();
                for (SimpleGizmoCollector.GizmoInstance instance : frameGizmos.drainGizmos()) {
                    DrawableGizmoPrimitives target = instance.isAlwaysOnTop() ? alwaysOnTopGizmos : standardGizmos;
                    instance.gizmo().emit(target, instance.getAlphaMultiplier(now));
                }

                // 1.21.11's DrawableGizmoPrimitives.render takes Matrix4f, 26.1's takes Matrix4fc.
                Matrix4f viewMatrix = new Matrix4f(modelViewMatrix);

                RenderTarget mainTarget = RenderCompat.mainRenderTarget(Minecraft.getInstance());
                RenderSystem.outputColorTextureOverride = mainTarget.getColorTextureView();
                RenderSystem.outputDepthTextureOverride = mainTarget.getDepthTextureView();
                try {
                    ImmutableSet<Draw3D> draws = ImmutableSet.copyOf(FHud.renders);
                    if (!standardGizmos.isEmpty()) {
                        standardGizmos.render(poseStack, consumers, cameraState, viewMatrix);
                        consumers.endLastBatch();
                    }

                    // Depth-tested surface elements (cull=true) draw while the world
                    // depth buffer is still intact.
                    Draw3D.renderDirectSurfaces(draws, poseStack, sink, tickDelta, false);
                    sink.endBatch();

                    if (!alwaysOnTopGizmos.isEmpty()) {
                        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mainTarget.getDepthTexture(), 1.0);
                        alwaysOnTopGizmos.render(poseStack, consumers, cameraState, viewMatrix);
                        consumers.endLastBatch();
                    }

                    Draw3D.renderDirectSurfaces(draws, poseStack, sink, tickDelta, true);
                    sink.endBatch();
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
                PoseStack poseStack = new PoseStack();
                RenderSink sink = new RenderSink(consumers);

                ImmutableSet<Draw3D> draws = ImmutableSet.copyOf(FHud.renders);
                for (Draw3D d : draws) {
                    d.renderDepthPass(poseStack, sink, tickDelta);
                }
                sink.endBatch();
                Draw3D.renderDirectSurfaces(draws, poseStack, sink, tickDelta, false);
                sink.endBatch();

                Draw3D.renderDirectSurfaces(draws, poseStack, sink, tickDelta, true);
                sink.endBatch();
            } catch (Throwable e) {
                e.printStackTrace();
            }

            profiler.pop();
        });
    }
    //? }
}
