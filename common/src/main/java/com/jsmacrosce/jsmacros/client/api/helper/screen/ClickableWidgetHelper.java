package com.jsmacrosce.jsmacros.client.api.helper.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import com.jsmacrosce.doclet.DocletCategory;
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
 * a button, and the base of every other widget helper: the checkbox, the slider, the
 * text field, the cycling button and the lock button are all this with a little more on
 * top.
 * <p>
 * A helper is a wrapper on the game's own widget rather than a copy of it, so a change
 * made through one is a change to the button the player is looking at. Two helpers for
 * the same button are two wrappers on one widget, which is why the game's own buttons
 * come back as a fresh object each time a screen is asked for them.
 * <p>
 * The tooltips are held on the helper rather than on the game, and they are the only
 * part of this class that is JsMacros' own. Everything else here is the game's button
 * seen from a script.
 * <p>
 * A script builds its own widgets with a builder rather than by hand: {@code
 * IScreen.buttonBuilder()} and its siblings hand back a builder whose {@code build()}
 * puts the widget on the screen and gives the helper to go on with.
 * example:
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
 */
@DocletCategory("Screen and UI Elements")
@SuppressWarnings("unused")
public class ClickableWidgetHelper<B extends ClickableWidgetHelper<B, T>, T extends AbstractWidget> extends BaseHelper<T> implements RenderElement, Alignable<B> {
    public int zIndex;
    public List<Component> tooltips;

    /**
     * tells a container screen that a widget of the script's own has just been clicked,
     * so the screen does not treat the release that follows as a drag of a held stack.
     * <p>
     * A container screen watches for that release itself, and a click that a script
     * produced never went through the input the screen is watching, so the two are out
     * of step. This is what every builder on a screen calls after a press, and a script
     * driving a widget by hand can call it for itself.
     * <p>
     * A screen that is not a container screen is left alone, so this is safe to call
     * with any screen.
     * example:
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

    public ClickableWidgetHelper(T btn) {
        this(btn, 0);
    }

    public ClickableWidgetHelper(T btn, int zIndex) {
        super(btn);
        this.zIndex = zIndex;
        this.tooltips = new ArrayList<>();
    }

    /**
     * the button's left edge in screen coordinates, counted in pixels rather than in
     * widgets. It is read off the game widget rather than remembered, so it follows a
     * move made through any route.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   for (const button of screen.getButtonWidgets()) {
     *     if (button.getX() > 100) {
     *       Chat.log(`a button out on the right at ${button.getX()}, ${button.getY()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the {@code x} coordinate of the button.
     * @since 1.0.5
     */
    public int getX() {
        return base.getX();
    }

    /**
     * the button's top edge in screen coordinates, on the same counting as
     * {@link #getX()}.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   const first = screen.getButtonWidgets().get(0);
     *   Chat.log(`the first button sits at ${first.getX()}, ${first.getY()}`);
     * }
     * </pre>
     *
     * @return the {@code y} coordinate of the button.
     * @since 1.0.5
     */
    public int getY() {
        return base.getY();
    }

    /**
     * moves the button to a new position, in screen coordinates. The size is left alone,
     * so this is a move rather than a placement, and the two axes move together.
     * <p>
     * The button is moved on the game widget itself, so it goes where it is drawn
     * immediately rather than on the next time the screen lays itself out.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   const buttons = screen.getButtonWidgets();
     *   if (buttons.size() > 0) {
     *     const first = buttons.get(0);
     *     // a nudge to the right and down, size unchanged
     *     first.setPos(first.getX() + 10, first.getY() + 10);
     *     Chat.log(`now at ${first.getX()}, ${first.getY()}`);
     *   }
     * }
     * </pre>
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
     * the button's width in pixels, read off the game widget. This is the size the
     * button is drawn at, which is not the same as the size it occupies once a scale is
     * applied to it; that is what the align methods measure, and
     * {@code getScaledWidth()} is the one to use for alignment.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   for (const button of screen.getButtonWidgets()) {
     *     // twenty is narrow, so a button under that is worth noticing
     *     if (20 > button.getWidth()) {
     *       Chat.log(`a narrow one: ${button.getLabel()} is ${button.getWidth()} wide`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the width of the button in pixels.
     * @since 1.0.5
     */
    public int getWidth() {
        return base.getWidth();
    }

    /**
     * the button's height in pixels, on the same counting as {@link #getWidth()}.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   for (const button of screen.getButtonWidgets()) {
     *     Chat.log(`${button.getLabel()}: ${button.getWidth()} by ${button.getHeight()}`);
     *   }
     * }
     * </pre>
     *
     * @return the height of the button in pixels.
     * @since 1.8.4
     */
    public int getHeight() {
        return base.getHeight();
    }

