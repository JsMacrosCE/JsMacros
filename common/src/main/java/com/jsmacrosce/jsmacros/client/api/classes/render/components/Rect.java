package com.jsmacrosce.jsmacros.client.api.classes.render.components;

import com.jsmacrosce.jsmacros.client.util.PoseCompat;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.render.RenderSink;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.client.api.classes.render.IDraw2D;
import com.jsmacrosce.jsmacros.util.ColorMath;

import com.jsmacrosce.jsmacros.client.api.classes.render.components3d.SurfaceRenderTypes;

 /**
 * A filled rectangle defined by two corner positions. Corners may be given in either
 * order; width and height are their absolute coordinate differences. Colors use
 * packed ARGB, and a zero-alpha color with nonzero RGB is treated as opaque.
 * <pre>
 * const draw = Hud.createDraw2D();
 * // Corners may be specified in either order.
 * const rect = draw.addRect(110, 60, 10, 10, 0x80000000);
 * draw.register();
 * Chat.log(`${rect.getWidth()} by ${rect.getHeight()}, from ${rect.getX1()}, ${rect.getY1()}`);
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.0.5
 */
@SuppressWarnings("unused")
@DocletCategory("Rendering/Graphics")
public class Rect implements RenderElement, Alignable<Rect> {

    /**
    * The overlay this rectangle belongs to, or {@code null} to use the window dimensions
    * for parent-relative alignment.
    */
    @Nullable
    public IDraw2D<?> parent;
    /**
    * Rotation in degrees, normalized by the constructor and {@link #setRotation(double)}.
    */
    public float rotation;
    /**
    * Whether rotation is about the rectangle's center rather than its first corner.
    * Directly constructed rectangles default to {@code false}; builders default to {@code true}.
    */
    public boolean rotateCenter;
    /**
    * The x coordinate of the first corner; it is not necessarily the left edge.
    */
    public int x1;
    /**
    * The y coordinate of the first corner; it is not necessarily the top edge.
    */
    public int y1;
    /**
    * The x coordinate of the second corner.
    */
    public int x2;
    /**
    * The y coordinate of the second corner.
    */
    public int y2;
    /**
    * The packed ARGB color. A zero-alpha value with nonzero RGB is made opaque by the
    * one-argument color setter; pure black remains transparent.
    */
    public int color;
    /**
    * Draw-order value relative to other elements on the overlay; lower values draw first.
    */
    public int zIndex;

    /**
    * Constructs a rectangle from two corners, applying {@link #setColor(int)}'s alpha
    * fix-up and normalizing the rotation with {@link Mth#wrapDegrees(float)}.
    *
    * @param x1       the first x position of this rectangle
    * @param y1       the first y position of this rectangle
    * @param x2       the second x position of this rectangle
    * @param y2       the second y position of this rectangle
    * @param color    the colour, with the alpha in the top byte
    * @param rotation the rotation in degrees
    * @param zIndex   the z-index against the other elements on the overlay
    */
    public Rect(int x1, int y1, int x2, int y2, int color, float rotation, int zIndex) {
        this(x1, y1, x2, y2, color, 0xFF, rotation, zIndex);
        setColor(color);
    }

    /**
    * Constructs a rectangle from two corners, replacing the color's top byte with the
    * separately supplied alpha and normalizing rotation with {@link Mth#wrapDegrees(float)}.
    *
    * @param x1       the first x position of this rectangle
    * @param y1       the first y position of this rectangle
    * @param x2       the second x position of this rectangle
    * @param y2       the second y position of this rectangle
    * @param color    the colour, whose top byte is replaced by the alpha
    * @param alpha    the value shifted into the top byte
    * @param rotation the rotation in degrees
    * @param zIndex   the z-index against the other elements on the overlay
    */
    public Rect(int x1, int y1, int x2, int y2, int color, int alpha, float rotation, int zIndex) {
        setPos(x1, y1, x2, y2);
        setColor(color, alpha);
        this.rotation = Mth.wrapDegrees(rotation);
        this.zIndex = zIndex;
    }

