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
 * a cycling button: a button that holds a value out of a list and steps to the next one
 * each time it is pressed, rather than holding a fixed label.
 * <p>
 * The values are not necessarily strings, and how each one is turned into the label on
 * the button is given to the builder rather than worked out from the value. A builder
 * made through {@code IScreen.cyclicButtonBuilder(...)} is handed that conversion as its
 * first argument, and it is the one thing a cycling button cannot be built without.
 * <p>
 * There are two lists a cycling button can hold, and which one it steps through is
 * decided by a toggle rather than by a setting: the default values are what it uses
 * normally, and the alternate values are what it uses on the ticks where the toggle
 * says so. A button built with no alternate values, or with no toggle, always steps
 * through the default ones.
 * example:
 * <pre>
 * const screen = Hud.createScreen("a screen", true);
 * const button = screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
 *   return Chat.createTextHelperFromString(String(value));
 * }))
 *    .pos(10, 20)
 *    .size(100, 20)
 *    .option("quality")
 *    .values(["low", "medium", "high"])
 *    .initially("low")
 *    .build();
 * Hud.openScreen(screen);
 *
 * // the value is whatever type the values were, and the string form is the label
 * Chat.log(`${button.getValue()}, shown as ${button.getStringValue()}`);
 * // step to the next one, forwards or backwards
 * button.forward();
 * button.backward();
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Screen and UI Elements")
@SuppressWarnings("unused")
public class CyclingButtonWidgetHelper<T> extends ClickableWidgetHelper<CyclingButtonWidgetHelper<T>, CycleButton<T>> {

    public CyclingButtonWidgetHelper(CycleButton<T> btn) {
        super(btn);
    }

    public CyclingButtonWidgetHelper(CycleButton<T> btn, int zIndex) {
        super(btn, zIndex);
    }

    /**
     * the value the button is on right now, which is one of the values the builder was
     * given and is whatever type those were rather than necessarily a string. Reading
     * it is how a script finds out which one the player has landed on.
     * <p>
     * The label drawn on the button is a separate thing: it is the value put through the
     * conversion the builder was given, and {@link #getStringValue()} is what gives it
     * as text.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
     *   return Chat.createTextHelperFromString(String(value));
     * }))
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .values(["low", "medium", "high"])
     *    .initially("low")
     *    .build();
     * Hud.openScreen(screen);
     * // the value itself, and the same value as the label the player sees
     * Chat.log(`${button.getValue()} shown as ${button.getStringValue()}`);
     * </pre>
     *
     * @return the current value.
     * @since 1.8.4
     */
    public T getValue() {
        return base.getValue();
    }

    /**
     * the label drawn on the button right now, as a plain string with the styling taken
     * out. This is the current value put through the conversion the builder was given,
     * which is not the same as calling {@code toString()} on the value: a builder can
     * be given any conversion, so a value of any type can be shown as any text.
     * <p>
     * The prefix the builder was given, if any, is not part of this; it is the value's
     * own half of the label. There is a separate reader for the prefix on the builder.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
     *   // the value decides its own label, with no namespace on it
     *   return Chat.createTextHelperFromString(value.toUpperCase());
     * }))
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .values(["low", "medium", "high"])
     *    .initially("low")
     *    .build();
     * Hud.openScreen(screen);
     * // the value half of the label, which is "LOW" here
     * Chat.log(button.getStringValue());
     * </pre>
     *
     * @return the current value in their string representation.
     * @since 1.8.4
     */
    public String getStringValue() {
        return ((MixinCyclingButton<T>) base).getValueToText().apply(getValue()).getString();
    }

