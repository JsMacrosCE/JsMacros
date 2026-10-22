package com.jsmacrosce.jsmacros.client.api.helper.screen;

import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.wagyourgui.elements.Slider;

import java.util.concurrent.atomic.AtomicReference;

/**
 * a slider: a bar with a handle on it that the player drags, which is JsMacros' own
 * widget rather than one of the game's.
 * <p>
 * The value on a slider is a fraction from nothing to all the way along, not a step
 * number, and the number of steps decides how finely it can be set. That is the one
 * thing worth knowing before reaching for it, because the builder's own starting value
 * is the other of the two: {@code initially} takes a step number and the value reader
 * gives a fraction.
 * <p>
 * Setting the value runs the callback when it actually changes, exactly as dragging the
 * handle does, and does not when the new value rounds to what the slider already holds.
 * A script can therefore set the same value twice and only see the callback once.
 * example:
 * <pre>
 * const screen = Hud.createScreen("a screen", true);
 * const slider = screen.sliderBuilder()
 *    .pos(10, 20)
 *    .size(100, 20)
 *    .message("volume")
 *    .steps(10)
 *    .initially(5)
 *    .action(JavaWrapper.methodToJava(function (s) {
 *      Chat.log(`now ${s.getValue()}`);
 *    }))
 *    .build();
 * Hud.openScreen(screen);
 *
 * // a fraction from 0 to 1, not a step number
 * Chat.log(`value ${slider.getValue()} of ${slider.getSteps()} steps`);
 * // setting it to what it already is runs nothing
 * slider.setValue(slider.getValue());
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Screen and UI Elements")
public class SliderWidgetHelper extends ClickableWidgetHelper<SliderWidgetHelper, Slider> {

    public SliderWidgetHelper(Slider btn) {
        super(btn);
    }

    public SliderWidgetHelper(Slider btn, int zIndex) {
        super(btn, zIndex);
    }

    /**
     * where the handle is, as a fraction from nothing at all up to all the way along,
     * rather than as a step number. A slider with ten steps counts fractions in ninths
     * rather than tenths, so the top of the slider reads one rather than ten and
     * multiplying by {@link #getSteps()} does not give back a step number either; it has
     * to be one less than the step count.
     * <p>
     * The value is snapped to the nearest position, so it is always exactly one of the
     * positions the step count describes rather than anything in between.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const slider = screen.sliderBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("volume")
     *    .steps(10)
     *    .build();
     * Hud.openScreen(screen);
     * // a fraction, and the step it lands on out of the ten
     * const step = Math.round(slider.getValue() * (slider.getSteps() - 1));
     * Chat.log(`${slider.getValue()} along, step ${step} of ${slider.getSteps() - 1}`);
     * </pre>
     *
     * @return the current value of this slider.
     * @since 1.8.4
     */
    public double getValue() {
        return this.base.getValue();
    }

    /**
     * moves the handle, taking the same fraction {@link #getValue()} gives rather than a
     * step number. A number outside the range is brought to the nearest end of it, and
     * the new value is snapped to the nearest step, so setting a value that rounds to
     * what the slider already holds leaves it where it is.
     * <p>
     * The callback given to the builder runs when the value actually moves and not when
     * it does not, so a script setting the value it already reads gets no callback at
     * all. That is the same rule the handle dragging follows.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const slider = screen.sliderBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("volume")
     *    .steps(10)
     *    .action(JavaWrapper.methodToJava(function (s) {
     *       Chat.log(`moved to ${s.getValue()}`);
     *     }))
     *    .build();
     * Hud.openScreen(screen);
     * // all the way along, which is 1 rather than 9
     * slider.setValue(1);
     * // this one rounds to the same place, so the callback does not run
     * slider.setValue(0.99);
     * </pre>
     *
     * @param value the new value
     * @return self for chaining.
     * @since 1.8.4
     */
    public SliderWidgetHelper setValue(double value) {
        base.setValue(value);
        return this;
    }

    /**
     * how many positions the handle can sit at, which is the number of steps rather than
     * the number of gaps between them. A slider with ten steps has ten positions and
     * nine gaps, so the fractions the value snaps to are ninths, and a value of one is
     * the tenth position rather than an eleventh.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const slider = screen.sliderBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("volume")
     *    .steps(10)
     *    .build();
     * Hud.openScreen(screen);
     * // ten positions, so the value is a multiple of a ninth
     * Chat.log(`${slider.getSteps()} steps, value ${slider.getValue()}`);
     * </pre>
     *
     * @return the set amount of steps of this slider.
     * @since 1.8.4
     */
    public int getSteps() {
        return base.getSteps();
    }

    /**
     * changes how many positions the handle can sit at, without moving the handle. This
     * is not a rebuild: the value is left exactly where it was rather than being snapped
     * to the new set of positions, so a slider can be holding a position the new step
     * count has no place for until it is moved.
     * <p>
     * Nothing here checks the number: a step count of one or less is passed straight
     * through rather than being brought up to two. The floor to two only happens when
     * the widget is built, so a count under two set on a built slider leaves it
     * dividing by nothing at all.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const slider = screen.sliderBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("volume")
     *    .steps(10)
     *    .initially(9)
     *    .build();
     * Hud.openScreen(screen);
     * // finer control, without the handle moving
     * slider.setSteps(20);
     * Chat.log(`${slider.getSteps()} steps, still at ${slider.getValue()}`);
     * </pre>
     *
     * @param steps the amount of steps
     * @return self for chaining.
     * @since 1.8.4
     */
    public SliderWidgetHelper setSteps(int steps) {
        base.setSteps(steps);
        return this;
    }

    @Override
    public String toString() {
        return String.format("SliderWidgetHelper:{\"message\": \"%s\", \"value\": %f, \"steps\": %d}", base.getMessage().getString(), getValue(), getSteps());
    }

    /**
     * the builder for a slider, handed back by {@code IScreen.sliderBuilder()} and
     * already bound to that screen.
     * <p>
     * It starts with two steps and a starting value of nothing. The two settings that
     * matter are the step count and the starting value, and the order they are set in
     * matters: {@code initially} clamps against the step count as it stands, so the
     * steps have to be set first or the value is clamped against the default of two.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const builder = screen.sliderBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("volume")
     *    // the step count has to be set before the starting value
     *    .steps(10)
     *    .initially(5);
     * // both are readable on the builder before anything is built
     * Chat.log(`${builder.getSteps()} steps, starting at ${builder.getValue()}`);
     * const slider = builder.build();
     * Hud.openScreen(screen);
     * // the starting value is a step number and the widget's is a fraction
     * Chat.log(`built at ${slider.getValue()}`);
     * </pre>
     *
     * @author Etheradon
     * @since 1.8.4
     */
    @DocletCategory("Screen and UI Elements")
    public static class SliderBuilder extends AbstractWidgetBuilder<SliderBuilder, Slider, SliderWidgetHelper> {

        private int steps = 2;
        private int value = 0;
        @Nullable
        private MethodWrapper<SliderWidgetHelper, IScreen, Object, ?> action;

        public SliderBuilder(IScreen screen) {
            super(screen);
        }

        /**
         * the step count set so far, which is the number of positions rather than the
         * number of gaps between them. It starts at two, which is the smallest count the
         * slider will work with.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * const builder = screen.sliderBuilder().pos(10, 20).size(100, 20);
         * Chat.log(`starts at ${builder.getSteps()} steps`);
         * builder.steps(20);
         * Chat.log(`now ${builder.getSteps()}`);
         * </pre>
         *
         * @return the amount of steps of this slider.
         * @since 1.8.4
         */
        public int getSteps() {
            return steps;
        }

        /**
         * sets how many positions the handle can sit at, clamping anything under two up
         * to two rather than refusing it, and leaving anything larger alone. The slider
         * will not work with fewer than two, so a smaller number is a mistake rather
         * than something to honour.
         * <p>
         * This only affects where the handle can go. The value already on the builder is
         * not re-clamped against the new count, so the two can disagree until
         * {@code initially} is called again.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * screen.sliderBuilder()
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .message("volume")
         *    // a count under two is brought up to two rather than refused
         *    .steps(1)
         *    .initially(1)
         *    .build();
         * Hud.openScreen(screen);
         * </pre>
         *
         * @param steps the amount of steps for the slider. Must be greater or equal to 2
         * @return self for chaining.
         * @since 1.8.4
         */
        public SliderBuilder steps(int steps) {
            this.steps = Mth.clamp(steps, 2, Integer.MAX_VALUE);
            return this;
        }

        /**
         * the starting value set so far, as a step number rather than as the fraction
         * the widget will hold. It starts at nothing, which is the leftmost position
         * whatever the step count is.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * const builder = screen.sliderBuilder().pos(10, 20).size(100, 20).steps(10);
         * Chat.log(`starts at step ${builder.getValue()}`);
         * builder.initially(5);
         * Chat.log(`now step ${builder.getValue()}`);
         * </pre>
         *
         * @return the initial value of the slider.
         * @since 1.8.4
         */
        public int getValue() {
            return value;
        }

        /**
         * sets which position the handle starts on, as a step number counted from
         * nothing. The number is clamped to nothing through one less than the step
         * count, so a value past the end of the slider is brought back to the end rather
         * than refused.
         * <p>
         * The clamp reads the step count as it stands at the moment of the call, so a
         * chain that sets the value before the steps is clamping against the default of
         * two and a value of five becomes one. Setting the steps first is what makes
         * this land where it looks like it should.
         * <p>
         * The built widget holds a fraction rather than a step number: the value given
         * is divided by one less than the step count, so five out of ten steps is five
         * ninths of the way along rather than halfway.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * const builder = screen.sliderBuilder()
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .message("volume")
         *    .steps(10)
         *    .initially(5);
         * const slider = builder.build();
         * Hud.openScreen(screen);
         * // step five of ten is five ninths along, not halfway
         * Chat.log(`built at ${slider.getValue()}`);
         * </pre>
         *
         * @param value the initial value of the slider. Must be between 0 and steps - 1
         * @return self for chaining.
         * @since 1.8.4
         */
        public SliderBuilder initially(int value) {
            this.value = Mth.clamp(value, 0, steps - 1);
            return this;
        }

        /**
         * the callback to run when the handle moves. It is given the slider and the
         * screen it was made on, and it runs on a real drag and on a scripted
         * {@link SliderWidgetHelper#setValue(double)} alike, but only when the value
         * actually changes.
         * <p>
         * It is {@code null} until something is set, which is why a slider built
         * without one is silent rather than doing nothing unusual.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * const builder = screen.sliderBuilder().pos(10, 20).size(100, 20);
         * Chat.log(`starts with a listener: ${builder.getAction() !== null}`);
         * builder.action(JavaWrapper.methodToJava(function (s) {
         *   Chat.log(`moved to ${s.getValue()}`);
         * }));
         * Chat.log(`now: ${builder.getAction() !== null}`);
         * </pre>
         *
         * @return the change listener of the slider.
         * @since 1.8.4
         */
        @Nullable
        public MethodWrapper<SliderWidgetHelper, IScreen, Object, ?> getAction() {
            return action;
        }

        /**
         * sets the callback to run when the handle moves. It is given the slider and the
         * screen the builder was made for. It runs on a real drag and on a scripted
         * change alike, and only when the value actually changes.
         * <p>
         * Passing {@code null} leaves whatever was set rather than clearing it, so a
         * chain that sets a listener and then passes {@code null} keeps the first one.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * screen.sliderBuilder()
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .message("volume")
         *    .steps(10)
         *    .action(JavaWrapper.methodToJava(function (s) {
         *       Chat.log(`now ${s.getValue()}`);
         *     }))
         *    .build();
         * Hud.openScreen(screen);
         * </pre>
         *
         * @param action the change listener for the slider
         * @return self for chaining.
         * @since 1.8.4
         */
        public SliderBuilder action(@Nullable MethodWrapper<SliderWidgetHelper, IScreen, Object, ?> action) {
            this.action = action;
            return this;
        }

        @Override
        public SliderWidgetHelper createWidget() {
            AtomicReference<SliderWidgetHelper> b = new AtomicReference<>(null);
            Slider slider = new Slider(getX(), getY(), getWidth(), getHeight(), getMessage().getRaw(), Mth.clamp((double) value / (steps - 1), 0D, 1D), (btn) -> {
                try {
                    if (action != null) {
                        action.accept(b.get(), screen);
                    }
                } catch (Exception e) {
                    JsMacrosClient.clientCore.profile.logError(e);
                }
            }, steps);
            b.set(new SliderWidgetHelper(slider, getZIndex()));
            return b.get();
        }

    }

}
