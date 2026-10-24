package com.jsmacrosce.jsmacros.client.api.helper.screen;

import net.minecraft.client.gui.components.Checkbox;
//? if >1.21.8 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import com.mojang.blaze3d.platform.InputConstants;
*///?}
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A checkbox widget whose checked state can be changed by the player or a script.
 * Scripted changes invoke the builder's change callback through the widget's press handler.
 * <pre>
 * const screen = Hud.createScreen("a screen", true);
 * const box = screen.checkBoxBuilder()
 *    .pos(10, 20)
 *    .size(20, 20)
 *    .message("enabled")
 *    .action(JavaWrapper.methodToJava(function (btn) {
 *      // the action fires on a scripted change as well as a player click
 *      Chat.log(`now ${btn.isChecked()}`);
 *    }))
 *    .build();
 * Hud.openScreen(screen);
 *
 * // setting a different state invokes the action
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

    /**
     * Wraps a checkbox with the default helper z-index of {@code 0}.
     *
     * @param btn the checkbox to wrap
     */
    public CheckBoxWidgetHelper(Checkbox btn) {
        super(btn);
    }

    /**
     * Wraps a checkbox with the supplied helper z-index.
     *
     * @param btn the checkbox to wrap
     * @param zIndex the helper's drawing-order index
     */
    public CheckBoxWidgetHelper(Checkbox btn, int zIndex) {
        super(btn, zIndex);
    }

    /**
     * Returns the widget's current checked state.
     *
     * @return {@code true} if this button is checked, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isChecked() {
        return base.selected();
    }

    /**
     * Toggles the checked state through the widget's press handler, invoking its change callback.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CheckBoxWidgetHelper toggle() {
        return setChecked(!base.selected());
    }

    /**
     * Sets the checked state by invoking the press handler if the requested state differs
     * from the current state. A changed state invokes the builder's change callback; an
     * unchanged state does nothing.
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
                    new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            base.onPress(fakeEvent);
            *///?} else {
            base.onPress();
            //?}
        }
        return this;
    }

    /**
     * Returns {@code CheckBoxWidgetHelper:{"message": "%s", "checked": %b}} with the
     * checkbox label and checked state; the label is not JSON-escaped.
     *
     * @return the formatted diagnostic string
     */
    @Override
    public String toString() {
        return String.format("CheckBoxWidgetHelper:{\"message\": \"%s\", \"checked\": %b}", base.getMessage().getString(), isChecked());
    }

    /**
     * Builder for a checkbox widget; its checked setting determines the widget's initial state.
     *
     * @author Etheradon
     * @since 1.8.4
     */
    @DocletCategory("Screen and UI Elements")
    public static class CheckBoxBuilder extends AbstractWidgetBuilder<CheckBoxBuilder, Checkbox, CheckBoxWidgetHelper> {

        private boolean checked = false;
        @Nullable
        private MethodWrapper<CheckBoxWidgetHelper, IScreen, Object, ?> action;

        /**
         * Associates this builder with the screen to which {@code build()} adds the widget.
         *
         * @param screen the screen used by {@code build()} to add the widget
         */
        public CheckBoxBuilder(IScreen screen) {
            super(screen);
        }

        /**
         * Returns whether this builder will create the checkbox initially checked.
         *
         * @return {@code true} if the checkbox is initially checked, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isChecked() {
            return checked;
        }

        /**
         * Sets the checkbox's initial checked state; it does not constrain later changes.
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
         * @return the configured change callback, or {@code null} if none is set.
         * @since 1.8.4
         */
        @Nullable
        public MethodWrapper<CheckBoxWidgetHelper, IScreen, Object, ?> getAction() {
            return action;
        }

        /**
         * @param action callback invoked when the checkbox state changes; {@code null} disables it
         * @return self for chaining.
         * @since 1.8.4
         */
        public CheckBoxBuilder action(@Nullable MethodWrapper<CheckBoxWidgetHelper, IScreen, Object, ?> action) {
            this.action = action;
            return this;
        }

        /**
         * Creates a checkbox from the configured label, position, checked state, dimensions,
         * and z-index, and connects its value-change callback. The callback forwards the
         * helper and associated screen to the configured action, logging caught exceptions.
         * This does not add the widget to the screen or apply {@code build()}'s alpha, active,
         * and visible flags.
         *
         * @return a helper for the newly created checkbox
         */
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
