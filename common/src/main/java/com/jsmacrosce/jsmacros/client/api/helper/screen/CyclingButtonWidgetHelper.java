package com.jsmacrosce.jsmacros.client.api.helper.screen;

import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinCyclingButton;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A button that displays a selected value and cycles through configured values when
 * pressed.
 * <p>
 * Values may have any type; the builder's converter supplies the text displayed for
 * each value.
 * <p>
 * When configured with a non-empty alternate list and a toggle, the toggle selects
 * which list cycling uses; otherwise it uses the default list.
 * <pre>
 * const screen = Hud.createScreen("a screen", true);
 * screen.setOnInit(JavaWrapper.methodToJava(function (self) {
 *   const button = self.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
 *     return Chat.createTextHelperFromString(String(value));
 *   }))
 *      .pos(10, 20)
 *      .size(100, 20)
 *      .option("quality")
 *      .values(["low", "medium", "high"])
 *      .initially("low")
 *      .build();
 *
 *   // the value is whatever type the values were, and the string form is the label
 *   Chat.log(`${button.getValue()}, shown as ${button.getStringValue()}`);
 *   // step to the next one, forwards or backwards
 *   button.forward();
 *   button.backward();
 * }));
 * Hud.openScreen(screen);
 * </pre>
 *
 * @param <T> the type of values displayed by the button
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Screen and UI Elements")
@SuppressWarnings("unused")
public class CyclingButtonWidgetHelper<T> extends ClickableWidgetHelper<CyclingButtonWidgetHelper<T>, CycleButton<T>> {

    /** Wraps {@code btn} with helper z-index {@code 0} and an empty tooltip list.
     * @param btn the cycling button to wrap
     */
    public CyclingButtonWidgetHelper(CycleButton<T> btn) {
        super(btn);
    }

    /** Wraps {@code btn} with the supplied helper z-index and an empty tooltip list.
     * @param btn the cycling button to wrap
     * @param zIndex the helper's draw-ordering value
     */
    public CyclingButtonWidgetHelper(CycleButton<T> btn, int zIndex) {
        super(btn, zIndex);
    }

    /**
     * Returns the selected value; it may have a different type from its displayed text.
     *
     * @return the current value.
     * @since 1.8.4
     */
    public T getValue() {
        return base.getValue();
    }

    /**
     * Returns the current value converted by the builder, as plain text without styling
     * or the option prefix. This need not match the value's {@code toString()}.
     *
     * @return the current value's converted display text, without styling or prefix.
     * @since 1.8.4
     */
    public String getStringValue() {
        return ((MixinCyclingButton<T>) base).getValueToText().apply(getValue()).getString();
    }

    /**
     * Sets the displayed value directly without cycling. If {@code val} is not in the
     * currently selected list, the display changes but the cycle index remains
     * unchanged.
     * <p>
     * The return value is {@code true} when the previous value equals {@code val}; the
     * value is set regardless. Equality uses {@code equals}.
     * <p>
     * A change here is written straight onto the button rather than pressed into it, so
     * the action given to the builder does not run.
     *
     * @param val the new value
     * @return {@code true} if the button was already on {@code val}, so this call left
     * it where it was, and {@code false} if the call moved it there.
     * @since 1.8.4
     */
    public boolean setValue(T val) {
        T lastVal = base.getValue();
        base.setValue(val);
        return lastVal.equals(base.getValue());
    }

    /**
     * Cycles by {@code amount} positions, wrapping at either end; positive values move
     * forward and negative values backward. The configured action runs for each cycle,
     * including a cycle that leaves the selected value unchanged.
     *
     * @param amount the amount to cycle by
     * @return self for chaining.
     * @since 1.8.4
     */
    public CyclingButtonWidgetHelper<T> cycle(int amount) {
        ((MixinCyclingButton) base).invokeCycle(amount);
        return this;
    }

    /**
     * Cycles forward once, wrapping to the first value at the end. The configured
     * action runs as it does for {@link #cycle(int)}.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CyclingButtonWidgetHelper<T> forward() {
        return cycle(1);
    }

    /**
     * Steps the button back one value, wrapping from the first value to the last. The
     * builder's action runs as it does during a click.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CyclingButtonWidgetHelper<T> backward() {
        return cycle(-1);
    }

    /** Returns a diagnostic string containing the current value's converted display text.
     * @return a diagnostic string containing the converted display text
     */
    @Override
    public String toString() {
        return String.format("CyclingButtonWidgetHelper:{\"value\": \"%s\"}", getStringValue());
    }

