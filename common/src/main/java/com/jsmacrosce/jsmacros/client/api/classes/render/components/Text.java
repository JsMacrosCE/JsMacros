package com.jsmacrosce.jsmacros.client.api.classes.render.components;

import com.jsmacrosce.jsmacros.client.util.PoseCompat;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.render.RenderSink;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
//? if <26.2 {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.client.api.classes.TextBuilder;
import com.jsmacrosce.jsmacros.client.api.classes.render.IDraw2D;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.util.ColorMath;

import java.nio.FloatBuffer;

 /**
 * A positioned text element with colour, shadow, scale, and rotation.
 * <p>
 * Text may be plain or carry styling, hover text, and click actions through
 * {@link TextHelper}. Its font-measured width is cached when created and refreshed by
 * {@code setText}; assigning {@code text} directly does not refresh it.
 * <p>
 * Alignment uses scaled dimensions. A screen overlay can use this element's text
 * interactions, while a plain overlay does not provide mouse input.
 * <pre>
 * const draw = Hud.createDraw2D();
 * const label = draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
 * draw.register();
 * Chat.log(`${label.getWidth()} wide, ${label.getHeight()} tall, as the font measures it`);
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.0.5
 */
@SuppressWarnings("unused")
@DocletCategory("Rendering/Graphics")
public class Text implements RenderElement, Alignable<Text> {

    /**
    * the overlay this text belongs to, or {@code null} if unbound. Parent dimensions
    * then come from the window.
    */
    @Nullable
    public IDraw2D<?> parent;

    /**
    * the text to draw. Assigning this field directly does not update the cached
    * {@code width}; use {@code setText} to update both.
    */
    public net.minecraft.network.chat.Component text;
    /**
    * the scale applied to this text and its dimensions. Builder-created text defaults
    * to {@code 1}; constructors take the scale as an argument. Negative values are
    * accepted by the element but not by its builder.
    */
    public double scale;
    /**
    * the rotation in degrees, folded by constructors and {@link #setRotation(double)}
    * into the range {@code -180} to {@code 180}. The builder retains its unfurled value.
    */
    public float rotation;
    /**
    * whether rotation is about the text's center. Directly constructed text defaults
    * to {@code false}; builder-created text defaults to {@code true}.
    */
    public boolean rotateCenter;
    /**
    * the x coordinate where the text line starts.
    */
    public int x;
    /**
    * the y coordinate at the top of the text line.
    */
    public int y;
    /**
    * the packed tint, with alpha in the top byte. A value without alpha is made opaque
    * unless it is pure black.
    */
    public int color;
    /**
    * the cached font-measured width, set on construction and refreshed by {@code setText}.
    * Direct assignment to {@code text} leaves this value unchanged.
    */
    public int width;
    /**
    * whether the game's font draws a drop shadow behind the glyphs.
    */
    public boolean shadow;
    /**
    * the order of this text among elements on its overlay; lower values draw first.
    */
    public int zIndex;

    /**
    * makes a text element from a literal string and measures its width with the game's
    * font. The rotation is wrapped to a single turn.
    * <p>
    * A string has no styling, hover text, or click action; use {@link TextHelper} for
    * those.
    *
    * @param text    what to draw
    * @param x       the x position
    * @param y       the y position
    * @param color   the colour, with the alpha in the top byte
    * @param zIndex  the z-index against the other elements on the overlay
    * @param shadow  whether to draw the game's drop shadow behind the glyphs
    * @param scale   the scale to apply
    * @param rotation the rotation in degrees, folded into a single turn
    */
    public Text(String text, int x, int y, int color, int zIndex, boolean shadow, double scale, float rotation) {
        this(TextHelper.wrap(net.minecraft.network.chat.Component.literal(text)), x, y, color, zIndex, shadow, scale, rotation);
    }