    /**
     * changes the button's text to a plain string, replacing whatever styling it had.
     * A string with a paragraph sign and a formatting code in it keeps that code as
     * typed text, so a label that wants colour is better made with a
     * {@link TextHelper} and passed to the other form of this.
     * <p>
     * It is deprecated in favour of {@link #setLabel(TextHelper)}, which is the same
     * call for a styled label and is the one a button in a script screen usually wants.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("before")
     *    .build();
     * Hud.openScreen(screen);
     * // the deprecated string form
     * button.setLabel("after");
     * Chat.log(`now reads ${button.getLabel().getString()}`);
     * </pre>
     *
     * @param label the plain text to show on the button
     * @return self for chaining.
     * @since 1.0.5, renamed from {@code setText} in 1.3.1
     * @deprecated only deprecated in buttonWidgetHelper for confusing name.
     */
    @Deprecated
    public B setLabel(String label) {
        base.setMessage(Component.literal(label));
        return (B) this;
    }

    /**
     * changes the button's text, keeping the styling the text was built with. This is
     * the form to reach for when the label is coloured or has anything else on it,
     * because a plain string would come back out of {@link #getLabel()} as raw
     * formatting codes rather than as the styled text the player sees.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("before")
     *    .build();
     * Hud.openScreen(screen);
     * // a styled label, so any styling comes back out of the reader
     * button.setLabel(Chat.createTextHelperFromString("after"));
     * Chat.log(`now reads ${button.getLabel().getString()}`);
     * </pre>
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
     * the button's current text, as a text wrapper rather than a plain string, so the
     * styling the label was built with comes with it. {@code getString()} on the result
     * gives the text on its own, and {@code getStringStripFormatting()} gives it
     * without the formatting codes.
     * <p>
     * This is the text the button is labelled with, which is not the same as the
     * button's tooltip: a tooltip set through {@link #addTooltip(Object)} is a separate
     * thing and comes back from {@link #getTooltips()}.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   for (const button of screen.getButtonWidgets()) {
     *     // the text on its own, with the styling codes left out
     *     Chat.log(button.getLabel().getStringStripFormatting());
     *   }
     * }
     * </pre>
     *
     * @return current button text.
     * @since 1.2.3, renamed fro {@code getText} in 1.3.1
     */
    public TextHelper getLabel() {
        return TextHelper.wrap(base.getMessage());
    }

    /**
     * whether the button can be pressed right now. An inactive button is drawn in its
     * disabled look and the game ignores clicks on it, which is how a screen greys out a
     * button whose action is not available.
     * <p>
     * This is a plain flag on the widget rather than a question about whether the
     * action would work, so a button can be active and still do nothing.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   for (const button of screen.getButtonWidgets()) {
     *     if (!button.getActive()) {
     *       Chat.log(`${button.getLabel()} is greyed out`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return button clickable state.
     * @since 1.0.5
     */
    public boolean getActive() {
        return base.active;
    }

