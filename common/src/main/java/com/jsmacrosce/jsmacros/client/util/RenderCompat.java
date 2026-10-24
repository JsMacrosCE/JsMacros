package com.jsmacrosce.jsmacros.client.util;

import com.jsmacrosce.doclet.DocletIgnore;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

// Accessors renamed or moved to GameRenderer in 26.2.
@DocletIgnore
public final class RenderCompat {
    private RenderCompat() {
    }

    public static Camera mainCamera(Minecraft minecraft) {
        //? if >=26.2 {
        /*return minecraft.gameRenderer.mainCamera();
        *///?} else {
        return minecraft.gameRenderer.getMainCamera();
        //?}
    }

    public static RenderTarget mainRenderTarget(Minecraft minecraft) {
        //? if >=26.2 {
        /*return minecraft.gameRenderer.mainRenderTarget();
        *///?} else {
        return minecraft.getMainRenderTarget();
        //?}
    }
}