    /**
    * Sets the first corner's x coordinate without changing any other coordinate.
    *
    * @param x1 the first x position of this rectangle
    * @return self for chaining.
    * @since 1.8.4
    */
    public Rect setX1(int x1) {
        this.x1 = x1;
        return this;
    }

    /**
    * Returns the x coordinate of the first corner; it is not necessarily the left edge.
    *
    * @return the first x position of this rectangle.
    * @since 1.8.4
    */
    public int getX1() {
        return x1;
    }

    /**
    * Sets the first corner's y coordinate without changing any other coordinate.
    *
    * @param y1 the first y position of this rectangle
    * @return self for chaining.
    * @since 1.8.4
    */
    public Rect setY1(int y1) {
        this.y1 = y1;
        return this;
    }

    /**
    * Returns the y coordinate of the first corner; it is not necessarily the top edge.
    *
    * @return the first y position of this rectangle.
    * @since 1.8.4
    */
    public int getY1() {
        return y1;
    }

    /**
    * Sets both coordinates of the first corner, leaving the second corner unchanged.
    *
    * @param x1 the first x position of this rectangle
    * @param y1 the first y position of this rectangle
    * @return self for chaining.
    * @since 1.8.4
    */
    public Rect setPos1(int x1, int y1) {
        this.x1 = x1;
        this.y1 = y1;
        return this;
    }

    /**
    * Sets the second corner's x coordinate without changing any other coordinate.
    *
    * @param x2 the second x position of this rectangle
    * @return self for chaining.
    * @since 1.8.4
    */
    public Rect setX2(int x2) {
        this.x2 = x2;
        return this;
    }

    /**
    * Returns the x coordinate of the second corner; it is not necessarily the right edge.
    *
    * @return the second x position of this rectangle.
    * @since 1.8.4
    */
    public int getX2() {
        return x2;
    }

    /**
    * Sets the second corner's y coordinate without changing any other coordinate.
    *
    * @param y2 the second y position of this rectangle
    * @return self for chaining.
    * @since 1.8.4
    */
    public Rect setY2(int y2) {
        this.y2 = y2;
        return this;
    }

    /**
    * the second y position of this rectangle, its other corner rather than an edge.
    *
    * @return the second y position of the rectangle.
    * @since 1.8.4
    */
    public int getY2() {
        return y2;
    }

    /**
    * sets the second corner, leaving the first corner unchanged.
    *
    * @param x2 the second x position of this rectangle
    * @param y2 the second y position of this rectangle
    * @return self for chaining.
    * @since 1.8.4
    */
    public Rect setPos2(int x2, int y2) {
        this.x2 = x2;
        this.y2 = y2;
        return this;
    }

    /**
    * sets both corners of this rectangle.
    *
    * @param x1 the first x position of this rectangle
    * @param y1 the first y position of this rectangle
    * @param x2 the second x position of this rectangle
    * @param y2 the second y position of this rectangle
    * @return self for chaining.
    * @since 1.1.8
    */
    public Rect setPos(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        return this;
    }

    /**
    * Resizes along the x axis, anchoring the currently smaller x coordinate. If
    * {@code x1 <= x2}, sets {@code x2 = x1 + width}; otherwise sets {@code x1 = x2 + width}.
    * Negative widths are accepted and can reverse the corner order; {@link #getWidth()}
    * reports the absolute coordinate difference, subject to integer overflow.
    *
    * @param width the signed x-coordinate offset in GUI-scaled pixels
    * @return self for chaining
    * @since 1.8.4
    */
    public Rect setWidth(int width) {
        if (x1 <= x2) {
            x2 = x1 + width;
        } else {
            x1 = x2 + width;
        }
        return this;
    }

