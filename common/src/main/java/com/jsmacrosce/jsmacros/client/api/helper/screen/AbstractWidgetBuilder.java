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
 * the shared half of every widget builder: the position, the size, the label, the
 * three flags and the alpha, gathered up and handed to {@link #build()}.
 * <p>
 * A builder is made by the screen it is for rather than constructed directly.
 * {@code IScreen.buttonBuilder()} and its siblings hand one back, already bound to that
 * screen, and it is {@code build()} that puts the finished widget there. The builders a
 * screen hands out differ only in what they add, and each of them fixes some of what is
 * here: a button's height is pinned at twenty however it is asked for, and a text field
 * takes the screen's font.
 * <p>
 * Every setter hands the builder back, so a widget is built in one chain. Nothing is
 * drawn or changed until {@code build()} runs, which is also the point at which the
 * screen is told about the widget.
 * <p>
 * The defaults are worth knowing because they are not all obvious. The height starts at
 * twenty, everything else starts at zero, and the label starts as empty text rather than
 * as {@code null}, so a builder with nothing set on it builds a widget at the top left
 * of the screen with no label.
 * example:
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
     * the width set so far, or zero if none has been set. This is the builder's own
     * figure rather than the widget's, and it only becomes the widget's width when
     * {@code build()} runs; a builder that has been laid out and not yet built still
     * reads the number that was set on it.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const builder = screen.buttonBuilder().pos(10, 20).size(100, 20);
     * // what was set, before anything exists
     * Chat.log(`${builder.getX()}, ${builder.getY()}, `
     *   + `${builder.getWidth()} by ${builder.getHeight()}`);
     * const button = builder.build();
     * Hud.openScreen(screen);
     * // and what the widget ended up as
     * Chat.log(`${button.getX()}, ${button.getY()}, `
     *   + `${button.getWidth()} by ${button.getHeight()}`);
     * </pre>
     *
     * @return the width of the widget.
     * @since 1.8.4
     */
    public int getWidth() {
        return width;
    }

    /**
     * sets the width the widget will be built at. This is a number the widget is drawn
     * at rather than a size it is scaled to, and the builders that pin their height pin
     * it after this rather than refusing the call, so a chained width followed by a
     * height on a button is not a conflict.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.buttonBuilder()
     *    .pos(10, 20)
     *    .width(200)
     *    .message("wide")
     *    .build();
     * Hud.openScreen(screen);
     * Chat.log(`${button.getWidth()} by ${button.getHeight()}`);
     * </pre>
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
     * the height set so far, which is twenty until something else is set. That default
     * is the height a button is drawn at in the game, and the builders for widgets with
     * a fixed height keep it there.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * // a builder with nothing set on it still has the default height
     * const button = screen.buttonBuilder().pos(10, 20).width(100).build();
     * Hud.openScreen(screen);
     * Chat.log(`height ${button.getHeight()}, width ${button.getWidth()}`);
     * </pre>
     *
     * @return the height of the widget.
     * @since 1.8.4
     */
    public int getHeight() {
        return height;
    }

    /**
     * sets the height the widget will be built at. The builders for widgets whose height
     * is fixed override this and ignore the number, so a button ends up twenty tall
     * whichever way its height is set.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * // a text field takes a height, unlike a button
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .message("type here")
     *    .build();
     * Hud.openScreen(screen);
     * Chat.log(`${field.getWidth()} by ${field.getHeight()}`);
     * </pre>
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
     * sets both halves of the size in one call, which is the same as setting the width
     * and then the height. The order within the builder does not matter here because
     * both are simply written down; what order does matter for is the builders that pin
     * one of them, since a size on a button is turned into a width of the given number
     * and a height of twenty however the height was asked for.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .message("type here")
     *    .build();
     * Hud.openScreen(screen);
     * Chat.log(`${field.getWidth()} by ${field.getHeight()}`);
     * </pre>
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
     * the order the widget will be drawn in against the others on the same screen,
     * higher being drawn later and so on top. It starts at zero, which is where a
     * widget lands unless the builder is told otherwise.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * // a later z-index is drawn over an earlier one
     * screen.buttonBuilder().pos(10, 20).size(100, 20).zIndex(1).message("under").build();
     * screen.buttonBuilder().pos(10, 20).size(100, 20).zIndex(2).message("over").build();
     * Hud.openScreen(screen);
     * </pre>
     *
     * @return the z-index of the widget.
     * @since 1.8.4
     */
    public int getZIndex() {
        return zIndex;
    }

    /**
     * sets the order the widget will be drawn in against the others on the same screen.
     * Higher numbers are drawn later and so appear on top, so this is what decides
     * which of two overlapping widgets the player can see and click.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * // two widgets in the same place, the second on top
     * screen.buttonBuilder().pos(10, 20).size(100, 20).zIndex(0).message("underneath").build();
     * screen.buttonBuilder().pos(10, 20).size(100, 20).zIndex(1).message("on top").build();
     * Hud.openScreen(screen);
     * </pre>
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
     * the left edge set so far, or zero if none has been set. This is the builder's own
     * figure; the widget takes it when {@code build()} runs.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const builder = screen.buttonBuilder().pos(30, 40).size(100, 20);
     * Chat.log(`laid out at ${builder.getX()}, ${builder.getY()} before building`);
     * const button = builder.build();
     * Hud.openScreen(screen);
     * Chat.log(`built at ${button.getX()}, ${button.getY()}`);
     * </pre>
     *
     * @return the x position of the widget.
     * @since 1.8.4
     */
    public int getX() {
        return x;
    }

    /**
     * sets the left edge the widget will be built at, in screen coordinates. Nothing
     * constrains it to the screen, so a widget can be laid out off the edge and simply
     * is not visible.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.buttonBuilder()
     *    .x(10)
     *    .y(40)
     *    .size(100, 20)
     *    .message("down the left")
     *    .build();
     * Hud.openScreen(screen);
     * </pre>
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
     * the top edge set so far, or zero if none has been set, on the same counting as
     * {@link #getX()}.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const builder = screen.buttonBuilder().pos(30, 40).size(100, 20);
     * Chat.log(`laid out at ${builder.getX()}, ${builder.getY()} before building`);
     * const button = builder.build();
     * Hud.openScreen(screen);
     * Chat.log(`built at ${button.getX()}, ${button.getY()}`);
     * </pre>
     *
     * @return the y position of the widget.
     * @since 1.8.4
     */
    public int getY() {
        return y;
    }

    /**
     * sets the top edge the widget will be built at, in screen coordinates, on the same
     * counting as {@code x}. Nothing constrains it to the screen.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.buttonBuilder()
     *    .pos(10, 40)
     *    .size(100, 20)
     *    .message("lower")
     *    .build();
     * Hud.openScreen(screen);
     * </pre>
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
     * sets both halves of the position in one call, which is the same as setting the
     * {@code x} and then the {@code y}.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("press me")
     *    .build();
     * Hud.openScreen(screen);
     * Chat.log(`at ${button.getX()}, ${button.getY()}`);
     * </pre>
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
     * the label set so far, as a text wrapper. It starts as empty text rather than as
     * {@code null}, so a builder with no label set still hands back something to read
     * and a widget built from it is drawn with nothing on it.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * // nothing set, so the label is empty rather than missing
     * const builder = screen.buttonBuilder().pos(10, 20).size(100, 20);
     * Chat.log(`label is "${builder.getMessage().getString()}"`);
     * builder.message("now it has one");
     * Chat.log(`label is "${builder.getMessage().getString()}"`);
     * </pre>
     *
     * @return the message of the widget or an empty text if none is set.
     * @since 1.8.4
     */
    public TextHelper getMessage() {
        return TextHelper.wrap(message);
    }

    /**
     * sets the label to a plain string. A paragraph sign and a formatting code in the
     * string are taken as typed text rather than as styling, so a label that wants
     * colour is better built as a text helper and given to the other form of this.
     * <p>
     * Passing {@code null} leaves the label as it was rather than clearing it, which is
     * the same as not calling this at all.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("plain text")
     *    .build();
     * Hud.openScreen(screen);
     * Chat.log(button.getLabel().getString());
     * </pre>
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
     * sets the label to a text helper, which is how a label gets styling: whatever the
     * text was built with is what the widget is drawn with, and it comes back out again
     * through the widget's own label reader.
     * <p>
     * Passing {@code null} leaves the label as it was rather than clearing it.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message(Chat.createTextHelperFromString("a label"))
     *    .build();
     * Hud.openScreen(screen);
     * // the styling is kept rather than being read back as raw codes
     * Chat.log(button.getLabel());
     * </pre>
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
     * whether the widget is set to be pressable, which is what it starts as. An
     * inactive widget cannot be interacted with and is drawn in its disabled look, so
     * this is the flag to turn off to hold a widget still.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * // a button that cannot be pressed yet
     * const button = screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("not yet")
     *    .active(false)
     *    .build();
     * Hud.openScreen(screen);
     * Chat.log(`active on the builder: ${button.getActive()}`);
     * </pre>
     *
     * @return {@code true} if the widget is active, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isActive() {
        return active;
    }

    /**
     * An inactive widget can not be interacted with and may have a different appearance.
     * <p>
     * This is the flag rather than a question about whether the action would work, so a
     * widget can be active and still do nothing when it is pressed.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("press me")
     *    .active(false)
     *    .build();
     * Hud.openScreen(screen);
     * // let it be pressed
     * button.setActive(true);
     * </pre>
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
     * whether the widget is set to be drawn, which is what it starts as. A widget that
     * is not visible is skipped entirely rather than drawn transparent, so it takes no
     * clicks and no tooltips either.
     * <p>
     * There is no reader for this on the finished widget, so a script that wants to know
     * whether a widget ended up visible has to keep the answer from the builder.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const builder = screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("hidden")
     *    .visible(false);
     * // the builder is the only place this can be read back from
     * Chat.log(`will be drawn: ${builder.isVisible()}`);
     * const button = builder.build();
     * Hud.openScreen(screen);
     * </pre>
     *
     * @return {@code true} if the widget is visible, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * sets whether the widget is drawn at all. This is a different thing from
     * {@code active}: an invisible widget is not drawn and takes no clicks, while an
     * inactive one is drawn greyed out and can be read.
     * <p>
     * The flag is applied when the widget is built, and there is no setter for it on a
     * built widget, so this is the only point at which it can be set. A widget that is
     * on screen and should be shown or hidden from then on is better handled by removing
     * it from the screen.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * // built hidden, so it is on the screen but never drawn
     * screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("not drawn")
     *    .visible(false)
     *    .build();
     * Hud.openScreen(screen);
     * </pre>
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
     * how opaque the widget is drawn, from nothing at all up to fully solid. It starts
     * at one, so a widget is solid unless something is said otherwise.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * // a number past one is brought back to one rather than being refused
     * const button = screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("solid")
     *    .alpha(5)
     *    .build();
     * Hud.openScreen(screen);
     * </pre>
     *
     * @return the alpha value of the widget.
     * @since 1.8.4
     */
    public float getAlpha() {
        return alpha;
    }

    /**
     * sets how opaque the widget is drawn, from nothing up to fully solid. A number
     * outside that range is brought back to the nearest end of it rather than being
     * refused, so an alpha of five is a solid widget and an alpha of minus one is an
     * invisible one rather than an error.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * // half see-through
     * const button = screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("see through")
     *    .alpha(0.5)
     *    .build();
     * Hud.openScreen(screen);
     * </pre>
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
     * makes the widget and puts it on the screen the builder came from, which is what
     * every chain on a builder is leading to.
     * <p>
     * Three things are applied here rather than on the builder: the alpha, the active
     * flag and the visible flag are written onto the finished widget. The position, the
     * size and the label were already given to the game widget as it was made, so
     * reading them back off the helper afterwards gives the numbers that were set.
     * <p>
     * The screen is told about the widget here, which is what puts it in the screen's
     * own list and draws it. Calling this twice on one builder makes two widgets.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("press me")
     *    .alpha(0.5)
     *    .active(true)
     *    .build();
     * Hud.openScreen(screen);
     * </pre>
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
     * makes the game widget this builder describes and wraps it. Each builder supplies
     * its own, and that is where the position, the size and the label actually reach
     * the game, because {@link #build()} only applies the three flags on top.
     * <p>
     * There is nothing for a script to call here: a builder is always used through
     * {@code build()}.
     *
     * @return a helper on the newly made game widget
     */
    protected abstract U createWidget();

    /**
     * the width the align methods measure this builder by, which is the width set on it
     * rather than the width of a widget that does not exist yet. This is what makes an
     * element alignable before it is built.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const builder = screen.buttonBuilder().size(100, 20).message("aligned before build");
     * // align it before there is anything to see
     * builder.align("center", "center");
     * Chat.log(`laid out at ${builder.getX()}, ${builder.getY()}`);
     * const button = builder.build();
     * Hud.openScreen(screen);
     * Chat.log(`built at ${button.getX()}, ${button.getY()}`);
     * </pre>
     *
     * @return the scaled width of the element
     * @since 1.8.4
     */
    @Override
    public int getScaledWidth() {
        return width;
    }

    /**
     * the width a parent-relative alignment on this builder is measured against, which
     * is the width of the screen the builder was made for. Unlike a built widget, this
     * asks the builder's own screen rather than whatever is open, so it answers the same
     * whether or not the screen has been opened yet.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const builder = screen.buttonBuilder().size(100, 20).message("centred");
     * // the screen is not open yet, and the builder still knows how wide it is
     * builder.alignHorizontally("center");
     * Hud.openScreen(screen);
     * Chat.log(`laid out at x ${builder.getX()}`);
     * </pre>
     *
     * @return the width of the parent element
     * @since 1.8.4
     */
    @Override
    public int getParentWidth() {
        return screen.getWidth();
    }

    /**
     * the height the align methods measure this builder by, which is the height set on
     * it, or twenty if none has been set.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const builder = screen.buttonBuilder().size(100, 20).message("aligned before build");
     * builder.alignVertically("center");
     * Chat.log(`laid out at y ${builder.getY()}`);
     * </pre>
     *
     * @return the scaled height of the element
     * @since 1.8.4
     */
    @Override
    public int getScaledHeight() {
        return height;
    }

    /**
     * the height a parent-relative alignment on this builder is measured against, which
     * is the height of the screen the builder was made for. As with the width form, this
     * asks the builder's own screen rather than whatever is open.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const builder = screen.buttonBuilder().size(100, 20).message("centred");
     * // the screen is not open yet, and the builder still knows how tall it is
     * builder.alignVertically("center");
     * Hud.openScreen(screen);
     * Chat.log(`laid out at y ${builder.getY()}`);
     * </pre>
     *
     * @return the height of the parent element
     * @since 1.8.4
     */
    @Override
    public int getParentHeight() {
        return screen.getHeight();
    }

    /**
     * the left edge the align methods move this builder to, which is the {@code x} set
     * on it.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const builder = screen.buttonBuilder().size(100, 20).message("moved before build");
     * Chat.log(`left edge is ${builder.getScaledLeft()}`);
     * builder.moveToX(40);
     * Chat.log(`now ${builder.getScaledLeft()}`);
     * </pre>
     *
     * @return the position of the scaled element's left side
     * @since 1.8.4
     */
    @Override
    public int getScaledLeft() {
        return x;
    }

    /**
     * the top edge the align methods move this builder to, which is the {@code y} set
     * on it.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const builder = screen.buttonBuilder().size(100, 20).message("moved before build");
     * Chat.log(`top edge is ${builder.getScaledTop()}`);
     * builder.moveToY(40);
     * Chat.log(`now ${builder.getScaledTop()}`);
     * </pre>
     *
     * @return the position of the scaled element's top side
     * @since 1.8.4
     */
    @Override
    public int getScaledTop() {
        return y;
    }

    /**
     * moves the builder to a new position, which for a builder is the same as
     * {@link #pos(int, int)}. It is here so a builder can be aligned with the align
     * methods, which are written in terms of this one.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const builder = screen.buttonBuilder().size(100, 20).message("moved before build");
     * // the same as pos
     * builder.moveTo(40, 60);
     * Chat.log(`laid out at ${builder.getX()}, ${builder.getY()}`);
     * </pre>
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
