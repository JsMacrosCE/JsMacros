package com.jsmacrosce.jsmacros.client.util;

import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

//? if >=26.2 {
/*import net.minecraft.client.gui.Hud;
*///?} else {
import net.minecraft.client.gui.Gui;
//?}

// 26.2 moved screen/overlay ownership to Gui and split the HUD into Gui.hud.
@DocletIgnore
public final class GuiCompat {
    private GuiCompat() {
    }

    public static @Nullable Screen screen(Minecraft minecraft) {
        // The GUI is not yet assigned while the client profile is being initialized.
        //? if >=26.2 {
        /*return minecraft.gui == null ? null : minecraft.gui.screen();
        *///?} else {
        return minecraft.screen;
        //?}
    }

    public static void setScreen(Minecraft minecraft, @Nullable Screen screen) {
        //? if >=26.2 {
        /*minecraft.gui.setScreen(screen);
        *///?} else {
        minecraft.setScreen(screen);
        //?}
    }

    public static @Nullable Overlay overlay(Minecraft minecraft) {
        //? if >=26.2 {
        /*return minecraft.gui == null ? null : minecraft.gui.overlay();
        *///?} else {
        return minecraft.getOverlay();
        //?}
    }

    // Callers must wait for Minecraft.gui to be initialized before accessing the HUD.
    //? if >=26.2 {
    /*public static Hud hud(Minecraft minecraft) {
        return minecraft.gui.hud;
    }
    *///?} else {
    public static Gui hud(Minecraft minecraft) {
        return minecraft.gui;
    }
    //?}

    public static @Nullable ToastManager toastManager(Minecraft minecraft) {
        //? if >=26.2 {
        /*return minecraft.gui == null ? null : minecraft.gui.toastManager();
        *///?} else {
        return minecraft.getToastManager();
        //?}
    }
}
