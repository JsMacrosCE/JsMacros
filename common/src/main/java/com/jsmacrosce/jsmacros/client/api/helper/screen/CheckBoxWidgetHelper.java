package com.jsmacrosce.jsmacros.client.api.helper.screen;

import net.minecraft.client.gui.components.Checkbox;
//? if >1.21.8 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
*///?}
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

import java.util.concurrent.atomic.AtomicReference;

/**
 * a checkbox: a box that is either ticked or not, and which the player toggles by
 * clicking it.
 * <p>
 * What is particular to a checkbox is that its state can be read and set, which is more
 * than the shared base offers for an ordinary button. Setting the state does not write
 * a flag: it presses the box, so the game's own change handler runs and a callback given
 * to the builder fires exactly as it would from a real click. That is the difference
 * from a plain field being set, and it is why the action is worth having here.
 * <p>
 * A script makes one through {@code IScreen.checkBoxBuilder()}, which starts unchecked,
 * or the form of that taking a boolean, which starts in the state given.
 * example:
 * <pre>
 * const screen = Hud.createScreen("a screen", true);
 * const box = screen.checkBoxBuilder()
 *    .pos(10, 20)
 *    .size(20, 20)
 *    .message("enabled")
 *    .action(JavaWrapper.methodToJava(function (btn) {
 *      // the action fires on a scripted change as well as a real click
 *      Chat.log(`now ${btn.isChecked()}`);
 *    }))
 *    .build();
 * Hud.openScreen(screen);
 *
 * // setting the state presses the box, so the action runs
 * box.setChecked(true);
 * Chat.log(`checked: ${box.isChecked()}`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Screen and UI Elements")
@SuppressWarnings("unused")
public class CheckBoxWidgetHelper extends ClickableWidgetHelper<CheckBoxWidgetHelper, Checkbox> {

    public CheckBoxWidgetHelper(Checkbox btn) {
        super(btn);
    }

    public CheckBoxWidgetHelper(Checkbox btn, int zIndex) {
        super(btn, zIndex);
    }

    /**
     * whether the box is ticked right now, read at the moment of the call. A box the
     * player has just clicked may not agree with what a script last set, so this is the
     * one to read rather than a value a script kept.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const box = screen.checkBoxBuilder()
     *    .pos(10, 20)
     *    .size(20, 20)
     *    .message("enabled")
     *    .build();
     * Hud.openScreen(screen);
     * // a player can have clicked it, so read the box rather than a value kept aside
     * if (box.isChecked()) {
     *   Chat.log(`${box.getLabel()} is ticked`);
     * }
     * </pre>
     *
     * @return {@code true} if this button is checked, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isChecked() {
        return base.selected();
    }

    /**
     * flips the box, whatever state it was in. This is the same as reading the current
     * state and passing the opposite to {@link #setChecked(boolean)}, including the
     * pressing: a box that was ticked is unticked through the game and a box that was
     * not is ticked, so the builder's action fires either way.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const box = screen.checkBoxBuilder()
     *    .pos(10, 20)
     *    .size(20, 20)
     *    .message("toggle me")
     *    .build();
     * Hud.openScreen(screen);
     * // three presses, back where it started
     * box.toggle().toggle().toggle();
     * Chat.log(`checked: ${box.isChecked()}`);
     * </pre>
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CheckBoxWidgetHelper toggle() {
        return setChecked(!base.selected());
    }

    /**
     * puts the box into a state, which is done by pressing it rather than by writing a
     * flag. A box that is already in the state asked for is left alone and nothing runs,
     * so setting a box to what it already is is a no-op rather than a second press. A
     * box that is not fires the change the game would fire on a real click, which means
     * the action given to the builder runs.
     * <p>
     * The press is sent at the box's own top left corner, because that is where a
     * scripted press goes, and a real click elsewhere inside the box is a different
     * position. The state still changes either way, since the box is the whole of what
     * the press is aimed at.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const box = screen.checkBoxBuilder()
     *    .pos(10, 20)
     *    .size(20, 20)
     *    .message("enabled")
     *    .build();
     * Hud.openScreen(screen);
     * // ticks it, because it started unticked
     * box.setChecked(true);
     * // and nothing happens this time, because it is already ticked
     * box.setChecked(true);
     * Chat.log(`checked: ${box.isChecked()}`);
     * </pre>
     *
     * @param checked whether to check or uncheck this button
     * @return self for chaining.
     * @since 1.8.4
     */
    public CheckBoxWidgetHelper setChecked(boolean checked) {
        if (base.selected() != checked) {
            //? if >1.21.8 {
            /*MouseButtonEvent fakeEvent = new MouseButtonEvent(
                    base.getX(),
                    base.getY(),
                    new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0));
            base.onPress(fakeEvent);
            *///?} else {
            base.onPress();
            //?}
        }
        return this;
    }

    @Override
    public String toString() {
        return String.format("CheckBoxWidgetHelper:{\"message\": \"%s\", \"checked\": %b}", base.getMessage().getString(), isChecked());
    }

    /**
     * the builder for a checkbox, handed back by {@code IScreen.checkBoxBuilder()} and
     * already bound to that screen. It starts unchecked, and the form of that taking a
     * boolean starts in the state given.
     * <p>
     * The starting state is only what the box is built with; a box on screen can be
     * ticked and unticked afterwards through the helper the builder hands back. The
     * height is taken from the shared builder rather than being pinned, so a checkbox
     * can be built at whatever height is asked for.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const builder = screen.checkBoxBuilder(true)
     *    .pos(10, 20)
     *    .size(20, 20)
     *    .message("on by default");
     * // the starting state is readable on the builder before anything is built
     * Chat.log(`starts checked: ${builder.isChecked()}`);
     * const box = builder.build();
     * Hud.openScreen(screen);
     * Chat.log(`built checked: ${box.isChecked()}`);
     * </pre>
     *
     * @author Etheradon
     * @since 1.8.4
     */
    @DocletCategory("Screen and UI Elements")
    public static class CheckBoxBuilder extends AbstractWidgetBuilder<CheckBoxBuilder, Checkbox, CheckBoxWidgetHelper> {

        private boolean checked = false;
        @Nullable
        private MethodWrapper<CheckBoxWidgetHelper, IScreen, Object, ?> action;

        public CheckBoxBuilder(IScreen screen) {
            super(screen);
        }

        /**
         * whether the box will be built ticked, which is only the starting state and
         * says nothing about the box after it has been on screen. It starts unticked.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * const builder = screen.checkBoxBuilder();
         * Chat.log(`starts checked: ${builder.isChecked()}`);
         * builder.checked(true);
         * Chat.log(`now: ${builder.isChecked()}`);
         * </pre>
         *
         * @return {@code true} if the checkbox is initially checked, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isChecked() {
            return checked;
        }

        /**
         * sets the state the box is built ticked in. This is only where it starts: the
         * box can be ticked and unticked afterwards through the helper, and a player
         * can click it, so this number is not what the box will go on being.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * screen.checkBoxBuilder()
         *    .pos(10, 20)
         *    .size(20, 20)
         *    .message("on by default")
         *    .checked(true)
         *    .build();
         * Hud.openScreen(screen);
         * </pre>
         *
         * @param checked whether the checkbox is initially checked or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public CheckBoxBuilder checked(boolean checked) {
            this.checked = checked;
            return this;
        }

        /**
         * @return the action to run when the button is pressed.
         * @since 1.8.4
         */
        @Nullable
        public MethodWrapper<CheckBoxWidgetHelper, IScreen, Object, ?> getAction() {
            return action;
        }

        /**
         * @param action the action to run when the button is pressed
         * @return self for chaining.
         * @since 1.8.4
         */
        public CheckBoxBuilder action(@Nullable MethodWrapper<CheckBoxWidgetHelper, IScreen, Object, ?> action) {
            this.action = action;
            return this;
        }

        @Override
        public CheckBoxWidgetHelper createWidget() {
            AtomicReference<CheckBoxWidgetHelper> b = new AtomicReference<>(null);
            Checkbox checkBox = Checkbox.builder(getMessage().getRaw(), mc.font).onValueChange((btn, value) -> {
                try {
                    if (action != null) {
                        action.accept(b.get(), screen);
                    }
                } catch (Exception e) {
                    JsMacrosClient.clientCore.profile.logError(e);
                }
            }).pos(getX(), getY()).selected(isChecked()).build();
            checkBox.setWidth(getWidth());
            checkBox.setHeight(getHeight());
            b.set(new CheckBoxWidgetHelper(checkBox, getZIndex()));
            return b.get();
        }

    }

}