    /**
    * returns the absolute distance between the two x positions.
    *
    * @return the width of this rectangle.
    * @since 1.8.4
    */
    public int getWidth() {
        return Math.abs(x2 - x1);
    }

    /**
    * Resizes along the y axis, anchoring the currently smaller y coordinate. If
    * {@code y1 <= y2}, sets {@code y2 = y1 + height}; otherwise sets {@code y1 = y2 + height}.
    * Negative heights are accepted and can reverse the corner order; {@link #getHeight()}
    * reports the absolute coordinate difference, subject to integer overflow.
    *
    * @param height the signed y-coordinate offset in GUI-scaled pixels
    * @return self for chaining
    * @since 1.8.4
    */
    public Rect setHeight(int height) {
        if (y1 <= y2) {
            y2 = y1 + height;
        } else {
            y1 = y2 + height;
        }
        return this;
    }

    /**
    * returns the absolute distance between the two y positions.
    *
    * @return the height of this rectangle.
    * @since 1.8.4
    */
    public int getHeight() {
        return Math.abs(y2 - y1);
    }

    /**
    * Resizes both axes by applying {@link #setWidth(int)} then {@link #setHeight(int)}.
    * Each axis anchors its currently smaller coordinate; negative offsets are accepted
    * and can reverse the corresponding corner order.
    *
    * @param width  the signed x-coordinate offset in GUI-scaled pixels
    * @param height the signed y-coordinate offset in GUI-scaled pixels
    * @return self for chaining
    * @since 1.8.4
    */
    public Rect setSize(int width, int height) {
        setWidth(width);
        setHeight(height);
        return this;
    }

    /**
    * Sets the packed ARGB color, making zero-alpha colors with nonzero RGB opaque; zero-alpha black stays transparent, and explicit alpha is preserved.
    *
    * @param color the packed colour, with the alpha in the top byte
    * @return self for chaining.
    * @since 1.0.5
    */
    public Rect setColor(int color) {
        this.color = ColorMath.fixAlpha(color);
        return this;
    }

    /**
    * sets the packed color and replaces its top byte with {@code alpha}; the value is
    * shifted directly and is not clamped to 0–255.
    *
    * @param color the colour, whose top byte is replaced
    * @param alpha the alpha, in the top byte
    * @return self for chaining.
    * @since 1.1.8
    */
    public Rect setColor(int color, int alpha) {
        this.color = (alpha << 24) | (color & 0xFFFFFF);
        return this;
    }

    /**
    * Replaces the alpha byte of this rectangle's packed color; values outside 0–255 are
    * not clamped.
    *
    * @param alpha the alpha to put in the top byte
    * @return self for chaining.
    * @since 1.1.8
    */
    public Rect setAlpha(int alpha) {
        this.color = (color & 0x00FFFFFF) | (alpha << 24);
        return this;
    }

    /**
    * Returns this rectangle's packed ARGB color. Colors set without an alpha byte are
    * made opaque by the one-argument color setter, except pure black.
    *
    * @return the color value of this rectangle.
    * @since 1.8.4
    */
    public int getColor() {
        return color;
    }

    /**
    * Returns the alpha byte of this rectangle's packed color, from 0 (transparent) to
    * 255 (opaque).
    *
    * @return the alpha value of this rectangle.
    * @since 1.8.4
    */
    public int getAlpha() {
        return (color >> 24) & 0xFF;
    }

    /**
    * Sets the rotation in degrees, normalized by {@link Mth#wrapDegrees(float)}.
    *
    * @param rotation the angle in degrees
    * @return self for chaining.
    * @since 1.2.6
    */
    public Rect setRotation(double rotation) {
        this.rotation = Mth.wrapDegrees((float) rotation);
        return this;
    }

    /**
    * Returns this rectangle's rotation in degrees, normalized by its constructor and
    * {@link #setRotation(double)}.
    *
    * @return the rotation of this rectangle.
    * @since 1.8.4
    */
    public float getRotation() {
        return rotation;
    }

