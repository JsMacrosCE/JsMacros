package com.jsmacrosce.jsmacros.client.mixin.events;

import com.google.common.collect.ImmutableSet;
//? if >1.21.5 {
import com.mojang.blaze3d.buffers.GpuBufferSlice;
//?}
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
//? if <=1.21.5 {
/*import net.minecraft.client.renderer.FogParameters;
*///?}
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.MultiBufferSource;
        //? if >=1.21.11 {
/*import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import org.joml.Matrix4f;
import org.joml.Vector4f;
*///?}
        //? if <=1.21.10 {
import net.minecraft.world.phys.Vec3;
        //?}
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw3D;
import com.jsmacrosce.jsmacros.client.api.library.impl.FHud;

@Mixin(LevelRenderer.class)
public class MixinWorldRenderer {

    @Shadow
    private LevelTargetBundle targets;

    //? if >=1.21.11 {
    /*@Shadow
    private LevelRenderState levelRenderState;

    @Unique
    private Matrix4f jsmacrosce_viewMatrix = null;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void onRenderLevelHead(
            GraphicsResourceAllocator graphicsResourceAllocator,
            DeltaTracker deltaTracker,
            boolean flag,
            Camera camera,
            Matrix4f viewMatrix,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            GpuBufferSlice shaderFog,
            Vector4f clearColor,
            boolean renderSky,
            CallbackInfo ci
    ) {
        this.jsmacrosce_viewMatrix = viewMatrix;
    }

    @Inject(method = "addWeatherPass", at = @At("TAIL"))
    private void onAddWeatherPass(
            FrameGraphBuilder frameGraphBuilder,
            GpuBufferSlice shaderFog,
            CallbackInfo ci
    ) {
        jsmacrosce_addRenderPass(frameGraphBuilder, Minecraft.getInstance().getDeltaTracker());
    }

    @Unique
    private void jsmacrosce_addRenderPass(FrameGraphBuilder frameGraphBuilder, DeltaTracker deltaTracker) {
        if (this.targets == null) {
            return;
        }

        LevelRenderState capturedLevelState = this.levelRenderState;
        Matrix4f capturedViewMatrix = this.jsmacrosce_viewMatrix;

        FramePass framePass = frameGraphBuilder.addPass("jsmacrosce_draw3d");
        LevelTargetBundle frameBufferSet = this.targets;
        frameBufferSet.main = framePass.readsAndWrites(frameBufferSet.main);

        framePass.executes(() -> {
            ProfilerFiller profiler = net.minecraft.util.profiling.Profiler.get();
            profiler.push("jsmacrosce_d3d");

            try {
                MultiBufferSource.BufferSource consumers = Minecraft.getInstance().renderBuffers().bufferSource();
                float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
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

                long now = net.minecraft.util.Util.getMillis();
                for (SimpleGizmoCollector.GizmoInstance instance : frameGizmos.drainGizmos()) {
                    DrawableGizmoPrimitives target = instance.isAlwaysOnTop() ? alwaysOnTopGizmos : standardGizmos;
                    instance.gizmo().emit(target, instance.getAlphaMultiplier(now));
                }

                Matrix4f viewMatrix = capturedViewMatrix != null ? capturedViewMatrix : new Matrix4f();

                if (!standardGizmos.isEmpty()) {
                    standardGizmos.render(matrixStack, consumers, capturedLevelState.cameraRenderState, viewMatrix);
                    consumers.endLastBatch();
                }

                if (!alwaysOnTopGizmos.isEmpty()) {
                    com.mojang.blaze3d.pipeline.RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
                    RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mainTarget.getDepthTexture(), 1.0);
                    alwaysOnTopGizmos.render(matrixStack, consumers, capturedLevelState.cameraRenderState, viewMatrix);
                    consumers.endLastBatch();
                }

            } catch (Throwable e) {
                e.printStackTrace();
            }

            profiler.pop();
        });
    }
    *///?} else if >1.21.8 {
    /*@Inject(method = "addWeatherPass", at = @At("TAIL"))
    private void onAddWeatherPass(
            FrameGraphBuilder frameGraphBuilder,
            Vec3 cameraPos,
            GpuBufferSlice shaderFog,
            CallbackInfo ci
    ) {
        jsmacrosce_addRenderPass(frameGraphBuilder, Minecraft.getInstance().getDeltaTracker());
    }

    @Unique
    private void jsmacrosce_addRenderPass(FrameGraphBuilder frameGraphBuilder, DeltaTracker deltaTracker) {
        if (this.targets == null) {
            return;
        }

        FramePass framePass = frameGraphBuilder.addPass("jsmacrosce_draw3d");
        LevelTargetBundle frameBufferSet = this.targets;
        frameBufferSet.main = framePass.readsAndWrites(frameBufferSet.main);

        framePass.executes(() -> {
            ProfilerFiller profiler = net.minecraft.util.profiling.Profiler.get();
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
    *///?} else if >1.21.5 {
    @Inject(method = "addWeatherPass", at = @At("TAIL"))
    private void onAddWeatherPass(
            FrameGraphBuilder frameGraphBuilder, Vec3 cameraPos, float partialTick,
            GpuBufferSlice shaderFog, CallbackInfo ci)
    {
        jsmacrosce_addRenderPass(frameGraphBuilder, Minecraft.getInstance().getDeltaTracker());
    }

    @Unique
    private void jsmacrosce_addRenderPass(FrameGraphBuilder frameGraphBuilder, DeltaTracker deltaTracker) {
        if (this.targets == null) {
            return;
        }

        FramePass framePass = frameGraphBuilder.addPass("jsmacrosce_draw3d");
        LevelTargetBundle frameBufferSet = this.targets;
        frameBufferSet.main = framePass.readsAndWrites(frameBufferSet.main);

        framePass.executes(() -> {
            ProfilerFiller profiler = net.minecraft.util.profiling.Profiler.get();
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
    //?} else {
    /*@Inject(method = "addWeatherPass", at = @At("TAIL"))
    private void onAddWeatherPass(
            FrameGraphBuilder frameGraphBuilder,
            Vec3 cameraPos,
            float partialTick,
            FogParameters fogParameters,
            CallbackInfo ci
    ) {
        jsmacrosce_addRenderPass(frameGraphBuilder, Minecraft.getInstance().getDeltaTracker());
    }

    @Unique
    private void jsmacrosce_addRenderPass(FrameGraphBuilder frameGraphBuilder, DeltaTracker deltaTracker) {
        if (this.targets == null) {
            return;
        }

        FramePass framePass = frameGraphBuilder.addPass("jsmacrosce_draw3d");
        LevelTargetBundle frameBufferSet = this.targets;
        frameBufferSet.main = framePass.readsAndWrites(frameBufferSet.main);

        framePass.executes(() -> {
            ProfilerFiller profiler = net.minecraft.util.profiling.Profiler.get();
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
    *///?}
}