    /**
    * makes a text element from a helper's text, preserving its styling, hover text, and
    * click actions, and measures its width with the game's font. The rotation is wrapped
    * to a single turn.
    *
    * @param text    what to draw, with its styling
    * @param x       the x position
    * @param y       the y position
    * @param color   the colour, with the alpha in the top byte
    * @param zIndex  the z-index against the other elements on the overlay
    * @param shadow  whether to draw the game's drop shadow behind the glyphs
    * @param scale   the scale to apply
    * @param rotation the rotation in degrees, folded into a single turn
    */
    public Text(TextHelper text, int x, int y, int color, int zIndex, boolean shadow, double scale, float rotation) {
        this.text = text.getRaw();
        this.x = x;
        this.y = y;
        setColor(color);
        this.width = mc.font.width(this.text);
        this.shadow = shadow;
        this.scale = scale;
        this.rotation = Mth.wrapDegrees(rotation);
        this.zIndex = zIndex;
    }

    /**
    * sets the x position without changing the y position or text.
    *
    * @param x the new x position for this text element
    * @return self for chaining.
    * @since 1.8.4
    */
    public Text setX(int x) {
        this.x = x;
        return this;
    }

    /**
    * returns the x position where the text line starts; scaling does not change it.
    *
    * @return the x position of this element.
    * @since 1.8.4
    */
    public int getX() {
        return x;
    }

    /**
    * sets the y position without changing the x position or text.
    *
    * @param y the new y position for this text element
    * @return self for chaining.
    * @since 1.8.4
    */
    public Text setY(int y) {
        this.y = y;
        return this;
    }

    /**
    * returns the y position at the top of the text line; scaling does not change it.
    *
    * @return the y position of this element.
    * @since 1.8.4
    */
    public int getY() {
        return y;
    }

