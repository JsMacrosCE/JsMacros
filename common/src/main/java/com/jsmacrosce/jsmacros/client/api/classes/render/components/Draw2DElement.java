package com.jsmacrosce.jsmacros.client.api.classes.render.components;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw2D;
import com.jsmacrosce.jsmacros.client.api.classes.render.IDraw2D;

import java.util.function.IntSupplier;

 /**
 * one 2D overlay nested inside another.
 * <p>
 * This is what a script gets for a panel: a {@link Draw2D} of its own, with a position,
 * a size and a transform of its own, drawn as one piece inside whatever overlay it was
 * added to. Anything drawn into the nested overlay is drawn in the <em>outer</em>
 * overlay's coordinates and then moved by this element's position, so a line of text at
 * {@code (5, 5)} inside a nested overlay at {@code (20, 20)} lands at {@code (25, 25)}.
 * <p>
 * Nesting moves the content and does not clip it, which is what makes it a grouping and
 * a transform rather than a container. Something drawn outside the nested overlay's own
 * width and height is still drawn, on the outer overlay.
 * <p>
 * The size is held as a supplier rather than a number, and a builder hands in one that
 * reads the overlay it came from, so a nested overlay built without a size of its own
 * follows that overlay if it is ever resized.
 * example:
 * <pre>
 * const outer = Hud.createDraw2D();
 * const panel = Hud.createDraw2D();
 * panel.addText("inside the panel", 5, 5, 0xFFFFFFFF, true);
 * // lands at 25, 25 on the outer overlay, and is not clipped to 100 by 50
 * outer.draw2DBuilder(panel).pos(20, 20).size(100, 50).buildAndAdd();
 * outer.register();
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Rendering/Graphics")
@SuppressWarnings("unused")
public class Draw2DElement implements RenderElement, Alignable<Draw2DElement> {

    /**
    * the overlay being nested, which is what gets drawn.
    * <p>
    * Final, so there is no setter and no way to swap it: a different nested overlay
    * means a different element. {@link #getDraw2D()} reads this.
    */
    public final Draw2D draw2D;
    /**
    * the overlay this element was added to, and {@code null} when it has not been put
    * on one.
    * <p>
    * This is what the parent width and height are read from, and they fall back to the
    * window when it is null. A builder sets it as it builds, so an element that was
    * only built and never added still has it.
    */
    @Nullable
    public IDraw2D<?> parent;
    /**
    * the x position of this element, which is its left edge.
    * <p>
    * This is the position before the scale is taken into account: it is where the
    * element is put, not where a scaled piece of it lands.
    */
    public int x;
    /**
    * the y position of this element, which is its top edge.
    * <p>
    * The counterpart of {@code x}, and read the same way.
    */
    public int y;
    /**
    * where the width comes from, asked again each time rather than cached.
    * <p>
    * A builder starts this off reading the overlay it came from, so an element with no
    * size of its own follows that overlay. {@link #setWidth(int)} replaces it with a
    * fixed number rather than changing this object, so anything else holding the old
    * one keeps reading the old value.
    */
    public IntSupplier width;
    /**
    * where the height comes from, asked again each time rather than cached.
    * <p>
    * The counterpart of {@code width}, and replaced the same way.
    */
    public IntSupplier height;
    /**
    * the scale applied to this element and everything drawn into it.
    * <p>
    * One is what an element comes with from a builder, and it is what the width and
    * height are multiplied by to get the scaled size. Made through the constructor
    * there is no default at all, since the scale is one of its arguments.
    */
    public float scale;
    /**
    * the rotation in degrees, read by the transform that draws this element.
    * <p>
    * Constructors and {@link #setRotation(double)} normalize finite angles to
    * {@code [-180, 180)}. Assigning directly to this field bypasses normalization.
    */
    public float rotation;
    /**
    * whether the rotation is about the middle of this element rather than about the
    * corner it is positioned by.
    * <p>
    * False on an element made directly and true on one made by a builder, so the same
    * rotation turns the two differently. There is no default written here; the builder
    * is what sets it to true.
    */
    public boolean rotateCenter;
    /**
    * the number this element is ordered against the others on its overlay, lower drawn
    * first.
    */
    public int zIndex;

    /**
    * makes a nested overlay element directly, rather than through a builder.
    * <p>
    * The width and height are suppliers rather than numbers, which is how a builder
    * ties a nested overlay's size to the overlay it came from. The rotation about the
    * middle is left off, so an element made this way rotates about its top left corner
    * unless {@link #setRotateCenter(boolean)} is called afterwards.
    *
    * @param draw2D  the overlay to nest
    * @param x       the x position of the element
    * @param y       the y position of the element
    * @param width   where the width comes from
    * @param height  where the height comes from
    * @param zIndex  the z-index against the other elements on the overlay
    * @param scale   the scale to apply
    * @param rotation the rotation in degrees
    */
    public Draw2DElement(Draw2D draw2D, int x, int y, IntSupplier width, IntSupplier height, int zIndex, float scale, float rotation) {
        this.draw2D = draw2D;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.draw2D.widthSupplier = () -> this.width.getAsInt();
        this.draw2D.heightSupplier = () -> this.height.getAsInt();
        this.zIndex = zIndex;
        this.scale = scale;
        this.rotation = Mth.wrapDegrees(rotation);
    }

    /**
    * the overlay this element is nesting, which is the one it was built around.
    * <p>
    * This is the object the element is holding rather than the element's own size or
    * position, and it is the same for the whole life of the element because there is no
    * way to swap it.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(20, 20).size(100, 50).buildAndAdd();
    * Chat.log(`nesting the overlay named ${element.getDraw2D() === panel}`);
    * outer.register();
    * </pre>
    *
    * @return the internal draw2D this draw2D element is wrapping.
    * @since 1.8.4
    */
    public Draw2D getDraw2D() {
        return draw2D;
    }

    /**
    * moves this element along the x axis and leaves the y axis alone.
    * <p>
    * The size is untouched, and so is the nested overlay, so anything drawn into it
    * moves with the element.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(0, 0).size(100, 50).buildAndAdd();
    * element.setX(40);
    * outer.register();
    * </pre>
    *
    * @param x the x position
    * @return self for chaining.
    * @since 1.8.4
    */
    public Draw2DElement setX(int x) {
        this.x = x;
        return this;
    }

    /**
    * the x position this element is drawn at, which is its left edge.
    * <p>
    * The position before the scale, so this is where the element goes rather than how
    * wide what lands there is.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(40, 20).size(100, 50).buildAndAdd();
    * Chat.log(`drawn at x ${element.getX()}`);
    * outer.register();
    * </pre>
    *
    * @return the x position of this draw2D.
    * @since 1.8.4
    */
    public int getX() {
        return x;
    }

    /**
    * moves this element along the y axis and leaves the x axis alone.
    * <p>
    * The counterpart of {@link #setX(int)}, and the size is untouched here as well.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(0, 0).size(100, 50).buildAndAdd();
    * element.setY(40);
    * outer.register();
    * </pre>
    *
    * @param y the y position
    * @return self for chaining.
    * @since 1.8.4
    */
    public Draw2DElement setY(int y) {
        this.y = y;
        return this;
    }

    /**
    * the y position this element is drawn at, which is its top edge.
    * <p>
    * The counterpart of {@link #getX()}, and read the same way.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(40, 20).size(100, 50).buildAndAdd();
    * Chat.log(`drawn at y ${element.getY()}`);
    * outer.register();
    * </pre>
    *
    * @return the y position of this draw2D.
    * @since 1.8.4
    */
    public int getY() {
        return y;
    }

    /**
    * puts this element at a position, leaving the size alone.
    * <p>
    * Unlike a rectangle, which keeps its size by working the second corner out, this
    * only writes the two positions, so the size is whatever the suppliers say and is
    * not changed by moving the element.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(0, 0).size(100, 50).buildAndAdd();
    * element.setPos(40, 20);
    * outer.register();
    * Chat.log(`${element.getWidth()} by ${element.getHeight()}, still`);
    * </pre>
    *
    * @param x the x position
    * @param y the y position
    * @return self for chaining.
    * @since 1.8.4
    */
    public Draw2DElement setPos(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /**
    * gives this element a fixed width, and stops it following anything else.
    * <p>
    * The width is held as a supplier and this replaces that supplier with one that
    * always gives this number, so an element that was following the overlay it came
    * from stops doing so. Zero is allowed and only a negative width is refused.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(0, 0).size(100, 50).buildAndAdd();
    * element.setWidth(200);
    * outer.register();
    * Chat.log(`the element is ${element.getWidth()} wide`);
    * </pre>
    *
    * @param width the width
    * @return self for chaining.
    * @throws IllegalArgumentException if the width is negative
    * @since 1.8.4
    */
    public Draw2DElement setWidth(int width) {
        if (width < 0) {
            throw new IllegalArgumentException("Width must not be negative");
        }
        this.width = () -> width;
        return this;
    }

    /**
    * the width of this element, asked from the supplier rather than from a stored
    * number.
    * <p>
    * This is the width before the scale. On an element whose supplier reads the overlay
    * it came from, this changes when that overlay does.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(0, 0).size(100, 50).buildAndAdd();
    * outer.register();
    * Chat.log(`${element.getWidth()} by ${element.getHeight()}`);
    * </pre>
    *
    * @return the width of this draw2D.
    * @since 1.8.4
    */
    public int getWidth() {
        return width.getAsInt();
    }

    /**
    * gives this element a fixed height, and stops it following anything else.
    * <p>
    * The counterpart of {@link #setWidth(int)}, and replaced the same way: the height
    * supplier is swapped for one that always gives this number.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(0, 0).size(100, 50).buildAndAdd();
    * element.setHeight(20);
    * outer.register();
    * Chat.log(`the element is ${element.getHeight()} tall`);
    * </pre>
    *
    * @param height the height
    * @return self for chaining.
    * @throws IllegalArgumentException if the height is negative
    * @since 1.8.4
    */
    public Draw2DElement setHeight(int height) {
        if (height < 0) {
            throw new IllegalArgumentException("Height  must not be negative");
        }
        this.height = () -> height;
        return this;
    }

    /**
    * the height of this element, asked from the supplier rather than from a stored
    * number.
    * <p>
    * The counterpart of {@link #getWidth()}, and read the same way.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(0, 0).size(100, 50).buildAndAdd();
    * outer.register();
    * Chat.log(`the element is ${element.getWidth()} by ${element.getHeight()}`);
    * </pre>
    *
    * @return the height of this draw2D.
    * @since 1.8.4
    */
    public int getHeight() {
        return height.getAsInt();
    }

    /**
    * gives this element a fixed width and a fixed height in one call.
    * <p>
    * The width is set first, so a negative height is refused with the width already
    * changed. Zero is allowed on both axes and only a negative number is refused.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(0, 0).buildAndAdd();
    * element.setSize(120, 60);
    * outer.register();
    * </pre>
    *
    * @param width  the width
    * @param height the height
    * @return self for chaining.
    * @throws IllegalArgumentException if the width or the height is negative
    * @since 1.8.4
    */
    public Draw2DElement setSize(int width, int height) {
        return setWidth(width).setHeight(height);
    }

    /**
    * scales this element and everything drawn into it.
    * <p>
    * A scale of one is what an element comes with, and the number is kept as a float
    * even though it is given as a double, so a scale that is not exactly
    * representable is rounded to the nearest one rather than kept.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(0, 0).size(50, 50).buildAndAdd();
    * element.setScale(2);
    * outer.register();
    * Chat.log(`drawn at twice the size, ${element.getScaledWidth()} wide`);
    * </pre>
    *
    * @param scale the scale
    * @return self for chaining.
    * @throws IllegalArgumentException if the scale is zero or negative
    * @since 1.8.4
    */
    public Draw2DElement setScale(double scale) {
        if (scale <= 0) {
            throw new IllegalArgumentException("Scale must be greater than 0");
        }
        this.scale = (float) scale;
        return this;
    }

    /**
    * the scale on this element, which is what the scaled size is worked out from.
    * <p>
    * One is what an element comes with, and the builder's own default as well.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(0, 0).size(50, 50).buildAndAdd();
    * element.setScale(1.5);
    * outer.register();
    * Chat.log(`scale ${element.getScale()}, drawn ${element.getScaledWidth()} wide`);
    * </pre>
    *
    * @return the scale of this draw2D.
    * @since 1.8.4
    */
    public float getScale() {
        return scale;
    }

    /**
    * turns this element, and everything drawn into it, by an angle in degrees.
    * <p>
    * Finite angles are normalized to {@code [-180, 180)}, so setting 450 stores 90.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(20, 20).size(50, 50).buildAndAdd();
    * element.setRotateCenter(true).setRotation(45);
    * outer.register();
    * </pre>
    *
    * @param rotation the rotation
    * @return self for chaining.
    * @since 1.8.4
    */
    public Draw2DElement setRotation(double rotation) {
        this.rotation = Mth.wrapDegrees((float) rotation);
        return this;
    }

    /**
    * the rotation on this element, in degrees, as it was last set.
    * <p>
    * Constructors and setters normalize finite angles to {@code [-180, 180)}, so setting
    * 450 reads back as 90. Direct writes to the public field bypass that normalization.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(20, 20).size(50, 50).buildAndAdd();
    * element.setRotation(450);
    * outer.register();
    * Chat.log(`reads back as ${element.getRotation()}`);
    * </pre>
    *
    * @return the rotation of this draw2D.
    * @since 1.8.4
    */
    public float getRotation() {
        return rotation;
    }

    /**
    * chooses whether the rotation is about the middle of this element or about the
    * corner it is positioned by.
    * <p>
    * A builder turns this on by default and an element made directly has it off, so the
    * two kinds of element turn differently at the same angle. Turning it on moves the
    * element half its size before the rotation and half back after, which is why the
    * position reads back the same either way.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(20, 20).size(50, 50).buildAndAdd();
    * element.setRotation(90).setRotateCenter(true);
    * outer.register();
    * </pre>
    *
    * @param rotateCenter whether this draw2D should be rotated around its center
    * @return self for chaining.
    * @since 1.8.4
    */
    public Draw2DElement setRotateCenter(boolean rotateCenter) {
        this.rotateCenter = rotateCenter;
        return this;
    }

    /**
    * whether the rotation on this element is about its middle rather than about its
    * top left corner.
    * <p>
    * True on an element from a builder and false on one made directly, since only the
    * builder sets it.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(20, 20).size(50, 50).buildAndAdd();
    * outer.register();
    * Chat.log(`built to turn about the middle: ${element.isRotatingCenter()}`);
    * </pre>
    *
    * @return {@code true} if this draw2D should be rotated around its center, {@code false}
    * otherwise.
    * @since 1.8.4
    */
    public boolean isRotatingCenter() {
        return rotateCenter;
    }

    /**
    * sets the number this element is ordered against the other elements on its
    * overlay, lower drawn first.
    * <p>
    * A nested overlay is ordered as one piece, so this moves the whole of it rather
    * than anything drawn inside it.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(0, 0).size(100, 50).buildAndAdd();
    * element.setZIndex(5);
    * outer.register();
    * </pre>
    *
    * @param zIndex the z-index of this draw2D
    * @return self for chaining.
    * @since 1.8.4
    */
    public Draw2DElement setZIndex(int zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    /**
    * the number this element is ordered against the others on its overlay.
    * <p>
    * Zero is what an element comes with. The overlay sorts by this lowest first and a
    * tie keeps the order the elements were added in.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(0, 0).size(100, 50).buildAndAdd();
    * element.setZIndex(5);
    * outer.register();
    * Chat.log(`z-index ${element.getZIndex()}`);
    * </pre>
    *
    * @return the z-index of this element
    */
    @Override
    public int getZIndex() {
        return zIndex;
    }

    /**
    * draws the nested overlay with this element's own transform on it, and is called by
    * the overlay rather than by a script.
    * <p>
    * The element is moved to its position and scaled, turned about either its middle or
    * its top left corner, and then the nested overlay draws all of its own elements
    * into that. The mouse arguments are ignored, as they are on every element in this
    * package, and the nested overlay inside it keeps drawing its own children at the
    * outer overlay's coordinates.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * panel.addText("inside", 5, 5, 0xFFFFFFFF, true);
    * // the overlay calls render on the element, and it on the nested overlay
    * outer.draw2DBuilder(panel).pos(20, 20).size(100, 50).buildAndAdd();
    * outer.register();
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
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        if (rotateCenter) {
            matrices.translate(width.getAsInt() / 2f, height.getAsInt() / 2f);
        }
        matrices.rotate((float) Math.toRadians(rotation));
        if (rotateCenter) {
            matrices.translate(-width.getAsInt() / 2f, -height.getAsInt() / 2f);
        }
        //don't translate back
        draw2D.render(drawContext);
        matrices.popMatrix();
        //?} else {
        /*PoseStack matrices = drawContext.pose();
        matrices.pushPose();
        matrices.translate(x, y, 0);
        matrices.scale(scale, scale, 1);
        if (rotateCenter) {
            matrices.translate(width.getAsInt() / 2d, height.getAsInt() / 2d, 0);
        }
        matrices.mulPose(new Quaternionf().rotateLocalZ((float) Math.toRadians(rotation)));
        if (rotateCenter) {
            matrices.translate(-width.getAsInt() / 2d, -height.getAsInt() / 2d, 0);
        }
        //don't translate back
        draw2D.render(drawContext);
        matrices.popPose();
        *///?}
    }

    /**
    * binds this element to the overlay it belongs to, which is what the parent width
    * and height are then read from.
    * <p>
    * The element builders call this as they build, so an element that has been built
    * is already bound. Binding is what makes the parent-relative align methods measure
    * against that overlay rather than against the window; {@code null} puts it back to
    * the window.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(0, 0).size(100, 50).build();
    * // a built element is already bound, and can be bound again by hand
    * element.setParent(outer);
    * outer.reAddElement(element);
    * outer.register();
    * </pre>
    *
    * @param parent the overlay this element is on, or {@code null} for the window
    * @return self for chaining.
    */
    public Draw2DElement setParent(IDraw2D<?> parent) {
        this.parent = parent;
        return this;
    }

    /**
    * the width of this element once the scale is applied, which is the width it is
    * drawn at.
    * <p>
    * The plain width multiplied by the scale and then cut to a whole number of pixels,
    * and the cut is toward zero, so a positive scale rounds down and a negative one
    * rounds up. A negative one is reachable here even though {@code setScale} refuses
    * anything at or below zero, because {@code scale} is a public field and the
    * constructor takes one. This is what the align methods measure.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(0, 0).size(50, 50).buildAndAdd();
    * element.setScale(1.5);
    * outer.register();
    * Chat.log(`drawn ${element.getScaledWidth()} wide, set at ${element.getWidth()}`);
    * </pre>
    *
    * @return the scaled width of this element
    */
    @Override
    public int getScaledWidth() {
        return (int) (width.getAsInt() * scale);
    }

    /**
    * the width of the overlay this element is on, or the window's width if it has not
    * been put on one.
    * <p>
    * This is what the parent-relative align methods measure against, so an element that
    * has not been added still aligns against the screen.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(0, 0).size(50, 50).buildAndAdd();
    * outer.register();
    * Chat.log(`measured against ${element.getParentWidth()} by ${element.getParentHeight()}`);
    * </pre>
    *
    * @return the width of the parent
    */
    @Override
    public int getParentWidth() {
        return parent != null ? parent.getWidth() : mc.getWindow().getGuiScaledWidth();
    }

    /**
    * the height of this element once the scale is applied, which is the height it is
    * drawn at.
    * <p>
    * The plain height multiplied by the scale and then cut to a whole number of pixels,
    * the same way as the width.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(0, 0).size(50, 50).buildAndAdd();
    * element.setScale(1.5);
    * outer.register();
    * Chat.log(`drawn ${element.getScaledHeight()} tall, set at ${element.getHeight()}`);
    * </pre>
    *
    * @return the scaled height of this element
    */
    @Override
    public int getScaledHeight() {
        return (int) (height.getAsInt() * scale);
    }

    /**
    * the height of the overlay this element is on, or the window's height if it has
    * not been put on one.
    * <p>
    * The counterpart of {@link #getParentWidth()}, and read the same way.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(0, 0).size(50, 50).buildAndAdd();
    * outer.register();
    * Chat.log(`measured against ${element.getParentWidth()} by ${element.getParentHeight()}`);
    * </pre>
    *
    * @return the height of the parent
    */
    @Override
    public int getParentHeight() {
        return parent != null ? parent.getHeight() : mc.getWindow().getGuiScaledHeight();
    }

    /**
    * the left edge of this element, which is its x position.
    * <p>
    * The scale does not move the element, only makes what is drawn inside it bigger, so
    * this is the same as {@link #getX()}.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(40, 20).size(50, 50).buildAndAdd();
    * outer.register();
    * Chat.log(`left edge ${element.getScaledLeft()}, right edge ${element.getScaledRight()}`);
    * </pre>
    *
    * @return the position of the scaled element's left side
    */
    @Override
    public int getScaledLeft() {
        return x;
    }

    /**
    * the top edge of this element, which is its y position.
    * <p>
    * The counterpart of {@link #getScaledLeft()}, and the same as {@link #getY()}.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(40, 20).size(50, 50).buildAndAdd();
    * outer.register();
    * Chat.log(`top edge ${element.getScaledTop()}, bottom edge ${element.getScaledBottom()}`);
    * </pre>
    *
    * @return the position of the scaled element's top side
    */
    @Override
    public int getScaledTop() {
        return y;
    }

    /**
    * puts this element at a position, which is the same as {@link #setPos(int, int)}.
    * <p>
    * Unlike a rectangle, nothing is worked out from the size, so the size survives
    * being moved.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const element = outer.draw2DBuilder(panel).pos(0, 0).size(100, 50).buildAndAdd();
    * element.moveTo(40, 20);
    * outer.register();
    * Chat.log(`moved to ${element.getX()}, ${element.getY()} and still ${element.getWidth()} wide`);
    * </pre>
    *
    * @param x the new x position
    * @param y the new y position
    * @return self for chaining.
    */
    @Override
    public Draw2DElement moveTo(int x, int y) {
        return setPos(x, y);
    }

    /**
    * the builder for a nested overlay element.
    * <p>
    * A script makes one through the overlay's own {@code draw2DBuilder} method, which
    * is what fills in the two arguments here. Every method on it is named after the
    * thing being set and hands the builder back, so a whole element is one chain that
    * ends in {@code build()} or {@code buildAndAdd()}.
    * <p>
    * The width and height start off reading the overlay this builder was made from, so
    * a builder given no size of its own produces an element the same size as the
    * overlay it is going on. Zero is the starting position and one is the starting
    * scale, the rotation starts at zero, and the rotation is about the middle of the
    * element until {@code rotateCenter} says otherwise.
    * <p>
    * Before building, the nested overlay reads this builder's current dimension suppliers.
    * Building installs suppliers that read the built element's current width and height, so
    * subsequent element resizing also changes the size reported by the child. Use a separate
    * child Draw2D for each independently sized wrapper: a child owns only one pair of suppliers.
    * example:
    * <pre>
    * const outer = Hud.createDraw2D();
    * const panel = Hud.createDraw2D();
    * const builder = outer.draw2DBuilder(panel);
    * Chat.log(`starting at ${builder.getWidth()} by ${builder.getHeight()}, the outer size`);
    * builder.pos(20, 20).size(100, 50);
    * builder.buildAndAdd();
    * outer.register();
    * </pre>
    *
    * @author Etheradon
    * @since 1.8.4
    */
    public static class Builder extends RenderElementBuilder<Draw2DElement> implements Alignable<Builder> {
        private final Draw2D draw2D;
        private int x = 0;
        private int y = 0;
        private IntSupplier width;
        private IntSupplier height;
        private float scale = 1;
        private float rotation = 0;
        private boolean rotateCenter = true;
        private int zIndex = 0;

        /**
        * makes a builder bound to one overlay, nesting another inside it.
        * <p>
        * The size is started off reading the overlay this was made from and the nested
        * overlay follows the builder's current readers, including later {@code width} or
        * {@code size} calls. After building, the child follows the built element's dimensions.
        *
        * @param parent the overlay this element is going on
        * @param draw2D the overlay to nest
        */
        public Builder(IDraw2D<?> parent, Draw2D draw2D) {
            super(parent);
            this.draw2D = draw2D;
            this.width = parent::getWidth;
            this.height = parent::getHeight;
            this.draw2D.widthSupplier = () -> this.width.getAsInt();
            this.draw2D.heightSupplier = () -> this.height.getAsInt();
        }

        /**
        * @param x the x position of the draw2D
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder x(int x) {
            this.x = x;
            return this;
        }

        /**
        * @return the x position of the draw2D.
        * @since 1.8.4
        */
        public int getX() {
            return x;
        }

        /**
        * @param y the y position of the draw2D
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder y(int y) {
            this.y = y;
            return this;
        }

        /**
        * @return the y position of the draw2D.
        * @since 1.8.4
        */
        public int getY() {
            return y;
        }

        /**
        * @param x the x position of the draw2D
        * @param y the y position of the draw2D
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        /**
        * @param width the width of the draw2D
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder width(int width) {
            if (width < 0) {
                throw new IllegalArgumentException("Width  must not be negative");
            }
            this.width = () -> width;
            return this;
        }

        /**
        * @return the width of the draw2D.
        * @since 1.8.4
        */
        public int getWidth() {
            return width.getAsInt();
        }

        /**
        * @param height the height of the draw2D
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder height(int height) {
            if (height < 0) {
                throw new IllegalArgumentException("Height  must not be negative");
            }
            this.height = () -> height;
            return this;
        }

        /**
        * @return the height of the draw2D.
        * @since 1.8.4
        */
        public int getHeight() {
            return height.getAsInt();
        }

        /**
        * @param width  the width of the draw2D
        * @param height the height of the draw2D
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder size(int width, int height) {
            return width(width).height(height);
        }

        /**
        * @param scale the scale of the draw2D
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder scale(double scale) {
            this.scale = (float) scale;
            return this;
        }

        /**
        * @return the scale of the draw2D.
        * @since 1.8.4
        */
        public float getScale() {
            return scale;
        }

        /**
        * @param rotation the rotation (clockwise) of the draw2D in degrees
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder rotation(double rotation) {
            this.rotation = (float) rotation;
            return this;
        }

        /**
        * @return the rotation (clockwise) of the draw2D in degrees.
        * @since 1.8.4
        */
        public float getRotation() {
            return rotation;
        }

        /**
        * @param rotateCenter whether this draw2D should be rotated around its center
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder rotateCenter(boolean rotateCenter) {
            this.rotateCenter = rotateCenter;
            return this;
        }

        /**
        * @return {@code true} if this draw2D should be rotated around its center,
        * {@code false} otherwise.
        * @since 1.8.4
        */
        public boolean isRotatingCenter() {
            return rotateCenter;
        }

        /**
        * @return the z-index of the draw2D.
        * @since 1.8.4
        */
        public Builder zIndex(int zIndex) {
            this.zIndex = zIndex;
            return this;
        }

        /**
        * @return the z-index of the draw2D.
        * @since 1.8.4
        */
        public int getZIndex() {
            return zIndex;
        }

        /**
        * makes the element out of everything set on this builder, and binds it to the
        * overlay this builder was made from.
        * <p>
        * A new element each time, so the same builder can build more than one and each
        * one is separate. This is what {@code build} and {@code buildAndAdd} call.
        *
        * @return the new element.
        */
        @Override
        protected Draw2DElement createElement() {
            return new Draw2DElement(draw2D, x, y, width, height, zIndex, scale, rotation).setRotateCenter(rotateCenter)
                    .setParent(parent);
        }

        /**
        * the width the element will be drawn at, which is the width put through the
        * scale.
        * <p>
        * Cut to a whole number of pixels, and the cut is toward zero, so a positive
        * scale rounds down and a negative one rounds up. A negative one can reach here
        * because this builder's {@code scale} takes any number, where the builders on
        * {@link Text} and {@link Item} refuse anything at or below zero.
        * example:
        * <pre>
        * const outer = Hud.createDraw2D();
        * const builder = outer.draw2DBuilder(Hud.createDraw2D()).size(50, 50).scale(1.5);
        * Chat.log(`will be drawn ${builder.getScaledWidth()} wide`);
        * </pre>
        *
        * @return the scaled width
        */
        @Override
        public int getScaledWidth() {
            return (int) (width.getAsInt() * scale);
        }

        /**
        * the width of the overlay this builder was made from, which is what the
        * parent-relative align methods measure against.
        * <p>
        * There is no window fallback here: a builder always has an overlay, which is
        * what the overlay's own builder methods pass in.
        * example:
        * <pre>
        * const outer = Hud.createDraw2D();
        * const builder = outer.draw2DBuilder(Hud.createDraw2D());
        * Chat.log(`measuring against ${builder.getParentWidth()} by ${builder.getParentHeight()}`);
        * </pre>
        *
        * @return the width of the parent
        */
        @Override
        public int getParentWidth() {
            return parent.getWidth();
        }

        /**
        * the height the element will be drawn at, which is the height put through the
        * scale.
        * <p>
        * The counterpart of {@code getScaledWidth}, and cut to a whole number of pixels
        * the same way.
        * example:
        * <pre>
        * const outer = Hud.createDraw2D();
        * const builder = outer.draw2DBuilder(Hud.createDraw2D()).size(50, 50).scale(1.5);
        * Chat.log(`will be drawn ${builder.getScaledHeight()} tall`);
        * </pre>
        *
        * @return the scaled height
        */
        @Override
        public int getScaledHeight() {
            return (int) (height.getAsInt() * scale);
        }

        /**
        * the height of the overlay this builder was made from, which is what the
        * parent-relative align methods measure against.
        * <p>
        * The counterpart of {@code getParentWidth}, and with no window fallback either.
        * example:
        * <pre>
        * const outer = Hud.createDraw2D();
        * const builder = outer.draw2DBuilder(Hud.createDraw2D());
        * Chat.log(`measuring against ${builder.getParentWidth()} by ${builder.getParentHeight()}`);
        * </pre>
        *
        * @return the height of the parent
        */
        @Override
        public int getParentHeight() {
            return parent.getHeight();
        }

        /**
        * the left edge the element will be at, which is its x position.
        * <p>
        * The scale makes what is inside the element bigger rather than moving it, so
        * this is the same as {@code getX}.
        * example:
        * <pre>
        * const outer = Hud.createDraw2D();
        * const builder = outer.draw2DBuilder(Hud.createDraw2D()).pos(40, 20);
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
        * the top edge the element will be at, which is its y position.
        * <p>
        * The counterpart of {@code getScaledLeft}, and the same as {@code getY}.
        * example:
        * <pre>
        * const outer = Hud.createDraw2D();
        * const builder = outer.draw2DBuilder(Hud.createDraw2D()).pos(40, 20);
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
        * puts the element at a position, which is the same as {@code pos}.
        * <p>
        * Nothing is worked out from the size, so the size survives being moved.
        * example:
        * <pre>
        * const outer = Hud.createDraw2D();
        * const panel = Hud.createDraw2D();
        * const builder = outer.draw2DBuilder(panel).size(100, 50);
        * builder.moveTo(40, 20);
        * builder.buildAndAdd();
        * outer.register();
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
