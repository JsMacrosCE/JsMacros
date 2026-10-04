package com.jsmacrosce.jsmacros.client.api.classes.render.components;

import com.jsmacrosce.doclet.DocletCategory;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.client.api.classes.TextBuilder;
import com.jsmacrosce.jsmacros.client.api.classes.render.IDraw2D;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.util.ColorUtil;

import java.nio.FloatBuffer;

 /**
 * a piece of text at a position, with a colour, a shadow and its own transform.
 * <p>
 * The text itself can be a plain string or anything {@link TextHelper} can carry, which
 * is the way to get styling, hover text and click actions into a line. The width is
 * measured with the game's own font and is remembered rather than re-measured, so a
 * change of text is what refreshes it.
 * <p>
 * That remembering is worth knowing about. The text is a public field, so writing to it
 * directly changes what is drawn without changing the remembered width, and the width
 * then reports the old text's. {@code setText} is the route that keeps the two in step.
 * <p>
 * There is a scale here, unlike a rectangle or a line, so the width and height the align
 * methods measure are the scaled ones. Negative scales are allowed on the element itself
 * and refused by its builder, so the two can disagree about which scales are legal.
 * <p>
 * A text element on a <em>screen</em> is also the one the screen makes hoverable and
 * clickable when the cursor is inside it, which is how a styled line with a click action
 * works in a menu. That does not happen on a plain overlay, which has no mouse of its
 * own and passes zero instead.
 * example:
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
    * the overlay this text was added to, and {@code null} when it has not been put on
    * one.
    * <p>
    * This is what the parent width and height are read from, and they fall back to the
    * window when it is null.
    */
    @Nullable
    public IDraw2D<?> parent;

    /**
    * what is drawn, as the game's own text type.
    * <p>
    * A public field, so it can be written directly. Doing that draws the new text while
    * leaving the remembered {@code width} describing the old one, so the text's own
    * {@code getWidth} and the size the align methods measure will both be out of date
    * until {@code setText} is used instead.
    */
    public net.minecraft.network.chat.Component text;
    /**
    * the scale applied to this text, and to the size the align methods measure.
    * <p>
    * One is what a text element comes with from a builder. Made through the
    * constructor there is no default at all, since the scale is one of its arguments.
    * A negative scale is accepted here and refused by the builder.
    */
    public double scale;
    /**
    * the rotation in degrees, wrapped into a single turn.
    * <p>
    * Both the constructor and {@link #setRotation(double)} fold the angle down, so this
    * is between -180 and 180. The builder does not fold it, so a builder and the text
    * it builds can read differently.
    */
    public float rotation;
    /**
    * whether the rotation is about the middle of this text or about the position it is
    * given by.
    * <p>
    * False on text made directly and true on text from a builder, so the two turn
    * differently at the same angle.
    */
    public boolean rotateCenter;
    /**
    * the x position of this text, which is where the line starts.
    * <p>
    * The left of the line as it is written rather than the left of its box, which is
    * the same thing here.
    */
    public int x;
    /**
    * the y position of this text, which is the top of the line.
    * <p>
    * The counterpart of {@code x}, and read the same way.
    */
    public int y;
    /**
    * the colour, packed with the alpha in the top byte.
    * <p>
    * A colour with no alpha of its own is made opaque unless it was pure black, so
    * {@code 0xFF0000} is stored as {@code 0xFFFF0000}.
    */
    public int color;
    /**
    * the width the font measured for the current text, remembered rather than re-measured.
    * <p>
    * Set when the element is made and again whenever the text is set through
    * {@code setText}. Nothing refreshes it, so writing the {@code text} field directly
    * leaves this describing the text that was there before.
    */
    public int width;
    /**
    * whether the text is drawn with the game's own drop shadow behind the glyphs.
    * <p>
    * False unless a builder is told otherwise, and it is a real shadow drawn by the
    * font rather than a second copy of the text.
    */
    public boolean shadow;
    /**
    * the number this text is ordered against the other elements on its overlay, lower
    * drawn first.
    */
    public int zIndex;

    /**
    * makes a text element from a plain string.
    * <p>
    * The string is turned into the game's own text type, so it is a literal: there is
    * no styling, no hover text and no click action in it. Use
    * {@link TextHelper} or the builder for those. The rotation is folded into a single
    * turn and the width is measured with the game's font straight away.
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
    * makes a text element from a helper, which keeps whatever the helper carries.
    * <p>
    * This is the form that keeps styling, hover text and click actions, because it
    * takes the helper's own text rather than making a literal out of a string. The
    * width is measured with the game's font once, here, and is remembered from then on.
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
    * moves this text along the x axis, and leaves the y axis alone.
    * <p>
    * The text itself is not changed, so the remembered width stays right and the line
    * simply starts somewhere else.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("moved", 10, 10, 0xFFFFFFFF, true);
    * label.setX(40);
    * draw.register();
    * </pre>
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
    * the x position of this text, which is where the line starts.
    * <p>
    * Unchanged by the scale, which makes the glyphs bigger rather than moving them.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("here", 40, 20, 0xFFFFFFFF, true);
    * draw.register();
    * Chat.log(`at x ${label.getX()}, y ${label.getY()}`);
    * </pre>
    *
    * @return the x position of this element.
    * @since 1.8.4
    */
    public int getX() {
        return x;
    }

    /**
    * moves this text along the y axis, and leaves the x axis alone.
    * <p>
    * The counterpart of {@code setX}, and the text is not re-measured.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("moved", 10, 10, 0xFFFFFFFF, true);
    * label.setY(40);
    * draw.register();
    * </pre>
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
    * the y position of this text, which is the top of the line.
    * <p>
    * The counterpart of {@code getX}, and unchanged by the scale as well.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("here", 40, 20, 0xFFFFFFFF, true);
    * draw.register();
    * Chat.log(`at x ${label.getX()}, y ${label.getY()}`);
    * </pre>
    *
    * @return the y position of this element.
    * @since 1.8.4
    */
    public int getY() {
        return y;
    }

    /**
    * puts this text at a position, and leaves the text itself alone.
    * <p>
    * Both coordinates are set and nothing is re-measured, so this is where the line
    * starts rather than anything about the line itself.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("moved", 0, 0, 0xFFFFFFFF, true);
    * label.setPos(40, 20);
    * draw.register();
    * </pre>
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
    * replaces what is drawn with a plain string, and re-measures the width.
    * <p>
    * The string becomes a literal, so anything already styled is gone. The width is
    * measured again here, which is what keeps {@code getWidth} and the align methods
    * in step with what is actually drawn.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("short", 10, 10, 0xFFFFFFFF, true);
    * label.setText("a much longer line of text");
    * draw.register();
    * Chat.log(`now ${label.getWidth()} wide`);
    * </pre>
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
    * replaces what is drawn with a helper's text, keeping its styling, and re-measures
    * the width.
    * <p>
    * The counterpart of the string form, and the one to use for hover text and click
    * actions. As there, the width is measured again here rather than left at whatever
    * the old text needed.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("plain", 10, 10, 0xFFFFFFFF, true);
    * label.setText(Chat.createTextHelperFromString("now styled"));
    * draw.register();
    * Chat.log(`now ${label.getWidth()} wide`);
    * </pre>
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
    * what is drawn, as a helper, so it can be read and restyled.
    * <p>
    * This wraps the text as it stands rather than making a copy, so a helper taken from
    * here and changed will change what is drawn. That is different from the builder's
    * own getter, which copies.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
    * draw.register();
    * Chat.log(`holds ${label.getText().getRaw().getString()}`);
    * </pre>
    *
    * @return a helper wrapping what is drawn
    * @since 1.2.7
    */
    public TextHelper getText() {
        return TextHelper.wrap(text);
    }

    /**
    * the width the font measured for this text, as remembered when it was set.
    * <p>
    * Not re-measured here, so this is a number read back rather than a measurement
    * taken now, and it is the unscaled width. The scaled one, which is what the align
    * methods move by, is {@code getScaledWidth}.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
    * draw.register();
    * Chat.log(`${label.getWidth()} wide, ${label.getScaledWidth()} scaled`);
    * </pre>
    *
    * @return the width of the text.
    * @since 1.0.5
    */
    public int getWidth() {
        return this.width;
    }

    /**
    * the height of one line of text, which is the font's line height.
    * <p>
    * The same number for every line of text in the game, so it does not depend on what
    * is written and it is not remembered per element. It is the unscaled height; the
    * scaled one is {@code getScaledHeight}.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
    * draw.register();
    * Chat.log(`one line is ${label.getHeight()} tall`);
    * </pre>
    *
    * @return the height of this text.
    * @since 1.8.4
    */
    public int getHeight() {
        return mc.font.lineHeight;
    }

    /**
    * chooses whether the game's own drop shadow is drawn behind the glyphs.
    * <p>
    * This is the font's shadow rather than a second copy of the text, and it is off
    * unless something turns it on, which means it is a different thing from a styled
    * background in the text itself.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
    * label.setShadow(false);
    * draw.register();
    * Chat.log(`shadow: ${label.hasShadow()}`);
    * </pre>
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
    * whether the game's drop shadow is drawn behind this text.
    * <p>
    * A builder starts with this off, and the {@code add} methods take it as an argument.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
    * draw.register();
    * Chat.log(`shadow: ${label.hasShadow()}`);
    * </pre>
    *
    * @return {@code true} if this text element is rendered with a shadow, {@code false}
    * otherwise.
    * @since 1.8.4
    */
    public boolean hasShadow() {
        return shadow;
    }

    /**
    * scales this text.
    * <p>
    * Only zero is refused. A negative scale is accepted here and left in, which turns
    * the text about as well as shrinking it, whereas the builder refuses anything at or
    * below zero, so the same number can be legal on one and not the other. The width
    * is not re-measured, since a scale is a transform rather than a change of text.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
    * label.setScale(2);
    * draw.register();
    * Chat.log(`scale ${label.getScale()}, drawn ${label.getScaledWidth()} wide`);
    * </pre>
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
    * the scale on this text, which is one unless something has changed it.
    * <p>
    * A double rather than a float, which is also what an item icon's scale is and is
    * not what a nested overlay's is. It is the number the scaled size is worked out
    * from, and it is the only size in this element that is not a whole number of
    * pixels before the scale.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
    * label.setScale(1.5);
    * draw.register();
    * Chat.log(`scale ${label.getScale()}`);
    * </pre>
    *
    * @return the scale of this text.
    * @since 1.8.4
    */
    public double getScale() {
        return scale;
    }

    /**
    * turns this text by an angle in degrees.
    * <p>
    * The angle is folded into a single turn, so 450 reads back as 90 and -270 reads back
    * as 90 too. The turn is about the middle of the text when {@code rotateCenter} is
    * on and about its position when it is not.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 40, 20, 0xFFFFFFFF, true);
    * label.setRotateCenter(true).setRotation(-270);
    * draw.register();
    * Chat.log(`reads back as ${label.getRotation()}`);
    * </pre>
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
    * the rotation on this text in degrees, folded into a single turn.
    * <p>
    * Between -180 and 180, because both the constructor and the setter fold the angle
    * down. The builder does not, so a builder and the text it builds can read
    * differently for the same number.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 40, 20, 0xFFFFFFFF, true);
    * label.setRotation(450);
    * draw.register();
    * Chat.log(`reads back as ${label.getRotation()}`);
    * </pre>
    *
    * @return the rotation of this text.
    * @since 1.8.4
    */
    public float getRotation() {
        return rotation;
    }

    /**
    * chooses whether the rotation is about the middle of the text or about its
    * position.
    * <p>
    * Text made directly turns about its position and text from a builder turns about
    * the middle, so this is what makes the two agree. With a single line the middle is
    * half the scaled width along and half the scaled line height down, because the
    * scale goes on before the turn.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 40, 20, 0xFFFFFFFF, true);
    * label.setRotateCenter(true).setRotation(45);
    * draw.register();
    * </pre>
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
    * whether the rotation on this text is about its middle.
    * <p>
    * False on text made directly and true on text from a builder, since only the
    * builder sets it.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 40, 20, 0xFFFFFFFF, true);
    * draw.register();
    * Chat.log(`turns about the middle: ${label.isRotatingCenter()}`);
    * </pre>
    *
    * @return {@code true} if this text should be rotated around its center, {@code false}
    * otherwise.
    * @since 1.8.4
    */
    public boolean isRotatingCenter() {
        return rotateCenter;
    }

    /**
    * sets the colour, alpha and all, from one packed number.
    * <p>
    * This tints the whole line. Any colour in the text's own styling is applied on top
    * of it, so a styled line keeps its own colours and this only decides the rest. A
    * colour with no alpha of its own is made opaque unless it was pure black.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
    * label.setColor(0xFF0000);
    * draw.register();
    * Chat.log(`0x${label.getColor().toString(16)}`);
    * </pre>
    *
    * @param color the new color for this text element
    * @return self for chaining.
    * @since 1.8.4
    */
    public Text setColor(int color) {
        this.color = ColorUtil.fixAlpha(color);
        return this;
    }

    /**
    * the colour, packed with the alpha in the top byte.
    * <p>
    * The tint for the whole line, which is a different thing from any colour in the
    * text's own styling: a styled line can be a different colour on the screen and
    * still report the tint here.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 10, 10, 0xFFFF0000, true);
    * draw.register();
    * Chat.log(`tint 0x${label.getColor().toString(16)}`);
    * </pre>
    *
    * @return the color of this text.
    * @since 1.8.4
    */
    public int getColor() {
        return this.color;
    }

    /**
    * sets the number this text is ordered against the other elements on its overlay,
    * lower drawn first.
    * <p>
    * Two lines of text drawn over the same place are ordered by this and by nothing
    * else, and a tie keeps the order they were added in.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const under = draw.addText("under", 10, 10, 0xFFFFFFFF, true);
    * const over = draw.addText("over", 10, 10, 0xFFFFFFFF, true);
    * under.setZIndex(2);
    * over.setZIndex(1);
    * draw.register();
    * </pre>
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
    * the number this text is ordered against the other elements on its overlay.
    * <p>
    * Zero is what text comes with, and the overlay sorts by this lowest first.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
    * label.setZIndex(3);
    * draw.register();
    * Chat.log(`z-index ${label.getZIndex()}`);
    * </pre>
    *
    * @return the z-index of this element
    */
    @Override
    public int getZIndex() {
        return zIndex;
    }

    /**
    * draws this text, and is called by the overlay rather than by a script.
    * <p>
    * The element's own transform is put on first, so the position, the scale and the
    * turn are the position, the size and the angle of the glyphs as they land rather
    * than of the box the text was measured into. The colour and the shadow go to the
    * game's own text drawing, which is also what applies any styling in the text.
    * <p>
    * The mouse arguments are not used. On a screen they are the real mouse and on a
    * plain overlay they are zero, and a screen is also what makes a text element
    * hoverable and clickable when the cursor is inside it.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * draw.addText("drawn by the overlay", 10, 10, 0xFFFFFFFF, true);
    * // the overlay calls render on it; a script does not
    * draw.register();
    * </pre>
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

        //? if >1.21.5 {
        matrices.popMatrix();
         //?} else {
        /*matrices.popPose();
        *///?}
    }

    @Override
    @DocletIgnore
    public void render3D(PoseStack matrixStack, MultiBufferSource consumers, int light, boolean seeThrough, float delta) {
        matrixStack.pushPose();
        matrixStack.translate(x, y, 0);
        matrixStack.scale((float) scale, (float) scale, 1);
        if (rotateCenter) {
            matrixStack.translate(getWidth() / 2d, getHeight() / 2d, 0);
        }
        matrixStack.mulPose(new org.joml.Quaternionf().rotateLocalZ((float) Math.toRadians(rotation)));
        if (rotateCenter) {
            matrixStack.translate(-getWidth() / 2d, -getHeight() / 2d, 0);
        }
        matrixStack.translate(-x, -y, 0);

        // The font renderer applies the packed light via its lightmap; keep the
        // requested text color unchanged so lighting is not applied twice.
        mc.font.drawInBatch(text, (float) x, (float) y, color, shadow, matrixStack.last().pose(), consumers,
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, 0, light);

        matrixStack.popPose();
    }

    /**
    * binds this text to the overlay it belongs to, which is what the parent width and
    * height are then read from.
    * <p>
    * The element builders call this as they build, so text that has been built is
    * already bound. Binding is what makes the parent-relative align methods measure
    * against that overlay rather than against the window, and {@code null} puts it back
    * to the window.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.textBuilder().text("hello").pos(10, 10).build();
    * // a built text is already bound, and can be bound again by hand
    * label.setParent(draw);
    * draw.reAddElement(label);
    * draw.register();
    * </pre>
    *
    * @param parent the overlay this element is on, or {@code null} for the window
    * @return self for chaining.
    */
    public Text setParent(IDraw2D<?> parent) {
        this.parent = parent;
        return this;
    }

    /**
    * the width of this text as it is drawn, which is the width put through the scale.
    * <p>
    * The result is cut to a whole number of pixels, and the cut is toward zero. A
    * positive scale therefore rounds down, and a negative one rounds up, which is what
    * an element given a negative scale through {@code setScale} gets. This is the
    * number the align methods measure, so text is aligned on the size it appears at
    * rather than the size it was given.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
    * label.setScale(1.5);
    * draw.register();
    * Chat.log(`drawn ${label.getScaledWidth()} wide, out of ${label.getWidth()}`);
    * </pre>
    *
    * @return the scaled width of this element
    */
    @Override
    public int getScaledWidth() {
        return (int) (scale * getWidth());
    }

    /**
    * the width of the overlay this text is on, or the window's width if it has not been
    * put on one.
    * <p>
    * This is what the parent-relative align methods measure against, so text that has
    * not been added still aligns against the screen.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
    * draw.register();
    * Chat.log(`measured against ${label.getParentWidth()}`);
    * </pre>
    *
    * @return the width of the parent
    */
    @Override
    public int getParentWidth() {
        return parent != null ? parent.getWidth() : mc.getWindow().getGuiScaledWidth();
    }

    /**
    * the height of this text as it is drawn, which is the font's line height put
    * through the scale.
    * <p>
    * The counterpart of {@code getScaledWidth}, and cut to a whole number of pixels the
    * same way. This is the number the vertical align methods measure.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
    * label.setScale(1.5);
    * draw.register();
    * Chat.log(`drawn ${label.getScaledHeight()} tall, out of ${label.getHeight()}`);
    * </pre>
    *
    * @return the scaled height of this element
    */
    @Override
    public int getScaledHeight() {
        return (int) (scale * mc.font.lineHeight);
    }

    /**
    * the height of the overlay this text is on, or the window's height if it has not
    * been put on one.
    * <p>
    * The counterpart of {@code getParentWidth}, and read the same way.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
    * draw.register();
    * Chat.log(`measured against ${label.getParentHeight()}`);
    * </pre>
    *
    * @return the height of the parent
    */
    @Override
    public int getParentHeight() {
        return parent != null ? parent.getHeight() : mc.getWindow().getGuiScaledHeight();
    }

    /**
    * the x position of this text, which is where the line starts.
    * <p>
    * The same as {@code getX}, because a scale makes the glyphs bigger rather than
    * moving where they start. The width the line actually covers is
    * {@code getScaledWidth}, which is what the align methods add to it.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 40, 20, 0xFFFFFFFF, true);
    * draw.register();
    * Chat.log(`from ${label.getScaledLeft()} to ${label.getScaledRight()}`);
    * </pre>
    *
    * @return the position of the scaled element's left side
    */
    @Override
    public int getScaledLeft() {
        return x;
    }

    /**
    * the y position of this text, which is the top of the line.
    * <p>
    * The counterpart of {@code getScaledLeft}, and the same as {@code getY}.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 40, 20, 0xFFFFFFFF, true);
    * draw.register();
    * Chat.log(`from ${label.getScaledTop()} to ${label.getScaledBottom()}`);
    * </pre>
    *
    * @return the position of the scaled element's top side
    */
    @Override
    public int getScaledTop() {
        return y;
    }

    /**
    * puts this text at a position, which is the same as {@code setPos}.
    * <p>
    * Nothing is re-measured and nothing is resized, so the text keeps the width the
    * font gave it. This is what the align methods call.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const label = draw.addText("hello", 0, 0, 0xFFFFFFFF, true);
    * label.moveTo(40, 20);
    * draw.register();
    * Chat.log(`at ${label.getX()}, ${label.getY()} and still ${label.getWidth()} wide`);
    * </pre>
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
    * the builder for a text element, made by the overlay's own {@code textBuilder}
    * method.
    * <p>
    * It takes the text in three shapes, a plain string, a {@link TextHelper} and a
    * {@link TextBuilder}, and the first two are the ones a script usually reaches for:
    * a string is a literal with no styling, a helper keeps whatever styling it carries.
    * Every method is named after the thing being set and hands the builder back, so a
    * whole element is one chain that ends in {@code build()} or {@code buildAndAdd()}.
    * <p>
    * The position starts at zero, the text starts empty and so draws nothing, the
    * colour starts opaque white, the scale starts at one, the rotation starts at zero
    * and is not folded into a single turn, the shadow starts off, and the rotation is
    * about the middle of the text until {@code rotateCenter} says otherwise. That last
    * one is the opposite of text made directly, so the two turn differently at the
    * same angle.
    * example:
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
        * makes a builder for text on one overlay.
        * <p>
        * A script does not call this directly: the overlay's own {@code textBuilder}
        * method is what fills in the overlay.
        *
        * @param draw2D the overlay the text will be added to
        */
        public Builder(IDraw2D<?> draw2D) {
            super(draw2D);
        }

        /**
        * sets the text from a helper, keeping whatever styling it carries.
        * <p>
        * A helper is the way to get hover text, click actions and formatting into a
        * line. {@code null} is ignored rather than applied, so a missing helper leaves
        * whatever was there before rather than emptying the element.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const styled = Chat.createTextHelperFromString("a green line");
        * draw.textBuilder().text(styled).pos(10, 10).color(0xFFFFFFFF).buildAndAdd();
        * draw.register();
        * </pre>
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
        * sets the text from a text builder, using what it builds.
        * <p>
        * The builder is run here rather than kept, so anything chained onto it has to
        * have been chained already. {@code null} is ignored, the same way as the other
        * two forms.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const built = Chat.createTextBuilder().append("hello ").withColor(0x00FF00);
        * draw.textBuilder().text(built).pos(10, 10).color(0xFFFFFFFF).buildAndAdd();
        * draw.register();
        * </pre>
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
        * sets the text from a plain string, which becomes a literal.
        * <p>
        * No styling, no hover text and no click action: those need the helper form.
        * {@code null} is ignored rather than applied, so a missing string leaves
        * whatever was there before rather than emptying the element.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.textBuilder().text("hello").pos(10, 10).color(0xFFFFFFFF).buildAndAdd();
        * draw.register();
        * </pre>
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
        * what this builder will build the text element with, as a helper.
        * <p>
        * A copy of the text rather than the text itself, so changing the helper this
        * returns does not change what the builder will build. The text element that
        * comes out of it has a getter of its own that does not copy, so the two
        * behave differently.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.textBuilder().text("hello");
        * Chat.log(`will build ${builder.getText().getRaw().getString()}`);
        * </pre>
        *
        * @return the content of the text element.
        * @since 1.8.4
        */
        public TextHelper getText() {
            return TextHelper.wrap(text.copy());
        }

        /**
        * puts the text at an x position, and leaves the y position alone.
        * <p>
        * Zero is where an element starts, and the text is not re-measured by moving it.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.textBuilder().text("hello").x(40).y(20).color(0xFFFFFFFF).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the x position this builder will build the text element at.
        * <p>
        * Zero until an {@code x} or a {@code pos} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.textBuilder().x(40);
        * Chat.log(`will be built at x ${builder.getX()}`);
        * </pre>
        *
        * @return the x position of the text element.
        * @since 1.8.4
        */
        public int getX() {
            return x;
        }

        /**
        * puts the text at a y position, and leaves the x position alone.
        * <p>
        * The counterpart of {@code x}, and the text is not re-measured by moving it.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.textBuilder().text("hello").y(20).color(0xFFFFFFFF).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the y position this builder will build the text element at.
        * <p>
        * Zero until a {@code y} or a {@code pos} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.textBuilder().y(20);
        * Chat.log(`will be built at y ${builder.getY()}`);
        * </pre>
        *
        * @return the y position of the text element.
        * @since 1.8.4
        */
        public int getY() {
            return y;
        }

        /**
        * puts the text at a position, and leaves the text itself alone.
        * <p>
        * Both coordinates are set and nothing is re-measured, so this is where the line
        * starts rather than anything about the line itself.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.textBuilder().text("hello").pos(10, 10).color(0xFFFFFFFF).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the width the font would measure this builder's text at, measured now.
        * <p>
        * Measured on every call rather than remembered, which is the difference from
        * the text element's own getter. The unscaled width; the scaled one is
        * {@code getScaledWidth}.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.textBuilder().text("hello");
        * Chat.log(`will be built ${builder.getWidth()} wide`);
        * </pre>
        *
        * @return the width of the string.
        * @since 1.8.4
        */
        public int getWidth() {
            return mc.font.width(text);
        }

        /**
        * the height of one line of text, which is the font's line height.
        * <p>
        * The same number for every line and not measured from the text, so it does not
        * change with what is written. The unscaled height; the scaled one is
        * {@code getScaledHeight}.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.textBuilder();
        * Chat.log(`one line is ${builder.getHeight()} tall`);
        * </pre>
        *
        * @return the height of the string.
        * @since 1.8.4
        */
        public int getHeight() {
            return mc.font.lineHeight;
        }

        /**
        * sets the tint for the whole line from one packed number.
        * <p>
        * Any colour in the text's own styling is applied on top of this, so a styled
        * line keeps its own colours and this decides the rest. The number is stored as
        * given here; the text element that comes out of it is made through the
        * constructor, which turns a colour with no alpha of its own into an opaque one.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const label = draw.textBuilder().text("hello").pos(10, 10).color(0xFF0000).buildAndAdd();
        * draw.register();
        * Chat.log(`built at 0x${label.getColor().toString(16)}`);
        * </pre>
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
        * sets the tint from three components, each from 0 to 255, fully opaque.
        * <p>
        * The three are packed with an alpha of 255 in the top byte, unlike the builders
        * for the other elements in this package, which pack the three on their own and
        * leave the alpha to be set separately. Here the line is always opaque.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const label = draw.textBuilder().text("hello").pos(10, 10).color(255, 128, 0).buildAndAdd();
        * draw.register();
        * Chat.log(`built at 0x${label.getColor().toString(16)}`);
        * </pre>
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
        * sets the tint from four components, each from 0 to 255.
        * <p>
        * The four are packed into one number with the alpha in the top byte, so this is
        * the only colour form here that carries its own transparency.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const label = draw.textBuilder().text("hello").pos(10, 10).color(255, 128, 0, 128).buildAndAdd();
        * draw.register();
        * Chat.log(`built at 0x${label.getColor().toString(16)}`);
        * </pre>
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
        * the tint this builder will build the text element with, as it is stored here.
        * <p>
        * The three-component form packs an alpha of 255 in, so unlike the builders for
        * the other elements in this package the number reported here is the one the
        * built element will have, apart from the fix-up the constructor applies to a
        * colour with no alpha.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.textBuilder().color(255, 0, 0);
        * Chat.log(`will build at 0x${builder.getColor().toString(16)}`);
        * </pre>
        *
        * @return the color of the text element.
        * @since 1.8.4
        */
        public int getColor() {
            return color;
        }

        /**
        * scales the text, and one is what a builder starts at.
        * <p>
        * A scale of zero or less is refused here, which is stricter than the text
        * element's own setter, which refuses only zero and lets a negative scale
        * through. The text is not re-measured, since a scale is a transform rather than
        * a change of text.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const label = draw.textBuilder().text("hello").pos(10, 10).scale(2).buildAndAdd();
        * draw.register();
        * Chat.log(`drawn ${label.getScaledWidth()} wide`);
        * </pre>
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
        * the scale this builder will build the text element at.
        * <p>
        * One until a {@code scale} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.textBuilder().scale(1.5);
        * Chat.log(`will be built at ${builder.getScale()} times the size`);
        * </pre>
        *
        * @return the scale of the text element.
        * @since 1.8.4
        */
        public double getScale() {
            return scale;
        }

        /**
        * turns the text by an angle in degrees.
        * <p>
        * Stored as given rather than folded into a single turn, so this reads back as
        * 450. The text element that comes out of it is made through the constructor,
        * which does fold it, so text built at 450 comes out turned 90 even though this
        * builder reports 450.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.textBuilder().text("hello").pos(40, 20).rotation(45).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the rotation this builder will build the text element at, in degrees.
        * <p>
        * Zero until a {@code rotation} call says otherwise, and not folded into a
        * single turn even though the text that gets built will be.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.textBuilder().rotation(450);
        * Chat.log(`builder says ${builder.getRotation()}`);
        * const label = builder.text("hello").pos(10, 10).buildAndAdd();
        * draw.register();
        * Chat.log(`the text says ${label.getRotation()}`);
        * </pre>
        *
        * @return the rotation (clockwise) of the text element in degrees.
        * @since 1.8.4
        */
        public float getRotation() {
            return rotation;
        }

        /**
        * chooses whether the rotation is about the middle of the text or about its
        * position.
        * <p>
        * True is what a builder starts at, which is the opposite of text made directly,
        * so the two turn differently until one of them is told otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const label = draw.textBuilder().text("hello").pos(40, 20).rotateCenter(false).buildAndAdd();
        * draw.register();
        * Chat.log(`turns about its position: ${!label.isRotatingCenter()}`);
        * </pre>
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
        * whether the text this builder builds will turn about its middle.
        * <p>
        * True until a {@code rotateCenter} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.textBuilder();
        * Chat.log(`builds turning about the middle: ${builder.isRotatingCenter()}`);
        * </pre>
        *
        * @return {@code true} if this text should be rotated around its center,
        * {@code false} otherwise.
        * @since 1.8.4
        */
        public boolean isRotatingCenter() {
            return rotateCenter;
        }

        /**
        * chooses whether the game's own drop shadow is drawn behind the glyphs, and
        * it starts off.
        * <p>
        * The font's shadow rather than a second copy of the text, and separate from any
        * background in the text's own styling.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const label = draw.textBuilder().text("hello").pos(10, 10).shadow(true).buildAndAdd();
        * draw.register();
        * Chat.log(`shadow: ${label.hasShadow()}`);
        * </pre>
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
        * whether the text this builder builds will have the game's drop shadow.
        * <p>
        * False until a {@code shadow} call says otherwise, which is the opposite of the
        * {@code add} methods, which take the shadow as an argument.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.textBuilder();
        * Chat.log(`builds with a shadow: ${builder.hasShadow()}`);
        * </pre>
        *
        * @return {@code true} if the text element has a shadow, {@code false} otherwise.
        * @since 1.8.4
        */
        public boolean hasShadow() {
            return shadow;
        }

        /**
        * sets the number the text will be ordered against the other elements on its
        * overlay, lower drawn first.
        * <p>
        * Zero is what a builder starts at, and a tie keeps the order the elements were
        * added in. Two lines of text drawn over the same place are ordered by this and
        * nothing else.
        * <p>
        * The version badge is missing on this one at the source: every other method on
        * this builder carries a {@code @since} and this does not, and no version has
        * been invented for it here.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.textBuilder().text("on top").pos(10, 10).zIndex(5).buildAndAdd();
        * draw.register();
        * </pre>
        *
        * @param zIndex the z-index of the text element
        * @return self for chaining.
        */
        public Builder zIndex(int zIndex) {
            this.zIndex = zIndex;
            return this;
        }

        /**
        * the z-index this builder will build the text element at.
        * <p>
        * Zero until a {@code zIndex} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.textBuilder().zIndex(5);
        * Chat.log(`will be built at z-index ${builder.getZIndex()}`);
        * </pre>
        *
        * @return the z-index of the text element.
        * @since 1.8.4
        */
        public int getZIndex() {
            return zIndex;
        }

        /**
        * makes the text element out of everything set on this builder, and binds it to
        * the overlay this builder was made from.
        * <p>
        * A new element each time, so the same builder can build more than one. This is
        * what {@code build} and {@code buildAndAdd} call.
        * <p>
        * The element is made through the constructor, which folds the rotation into a
        * single turn and makes a colour with no alpha of its own opaque, so those are
        * two places where the built text can differ from what this builder reports.
        *
        * @return the new text element.
        */
        @Override
        public Text createElement() {
            return new Text(TextHelper.wrap(text), x, y, color, zIndex, shadow, scale, rotation).setRotateCenter(
                    rotateCenter).setParent(parent);
        }

        /**
        * the width the text will be drawn at, which is the width put through the scale.
        * <p>
        * Cut to a whole number of pixels, so a scale that does not land on a whole
        * number rounds down. This is the number the align methods measure.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.textBuilder().text("hello").scale(1.5);
        * Chat.log(`will be drawn ${builder.getScaledWidth()} wide, out of ${builder.getWidth()}`);
        * </pre>
        *
        * @return the scaled width
        */
        @Override
        public int getScaledWidth() {
            return (int) (scale * getWidth());
        }

        /**
        * the width of the overlay this builder was made from, which is what the
        * parent-relative align methods measure against.
        * <p>
        * There is no window fallback here, because a builder always has an overlay.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.textBuilder();
        * Chat.log(`measuring against ${builder.getParentWidth()}`);
        * </pre>
        *
        * @return the width of the parent
        */
        @Override
        public int getParentWidth() {
            return parent.getWidth();
        }

        /**
        * the height the text will be drawn at, which is the line height put through the
        * scale.
        * <p>
        * The counterpart of {@code getScaledWidth}, and cut to a whole number of pixels
        * the same way.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.textBuilder().text("hello").scale(1.5);
        * Chat.log(`will be drawn ${builder.getScaledHeight()} tall, out of ${builder.getHeight()}`);
        * </pre>
        *
        * @return the scaled height
        */
        @Override
        public int getScaledHeight() {
            return (int) (scale * getHeight());
        }

        /**
        * the height of the overlay this builder was made from, which is what the
        * parent-relative align methods measure against.
        * <p>
        * The counterpart of {@code getParentWidth}, and with no window fallback either.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.textBuilder();
        * Chat.log(`measuring against ${builder.getParentHeight()}`);
        * </pre>
        *
        * @return the height of the parent
        */
        @Override
        public int getParentHeight() {
            return parent.getHeight();
        }

        /**
        * the x position the text will be at, which is where the line starts.
        * <p>
        * The same as {@code getX}: a scale makes the glyphs bigger rather than moving
        * where they start. The width they cover is {@code getScaledWidth}.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.textBuilder().text("hello").pos(40, 20);
        * Chat.log(`will be at ${builder.getScaledLeft()}, ${builder.getScaledTop()}`);
        * </pre>
        *
        * @return the position of the scaled element's left side
        */
        @Override
        public int getScaledLeft() {
            return x;
        }

        /**
        * the y position the text will be at, which is the top of the line.
        * <p>
        * The counterpart of {@code getScaledLeft}, and the same as {@code getY}.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.textBuilder().text("hello").pos(40, 20);
        * Chat.log(`will be at ${builder.getScaledLeft()}, ${builder.getScaledTop()}`);
        * </pre>
        *
        * @return the position of the scaled element's top side
        */
        @Override
        public int getScaledTop() {
            return y;
        }

        /**
        * puts the text at a position, which is the same as {@code pos}.
        * <p>
        * Nothing is re-measured and nothing is resized, so the text keeps the width the
        * font gave it. This is what the align methods call.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.textBuilder().text("hello");
        * builder.moveTo(40, 20);
        * builder.color(0xFFFFFFFF).buildAndAdd();
        * draw.register();
        * Chat.log(`will be built at ${builder.getX()}, ${builder.getY()}`);
        * </pre>
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