    /**
    * sets both position coordinates without changing the text.
    *
    * @param x the new x position
    * @param y the new y position
    * @return self for chaining.
    * @since 1.0.5
    */
    public Text setPos(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /**
    * replaces the text with a literal string and updates its remembered font-measured
    * width. Any existing styling, hover text, and click actions are discarded.
    *
    * @param text what to draw
    * @return self for chaining.
    * @since 1.0.5
    */
    public Text setText(String text) {
        this.text = net.minecraft.network.chat.Component.literal(text);
        this.width = mc.font.width(text);
        return this;
    }

    /**
    * Replaces the displayed text with the helper's text, preserving its styling and
    * updating the cached font-measured width.
    *
    * @param text what to draw, with its styling
    * @return self for chaining.
    * @since 1.2.7
    */
    public Text setText(TextHelper text) {
        this.text = text.getRaw();
        this.width = mc.font.width(this.text);
        return this;
    }

    /**
    * Returns a helper wrapping the displayed text without copying it; changes made
    * through the helper affect this element's text.
    *
    * @return a helper wrapping what is drawn
    * @since 1.2.7
    */
    public TextHelper getText() {
        return TextHelper.wrap(text);
    }

    /**
    * Returns the cached, unscaled font-measured width. It is updated when the text is
    * set, not when this getter is called; direct assignment to {@code text} leaves it
    * unchanged.
    *
    * @return the width of the text.
    * @since 1.0.5
    */
    public int getWidth() {
        return this.width;
    }

    /**
    * Returns the font's unscaled line height, independent of this element's text.
    *
    * @return the height of this text.
    * @since 1.8.4
    */
    public int getHeight() {
        return mc.font.lineHeight;
    }

    /**
    * Sets whether the game's font draws a drop shadow behind the glyphs.
    *
    * @param shadow whether the text should be rendered with a shadow
    * @return self for chaining.
    * @since 1.8.4
    */
    public Text setShadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    /**
    * Returns whether the game's font draws a drop shadow behind this text.
    *
    * @return {@code true} if this text element is rendered with a shadow, {@code false}
    * otherwise.
    * @since 1.8.4
    */
    public boolean hasShadow() {
        return shadow;
    }

    /**
    * Sets this text's scale without remeasuring its cached width. Zero is rejected;
    * negative values are accepted, unlike in the builder.
    *
    * @param scale the scale to apply
    * @return self for chaining.
    * @throws IllegalArgumentException if the scale is zero
    * @since 1.0.5
    */
    public Text setScale(double scale) throws IllegalArgumentException {
        if (scale == 0) {
            throw new IllegalArgumentException("Scale can't be 0");
        }
        this.scale = scale;
        return this;
    }

    /**
    * Returns this text's scale.
    *
    * @return the scale of this text.
    * @since 1.8.4
    */
    public double getScale() {
        return scale;
    }

    /**
    * Sets this text's rotation in degrees, wrapping the stored value into the range
    * {@code -180} to {@code 180}.
    *
    * @param rotation the angle in degrees
    * @return self for chaining.
    * @since 1.0.5
    */
    public Text setRotation(double rotation) {
        this.rotation = Mth.wrapDegrees((float) rotation);
        return this;
    }

    /**
    * Returns this text's rotation in degrees, wrapped into the range {@code -180} to
    * {@code 180} by its constructor and setter; the builder retains its unwrapped value.
    *
    * @return the rotation of this text.
    * @since 1.8.4
    */
    public float getRotation() {
        return rotation;
    }

    /**
    * Sets whether rotation is about the text's center rather than its positioned
    * top-left corner. Directly constructed text defaults to {@code false}; builder-created
    * text defaults to {@code true}.
    *
    * @param rotateCenter whether this text should be rotated around its center
    * @return self for chaining.
    * @since 1.8.4
    */
    public Text setRotateCenter(boolean rotateCenter) {
        this.rotateCenter = rotateCenter;
        return this;
    }

    /**
    * Whether rotation is about the text's center rather than its positioned top-left
    * corner.
    *
    * @return {@code true} if this text should be rotated around its center, {@code false}
    * otherwise.
    * @since 1.8.4
    */
    public boolean isRotatingCenter() {
        return rotateCenter;
    }

    /**
    * Sets the packed tint for the text. A zero alpha byte is made opaque when the RGB
    * value is nonzero; pure black remains transparent.
    *
    * @param color the new color for this text element
    * @return self for chaining.
    * @since 1.8.4
    */
    public Text setColor(int color) {
        this.color = ColorMath.fixAlpha(color);
        return this;
    }

    /**
    * Returns the packed tint, with alpha in the top byte; this is separate from colors
    * carried by the text's styling.
    *
    * @return the color of this text.
    * @since 1.8.4
    */
    public int getColor() {
        return this.color;
    }

    /**
    * Sets this text's ordering value among elements on its overlay; lower values draw
    * first, and equal values retain insertion order.
    *
    * @param zIndex the new z-index for this text element
    * @return self for chaining.
    * @since 1.8.4
    */
    public Text setZIndex(int zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    /**
    * Returns this text's ordering value among elements on its overlay; lower values draw
    * first.
    *
    * @return the z-index of this element
    */
    @Override
    public int getZIndex() {
        return zIndex;
    }

    /**
    * Draws this text with its position, scale, rotation, tint, and shadow. The overlay
    * or screen calls this method; the mouse coordinates and frame delta are unused.
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

        setupMatrix(matrices, x, y, (float) scale, rotation, getWidth(), getHeight(), rotateCenter);
        //? if >=26.1 {
        /*drawContext.text(mc.font, text, x, y, color, shadow);
        *///?} else {
        drawContext.drawString(mc.font, text, x, y, color, shadow);
        //?}

        //? if >1.21.5 {
        matrices.popMatrix();
        //?} else {
        /*matrices.popPose();
        *///?}
    }

    @Override
    @DocletIgnore
    public void render3D(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        //? if >1.21.5 {
        Matrix3x2fStack matrices = drawContext.pose();
        matrices.pushMatrix();
        //?} else {
        /*PoseStack matrices = drawContext.pose();
        matrices.pushPose();
        *///?}

        setupMatrix(matrices, x, y, (float) scale, rotation, getWidth(), getHeight(), rotateCenter);
        //? if >1.21.5 {
        Matrix4f matrix4f;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            FloatBuffer buf = memoryStack.mallocFloat(16);
            matrices.get4x4(buf);
            buf.rewind();
            matrix4f = new Matrix4f().set(buf);
        }
        //?}
        //? if >=26.2 {
        /*drawContext.text(mc.font, text, (int) x, (int) y, color, shadow);
        *///?} else {
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(new ByteBufferBuilder(1536));
        mc.font.drawInBatch(
            text,
            x,
            y,
            color,
            shadow,
            //? if >1.21.5 {
            matrix4f,
            //?} else {
            /*matrices.last().pose(),
            *///?}
            buffer,
            Font.DisplayMode.NORMAL,
            0,
            0xFFF000F0
        );
        buffer.endBatch();
        //?}

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
        poseStack.translate(x, y, 0);
        poseStack.scale((float) scale, (float) scale, 1);
        if (rotateCenter) {
            poseStack.translate(getWidth() / 2d, getHeight() / 2d, 0);
        }
        PoseCompat.rotate(poseStack, new org.joml.Quaternionf().rotateLocalZ((float) Math.toRadians(rotation)));
        if (rotateCenter) {
            poseStack.translate(-getWidth() / 2d, -getHeight() / 2d, 0);
        }
        poseStack.translate(-x, -y, 0);

        // The font renderer applies the packed light via its lightmap; keep the
        // requested text color unchanged so lighting is not applied twice.
        //? if >=26.2 {
        /*sink.drawText(poseStack, text.getVisualOrderText(), (float) x, (float) y, color, shadow,
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, light);
        *///?} else {
        mc.font.drawInBatch(text, (float) x, (float) y, color, shadow, poseStack.last().pose(), sink.getSource(),
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, 0, light);
        //?}

        poseStack.popPose();
    }