    /**
     * puts the button on a value, which is a direct choice rather than a step through
     * the list. The value has to be one the builder was given; nothing here checks that
     * it is one of them, and a value that is not leaves the button showing it while its
     * place in the list stays where it was, so the next step goes from the old place
     * rather than from the new value.
     * <p>
     * The return value reads backwards from what its name suggests, and the flag
     * answers whether the button was <b>already</b> on this value rather than whether
     * it has moved onto it. The button takes whatever value it is given without
     * checking it against the old one, so afterwards it is on {@code val} either way,
     * and the flag is the old value compared against the new one: {@code true} when
     * they are the same, so a call that actually moved the button answers
     * {@code false}. That comparison is {@code equals} rather than identity, so a
     * value type that does not compare by value can answer {@code true} for a value
     * that looks the same.
     * <p>
     * A change here is written straight onto the button rather than pressed into it, so
     * the action given to the builder does not run.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
     *   return Chat.createTextHelperFromString(String(value));
     * }))
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .values(["low", "medium", "high"])
     *    .initially("low")
     *    .build();
     * Hud.openScreen(screen);
     * // the button was on "low", so this call moved it and the flag is false
     * Chat.log(`was already on that: ${button.setValue("high")}`);
     * // the button is on "high" now, so this call changes nothing and the flag is true
     * Chat.log(`was already on that: ${button.setValue("high")}`);
     * </pre>
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
     * steps the button a number of places through its list, forwards for a positive
     * amount and backwards for a negative one, wrapping round at either end. This is the
     * call the other two are made from, and a step of zero leaves the button where it
     * is.
     * <p>
     * A step goes through whichever of the two lists the alternate toggle currently
     * selects, so a button with an alternate list and a toggle steps through one list on
     * one press and the other on the next.
     * <p>
     * The step is taken rather than written, so the action given to the builder runs as
     * it does on a real click. It runs on every step, even one that lands back on the
     * value the button already had, which is what happens on a list of one.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
     *   return Chat.createTextHelperFromString(String(value));
     * }))
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .values(["low", "medium", "high"])
     *    .initially("low")
     *    .build();
     * Hud.openScreen(screen);
     * // three forwards, which is one whole trip round
     * button.cycle(3);
     * Chat.log(`back to ${button.getValue()}`);
     * </pre>
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
     * steps the button one place forwards through its list, wrapping round to the start
     * at the end. This is the same as {@link #cycle(int)} with an amount of one, and
     * it runs the action given to the builder as a real click does.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
     *   return Chat.createTextHelperFromString(String(value));
     * }))
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .values(["low", "medium", "high"])
     *    .initially("low")
     *    .build();
     * Hud.openScreen(screen);
     * button.forward();
     * Chat.log(`now ${button.getStringValue()}`);
     * </pre>
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CyclingButtonWidgetHelper<T> forward() {
        return cycle(1);
    }

    /**
     * steps the button one place backwards through its list, wrapping round to the end
     * at the start. This is the same as {@link #cycle(int)} with an amount of minus one,
     * and it runs the action given to the builder as a real click does.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const button = screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
     *   return Chat.createTextHelperFromString(String(value));
     * }))
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .values(["low", "medium", "high"])
     *    .initially("high")
     *    .build();
     * Hud.openScreen(screen);
     * // backwards off the end of the list wraps to the start
     * button.backward();
     * Chat.log(`now ${button.getStringValue()}`);
     * </pre>
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CyclingButtonWidgetHelper<T> backward() {
        return cycle(-1);
    }

    @Override
    public String toString() {
        return String.format("CyclingButtonWidgetHelper:{\"value\": \"%s\"}", getStringValue());
    }

    /**
     * the builder for a cycling button, handed back by
     * {@code IScreen.cyclicButtonBuilder(...)} and already bound to that screen.
     * <p>
     * The conversion from a value to the text on the button is passed in when the
     * builder is made and cannot be left out, because a cycling button holds values
     * rather than strings and this is what decides how each is shown. The list of values
     * and the starting value are set on the builder, and there are two lists with a
     * toggle choosing between them.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const builder = screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
     *   return Chat.createTextHelperFromString(String(value));
     * }))
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .option("quality")
     *    .values(["low", "medium", "high"])
     *    .initially("low");
     * // all of it is readable on the builder before anything is built
     * Chat.log(`${builder.getDefaultValues().size()} values, `
     *   + `starting at ${builder.getInitialValue()}, `
     *   + `option "${builder.getOption().getString()}"`);
     * const button = builder.build();
     * Hud.openScreen(screen);
     * </pre>
     *
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

        public CyclicButtonBuilder(IScreen screen, MethodWrapper<T, ?, TextHelper, ?> valueToText) {
            super(screen);
            this.valueToText = valueToText;
        }

        /**
         * the value the button will be built on, which is {@code null} until something
         * is set. The built button does not have to be on this value afterwards, since
         * a player can cycle it and a script can set it outright.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * const builder = screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
         *   return Chat.createTextHelperFromString(String(value));
         * }))
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .values(["low", "medium", "high"]);
         * Chat.log(`starts on: ${builder.getInitialValue()}`);
         * builder.initially("medium");
         * Chat.log(`now: ${builder.getInitialValue()}`);
         * </pre>
         *
         * @return the initial value of the slider.
         * @since 1.8.4
         */
        public T getInitialValue() {
            return value;
        }