    /**
    * Sets whether rotation is about the rectangle's center rather than its first corner.
    *
    * @param rotateCenter whether this rectangle should be rotated around its center
    * @return self for chaining.
    * @since 1.8.4
    */
    public Rect setRotateCenter(boolean rotateCenter) {
        this.rotateCenter = rotateCenter;
        return this;
    }

    /**
    * Returns whether rotation is about the rectangle's center.
    *
    * @return {@code true} if this rectangle should be rotated around its center, {@code false}
    * otherwise.
    * @since 1.8.4
    */
    public boolean isRotatingCenter() {
        return rotateCenter;
    }

    /**
    * Sets this rectangle's overlay draw-order value; lower values draw first.
    *
    * @param zIndex the new z-index for this rectangle
    * @return self for chaining.
    * @since 1.8.4
    */
    public Rect setZIndex(int zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    /**
    * Returns this rectangle's overlay draw-order value; lower values draw first.
    *
    * @return the z-index of this element
    */
    @Override
    public int getZIndex() {
        return zIndex;
    }

    /**
    * Draws this rectangle through the overlay's graphics context. The mouse coordinates
    * and frame delta are unused.
    *
    * @param drawContext the graphics context to draw into
    * @param mouseX      the mouse x, not used by this element
    * @param mouseY      the mouse y, not used by this element
    * @param delta       the frame delta, not used by this element
    */
    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        //? if >1.21.5 {
        Matrix3x2fStack matrices = drawContext.pose();
        matrices.pushMatrix();
        //?} else {
        /*PoseStack matrices = drawContext.pose();
        matrices.pushPose();
        *///?}

        setupMatrix(matrices, x1, y1, 1, rotation, getWidth(), getHeight(), rotateCenter);
        drawContext.fill(x1, y1, x2, y2, this.color);
        //? if >1.21.5 {
        matrices.popMatrix();
        //?} else {
        /*matrices.popPose();
        *///?}
    }

    @Override
    @DocletIgnore
    public void render3D(PoseStack poseStack, RenderSink sink, int light, boolean seeThrough, float delta) {
        poseStack.pushPose();
        poseStack.translate(x1, y1, 0);
        if (rotateCenter) {
            poseStack.translate(getWidth() / 2d, getHeight() / 2d, 0);
        }
        PoseCompat.rotate(poseStack, new Quaternionf().rotateLocalZ((float) Math.toRadians(rotation)));
        if (rotateCenter) {
            poseStack.translate(-getWidth() / 2d, -getHeight() / 2d, 0);
        }
        poseStack.translate(-x1, -y1, 0);

        float brightness = RenderElement.lightBrightness(light);
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f * brightness;
        float g = ((color >> 8) & 0xFF) / 255.0f * brightness;
        float b = (color & 0xFF) / 255.0f * brightness;
        float left = x1;
        float top = y1;
        float right = x2;
        float bottom = y2;
        sink.submitGeometry(poseStack, SurfaceRenderTypes.quads(false, !seeThrough), (pose, vc) -> {
            vc.addVertex(pose, left, top, 0).setColor(r, g, b, a);
            vc.addVertex(pose, right, top, 0).setColor(r, g, b, a);
            vc.addVertex(pose, right, bottom, 0).setColor(r, g, b, a);
            vc.addVertex(pose, left, bottom, 0).setColor(r, g, b, a);
        });

        poseStack.popPose();
    }

    /**
    * Sets the overlay used for parent-relative dimensions and alignment; {@code null}
    * makes those dimensions fall back to the window. Builders assign their overlay when
    * creating the rectangle.
    *
    * @param parent the overlay this element is on, or {@code null} for the window
    * @return self for chaining.
    */
    public Rect setParent(IDraw2D<?> parent) {
        this.parent = parent;
        return this;
    }

    /**
    * Returns this rectangle's width; rectangles have no additional scale.
    *
    * @return the scaled width of this element
    */
    @Override
    public int getScaledWidth() {
        return getWidth();
    }