    /**
     * sets whether the button can be pressed. Turning a button off is how a screen holds
     * a button still while something it depends on is not ready, and turning one on is
     * how it is let go again.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("do it")
     *    .active(false)
     *    .build();
     * Hud.openScreen(screen);
     * // let it be pressed
     * button.setActive(true);
     * Chat.log(`active now: ${button.getActive()}`);
     * </pre>
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
     * sets the button's width in pixels. The height is left alone, so this resizes
     * rather than scales, and a widget that draws itself from its own size follows
     * straight away.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .build();
     * Hud.openScreen(screen);
     * button.setWidth(200);
     * Chat.log(`now ${button.getWidth()} by ${button.getHeight()}`);
     * </pre>
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
     * presses the button as though the player had, which runs whatever the game has
     * bound to it: the action the builder was given, or the game's own handling.
     * <p>
     * The click goes through the same pair of press and release the game would send, so
     * a button that reacts on release reacts here. There is no mouse position involved,
     * because the press is sent at the button's own top left corner rather than wherever
     * the pointer is, which means a button that only reacts to a press on part of it can
     * behave differently here than under a real click.
     * <p>
     * This waits for the click to finish, so calling it from the client thread holds that
     * thread until the widget has handled it. The form taking a flag lets the call go
     * back as soon as the work is queued instead.
     * <p>
     * The action runs with the button as its first argument and the screen it was built
     * on as its second, the same as a real press.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("press me")
     *    .action(JavaWrapper.methodToJava(function (btn) {
     *       Chat.log("the action ran");
     *     }))
     *    .build();
     * Hud.openScreen(screen);
     * // the waiting form, which is the one to reach for first
     * button.click();
     * </pre>
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
     * presses the button as though the player had, with a choice about whether to wait
     * for it.
     * <p>
     * When called from the client thread there is nothing to hand off and nothing to
     * wait for, so the {@code await} flag makes no difference there. Off the client
     * thread the click is queued onto it, and the flag decides whether this call blocks
     * until its own task has run. Blocking also waits out whatever else was queued
     * ahead of it, which is usually what a script wants anyway and occasionally is not.
     * <p>
     * Passing {@code false} is the way to press a button without holding the calling
     * thread up, at the cost of the action not having run by the time this returns.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   const buttons = screen.getButtonWidgets();
     *   if (buttons.size() > 0) {
     *     // queue the click and carry on without waiting for it
     *     buttons.get(0).click(false);
     *     Chat.log("the click has been queued");
     *   }
     * }
     * </pre>
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
     * replaces the button's tooltips with the ones given, so this starts again from an
     * empty list rather than adding to what was there. Anything that is not already a
     * piece of text is put through {@code toString()} and taken as plain text, so a
     * number or an object handed in here ends up as its own string form.
     * <p>
     * A tooltip is only drawn while the pointer is over the button, and a button with
     * no tooltips draws none however long it is hovered.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("hover me")
     *    .build();
     * Hud.openScreen(screen);
     * // a plain string and a number; the number becomes its own text
     * button.setTooltip("costs 10", 10);
     * Chat.log(`${button.getTooltips().size()} tooltip(s)`);
     * </pre>
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
     * adds one tooltip to the end of the list, leaving the ones already there. A text
     * builder or a text wrapper keeps its styling, a plain string is taken as literal
     * text, and anything else goes through {@code toString()}.
     * <p>
     * A tooltip is drawn in the order the list is in, so this is the way to put a line
     * after an existing one.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("hover me")
     *    .build();
     * Hud.openScreen(screen);
     * // three lines, in this order
     * button.addTooltip("a stone block")
     *      .addTooltip(Chat.createTextHelperFromString("mined with a pickaxe"))
     *      .addTooltip("unbreaking does not help");
     * for (const tip of button.getTooltips()) {
     *   Chat.log(`  ${tip.getString()}`);
     * }
     * </pre>
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
     * removes the tooltip at a position in the list, counting from zero. The check is on
     * the index rather than on the list being non-empty, so an index past the end is
     * refused rather than throwing.
     * <p>
     * Removing by index is the form to use when the same tooltip has been added more
     * than once, because removing by text takes the first match either way.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("hover me")
     *    .build();
     * Hud.openScreen(screen);
     * // tooltips go on the built button, not on the builder
     * button.addTooltip("first").addTooltip("second");
     * // take the first line off, leaving the second
     * const removed = button.removeTooltip(0);
     * Chat.log(`removed: ${removed}, left: ${button.getTooltips().size()}`);
     * // an index past the end is refused rather than throwing
     * Chat.log(`out of range: ${button.removeTooltip(99)}`);
     * </pre>
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
     * removes the first tooltip equal to the one given, matched on the underlying game
     * text rather than on the wrapper, so a text wrapper built separately from the same
     * string still matches.
     * <p>
     * A tooltip that is not in the list is simply not there, which answers {@code false}
     * rather than throwing. When the same text has been added more than once this takes
     * the first of them, so a script that needs a particular copy should use the index
     * form instead.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("hover me")
     *    .build();
     * Hud.openScreen(screen);
     * // tooltips go on the built button, not on the builder
     * button.addTooltip("first").addTooltip("second");
     * // matched on the text, so a separately built wrapper finds it
     * const gone = button.removeTooltip(Chat.createTextHelperFromString("first"));
     * Chat.log(`removed: ${gone}`);
     * </pre>
     *
     * @param tooltip the tooltip to remove
     * @return {@code true} if the tooltip was removed successfully, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean removeTooltip(TextHelper tooltip) {
        return tooltips.remove(tooltip.getRaw());
    }

    /**
     * the tooltips currently on the button, as text wrappers, in the order they will be
     * drawn. The list handed back is a fresh one built from the live tooltip list, so
     * changing it does not change the button; changing the button is what goes through
     * {@link #addTooltip(Object)} and the remove calls.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   for (const button of screen.getButtonWidgets()) {
     *     const tips = button.getTooltips();
     *     if (tips.size() > 0) {
     *       Chat.log(`${button.getLabel()} has ${tips.size()} tooltip(s)`);
     *       for (const tip of tips) {
     *         Chat.log(`  ${tip.getString()}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return a copy of the tooltips.
     * @since 1.8.4
     */
    public List<TextHelper> getTooltips() {
        return tooltips.stream().map(TextHelper::wrap).collect(Collectors.toList());
    }

