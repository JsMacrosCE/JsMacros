package com.jsmacrosce.jsmacros.client.api.helper.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.TextBuilder;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.client.api.classes.render.components.Alignable;
import com.jsmacrosce.jsmacros.client.api.classes.render.components.RenderElement;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

//? if >1.21.8 {
/*import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
*///?}

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * Wraps a game widget so scripts can inspect and control it; other widget helpers
 * extend this base.
 * <p>
 * Each helper refers to its wrapped widget, so changes through a helper affect that
 * widget. Screens may return new helpers for the same widget on separate queries.
 * <p>
 * Tooltips and {@code zIndex} are held by the helper for JsMacros' rendering; the wrapped
 * widget state comes from the game.
 * <p>
 * A script builds its own widgets with a builder rather than by hand: {@code
 * IScreen.buttonBuilder()} and its siblings hand back a builder whose {@code build()}
 * puts the widget on the screen and gives the helper to go on with.
 * <pre>
 * // a button on a screen of the script's own
 * const screen = Hud.createScreen("a screen", true);
 * const button = screen.buttonBuilder()
 *    .pos(10, 20)
 *    .size(100, 20)
 *    .message("press me")
 *    .action(JavaWrapper.methodToJava(function (btn) {
 *      // the callback is given the button, so it can change the button
 *      btn.setLabel("pressed");
 *      btn.setActive(false);
 *    }))
 *    .build();
 * Hud.openScreen(screen);
 *
 * // and the game's own buttons, wrapped as the screen hands them over
 * const open = Hud.getOpenScreen();
 * if (open !== null) {
 *   for (const widget of open.getButtonWidgets()) {
 *     Chat.log(`${widget.getLabel()} at ${widget.getX()}, ${widget.getY()}`);
 *   }
 * }
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.0.5
  * @param <B> the concrete helper type, used for fluent method returns
  * @param <T> the wrapped Minecraft widget type
 */
@DocletCategory("Screen and UI Elements")
@SuppressWarnings("unused")
public class ClickableWidgetHelper<B extends ClickableWidgetHelper<B, T>, T extends AbstractWidget> extends BaseHelper<T> implements RenderElement, Alignable<B> {
    /** The value used by this helper's screen for widget draw ordering. */
    public int zIndex;
    /** Mutable tooltip components held by this helper; initially empty. */
    public List<Component> tooltips;

    /**
     * Marks the next release to be skipped on a container screen. Other screen types
     * are unchanged.
     * <pre>
     * const ClickableWidgetHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.screen.ClickableWidgetHelper");
     * const screen = Hud.createScreen("a screen", true);
     * screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("press me")
     *    .action(JavaWrapper.methodToJava(function (btn) {
     *       ClickableWidgetHelper.clickedOn(screen);
     *     }))
     *    .build();
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param screen the screen the widget belongs to
     */
    public static void clickedOn(IScreen screen) {
        if (screen instanceof AbstractContainerScreen<?> handled) {
            handled.skipNextRelease = true;
        }
    }

    /** Wraps {@code btn} with z-index {@code 0} and an empty tooltip list.
     * @param btn the widget to wrap
     */
    public ClickableWidgetHelper(T btn) {
        this(btn, 0);
    }

    /** Wraps {@code btn} with the supplied helper draw-ordering value and an empty tooltip list.
     * @param btn the widget to wrap
     * @param zIndex the helper's draw-ordering value
     */
    public ClickableWidgetHelper(T btn, int zIndex) {
        super(btn);
        this.zIndex = zIndex;
        this.tooltips = new ArrayList<>();
    }

    /**
     * Returns the widget's left edge in screen-coordinate pixels.
     *
     * @return the {@code x} coordinate of the button.
     * @since 1.0.5
     */
    public int getX() {
        return base.getX();
    }

    /**
     * Returns the widget's top edge in screen-coordinate pixels.
     *
     * @return the {@code y} coordinate of the button.
     * @since 1.0.5
     */
    public int getY() {
        return base.getY();
    }

    /**
     * Moves the widget to the given screen-coordinate pixel position without changing
     * its size.
     *
     * @param x the new left edge
     * @param y the new top edge
     * @return self for chaining.
     * @since 1.0.5
     */
    public B setPos(int x, int y) {
        base.setPosition(x, y);
        return (B) this;
    }

    /**
     * Returns the widget's width in pixels.
     *
     * @return the width of the button in pixels.
     * @since 1.0.5
     */
    public int getWidth() {
        return base.getWidth();
    }

    /**
     * Returns the widget's height in pixels.
     *
     * @return the height of the button in pixels.
     * @since 1.8.4
     */
    public int getHeight() {
        return base.getHeight();
    }