    /**
    * Returns the assigned overlay's width, or the window's GUI-scaled width when no
    * overlay is assigned.
    *
    * @return the width of the parent
    */
    @Override
    public int getParentWidth() {
        return parent != null ? parent.getWidth() : mc.getWindow().getGuiScaledWidth();
    }

    /**
    * Returns this rectangle's height; rectangles have no additional scale.
    *
    * @return the scaled height of this element
    */
    @Override
    public int getScaledHeight() {
        return getHeight();
    }

    /**
    * Returns the assigned overlay's height, or the window's GUI-scaled height when no
    * overlay is assigned.
    *
    * @return the height of the parent
    */
    @Override
    public int getParentHeight() {
        return parent != null ? parent.getHeight() : mc.getWindow().getGuiScaledHeight();
    }

    /**
    * Returns the smaller of the two x coordinates, independent of corner order.
    *
    * @return the position of the scaled element's left side
    */
    @Override
    public int getScaledLeft() {
        return Math.min(x1, x2);
    }

    /**
    * Returns the smaller of the two y coordinates, independent of corner order.
    *
    * @return the position of the scaled element's top side
    */
    @Override
    public int getScaledTop() {
        return Math.min(y1, y2);
    }

    /**
    * Moves the rectangle so {@code (x, y)} is its first corner, preserving its current
    * width and height.
    *
    * @param x the new x position
    * @param y the new y position
    * @return self for chaining.
    */
    @Override
    public Rect moveTo(int x, int y) {
        return setPos(x, y, x + getScaledWidth(), y + getScaledHeight());
    }

    /**
    * Builder for a rectangle on an overlay. Coordinates can be supplied as two corners
    * or as a first corner and a size. Defaults are zero coordinates, opaque white,
    * zero rotation, and rotation about the center; unlike directly constructed
    * rectangles, builders rotate about the center by default.
    *
    * @author Etheradon
    * @since 1.8.4
    */
    @DocletCategory("Rendering/Graphics")
    public static final class Builder extends RenderElementBuilder<Rect> implements Alignable<Builder> {
        private int x1 = 0;
        private int y1 = 0;
        private int x2 = 0;
        private int y2 = 0;
        private int color = 0xFFFFFFFF;
        private int alpha = 0xFF;
        private float rotation = 0;
        private boolean rotateCenter = true;
        private int zIndex = 0;

        /**
        * Creates a rectangle builder associated with the given overlay.
        *
        * @param draw2D the overlay the rectangles will be added to
        */
        public Builder(IDraw2D<?> draw2D) {
            super(draw2D);
        }

        /**
        * Sets the first x coordinate of the rectangle.
        *
        * @param x1 the first x position of the rectangle
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder x1(int x1) {
            this.x1 = x1;
            return this;
        }

        /**
        * Returns the first x coordinate; it starts at zero.
        *
        * @return the first x position of the rectangle.
        * @since 1.8.4
        */
        public int getX1() {
            return x1;
        }

        /**
        * Sets the first y coordinate of the rectangle.
        *
        * @param y1 the first y position of the rectangle
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder y1(int y1) {
            this.y1 = y1;
            return this;
        }

        /**
        * Returns the first y coordinate; it starts at zero.
        *
        * @return the first y position of the rectangle.
        * @since 1.8.4
        */
        public int getY1() {
            return y1;
        }

        /**
        * Sets the first corner coordinates.
        *
        * @param x1 the first x position of the rectangle
        * @param y1 the first y position of the rectangle
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder pos1(int x1, int y1) {
            this.x1 = x1;
            this.y1 = y1;
            return this;
        }

        /**
        * Sets the second x coordinate of the rectangle.
        *
        * @param x2 the second x position of the rectangle
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder x2(int x2) {
            this.x2 = x2;
            return this;
        }

        /**
        * Returns the second x coordinate; size setters calculate it from the first x coordinate.
        *
        * @return the second x position of the rectangle.
        * @since 1.8.4
        */
        public int getX2() {
            return x2;
        }