    /**
     * draws the button and, when the pointer is over it and it has tooltips, the
     * tooltips. The tooltip drawing is the only part of this that is JsMacros' own; the
     * button itself is drawn by the game.
     * <p>
     * A button with no tooltips draws no tooltip however long it is hovered, and the
     * tooltip is only drawn once the pointer is actually inside the button's bounds.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   for (const button of screen.getButtonWidgets()) {
     *     // hovering is reported through the tooltip, so an empty list means no tooltip
     *     if (button.getTooltips().size() > 0) {
     *       Chat.log(`${button.getLabel()} will show a tooltip when hovered`);
     *     }
     *   }
     * }
     * </pre>
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
     * the order this widget is drawn in against the others on the same screen, higher
     * being drawn later and so on top. It is a number the screen sorts on rather than
     * anything the game computes, and every widget a script builds defaults to zero.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   for (const button of screen.getButtonWidgets()) {
     *     Chat.log(`${button.getZIndex()}: ${button.getLabel()}`);
     *   }
     * }
     * </pre>
     *
     * @return the z-index of this widget
     * @since 1.0.5
     */
    @Override
    public int getZIndex() {
        return zIndex;
    }

    @Override
    public String toString() {
        return String.format("ButtonWidgetHelper:{\"message\": \"%s\"}", base.getMessage().getString());
    }

    /**
     * the width this widget is measured for by the align methods. This class has no
     * scale of its own, so it hands the plain width back unchanged. It is here so a
     * button can be measured alongside something that does scale.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   for (const button of screen.getButtonWidgets()) {
     *     if (button.getScaledWidth() > 200) {
     *       Chat.log(`a wide one: ${button.getLabel()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the scaled width of the widget
     * @since 1.0.5
     */
    @Override
    public int getScaledWidth() {
        return getWidth();
    }

    /**
     * the width the align methods measure a parent-relative alignment against, which
     * for a widget is the width of the screen that is open. It reads the open screen
     * directly rather than going through the screen the widget was made on, so it is
     * only safe to ask while a screen is actually open.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   const buttons = screen.getButtonWidgets();
     *   if (buttons.size() > 0) {
     *     const first = buttons.get(0);
     *     // centre it on the open screen
     *     first.align("center", "center");
     *     Chat.log(`now at ${first.getX()}, ${first.getY()}`);
     *   }
     * }
     * </pre>
     *
     * @return the width of the parent element
     * @since 1.0.5
     */
    @Override
    public int getParentWidth() {
        return Minecraft.getInstance().screen.width;
    }

    /**
     * the height this widget is measured for by the align methods, which on this class
     * is the plain height, handed back unchanged.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   for (const button of screen.getButtonWidgets()) {
     *     Chat.log(`${button.getLabel()} is ${button.getScaledHeight()} tall`);
     *   }
     * }
     * </pre>
     *
     * @return the scaled height of the widget
     * @since 1.0.5
     */
    @Override
    public int getScaledHeight() {
        return getHeight();
    }

    /**
     * the height the align methods measure a parent-relative alignment against, which
     * for a widget is the height of the screen that is open. Like the width form, it
     * reads the open screen directly, so it is only safe to ask while one is.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   const buttons = screen.getButtonWidgets();
     *   if (buttons.size() > 0) {
     *     // line the first button up with the middle of the open screen
     *     buttons.get(0).alignVertically("center");
     *     Chat.log(`now at y ${buttons.get(0).getY()}`);
     *   }
     * }
     * </pre>
     *
     * @return the height of the parent element
     * @since 1.0.5
     */
    @Override
    public int getParentHeight() {
        return Minecraft.getInstance().screen.height;
    }

    /**
     * the left edge the align methods move, which is the button's own {@code x}.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   const buttons = screen.getButtonWidgets();
     *   if (buttons.size() > 0) {
     *     Chat.log(`left edge is ${buttons.get(0).getScaledLeft()}`);
     *   }
     * }
     * </pre>
     *
     * @return the position of the scaled element's left side
     * @since 1.0.5
     */
    @Override
    public int getScaledLeft() {
        return getX();
    }

    /**
     * the top edge the align methods move, which is the button's own {@code y}.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   const buttons = screen.getButtonWidgets();
     *   if (buttons.size() > 0) {
     *     Chat.log(`top edge is ${buttons.get(0).getScaledTop()}`);
     *   }
     * }
     * </pre>
     *
     * @return the position of the scaled element's top side
     * @since 1.0.5
     */
    @Override
    public int getScaledTop() {
        return getY();
    }

    /**
     * moves the widget to a new position, which for a button is the same call as
     * {@link #setPos(int, int)}. It is here so a button can be aligned with the align
     * methods, which are written in terms of this one.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   const buttons = screen.getButtonWidgets();
     *   if (buttons.size() > 0) {
     *     const first = buttons.get(0);
     *     // the same as setPos
     *     first.moveTo(40, 60);
     *     Chat.log(`now at ${first.getX()}, ${first.getY()}`);
     *   }
     * }
     * </pre>
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
