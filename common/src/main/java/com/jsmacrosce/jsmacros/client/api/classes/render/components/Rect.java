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
 * a filled rectangle, given as two corners.
 * <p>
 * The two corners are there because they are the two things that are easy to compute,
 * rather than a position and a size: a frame is two edges, a bar is two edges, and a
 * piece of a list is a row and a column. They do not have to be in order, so a
 * rectangle whose second corner is up and to the left of its first is a perfectly
 * ordinary one, and the size is the distance between them whichever way round they are.
 * <p>
 * The width and height are not numbers stored in their own right. {@code x2} is a
 * position rather than {@code x1} plus a width, and a width is set by moving whichever
 * of the two corners is further along. That is also why a negative width turns the
 * rectangle round rather than being refused: nothing here checks it.
 * <p>
 * There is no scale on a rectangle, so its size is the same drawn and undrawn and there
 * is nothing between {@code getWidth} and the size the align methods measure. The
 * colour is packed with the alpha in the top byte the same way the rest of the API
 * packs one, and a colour that carries no alpha of its own is made opaque rather than
 * invisible.
 * example:
 * <pre>
 * const draw = Hud.createDraw2D();
 * // the corners the other way round is fine
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
    * the overlay this rectangle was added to, and {@code null} when it has not been put
    * on one.
    * <p>
    * This is what the parent width and height are read from, and they fall back to the
    * window when it is null.
    */
    @Nullable
    public IDraw2D<?> parent;
    /**
    * the rotation in degrees, wrapped into a single turn.
    * <p>
    * Both the constructor and {@link #setRotation(double)} fold the angle down, so this
    * is between -180 and 180 rather than whatever was given.
    */
    public float rotation;
    /**
    * whether the rotation is about the middle of this rectangle rather than about the
    * corner it is given by.
    * <p>
    * False on a rectangle made directly and true on one from a builder, so the two turn
    * differently at the same angle.
    */
    public boolean rotateCenter;
    /**
    * the first x position, which is one of the two corners.
    * <p>
    * Not necessarily the left one: the smaller of the two is what the align methods use
    * as the left edge.
    */
    public int x1;
    /**
    * the first y position, which is one of the two corners.
    * <p>
    * Not necessarily the top one, for the same reason as {@code x1}.
    */
    public int y1;
    /**
    * the second x position, which is the other corner.
    * <p>
    * A width is set by writing {@code x1} plus that width here, or the other way round
    * when {@code x1} is the larger of the two.
    */
    public int x2;
    /**
    * the second y position, which is the other corner.
    * <p>
    * The counterpart of {@code x2}, and set the same way.
    */
    public int y2;
    /**
    * the colour, packed with the alpha in the top byte.
    * <p>
    * A colour set through the one-argument form is made opaque if it had no alpha and
    * was not pure black, so {@code 0xFF0000} reads back as {@code 0xFFFF0000}.
    */
    public int color;
    /**
    * the number this rectangle is ordered against the other elements on its overlay,
    * lower drawn first.
    */
    public int zIndex;

    /**
    * makes a rectangle with an opaque colour, from two corners.
    * <p>
    * The colour given is put through the same fix-up as {@link #setColor(int)}, so a
    * colour with no alpha of its own comes out opaque rather than invisible. The
    * rotation is folded into a single turn.
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
    * makes a rectangle from two corners, with the alpha given separately.
    * <p>
    * The alpha replaces the top byte of the colour outright, so it is the whole of the
    * transparency: the colour's own top byte is thrown away rather than combined.
    *
    * @param x1       the first x position of this rectangle
    * @param y1       the first y position of this rectangle
    * @param x2       the second x position of this rectangle
    * @param y2       the second y position of this rectangle
    * @param color    the colour, whose top byte is replaced by the alpha
    * @param alpha    the alpha, in the top byte
    * @param rotation the rotation in degrees, folded into a single turn
    * @param zIndex   the z-index against the other elements on the overlay
    */
    public Rect(int x1, int y1, int x2, int y2, int color, int alpha, float rotation, int zIndex) {
        setPos(x1, y1, x2, y2);
        setColor(color, alpha);
        this.rotation = Mth.wrapDegrees(rotation);
        this.zIndex = zIndex;
    }

    /**
    * moves the first x position, and leaves the other three corners alone.
    * <p>
    * The width follows from wherever this corner ends up, so a rectangle whose corners
    * are the other way round still reports a size and still draws.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * rect.setX1(20);
    * draw.register();
    * Chat.log(`now ${rect.getWidth()} wide, from ${rect.getX1()}`);
    * </pre>
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
    * the first x position of this rectangle, which is one of its two corners.
    * <p>
    * The one that was given rather than an edge: the corners can be in either order, so
    * on a rectangle drawn right to left this is the larger of the two x positions.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 20, 110, 70, 0xFFFF0000);
    * draw.register();
    * Chat.log(`corners ${rect.getX1()},${rect.getY1()} and ${rect.getX2()},${rect.getY2()}`);
    * </pre>
    *
    * @return the first x position of this rectangle.
    * @since 1.8.4
    */
    public int getX1() {
        return x1;
    }

    /**
    * moves the first y position, and leaves the other three corners alone.
    * <p>
    * The height follows from wherever this corner ends up, the same way the width does
    * for the x positions.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * rect.setY1(20);
    * draw.register();
    * Chat.log(`now ${rect.getHeight()} tall, from ${rect.getY1()}`);
    * </pre>
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
    * the first y position of this rectangle, which is one of its two corners.
    * <p>
    * The one that was given rather than an edge, the same way {@code getX1} is rather
    * than a left edge.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 20, 110, 70, 0xFFFF0000);
    * draw.register();
    * Chat.log(`corners ${rect.getX1()},${rect.getY1()} and ${rect.getX2()},${rect.getY2()}`);
    * </pre>
    *
    * @return the first y position of this rectangle.
    * @since 1.8.4
    */
    public int getY1() {
        return y1;
    }

    /**
    * moves the first corner, and leaves the second one alone.
    * <p>
    * Both coordinates of the same corner, so the two axes are set together and the
    * width and height follow from where the corner lands.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * rect.setPos1(20, 30);
    * draw.register();
    * Chat.log(`now ${rect.getWidth()} by ${rect.getHeight()}`);
    * </pre>
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
    * moves the second x position, and leaves the other three corners alone.
    * <p>
    * A width is often easier to think of as a change to this corner than as a change to
    * the size, which is what this is for.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * rect.setX2(160);
    * draw.register();
    * Chat.log(`now ${rect.getWidth()} wide`);
    * </pre>
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
    * the second x position of this rectangle, which is its other corner.
    * <p>
    * The one that was given rather than an edge, the same way {@code getX1} is.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 20, 110, 70, 0xFFFF0000);
    * draw.register();
    * Chat.log(`corners ${rect.getX1()},${rect.getY1()} and ${rect.getX2()},${rect.getY2()}`);
    * </pre>
    *
    * @return the second x position of this rectangle.
    * @since 1.8.4
    */
    public int getX2() {
        return x2;
    }

    /**
    * moves the second y position, and leaves the other three corners alone.
    * <p>
    * The counterpart of {@code setX2}, and the reason a bar is two edges rather than a
    * position and a height.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * rect.setY2(90);
    * draw.register();
    * Chat.log(`now ${rect.getHeight()} tall`);
    * </pre>
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
    * the second y position of this rectangle, which is its other corner.
    * <p>
    * The one that was given rather than an edge, the same way {@code getY1} is.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 20, 110, 70, 0xFFFF0000);
    * draw.register();
    * Chat.log(`corners ${rect.getX1()},${rect.getY1()} and ${rect.getX2()},${rect.getY2()}`);
    * </pre>
    *
    * @return the second y position of the rectangle.
    * @since 1.8.4
    */
    public int getY2() {
        return y2;
    }

    /**
    * moves the second corner, and leaves the first one alone.
    * <p>
    * The counterpart of {@code setPos1}, and useful for resizing from the far side:
    * moving this corner left and up shrinks the rectangle without touching where it
    * starts.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * rect.setPos2(60, 30);
    * draw.register();
    * Chat.log(`now ${rect.getWidth()} by ${rect.getHeight()}`);
    * </pre>
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
    * puts both corners at once, which is how a rectangle is made from a frame or a
    * pair of edges.
    * <p>
    * Both corners are written rather than one of them being worked out, so this is the
    * one setter here that can change the size and the position in a single call.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(0, 0, 10, 10, 0xFFFF0000);
    * rect.setPos(10, 10, 110, 60);
    * draw.register();
    * Chat.log(`now ${rect.getWidth()} by ${rect.getHeight()}`);
    * </pre>
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
    * resizes this rectangle, keeping the corner it was built around.
    * <p>
    * A width is not a number stored in its own right, so this moves whichever of the
    * two x positions is further along: when {@code x1} is the smaller the second one is
    * moved to {@code x1} plus the width, and when it is the larger the first one is
    * moved instead. A rectangle whose corners are the other way round therefore keeps
    * its left edge when it is widened, which is usually what is wanted.
    * <p>
    * Nothing is checked, so a negative width is not refused: it moves the corner past
    * the other one and the rectangle turns round, still reporting the size it now has.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * rect.setWidth(200);
    * draw.register();
    * Chat.log(`from ${rect.getX1()} to ${rect.getX2()}, so ${rect.getWidth()} wide`);
    * </pre>
    *
    * @param width the new width of this rectangle
    * @return self for chaining.
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
    * how far apart the two x positions are, which is this rectangle's width.
    * <p>
    * The distance and not a stored size, so it is the same whichever way round the
    * corners are, and it follows any corner being moved. There is no scale on a
    * rectangle, so this is also what is drawn and what the align methods measure.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(110, 60, 10, 10, 0xFFFF0000);
    * draw.register();
    * Chat.log(`${rect.getWidth()} wide, corners the other way round`);
    * </pre>
    *
    * @return the width of this rectangle.
    * @since 1.8.4
    */
    public int getWidth() {
        return Math.abs(x2 - x1);
    }

    /**
    * resizes this rectangle, keeping the corner it was built around.
    * <p>
    * The counterpart of {@code setWidth}, and moved along the y axis the same way:
    * whichever y position is further down is the one that moves. A negative height is
    * not refused and turns the rectangle round.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * rect.setHeight(20);
    * draw.register();
    * Chat.log(`from ${rect.getY1()} to ${rect.getY2()}, so ${rect.getHeight()} tall`);
    * </pre>
    *
    * @param height the new height of this rectangle
    * @return self for chaining.
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
    * how far apart the two y positions are, which is this rectangle's height.
    * <p>
    * The distance rather than a stored size, the same way {@code getWidth} is, and
    * this is also what is drawn because there is no scale.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * draw.register();
    * Chat.log(`${rect.getHeight()} tall, and ${rect.getWidth()} wide`);
    * </pre>
    *
    * @return the height of this rectangle.
    * @since 1.8.4
    */
    public int getHeight() {
        return Math.abs(y2 - y1);
    }

    /**
    * resizes this rectangle on both axes, keeping the corners it was built around.
    * <p>
    * The width is set first, so a height that turns the rectangle round leaves the
    * width already applied. Neither is checked, so negative numbers are allowed and
    * turn the rectangle round.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * rect.setSize(40, 20);
    * draw.register();
    * Chat.log(`now ${rect.getWidth()} by ${rect.getHeight()}`);
    * </pre>
    *
    * @param width  the new width of this rectangle
    * @param height the new height of this rectangle
    * @return self for chaining.
    * @since 1.8.4
    */
    public Rect setSize(int width, int height) {
        setWidth(width);
        setHeight(height);
        return this;
    }

    /**
    * sets the colour, alpha and all, from one packed number.
    * <p>
    * A colour that arrives with no alpha of its own is made opaque rather than
    * invisible, so {@code 0xFF0000} is stored as {@code 0xFFFF0000}. Pure black is the
    * one thing that is left alone, because there is no colour there to make opaque and
    * {@code 0x000000} stays fully transparent. A colour that does carry an alpha is
    * stored exactly as given.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFFFFFF);
    * rect.setColor(0xFF0000);
    * draw.register();
    * Chat.log(`stored as 0x${rect.getColor().toString(16)}, alpha ${rect.getAlpha()}`);
    * </pre>
    *
    * @param color the packed colour, with the alpha in the top byte
    * @return self for chaining.
    * @since 1.0.5
    */
    public Rect setColor(int color) {
        this.color = ColorUtil.fixAlpha(color);
        return this;
    }

    /**
    * sets the colour and the transparency separately.
    * <p>
    * The alpha replaces the top byte of the colour rather than being blended with it,
    * so a colour that came with an alpha of its own loses it. Anything outside 0 to 255
    * is not clamped: a value over 255 shifts into the colour channels and a value of
    * exactly 256 wraps round to no alpha at all.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFFFFFF);
    * rect.setColor(0x123456, 128);
    * draw.register();
    * Chat.log(`alpha ${rect.getAlpha()}, colour 0x${rect.getColor().toString(16)}`);
    * </pre>
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
    * sets only the transparency, leaving the colour alone.
    * <p>
    * The counterpart of the two-argument colour setter, and it is usually the one to
    * reach for: a rectangle is made opaque and then faded rather than built at a
    * particular alpha. As there, a value outside 0 to 255 is not clamped.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFFFFFF);
    * rect.setAlpha(128);
    * draw.register();
    * Chat.log(`alpha ${rect.getAlpha()}, colour 0x${rect.getColor().toString(16)}`);
    * </pre>
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
    * the colour, packed with the alpha in the top byte.
    * <p>
    * What the element actually holds, which is not always what was last handed to a
    * setter: a colour given with no alpha comes back opaque. The colour can also be
    * read as a plain number, so shifting out the pieces is how a script gets at the
    * red, green, blue and alpha separately.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * draw.register();
    * Chat.log(`0x${rect.getColor().toString(16)}, alpha ${rect.getAlpha()}`);
    * </pre>
    *
    * @return the color value of this rectangle.
    * @since 1.8.4
    */
    public int getColor() {
        return color;
    }

    /**
    * the transparency of this rectangle, which is the top byte of the colour.
    * <p>
    * From 0 for invisible to 255 for opaque. A colour set without an alpha reads back
    * as 255 rather than 0, unless it was pure black.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * rect.setAlpha(64);
    * draw.register();
    * Chat.log(`alpha ${rect.getAlpha()}`);
    * </pre>
    *
    * @return the alpha value of this rectangle.
    * @since 1.8.4
    */
    public int getAlpha() {
        return (color >> 24) & 0xFF;
    }

    /**
    * turns this rectangle by an angle in degrees.
    * <p>
    * The angle is folded into a single turn, so 450 reads back as 90 and -270 reads back
    * as 90 too. Whether the turn is about the middle or about a corner is
    * {@code rotateCenter}, and off on a rectangle made directly.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(40, 40, 140, 90, 0xFFFF0000);
    * rect.setRotateCenter(true).setRotation(450);
    * draw.register();
    * Chat.log(`reads back as ${rect.getRotation()}`);
    * </pre>
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
    * the rotation on this rectangle in degrees, folded into a single turn.
    * <p>
    * Between -180 and 180, because both the constructor and the setter fold the angle
    * down. The line in this package is the odd one out: its constructor folds the angle
    * but its setter does not, so a line reads back whatever was last set.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * rect.setRotation(-270);
    * draw.register();
    * Chat.log(`reads back as ${rect.getRotation()}`);
    * </pre>
    *
    * @return the rotation of this rectangle.
    * @since 1.8.4
    */
    public float getRotation() {
        return rotation;
    }

    /**
    * chooses whether the rotation is about the middle of this rectangle or about one of
    * its corners.
    * <p>
    * A rectangle made directly turns about its first corner, and a builder turns about
    * the middle, so this is what makes the two agree. Turning about the middle moves the
    * rectangle half its size across before the turn and half back after, so the corners
    * read back the same either way.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(40, 40, 140, 90, 0xFFFF0000);
    * rect.setRotateCenter(true).setRotation(45);
    * draw.register();
    * </pre>
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
    * whether the rotation on this rectangle is about its middle.
    * <p>
    * False on a rectangle made directly and true on one from a builder, since only the
    * builder sets it.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(40, 40, 140, 90, 0xFFFF0000);
    * draw.register();
    * Chat.log(`turns about the middle: ${rect.isRotatingCenter()}`);
    * </pre>
    *
    * @return {@code true} if this rectangle should be rotated around its center, {@code false}
    * otherwise.
    * @since 1.8.4
    */
    public boolean isRotatingCenter() {
        return rotateCenter;
    }

    /**
    * sets the number this rectangle is ordered against the other elements on its
    * overlay, lower drawn first.
    * <p>
    * Two rectangles drawn in the same place are ordered by this and by nothing else, and
    * a tie keeps the order they were added in.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const back = draw.addRect(0, 0, 100, 100, 0xFF000000);
    * const front = draw.addRect(0, 0, 100, 100, 0x80FFFFFF);
    * back.setZIndex(2);
    * front.setZIndex(1);
    * draw.register();
    * </pre>
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
    * the number this rectangle is ordered against the other elements on its overlay.
    * <p>
    * Zero is what a rectangle comes with, and the overlay sorts by this lowest first.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(0, 0, 100, 100, 0xFF000000);
    * rect.setZIndex(3);
    * draw.register();
    * Chat.log(`z-index ${rect.getZIndex()}`);
    * </pre>
    *
    * @return the z-index of this element
    */
    @Override
    public int getZIndex() {
        return zIndex;
    }

    /**
    * draws this rectangle, and is called by the overlay rather than by a script.
    * <p>
    * The corners are handed to the game's own fill as they stand, so a rectangle whose
    * corners are the other way round draws in the same place, and the rotation is put on
    * the matrix first. There is no scale on a rectangle, so that transform is only ever
    * a position and a turn.
    * <p>
    * The mouse arguments are not used. On a screen they are the real mouse and on a
    * plain overlay they are zero, and either way this rectangle does not look at them.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * draw.addRect(10, 10, 110, 60, 0x80000000);
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
    public void render3D(PoseStack matrixStack, MultiBufferSource consumers, int light, boolean seeThrough, float delta) {
        matrixStack.pushPose();
        matrixStack.translate(x1, y1, 0);
        if (rotateCenter) {
            matrixStack.translate(getWidth() / 2d, getHeight() / 2d, 0);
        }
        matrixStack.mulPose(new Quaternionf().rotateLocalZ((float) Math.toRadians(rotation)));
        if (rotateCenter) {
            matrixStack.translate(-getWidth() / 2d, -getHeight() / 2d, 0);
        }
        matrixStack.translate(-x1, -y1, 0);

        float brightness = RenderElement.lightBrightness(light);
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f * brightness;
        float g = ((color >> 8) & 0xFF) / 255.0f * brightness;
        float b = (color & 0xFF) / 255.0f * brightness;
        PoseStack.Pose pose = matrixStack.last();

        VertexConsumer vc = consumers.getBuffer(SurfaceRenderTypes.quads(false, !seeThrough));
        vc.addVertex(pose, x1, y1, 0).setColor(r, g, b, a);
        vc.addVertex(pose, x2, y1, 0).setColor(r, g, b, a);
        vc.addVertex(pose, x2, y2, 0).setColor(r, g, b, a);
        vc.addVertex(pose, x1, y2, 0).setColor(r, g, b, a);

        matrixStack.popPose();
    }

    /**
    * binds this rectangle to the overlay it belongs to, which is what the parent width
    * and height are then read from.
    * <p>
    * The element builders call this as they build, so a rectangle that has been built
    * is already bound. Binding is what makes the parent-relative align methods measure
    * against that overlay rather than against the window, and {@code null} puts it back
    * to the window.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.rectBuilder().pos(10, 10, 110, 60).build();
    * // a built rectangle is already bound, and can be bound again by hand
    * rect.setParent(draw);
    * draw.reAddElement(rect);
    * draw.register();
    * </pre>
    *
    * @param parent the overlay this element is on, or {@code null} for the window
    * @return self for chaining.
    */
    public Rect setParent(IDraw2D<?> parent) {
        this.parent = parent;
        return this;
    }

    /**
    * the width as it is drawn, which for a rectangle is the same as the width.
    * <p>
    * There is no scale on a rectangle, so this is exactly {@code getWidth} and it is
    * here because the align methods measure against it.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * draw.register();
    * Chat.log(`${rect.getScaledWidth()} scaled, ${rect.getWidth()} plain`);
    * </pre>
    *
    * @return the scaled width of this element
    */
    @Override
    public int getScaledWidth() {
        return getWidth();
    }

    /**
    * the width of the overlay this rectangle is on, or the window's width if it has not
    * been put on one.
    * <p>
    * This is what the parent-relative align methods measure against, so a rectangle
    * that has not been added still aligns against the screen.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * draw.register();
    * Chat.log(`measured against ${rect.getParentWidth()}`);
    * </pre>
    *
    * @return the width of the parent
    */
    @Override
    public int getParentWidth() {
        return parent != null ? parent.getWidth() : mc.getWindow().getGuiScaledWidth();
    }

    /**
    * the height as it is drawn, which for a rectangle is the same as the height.
    * <p>
    * The counterpart of {@code getScaledWidth}, and for the same reason: there is no
    * scale on a rectangle.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * draw.register();
    * Chat.log(`${rect.getScaledHeight()} scaled, ${rect.getHeight()} plain`);
    * </pre>
    *
    * @return the scaled height of this element
    */
    @Override
    public int getScaledHeight() {
        return getHeight();
    }

    /**
    * the height of the overlay this rectangle is on, or the window's height if it has
    * not been put on one.
    * <p>
    * The counterpart of {@code getParentWidth}, and read the same way.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 10, 110, 60, 0xFFFF0000);
    * draw.register();
    * Chat.log(`measured against ${rect.getParentHeight()}`);
    * </pre>
    *
    * @return the height of the parent
    */
    @Override
    public int getParentHeight() {
        return parent != null ? parent.getHeight() : mc.getWindow().getGuiScaledHeight();
    }

    /**
    * the left edge of this rectangle, which is the smaller of its two x positions.
    * <p>
    * The corners can be in either order, and this is whichever is further left, so it is
    * the same for a rectangle drawn in either direction. That is what the align methods
    * move.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(110, 60, 10, 10, 0xFFFF0000);
    * draw.register();
    * Chat.log(`left ${rect.getScaledLeft()}, right ${rect.getScaledRight()}`);
    * </pre>
    *
    * @return the position of the scaled element's left side
    */
    @Override
    public int getScaledLeft() {
        return Math.min(x1, x2);
    }

    /**
    * the top edge of this rectangle, which is the smaller of its two y positions.
    * <p>
    * The counterpart of {@code getScaledLeft}, and whichever of the two y positions is
    * further up.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(10, 60, 110, 10, 0xFFFF0000);
    * draw.register();
    * Chat.log(`top ${rect.getScaledTop()}, bottom ${rect.getScaledBottom()}`);
    * </pre>
    *
    * @return the position of the scaled element's top side
    */
    @Override
    public int getScaledTop() {
        return Math.min(y1, y2);
    }

    /**
    * puts this rectangle at a position and keeps the size it has.
    * <p>
    * The new position becomes the top left corner and the far corner is worked out from
    * the size, so moving a rectangle does not resize it. This is what the align methods
    * call, and what makes them leave the size alone.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.addRect(0, 0, 100, 50, 0xFFFF0000);
    * rect.moveTo(20, 30);
    * draw.register();
    * Chat.log(`at ${rect.getX1()}, ${rect.getY1()} and still ${rect.getWidth()} wide`);
    * </pre>
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
    * the builder for a rectangle, made by the overlay's own {@code rectBuilder}
    * method.
    * <p>
    * A rectangle here is a corner and a size rather than two corners, because that is
    * what a frame or a panel is: give the corner and the extent and the far corner is
    * worked out. Everything is named after the thing being set and hands the builder
    * back, so a whole rectangle is one chain that ends in {@code build()} or
    * {@code buildAndAdd()}.
    * <p>
    * Both corners start at zero, the colour starts opaque white and is kept apart from
    * the alpha so either can be set on its own, the rotation starts at zero and is not
    * folded down into a single turn, and the rotation is about the middle of the
    * rectangle until {@code rotateCenter} says otherwise. That last one is the opposite
    * of a rectangle made directly, so the two turn differently at the same angle.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * draw.rectBuilder().pos1(20, 20).size(100, 50).color(0x80000000).buildAndAdd();
    * draw.register();
    * </pre>
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
        * makes a builder for rectangles on one overlay.
        * <p>
        * A script does not call this directly: the overlay's own {@code rectBuilder}
        * method is what fills in the overlay.
        *
        * @param draw2D the overlay the rectangles will be added to
        */
        public Builder(IDraw2D<?> draw2D) {
            super(draw2D);
        }

        /**
        * puts the first x position, which is the corner the size is measured from.
        * <p>
        * Zero is where a rectangle starts. Setting this after a {@code width} call
        * moves the rectangle without resizing it, because the far corner stays where it
        * was.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.rectBuilder().x1(20).size(100, 50).color(0xFFFF0000).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the x position of the corner the size is measured from.
        * <p>
        * Zero until an {@code x1} or a {@code pos1} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const rect = draw.rectBuilder().pos1(20, 20).size(100, 50).color(0xFFFF0000).buildAndAdd();
        * draw.register();
        * Chat.log(`corner at ${rect.getX1()}, ${rect.getY1()}`);
        * </pre>
        *
        * @return the first x position of the rectangle.
        * @since 1.8.4
        */
        public int getX1() {
            return x1;
        }

        /**
        * puts the first y position, which is the corner the size is measured from.
        * <p>
        * The counterpart of {@code x1}, and set the same way.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.rectBuilder().y1(20).size(100, 50).color(0xFFFF0000).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the y position of the corner the size is measured from.
        * <p>
        * Zero until a {@code y1} or a {@code pos1} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const rect = draw.rectBuilder().pos1(20, 20).size(100, 50).color(0xFFFF0000).buildAndAdd();
        * draw.register();
        * Chat.log(`corner at ${rect.getX1()}, ${rect.getY1()}`);
        * </pre>
        *
        * @return the first y position of the rectangle.
        * @since 1.8.4
        */
        public int getY1() {
            return y1;
        }

        /**
        * puts the corner the size is measured from, in one call.
        * <p>
        * The counterpart of {@code pos2}, and the one to reach for when the rectangle
        * is positioned and sized: this corner plus the size is the whole rectangle.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.rectBuilder().pos1(20, 20).size(100, 50).color(0xFFFF0000).buildAndAdd();
        * draw.register();
        * </pre>
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
        * puts the second x position, which is the far corner.
        * <p>
        * On a builder the far corner is usually worked out from the size rather than
        * given, so this is for the cases where it is not.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.rectBuilder().pos1(20, 20).x2(160).color(0xFFFF0000).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the x position of the far corner.
        * <p>
        * Worked out from the corner and the size when the size is the one that was set.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const rect = draw.rectBuilder().pos1(20, 20).size(100, 50).color(0xFFFF0000).buildAndAdd();
        * draw.register();
        * Chat.log(`far corner at ${rect.getX2()}, ${rect.getY2()}`);
        * </pre>
        *
        * @return the second x position of the rectangle.
        * @since 1.8.4
        */
        public int getX2() {
            return x2;
        }

        /**
        * puts the second y position, which is the far corner.
        * <p>
        * The counterpart of {@code x2}, and for the same reason usually worked out from
        * the size instead.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.rectBuilder().pos1(20, 20).y2(90).color(0xFFFF0000).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the y position of the far corner.
        * <p>
        * Worked out from the corner and the size when the size is the one that was set.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const rect = draw.rectBuilder().pos1(20, 20).size(100, 50).color(0xFFFF0000).buildAndAdd();
        * draw.register();
        * Chat.log(`far corner at ${rect.getX2()}, ${rect.getY2()}`);
        * </pre>
        *
        * @return the second y position of the rectangle.
        * @since 1.8.4
        */
        public int getY2() {
            return y2;
        }

        /**
        * puts the far corner, leaving the first one alone.
        * <p>
        * Useful for resizing from the far side: this corner and the first one are the
        * whole rectangle, so moving this one left and up shrinks it without touching
        * where it starts.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.rectBuilder().pos1(20, 20).pos2(160, 90).color(0xFFFF0000).buildAndAdd();
        * draw.register();
        * </pre>
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
        * puts both corners, which is the same thing the rectangle itself takes.
        * <p>
        * The builder's other route is a corner and a size; this is the two-corner form
        * for when both are already known, as they are for the two edges of a frame.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.rectBuilder().pos(20, 20, 160, 90).color(0xFFFF0000).buildAndAdd();
        * draw.register();
        * </pre>
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
        * The width will just set the x2 position to {@code x1 + width}.
        * <p>
        * Which means it is always measured from {@code x1}, whichever way round the two
        * corners are, so setting this after the corners have been given the other way
        * round flips the rectangle. That is the one thing here that differs from the
        * same method on the rectangle itself, which moves whichever corner is further
        * along.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.rectBuilder().pos1(20, 20).width(100).height(50).color(0xFFFF0000).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the width this builder will build the rectangle at.
        * <p>
        * The distance between the two corners, so it is zero until one of
        * {@code pos}, {@code pos1} with a {@code size}, or {@code width} says
        * otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.rectBuilder().pos1(20, 20).size(100, 50);
        * Chat.log(`${builder.getWidth()} by ${builder.getHeight()}`);
        * </pre>
        *
        * @return the width of the rectangle.
        * @since 1.8.4
        */
        public int getWidth() {
            return Math.abs(this.x2 - this.x1);
        }

        /**
        * The width will just set the y2 position to {@code y1 + height}.
        * <p>
        * Which means it is always measured from {@code y1}, so setting this after the
        * corners have been given the other way round flips the rectangle vertically,
        * where the rectangle itself moves whichever y position is further down and so
        * can disagree. The wording above is the source comment, kept as it was written:
        * the word it used was width rather than height, which the sentence itself shows
        * is the wrong word for a method called height.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.rectBuilder().pos1(20, 20).width(100).height(50).color(0xFFFF0000).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the height this builder will build the rectangle at.
        * <p>
        * The distance between the two corners, the counterpart of {@code getWidth}.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.rectBuilder().pos1(20, 20).size(100, 50);
        * Chat.log(`${builder.getWidth()} by ${builder.getHeight()}`);
        * </pre>
        *
        * @return the height of the rectangle.
        * @since 1.8.4
        */
        public int getHeight() {
            return Math.abs(this.y2 - this.y1);
        }

        /**
        * gives the rectangle a width and a height, which are the two edges from the
        * corner.
        * <p>
        * The usual way to finish a builder: a corner with {@code pos1} and an extent with
        * this is the whole rectangle.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.rectBuilder().pos1(20, 20).size(100, 50).color(0x80000000).buildAndAdd();
        * draw.register();
        * </pre>
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
        * sets the colour from one packed number, and takes its alpha from it as well.
        * <p>
        * The alpha is read out of the number and kept separately, so a later {@code
        * alpha} call changes the transparency without touching the colour. A colour
        * with no alpha of its own comes out opaque unless it was pure black, the same
        * fix-up the rectangle itself does.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.rectBuilder().pos1(20, 20).size(100, 50).color(0x80FF0000).buildAndAdd();
        * draw.register();
        * </pre>
        *
        * @param color the color of the rectangle
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder color(int color) {
            this.color = color;
            this.alpha = ColorUtil.fixAlpha(color) >>> 24;
            return this;
        }

        /**
        * sets the colour from three components, each from 0 to 255.
        * <p>
        * The three are packed into the low 24 bits and the separate alpha is left as it
        * was, which is opaque unless something has changed it. That means the number
        * this builder reports from {@code getColor} is not quite the number the built
        * rectangle has: read straight back it is {@code 0x00RRGGBB}, and on the
        * rectangle it is {@code 0xFFRRGGBB} because the alpha is put back on when the
        * element is made.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const rect = draw.rectBuilder().pos1(20, 20).size(100, 50).color(255, 128, 0).buildAndAdd();
        * draw.register();
        * Chat.log(`built at 0x${rect.getColor().toString(16)}, alpha ${rect.getAlpha()}`);
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
        * The same three-argument packing as the shorter form, with the fourth going
        * into the separate alpha. As there, the number this builder reports is the low
        * 24 bits on their own and the built rectangle has the alpha put back on.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const rect = draw.rectBuilder().pos1(20, 20).size(100, 50).color(255, 128, 0, 128).buildAndAdd();
        * draw.register();
        * Chat.log(`built at alpha ${rect.getAlpha()}`);
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
            this.color = (r << 16) | (g << 8) | b;
            this.alpha = a;
            return this;
        }

        /**
        * sets the colour and the alpha as two separate numbers.
        * <p>
        * Both are stored as given with no fix-up, so this is the form that will hold
        * a colour with a zero alpha byte, which the one-argument form would make
        * opaque. The two are combined when the element is built.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.rectBuilder().pos1(20, 20).size(100, 50).color(0xFF0000, 128).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the colour this builder will build the rectangle at, as it is stored here.
        * <p>
        * Worth knowing which number this is. After {@code color(int)} or
        * {@code color(int, int)} it is the full packed number, because the alpha was
        * pulled out of it. After the three- or four-component forms it is the low 24
        * bits on their own, so it reads as {@code 0x00RRGGBB} and not as the
        * {@code 0xFFRRGGBB} the built rectangle ends up with. The built rectangle's own
        * {@code getColor} is the honest one.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.rectBuilder().color(255, 0, 0);
        * Chat.log(`builder says 0x${builder.getColor().toString(16)}`);
        * const rect = builder.pos1(20, 20).size(100, 50).buildAndAdd();
        * draw.register();
        * Chat.log(`the rectangle says 0x${rect.getColor().toString(16)}`);
        * </pre>
        *
        * @return the color of the rectangle.
        * @since 1.8.4
        */
        public int getColor() {
            return color;
        }

        /**
        * sets only the transparency, leaving the colour alone.
        * <p>
        * This is the one to reach for when a rectangle is made opaque and then faded.
        * The number is not clamped, so a value over 255 is a shift into the colour
        * channels and 256 is no alpha at all.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const rect = draw.rectBuilder().pos1(20, 20).size(100, 50).color(0xFF0000).alpha(128).buildAndAdd();
        * draw.register();
        * Chat.log(`built at alpha ${rect.getAlpha()}`);
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
        * the transparency this builder will build the rectangle at, from 0 to 255.
        * <p>
        * Opaque is what it starts at, and it is a number of its own here rather than
        * being read out of the colour, which is why {@code alpha} and {@code color(int,
        * int)} can each change one without disturbing the other.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.rectBuilder().color(0xFF0000).alpha(64);
        * Chat.log(`will be built at alpha ${builder.getAlpha()}`);
        * </pre>
        *
        * @return the alpha value of the color.
        * @since 1.8.4
        */
        public int getAlpha() {
            return alpha;
        }

        /**
        * turns the rectangle by an angle in degrees.
        * <p>
        * Stored as given rather than folded into a single turn, so this reads back as
        * 450 where the rectangle's own setter would report 90. The turn is about the
        * middle until {@code rotateCenter} says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.rectBuilder().pos1(20, 20).size(100, 50).rotation(45).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the rotation this builder will build the rectangle at, in degrees.
        * <p>
        * Zero until a {@code rotation} call says otherwise, and not folded into a
        * single turn: the built rectangle is made through the constructor that does
        * fold it, so a rectangle built at 450 comes out turned 90 even though this
        * reads 450.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.rectBuilder().rotation(450);
        * Chat.log(`builder says ${builder.getRotation()}`);
        * const rect = builder.pos1(20, 20).size(100, 50).buildAndAdd();
        * draw.register();
        * Chat.log(`the rectangle says ${rect.getRotation()}`);
        * </pre>
        *
        * @return the rotation (clockwise) of the rectangle in degrees.
        * @since 1.8.4
        */
        public float getRotation() {
            return rotation;
        }

        /**
        * chooses whether the rotation is about the middle of the rectangle or about a
        * corner.
        * <p>
        * True is what a builder starts at, which is the opposite of a rectangle made
        * directly, so the two turn differently until one of them is told otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const rect = draw.rectBuilder().pos1(20, 20).size(100, 50).rotateCenter(false).buildAndAdd();
        * draw.register();
        * Chat.log(`turns about a corner: ${!rect.isRotatingCenter()}`);
        * </pre>
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
        * whether the rectangle this builder builds will turn about its middle.
        * <p>
        * True until a {@code rotateCenter} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.rectBuilder();
        * Chat.log(`builds turning about the middle: ${builder.isRotatingCenter()}`);
        * </pre>
        *
        * @return {@code true} if this rectangle should be rotated around its center,
        * {@code false} otherwise.
        * @since 1.8.4
        */
        public boolean isRotatingCenter() {
            return rotateCenter;
        }

        /**
        * sets the number the rectangle will be ordered against the other elements on
        * its overlay, lower drawn first.
        * <p>
        * Zero is what a builder starts at, and a tie keeps the order the elements were
        * added in.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.rectBuilder().pos1(20, 20).size(100, 50).zIndex(5).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the z-index this builder will build the rectangle at.
        * <p>
        * Zero until a {@code zIndex} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.rectBuilder().zIndex(5);
        * Chat.log(`will be built at z-index ${builder.getZIndex()}`);
        * </pre>
        *
        * @return the z-index of the rectangle.
        * @since 1.8.4
        */
        public int getZIndex() {
            return zIndex;
        }

        /**
        * makes the rectangle out of everything set on this builder, and binds it to
        * the overlay this builder was made from.
        * <p>
        * A new rectangle each time, so the same builder can build more than one. This
        * is what {@code build} and {@code buildAndAdd} call.
        * <p>
        * The colour is put together here rather than at each setter: the separate
        * alpha becomes the top byte and the low 24 bits of the stored colour are kept,
        * which is why a builder set with the three-component form ends up opaque.
        *
        * @return the new rectangle.
        */
        @Override
        public Rect createElement() {
            return new Rect(x1, y1, x2, y2, color, alpha, rotation, zIndex).setRotateCenter(rotateCenter).setParent(
                    parent);
        }

        /**
        * the width this builder will build the rectangle at, which is the width as
        * drawn.
        * <p>
        * There is no scale on a rectangle, so this is the same number as
        * {@code getWidth}, and it is here because the align methods measure against it.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.rectBuilder().pos1(20, 20).size(100, 50);
        * Chat.log(`${builder.getScaledWidth()} scaled, ${builder.getWidth()} plain`);
        * </pre>
        *
        * @return the scaled width
        */
        @Override
        public int getScaledWidth() {
            return getWidth();
        }

        /**
        * the width of the overlay this builder was made from, which is what the
        * parent-relative align methods measure against.
        * <p>
        * There is no window fallback here, because a builder always has an overlay.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.rectBuilder();
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
        * the height this builder will build the rectangle at, which is the height as
        * drawn.
        * <p>
        * The counterpart of {@code getScaledWidth}, and the same number as
        * {@code getHeight} for the same reason.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.rectBuilder().pos1(20, 20).size(100, 50);
        * Chat.log(`${builder.getScaledHeight()} scaled, ${builder.getHeight()} plain`);
        * </pre>
        *
        * @return the scaled height
        */
        @Override
        public int getScaledHeight() {
            return getHeight();
        }

        /**
        * the height of the overlay this builder was made from, which is what the
        * parent-relative align methods measure against.
        * <p>
        * The counterpart of {@code getParentWidth}, and with no window fallback either.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.rectBuilder();
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
        * the left edge this builder will build the rectangle at, which is the smaller
        * of its two x positions.
        * <p>
        * The corners can be in either order and this is whichever is further left, the
        * same way the rectangle's own method reads.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.rectBuilder().pos1(20, 20).size(100, 50);
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
        * the top edge this builder will build the rectangle at, which is the smaller of
        * its two y positions.
        * <p>
        * The counterpart of {@code getScaledLeft}, and whichever y corner is further up.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.rectBuilder().pos1(20, 20).size(100, 50);
        * Chat.log(`top ${builder.getScaledTop()}, bottom ${builder.getScaledBottom()}`);
        * </pre>
        *
        * @return the position of the scaled element's top side
        */
        @Override
        public int getScaledTop() {
            return Math.min(y1, y2);
        }

        /**
        * puts the rectangle at a position and keeps the size it has.
        * <p>
        * The same as {@code pos(x, y, x + getWidth(), y + getHeight())}, so the new
        * position is the corner the size is measured from. This is what the align
        * methods call.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.rectBuilder().pos1(0, 0).size(100, 50);
        * builder.moveTo(40, 20);
        * builder.buildAndAdd();
        * draw.register();
        * Chat.log(`corner ${builder.getX1()}, ${builder.getY1()}`);
        * </pre>
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
