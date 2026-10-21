package com.jsmacrosce.jsmacros.client.api.helper.screen;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.client.api.classes.render.components.Alignable;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;

/**
 * Base builder for screen widgets, storing their position, size, label, state flags,
 * alpha, and z-index until {@link #build()} creates and adds a widget to its screen.
 * Builders are obtained from their screen and setters return the builder for chaining.
 * Defaults are position and width zero, height 20, empty label, active and visible,
 * alpha 1.0, and z-index zero. Subclasses may constrain properties; for example,
 * button builders fix height at 20 and text-field builders use the screen font.
 * <p>
 * <pre>
 * const screen = Hud.createScreen("a screen", true);
 * // the shared half on its own: a label, a box and the flags
 * screen.buttonBuilder()
 *    .pos(10, 20)
 *    .size(100, 20)
 *    .message("a button")
 *    .visible(true)
 *    .active(true)
 *    .alpha(0.8)
 *    .zIndex(5)
 *    .build();
 * Hud.openScreen(screen);
 * </pre>
 *
 * @param <B> the builder class itself, so a chain keeps its own type
 * @param <T> the kind of game widget this builder makes
 * @param <U> the kind of widget helper this builder hands back
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Screen and UI Elements")
public abstract class AbstractWidgetBuilder<B extends AbstractWidgetBuilder<B, T, U>, T extends AbstractWidget, U extends ClickableWidgetHelper<U, T>> implements Alignable<B> {

    protected final IScreen screen;

    private int zIndex;
    private int width;
    private int height = 20;
    private int x;
    private int y;
    private Component message = Component.empty();
    private boolean active = true;
    private boolean visible = true;
    private float alpha = 1.0F;

    protected AbstractWidgetBuilder(IScreen screen) {
        this.screen = screen;
    }

    /**
     * Returns the width configured on this builder; it is not read from a built widget.
     *
     * @return the width of the widget.
     * @since 1.8.4
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the configured width. Subclasses may constrain other dimensions independently.
     *
     * @param width the width of the widget
     * @return self for chaining.
     * @since 1.8.4
     */
    public B width(int width) {
        this.width = width;
        return (B) this;
    }

    /**
     * Returns the height configured on this builder, initially 20.
     *
     * @return the height of the widget.
     * @since 1.8.4
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the configured height; subclasses with fixed heights may override this setter.
     *
     * @param height the height of the widget
     * @return self for chaining.
     * @since 1.8.4
     */
    public B height(int height) {
        this.height = height;
        return (B) this;
    }

    /**
     * Sets both configured dimensions. Subclasses may override this to constrain a dimension.
     *
     * @param width  the width of the widget
     * @param height the height of the widget
     * @return self for chaining.
     * @since 1.8.4
     */
    public B size(int width, int height) {
        this.width = width;
        this.height = height;
        return (B) this;
    }

    /**
     * Returns this builder's z-index, initially zero.
     *
     * @return the z-index of the widget.
     * @since 1.8.4
     */
    public int getZIndex() {
        return zIndex;
    }

    /**
     * Sets the z-index used when creating the widget.
     *
     * @param zIndex the z-index of the widget
     * @return self for chaining.
     * @since 1.8.4
     */
    public B zIndex(int zIndex) {
        this.zIndex = zIndex;
        return (B) this;
    }

    /**
     * Returns the builder's configured x coordinate, initially zero.
     *
     * @return the x position of the widget.
     * @since 1.8.4
     */
    public int getX() {
        return x;
    }

    /**
     * Sets the widget's configured x coordinate; this builder does not constrain it to the screen.
     *
     * @param x the x position of the widget
     * @return self for chaining.
     * @since 1.8.4
     */
    public B x(int x) {
        this.x = x;
        return (B) this;
    }

    /**
     * Returns the builder's configured y coordinate, initially zero.
     *
     * @return the y position of the widget.
     * @since 1.8.4
     */
    public int getY() {
        return y;
    }

    /**
     * Sets the builder's y coordinate; this builder does not constrain it to the screen.
     *
     * @param y the y position of the widget
     * @return self for chaining.
     * @since 1.8.4
     */
    public B y(int y) {
        this.y = y;
        return (B) this;
    }

    /**
     * Sets both configured coordinates.
     *
     * @param x the x position of the widget
     * @param y the y position of the widget
     * @return self for chaining.
     * @since 1.8.4
     */
    public B pos(int x, int y) {
        this.x = x;
        this.y = y;
        return (B) this;
    }

    /**
     * Returns the configured label as a text helper. It is empty until a label is set.
     *
     * @return the message of the widget or an empty text if none is set.
     * @since 1.8.4
     */
    public TextHelper getMessage() {
        return TextHelper.wrap(message);
    }

    /**
     * Sets the label from a literal string. Passing {@code null} leaves the label unchanged.
     *
     * @param message the message of the widget
     * @return self for chaining.
     * @since 1.8.4
     */
    public B message(@Nullable String message) {
        if (message != null) {
            this.message = Component.literal(message);
        }
        return (B) this;
    }

    /**
     * Sets the label from a text helper, preserving its component styling. Passing
     * {@code null} leaves the label unchanged.
     *
     * @param message the message of the widget
     * @return self for chaining.
     * @since 1.8.4
     */
    public B message(@Nullable TextHelper message) {
        if (message != null) {
            this.message = message.getRaw();
        }
        return (B) this;
    }

    /**
     * Returns whether this builder's widget is active; the default is {@code true}.
     *
     * @return {@code true} if the widget is active, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets whether the built widget is active. Inactive widgets cannot be interacted
     * with; this flag does not determine whether an active widget's action does anything.
     *
     * @param active whether the widget should be active or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public B active(boolean active) {
        this.active = active;
        return (B) this;
    }

    /**
     * Returns whether the builder is set to create a visible widget; the default is
     * {@code true}. The built widget has no corresponding visibility reader.
     *
     * @return {@code true} if the widget is visible, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets whether the created widget is visible; unlike {@code active}, this controls
     * drawing. The value is applied during {@link #build()} and has no built-widget setter.
     *
     * @param visible whether the widget should be visible or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public B visible(boolean visible) {
        this.visible = visible;
        return (B) this;
    }

    /**
     * Returns the alpha applied to the widget when built, from {@code 0.0} (transparent)
     * to {@code 1.0} (opaque); the default is {@code 1.0}.
     *
     * @return the alpha value of the widget.
     * @since 1.8.4
     */
    public float getAlpha() {
        return alpha;
    }

    /**
     * Sets the widget's alpha, clamped to the range {@code 0.0} (transparent) through
     * {@code 1.0} (opaque).
     *
     * @param alpha the alpha value of the widget
     * @return self for chaining.
     * @since 1.8.4
     */
    public B alpha(double alpha) {
        this.alpha = (float) Mth.clamp(alpha, 0.0F, 1.0F);
        return (B) this;
    }

    /**
     * Creates the widget with the builder's properties, applies its alpha, active and
     * visible flags, and adds it to the screen associated with this builder. Each call
     * creates and adds a new widget.
     *
     * @return the build widget for the set properties.
     * @since 1.8.4
     */
    public final U build() {
        U helper = createWidget();
        T widget = helper.getRaw();
        widget.setAlpha(alpha);
        widget.active = active;
        widget.visible = visible;
        screen.reAddElement(helper);
        return helper;
    }

    /**
     * Creates and wraps the game widget for {@link #build()}; subclasses provide the
     * widget-specific construction.
     *
     * @return a helper on the newly made game widget
     */
    protected abstract U createWidget();

    /**
     * Returns the builder's configured width, used for alignment even before the widget
     * is built.
     *
     * @return the scaled width of the element
     * @since 1.8.4
     */
    @Override
    public int getScaledWidth() {
        return width;
    }

    /**
     * Returns the width reported by the screen associated with this builder, used as
     * the parent extent for alignment.
     *
     * @return the width of the parent element
     * @since 1.8.4
     */
    @Override
    public int getParentWidth() {
        return screen.getWidth();
    }

    /**
     * Returns the builder's configured height, used for alignment even before the widget
     * is built.
     *
     * @return the scaled height of the element
     * @since 1.8.4
     */
    @Override
    public int getScaledHeight() {
        return height;
    }

    /**
     * Returns the height reported by the screen associated with this builder, used as
     * the parent extent for alignment.
     *
     * @return the height of the parent element
     * @since 1.8.4
     */
    @Override
    public int getParentHeight() {
        return screen.getHeight();
    }

    /**
     * Returns the builder's current x coordinate for alignment.
     *
     * @return the position of the scaled element's left side
     * @since 1.8.4
     */
    @Override
    public int getScaledLeft() {
        return x;
    }

    /**
     * Returns the builder's current y coordinate for alignment.
     *
     * @return the position of the scaled element's top side
     * @since 1.8.4
     */
    @Override
    public int getScaledTop() {
        return y;
    }

    /**
     * Sets the builder's position, equivalent to {@link #pos(int, int)}. Alignment
     * methods use this operation; it does not resize the eventual widget.
     *
     * @param x the new x position
     * @param y the new y position
     * @return self for chaining.
     * @since 1.8.4
     */
    @Override
    public B moveTo(int x, int y) {
        return pos(x, y);
    }

}
