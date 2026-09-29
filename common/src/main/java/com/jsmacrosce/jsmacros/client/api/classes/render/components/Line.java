package com.jsmacrosce.jsmacros.client.api.classes.render.components;

import com.jsmacrosce.doclet.DocletCategory;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.client.api.classes.render.IDraw2D;
import com.jsmacrosce.jsmacros.client.util.ColorUtil;

//? if >=1.21.11 {
/*import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.jsmacrosce.jsmacros.client.api.classes.render.components3d.SurfaceRenderTypes;
*///? } else {
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.jsmacrosce.jsmacros.client.api.classes.render.components3d.SurfaceRenderTypes;
//? }

 /**
 * a line from one point to another, with a thickness.
 * <p>
 * The two points have a direction, so this is not the same as a rectangle with a corner
 * at each end: the points say where the line goes rather than where its two edges are,
 * and a rectangle is the wrong shape for a diagonal.
 * <p>
 * There are two sizes here and they are not the same thing. {@code width} is the
 * thickness of the stroke, while {@code getScaledWidth} is how far across the screen the
 * line reaches, which is the distance between the two x positions. A diagonal ten across
 * and ten down with a thickness of one has a {@code getWidth} of one and a
 * {@code getScaledWidth} of ten, and it is worth keeping that straight.
 * <p>
 * The thickness is applied by filling a band along the line, and the band is put down
 * from half the thickness on one side to half on the other. Both of those are cut to
 * whole numbers before the fill, so a thickness of one works out to zero on each side.
 * That is the code as written; whether a one-pixel line is worth reaching for is
 * something to look at in game.
 * <p>
 * The rotation turns the whole line, thickness included, about its middle only when
 * {@code rotateCenter} is on. Off is how a line made directly starts, and then the turn
 * is about the first point. The constructor folds the angle into a single turn while
 * {@code setRotation} does not, so the two can be given the same number and read back
 * differently.
 * example:
 * <pre>
 * const draw = Hud.createDraw2D();
 * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF, 0, 2);
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
    * the overlay this line was added to, and {@code null} when it has not been put on
    * one.
    * <p>
    * This is what the parent width and height are read from, and they fall back to the
    * window when it is null.
    */
    @Nullable
    public IDraw2D<?> parent;
    /**
    * the x position of the first point, which is where the line starts.
    * <p>
    * Which of the two is the start is the caller's choice, and the line draws the same
    * either way round.
    */
    public int x1;
    /**
    * the y position of the first point, which is where the line starts.
    * <p>
    * The counterpart of {@code x1}, and read the same way.
    */
    public int y1;
    /**
    * the x position of the second point, which is where the line ends.
    * <p>
    * The distance between the two x positions is the scaled width, which is how far
    * across the screen the line reaches and nothing to do with how thick it is.
    */
    public int x2;
    /**
    * the y position of the second point, which is where the line ends.
    * <p>
    * The counterpart of {@code x2}, and the distance between the two y positions is
    * the scaled height.
    */
    public int y2;
    /**
    * the colour, packed with the alpha in the top byte.
    * <p>
    * A colour set with no alpha of its own is made opaque unless it was pure black, so
    * {@code 0xFF0000} is stored as {@code 0xFFFF0000}.
    */
    public int color;
    /**
    * the rotation in degrees.
    * <p>
    * The constructor folds this into a single turn and {@link #setRotation(double)} does
    * not, so a line built at 450 reads back as 90 and a line set to 450 after that reads
    * back as 450.
    */
    public float rotation;
    /**
    * whether the rotation is about the middle of the line rather than about its first
    * point.
    * <p>
    * False on a line made directly and true on one from a builder, so the two turn
    * differently at the same angle.
    */
    public boolean rotateCenter;
    /**
    * the thickness of the line, and not how far across the screen it reaches.
    * <p>
    * A float and not a whole number of pixels, though the band it produces is cut to
    * whole numbers when it is drawn. One is the thickness a builder starts at, and
    * the reach is the distance between the two points, which is not set at all.
    */
    public float width;
    /**
    * the number this line is ordered against the other elements on its overlay, lower
    * drawn first.
    */
    public int zIndex;

    /**
    * makes a line between two points.
    * <p>
    * The rotation is folded into a single turn here, which is the one place in this
    * class where an angle is reduced rather than stored as given. The colour is put
    * through the same fix-up as {@link #setColor(int)}, so one with no alpha of its own
    * comes out opaque.
    *
    * @param x1       the x position of the start of the line
    * @param y1       the y position of the start of the line
    * @param x2       the x position of the end of the line
    * @param y2       the y position of the end of the line
    * @param color    the colour, with the alpha in the top byte
    * @param rotation the rotation in degrees, folded into a single turn
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
    * moves the first point along the x axis, and leaves the second point alone.
    * <p>
    * The line keeps its direction and its thickness and simply starts somewhere else,
    * so the two points are free to be moved one at a time.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF);
    * line.setX1(10);
    * draw.register();
    * </pre>
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
    * the x position of the first point of this line.
    * <p>
    * The point that was given as the start, which is not necessarily the left one: a
    * line drawn right to left has its first point on the right.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(100, 0, 20, 50, 0xFFFFFFFF);
    * draw.register();
    * Chat.log(`from ${line.getX1()} to ${line.getX2()}, left edge ${line.getScaledLeft()}`);
    * </pre>
    *
    * @return the x position of the start of the line.
    * @since 1.8.4
    */
    public int getX1() {
        return x1;
    }

    /**
    * moves the first point along the y axis, and leaves the second point alone.
    * <p>
    * The counterpart of {@code setX1}, and the second point is left where it was.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF);
    * line.setY1(10);
    * draw.register();
    * </pre>
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
    * the y position of the first point of this line.
    * <p>
    * The point that was given as the start, which is not necessarily the top one for
    * the same reason {@code getX1} is not necessarily the left one.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 100, 50, 20, 0xFFFFFFFF);
    * draw.register();
    * Chat.log(`from y ${line.getY1()} to ${line.getY2()}, top edge ${line.getScaledTop()}`);
    * </pre>
    *
    * @return the y position of the start of the line.
    * @since 1.8.4
    */
    public int getY1() {
        return y1;
    }

    /**
    * moves the first point, and leaves the second one where it is.
    * <p>
    * Both coordinates of the same point, which is the way to move one end of a line
    * without disturbing the other.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF);
    * line.setPos1(10, 20);
    * draw.register();
    * </pre>
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
    * moves the second point along the x axis, and leaves the first point alone.
    * <p>
    * The counterpart of {@code setX1} for the other end of the line, and it is also
    * what the scaled width follows.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF);
    * line.setX2(80);
    * draw.register();
    * Chat.log(`now reaching ${line.getScaledWidth()} across`);
    * </pre>
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
    * the x position of the second point of this line.
    * <p>
    * The other end rather than an edge, the same way {@code getX1} is the other end and
    * not a left edge.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF);
    * draw.register();
    * Chat.log(`ends at ${line.getX2()}, ${line.getY2()}`);
    * </pre>
    *
    * @return the x position of the end of the line.
    * @since 1.8.4
    */
    public int getX2() {
        return x2;
    }

    /**
    * moves the second point along the y axis, and leaves the first point alone.
    * <p>
    * The counterpart of {@code setX2}, and the scaled height follows it.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF);
    * line.setY2(80);
    * draw.register();
    * Chat.log(`now reaching ${line.getScaledHeight()} down`);
    * </pre>
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
    * the y position of the second point of this line.
    * <p>
    * The other end rather than an edge, the same way {@code getY1} is.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF);
    * draw.register();
    * Chat.log(`ends at ${line.getX2()}, ${line.getY2()}`);
    * </pre>
    *
    * @return the y position of the end of the line.
    * @since 1.8.4
    */
    public int getY2() {
        return y2;
    }

    /**
    * moves the second point, and leaves the first one where it is.
    * <p>
    * The counterpart of {@code setPos1}. Moving this end without the other is what
    * makes a line the right shape for a frame, a bar or a border.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF);
    * line.setPos2(20, 80);
    * draw.register();
    * </pre>
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
    * puts both points at once, which is how a line is made in the first place.
    * <p>
    * Both points are written rather than one being worked out from the other, so this
    * is the one setter here that changes the line and its direction in a single call.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 10, 10, 0xFFFFFFFF);
    * line.setPos(10, 20, 80, 40);
    * draw.register();
    * </pre>
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
    * sets the colour, alpha and all, from one packed number.
    * <p>
    * A colour that arrives with no alpha of its own is made opaque rather than
    * invisible, so {@code 0xFF0000} is stored as {@code 0xFFFF0000}. Pure black is left
    * alone, because there is no colour there to make opaque and {@code 0x000000} stays
    * fully transparent.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF);
    * line.setColor(0x00FF00);
    * draw.register();
    * Chat.log(`0x${line.getColor().toString(16)}, alpha ${line.getAlpha()}`);
    * </pre>
    *
    * @param color the color of the line
    * @return self for chaining.
    * @since 1.8.4
    */
    public Line setColor(int color) {
        this.color = ColorUtil.fixAlpha(color);
        return this;
    }

    /**
    * sets the colour and the transparency separately.
    * <p>
    * The alpha replaces the top byte of the colour rather than being blended with it.
    * A value outside 0 to 255 is not clamped, and a value of exactly 256 wraps round to
    * no alpha at all.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF);
    * line.setColor(0x123456, 128);
    * draw.register();
    * Chat.log(`alpha ${line.getAlpha()}`);
    * </pre>
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
    * the colour, packed with the alpha in the top byte.
    * <p>
    * What the line actually holds, which is not always what was last handed to a
    * setter: a colour given with no alpha reads back opaque. The pieces can be shifted
    * out of the number when a script wants them separately.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFF0000);
    * draw.register();
    * Chat.log(`0x${line.getColor().toString(16)}, alpha ${line.getAlpha()}`);
    * </pre>
    *
    * @return the color of the line.
    * @since 1.8.4
    */
    public int getColor() {
        return color;
    }

    /**
    * sets only the transparency, leaving the colour alone.
    * <p>
    * The counterpart of the two-argument colour setter, and usually the one to reach
    * for: a line is made opaque and then faded. A value outside 0 to 255 is not
    * clamped.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF);
    * line.setAlpha(64);
    * draw.register();
    * Chat.log(`alpha ${line.getAlpha()}`);
    * </pre>
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
    * the transparency of this line, which is the top byte of the colour.
    * <p>
    * From 0 for invisible to 255 for opaque. A colour set without an alpha of its own
    * reads back as 255 rather than 0, unless it was pure black.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFF0000);
    * line.setAlpha(64);
    * draw.register();
    * Chat.log(`alpha ${line.getAlpha()}`);
    * </pre>
    *
    * @return the alpha value of the line's color.
    * @since 1.8.4
    */
    public int getAlpha() {
        return (color >> 24) & 0xFF;
    }

    /**
    * turns the line by an angle in degrees.
    * <p>
    * Stored as given, with no folding into a single turn, so this reads back as 450
    * where the constructor would have stored 90. The turn is about the middle of the
    * line when {@code rotateCenter} is on and about the first point when it is not.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF, 0, 2);
    * line.setRotateCenter(true).setRotation(450);
    * draw.register();
    * Chat.log(`reads back as ${line.getRotation()}`);
    * </pre>
    *
    * @param rotation the rotation (clockwise) of the line
    * @return self for chaining.
    * @since 1.8.4
    */
    public Line setRotation(double rotation) {
        this.rotation = (float) rotation;
        return this;
    }

    /**
    * the rotation on this line in degrees, as it was last set.
    * <p>
    * Not folded into a single turn, so a line that was made at 450 and then set again
    * reads 450 where one that was only ever made at 450 reads 90.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const made = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF, 450, 1);
    * draw.register();
    * Chat.log(`built at 450 reads ${made.getRotation()}`);
    * </pre>
    *
    * @return the rotation (clockwise) of the line.
    * @since 1.8.4
    */
    public float getRotation() {
        return rotation;
    }

    /**
    * chooses whether the rotation is about the middle of the line or about its first
    * point.
    * <p>
    * A line made directly turns about the first point and a builder turns about the
    * middle, so this is what makes the two agree. The rotation is applied to the whole
    * line either way, thickness included.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF, 0, 2);
    * line.setRotateCenter(true).setRotation(90);
    * draw.register();
    * </pre>
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
    * whether the rotation on this line is about its middle.
    * <p>
    * False on a line made directly and true on one from a builder, since only the
    * builder sets it.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF);
    * draw.register();
    * Chat.log(`turns about the middle: ${line.isRotatingCenter()}`);
    * </pre>
    *
    * @return {@code true} if this line should be rotated around its center, {@code false}
    * otherwise.
    * @since 1.8.4
    */
    public boolean isRotatingCenter() {
        return rotateCenter;
    }

    /**
    * sets the thickness of the line.
    * <p>
    * This is the stroke and not the reach: how far across the screen the line goes is
    * the distance between its two points and is not changed by this. A float, so a
    * thickness that is not a whole number is kept, though the band it produces is cut
    * to whole numbers when the line is drawn. Nothing is checked, so zero and negative
    * numbers are accepted.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF);
    * line.setWidth(3);
    * draw.register();
    * Chat.log(`${line.getWidth()} thick, reaching ${line.getScaledWidth()} across`);
    * </pre>
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
    * the thickness of this line.
    * <p>
    * Not the same number as {@code getScaledWidth}, which is how far across the screen
    * the line reaches: the two are the two different halves of a line, the stroke and
    * the extent. A line that is 2 thick and 50 across has a width of 2 and a scaled
    * width of 50.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF, 0, 2);
    * draw.register();
    * Chat.log(`${line.getWidth()} thick, reaching ${line.getScaledWidth()} across`);
    * </pre>
    *
    * @return the width of the line.
    * @since 1.8.4
    */
    public float getWidth() {
        return width;
    }

    /**
    * sets the number this line is ordered against the other elements on its overlay,
    * lower drawn first.
    * <p>
    * Two lines drawn over the same place are ordered by this and by nothing else, and a
    * tie keeps the order they were added in.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const under = draw.addLine(0, 0, 50, 50, 0xFF000000);
    * const over = draw.addLine(0, 0, 50, 50, 0x80FFFFFF);
    * under.setZIndex(2);
    * over.setZIndex(1);
    * draw.register();
    * </pre>
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
    * the number this line is ordered against the other elements on its overlay.
    * <p>
    * Zero until a {@code setZIndex} call says otherwise, and the overlay sorts by this
    * lowest first. A tie keeps the order the elements were added in, so two lines drawn
    * over the same place are separated by this and by nothing else.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF);
    * line.setZIndex(3);
    * draw.register();
    * Chat.log(`z-index ${line.getZIndex()}`);
    * </pre>
    *
    * @return the z-index of this element
    */
    @Override
    public int getZIndex() {
        return zIndex;
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
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
                (int) -halfWidth,
                (int) length,
                (int) halfWidth,
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
    public void render3D(PoseStack matrixStack, MultiBufferSource consumers, int light, boolean seeThrough, float delta) {
        matrixStack.pushPose();
        matrixStack.translate(x1, y1, 0);
        if (rotateCenter) {
            matrixStack.translate(getScaledWidth() / 2d, getScaledHeight() / 2d, 0);
        }
        matrixStack.mulPose(new Quaternionf().rotateLocalZ((float) Math.toRadians(rotation)));
        if (rotateCenter) {
            matrixStack.translate(-getScaledWidth() / 2d, -getScaledHeight() / 2d, 0);
        }
        matrixStack.translate(-x1, -y1, 0);

        float brightness = RenderElement.lightBrightness(light);
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f * brightness;
        float g = ((color >> 8) & 0xFF) / 255.0f * brightness;
        float b = (color & 0xFF) / 255.0f * brightness;

        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length == 0 || width <= 0) {
            matrixStack.popPose();
            return;
        }

        // Render a quad instead of relying on the target-specific line vertex
        // format. This preserves the script-facing width in surface pixel units
        // on every supported version.
        float halfWidth = width / 2.0f;
        float offsetX = -dy / length * halfWidth;
        float offsetY = dx / length * halfWidth;
        VertexConsumer vc = consumers.getBuffer(SurfaceRenderTypes.quads(false, !seeThrough));
        PoseStack.Pose pose = matrixStack.last();
        vc.addVertex(pose, x1 + offsetX, y1 + offsetY, 0).setColor(r, g, b, a);
        vc.addVertex(pose, x1 - offsetX, y1 - offsetY, 0).setColor(r, g, b, a);
        vc.addVertex(pose, x2 - offsetX, y2 - offsetY, 0).setColor(r, g, b, a);
        vc.addVertex(pose, x2 + offsetX, y2 + offsetY, 0).setColor(r, g, b, a);

        matrixStack.popPose();
    }

    /**
    * binds this line to the overlay it belongs to, which is what the parent width and
    * height are then read from.
    * <p>
    * The element builders call this as they build, so a line that has been built is
    * already bound. Binding is what makes the parent-relative align methods measure
    * against that overlay rather than against the window, and {@code null} puts it back
    * to the window.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.lineBuilder().pos(0, 0, 50, 50).build();
    * // a built line is already bound, and can be bound again by hand
    * line.setParent(draw);
    * draw.reAddElement(line);
    * draw.register();
    * </pre>
    *
    * @param parent the overlay this element is on, or {@code null} for the window
    * @return self for chaining.
    */
    public Line setParent(IDraw2D<?> parent) {
        this.parent = parent;
        return this;
    }

    /**
    * puts this line at a position, keeping the extent it has.
    * <p>
    * The new position becomes the first point and the second one is worked out from how
    * far across and how far down the line already reached, so the line keeps its size
    * and its thickness. As in the builder's {@code moveTo}, the sign of that reach is
    * not kept, so a line that ran right to left or bottom to top comes out mirrored.
    * This is what the align methods call.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 20, 0xFFFFFFFF);
    * line.moveTo(40, 20);
    * draw.register();
    * Chat.log(`now from ${line.getX1()},${line.getY1()} to ${line.getX2()},${line.getY2()}`);
    * </pre>
    *
    * @param x the new x position
    * @param y the new y position
    * @return self for chaining.
    */
    @Override
    public Line moveTo(int x, int y) {
        return setPos(x, y, x + getScaledWidth(), y + getScaledHeight());
    }

    /**
    * how far across the screen the line reaches, which is the distance between its two
    * x positions.
    * <p>
    * The counterpart of the thickness, and not to be confused with it: this is the
    * extent and {@code getWidth} is the stroke. It is the number the horizontal align
    * methods measure against.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF, 0, 2);
    * draw.register();
    * Chat.log(`reaching ${line.getScaledWidth()} across, ${line.getWidth()} thick`);
    * </pre>
    *
    * @return the scaled width
    */
    @Override
    public int getScaledWidth() {
        return Math.abs(x2 - x1);
    }

    /**
    * the width of the overlay this line is on, or the window's width if it has not been
    * put on one.
    * <p>
    * This is what the parent-relative align methods measure against, so a line that has
    * not been added still aligns against the screen. A line that came out of a builder
    * is already bound to that builder's overlay.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF);
    * draw.register();
    * Chat.log(`measured against ${line.getParentWidth()}`);
    * </pre>
    *
    * @return the width of the parent
    */
    @Override
    public int getParentWidth() {
        return parent != null ? parent.getWidth() : mc.getWindow().getGuiScaledWidth();
    }

    /**
    * how far down the screen the line reaches, which is the distance between its two y
    * positions.
    * <p>
    * The counterpart of {@code getScaledWidth}, and the same distinction: this is the
    * extent and {@code getWidth} is the thickness. This is the number the vertical
    * align methods measure.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 20, 0xFFFFFFFF, 0, 2);
    * draw.register();
    * Chat.log(`reaching ${line.getScaledHeight()} down, ${line.getWidth()} thick`);
    * </pre>
    *
    * @return the scaled height
    */
    @Override
    public int getScaledHeight() {
        return Math.abs(y2 - y1);
    }

    /**
    * the height of the overlay this line is on, or the window's height if it has not
    * been put on one.
    * <p>
    * The counterpart of {@code getParentWidth}, and read the same way. This is what the
    * parent-relative align methods measure against, so a line that has not been added
    * still aligns against the screen.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF);
    * draw.register();
    * Chat.log(`measured against ${line.getParentHeight()}`);
    * </pre>
    *
    * @return the height of the parent
    */
    @Override
    public int getParentHeight() {
        return parent != null ? parent.getHeight() : mc.getWindow().getGuiScaledHeight();
    }

    /**
    * the left edge of the line, which is the smaller of its two x positions.
    * <p>
    * The two points can be in either order and this is whichever is further left, the
    * same way a rectangle reads its corners. The width the line covers is
    * {@code getScaledWidth}, which is what the align methods add to it.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(50, 0, 0, 20, 0xFFFFFFFF);
    * draw.register();
    * Chat.log(`from ${line.getScaledLeft()} to ${line.getScaledRight()}`);
    * </pre>
    *
    * @return the position of the scaled element's left side
    */
    @Override
    public int getScaledLeft() {
        return Math.min(x1, x2);
    }

    /**
    * the top edge of the line, which is the smaller of its two y positions.
    * <p>
    * The counterpart of {@code getScaledLeft}, and whichever y point is further up.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const line = draw.addLine(0, 50, 20, 0, 0xFFFFFFFF);
    * draw.register();
    * Chat.log(`top ${line.getScaledTop()}, bottom ${line.getScaledBottom()}`);
    * </pre>
    *
    * @return the position of the scaled element's top side
    */
    @Override
    public int getScaledTop() {
        return Math.min(y1, y2);
    }

    /**
    * the builder for a line, made by the overlay's own {@code lineBuilder} method.
    * <p>
    * A line here is two points rather than a corner and a size, because that is what a
    * line is: it has a direction, and a diagonal is the normal case rather than the
    * exception. Everything is named after the thing being set and hands the builder
    * back, so a whole line is one chain that ends in {@code build()} or
    * {@code buildAndAdd()}.
    * <p>
    * Both points start at zero, the thickness starts at one and is the stroke rather
    * than the reach, the colour starts opaque white and is kept apart from the alpha so
    * either can be set on its own, the rotation starts at zero and is not folded into a
    * single turn, and the rotation is about the middle of the line until
    * {@code rotateCenter} says otherwise. That last one is the opposite of a line made
    * directly, so the two turn differently at the same angle.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * draw.lineBuilder().pos(0, 0, 50, 50).color(0xFFFFFFFF).width(2).buildAndAdd();
    * draw.register();
    * </pre>
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
        * makes a builder for lines on one overlay.
        * <p>
        * A script does not call this directly: the overlay's own {@code lineBuilder}
        * method is what fills in the overlay.
        *
        * @param draw2D the overlay the lines will be added to
        */
        public Builder(IDraw2D<?> draw2D) {
            super(draw2D);
        }

        /**
        * puts the first point's x position, and leaves the rest of the line alone.
        * <p>
        * Both points start at zero, so a line is only a line once both have been set.
        * The two points can be given in either order, so a line drawn right to left is
        * a line and not the same one drawn back.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.lineBuilder().pos1(0, 0).x2(50).y2(50).color(0xFFFFFFFF).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the x position of the first point this builder will build the line at.
        * <p>
        * Zero until an {@code x1}, a {@code pos1} or a {@code pos} call says
        * otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.lineBuilder().pos1(10, 20);
        * Chat.log(`first point at x ${builder.getX1()}`);
        * </pre>
        *
        * @return the x position of the first point.
        * @since 1.8.4
        */
        public int getX1() {
            return x1;
        }

        /**
        * puts the first point's y position, and leaves the rest of the line alone.
        * <p>
        * The counterpart of {@code x1}, and for the same reason a line needs both of
        * them before it is a line.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.lineBuilder().pos1(0, 0).y2(50).x2(50).color(0xFFFFFFFF).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the y position of the first point this builder will build the line at.
        * <p>
        * Zero until a {@code y1}, a {@code pos1} or a {@code pos} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.lineBuilder().pos1(10, 20);
        * Chat.log(`first point at y ${builder.getY1()}`);
        * </pre>
        *
        * @return the y position of the first point.
        * @since 1.8.4
        */
        public int getY1() {
            return y1;
        }

        /**
        * puts the first point, in one call, and leaves the second one alone.
        * <p>
        * The way to set one end of a line without disturbing the other, which is what a
        * frame or a border needs.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.lineBuilder().pos1(0, 0).pos2(50, 50).color(0xFFFFFFFF).buildAndAdd();
        * draw.register();
        * </pre>
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
        * puts the second point's x position, and leaves the first point alone.
        * <p>
        * The counterpart of {@code x1} for the other end, and the number the scaled
        * width follows.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.lineBuilder().pos1(0, 0).x2(80).y2(50).color(0xFFFFFFFF).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the x position of the second point this builder will build the line at.
        * <p>
        * Zero until an {@code x2}, a {@code pos2} or a {@code pos} call says
        * otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.lineBuilder().pos2(50, 20);
        * Chat.log(`second point at x ${builder.getX2()}`);
        * </pre>
        *
        * @return the x position of the second point.
        * @since 1.8.4
        */
        public int getX2() {
            return x2;
        }

        /**
        * puts the second point's y position, and leaves the first point alone.
        * <p>
        * The counterpart of {@code x2}, and the number the scaled height follows.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.lineBuilder().pos1(0, 0).y2(80).x2(50).color(0xFFFFFFFF).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the y position of the second point this builder will build the line at.
        * <p>
        * Zero until a {@code y2}, a {@code pos2} or a {@code pos} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.lineBuilder().pos2(50, 20);
        * Chat.log(`second point at y ${builder.getY2()}`);
        * </pre>
        *
        * @return the y position of the second point.
        * @since 1.8.4
        */
        public int getY2() {
            return y2;
        }

        /**
        * puts the second point, in one call, and leaves the first one alone.
        * <p>
        * The counterpart of {@code pos1}. Between the two is a whole line, so a frame
        * is four of these.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.lineBuilder().pos1(0, 0).pos2(80, 50).color(0xFFFFFFFF).buildAndAdd();
        * draw.register();
        * </pre>
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
        * puts both points at once, which is how a line is normally given.
        * <p>
        * All four coordinates are written rather than one end being worked out from the
        * other, so this is the one setter that sets the whole line in a single call.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.lineBuilder().pos(0, 0, 50, 50).color(0xFFFFFFFF).buildAndAdd();
        * draw.register();
        * </pre>
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
        * turns the line by an angle in degrees.
        * <p>
        * Stored as given rather than folded into a single turn, so this reads back as
        * 450. The built line goes through the constructor, which does fold it, so a line
        * built at 450 comes out turned 90 even though this builder reports 450. The turn
        * is about the middle of the line until {@code rotateCenter} says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.lineBuilder().pos(0, 0, 50, 50).rotation(45).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the rotation this builder will build the line at, in degrees.
        * <p>
        * Zero until a {@code rotation} call says otherwise, and not folded into a
        * single turn even though the line that gets built will be.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.lineBuilder().rotation(450);
        * Chat.log(`builder says ${builder.getRotation()}`);
        * const line = builder.pos(0, 0, 50, 50).buildAndAdd();
        * draw.register();
        * Chat.log(`the line says ${line.getRotation()}`);
        * </pre>
        *
        * @return the rotation (clockwise) of the line.
        * @since 1.8.4
        */
        public float getRotation() {
            return rotation;
        }

        /**
        * chooses whether the rotation is about the middle of the line or about its
        * first point.
        * <p>
        * True is what a builder starts at, which is the opposite of a line made
        * directly, so the two turn differently until one of them is told otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const line = draw.lineBuilder().pos(0, 0, 50, 50).rotateCenter(false).buildAndAdd();
        * draw.register();
        * Chat.log(`turns about a point: ${!line.isRotatingCenter()}`);
        * </pre>
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
        * whether the line this builder builds will turn about its middle.
        * <p>
        * True until a {@code rotateCenter} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.lineBuilder();
        * Chat.log(`builds turning about the middle: ${builder.isRotatingCenter()}`);
        * </pre>
        *
        * @return {@code true} if this line should be rotated around its center,
        * {@code false} otherwise.
        * @since 1.8.4
        */
        public boolean isRotatingCenter() {
            return rotateCenter;
        }

        /**
        * sets the thickness of the line, which starts at one.
        * <p>
        * The stroke and not the reach: how far across the screen the line goes is the
        * distance between its two points and is not changed by this. Nothing is checked
        * here, so zero and negative thicknesses are accepted.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const line = draw.lineBuilder().pos(0, 0, 50, 50).width(3).buildAndAdd();
        * draw.register();
        * Chat.log(`${line.getWidth()} thick`);
        * </pre>
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
        * the thickness this builder will build the line at, which starts at one.
        * <p>
        * Not the same number as {@code getScaledWidth}: this is the stroke and that is
        * the extent, so a 3 thick line reaching 50 across has a width of 3 and a scaled
        * width of 50.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.lineBuilder().pos(0, 0, 50, 50).width(3);
        * Chat.log(`${builder.getWidth()} thick, reaching ${builder.getScaledWidth()} across`);
        * </pre>
        *
        * @return the width of the line.
        * @since 1.8.4
        */
        public float getWidth() {
            return width;
        }

        /**
        * sets the colour from one packed number, and takes its alpha from it as well.
        * <p>
        * The alpha is read out of the number and kept separately, so a later
        * {@code alpha} call changes the transparency without touching the colour. A
        * colour with no alpha of its own comes out opaque unless it was pure black.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.lineBuilder().pos(0, 0, 50, 50).color(0x80FF0000).buildAndAdd();
        * draw.register();
        * </pre>
        *
        * @param color the color of the line
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder color(int color) {
            this.color = color;
            this.alpha = ColorUtil.fixAlpha(color) >>> 24;
            return this;
        }

        /**
        * sets the colour and the alpha as two separate numbers.
        * <p>
        * Both are stored as given with no fix-up, so this is the form that will hold a
        * colour whose alpha byte is zero, which the one-argument form would make opaque.
        * The two are combined when the line is built.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const line = draw.lineBuilder().pos(0, 0, 50, 50).color(0xFF0000, 128).buildAndAdd();
        * draw.register();
        * Chat.log(`built at alpha ${line.getAlpha()}`);
        * </pre>
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
        * sets the colour from three components, each from 0 to 255.
        * <p>
        * The three are packed into the low 24 bits and the separate alpha is left as it
        * was, which is opaque unless something has changed it. So the number this
        * builder reports from {@code getColor} is {@code 0x00RRGGBB} while the built
        * line is {@code 0xFFRRGGBB}, the alpha being put back on when the element is
        * made.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const line = draw.lineBuilder().pos(0, 0, 50, 50).color(255, 128, 0).buildAndAdd();
        * draw.register();
        * Chat.log(`built at 0x${line.getColor().toString(16)}`);
        * </pre>
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
        * sets the colour from four components, each from 0 to 255.
        * <p>
        * The same packing as the shorter form, with the fourth going into the packed
        * colour rather than into the separate alpha, so the alpha this builder reports
        * is still whatever it was before.
        * <p>
        * That matters on the line that gets built. The builder combines the two when it
        * makes the line, and it takes the top byte from the separate alpha and the rest
        * from the packed colour, so the fourth component here is thrown away and the
        * line comes out as opaque as the separate alpha says. Use {@code alpha} or
        * {@code color(int, int)} to set the transparency of the line itself.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const line = draw.lineBuilder().pos(0, 0, 50, 50).color(255, 128, 0, 128).buildAndAdd();
        * draw.register();
        * Chat.log(`built at 0x${line.getColor().toString(16)}`);
        * </pre>
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
        * the colour this builder will build the line at, as it is stored here.
        * <p>
        * Which number this is depends on how it was set. After {@code color(int)} and
        * {@code color(int, int)} it is the full packed number. After the
        * three-component form it is the low 24 bits on their own, and after the
        * four-component form it is the full number again, because that one packs the
        * alpha in. The built line's own {@code getColor} is the honest one either way.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const three = draw.lineBuilder().color(255, 0, 0);
        * Chat.log(`three components: 0x${three.getColor().toString(16)}`);
        * const four = draw.lineBuilder().color(255, 0, 0, 128);
        * Chat.log(`four components: 0x${four.getColor().toString(16)}`);
        * </pre>
        *
        * @return the color of the line.
        * @since 1.8.4
        */
        public int getColor() {
            return color;
        }

        /**
        * sets only the transparency, leaving the colour alone.
        * <p>
        * The number is not clamped, so a value over 255 is a shift into the colour
        * channels and 256 is no alpha at all.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const line = draw.lineBuilder().pos(0, 0, 50, 50).color(0xFF0000).alpha(64).buildAndAdd();
        * draw.register();
        * Chat.log(`built at alpha ${line.getAlpha()}`);
        * </pre>
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
        * the transparency this builder will build the line at, from 0 to 255.
        * <p>
        * Opaque is what it starts at, and it is a number of its own here rather than
        * being read out of the colour, which is why {@code color(int, int)} and
        * {@code alpha} can set the transparency without disturbing the tint.
        * <p>
        * The one form that does not reach this is the four-component colour, which
        * packs its alpha into the colour and leaves this number alone. The line that
        * gets built takes its top byte from here and not from there, so that fourth
        * component is lost.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.lineBuilder().color(255, 0, 0, 128);
        * Chat.log(`builder still reports ${builder.getAlpha()}, not 128`);
        * const line = builder.pos(0, 0, 50, 50).buildAndAdd();
        * draw.register();
        * // the built line takes its alpha from the builder's own number
        * Chat.log(`the line is at ${line.getAlpha()}`);
        * </pre>
        *
        * @return the alpha value of the color.
        * @since 1.8.4
        */
        public int getAlpha() {
            return alpha;
        }

        /**
        * sets the number the line will be ordered against the other elements on its
        * overlay, lower drawn first.
        * <p>
        * Zero is what a builder starts at, and a tie keeps the order the elements were
        * added in.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.lineBuilder().pos(0, 0, 50, 50).zIndex(5).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the z-index this builder will build the line at.
        * <p>
        * Zero until a {@code zIndex} call says otherwise, and it is this number the
        * built line is given rather than a fresh one. The built line and this builder
        * are the same {@code zIndex} field all the way through {@code createElement}.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.lineBuilder().zIndex(5);
        * Chat.log(`will be built at z-index ${builder.getZIndex()}`);
        * </pre>
        *
        * @return the z-index of the line.
        * @since 1.8.4
        */
        public int getZIndex() {
            return zIndex;
        }

        /**
        * makes the line out of everything set on this builder, and binds it to the
        * overlay this builder was made from.
        * <p>
        * A new line each time, so the same builder can build more than one. This is
        * what {@code build} and {@code buildAndAdd} call.
        * <p>
        * The colour is put together here: the separate alpha becomes the top byte and
        * the low 24 bits of the stored colour are kept, which is why a builder set with
        * the three-component form ends up opaque.
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
        * puts the line at a position and keeps the extent it has.
        * <p>
        * The new position becomes the first point and the second one is worked out from
        * how far across and how far down the line already reached, so moving a line
        * keeps its size and its thickness. This is what the align methods call.
        * <p>
        * What it does not keep is which way the line was pointing. Those two numbers
        * are magnitudes, so a line whose points were the other way round comes back
        * mirrored: a line drawn right to left and one drawn bottom to top both come out
        * pointing down and to the right, at the same size. A line that already runs
        * down and to the right is the one this leaves exactly as it was.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.lineBuilder().pos(0, 0, 50, 20);
        * builder.moveTo(10, 10);
        * builder.buildAndAdd();
        * draw.register();
        * Chat.log(`now from ${builder.getX1()},${builder.getY1()} to ${builder.getX2()},${builder.getY2()}`);
        * </pre>
        *
        * @param x the new x position
        * @param y the new y position
        * @return self for chaining.
        */
        @Override
        public Builder moveTo(int x, int y) {
            return pos(x, y, x + getScaledWidth(), y + getScaledHeight());
        }

        /**
        * how far across the screen the line will reach, which is the distance between
        * its two x positions.
        * <p>
        * The counterpart of the thickness, and not to be confused with it: this is the
        * extent and {@code getWidth} is the stroke. It is the number the align methods
        * measure against.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.lineBuilder().pos(0, 0, 50, 20).width(3);
        * Chat.log(`reaching ${builder.getScaledWidth()} across, ${builder.getWidth()} thick`);
        * </pre>
        *
        * @return the scaled width
        */
        @Override
        public int getScaledWidth() {
            return Math.abs(x2 - x1);
        }

        /**
        * the width of the overlay this builder was made from, which is what the
        * parent-relative align methods measure against.
        * <p>
        * There is no window fallback here, because a builder always has an overlay.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.lineBuilder();
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
        * how far down the screen the line will reach, which is the distance between its
        * two y positions.
        * <p>
        * The counterpart of {@code getScaledWidth}, and the same distinction: this is
        * the extent and {@code getWidth} is the thickness.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.lineBuilder().pos(0, 0, 50, 20).width(3);
        * Chat.log(`reaching ${builder.getScaledHeight()} down, ${builder.getWidth()} thick`);
        * </pre>
        *
        * @return the scaled height
        */
        @Override
        public int getScaledHeight() {
            return Math.abs(y2 - y1);
        }

        /**
        * the height of the overlay this builder was made from, which is what the
        * parent-relative align methods measure against.
        * <p>
        * The counterpart of {@code getParentWidth}, and with no window fallback either.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.lineBuilder();
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
        * the left edge of the line, which is the smaller of its two x positions.
        * <p>
        * The two points can be in either order and this is whichever is further left,
        * the same way the rectangle reads its corners.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.lineBuilder().pos(50, 0, 0, 20);
        * Chat.log(`left ${builder.getScaledLeft()}, right ${builder.getScaledRight()}`);
        * </pre>
        *
        * @return the position of the scaled element's left side
        */
        @Override
        public int getScaledLeft() {
            return Math.min(x1, x2);
        }

        /**
        * the top edge of the line, which is the smaller of its two y positions.
        * <p>
        * The counterpart of {@code getScaledLeft}, and whichever y point is further up.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.lineBuilder().pos(0, 50, 20, 0);
        * Chat.log(`top ${builder.getScaledTop()}, bottom ${builder.getScaledBottom()}`);
        * </pre>
        *
        * @return the position of the scaled element's top side
        */
        @Override
        public int getScaledTop() {
            return Math.min(y1, y2);
        }

    }

}
