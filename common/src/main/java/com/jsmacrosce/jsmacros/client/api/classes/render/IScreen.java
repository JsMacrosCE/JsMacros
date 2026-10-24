package com.jsmacrosce.jsmacros.client.api.classes.render;

import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.api.math.Pos2D;
import com.jsmacrosce.jsmacros.api.math.Vec2D;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.screen.*;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A script-created screen combines {@link IDraw2D} drawing with interactive widgets and
 * screen input callbacks. Create one with {@code Hud.createScreen} and open it with
 * {@code Hud.openScreen}; call {@link #reloadScreen()} to clear and rebuild its elements
 * by rerunning its init callback.
 * <p>
 * Each input callback setter replaces the previous callback of that kind. Mouse
 * positions use the same screen coordinates as drawing elements.
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
     * Returns this screen's simple Java class name, not its title.
     *
      * @return the simple class name of this screen
     * @since 1.2.7
     */
    default String getScreenClassName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Returns this screen's title as rich text.
     *
      * @return the title of this screen
     * @since 1.0.5 (TextHelper since 1.9.3)
     */
    TextHelper getTitleText();

    /**
     * Returns helpers for script-added clickable widgets and {@code Button} widgets among
     * the screen's children. Script-added helpers are reused; other buttons receive new
     * wrappers on each call.
     *
     * @return an immutable list snapshot of the matching widget helpers
     * @since 1.0.5
     */
    List<ClickableWidgetHelper<?, ?>> getButtonWidgets();

    /**
     * Returns helpers for script-added text fields and {@code EditBox} widgets among the
     * screen's children. Script-added helpers are reused; other fields receive new
     * wrappers on each call.
     *
     * @return an immutable list snapshot of the matching text-field helpers
     * @since 1.0.5
     */
    List<TextFieldWidgetHelper> getTextFields();

    /**
     * Adds a clickable button at the given screen position and size. Its callback receives
     * the button helper.
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
     * Adds a clickable button ordered against other screen elements by {@code zIndex}.
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
     * Adds a checkbox whose callback runs when its checked state changes.
     * The {@code showMessage} parameter is currently ignored.
     * @param x           the x position of the checkbox
     * @param y           the y position of the checkbox
     * @param width       the width of the checkbox
     * @param height      the height of the checkbox
     * @param text        the text to display next to the checkbox
     * @param checked     whether the checkbox is checked or not
     * @param showMessage ignored; does not change label visibility
     * @param callback    called when the checkbox state changes
     * @return a {@link CheckBoxWidgetHelper} for the given input.
     * @since 1.8.4
     */
    CheckBoxWidgetHelper addCheckbox(int x, int y, int width, int height, String text, boolean checked, boolean showMessage, MethodWrapper<CheckBoxWidgetHelper, IScreen, Object, ?> callback);

    /**
     * Adds a checkbox whose callback runs when its checked state changes.
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
     * @param checked  the initial checked state
     * @param callback calls your method as a {@link Consumer}&lt;{@link CheckBoxWidgetHelper}&gt;
     * @return a {@link CheckBoxWidgetHelper} for the given input.
     * @since 1.8.4
     */
    CheckBoxWidgetHelper addCheckbox(int x, int y, int width, int height, String text, boolean checked, MethodWrapper<CheckBoxWidgetHelper, IScreen, Object, ?> callback);

    /**
     * Adds a checkbox ordered against other screen elements by {@code zIndex}.
     *
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
     * Adds a checkbox ordered by {@code zIndex} whose callback runs when its checked state changes.
     * The {@code showMessage} parameter is currently ignored.
     *
     * @param x           the x position of the checkbox
     * @param y           the y position of the checkbox
     * @param width       the width of the checkbox
     * @param height      the height of the checkbox
     * @param zIndex      the z-index of the checkbox
     * @param text        the text to display next to the checkbox
     * @param checked     whether the checkbox is checked or not
     * @param showMessage ignored; does not change label visibility
     * @param callback    called when the checked state changes
     * @return a {@link CheckBoxWidgetHelper} for the given input.
     * @since 1.8.4
     */
    CheckBoxWidgetHelper addCheckbox(int x, int y, int width, int height, int zIndex, String text, boolean checked, boolean showMessage, MethodWrapper<CheckBoxWidgetHelper, IScreen, Object, ?> callback);

    /**
     * Adds a slider whose value ranges from 0 to 1. The step count includes both ends;
     * counts below 2 are raised to 2. Overloads without a step count use a continuous slider.
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
     * @param value    the initial value on the 0-to-1 scale
     * @param callback calls your method as a {@link Consumer}&lt;{@link SliderWidgetHelper}&gt;
     * @param steps    the number of steps the slider should have
     * @return a {@link SliderWidgetHelper} for the given input.
     * @since 1.8.4
     */
    SliderWidgetHelper addSlider(int x, int y, int width, int height, String text, double value, int steps, MethodWrapper<SliderWidgetHelper, IScreen, Object, ?> callback);

    /**
     * Adds a stepped slider ordered against other screen elements by {@code zIndex}.
     *
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
     * Adds a continuous slider whose value ranges from 0 to 1.
     *
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
     * Adds a continuous slider ordered against other screen elements by {@code zIndex};
     * its value ranges from 0 to 1.
     *
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
     * Adds a lock button at its fixed size. Its callback runs when clicked.
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
     * @param callback called when the button is clicked
     *                 {@link Consumer}&lt;{@link LockButtonWidgetHelper}&gt;
     * @return {@link LockButtonWidgetHelper} for the given input.
     * @since 1.8.4
     */
    LockButtonWidgetHelper addLockButton(int x, int y, MethodWrapper<LockButtonWidgetHelper, IScreen, Object, ?> callback);

    /**
     * Adds a fixed-size lock button ordered against other screen elements by {@code zIndex}.
     *
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
     * Adds a button that cycles through the supplied values when clicked.
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
     * @param callback called when the value changes
     *                 {@link Consumer}&lt;{@link CyclingButtonWidgetHelper}&gt;
     * @param values   the values to cycle through
     * @param initial  the initial value of the cycling button
     * @return {@link CyclingButtonWidgetHelper} for the given input.
     * @since 1.8.4
     */
    CyclingButtonWidgetHelper<?> addCyclingButton(int x, int y, int width, int height, String[] values, String initial, MethodWrapper<CyclingButtonWidgetHelper<?>, IScreen, Object, ?> callback);

    /**
     * Adds a cycling button ordered against the screen's other elements by {@code zIndex}.
     *
     * @param x        the x position of the cycling button
     * @param y        the y position of the cycling button
     * @param width    the width of the cycling button
     * @param height   the height of the cycling button
     * @param zIndex   the z-index of the cycling button
     * @param values   the values to cycle through
     * @param initial  the initial value
     * @param callback called when the value changes
     * @return the added cycling button
     * @since 1.8.4
     */
    CyclingButtonWidgetHelper<?> addCyclingButton(int x, int y, int width, int height, int zIndex, String[] values, String initial, MethodWrapper<CyclingButtonWidgetHelper<?>, IScreen, Object, ?> callback);

    /**
     * Adds a cycling button with alternate display values, ordered by {@code zIndex}.
     * @param x            the x position of the cycling button
     * @param y            the y position of the cycling button
     * @param width        the width of the cycling button
     * @param height       the height of the cycling button
     * @param zIndex       the z-index of the cycling button
     * @param values       the values to cycle through
     * @param alternatives the alternative values to cycle through
     * @param prefix       the prefix of the values
     * @param initial      the initial value of the cycling button
     * @param callback     called when the value changes
     * @return the added cycling button
     * @since 1.8.4
     */
    CyclingButtonWidgetHelper<?> addCyclingButton(int x, int y, int width, int height, int zIndex, String[] values, String[] alternatives, String initial, String prefix, MethodWrapper<CyclingButtonWidgetHelper<?>, IScreen, Object, ?> callback);

    /**
     * Adds a cycling button with alternate display values and a callback-selected alternate list.
     * @param x               the x position of the cycling button
     * @param y               the y position of the cycling button
     * @param width           the width of the cycling button
     * @param height          the height of the cycling button
     * @param zIndex          the z-index of the cycling button
     * @param values          the values to cycle through
     * @param alternatives    the alternative values to cycle through
     * @param prefix          the prefix of the values
     * @param initial         the initial value of the cycling button
     * @param alternateToggle selects which value list is active
     * @param callback        called when the value changes
     * @return the added cycling button
     * @since 1.8.4
     */
    CyclingButtonWidgetHelper<?> addCyclingButton(int x, int y, int width, int height, int zIndex, String[] values, String[] alternatives, String initial, String prefix, MethodWrapper<?, ?, Boolean, ?> alternateToggle, MethodWrapper<CyclingButtonWidgetHelper<?>, IScreen, Object, ?> callback);

    /**
     * Removes the button from this screen.
     *
     * @param btn the button to remove
     * @return this screen
     * @since 1.0.5
     */
    @Deprecated
    @DocletIgnore
    IScreen removeButton(ClickableWidgetHelper<?, ?> btn);

    /**
     * Adds a text field; its message is shown as a placeholder while empty.
     *
     * @param x the field's left edge
     * @param y the field's top edge
     * @param width the field width
     * @param height the field height
     * @param message the placeholder text
     * @param onChange called with the changed text
     * @return the added text field
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
     * Removes the text field from this screen.
     *
     * @param inp the field to remove
     * @return this screen
     * @since 1.0.5
     */
    @Deprecated
    @DocletIgnore
    IScreen removeTextInput(TextFieldWidgetHelper inp);

    /**
     * Sets the callback for mouse-button presses over this screen. It receives the mouse
     * position and button number (0 left, 1 right, 2 middle), in screen coordinates.
     * <p>
     * A screen has one of these rather than a list, so setting it again replaces the last
     * one. Returning from it does not stop the click reaching the widgets underneath, so
     * this is for reacting rather than for swallowing.
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
     * Sets the callback for mouse dragging, which receives a segment from where the drag
     * started to where the mouse is now, plus the button that is down. The segment's two
     * ends are the start of the drag and the current position rather than the last two
     * positions, so it covers the whole drag rather than one step of it.
     * <p>
     * As with every other callback here, setting it again replaces the last one.
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
     * Sets the callback for mouse-button releases over this screen, receiving the mouse
     * position and which button came up. It runs whether or not the press started on this
     * screen, so it can be the other half of a drag set up elsewhere.
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
     * Runs when the player scrolls over this screen. The callback receives the mouse
     * position and a second {@link Pos2D} containing horizontal and vertical scroll
     * amounts in its x and y coordinates.
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
     * Runs when a key is pressed over this screen, receiving the game's key code and the
     * modifier-key bitmask. The key code identifies a key, not a typed character; use
     * {@code KeyBind} to refer to named keys.
     * Codes are GLFW key codes on older targets and SDL physical key codes on Minecraft 26.3
     * and later. Modifier bits remain shift=1, control=2, alt=4, and super=8 on every target.
     * <p>
     * This callback does not consume the key; game handling may still occur.
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
     * Runs when a key produces a character, receiving that character and the modifier-key
     * bitmask. Use this rather than {@link #setOnKeyPressed(MethodWrapper)} for text input;
     * the character reflects the active keyboard layout and modifiers.
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
     * Runs when this screen closes, receiving the screen being closed. It is a notification;
     * it does not prevent the close or change which screen is restored.
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
     * Closes this screen, invoking its close handling and returning to its parent if set.
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
     * Reinitializes this screen, clearing its elements and running the callback set with
     * {@link #setOnInit(MethodWrapper)}. Elements added outside that callback are cleared
     * too. Reinitialization is scheduled on the client thread.
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
     * Returns a button builder bound to this screen. Its {@code build()} adds the button
     * to this screen; button height is fixed at 20.
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
     * Returns a checkbox builder bound to this screen. Its {@code build()} adds the
     * checkbox here; this overload starts unchecked.
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
     * Returns a checkbox builder bound to this screen, initialized to {@code checked}.
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
     * Returns a cycling-button builder bound to this screen. The converter supplies the
     * text displayed for each value; it allows values other than strings.
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
     * Returns a lock-button builder bound to this screen. Its button starts unlocked
     * and has a fixed size.
     *
     * @return a new builder for lock buttons.
     * @since 1.8.4
     */
    LockButtonWidgetHelper.LockButtonBuilder lockButtonBuilder();

    /**
     * Returns a lock-button builder bound to this screen, with the initial locked state
     * set by {@code locked}. The button has a fixed size.
     *
     * @param locked whether the lock button should be locked by default
     * @return a new builder for lock buttons.
     * @since 1.8.4
     */
    LockButtonWidgetHelper.LockButtonBuilder lockButtonBuilder(boolean locked);

    /**
     * Returns a slider builder bound to this screen. Set its step count and callback
     * on the builder. Its initial position is a step index, clamped using the step
     * count at the time {@code initially} is called, then converted to the widget's
     * 0-to-1 value; set {@code steps} before {@code initially} when configuring both.
     *
     * @return a new builder for sliders.
     * @since 1.8.4
     */
    SliderWidgetHelper.SliderBuilder sliderBuilder();

    /**
     * Returns a text-field builder bound to this screen and using its font. Its action
     * receives the changed text and this screen, as with {@link #addTextInput}.
     *
     * @return a new builder for text fields.
     * @since 1.8.4
     */
    TextFieldWidgetHelper.TextFieldBuilder textFieldBuilder();

    /**
     * Returns a texture-drawn button builder bound to this screen. Supply textures for
     * the enabled, disabled, enabled-focused, and disabled-focused states; no state
     * falls back to another texture.
     *
     * @return a new builder for textured buttons.
     * @since 1.8.4
     */
    ButtonWidgetHelper.TexturedButtonBuilder texturedButtonBuilder();

    //? if <=1.21.8 {
    /**
     * @return whether the shift key is pressed.
     * @since 1.8.4
     */
    default boolean isShiftDown() {
        return Screen.hasShiftDown();
    }

    /**
     * @return whether the control key is pressed.
     * @since 1.8.4
     */
    default boolean isCtrlDown() {
        return Screen.hasControlDown();
    }

    /**
     * @return whether the alt key is pressed.
     * @since 1.8.4
     */
    default boolean isAltDown() {
        return Screen.hasAltDown();
    }
    //?}

    /**
     * Returns the close callback, or {@code null} if none was set. This is the callback
     * itself; invoking it manually runs the callback but does not itself close the screen.
     *
     * @return the close callback, or {@code null} if there is none
     * @since 1.2.7
     */
    @Nullable
    MethodWrapper<IScreen, Object, Object, ?> getOnClose();

}
