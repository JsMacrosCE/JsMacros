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
  * Draws a {@link Draw2D} as a positioned, scaled, and rotated element of another overlay.
 * <p>
  * Its nested overlay is transformed as one element; content outside its width or height
  * is not clipped. Dimensions default to suppliers that follow the builder's parent.
 * <p>
  * <pre>
 * const outer = Hud.createDraw2D();
 * const panel = Hud.createDraw2D();
 * panel.addText("inside the panel", 5, 5, Color.rgb(0xFF, 0xFF, 0xFF), true);
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
    * The overlay rendered by this element.
    */
    public final Draw2D draw2D;
    /**
    * The parent overlay used for parent-size queries, or {@code null} to use the
    * window's GUI-scaled dimensions. Builders set this when creating the element.
    */
    @Nullable
    public IDraw2D<?> parent;
    /**
    * The element's x position; scaling is applied after translation.
    */
    public int x;
    /**
    * The element's y position; scaling is applied after translation.
    */
    public int y;
    /**
    * Supplies the element's width. The builder's default supplier follows its parent;
    * {@link #setWidth(int)} replaces the supplier with a fixed value.
    */
    public IntSupplier width;
    /**
    * Supplies the element's height; its default and replacement behavior matches {@link #width}.
    */
    public IntSupplier height;
    /**
    * The scale applied to the element and nested content. Builders default it to {@code 1}.
    */
    public float scale;
    /**
    * The rotation in degrees. Constructors and {@link #setRotation(double)} normalize angles to
    * {@code [-180, 180)}. Assigning directly to this field bypasses normalization.
    */
    public float rotation;
    /**
    * Whether rotation is about the element's center rather than its positioned corner.
    * Direct construction defaults to {@code false}; builders default to {@code true}.
    */
    public boolean rotateCenter;
    /**
    * This element's ordering value on its overlay; lower values are drawn first.
    */
    public int zIndex;

    /**
    * Creates an element nesting the specified overlay. Rotation is about the positioned
    * corner by default; use {@link #setRotateCenter(boolean)} to rotate about its center.
    *
    * @param draw2D the overlay to nest
    * @param x the x position
    * @param y the y position
    * @param width the width supplier
    * @param height the height supplier
    * @param zIndex the ordering value on the parent overlay
    * @param scale the scale to apply
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
    * Returns the overlay nested by this element.
    *
    * @return the nested overlay
    * @since 1.8.4
    */
    public Draw2D getDraw2D() {
        return draw2D;
    }

    /**
    * Sets the x position without changing the y position or dimensions.
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
    * Returns the element's x position, which scaling does not change.
    *
    * @return the x position
    * @since 1.8.4
    */
    public int getX() {
        return x;
    }

    /**
    * Sets the y position without changing the x position or dimensions.
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
    * Returns the element's y position, which scaling does not change.
    *
    * @return the y position
    * @since 1.8.4
    */
    public int getY() {
        return y;
    }

    /**
    * Sets the element's position without changing its dimensions.
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
    * Sets a fixed width, replacing any supplier that tracks another overlay. Zero is
    * allowed; negative values throw {@link IllegalArgumentException}.
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
    * Returns the current width from its supplier, before scaling. A supplier tied to
    * another overlay reflects that overlay's current width.
    *
    * @return the current width
    * @since 1.8.4
    */
    public int getWidth() {
        return width.getAsInt();
    }

    /**
    * Sets a fixed height, replacing any supplier that tracks another overlay.
    * Zero is allowed; negative values throw {@link IllegalArgumentException}.
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
    * Returns the current height from this element's height supplier.
    *
    * @return the height of this draw2D.
    * @since 1.8.4
    */
    public int getHeight() {
        return height.getAsInt();
    }

    /**
    * Sets fixed width and height. Zero is allowed; negative dimensions throw
    * {@link IllegalArgumentException}. Width is set before height, so a negative
    * height leaves the width changed.
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
    * Sets the scale applied to this element and its nested overlay. The value is
    * narrowed to {@code float}; zero and negative values throw
    * {@link IllegalArgumentException}.
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
    * Returns the scale used to calculate the element's scaled dimensions.
    *
    * @return the scale of this draw2D.
    * @since 1.8.4
    */
    public float getScale() {
        return scale;
    }

    /**
    * Sets the rotation in degrees; the value is narrowed to {@code float} and
    * normalized to {@code [-180, 180)}.
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
    * Returns the rotation in degrees. Constructors and setters normalize the value;
    * direct writes to the public field do not.
    *
    * @return the rotation of this draw2D.
    * @since 1.8.4
    */
    public float getRotation() {
        return rotation;
    }

    /**
    * Sets whether rotation is about the element's center rather than its positioned
    * corner. Builders enable this by default; directly constructed elements do not.
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
    * Returns whether rotation is about the element's center. Builders enable this
    * by default; directly constructed elements do not.
    *
    * @return {@code true} if this draw2D should be rotated around its center, {@code false}
    * otherwise.
    * @since 1.8.4
    */
    public boolean isRotatingCenter() {
        return rotateCenter;
    }

    /**
    * Sets this element's ordering value relative to other elements on its overlay;
    * lower values are drawn first. A nested overlay is ordered as a single element.
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
    * Returns this element's ordering value on its overlay. Lower values draw first;
    * newly constructed elements default to zero.
    *
    * @return the z-index of this element
    */
    @Override
    public int getZIndex() {
        return zIndex;
    }

    /**
    * Renders the nested overlay under this element's position, scale, and rotation
    * transform. Rotation is about the element's center when {@code rotateCenter} is
    * enabled, and otherwise about its positioned corner. Mouse coordinates and frame
    * delta are unused.
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
    * Sets the overlay used as this element's parent for parent-size queries. Builders
    * set it when constructing the element; passing {@code null} makes those queries
    * use the window's GUI-scaled dimensions.
    *
    * @param parent the overlay this element is on, or {@code null} for the window
    * @return self for chaining.
    */
    public Draw2DElement setParent(IDraw2D<?> parent) {
        this.parent = parent;
        return this;
    }

    /**
    * Returns the element width multiplied by its scale and cast to an integer (truncated
    * toward zero). Alignment calculations use this scaled width.
    *
    * @return the scaled width of this element
    */
    @Override
    public int getScaledWidth() {
        return (int) (width.getAsInt() * scale);
    }

    /**
    * Returns the parent overlay's width, or the window's GUI-scaled width when no parent
    * is set.
    *
    * @return the width of the parent
    */
    @Override
    public int getParentWidth() {
        return parent != null ? parent.getWidth() : mc.getWindow().getGuiScaledWidth();
    }

    /**
    * Returns the element height multiplied by its scale and cast to an integer
    * (truncated toward zero).
    *
    * @return the scaled height of this element
    */
    @Override
    public int getScaledHeight() {
        return (int) (height.getAsInt() * scale);
    }

    /**
    * Returns the parent overlay's height, or the window's GUI-scaled height when no
    * parent is set.
    *
    * @return the height of the parent
    */
    @Override
    public int getParentHeight() {
        return parent != null ? parent.getHeight() : mc.getWindow().getGuiScaledHeight();
    }

    /**
    * Returns this element's x position; scaling does not change the left edge.
    *
    * @return the position of the scaled element's left side
    */
    @Override
    public int getScaledLeft() {
        return x;
    }

    /**
    * Returns this element's y position; scaling does not change the top edge.
    *
    * @return the position of the scaled element's top side
    */
    @Override
    public int getScaledTop() {
        return y;
    }

    /**
    * Sets this element's position without changing its size.
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
    * Builds a nested overlay element with configurable position, dimensions, scale, rotation, and draw order.
    * <p>
    * Dimensions initially follow the parent overlay; position defaults to {@code (0, 0)},
    * scale to {@code 1}, rotation to {@code 0}, and rotation center to the element center.
    * The nested overlay reads the builder's dimension suppliers, then follows the built
    * element's dimensions after construction. A child overlay has only one pair of dimension
    * suppliers, so do not use it for multiple independently sized wrappers.
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
        * Creates a builder that nests {@code draw2D} in {@code parent}. Dimensions initially
        * follow the parent; the child overlay follows the builder's dimensions and then the
        * built element's dimensions.
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
        * Sets the horizontal position.
        *
        * @param x the x position
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder x(int x) {
            this.x = x;
            return this;
        }

        /**
        * @return the horizontal position
        * @since 1.8.4
        */
        public int getX() {
            return x;
        }

        /**
        * Sets the vertical position.
        *
        * @param y the y position
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder y(int y) {
            this.y = y;
            return this;
        }

        /**
        * @return the vertical position
        * @since 1.8.4
        */
        public int getY() {
            return y;
        }

        /**
        * Sets both position coordinates.
        *
        * @param x the x position
        * @param y the y position
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        /**
        * Sets the width; negative values are rejected.
        *
        * @param width the width
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
        * @return the current width from the dimension supplier
        * @since 1.8.4
        */
        public int getWidth() {
            return width.getAsInt();
        }

        /**
        * Sets the height; negative values are rejected.
        *
        * @param height the height
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
        * Returns the builder's current height, read from its dimension supplier.
        *
        * @return the current height
        * @since 1.8.4
        */
        public int getHeight() {
            return height.getAsInt();
        }

        /**
        * Sets both dimensions; negative values are rejected by the corresponding setters.
        *
        * @param width the width
        * @param height the height
        * @return this builder
        * @since 1.8.4
        */
        public Builder size(int width, int height) {
            return width(width).height(height);
        }

        /**
        * Sets the scale applied to the nested overlay. The value is narrowed to {@code float}.
        *
        * @param scale the scale
        * @return this builder
        * @since 1.8.4
        */
        public Builder scale(double scale) {
            this.scale = (float) scale;
            return this;
        }

        /**
        * Returns the scale applied to the nested overlay.
        *
        * @return the current scale
        * @since 1.8.4
        */
        public float getScale() {
            return scale;
        }

        /**
        * Sets the rotation in degrees. The value is narrowed to {@code float} without normalization.
        *
        * @param rotation the rotation in degrees
        * @return this builder
        * @since 1.8.4
        */
        public Builder rotation(double rotation) {
            this.rotation = (float) rotation;
            return this;
        }

        /**
        * Returns the configured rotation in degrees; builder values are not normalized.
        *
        * @return the current rotation
        * @since 1.8.4
        */
        public float getRotation() {
            return rotation;
        }

        /**
        * Sets whether rotation is about the element's center rather than its positioned corner.
        *
        * @param rotateCenter {@code true} to rotate about the center
        * @return this builder
        * @since 1.8.4
        */
        public Builder rotateCenter(boolean rotateCenter) {
            this.rotateCenter = rotateCenter;
            return this;
        }

        /**
        * Returns whether rotation is about the element's center.
        *
        * @return {@code true} if rotation is about the center
        * @since 1.8.4
        */
        public boolean isRotatingCenter() {
            return rotateCenter;
        }

        /**
        * Sets the ordering value used relative to other elements; lower values draw first.
        *
        * @param zIndex the ordering value
        * @return this builder
        * @since 1.8.4
        */
        public Builder zIndex(int zIndex) {
            this.zIndex = zIndex;
            return this;
        }

        /**
        * Returns the configured ordering value; lower values draw first.
        *
        * @return the ordering value
        * @since 1.8.4
        */
        public int getZIndex() {
            return zIndex;
        }

        /**
        * Creates an element from this builder's settings and associates it with the
        * overlay used to create the builder. Each call creates a distinct element.
        *
        * @return the new element.
        */
        @Override
        protected Draw2DElement createElement() {
            return new Draw2DElement(draw2D, x, y, width, height, zIndex, scale, rotation).setRotateCenter(rotateCenter)
                    .setParent(parent);
        }

        /**
        * Returns the width multiplied by the builder's scale and converted to an
        * integer by Java's cast (truncation toward zero).
        *
        * @return the scaled width
        */
        @Override
        public int getScaledWidth() {
            return (int) (width.getAsInt() * scale);
        }

        /**
        * Returns the width of the overlay used to create this builder, which
        * parent-relative alignment uses as its reference.
        *
        * @return the width of the parent
        */
        @Override
        public int getParentWidth() {
            return parent.getWidth();
        }

        /**
        * Returns the height multiplied by the builder's scale and converted to an
        * integer by Java's cast (truncation toward zero).
        *
        * @return the scaled height
        */
        @Override
        public int getScaledHeight() {
            return (int) (height.getAsInt() * scale);
        }

        /**
        * Returns the height of the overlay used to create this builder, which
        * parent-relative alignment uses as its reference.
        *
        * @return the height of the parent
        */
        @Override
        public int getParentHeight() {
            return parent.getHeight();
        }

        /**
        * Returns the element's x position; scaling does not change this position.
        *
        * @return the position of the scaled element's left side
        */
        @Override
        public int getScaledLeft() {
            return x;
        }

        /**
        * Returns the element's y position; scaling does not change this position.
        *
        * @return the position of the scaled element's top side
        */
        @Override
        public int getScaledTop() {
            return y;
        }

        /**
        * Sets the element's position without changing its size.
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
