package com.jsmacrosce.jsmacros.client.api.classes.render;

import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.api.math.Pos2D;
import com.jsmacrosce.jsmacros.api.math.Vec2D;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.screen.*;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A script-made screen: a 2D overlay that is the game's own menu class.
 * <p>
 * Everything a 2D overlay can hold works here too, since this extends it, so text,
 * rectangles, lines, images and item icons are all available in the same way. What is
 * different is that a screen is <em>opened</em> rather than registered. It is the
 * game's own screen class, so opening one pauses the game in singleplayer, takes the
 * mouse, and gives it a place to put buttons and text fields. A 2D overlay just draws
 * on top of whatever is already there and has none of that.
 * <p>
 * One is made with {@code Hud.createScreen(String, boolean)}, which returns a
 * {@code ScriptScreen}, and opened with {@code Hud.openScreen}. Nothing on it draws
 * until it is opened, and closing it hands control back to whatever was open before.
 * <p>
 * A screen can be emptied and rebuilt at any time with
 * {@link #reloadScreen()}, which is what a script calls after it has changed something
 * that the widgets were laid out from. The init function is run again on the way, and
 * as on any 2D overlay the screen is emptied first, so an init function is what puts
 * the widgets back.
 * <p>
 * The input callbacks are the other half of a screen. They are set as pairs of
 * {@code setOn...} calls, and each one replaces the last: a screen has one mouse-down
 * handler rather than a list. They all report in the same screen space as
 * {@link #getWidth()}, so a position from one can be used as a drawing position
 * directly.
 * example:
     * <pre>
 * const screen = Hud.createScreen("my screen", true);
 * screen.addText("something to read", 10, 40, 0xFFFFFFFF, true);
 * screen.addButton(10, 20, 100, 20, "a button", JavaWrapper.methodToJava(function (button) {
 *   Chat.log("clicked");
 * }));
 * Hud.openScreen(screen);
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Screen and UI Elements")
public interface IScreen extends IDraw2D<IScreen> {

    /**
     * The simple name of this screen's class, without the package. A script made screen
     * reports {@code ScriptScreen}; a game's own screen reached through this reports
     * whatever the game calls it, such as {@code InventoryScreen}.
     * <p>
     * It is a class name rather than a screen title, so two screens with the same title
     * report the same thing as each other and it says nothing about what the screen is
     * for. {@link #getTitleText()} is the title.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   Chat.log(`${screen.getScreenClassName()}: ${screen.getTitleText()}`);
     * }
     * </pre>
     *
     * @return
     * @return the simple class name of this screen
     * @since 1.2.7
     */
    default String getScreenClassName() {
        return this.getClass().getSimpleName();
    }

    /**
     * The screen's title, which for a script made screen is whatever was passed to
     * {@code Hud.createScreen}. It is shown at the top of the screen by the game unless
     * drawing it is turned off, and it is not the same as the screen's class name.
     * <p>
     * This is a rich text object, so it keeps whatever styling the title was given
     * rather than being a plain string.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   Chat.log(`open: ${screen.getTitleText()}`);
     * }
     * </pre>
     *
     * @return
     * @return the title of this screen
     * @since 1.0.5 (TextHelper since 1.9.3)
     */
    TextHelper getTitleText();

    /**
     * in {@code 1.3.1} updated to work with all button widgets not just ones added by scripts.
     * <p>
     * Every button on this screen, whether a script put it there or the game did. The
     * game's own buttons are wrapped as they are found rather than skipped, so a script
     * that walks this list sees the whole screen rather than only its own additions.
     * Each entry is a helper, and it is the helper that carries the button's text and
     * whether it is currently enabled.
     * <p>
     * A button a script added is already a helper, so it appears as itself. One the game
     * made is wrapped fresh, so two calls give different helper objects for the same
     * button.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   for (const button of screen.getButtonWidgets()) {
     *     Chat.log(`button: ${button.getLabel()}`);
     *   }
     * }
     * </pre>
     *
     * @return every button on this screen
     * @since 1.0.5
     */
    List<ClickableWidgetHelper<?, ?>> getButtonWidgets();

    /**
     * in {@code 1.3.1} updated to work with all text fields not just ones added by scripts.
     * <p>
     * Every text field on this screen, whether a script put it there or the game did,
     * on the same terms as {@link #getButtonWidgets()}. A field the game made, such as
     * the search box in a recipe screen, is wrapped as it is found rather than skipped.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   for (const field of screen.getTextFields()) {
     *     Chat.log(`field says: ${field.getText()}`);
     *   }
     * }
     * </pre>
     *
     * @return every text field on this screen
     * @since 1.0.5
     */
    List<TextFieldWidgetHelper> getTextFields();

    /**
     * A clickable button, laid out at the given position and size and labelled with the
     * text. The callback is given the button and this screen, so it can read or change
     * the button as well as acting on the click.
     * <p>
     * The position is in the same screen space as every element coordinate, and a screen
     * lays its widgets out from the top left rather than from anything a script is
     * holding. The button is not drawn until the screen is opened.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.addButton(10, 20, 100, 20, "press me", JavaWrapper.methodToJava(function (button) {
     *   Chat.log("clicked");
     * }));
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param x screen x, the button's left edge
     * @param y screen y, the button's top edge
     * @param width how wide the button is
     * @param height how tall the button is
     * @param text the label on the button
     * @param callback calls your method as a {@link Consumer}&lt;{@link ClickableWidgetHelper}&gt;
     * @return the button, as a helper
     * @since 1.0.5
     */
    ClickableWidgetHelper<?, ?> addButton(int x, int y, int width, int height, String text, MethodWrapper<ClickableWidgetHelper<?, ?>, IScreen, Object, ?> callback);

    /**
     * The same as the shorter form with a z-index, so this button can be ordered against
     * the other elements on the screen. The two buttons in the example below are at the
     * same place for exactly that reason.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.addButton(10, 20, 100, 20, 0, "behind", JavaWrapper.methodToJava(function (button) {}));
     * screen.addButton(10, 20, 100, 20, 1, "in front", JavaWrapper.methodToJava(function (button) {}));
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param x screen x, the button's left edge
     * @param y screen y, the button's top edge
     * @param width how wide the button is
     * @param height how tall the button is
     * @param zIndex the z-index of the button against this screen's other elements
     * @param text the label on the button
     * @param callback calls your method as a {@link Consumer}&lt;{@link ClickableWidgetHelper}&gt;
     * @return the button, as a helper
     * @since 1.4.0
     */
    ClickableWidgetHelper<?, ?> addButton(int x, int y, int width, int height, int zIndex, String text, MethodWrapper<ClickableWidgetHelper<?, ?>, IScreen, Object, ?> callback);

    /**
     *                    {@link Consumer}&lt;{@link CheckBoxWidgetHelper}&gt;
     * @param x           the x position of the checkbox
     * @param y           the y position of the checkbox
     * @param width       the width of the checkbox
     * @param height      the height of the checkbox
     * @param text        the text to display next to the checkbox
     * @param checked     whether the checkbox is checked or not
     * @param showMessage whether to show the message or not
     * @param callback    calls your method as a
     * @return a {@link CheckBoxWidgetHelper} for the given input.
     * @since 1.8.4
     */
    CheckBoxWidgetHelper addCheckbox(int x, int y, int width, int height, String text, boolean checked, boolean showMessage, MethodWrapper<CheckBoxWidgetHelper, IScreen, Object, ?> callback);

    /**
     * A box with a label beside it that can be ticked. The callback is given the checkbox
     * and this screen and runs when it is toggled, and the checkbox reports whether it
     * is currently ticked.
     * <p>
     * There are four forms of this. The ones that take a z-index order the checkbox
     * against the screen's other elements, and the ones that take a show message
     * argument control the box the game draws a message in.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.addCheckbox(10, 20, 20, 20, "enabled", false, JavaWrapper.methodToJava(function (box) {
     *   Chat.log(`now ${box.isChecked()}`);
     * }));
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param x        the x position of the checkbox
     * @param y        the y position of the checkbox
     * @param width    the width of the checkbox
     * @param height   the height of the checkbox
     * @param text     the text to display next to the checkbox
     * @param checked  whether the checkbox is checked or not
     * @param callback calls your method as a {@link Consumer}&lt;{@link CheckBoxWidgetHelper}&gt;
     * @return a {@link CheckBoxWidgetHelper} for the given input.
     * @since 1.8.4
     */
    CheckBoxWidgetHelper addCheckbox(int x, int y, int width, int height, String text, boolean checked, MethodWrapper<CheckBoxWidgetHelper, IScreen, Object, ?> callback);

    /**
     * @param x        the x position of the checkbox
     * @param y        the y position of the checkbox
     * @param width    the width of the checkbox
     * @param height   the height of the checkbox
     * @param zIndex   the z-index of the checkbox
     * @param text     the text to display next to the checkbox
     * @param checked  whether the checkbox is checked or not
     * @param callback calls your method as a {@link Consumer}&lt;{@link CheckBoxWidgetHelper}&gt;
     * @return a {@link CheckBoxWidgetHelper} for the given input.
     * @since 1.8.4
     */
    CheckBoxWidgetHelper addCheckbox(int x, int y, int width, int height, int zIndex, String text, boolean checked, MethodWrapper<CheckBoxWidgetHelper, IScreen, Object, ?> callback);

    /**
     *                    {@link Consumer}&lt;{@link CheckBoxWidgetHelper}&gt;
     * @param x           the x position of the checkbox
     * @param y           the y position of the checkbox
     * @param width       the width of the checkbox
     * @param height      the height of the checkbox
     * @param zIndex      the z-index of the checkbox
     * @param text        the text to display next to the checkbox
     * @param checked     whether the checkbox is checked or not
     * @param showMessage whether to show the message or not
     * @param callback    calls your method as a
     * @return a {@link CheckBoxWidgetHelper} for the given input.
     * @since 1.8.4
     */
    CheckBoxWidgetHelper addCheckbox(int x, int y, int width, int height, int zIndex, String text, boolean checked, boolean showMessage, MethodWrapper<CheckBoxWidgetHelper, IScreen, Object, ?> callback);

    /**
     * A bar with a handle that can be dragged along it. The steps say how many
     * positions the handle snaps to, counting both ends, so a slider with 10 steps has
     * ten positions and moves in ninths rather than tenths. The value is on that same
     * 0 to 1 scale rather than being a position number.
     * <p>
     * A step count of 1 or less is raised to 2, so a slider always has at least the two
     * ends.
     * <p>
     * There are two forms without a step count. Those two build a different widget that
     * moves continuously instead, and otherwise take the same arguments.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.addSlider(10, 20, 100, 20, "volume", 0.5, 10, JavaWrapper.methodToJava(function (slider) {
     *   Chat.log(`now ${slider.getValue()}`);
     * }));
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param x        the x position of the slider
     * @param y        the y position of the slider
     * @param width    the width of the slider
     * @param height   the height of the slider
     * @param text     the text to be displayed inside the slider
     * @param value    the initial value of the slider
     * @param callback calls your method as a {@link Consumer}&lt;{@link SliderWidgetHelper}&gt;
     * @param steps    the number of steps the slider should have
     * @return a {@link SliderWidgetHelper} for the given input.
     * @since 1.8.4
     */
    SliderWidgetHelper addSlider(int x, int y, int width, int height, String text, double value, int steps, MethodWrapper<SliderWidgetHelper, IScreen, Object, ?> callback);

    /**
     * @param x        the x position of the slider
     * @param y        the y position of the slider
     * @param width    the width of the slider
     * @param height   the height of the slider
     * @param zIndex   the z-index of the slider
     * @param text     the text to be displayed inside the slider
     * @param value    the initial value of the slider
     * @param callback calls your method as a {@link Consumer}&lt;{@link SliderWidgetHelper}&gt;
     * @param steps    the number of steps the slider should have
     * @return a {@link SliderWidgetHelper} for the given input.
     * @since 1.8.4
     */
    SliderWidgetHelper addSlider(int x, int y, int width, int height, int zIndex, String text, double value, int steps, MethodWrapper<SliderWidgetHelper, IScreen, Object, ?> callback);

    /**
     * @param x        the x position of the slider
     * @param y        the y position of the slider
     * @param width    the width of the slider
     * @param height   the height of the slider
     * @param text     the text to be displayed inside the slider
     * @param value    the initial value of the slider
     * @param callback calls your method as a {@link Consumer}&lt;{@link SliderWidgetHelper}&gt;
     * @return a {@link SliderWidgetHelper} for the given input.
     * @since 1.8.4
     */
    SliderWidgetHelper addSlider(int x, int y, int width, int height, String text, double value, MethodWrapper<SliderWidgetHelper, IScreen, Object, ?> callback);

    /**
     * @param x        the x position of the slider
     * @param y        the y position of the slider
     * @param width    the width of the slider
     * @param height   the height of the slider
     * @param zIndex   the z-index of the slider
     * @param text     the text to be displayed inside the slider
     * @param value    the initial value of the slider
     * @param callback calls your method as a {@link Consumer}&lt;{@link SliderWidgetHelper}&gt;
     * @return a {@link SliderWidgetHelper} for the given input.
     * @since 1.8.4
     */
    SliderWidgetHelper addSlider(int x, int y, int width, int height, int zIndex, String text, double value, MethodWrapper<SliderWidgetHelper, IScreen, Object, ?> callback);

    /**
     * A padlock the player can click to lock and unlock. The callback is given the button
     * and this screen and runs on each click, and the button reports which state it is in.
     * <p>
     * This is the only button that takes no size: it draws at its own fixed size at the
     * position given.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.addLockButton(10, 20, JavaWrapper.methodToJava(function (button) {
     *   Chat.log(`locked: ${button.isLocked()}`);
     * }));
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param x        the x position of the lock button
     * @param y        the y position of the lock button
     * @param callback calls your method as a
     *                 {@link Consumer}&lt;{@link LockButtonWidgetHelper}&gt;
     * @return {@link LockButtonWidgetHelper} for the given input.
     * @since 1.8.4
     */
    LockButtonWidgetHelper addLockButton(int x, int y, MethodWrapper<LockButtonWidgetHelper, IScreen, Object, ?> callback);

    /**
     * @param x        the x position of the lock button
     * @param y        the y position of the lock button
     * @param zIndex   the z-index of the lock button
     * @param callback calls your method as a
     *                 {@link Consumer}&lt;{@link LockButtonWidgetHelper}&gt;
     * @return {@link LockButtonWidgetHelper} for the given input.
     * @since 1.8.4
     */
    LockButtonWidgetHelper addLockButton(int x, int y, int zIndex, MethodWrapper<LockButtonWidgetHelper, IScreen, Object, ?> callback);

    /**
     * A button that steps through a fixed list of values each time it is clicked, rather
     * than being a simple press. The label shows whichever value is current, and the
     * callback is given the button and this screen once the value has moved on.
     * <p>
     * There are four forms of this. The later ones add alternative values that the button
     * can be switched between, a prefix put in front of the value, and a function that
     * decides which set is in use.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.addCyclingButton(10, 20, 100, 20, ["low", "medium", "high"], "low",
     *   JavaWrapper.methodToJava(function (button) {
     *     Chat.log(`now ${button.getValue()}`);
     *   }));
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param x        the x position of the cycling button
     * @param y        the y position of the cycling button
     * @param width    the width of the cycling button
     * @param height   the height of the cycling button
     * @param callback calls your method as a
     *                 {@link Consumer}&lt;{@link CyclingButtonWidgetHelper}&gt;
     * @param values   the values to cycle through
     * @param initial  the initial value of the cycling button
     * @return {@link CyclingButtonWidgetHelper} for the given input.
     * @since 1.8.4
     */
    CyclingButtonWidgetHelper<?> addCyclingButton(int x, int y, int width, int height, String[] values, String initial, MethodWrapper<CyclingButtonWidgetHelper<?>, IScreen, Object, ?> callback);

    /**
     * @param x        the x position of the cycling button
     * @param y        the y position of the cycling button
     * @param width    the width of the cycling button
     * @param height   the height of the cycling button
     * @param zIndex   the z-index of the cycling button
     * @param callback calls your method as a
     *                 {@link Consumer}&lt;{@link CyclingButtonWidgetHelper}&gt;
     * @param values   the values to cycle through
     * @param initial  the initial value of the cycling button
     * @return {@link CyclingButtonWidgetHelper} for the given input.
     * @since 1.8.4
     */
    CyclingButtonWidgetHelper<?> addCyclingButton(int x, int y, int width, int height, int zIndex, String[] values, String initial, MethodWrapper<CyclingButtonWidgetHelper<?>, IScreen, Object, ?> callback);

    /**
     *                     {@link Consumer}&lt;{@link CyclingButtonWidgetHelper}&gt;
     * @param x            the x position of the cycling button
     * @param y            the y position of the cycling button
     * @param width        the width of the cycling button
     * @param height       the height of the cycling button
     * @param zIndex       the z-index of the cycling button
     * @param callback     calls your method as a
     * @param values       the values to cycle through
     * @param alternatives the alternative values to cycle through
     * @param prefix       the prefix of the values
     * @param initial      the initial value of the cycling button
     * @return {@link CyclingButtonWidgetHelper} for the given input.
     * @since 1.8.4
     */
    CyclingButtonWidgetHelper<?> addCyclingButton(int x, int y, int width, int height, int zIndex, String[] values, String[] alternatives, String initial, String prefix, MethodWrapper<CyclingButtonWidgetHelper<?>, IScreen, Object, ?> callback);

    /**
     *                        {@link Consumer}&lt;{@link CyclingButtonWidgetHelper}&gt;
     *                        alternative values
     * @param x               the x position of the cycling button
     * @param y               the y position of the cycling button
     * @param width           the width of the cycling button
     * @param height          the height of the cycling button
     * @param zIndex          the z-index of the cycling button
     * @param callback        calls your method as a
     * @param values          the values to cycle through
     * @param alternatives    the alternative values to cycle through
     * @param prefix          the prefix of the values
     * @param initial         the initial value of the cycling button
     * @param alternateToggle the method to determine if the cycling button should use the
     * @return {@link CyclingButtonWidgetHelper} for the given input.
     * @since 1.8.4
     */
    CyclingButtonWidgetHelper<?> addCyclingButton(int x, int y, int width, int height, int zIndex, String[] values, String[] alternatives, String initial, String prefix, MethodWrapper<?, ?, Boolean, ?> alternateToggle, MethodWrapper<CyclingButtonWidgetHelper<?>, IScreen, Object, ?> callback);

    /**
     * @param btn
     * @return
     * @since 1.0.5
     */
    @Deprecated
    IScreen removeButton(ClickableWidgetHelper<?, ?> btn);

    /**
     * @param x
     * @param y
     * @param width
     * @param height
     * @param message
     * @param onChange calls your method as a {@link Consumer}&lt;{@link String}&gt;
     * @return
     * @since 1.0.5
     */
    TextFieldWidgetHelper addTextInput(int x, int y, int width, int height, String message, MethodWrapper<String, IScreen, Object, ?> onChange);

    /**
     * A box the player can type into. The message is what it shows while nothing has been
     * typed, not a label beside it, and the callback is given the current text and this
     * screen on each change.
     * <p>
     * The field is not usable until the screen is opened, and it is emptied along with
     * the rest of the screen by an init, so anything typed into it is lost if the screen
     * is reloaded.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.addTextInput(10, 20, 100, 20, 0, "type a name", JavaWrapper.methodToJava(function (text) {
     *   Chat.log(`so far: ${text}`);
     * }));
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param x screen x, the field's left edge
     * @param y screen y, the field's top edge
     * @param width how wide the field is
     * @param height how tall the field is
     * @param zIndex the z-index of the field against this screen's other elements
     * @param message the placeholder shown while the field is empty
     * @param onChange calls your method as a {@link Consumer}&lt;{@link String}&gt;
     * @return the field, as a helper
     * @since 1.0.5
     */
    TextFieldWidgetHelper addTextInput(int x, int y, int width, int height, int zIndex, String message, MethodWrapper<String, IScreen, Object, ?> onChange);

    /**
     * @param inp
     * @return
     * @since 1.0.5
     */
    @Deprecated
    IScreen removeTextInput(TextFieldWidgetHelper inp);

    /**
     * Runs when the player presses a mouse button over this screen, and is given where
     * the mouse is and which button went down: 0 is left, 1 is right, 2 is middle. The
     * position is in the same screen space as every element coordinate.
     * <p>
     * A screen has one of these rather than a list, so setting it again replaces the last
     * one. Returning from it does not stop the click reaching the widgets underneath, so
     * this is for reacting rather than for swallowing.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.setOnMouseDown(JavaWrapper.methodToJava(function (pos, button) {
     *   Chat.log(`button ${button} at ${pos}`);
     * }));
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param onMouseDown calls your method as a {@link BiConsumer}&lt;{@link Pos2D}, {@link Integer}&gt;
     * @return self for chaining
     * @since 1.2.7
     */
    IScreen setOnMouseDown(MethodWrapper<Pos2D, Integer, Object, ?> onMouseDown);

    /**
     * Runs while the mouse is being dragged, and is given a segment from where the drag
     * started to where the mouse is now, plus the button that is down. The segment's two
     * ends are the start of the drag and the current position rather than the last two
     * positions, so it covers the whole drag rather than one step of it.
     * <p>
     * As with every other callback here, setting it again replaces the last one.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.setOnMouseDrag(JavaWrapper.methodToJava(function (from, button) {
     *   Chat.log(`dragging with ${button}, now at ${from.getEnd()}`);
     * }));
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param onMouseDrag calls your method as a {@link BiConsumer}&lt;{@link Vec2D}, {@link Integer}&gt;
     * @return self for chaining
     * @since 1.2.7
     */
    IScreen setOnMouseDrag(MethodWrapper<Vec2D, Integer, Object, ?> onMouseDrag);

    /**
     * Runs when a mouse button is released over this screen, and is given where the mouse
     * is and which button came up. It runs whether or not the press started on this
     * screen, so it can be the other half of a drag set up elsewhere.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.setOnMouseUp(JavaWrapper.methodToJava(function (pos, button) {
     *   Chat.log(`released ${button} at ${pos}`);
     * }));
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param onMouseUp calls your method as a {@link BiConsumer}&lt;{@link Pos2D}, {@link Integer}&gt;
     * @return self for chaining
     * @since 1.2.7
     */
    IScreen setOnMouseUp(MethodWrapper<Pos2D, Integer, Object, ?> onMouseUp);

    /**
     * Runs when the player scrolls over this screen, and is given the mouse position and
     * how far it scrolled. The second argument is a second position, not a single
     * number, and both of its coordinates are amounts rather than positions: the first
     * is the horizontal scroll and the second the vertical one, so a vertical wheel
     * notch comes through as a change in the second and nothing in the first.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.setOnScroll(JavaWrapper.methodToJava(function (pos, amount) {
     *   Chat.log(`scrolled by ${amount.getX()} across and ${amount.getY()} up`);
     * }));
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param onScroll calls your method as a {@link BiConsumer}&lt;{@link Pos2D}, {@link Pos2D}&gt;
     * @return self for chaining
     * @since 1.2.7
     */
    IScreen setOnScroll(MethodWrapper<Pos2D, Pos2D, Object, ?> onScroll);

    /**
     * Runs when a key goes down over this screen, and is given the key code and the
     * modifier keys that were held. The key code is the game's own rather than a
     * character's, so it is a number for a named key and {@link KeyBind} is the way to
     * name one.
     * <p>
     * It runs for every key including the ones the game handles itself, so a screen that
     * wants to close on escape has to check for it rather than assume it.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.setOnKeyPressed(JavaWrapper.methodToJava(function (key, modifiers) {
     *   Chat.log(`key ${key} with modifiers ${modifiers}`);
     * }));
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param onKeyPressed calls your method as a {@link BiConsumer}&lt;{@link Integer}, {@link Integer}&gt;
     * @return self for chaining
     * @since 1.2.7
     */
    IScreen setOnKeyPressed(MethodWrapper<Integer, Integer, Object, ?> onKeyPressed);

    /**
     * Runs when a key produces a character, and is given that character and the modifier
     * keys that were held. This is the pair to use for typing rather than
     * {@link #setOnKeyPressed(MethodWrapper)}, because it is what arrives once per
     * character typed rather than once per key, so shift and the layout have already been
     * applied by the time it runs.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.setOnCharTyped(JavaWrapper.methodToJava(function (chr, modifiers) {
     *   Chat.log(`typed ${chr}`);
     * }));
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param onCharTyped calls your method as a {@link BiConsumer}&lt;{@link Character}, {@link Integer}&gt;
     * @return self for chaining
     * @since 1.8.4
     */
    IScreen setOnCharTyped(MethodWrapper<Character, Integer, Object, ?> onCharTyped);

    /**
     * Runs when this screen closes, and is given the screen that is closing. It is a
     * notification rather than a replacement: the screen still closes and still goes back
     * to whatever was open before it, so this is where to save state rather than where to
     * decide whether to close.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.setOnClose(JavaWrapper.methodToJava(function (closing) {
     *   Chat.log(`closed ${closing.getTitleText()}`);
     * }));
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param onClose calls your method as a {@link Consumer}&lt;{@link IScreen}&gt;
     * @return self for chaining
     * @since 1.2.7
     */
    IScreen setOnClose(MethodWrapper<IScreen, Object, Object, ?> onClose);

    /**
     * closes this screen and hands control back to whatever was open before it.
     * <p>
     * This is the same as the player closing it, so the close callback runs and the
     * screen's own parent handling applies. It is the same as opening nothing, and a
     * screen with no parent set goes back to the game itself.
     * <p>
     * Closing a screen that is not open does nothing.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.addButton(10, 20, 100, 20, "close", JavaWrapper.methodToJava(function (button) {
     *   screen.close();
     * }));
     * Hud.openScreen(screen);
     * </pre>
     *
     * @since 1.1.9
     */
    void close();

    /**
     * calls the screen's init function re-loading it.
     * <p>
     * The screen is emptied and its init function runs again, which is what a script calls
     * after it has changed something the widgets were laid out from. The callback for
     * that init function is the one set with
     * {@link #setOnInit(MethodWrapper)}, so a screen whose widgets are built in an init
     * function is rebuilt by this; a screen that was filled in by hand loses its widgets
     * to it and has to be filled in again.
     * <p>
     * The screen stays open throughout. It is the layout that is rebuilt, not the screen.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.setOnInit(JavaWrapper.methodToJava(function (self) {
     *   self.addText("rebuilt", 10, 10, 0xFFFFFFFF, true);
     * }));
     * Hud.openScreen(screen);
     * // the widgets are laid out again from scratch
     * screen.reloadScreen();
     * </pre>
     *
     * @return self for chaining
     * @since 1.2.7
     */
    IScreen reloadScreen();

    /**
     * The builder is bound to this screen, so {@code build()} on it puts the button
     * where the matching {@link #addButton} call would have: it creates the widget and
     * hands it to {@code reAddElement} on the screen it was made from. The button's
     * position and size are normally given to the builder rather than the {@code add}
     * call, and the height is fixed at 20 however it is set.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("press me")
     *    .action(JavaWrapper.methodToJava(function (button) {
     *      Chat.log("clicked");
     *    }))
     *    .build();
     * Hud.openScreen(screen);
     * </pre>
     *
     * @return a new builder for buttons.
     * @since 1.8.4
     */
    ButtonWidgetHelper.ButtonBuilder buttonBuilder();

    /**
     * The builder is bound to this screen, so {@code build()} on it puts the
     * checkbox where the matching {@code addCheckbox} call would have. This form starts
     * unchecked; the one taking a boolean starts in that state.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.checkBoxBuilder()
     *    .pos(10, 20)
     *    .size(20, 20)
     *    .message("enabled")
     *    .build();
     * Hud.openScreen(screen);
     * </pre>
     *
     * @return a new builder for checkboxes.
     * @since 1.8.4
     */
    CheckBoxWidgetHelper.CheckBoxBuilder checkBoxBuilder();

    /**
     * The same as {@link #checkBoxBuilder()} with the starting state chosen here rather
     * than left unchecked.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.checkBoxBuilder(true)
     *    .pos(10, 20)
     *    .size(20, 20)
     *    .message("on by default")
     *    .build();
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param checked whether the checkbox should be checked by default
     * @return a new builder for checkboxes.
     * @since 1.8.4
     */
    CheckBoxWidgetHelper.CheckBoxBuilder checkBoxBuilder(boolean checked);

    /**
     * A cycling button holds values rather than a list of strings, and this is how each
     * one becomes the label on the button. Without it a button's values are assumed to
     * be strings; with it they can be anything and this decides how each is displayed.
     * <p>
     * The builder is bound to this screen, so {@code build()} on it puts the button
     * where the matching {@link #addCyclingButton} call would have.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
     *   return Chat.createTextHelperFromString(String(value));
     * }))
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .values(["low", "medium", "high"])
     *    .initially("low")
     *    .build();
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param valueToText turns a value into the text shown on the button
     * @return a new builder for cycling buttons.
     * @since 1.8.4
     */
    CyclingButtonWidgetHelper.CyclicButtonBuilder<?> cyclicButtonBuilder(MethodWrapper<Object, ?, TextHelper, ?> valueToText);

    /**
     * The builder is bound to this screen, so {@code build()} on it puts the button
     * where the matching {@link #addLockButton} call would have. This form starts
     * unlocked. A padlock draws at its own fixed size, so only its position is set.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.lockButtonBuilder()
     *    .pos(10, 20)
     *    .build();
     * Hud.openScreen(screen);
     * </pre>
     *
     * @return a new builder for lock buttons.
     * @since 1.8.4
     */
    LockButtonWidgetHelper.LockButtonBuilder lockButtonBuilder();

    /**
     * The same as {@link #lockButtonBuilder()} with the starting state chosen here rather
     * than left unlocked.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.lockButtonBuilder(true)
     *    .pos(10, 20)
     *    .build();
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param locked whether the lock button should be locked by default
     * @return a new builder for lock buttons.
     * @since 1.8.4
     */
    LockButtonWidgetHelper.LockButtonBuilder lockButtonBuilder(boolean locked);

    /**
     * The builder is bound to this screen, so {@code build()} on it puts the slider
     * where the matching {@code addSlider} call would have. The step count and the
     * callback are set on the builder rather than being arguments here.
     * <p>
     * The initial value is a whole position rather than a fraction. {@code initially}
     * takes an {@code int} clamped to {@code 0} through {@code steps - 1}, which is
     * divided by {@code steps - 1} to make the 0 to 1 value the widget holds, so
     * {@code 5} with ten steps starts it at five ninths of the way along rather than
     * at half. The clamp reads the step count as it stands, so the step count has to
     * be set first.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.sliderBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("volume")
     *    .steps(10)
     *    .initially(5)
     *    .action(JavaWrapper.methodToJava(function (slider) {
     *      Chat.log(`now ${slider.getValue()}`);
     *    }))
     *    .build();
     * Hud.openScreen(screen);
     * </pre>
     *
     * @return a new builder for sliders.
     * @since 1.8.4
     */
    SliderWidgetHelper.SliderBuilder sliderBuilder();

    /**
     * The builder is bound to this screen and has the screen's font, so
     * {@code build()} on it puts the field where the matching
     * {@link #addTextInput} call would have. The callback here is given the current
     * text and this screen, the same as on that call.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("type a name")
     *    .action(JavaWrapper.methodToJava(function (text) {
     *      Chat.log(`so far: ${text}`);
     *    }))
     *    .build();
     * Hud.openScreen(screen);
     * </pre>
     *
     * @return a new builder for text fields.
     * @since 1.8.4
     */
    TextFieldWidgetHelper.TextFieldBuilder textFieldBuilder();

    /**
     * A button drawn from a texture rather than with the game's own look. The builder is
     * bound to this screen, so {@code build()} on it puts the button where the
     * matching {@link #addButton} call would have. The four setters are the four states
     * the button is drawn in: enabled, disabled, enabled and focused, disabled and
     * focused. They go into the sprite set exactly as given, with no defaulting to the
     * enabled one, so a texture has to be given for every state the button can be seen
     * in.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.texturedButtonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .enabledTexture("minecraft:item/diamond")
     *    .disabledTexture("minecraft:item/emerald")
     *    .enabledFocusedTexture("minecraft:item/diamond")
     *    .disabledFocusedTexture("minecraft:item/emerald")
     *    .build();
     * Hud.openScreen(screen);
     * </pre>
     *
     * @return a new builder for textured buttons.
     * @since 1.8.4
     */
    ButtonWidgetHelper.TexturedButtonBuilder texturedButtonBuilder();

    //? if <=1.21.8 {
    /**
     * @return {@code true} if the shift key is pressed, {@code false} otherwise.
     * @since 1.8.4
     */
    default boolean isShiftDown() {
        return Screen.hasShiftDown();
    }

    /**
     * @return {@code true} if the ctrl key is pressed, {@code false} otherwise.
     * @since 1.8.4
     */
    default boolean isCtrlDown() {
        return Screen.hasControlDown();
    }

    /**
     * @return {@code true} if the alt key is pressed, {@code false} otherwise.
     * @since 1.8.4
     */
    default boolean isAltDown() {
        return Screen.hasAltDown();
    }
    //?}

    /**
     * the close callback, or {@code null} if none was set.
     * <p>
     * This is the callback itself rather than a result, so it is what to call to run the
     * close handling by hand, and what to check to find out whether the screen has one at
     * all. It is {@code null} on a screen that never had
     * {@link #setOnClose(MethodWrapper)} called on it, which is the normal case.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   Chat.log(`has a close callback: ${screen.getOnClose() !== null}`);
     * }
     * </pre>
     *
     * @return the close callback, or {@code null} if there is none
     * @since 1.2.7
     */
    @Nullable
    MethodWrapper<IScreen, Object, Object, ?> getOnClose();

}