    /**
    * Sets the overlay used for parent-relative dimensions. A {@code null} parent makes
    * those dimensions come from the window; builders bind text to their originating overlay.
    *
    * @param parent the overlay this element is on, or {@code null} for the window
    * @return self for chaining.
    */
    public Text setParent(IDraw2D<?> parent) {
        this.parent = parent;
        return this;
    }

    /**
    * Returns the cached font-measured width multiplied by scale and truncated toward
    * zero. Alignment uses this scaled width.
    *
    * @return the scaled width of this element
    */
    @Override
    public int getScaledWidth() {
        return (int) (scale * getWidth());
    }

    /**
    * Returns the bound overlay's width, or the window's GUI-scaled width when unbound.
    *
    * @return the width of the parent
    */
    @Override
    public int getParentWidth() {
        return parent != null ? parent.getWidth() : mc.getWindow().getGuiScaledWidth();
    }

    /**
    * Returns the font line height multiplied by the scale and truncated toward zero.
    *
    * @return the scaled height of this element
    */
    @Override
    public int getScaledHeight() {
        return (int) (scale * mc.font.lineHeight);
    }

    /**
    * Returns the bound overlay's height, or the window's GUI-scaled height when unbound.
    *
    * @return the height of the parent
    */
    @Override
    public int getParentHeight() {
        return parent != null ? parent.getHeight() : mc.getWindow().getGuiScaledHeight();
    }

    /**
    * Returns this text's x position; scaling does not change its left edge.
    *
    * @return the position of the scaled element's left side
    */
    @Override
    public int getScaledLeft() {
        return x;
    }

    /**
    * Returns this text's y position; scaling does not change its top edge.
    *
    * @return the position of the scaled element's top side
    */
    @Override
    public int getScaledTop() {
        return y;
    }

    /**
    * Sets this text's position; the text is not remeasured or resized.
    *
    * @param x the new x position
    * @param y the new y position
    * @return self for chaining.
    */
    @Override
    public Text moveTo(int x, int y) {
        return setPos(x, y);
    }

    /**
    * Builds a text element for an overlay. Text can be supplied as a literal string,
    * a {@link TextHelper} retaining its styling, or a {@link TextBuilder} whose result
    * is used. Defaults are position {@code (0, 0)}, empty text, opaque white, scale
    * {@code 1}, rotation {@code 0}, and no shadow; builder-created text rotates about
    * its center by default.
    * <pre>
    * const draw = Hud.createDraw2D();
    * draw.textBuilder().text("hello").pos(10, 10).color(0xFFFFFFFF).buildAndAdd();
    * draw.register();
    * </pre>
    *
    * @author Etheradon
    * @since 1.8.4
    */
    @DocletCategory("Rendering/Graphics")
    public static class Builder extends RenderElementBuilder<Text> implements Alignable<Builder> {
        private int x = 0;
        private int y = 0;
        private net.minecraft.network.chat.Component text = net.minecraft.network.chat.Component.empty();
        private int color = 0xFFFFFFFF;
        private double scale = 1;
        private float rotation = 0;
        private boolean rotateCenter = true;
        private boolean shadow = false;
        private int zIndex = 0;