        /**
         * sets the value the button is built on. It is one of the values the builder
         * holds, and nothing here checks that, so a value from somewhere else leaves the
         * button on something its own list does not have.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
         *   return Chat.createTextHelperFromString(String(value));
         * }))
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .values(["low", "medium", "high"])
         *    .initially("medium")
         *    .build();
         * Hud.openScreen(screen);
         * </pre>
         *
         * @param value the initial value of the slider
         * @return self for chaining.
         * @since 1.8.4
         */
        public CyclicButtonBuilder<T> initially(T value) {
            this.value = value;
            return this;
        }

        /**
         * The option text is a prefix of all values, separated by a colon.
         * <p>
         * It starts as empty text rather than as {@code null}, so a builder with no
         * option set still hands back something to read. What the built button does
         * with an empty option is a separate question on the builder.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * const builder = screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
         *   return Chat.createTextHelperFromString(String(value));
         * }))
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .values(["low", "medium", "high"]);
         * Chat.log(`starts as "${builder.getOption().getString()}"`);
         * builder.option("quality");
         * Chat.log(`now "${builder.getOption().getString()}"`);
         * </pre>
         *
         * @return the option text of the button or an empty text if it is omitted.
         * @since 1.8.4
         */
        public TextHelper getOption() {
            return TextHelper.wrap(optionText);
        }

        /**
         * sets the prefix drawn in front of the value on the button. The value's own
         * label follows it after a colon, so a prefix of {@code quality} and a value of
         * {@code low} is drawn as {@code quality: low}.
         * <p>
         * Passing {@code null} leaves the prefix as it was rather than clearing it, so a
         * chain that sets a prefix and then passes {@code null} keeps the first one.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * const button = screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
         *   return Chat.createTextHelperFromString(String(value));
         * }))
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .option("quality")
         *    .values(["low", "medium", "high"])
         *    .initially("low")
         *    .build();
         * Hud.openScreen(screen);
         * // the value half of the label, which is all the helper gives back
         * Chat.log(`showing ${button.getStringValue()}`);
         * </pre>
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
         * the same as the string form, with the prefix built as a text helper so that it
         * can carry styling. Passing {@code null} leaves the prefix as it was rather
         * than clearing it.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
         *   return Chat.createTextHelperFromString(String(value));
         * }))
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .option(Chat.createTextHelperFromString("quality"))
         *    .values(["low", "medium", "high"])
         *    .build();
         * Hud.openScreen(screen);
         * </pre>
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
         * @return the action to run when the button is pressed.
         * @since 1.8.4
         */
        @Nullable
        public MethodWrapper<CyclingButtonWidgetHelper<T>, IScreen, Object, ?> getAction() {
            return action;
        }

        /**
         * @param action the action to run when the button is pressed
         * @return self for chaining.
         * @since 1.8.4
         */
        public CyclicButtonBuilder<T> action(@Nullable MethodWrapper<CyclingButtonWidgetHelper<T>, IScreen, Object, ?> action) {
            this.action = action;
            return this;
        }

