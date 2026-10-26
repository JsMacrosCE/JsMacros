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
 * A line is defined by two endpoints, a stroke thickness, and a rotation. The
 * endpoint differences determine its scaled width and height; {@code width} is the
 * stroke thickness. Nonpositive thickness is not rendered, and the 2D renderer
 * rounds positive thickness and rendered length up to whole pixels. Rotation is
 * about the first endpoint by default, or the midpoint when {@code rotateCenter}
 * is true. Constructors and {@link #setRotation(double)} normalize finite angles to
 * {@code [-180, 180)}.
 * <p>
  * <pre>
 * const draw = Hud.createDraw2D();
 * const line = draw.addLine(0, 0, 50, 50, Color.rgb(0xFF, 0xFF, 0xFF), 0, 2);
 * draw.register();
 * Chat.log(`${line.getWidth()} thick, reaching ${line.getScaledWidth()} across`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Rendering/Graphics")
public class Line implements RenderElement, Alignable<Line> {

    /**
    * The overlay this line belongs to, or {@code null} if it is not attached to one.
    * Parent dimensions then fall back to the GUI-scaled window dimensions.
    */
    @Nullable
    public IDraw2D<?> parent;
    /**
    * The x coordinate of the first endpoint. Endpoint order is caller-defined.
    */
    public int x1;
    /**
    * The y coordinate of the first endpoint.
    */
    public int y1;
    /**
    * The x coordinate of the second endpoint.
    */
    public int x2;
    /**
    * The y coordinate of the second endpoint.
    */
    public int y2;
    /**
    * The packed ARGB color. Colors with a zero alpha byte are made opaque except for pure black.
    */
    public int color;
    /**
    * The rotation in degrees. Constructors and {@link #setRotation(double)} normalize
    * finite angles to {@code [-180, 180)}; direct field writes do not.
    */
    public float rotation;
    /**
    * Whether rotation is about the midpoint rather than the first endpoint. Directly
    * constructed lines default to {@code false}; builder-created lines default to {@code true}.
    */
    public boolean rotateCenter;
    /**
    * The stroke thickness in screen pixels. The value is stored as a float; the builder
    * defaults to one, and the 2D renderer rounds positive widths up when drawing.
    */
    public float width;
    /**
    * Draw-order value relative to other elements on the overlay; lower values are drawn first.
    */
    public int zIndex;

    /**
    * Creates a line between two endpoints.
    * <p>
    * Finite rotations are normalized to {@code [-180, 180)}. The color is processed by
    * {@link #setColor(int)}; a zero-alpha color becomes opaque except for pure black.
    *
    * @param x1       the x position of the start of the line
    * @param y1       the y position of the start of the line
    * @param x2       the x position of the end of the line
    * @param y2       the y position of the end of the line
    * @param color    the colour, with the alpha in the top byte
    * @param rotation the rotation in degrees
    * @param width    the thickness of the line
    * @param zIndex   the z-index against the other elements on the overlay
    */
    public Line(int x1, int y1, int x2, int y2, int color, float rotation, float width, int zIndex) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        setColor(color);
        this.rotation = Mth.wrapDegrees(rotation);
        this.width = width;
        this.zIndex = zIndex;
    }

    /**
    * Sets the first endpoint's x coordinate without changing the other coordinates.
    *
    * @param x1 the x position of the start of the line
    * @return self for chaining.
    * @since 1.8.4
    */
    public Line setX1(int x1) {
        this.x1 = x1;
        return this;
    }

    /**
    * Returns the x coordinate of the first endpoint, which need not be the leftmost.
    *
    * @return the x position of the start of the line.
    * @since 1.8.4
    */
    public int getX1() {
        return x1;
    }

    /**
    * Sets the first endpoint's y coordinate without changing the other coordinates.
    *
    * @param y1 the y position of the start of the line
    * @return self for chaining.
    * @since 1.8.4
    */
    public Line setY1(int y1) {
        this.y1 = y1;
        return this;
    }

    /**
    * Returns the y coordinate of the first endpoint, which need not be the topmost.
    *
    * @return the y position of the start of the line.
    * @since 1.8.4
    */
    public int getY1() {
        return y1;
    }

    /**
    * Sets both coordinates of the first endpoint without changing the second.
    *
    * @param x1 the x position of the start of the line
    * @param y1 the y position of the start of the line
    * @return self for chaining.
    * @since 1.8.4
    */
    public Line setPos1(int x1, int y1) {
        this.x1 = x1;
        this.y1 = y1;
        return this;
    }

    /**
    * Sets the second endpoint's x coordinate without changing the other coordinates.
    *
    * @param x2 the x position of the end of the line
    * @return self for chaining.
    * @since 1.8.4
    */
    public Line setX2(int x2) {
        this.x2 = x2;
        return this;
    }

    /**
    * Returns the x coordinate of the second endpoint, which need not be the rightmost.
    *
    * @return the x position of the end of the line.
    * @since 1.8.4
    */
    public int getX2() {
        return x2;
    }

    /**
    * Sets the second endpoint's y coordinate without changing the other coordinates.
    *
    * @param y2 the y position of the end of the line
    * @return self for chaining.
    * @since 1.8.4
    */
    public Line setY2(int y2) {
        this.y2 = y2;
        return this;
    }

    /**
    * Returns the y coordinate of the line's second endpoint.
    *
    * @return the y position of the end of the line.
    * @since 1.8.4
    */
    public int getY2() {
        return y2;
    }

    /**
    * Sets the second endpoint, leaving the first unchanged.
    *
    * @param x2 the x position of the end of the line
    * @param y2 the y position of the end of the line
    * @return self for chaining.
    * @since 1.8.4
    */
    public Line setPos2(int x2, int y2) {
        this.x2 = x2;
        this.y2 = y2;
        return this;
    }

    /**
    * Sets both endpoints of the line.
    *
    * @param x1 the x position of the start of the line
    * @param y1 the y position of the start of the line
    * @param x2 the x position of the end of the line
    * @param y2 the y position of the end of the line
    * @return self for chaining.
    * @since 1.8.4
    */
    public Line setPos(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        return this;
    }

    /**
    * Sets the packed ARGB color. A zero alpha byte with nonzero RGB is treated as opaque;
    * pure black remains transparent.
    *
    * @param color the color of the line
    * @return self for chaining.
    * @since 1.8.4
    */
    public Line setColor(int color) {
        this.color = ColorMath.fixAlpha(color);
        return this;
    }

    /**
    * Sets the RGB color and alpha separately. The alpha replaces the top byte and is
    * not clamped; only its low eight bits are returned by {@link #getAlpha()}.
    *
    * @param color the color of the line
    * @param alpha the alpha of the line
    * @return self for chaining.
    * @since 1.8.4
    */
    public Line setColor(int color, int alpha) {
        this.color = (alpha << 24) | (color & 0xFFFFFF);
        return this;
    }

    /**
    * Returns the packed ARGB color currently stored on the line.
    *
    * @return the color of the line.
    * @since 1.8.4
    */
    public int getColor() {
        return color;
    }

    /**
    * Sets the alpha byte without changing the RGB channels. Values are not clamped.
    *
    * @param alpha the alpha value of the line's color
    * @return self for chaining.
    * @since 1.8.4
    */
    public Line setAlpha(int alpha) {
        this.color = (alpha << 24) | (color & 0xFFFFFF);
        return this;
    }

    /**
    * Returns the top byte of the packed color, from 0 (transparent) to 255 (opaque).
    *
    * @return the alpha value of the line's color.
    * @since 1.8.4
    */
    public int getAlpha() {
        return (color >> 24) & 0xFF;
    }

    /**
    * Sets the rotation in degrees, normalized by Minecraft's degree-wrapping function.
    * Rotation is about the midpoint when {@code rotateCenter} is true, otherwise about
    * the first endpoint.
    *
    * @param rotation the rotation (clockwise) of the line
    * @return self for chaining.
    * @since 1.8.4
    */
    public Line setRotation(double rotation) {
        this.rotation = Mth.wrapDegrees((float) rotation);
        return this;
    }

    /**
    * Returns the stored rotation in degrees. Constructors and setters normalize finite
    * angles; direct writes to the public field do not.
    *
    * @return the rotation (clockwise) of the line.
    * @since 1.8.4
    */
    public float getRotation() {
        return rotation;
    }

    /**
    * Selects whether rotation is about the line's midpoint or its first endpoint.
    *
    * @param rotateCenter whether this line should be rotated around its center
    * @return self for chaining.
    * @since 1.8.4
    */
    public Line setRotateCenter(boolean rotateCenter) {
        this.rotateCenter = rotateCenter;
        return this;
    }

    /**
    * Returns whether rotation is about this line's midpoint.
    *
    * @return {@code true} if this line should be rotated around its center, {@code false}
    * otherwise.
    * @since 1.8.4
    */
    public boolean isRotatingCenter() {
        return rotateCenter;
    }

    /**
    * Sets the line's stroke thickness in screen pixels. Values are stored as floats;
    * non-positive widths are not rendered, and fractional widths are rounded up when drawn.
    *
    * @param width the width of the line
    * @return self for chaining.
    * @since 1.8.4
    */
    public Line setWidth(double width) {
        this.width = (float) width;
        return this;
    }

    /**
    * Returns the line's stroke thickness in screen pixels.
    *
    * @return the width of the line.
    * @since 1.8.4
    */
    public float getWidth() {
        return width;
    }

    /**
    * Sets this line's draw-order value relative to other elements in its overlay;
    * lower values are drawn first. Equal values retain insertion order in the overlay.
    *
    * @param zIndex the z-index of the line
    * @return self for chaining.
    * @since 1.8.4
    */
    public Line setZIndex(int zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    /**
    * Returns this line's draw-order value.
    *
    * @return the z-index of this element
    */
    @Override
    public int getZIndex() {
        return zIndex;
    }

    /**
     * Draws the line as a filled strip between its endpoints, applying the element's
     * rotation and endpoint orientation. Widths less than or equal to zero skip drawing.
     * The strip bounds use floor/ceiling integer rounding of its thickness and length.
     * The mouse coordinates and frame delta are unused. The graphics pose is pushed
     * and popped on normal completion.
     *
     * @param drawContext the graphics context receiving the strip
     * @param mouseX unused mouse x coordinate
     * @param mouseY unused mouse y coordinate
     * @param delta unused frame delta
     */
    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (width <= 0) return;
        //? if >1.21.5 {
        Matrix3x2fStack matrices = drawContext.pose();
        matrices.pushMatrix();
        //?} else {
        /*PoseStack matrices = drawContext.pose();
        matrices.pushPose();
        *///?}
        setupMatrix(matrices, x1, y1, 1, rotation, getScaledWidth(), getScaledHeight(), rotateCenter);

        float halfWidth = this.width / 2.0f;
        float dx = this.x2 - this.x1;
        float dy = this.y2 - this.y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.atan2(dy, dx);

        //? if >1.21.5 {
        matrices.translate(this.x1, this.y1);
        matrices.rotate(angle);
        //?} else {
        /*matrices.translate(this.x1, this.y1, 0);
        matrices.mulPose(new Quaternionf().rotateLocalZ(angle));
        *///?}

        drawContext.fill(
                0,
                -(int) Math.floor(halfWidth),
                (int) Math.ceil(length),
                -(int) Math.floor(halfWidth) + Math.max(1, (int) Math.ceil(this.width)),
                this.color
        );

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
            poseStack.translate(getScaledWidth() / 2d, getScaledHeight() / 2d, 0);
        }
        PoseCompat.rotate(poseStack, new Quaternionf().rotateLocalZ((float) Math.toRadians(rotation)));
        if (rotateCenter) {
            poseStack.translate(-getScaledWidth() / 2d, -getScaledHeight() / 2d, 0);
        }
        poseStack.translate(-x1, -y1, 0);

        float brightness = RenderElement.lightBrightness(light);
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f * brightness;
        float g = ((color >> 8) & 0xFF) / 255.0f * brightness;
        float b = (color & 0xFF) / 255.0f * brightness;

        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length == 0 || width <= 0) {
            poseStack.popPose();
            return;
        }

        // Render a quad instead of relying on the target-specific line vertex
        // format. This preserves the script-facing width in surface pixel units
        // on every supported version.
        float halfWidth = width / 2.0f;
        float offsetX = -dy / length * halfWidth;
        float offsetY = dx / length * halfWidth;
        float startX = x1;
        float startY = y1;
        float endX = x2;
        float endY = y2;
        sink.submitGeometry(poseStack, SurfaceRenderTypes.quads(false, !seeThrough), (pose, vc) -> {
            vc.addVertex(pose, startX + offsetX, startY + offsetY, 0).setColor(r, g, b, a);
            vc.addVertex(pose, startX - offsetX, startY - offsetY, 0).setColor(r, g, b, a);
            vc.addVertex(pose, endX - offsetX, endY - offsetY, 0).setColor(r, g, b, a);
            vc.addVertex(pose, endX + offsetX, endY + offsetY, 0).setColor(r, g, b, a);
        });

        poseStack.popPose();
    }

    /**
    * Sets the overlay used for parent-relative dimensions and alignment. Passing
    * {@code null} makes parent dimensions fall back to the window.
    *
    * @param parent the overlay this element is on, or {@code null} for the window
    * @return self for chaining.
    */
    public Line setParent(IDraw2D<?> parent) {
        this.parent = parent;
        return this;
    }

    /**
    * Moves the line so its minimum x and y coordinates match the requested position,
    * translating both endpoints without changing their relative positions.
    *
    * @param x the new x position
    * @param y the new y position
    * @return self for chaining.
    */
    @Override
    public Line moveTo(int x, int y) {
        int dx = x - getScaledLeft();
        int dy = y - getScaledTop();
        return setPos(x1 + dx, y1 + dy, x2 + dx, y2 + dy);
    }

    /**
    * Returns the absolute difference between the line's x coordinates.
    *
    * @return the scaled width
    */
    @Override
    public int getScaledWidth() {
        return Math.abs(x2 - x1);
    }

    /**
    * Returns the parent overlay's width, or the GUI-scaled window width when no parent is set.
    *
    * @return the width of the parent
    */
    @Override
    public int getParentWidth() {
        return parent != null ? parent.getWidth() : mc.getWindow().getGuiScaledWidth();
    }

    /**
    * Returns the absolute difference between the line's two y coordinates.
    *
    * @return the scaled height
    */
    @Override
    public int getScaledHeight() {
        return Math.abs(y2 - y1);
    }

    /**
    * Returns the parent overlay's height, or the GUI-scaled window height if no parent is set.
    *
    * @return the height of the parent
    */
    @Override
    public int getParentHeight() {
        return parent != null ? parent.getHeight() : mc.getWindow().getGuiScaledHeight();
    }

    /**
    * Returns the smaller of the line's two x coordinates.
    *
    * @return the position of the scaled element's left side
    */
    @Override
    public int getScaledLeft() {
        return Math.min(x1, x2);
    }

    /**
    * Returns the smaller of the line's two y coordinates.
    *
    * @return the position of the scaled element's top side
    */
    @Override
    public int getScaledTop() {
        return Math.min(y1, y2);
    }

    /**
    * Builder for configuring and creating a line on an overlay. The four endpoint
    * coordinates default to zero, width to one, rotation to zero, and color to opaque
    * white; builder-created lines rotate around their center by default.
    *
    * @author Etheradon
    * @since 1.8.4
    */
    @DocletCategory("Rendering/Graphics")
    public static class Builder extends RenderElementBuilder<Line> implements Alignable<Builder> {

        private int x1 = 0;
        private int y1 = 0;
        private int x2 = 0;
        private int y2 = 0;
        private float rotation = 0;
        private boolean rotateCenter = true;
        private int color = 0xFFFFFFFF;
        private int alpha = 0xFF;
        private int zIndex = 0;
        private float width = 1;

        /**
        * Creates a builder associated with the given overlay.
        *
        * @param draw2D the overlay the lines will be added to
        */
        public Builder(IDraw2D<?> draw2D) {
            super(draw2D);
        }

        /**
        * Sets the x coordinate of the first endpoint.
        *
        * @param x1 the x position of the first point
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder x1(int x1) {
            this.x1 = x1;
            return this;
        }

        /**
        * Returns the x coordinate of the first endpoint.
        *
        * @return the x position of the first point.
        * @since 1.8.4
        */
        public int getX1() {
            return x1;
        }

        /**
        * Sets the y coordinate of the first endpoint.
        *
        * @param y1 the y position of the first point
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder y1(int y1) {
            this.y1 = y1;
            return this;
        }

        /**
        * Returns the y coordinate of the first endpoint.
        *
        * @return the y position of the first point.
        * @since 1.8.4
        */
        public int getY1() {
            return y1;
        }

        /**
        * Sets both coordinates of the first endpoint, leaving the second unchanged.
        *
        * @param x1 the x position of the first point
        * @param y1 the y position of the first point
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder pos1(int x1, int y1) {
            this.x1 = x1;
            this.y1 = y1;
            return this;
        }

        /**
        * Sets the second endpoint's x coordinate without changing other coordinates.
        *
        * @param x2 the x position of the second point
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder x2(int x2) {
            this.x2 = x2;
            return this;
        }

        /**
        * Returns the x coordinate of the builder's second endpoint (zero by default).
        *
        * @return the x position of the second point.
        * @since 1.8.4
        */
        public int getX2() {
            return x2;
        }

        /**
        * Sets the second endpoint's y coordinate without changing other coordinates.
        *
        * @param y2 the y position of the second point
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder y2(int y2) {
            this.y2 = y2;
            return this;
        }

        /**
        * Returns the y coordinate of the builder's second endpoint (zero by default).
        *
        * @return the y position of the second point.
        * @since 1.8.4
        */
        public int getY2() {
            return y2;
        }

        /**
        * Sets both coordinates of the second endpoint, leaving the first unchanged.
        *
        * @param x2 the x position of the second point
        * @param y2 the y position of the second point
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder pos2(int x2, int y2) {
            this.x2 = x2;
            this.y2 = y2;
            return this;
        }

        /**
        * Sets both endpoints of the line.
        *
        * @param x1 the x position of the first point
        * @param y1 the y position of the first point
        * @param x2 the x position of the second point
        * @param y2 the y position of the second point
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
        * Sets the rotation in degrees. The builder stores the supplied value; the built
        * line normalizes finite angles, and rotates about its center by default.
        *
        * @param rotation the rotation (clockwise) of the line
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder rotation(double rotation) {
            this.rotation = (float) rotation;
            return this;
        }

        /**
        * Returns the stored rotation in degrees (zero by default); the built line
        * normalizes finite angles.
        *
        * @return the rotation (clockwise) of the line.
        * @since 1.8.4
        */
        public float getRotation() {
            return rotation;
        }

        /**
        * Selects whether rotation is about the line's center rather than its first
        * endpoint. Builder-created lines default to center rotation.
        *
        * @param rotateCenter whether this line should be rotated around its center
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder rotateCenter(boolean rotateCenter) {
            this.rotateCenter = rotateCenter;
            return this;
        }

        /**
        * Returns whether lines built by this builder rotate about their midpoint.
        * Builder-created lines use midpoint rotation by default.
        *
        * @return {@code true} if this line should be rotated around its center,
        * {@code false} otherwise.
        * @since 1.8.4
        */
        public boolean isRotatingCenter() {
            return rotateCenter;
        }

        /**
        * Sets the stroke thickness in screen pixels; it does not change the distance
        * between the endpoints. Values, including zero and negative values, are stored
        * as floats without validation.
        *
        * @param width the width of the line
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder width(double width) {
            this.width = (float) width;
            return this;
        }

        /**
        * Returns the configured stroke thickness in screen pixels; this is distinct
        * from the endpoint-to-endpoint extent returned by {@code getScaledWidth()}.
        *
        * @return the width of the line.
        * @since 1.8.4
        */
        public float getWidth() {
            return width;
        }

        /**
        * Sets the packed color and derives the builder's separate alpha from the value after
        * applying the renderer's zero-alpha rule. A later {@code alpha} call changes the
        * separate alpha without changing this packed color.
        *
        * @param color the color of the line
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder color(int color) {
            this.color = color;
            this.alpha = ColorMath.fixAlpha(color) >>> 24;
            return this;
        }

        /**
        * Sets the packed color and separate alpha without fix-up. The builder uses
        * the separate alpha as the built line's top byte, so this overload can retain
        * an alpha of zero.
        *
        * @param color the color of the line
        * @param alpha the alpha component of the color
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder color(int color, int alpha) {
            this.color = color;
            this.alpha = alpha;
            return this;
        }

        /**
        * Packs the components into the color's low 24 bits without changing the
        * separate alpha. Components are shifted and combined as supplied, without
        * range validation; the built line takes its top byte from the separate alpha.
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
        * Packs all four components into the stored color without range validation.
        * This does not change the builder's separate alpha: when built, the line uses
        * that separate value for its top byte, so use {@code alpha} to set transparency.
        *
        * @param r the red component of the color
        * @param g the green component of the color
        * @param b the blue component of the color
        * @param a the alpha value of the color
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder color(int r, int g, int b, int a) {
            this.color = (a << 24) | (r << 16) | (g << 8) | b;
            return this;
        }

        /**
        * Returns the stored packed color. Its value depends on the setter used; when
        * building, the separate alpha replaces the stored color's top byte.
        *
        * @return the color of the line.
        * @since 1.8.4
        */
        public int getColor() {
            return color;
        }

        /**
        * Sets the separate alpha without changing the stored color. The value is not
        * clamped; when building, its low eight bits become the line's alpha byte.
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
        * Returns the separate alpha used for the built line's top byte. It defaults to
        * opaque and is independent of the alpha packed by the four-component
        * {@code color} overload.
        *
        * @return the alpha value of the color.
        * @since 1.8.4
        */
        public int getAlpha() {
            return alpha;
        }

        /**
        * Sets this builder's draw-order value; lower values are drawn first.
        *
        * @param zIndex the z-index of the line
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder zIndex(int zIndex) {
            this.zIndex = zIndex;
            return this;
        }

        /**
        * Returns the draw-order value that will be assigned to the built line.
        *
        * @return the z-index of the line.
        * @since 1.8.4
        */
        public int getZIndex() {
            return zIndex;
        }

        /**
        * Creates a line from the builder's current values and associates it with the
        * builder's overlay. Each call creates a new line; its color uses the builder's
        * separate alpha as the top byte and the color's low 24 bits as RGB.
        *
        * @return the new line.
        */
        @Override
        protected Line createElement() {
            return new Line(
                    x1,
                    y1,
                    x2,
                    y2,
                    (alpha << 24) | (color & 0xFFFFFF),
                    rotation,
                    width,
                    zIndex
            ).setRotateCenter(rotateCenter).setParent(parent);
        }

        /**
        * Translates both endpoints so their minimum x and y coordinates match the
        * requested position, preserving their relative positions.
        *
        * @param x the new x position
        * @param y the new y position
        * @return self for chaining.
        */
        @Override
        public Builder moveTo(int x, int y) {
            int dx = x - getScaledLeft();
            int dy = y - getScaledTop();
            return pos(x1 + dx, y1 + dy, x2 + dx, y2 + dy);
        }

        /**
        * Returns the absolute difference between the endpoints' x coordinates; this is
        * the line's horizontal extent, not its stroke thickness.
        *
        * @return the scaled width
        */
        @Override
        public int getScaledWidth() {
            return Math.abs(x2 - x1);
        }

        /**
        * Returns the width of the overlay associated with this builder.
        *
        * @return the width of the parent
        */
        @Override
        public int getParentWidth() {
            return parent.getWidth();
        }

        /**
        * Returns the absolute difference between the endpoints' y coordinates; this is
        * the line's vertical extent, not its stroke thickness.
        *
        * @return the scaled height
        */
        @Override
        public int getScaledHeight() {
            return Math.abs(y2 - y1);
        }

        /**
        * Returns the height of the overlay associated with this builder.
        *
        * @return the height of the parent
        */
        @Override
        public int getParentHeight() {
            return parent.getHeight();
        }

        /**
        * Returns the smaller x coordinate of the two endpoints.
        *
        * @return the position of the scaled element's left side
        */
        @Override
        public int getScaledLeft() {
            return Math.min(x1, x2);
        }

        /**
        * Returns the smaller y coordinate of the two endpoints.
        *
        * @return the position of the scaled element's top side
        */
        @Override
        public int getScaledTop() {
            return Math.min(y1, y2);
        }

    }

}