        /**
        * Creates a text builder bound to the given overlay.
        *
        * @param draw2D the overlay the text will be added to
        */
        public Builder(IDraw2D<?> draw2D) {
            super(draw2D);
        }

        /**
        * Sets the text from a helper, retaining its styling. A {@code null} value is
        * ignored and leaves the current text unchanged.
        *
        * @param text the content of the text element
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder text(TextHelper text) {
            if (text != null) {
                this.text = text.getRaw();
            }
            return this;
        }

        /**
        * Builds the supplied text builder immediately and uses its result. A
        * {@code null} value is ignored and leaves the current text unchanged.
        *
        * @param text the content of the text element
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder text(TextBuilder text) {
            if (text != null) {
                this.text = text.build().getRaw();
            }
            return this;
        }

        /**
        * Sets the text to a literal string. A {@code null} value is ignored and leaves
        * the current text unchanged.
        *
        * @param text the content of the text element
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder text(String text) {
            if (text != null) {
                this.text = net.minecraft.network.chat.Component.literal(text);
            }
            return this;
        }

        /**
        * Returns a helper for the builder's text. The helper wraps a copy, so changes
        * to it do not affect this builder.
        *
        * @return the content of the text element.
        * @since 1.8.4
        */
        public TextHelper getText() {
            return TextHelper.wrap(text.copy());
        }

        /**
        * Sets the x-coordinate without changing the y-coordinate.
        *
        * @param x the x position of the text element
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder x(int x) {
            this.x = x;
            return this;
        }

        /**
        * Returns the builder's x-coordinate, initially zero.
        *
        * @return the x position of the text element.
        * @since 1.8.4
        */
        public int getX() {
            return x;
        }

        /**
        * Sets the y-coordinate without changing the x-coordinate.
        *
        * @param y the y position of the text element
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder y(int y) {
            this.y = y;
            return this;
        }

        /**
        * Returns the builder's y-coordinate, initially zero.
        *
        * @return the y position of the text element.
        * @since 1.8.4
        */
        public int getY() {
            return y;
        }

        /**
        * Sets both coordinates without changing the text.
        *
        * @param x the x position of the text element
        * @param y the y position of the text element
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        /**
        * Measures and returns the builder's text width in unscaled font units.
        *
        * @return the width of the string.
        * @since 1.8.4
        */
        public int getWidth() {
            return mc.font.width(text);
        }

        /**
        * Returns the font's line height in unscaled units.
        *
        * @return the height of the string.
        * @since 1.8.4
        */
        public int getHeight() {
            return mc.font.lineHeight;
        }

        /**
        * Sets the text tint as a packed ARGB value. The builder stores the value as
        * given; when built, a value with no alpha byte is made opaque.
        *
        * @param color the color of the text element
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder color(int color) {
            this.color = color;
            return this;
        }

        /**
        * Sets an opaque tint from red, green, and blue components in the range 0–255.
        *
        * @param r the red component of the color
        * @param g the green component of the color
        * @param b the blue component of the color
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder color(int r, int g, int b) {
            return color(r, g, b, 255);
        }

        /**
        * Sets the tint from red, green, blue, and alpha components, packed as ARGB.
        *
        * @param r the red component of the color
        * @param g the green component of the color
        * @param b the blue component of the color
        * @param a the alpha component of the color
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder color(int r, int g, int b, int a) {
            this.color = (a << 24) | (r << 16) | (g << 8) | b;
            return this;
        }

        /**
        * Returns the builder's packed tint. The three-component {@code color} overload
        * supplies alpha 255; a packed value without an alpha byte is fixed up when built.
        *
        * @return the color of the text element.
        * @since 1.8.4
        */
        public int getColor() {
            return color;
        }

        /**
        * Sets the text scale. The builder rejects zero and negative values; changing the
        * scale does not remeasure the text.
        *
        * @param scale the scale of the text element
        * @return self for chaining.
        * @throws IllegalArgumentException if the scale is zero or negative
        * @since 1.8.4
        */
        public Builder scale(double scale) {
            if (scale <= 0) {
                throw new IllegalArgumentException("Scale must be greater than 0");
            }
            this.scale = scale;
            return this;
        }

