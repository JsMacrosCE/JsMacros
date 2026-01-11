package com.jsmacrosce.jsmacros.client.api.classes.render.components;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import com.jsmacrosce.doclet.DocletIgnore;

/**
 * @author Wagyourtail
 */
public interface RenderElement extends Renderable {

    Minecraft mc = Minecraft.getInstance();

    int getZIndex();

    @DocletIgnore
    default void render3D(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        render(drawContext, mouseX, mouseY, delta);
    }

    @DocletIgnore
    default void setupMatrix(
            //? if >1.21.5 {
            Matrix3x2fStack matrices,
            //?} else {
            /*PoseStack matrices,
            *///?}
            double x,
            double y,
            float scale,
            float rotation,
            double width,
            double height,
            boolean rotateAroundCenter) {
        matrices.translate(
                (float) x,
                (float) y
            //? if <=1.21.5 {
                /*, 1
            *///?}
        );
        matrices.scale(
                scale,
                scale
                //? if <=1.21.5 {
                /*, 1
                *///?}
        );
        if (rotateAroundCenter) {
            matrices.translate(
                    (float) (width / 2),
                    (float) (height / 2)
                    //? if <=1.21.5 {
                    /*, 1
                    *///?}
            );
        }
        //? if <=1.21.5 {
        /*matrices.mulPose(new Quaternionf().rotateLocalZ((float) Math.toRadians(rotation)));
        *///?} else {
        matrices.rotate((float) Math.toRadians(rotation));
        //?}

        if (rotateAroundCenter) {
            matrices.translate(
                    (float) (-width / 2),
                    (float) (-height / 2)
                    //? if <=1.21.5 {
                    /*, 1
                    *///?}
            );
        }
        matrices.translate(
                (float) -x,
                (float) -y
                //? if <=1.21.5 {
                /*, 1
                *///?}
        );
    }

}
