package com.jsmacrosce.jsmacros.forge.client.forgeevents;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.Profiler;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import com.jsmacrosce.jsmacros.client.access.IScreenInternal;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw2D;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw3D;
import com.jsmacrosce.jsmacros.client.api.classes.render.IDraw2D;
import com.jsmacrosce.jsmacros.client.api.classes.render.ScriptScreen;
import com.jsmacrosce.jsmacros.client.api.library.impl.FHud;
import com.jsmacrosce.jsmacros.client.tick.TickBasedEvents;
import com.jsmacrosce.jsmacros.forge.client.api.classes.CommandBuilderForge;

import java.util.Comparator;
import java.util.stream.Collectors;

public class ForgeEvents {
    private static final Minecraft client = Minecraft.getInstance();

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ForgeEvents::renderWorldListener);
        NeoForge.EVENT_BUS.addListener(ForgeEvents::onTick);
        NeoForge.EVENT_BUS.addListener(ForgeEvents::onRegisterCommands);

        NeoForge.EVENT_BUS.addListener(ForgeEvents::onScreenDraw);

        NeoForge.EVENT_BUS.addListener(ForgeEvents::onScreenKeyPressed);
        NeoForge.EVENT_BUS.addListener(ForgeEvents::onScreenCharTyped);

        NeoForge.EVENT_BUS.addListener(ForgeEvents::onScreenMouseClicked);
        NeoForge.EVENT_BUS.addListener(ForgeEvents::onScreenMouseReleased);
        NeoForge.EVENT_BUS.addListener(ForgeEvents::onScreenMouseScroll);
        NeoForge.EVENT_BUS.addListener(ForgeEvents::onScreenMouseDragged);
    }

    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        ((IScreenInternal) event.getScreen()).jsmacros_keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers());
    }

    public static void onScreenCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        // getCodePoint returns int in all versions - cast to char
        char codepoint = (char) event.getCodePoint();

        ((IScreenInternal) event.getScreen()).jsmacros_charTyped(codepoint, event.getModifiers());
    }

    public static void onScreenDraw(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof ScriptScreen)) {
            ((IScreenInternal) event.getScreen()).jsmacros_render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        }
    }

    public static void onScreenMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        ((IScreenInternal) event.getScreen()).jsmacros_mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton());
    }

    public static void onScreenMouseReleased(ScreenEvent.MouseButtonPressed.Pre event) {
        ((IScreenInternal) event.getScreen()).jsmacros_mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton());
    }

    public static void onScreenMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        ((IScreenInternal) event.getScreen()).jsmacros_mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(), event.getScrollDeltaY());
    }

    public static void onScreenMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        ((IScreenInternal) event.getScreen()).jsmacros_mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY());
    }

    public static void renderHudListener(GuiGraphics GuiGraphics, DeltaTracker partialTicks) {
        for (IDraw2D<Draw2D> h : ImmutableSet.copyOf(FHud.overlays).stream().sorted(Comparator.comparingInt(IDraw2D::getZIndex)).collect(Collectors.toList())) {
            try {
                h.render(GuiGraphics);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void onRegisterGuiOverlays(RegisterGuiLayersEvent ev) {
        // TODO: This used to be DEBUG_OVERLAY in 1.21.8, removed in 1.21.9 or 1.21.10.
        //  How did this get handled on the fabric side?
        //? if >1.21.8 {
        /*ResourceLocation layer = VanillaGuiLayers.AFTER_CAMERA_DECORATIONS;
        *///?} else {
        ResourceLocation layer = VanillaGuiLayers.DEBUG_OVERLAY;
        //?}

        ev.registerBelow(layer, ResourceLocation.parse("jsmacrosce:hud"), ForgeEvents::renderHudListener);
    }

    //? if >1.21.5 {
    public static void renderWorldListener(RenderLevelStageEvent.AfterLevel e) {
    //?} else {
    /*public static void renderWorldListener(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
    *///?}
        var profiler = Profiler.get();
        profiler.push("jsmacrosce_draw3d");
        try {
            MultiBufferSource.BufferSource consumers = Minecraft.getInstance().renderBuffers().bufferSource();
            //? if >1.21.8 {
            /*DeltaTracker deltaTracker = Minecraft.getInstance().getDeltaTracker();
            *///?} else {
            DeltaTracker deltaTracker = e.getPartialTick();
            //?}
            float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
            PoseStack poseStack = new PoseStack();

            for (Draw3D d : ImmutableSet.copyOf(FHud.renders)) {
                d.render(poseStack, consumers, tickDelta);
            }

            consumers.endBatch();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        profiler.pop();
    }

    public static void onTick(ClientTickEvent.Post event) {
        TickBasedEvents.onTick(Minecraft.getInstance());
    }

    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        CommandBuilderForge.onRegisterEvent(event);
    }

}