        /**
         * @return the function to convert a value to a text.
         * @since 1.8.4
         */
        public MethodWrapper<T, ?, TextHelper, ?> getValueToText() {
            return valueToText;
        }

        /**
         * @param valueToText the function to convert a value to a text
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
         * The button will normally cycle through the default values, but if the alternate toggle is
         * true, it will cycle through the alternate values.
         * <p>
         * It starts as an empty list rather than as {@code null}, and a button built with
         * no values at all has nothing to cycle through.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * const builder = screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
         *   return Chat.createTextHelperFromString(String(value));
         * }))
         *    .pos(10, 20)
         *    .size(100, 20);
         * Chat.log(`starts with ${builder.getDefaultValues().size()} values`);
         * builder.values(["low", "medium", "high"]);
         * Chat.log(`now ${builder.getDefaultValues().size()} values`);
         * </pre>
         *
         * @return the list of all default values.
         * @since 1.8.4
         */
        public List<T> getDefaultValues() {
            return defaultValues;
        }

        /**
         * the second of the two lists the button can hold, and the one it steps
         * through on a press where {@link #getAlternateToggle()} answers {@code true}
         * rather than the default ones.
         * <p>
         * It starts as an empty list, and an empty alternate list is what makes a button
         * ignore its toggle: the toggle is only consulted when there is something on the
         * other side of it.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * const builder = screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
         *   return Chat.createTextHelperFromString(String(value));
         * }))
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .values(["low", "medium", "high"]);
         * Chat.log(`starts with ${builder.getAlternateValues().size()} alternatives`);
         * builder.alternatives(["1", "2", "3"]);
         * Chat.log(`now ${builder.getAlternateValues().size()}`);
         * </pre>
         *
         * @return the list of all alternate values.
         * @since 1.8.4
         */
        public List<T> getAlternateValues() {
            return alternateValues;
        }

        /**
         * sets the values the button cycles through normally, as a list of arguments
         * rather than as a list. This replaces the whole list rather than adding to it,
         * so calling it twice leaves only the second list.
         * <p>
         * Calling it does not touch the alternate list, so a button can have values and
         * no alternatives or the other way round.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
         *   return Chat.createTextHelperFromString(String(value));
         * }))
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .values("low", "medium", "high")
         *    .initially("low")
         *    .build();
         * Hud.openScreen(screen);
         * </pre>
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
         * sets the values the button cycles through when its toggle says so, as a list
         * of arguments rather than as a list. This replaces the whole alternate list,
         * and it is what makes the toggle worth having: a button with no alternate list
         * cycles through the default values whatever the toggle says.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
         *   return Chat.createTextHelperFromString(String(value));
         * }))
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .option("difficulty")
         *    .values("easy", "normal", "hard")
         *    .alternatives("peaceful", "easy")
         *    .alternateToggle(JavaWrapper.methodToJava(function () {
         *       // true puts the button on the alternate list for this press
         *       return true;
         *     }))
         *    .build();
         * Hud.openScreen(screen);
         * </pre>
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
         * sets both lists at once, as two arrays rather than as two lists. This is the
         * same as the two list form with each array turned into a list, so it replaces
         * both of them and leaves the toggle alone.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * const builder = screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
         *   return Chat.createTextHelperFromString(String(value));
         * }))
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .values(["easy", "normal", "hard"], ["peaceful", "easy"]);
         * Chat.log(`${builder.getDefaultValues().size()} and `
         *   + `${builder.getAlternateValues().size()}`);
         * </pre>
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
         * sets both lists at once, as two lists rather than as two arrays. This replaces
         * both of them, and it is the form to reach for when the lists are built
         * somewhere else rather than written out as arguments.
         * <p>
         * The lists are held as they are given rather than copied, so a list that is
         * changed afterwards changes the builder.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * const defaults = ["easy", "normal", "hard"];
         * const alternatives = ["peaceful", "easy"];
         * const builder = screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
         *   return Chat.createTextHelperFromString(String(value));
         * }))
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .values(defaults, alternatives);
         * Chat.log(`${builder.getDefaultValues().size()} and `
         *   + `${builder.getAlternateValues().size()}`);
         * </pre>
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
         * the toggle that chooses between the two lists, which is {@code null} until one
         * is set. It is asked once per step, and {@code true} means step through the
         * alternate values on this one.
         * <p>
         * A builder with a toggle but no alternate values is no different from one with
         * neither, because the toggle is only asked when there is something on the other
         * side of it.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * const builder = screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
         *   return Chat.createTextHelperFromString(String(value));
         * }))
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .values("easy", "normal", "hard")
         *    .alternatives("peaceful", "easy");
         * Chat.log(`has a toggle: ${builder.getAlternateToggle() !== null}`);
         * builder.alternateToggle(JavaWrapper.methodToJava(function () {
         *   return true;
         * }));
         * Chat.log(`now: ${builder.getAlternateToggle() !== null}`);
         * </pre>
         *
         * @return the toggle function to determine if the button should cycle through the default
         * or the alternate values.
         * @since 1.8.4
         */
        @Nullable
        public MethodWrapper<?, ?, Boolean, ?> getAlternateToggle() {
            return alternateToggle;
        }

