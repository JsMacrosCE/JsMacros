package com.jsmacrosce.jsmacros.client.util;

import com.jsmacrosce.doclet.DocletIgnore;
import com.mojang.blaze3d.platform.Window;

//? if >=26.3 {
/*import org.lwjgl.sdl.SDLVideo;
*///?} else {
import org.lwjgl.glfw.GLFW;
//?}

@DocletIgnore
public final class WindowCompat {
    private WindowCompat() {
    }

    public static void setSize(Window window, int width, int height) {
        //? if >=26.3 {
        /*SDLVideo.SDL_SetWindowSize(window.handle(), width, height);
        *///?} else if >1.21.8 {
        /*GLFW.glfwSetWindowSize(window.handle(), width, height);
        *///?} else {
        GLFW.glfwSetWindowSize(window.getWindow(), width, height);
        //?}
    }
}