    /**
     * Builder for a cycling button, bound to the screen that created it. Supply a
     * value-to-text converter, then configure its values and optional initial value.
     *
     * @param <T> the type of values configured for the button
     * @author Etheradon
     * @since 1.8.4
     */
    @DocletCategory("Screen and UI Elements")
    public static class CyclicButtonBuilder<T> extends AbstractWidgetBuilder<CyclicButtonBuilder<T>, CycleButton<T>, CyclingButtonWidgetHelper<T>> {

        private T value = null;
        private Component optionText = Component.empty();
        @Nullable
        private MethodWrapper<CyclingButtonWidgetHelper<T>, IScreen, Object, ?> action;
        private MethodWrapper<T, ?, TextHelper, ?> valueToText;
        @Nullable
        private MethodWrapper<?, ?, Boolean, ?> alternateToggle;
        private boolean optionTextOmitted = false;
        private List<T> defaultValues = Collections.emptyList();
        private List<T> alternateValues = Collections.emptyList();

        /** Retains the screen and value-to-text converter for subsequent widget creation.
         * @param screen the screen associated with this builder
         * @param valueToText the function converting values to non-null display text
         */
        public CyclicButtonBuilder(IScreen screen, MethodWrapper<T, ?, TextHelper, ?> valueToText) {
            super(screen);
            this.valueToText = valueToText;
        }

        /**
         * @return the configured initial value, or {@code null} if none was set
         * @since 1.8.4
         */
        public T getInitialValue() {
            return value;
        }

        /**
         * Sets the value used to initialize the button. The value is not checked against
         * the configured lists.
         *
          * @param value the initial value for the cycling button
         * @return self for chaining.
         * @since 1.8.4
         */
        public CyclicButtonBuilder<T> initially(T value) {
            this.value = value;
            return this;
        }

        /**
         * @return the option prefix, or empty text if no option was set
         * @since 1.8.4
         */
        public TextHelper getOption() {
            return TextHelper.wrap(optionText);
        }

        /**
         * Sets the prefix displayed before the value, separated by a colon. Passing
         * {@code null} leaves the existing prefix unchanged.
         *
         * @param option the option text of the button
         * @return self for chaining.
         * @since 1.8.4
         */
        public CyclicButtonBuilder<T> option(String option) {
            if (option != null) {
                optionText = Component.literal(option);
            }
            return this;
        }

        /**
         * Sets the prefix from a text helper, preserving its styling. Passing
         * {@code null} leaves the existing prefix unchanged.
         *
         * @param option the option text of the button
         * @return self for chaining.
         * @since 1.8.4
         */
        public CyclicButtonBuilder<T> option(TextHelper option) {
            if (option != null) {
                optionText = option.getRaw();
            }
            return this;
        }

        /**
         * @return the configured action, or {@code null} if none is set
         * @since 1.8.4
         */
        @Nullable
        public MethodWrapper<CyclingButtonWidgetHelper<T>, IScreen, Object, ?> getAction() {
            return action;
        }

        /**
         * @param action the action to run when the button cycles
         * @return self for chaining.
         * @since 1.8.4
         */
        public CyclicButtonBuilder<T> action(@Nullable MethodWrapper<CyclingButtonWidgetHelper<T>, IScreen, Object, ?> action) {
            this.action = action;
            return this;
        }

        /**
         * @return the function used to convert values to button text
         * @since 1.8.4
         */
        public MethodWrapper<T, ?, TextHelper, ?> getValueToText() {
            return valueToText;
        }

        /**
         * @param valueToText the non-null function used to render each value as button text
         * @return self for chaining.
         * @since 1.8.4
         */
        public CyclicButtonBuilder<T> valueToText(MethodWrapper<T, ?, TextHelper, ?> valueToText) {
            if (valueToText != null) {
                this.valueToText = valueToText;
            }
            return this;
        }

        /**
         * Returns the default values, initially an empty list. These are used unless a configured toggle selects a non-empty alternate list.
         *
         * @return the list of all default values.
         * @since 1.8.4
         */
        public List<T> getDefaultValues() {
            return defaultValues;
        }

        /**
         * Returns the alternate values, initially an empty list. They are selected only when an alternate toggle is configured and this list is non-empty.
         *
         * @return the list of all alternate values.
         * @since 1.8.4
         */
        public List<T> getAlternateValues() {
            return alternateValues;
        }

        /**
         * Replaces the default values; the alternate values are unchanged.
         *
         * @param values the default values of the button
         * @return self for chaining.
         * @since 1.8.4
         */
        @SafeVarargs
        public final CyclicButtonBuilder<T> values(T... values) {
            this.defaultValues = Arrays.asList(values);
            return this;
        }