        /**
         * sets the toggle that chooses between the two lists. It is asked on each step,
         * and {@code true} means the alternate values this time while {@code false}
         * means the default ones, so a toggle that answers the same way every time gives
         * a button that only ever cycles through one list.
         * <p>
         * Passing {@code null} leaves whatever was set rather than clearing it, so a
         * chain that sets a toggle and then passes {@code null} keeps the first one.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
         *   return Chat.createTextHelperFromString(String(value));
         * }))
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .option("difficulty")
         *    .values("easy", "normal", "hard")
         *    .alternatives("peaceful", "easy")
         *    // every other press uses the alternate list
         *    .alternateToggle(JavaWrapper.methodToJava(function () {
         *       return Math.random() > 0.5;
         *     }))
         *    .build();
         * Hud.openScreen(screen);
         * </pre>
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
         * whether the prefix is left off the button when it is built, which starts out
         * {@code false}. This is a separate thing from setting the prefix: the prefix can
         * be set and still be left off.
         * <p>
         * The prefix is left off whenever this is on and also when the prefix is blank,
         * so a builder with no prefix set omits it either way and this makes no
         * difference until there is something to omit.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * const builder = screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
         *   return Chat.createTextHelperFromString(String(value));
         * }))
         *    .pos(10, 20)
         *    .size(100, 20);
         * Chat.log(`omits the option: ${builder.isOptionTextOmitted()}`);
         * builder.omitTextOption(true);
         * Chat.log(`now: ${builder.isOptionTextOmitted()}`);
         * </pre>
         *
         * @return {@code true} if the prefix option text should be omitted, {@code false}
         * otherwise.
         * @since 1.8.4
         */
        public boolean isOptionTextOmitted() {
            return optionTextOmitted;
        }

        /**
         * sets whether the prefix is left off the button. With it on the button is drawn
         * with the value alone rather than with the prefix and a colon in front of it.
         * <p>
         * The prefix can be set and still be left off this way, and a builder with no
         * prefix set omits it either way, so this only makes a difference once there is
         * a prefix to leave off.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * screen.cyclicButtonBuilder(JavaWrapper.methodToJava(function (value) {
         *   return Chat.createTextHelperFromString(String(value));
         * }))
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .option("quality")
         *    // drawn as "low" rather than "quality: low"
         *    .omitTextOption(true)
         *    .values("low", "medium", "high")
         *    .initially("low")
         *    .build();
         * Hud.openScreen(screen);
         * </pre>
         *
         * @param optionTextOmitted whether the prefix option text should be omitted or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public CyclicButtonBuilder<T> omitTextOption(boolean optionTextOmitted) {
            this.optionTextOmitted = optionTextOmitted;
            return this;
        }

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