    /**
     * Sets the label to a literal plain-text component. Use
     * {@link #setLabel(TextHelper)} to supply styled text.
     *
     * @param label the plain text to show on the button
     * @return self for chaining.
     * @since 1.0.5, renamed from {@code setText} in 1.3.1
     * @deprecated The string overload is ambiguous; use {@link #setLabel(TextHelper)}.
     */
    @DocletIgnore
    @Deprecated
    public B setLabel(String label) {
        base.setMessage(Component.literal(label));
        return (B) this;
    }

    /**
     * Sets the label from the supplied text component, preserving its styling.
     *
     * @param helper the styled text to show on the button
     * @return self for chaining.
     * @since 1.3.1
     */
    public B setLabel(TextHelper helper) {
        base.setMessage(helper.getRaw());
        return (B) this;
    }

    /**
     * returns the button's label as a text wrapper, preserving its styling. Use
     * {@code getStringStripFormatting()} on the result for plain text without formatting.
     *
     * @return current button text.
     * @since 1.2.3, renamed fro {@code getText} in 1.3.1
     */
    public TextHelper getLabel() {
        return TextHelper.wrap(base.getMessage());
    }

    /**
     * returns the widget's active flag. Inactive widgets reject mouse clicks; this flag
     * does not indicate whether the button's action will succeed.
     *
     * @return button clickable state.
     * @since 1.0.5
     */
    public boolean getActive() {
        return base.active;
    }

    /**
     * sets the widget's active flag, controlling whether it accepts mouse clicks.
     *
     * @param t whether the button should be clickable
     * @return self for chaining.
     * @since 1.0.5
     */
    public B setActive(boolean t) {
        base.active = t;
        return (B) this;
    }

    /**
     * sets the widget's width in pixels without changing its height.
     *
     * @param width the new width in pixels
     * @return self for chaining.
     * @since 1.0.5
     */
    public B setWidth(int width) {
        base.setWidth(width);
        return (B) this;
    }

    /**
     * sends a left-button press and release at the widget's top-left coordinates. Off the
     * client thread, waits for the queued input task to run; on the client thread it runs
     * immediately.
     *
     * @return self for chaining.
     * @throws InterruptedException if the thread is interrupted while waiting for the
     * click to be handled
     * @since 1.3.1
     */
    public B click() throws InterruptedException {
        click(true);
        return (B) this;
    }

    /**
     * sends a left-button press and release at the widget's top-left coordinates. Off the
     * client thread, queues the input task; {@code await} controls whether this call
     * waits for that task. On the client thread, the input runs immediately and the flag
     * has no effect.
     *
     * @param await should wait for button to finish clicking.
     * @return self for chaining.
     * @throws InterruptedException if the thread is interrupted while waiting for the
     * click to be handled
     * @since 1.3.1
     */
    public B click(boolean await) throws InterruptedException {
        if (JsMacrosClient.clientCore.profile.checkJoinedThreadStack()) {
            //? if >1.21.8 {
            /*MouseButtonEvent fakeEvent = new MouseButtonEvent(
                    base.getX(),
                    base.getY(),
                    new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0));
            base.mouseClicked(fakeEvent, false);
            base.mouseReleased(fakeEvent);
            *///?} else {
            base.mouseClicked(base.getX(), base.getY(), 0);
            base.mouseReleased(base.getX(), base.getY(), 0);
            //?}
        } else {
            final Semaphore waiter = new Semaphore(await ? 0 : 1);
            Minecraft.getInstance().execute(() -> {
                //? if >1.21.8 {
                /*MouseButtonEvent fakeEvent = new MouseButtonEvent(
                        base.getX(),
                        base.getY(),
                        new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0));
                base.mouseClicked(fakeEvent, false);
                base.mouseReleased(fakeEvent);
                *///?} else {
                base.mouseClicked(base.getX(), base.getY(), 0);
                base.mouseReleased(base.getX(), base.getY(), 0);
                //?}
                waiter.release();
            });
            waiter.acquire();
        }
        return (B) this;
    }

    /**
     * replaces the helper's tooltip list. {@link TextBuilder} and {@link TextHelper}
     * values retain styling; other values are converted with {@code toString()} to plain
     * text. The tooltip list is drawn while the widget is hovered.
     *
     * @param tooltips the tooltips to set
     * @return self for chaining.
     * @since 1.8.4
     */
    public B setTooltip(Object... tooltips) {
        this.tooltips = new ArrayList<>();
        for (Object text : tooltips) {
            addTooltip(text);
        }
        return (B) this;
    }