        /**
         * Replaces the alternate values; the default values are unchanged. An empty alternate list causes the builder to use only the default list, regardless of the toggle.
         *
         * @param values the alternate values of the button
         * @return self for chaining.
         * @since 1.8.4
         */
        @SafeVarargs
        public final CyclicButtonBuilder<T> alternatives(T... values) {
            this.alternateValues = Arrays.asList(values);
            return this;
        }

        /**
         * Sets the default and alternate values, replacing both lists while leaving the toggle unchanged.
         *
         * @param defaults     the default values of the button
         * @param alternatives the alternate values of the button
         * @return self for chaining.
         * @since 1.8.4
         */
        public CyclicButtonBuilder<T> values(T[] defaults, T[] alternatives) {
            return values(Arrays.asList(defaults), Arrays.asList(alternatives));
        }

        /**
         * Replaces both value lists and stores the supplied lists by reference, so later changes to either list are reflected by the builder.
         *
         * @param defaults     the default values of the button
         * @param alternatives the alternate values of the button
         * @return self for chaining.
         * @since 1.8.4
         */
        public CyclicButtonBuilder<T> values(List<T> defaults, List<T> alternatives) {
            this.defaultValues = defaults;
            this.alternateValues = alternatives;
            return this;
        }

        /**
         * Returns the toggle that selects the list during cycling, or {@code null} if none is configured.
         *
         * @return the configured toggle, or {@code null} if unset.
         * @since 1.8.4
         */
        @Nullable
        public MethodWrapper<?, ?, Boolean, ?> getAlternateToggle() {
            return alternateToggle;
        }

        /**
         * Sets the function that selects the value list for each cycle: {@code true}
         * selects the alternate list and {@code false} the default list. Passing
         * {@code null} leaves the existing toggle unchanged.
         *
         * @param alternateToggle the toggle function to determine if the button should cycle
         *                        through the default or the alternate values
         * @return self for chaining.
         * @since 1.8.4
         */
        public CyclicButtonBuilder<T> alternateToggle(@Nullable MethodWrapper<?, ?, Boolean, ?> alternateToggle) {
            if (alternateToggle != null) {
                this.alternateToggle = alternateToggle;
            }
            return this;
        }

        /**
         * Returns whether the builder is configured to omit the option prefix; initially
         * {@code false}. A blank prefix is omitted regardless of this setting.
         *
         * @return {@code true} if the prefix option text should be omitted, {@code false}
         * otherwise.
         * @since 1.8.4
         */
        public boolean isOptionTextOmitted() {
            return optionTextOmitted;
        }

        /**
         * Sets whether to display only the value instead of the option prefix and value.
         * A blank prefix is omitted regardless of this setting.
         *
         * @param optionTextOmitted whether the prefix option text should be omitted or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public CyclicButtonBuilder<T> omitTextOption(boolean optionTextOmitted) {
            this.optionTextOmitted = optionTextOmitted;
            return this;
        }

        /** Creates and wraps a cycling button from the builder's current configuration; this does not add it to the screen or apply the common post-creation styles. Use {@code build()} to create, configure, and add it.
         * @return the newly wrapped cycling button
         */
        @Override
        public CyclingButtonWidgetHelper<T> createWidget() {
            AtomicReference<CyclingButtonWidgetHelper<T>> b = new AtomicReference<>(null);
            //? if >=1.21.11 {
            /*CycleButton.Builder<T> builder = CycleButton.builder(
                    obj -> Component.literal(valueToText.apply(obj).getRaw().getString()),
                    value
            );
            *///? } else {
            CycleButton.Builder<T> builder = CycleButton.builder(obj -> valueToText.apply(obj).getRaw());
            //? }
            if (optionTextOmitted || StringUtils.isBlank(optionText.getString())) {
                builder.displayOnlyValue();
            }
            if (alternateToggle != null && !alternateValues.isEmpty()) {
                builder.withValues(alternateToggle::get, defaultValues, alternateValues);
            } else {
                builder.withValues(defaultValues);
            }
            //? if <1.21.11 {
            builder.withInitialValue(value);
            //? }
            CycleButton<T> cyclingButton = builder.create(getX(), getY(), getWidth(), getHeight(), optionText, (btn, val) -> {
                try {
                    if (action != null) {
                        action.accept(b.get(), screen);
                    }
                } catch (Exception e) {
                    JsMacrosClient.clientCore.profile.logError(e);
                }
                clickedOn(screen);
            });
            b.set(new CyclingButtonWidgetHelper<>(cyclingButton, getZIndex()));
            return b.get();
        }

    }

}