        /**
        * Sets the second y coordinate of the rectangle.
        *
        * @param y2 the second y position of the rectangle
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder y2(int y2) {
            this.y2 = y2;
            return this;
        }

        /**
        * Returns the second y coordinate; size setters calculate it from the first y coordinate.
        *
        * @return the second y position of the rectangle.
        * @since 1.8.4
        */
        public int getY2() {
            return y2;
        }

        /**
        * Sets the second corner coordinates.
        *
        * @param x2 the second x position of the rectangle
        * @param y2 the second y position of the rectangle
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder pos2(int x2, int y2) {
            this.x2 = x2;
            this.y2 = y2;
            return this;
        }

        /**
        * Sets both rectangle corners.
        *
        * @param x1 the first x position of the rectangle
        * @param y1 the first y position of the rectangle
        * @param x2 the second x position of the rectangle
        * @param y2 the second y position of the rectangle
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder pos(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            return this;
        }

        /**
        * Sets the second x coordinate to {@code x1 + width}; the value is measured from
        * the first corner, regardless of the existing second corner.
        *
        * @param width the width of the rectangle
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder width(int width) {
            this.x2 = this.x1 + width;
            return this;
        }

        /**
        * Returns the absolute distance between the two x coordinates.
        *
        * @return the width of the rectangle.
        * @since 1.8.4
        */
        public int getWidth() {
            return Math.abs(this.x2 - this.x1);
        }

        /**
        * Sets the second y coordinate to {@code y1 + height}; the value is measured from
        * the first corner, regardless of the existing second corner.
        *
        * @param height the height of the rectangle
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder height(int height) {
            this.y2 = this.y1 + height;
            return this;
        }

        /**
        * Returns the absolute distance between the two y coordinates.
        *
        * @return the height of the rectangle.
        * @since 1.8.4
        */
        public int getHeight() {
            return Math.abs(this.y2 - this.y1);
        }

        /**
        * Sets the rectangle's width and height, measured from its first corner.
        *
        * @param width  the width of the rectangle
        * @param height the height of the rectangle
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder size(int width, int height) {
            return width(width).height(height);
        }

        /**
        * Sets a packed color and stores its fixed-up alpha separately; a later
        * {@code alpha} call replaces that alpha.
        *
        * @param color the color of the rectangle
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder color(int color) {
            this.color = color;
            this.alpha = ColorMath.fixAlpha(color) >>> 24;
            return this;
        }

        /**
        * Packs the components into the low 24 bits; the separately stored alpha is
        * unchanged. Component values are packed without range validation.
        *
        * @param r the red component of the color
        * @param g the green component of the color
        * @param b the blue component of the color
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder color(int r, int g, int b) {
            this.color = (r << 16) | (g << 8) | b;
            return this;
        }

        /**
        * Packs the RGB components into the low 24 bits and stores {@code a} separately;
        * component values are packed without range validation.
        *
        * @param r the red component of the color
        * @param g the green component of the color
        * @param b the blue component of the color
        * @param a the alpha value of the color
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder color(int r, int g, int b, int a) {
            this.color = (r << 16) | (g << 8) | b;
            this.alpha = a;
            return this;
        }

        /**
        * Stores the color and alpha separately, without applying the packed-color
        * overload's alpha fix-up.
        *
        * @param color the color of the rectangle
        * @param alpha the alpha value of the color
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder color(int color, int alpha) {
            this.color = color;
            this.alpha = alpha;
            return this;
        }

        /**
        * Returns the stored color value. Component-based {@code color} overloads store
        * only the low 24 RGB bits; packed-color overloads retain the supplied value.
        *
        * @return the color of the rectangle.
        * @since 1.8.4
        */
        public int getColor() {
            return color;
        }

