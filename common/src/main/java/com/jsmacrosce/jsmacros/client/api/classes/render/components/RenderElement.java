package com.jsmacrosce.jsmacros.client.api.classes.render.components;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;

 /**
 * The one thing every element a script puts on a 2D overlay has in common.
 * <p>
 * A {@link Rect}, a {@link Text}, an {@link Item}, a {@link Line}, an {@link Image} and a
 * nested overlay element are all this, which is why a single list on the overlay holds
 * all of them and a single sort orders all of them.
 * <p>
 * There is very little here, because a script does not normally hold one of these
 * itself: the {@code add} methods on the overlay and the element builders hand one back
 * instead. What is here is the z-index the overlay sorts by and the call it makes on
 * each element every frame.
 * example:
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
    * the game client itself, which is what {@code Minecraft.getInstance()} returns.
    * <p>
    * The element classes in this package read the font and the window off this rather
    * than carrying one each, which is why a line of text knows its own height without
    * being handed a font. It is a constant: the client is a singleton, so this is the
    * same object for the whole run.
    * example:
    * <pre>
    * // the same object Minecraft.getInstance() hands back
    * const RenderElement = Java.type("com.jsmacrosce.jsmacros.client.api.classes.render.components.RenderElement");
    * Chat.log(`font line height is ${RenderElement.mc.font.lineHeight}`);
    * </pre>
    */
    Minecraft mc = Minecraft.getInstance();

    /**
    * the number this element is ordered against the others on its overlay.
    * <p>
    * The overlay sorts by this before it draws, lowest first, and because that sort is
    * stable two elements with the same number are drawn in the order they were added.
    * So a larger number is nearer the viewer, and two elements can be swapped over by
    * swapping their numbers.
    * <p>
    * This is per element. A whole 2D overlay is ordered against other overlays by
    * {@link com.jsmacrosce.jsmacros.client.api.classes.render.IDraw2D#getZIndex()}
    * instead, which is a different number on a different object.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const a = draw.addRect(0, 0, 100, 100, 0xFF000000);
    * const b = draw.addRect(0, 0, 100, 100, 0x80FFFFFF);
    * a.setZIndex(2);
    * b.setZIndex(1);
    * draw.register();
    * Chat.log(`a is ${a.getZIndex()} and b is ${b.getZIndex()}, so a is drawn last`);
    * </pre>
    *
    * @return the z-index of this element
    */
    int getZIndex();

    //? if >=26.1 {
    /*void render(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta);

    @DocletIgnore
    @Override
    default void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
        render(drawContext, mouseX, mouseY, delta);
    }
    *///?} else {
    /**
    * draws this element, and is called on it by the overlay rather than by a script.
    * <p>
    * Each element does its own matrix setup around the draw, so the position, scale and
    * rotation on the element are what put it where it lands. The overlay calls this once
    * per element per frame and the elements never ask when to draw themselves.
    * <p>
    * The mouse arguments are not the same thing everywhere. On a plain overlay they are
    * all zero, because that overlay has no mouse of its own. On a screen they are the
    * real mouse position and frame delta, and a {@link Text} element on a screen is the
    * one the screen makes hoverable and clickable when the cursor is inside it.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("drawn by the overlay", 0, 0, 0xFFFFFFFF, true);
    * // the overlay calls render on it; a script does not
    * draw.register();
    * Chat.log(`the text is ${label.getWidth()} wide and sits at ${label.getX()}, ${label.getY()}`);
    * </pre>
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
    * the same draw, but on the path a surface and an item icon take.
    * <p>
    * Two of the elements in this package override it and the other four fall through to
    * {@code render} unchanged. {@link Text} overrides it to put its glyphs through
    * {@code drawInBatch} into an immediate buffer, and {@link Item} overrides it for
    * the item icon.
    * <p>
    * The override on {@link Item} is there to collapse the z offset an item model
    * carries, so the icon lies in the surface plane rather than standing off it. On
    * 1.21.8 it no longer does that: both of the branches the {@code is3dRender} flag
    * selects between end in the same call, and the matrix stack of 1.21.5 and later is
    * 2D and has no z axis to collapse. The flattening is still real on the other
    * variant of this method, the {@link PoseStack} one, which is what a surface's
    * world-space buffer source goes through.
    */
    @DocletIgnore
    default void render3D(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        render(drawContext, mouseX, mouseY, delta);
    }

    /**
    * Renders this element in world space into a surface's buffer source. Only
    * implemented for 1.21.11+; the default is a no-op.
    * <p>
    * The wording above is the source comment and is kept as it was written. Checked
    * against this file: the default body here is empty, but {@link Rect},
    * {@link Line}, {@link Image}, {@link Text} and {@link Item} each override it, and
    * none of those five overrides is inside a version conditional, so they are compiled
    * on 1.21.8 as well. All five of them draw something, so within this package it is
    * {@link Draw2DElement} that is left on the default: a nested overlay is a 2D thing
    * and has nothing to put into a world-space buffer. An implementation of
    * {@code RenderElement} from outside this package that does not override it draws
    * nothing through it either.
    */
    @DocletIgnore
    default void render3D(PoseStack matrixStack, MultiBufferSource consumers, int light, boolean seeThrough, float delta) {
    }

    /**
    * Converts a packed lightmap value to a brightness multiplier in [0, 1], using
    * the brighter of block and sky light.
    * <p>
    * Both halves of the packed value are four bits wide, and the brighter of the two
    * divided by 15 is what comes back, so a fully lit value gives 1.0 and an unlit one
    * gives 0.0.
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
    * Multiplies an ARGB color's RGB by the packed light's brightness, keeping alpha.
    * <p>
    * Only the three colour bytes are touched and the alpha byte is copied through
    * unchanged, so this darkens a colour rather than fading it.
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
    * puts the element's own transform on the matrix, draws with it, and puts the
    * matrix back exactly as it found it.
    * <p>
    * The order is translate to the position, scale, move to the middle of the element
    * if it rotates about its middle, rotate by the given angle in degrees, move back
    * from the middle if it was moved to it, and translate back by the negative
    * position. The last step is what makes it scoped: the caller keeps whatever
    * transform it already had.
    * <p>
    * An element that does not rotate about its middle rotates about the corner it was
    * translated to, which is its top left.
    *
    * @param matrices           the matrix stack to transform and leave as it was
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