    /**
     * appends a tooltip, preserving insertion order. {@link TextBuilder} and
     * {@link TextHelper} values retain styling; other values are converted with
     * {@code toString()} to plain text.
     *
     * @param tooltip the tooltip to add
     * @return self for chaining.
     * @since 1.8.4
     */
    public B addTooltip(Object tooltip) {
        if (tooltip instanceof TextBuilder) {
            tooltips.add(((TextBuilder) tooltip).build().getRaw());
        } else if (tooltip instanceof TextHelper) {
            tooltips.add(((TextHelper) tooltip).getRaw());
        } else if (tooltip instanceof String) {
            tooltips.add(Component.literal((String) tooltip));
        } else {
            tooltips.add(Component.literal(tooltip.toString()));
        }
        return (B) this;
    }

    /**
     * Removes the tooltip at the zero-based index. An invalid index returns {@code false};
     * duplicate tooltips can be distinguished by their positions.
     *
     * @param index the index of the tooltip to remove
     * @return {@code true} if the tooltip was removed successfully, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean removeTooltip(int index) {
        if (index >= 0 && index < tooltips.size()) {
            tooltips.remove(index);
            return true;
        }
        return false;
    }

    /**
     * Removes the first tooltip whose underlying text component equals the supplied
     * component. Returns {@code false} if no match exists.
     *
     * @param tooltip the tooltip to remove
     * @return {@code true} if the tooltip was removed successfully, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean removeTooltip(TextHelper tooltip) {
        return tooltips.remove(tooltip.getRaw());
    }

    /**
     * Returns wrappers for the current tooltips in insertion order. The returned list is
     * independent; modifying it does not change the widget.
     *
     * @return a copy of the tooltips.
     * @since 1.8.4
     */
    public List<TextHelper> getTooltips() {
        return tooltips.stream().map(TextHelper::wrap).collect(Collectors.toList());
    }

    /**
     * Renders the wrapped widget and requests tooltip rendering when the pointer is over
     * it and its tooltip list is non-empty.
     *
     * @param drawContext the graphics the widget is drawn into
     * @param mouseX     the pointer's position on the x axis
     * @param mouseY     the pointer's position on the y axis
     * @param delta      how far through the current tick the drawing is
     * @since 1.0.5
     */
    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        //? if >=26.1 {
        /*base.extractRenderState(drawContext, mouseX, mouseY, delta);
        *///?} else {
        base.render(drawContext, mouseX, mouseY, delta);
        //?}
        if (base.isMouseOver(mouseX, mouseY) && !tooltips.isEmpty()) {
            //? if >1.21.5 {
            drawContext.setComponentTooltipForNextFrame(mc.font, tooltips, mouseX, mouseY);
            //?} else {
            /*drawContext.renderComponentTooltip(mc.font, tooltips, mouseX, mouseY);
            *///?}
        }
    }

    /**
     * Returns this helper's z-index, used by its screen for widget draw ordering.
     *
     * @return the z-index of this widget
     * @since 1.0.5
     */
    @Override
    public int getZIndex() {
        return zIndex;
    }

    /** Returns a diagnostic string containing the widget message as plain text.
     * @return a diagnostic string containing the widget message
     */
    @Override
    public String toString() {
        return String.format("ButtonWidgetHelper:{\"message\": \"%s\"}", base.getMessage().getString());
    }

    /**
     * Returns the widget width used by alignment; this helper applies no scaling.
     *
     * @return the scaled width of the widget
     * @since 1.0.5
     */
    @Override
    public int getScaledWidth() {
        return getWidth();
    }

    /**
     * Returns the open screen's width for parent-relative alignment. Requires an open
     * screen.
     *
     * @return the width of the parent element
     * @since 1.0.5
     */
    @Override
    public int getParentWidth() {
        return Minecraft.getInstance().screen.width;
    }

    /**
     * Returns the widget height used by alignment; this helper applies no scaling.
     *
     * @return the scaled height of the widget
     * @since 1.0.5
     */
    @Override
    public int getScaledHeight() {
        return getHeight();
    }

    /**
     * Returns the open screen's height for parent-relative alignment. Requires an open
     * screen.
     *
     * @return the height of the parent element
     * @since 1.0.5
     */
    @Override
    public int getParentHeight() {
        return Minecraft.getInstance().screen.height;
    }

    /**
     * Returns the widget's {@code x} coordinate; this helper applies no scaling.
     *
     * @return the position of the scaled element's left side
     * @since 1.0.5
     */
    @Override
    public int getScaledLeft() {
        return getX();
    }

    /**
     * Returns the widget's top edge; this class has no scaling, so it is its {@code y} coordinate.
     *
     * @return the top edge in screen-coordinate pixels
     * @since 1.0.5
     */
    @Override
    public int getScaledTop() {
        return getY();
    }

    /**
     * Moves the widget to the given screen-coordinate pixel position without changing its size.
     * This is equivalent to {@link #setPos(int, int)} and supports the alignment methods.
     *
     * @param x the new x position
     * @param y the new y position
     * @return self for chaining.
     * @since 1.0.5
     */
    @Override
    public B moveTo(int x, int y) {
        return setPos(x, y);
    }

}