        /**
        * Returns the scale configured for this builder; it defaults to {@code 1}.
        *
        * @return the scale of the text element.
        * @since 1.8.4
        */
        public double getScale() {
            return scale;
        }

        /**
        * Sets the rotation in degrees. The builder stores the angle as a float without
        * wrapping it; the created text wraps it to a single turn.
        *
        * @param rotation the rotation (clockwise) of the text element in degrees
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder rotation(double rotation) {
            this.rotation = (float) rotation;
            return this;
        }

        /**
        * Returns the builder's rotation in degrees before the created text wraps it;
        * defaults to {@code 0}.
        *
        * @return the rotation (clockwise) of the text element in degrees.
        * @since 1.8.4
        */
        public float getRotation() {
            return rotation;
        }

        /**
        * Chooses whether rotation is about the text's center rather than its position.
        *
        * @param rotateCenter whether this text should be rotated around its center
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder rotateCenter(boolean rotateCenter) {
            this.rotateCenter = rotateCenter;
            return this;
        }

        /**
        * Returns whether created text rotates about its center; defaults to {@code true}.
        *
        * @return {@code true} if this text should be rotated around its center,
        * {@code false} otherwise.
        * @since 1.8.4
        */
        public boolean isRotatingCenter() {
            return rotateCenter;
        }

        /**
        * Chooses whether the game's font draws a drop shadow behind the glyphs.
        *
        * @param shadow whether the text should have a shadow or not
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder shadow(boolean shadow) {
            this.shadow = shadow;
            return this;
        }

        /**
        * Returns whether created text has the game's font shadow; defaults to {@code false}.
        *
        * @return {@code true} if the text element has a shadow, {@code false} otherwise.
        * @since 1.8.4
        */
        public boolean hasShadow() {
            return shadow;
        }

        /**
        * Sets the z-index used to order this text relative to other overlay elements.
        *
        * @param zIndex the z-index of the text element
        * @return self for chaining.
        */
        public Builder zIndex(int zIndex) {
            this.zIndex = zIndex;
            return this;
        }

        /**
        * Returns the z-index configured for elements built by this builder; defaults to zero.
        *
        * @return the z-index of the text element.
        * @since 1.8.4
        */
        public int getZIndex() {
            return zIndex;
        }

        /**
        * Creates a text element from this builder's settings and binds it to the originating overlay.
        *
        * @return the new text element.
        */
        @Override
        public Text createElement() {
            return new Text(TextHelper.wrap(text), x, y, color, zIndex, shadow, scale, rotation).setRotateCenter(
                    rotateCenter).setParent(parent);
        }

        /**
        * Returns the text width multiplied by scale and truncated to an integer; alignment uses this width.
        *
        * @return the scaled width
        */
        @Override
        public int getScaledWidth() {
            return (int) (scale * getWidth());
        }

        /**
        * Returns the width of the overlay this builder was created from.
        *
        * @return the width of the parent
        */
        @Override
        public int getParentWidth() {
            return parent.getWidth();
        }

        /**
        * Returns the text height multiplied by scale and truncated to an integer.
        *
        * @return the scaled height
        */
        @Override
        public int getScaledHeight() {
            return (int) (scale * getHeight());
        }

        /**
        * Returns the height of the overlay this builder was created from.
        *
        * @return the height of the parent
        */
        @Override
        public int getParentHeight() {
            return parent.getHeight();
        }

        /**
        * Returns the text's x position, unchanged by scale.
        *
        * @return the text's x position, unchanged by scale
        */
        @Override
        public int getScaledLeft() {
            return x;
        }

        /**
        * Returns the text's y position, unchanged by scale.
        *
        * @return the text's y position, unchanged by scale
        */
        @Override
        public int getScaledTop() {
            return y;
        }

        /**
        * Moves the text to the given position without changing its size.
        *
        * @param x the new x position
        * @param y the new y position
        * @return self for chaining.
        */
        @Override
        public Builder moveTo(int x, int y) {
            return pos(x, y);
        }

    }

}
