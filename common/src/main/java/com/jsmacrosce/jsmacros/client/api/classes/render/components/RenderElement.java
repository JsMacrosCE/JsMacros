package com.jsmacrosce.jsmacros.client.api.classes.render.components;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import com.jsmacrosce.doclet.DocletIgnore;

/**
 * @author Wagyourtail
 */
public interface RenderElement extends Renderable {

    Minecraft mc = Minecraft.getInstance();

    int getZIndex();

    //? if >=26.1 {
    /*void render(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta);

    @DocletIgnore
    @Override
    default void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
        render(drawContext, mouseX, mouseY, delta);
    }
    *///?} else {
    @Override
    void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta);
    //?}

    @DocletIgnore
    //? if >=26.1 {
    /*default void render3D(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
    *///?} else {
    default void render3D(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
    //?}
        render(drawContext, mouseX, mouseY, delta);
    }

    /**
     * Renders this element in world space into a surface's buffer source. Only
     * implemented for 1.21.11+; the default is a no-op.
     */
    @DocletIgnore
    default void render3D(PoseStack matrixStack, MultiBufferSource consumers, int light, boolean seeThrough, float delta) {
    }

    /**
     * Converts a packed lightmap value to a brightness multiplier in [0, 1], using
     * the brighter of block and sky light.
     */
    @DocletIgnore
    static float lightBrightness(int packedLight) {
        int block = (packedLight >> 4) & 0xF;
        int sky = (packedLight >> 20) & 0xF;
        return Math.max(block, sky) / 15.0F;
    }

    /**
     * Multiplies an ARGB color's RGB by the packed light's brightness, keeping alpha.
     */
    @DocletIgnore
    static int applyLight(int argb, int packedLight) {
        float brightness = lightBrightness(packedLight);
        int alpha = argb & 0xFF000000;
        int red = (int) (((argb >> 16) & 0xFF) * brightness);
        int green = (int) (((argb >> 8) & 0xFF) * brightness);
        int blue = (int) ((argb & 0xFF) * brightness);
        return alpha | (red << 16) | (green << 8) | blue;
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
