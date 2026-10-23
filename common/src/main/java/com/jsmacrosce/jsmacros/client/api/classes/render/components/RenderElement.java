package com.jsmacrosce.jsmacros.client.api.classes.render.components;
import com.jsmacrosce.jsmacros.client.api.classes.render.RenderSink;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;

 /**
  * A renderable element that can be added to a 2D overlay. The overlay orders its
  * elements by z-index and renders them each frame. Lower z-indexes draw first; ties
  * retain insertion order.
 * <pre>
 * const draw = Hud.createDraw2D();
 * const back = draw.addRect(0, 0, 100, 100, 0xFF000000);
 * const front = draw.addRect(10, 10, 50, 50, 0xFFFF0000);
 * // sorted by z-index lowest first, and a tie keeps the order the two were added in
 * front.setZIndex(back.getZIndex() + 1);
 * draw.register();
 * </pre>
 *
 * @author Wagyourtail
 */
@DocletCategory("Rendering/Graphics")
public interface RenderElement extends Renderable {

    /**
    * The Minecraft client instance used by render elements to access client resources
    * such as the font and window.
    */
    Minecraft mc = Minecraft.getInstance();

    /**
    * Returns this element's ordering value on its overlay. Lower values draw first;
    * equal values retain insertion order. This is distinct from the value used to order
    * whole overlays by
    * {@link com.jsmacrosce.jsmacros.client.api.classes.render.IDraw2D#getZIndex()}
    * instead, which is a different number on a different object.
    * @return the z-index of this element
    */
    int getZIndex();

    //? if >=26.1 {
    /*/^* Collects this element's GUI render state when called by its overlay or screen.
     * A plain overlay passes zero for the mouse coordinates and frame value; a screen forwards its current values.
     * @param drawContext the graphics context used to collect render state
     * @param mouseX the mouse x-coordinate, or 0 on a plain overlay
     * @param mouseY the mouse y-coordinate, or 0 on a plain overlay
     * @param delta the frame value supplied by the caller, or 0 on a plain overlay
     ^/
    *///?}
    //? if >=26.1 {
    /*void render(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta);

    @DocletIgnore
    @Override
    default void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
        render(drawContext, mouseX, mouseY, delta);
    }
    *///?} else {
    /**
    * Draws this element when called by its overlay or screen. A plain overlay passes
    * zero for the mouse coordinates and frame delta; a screen passes its current values.
    *
    * @param drawContext the graphics context to draw into
    * @param mouseX      the mouse x, or 0 on a plain overlay
    * @param mouseY      the mouse y, or 0 on a plain overlay
    * @param delta       the frame delta, or 0 on a plain overlay
    */
    @Override
    void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta);
    //?}

    /**
    * Draws this element through the GUI graphics path used by surfaces. The default
    * delegates to {@link #render(GuiGraphics, int, int, float)}.
    */
    @DocletIgnore
    default void render3D(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        render(drawContext, mouseX, mouseY, delta);
    }

    /**
    * Renders this element in world space into a surface's buffer source. The default is
    * a no-op; elements that support this path override it.
    */
    @DocletIgnore
    default void render3D(PoseStack matrixStack, RenderSink sink, int light, boolean seeThrough, float delta) {
    }

    /**
    * Converts the block and sky components of a packed lightmap value to a brightness
    * multiplier by taking the greater four-bit component and dividing it by 15.
    *
    * @param packedLight the packed lightmap value
    * @return the brightness, from 0.0 to 1.0.
    */
    @DocletIgnore
    static float lightBrightness(int packedLight) {
        int block = (packedLight >> 4) & 0xF;
        int sky = (packedLight >> 20) & 0xF;
        return Math.max(block, sky) / 15.0F;
    }

    /**
    * Scales an ARGB color's RGB channels by the packed light brightness and preserves
    * its alpha channel.
    *
    * @param argb        the colour to darken
    * @param packedLight the packed lightmap value
    * @return the colour with its RGB scaled by the brightness.
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

    /**
    * Applies the element transform to the matrix: position, scale, then rotation in
    * degrees about the center when requested or otherwise about the positioned corner.
    * The final inverse translation cancels the position translation; scale and
    * rotation remain applied to the matrix.
    *
    * @param matrices           the matrix stack to transform
    * @param x                  the x position of the element
    * @param y                  the y position of the element
    * @param scale              the scale to apply
    * @param rotation           the rotation in degrees
    * @param width              the width of the element, used for the middle
    * @param height             the height of the element, used for the middle
    * @param rotateAroundCenter whether the rotation is about the middle rather than
    * about the top left corner
    */
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