        /**
        * Sets the alpha value independently of the stored color. The value is not
        * clamped; when built, its shifted bits may also affect the color channels.
        *
        * @param alpha the alpha value of the color
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder alpha(int alpha) {
            this.alpha = alpha;
            return this;
        }

        /**
        * Returns the separately stored alpha value, initially 255.
        *
        * @return the alpha value of the color.
        * @since 1.8.4
        */
        public int getAlpha() {
            return alpha;
        }

        /**
        * Sets the rotation in degrees; the builder stores the supplied value as a
        * {@code float} without normalizing it.
        *
        * @param rotation the rotation (clockwise) of the rectangle in degrees
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder rotation(double rotation) {
            this.rotation = (float) rotation;
            return this;
        }

        /**
        * Returns the stored rotation in degrees, without normalization.
        *
        * @return the rotation (clockwise) of the rectangle in degrees.
        * @since 1.8.4
        */
        public float getRotation() {
            return rotation;
        }

        /**
        * Selects whether rotation is around the rectangle's center; builders default
        * to {@code true}.
        *
        * @param rotateCenter whether this rectangle should be rotated around its center
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder rotateCenter(boolean rotateCenter) {
            this.rotateCenter = rotateCenter;
            return this;
        }

        /**
        * Returns whether the built rectangle rotates around its center.
        *
        * @return {@code true} if this rectangle should be rotated around its center,
        * {@code false} otherwise.
        * @since 1.8.4
        */
        public boolean isRotatingCenter() {
            return rotateCenter;
        }

        /**
        * Sets the overlay ordering index; lower values are drawn first. Builders
        * default to zero.
        *
        * @param zIndex the z-index of the rectangle
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder zIndex(int zIndex) {
            this.zIndex = zIndex;
            return this;
        }

        /**
        * Returns the overlay ordering index to be used when building the rectangle.
        *
        * @return the z-index of the rectangle.
        * @since 1.8.4
        */
        public int getZIndex() {
            return zIndex;
        }

        /**
        * Creates a new rectangle from the builder's current values and assigns the
        * builder's overlay as its parent. The builder's alpha is combined with its
        * stored RGB value when the rectangle is constructed.
        *
        * @return the new rectangle.
        */
        @Override
        public Rect createElement() {
            return new Rect(x1, y1, x2, y2, color, alpha, rotation, zIndex).setRotateCenter(rotateCenter).setParent(
                    parent);
        }

        /**
        * Returns the builder's width; rectangles have no additional scale.
        *
        * @return the scaled width
        */
        @Override
        public int getScaledWidth() {
            return getWidth();
        }

        /**
        * Returns the width of the builder's overlay, used by parent-relative alignment.
        *
        * @return the width of the parent
        */
        @Override
        public int getParentWidth() {
            return parent.getWidth();
        }

        /**
        * Returns the builder's height; rectangles have no additional scale.
        *
        * @return the scaled height
        */
        @Override
        public int getScaledHeight() {
            return getHeight();
        }

        /**
        * Returns the height of the builder's overlay, used by parent-relative alignment.
        *
        * @return the height of the parent
        */
        @Override
        public int getParentHeight() {
            return parent.getHeight();
        }

        /**
        * Returns the smaller of the builder's two x coordinates, regardless of corner order.
        *
        * @return the position of the scaled element's left side
        */
        @Override
        public int getScaledLeft() {
            return Math.min(x1, x2);
        }

        /**
        * Returns the smaller of the builder's two y coordinates, regardless of corner order.
        *
        * @return the position of the scaled element's top side
        */
        @Override
        public int getScaledTop() {
            return Math.min(y1, y2);
        }

        /**
        * Moves the builder so {@code (x, y)} is its first corner, preserving its current
        * width and height.
        *
        * @param x the new x position
        * @param y the new y position
        * @return self for chaining.
        */
        @Override
        public Builder moveTo(int x, int y) {
            return pos(x, y, x + getWidth(), y + getHeight());
        }

    }

}
