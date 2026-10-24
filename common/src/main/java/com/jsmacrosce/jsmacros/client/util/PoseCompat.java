package com.jsmacrosce.jsmacros.client.util;

import com.jsmacrosce.doclet.DocletIgnore;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

@DocletIgnore
public final class PoseCompat {
    private PoseCompat() {
    }

    public static void rotate(PoseStack poses, Quaternionf rotation) {
        //? if >=26.3 {
        /*poses.rotate(rotation);
        *///?} else {
        poses.mulPose(rotation);
        //?}
    }
}
