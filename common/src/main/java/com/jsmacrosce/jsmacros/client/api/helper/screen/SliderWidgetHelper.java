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
 * A JsMacros slider widget with a draggable handle.
 * <p>
 * The widget value is normalized from {@code 0.0} to {@code 1.0}; builder
 * {@link SliderBuilder#initially(int)} instead accepts a zero-based position.
 * <p>
 * The callback runs only when an input changes the value.
 * <pre>
 * const screen = Hud.createScreen("a screen", true);
 * screen.setOnInit(JavaWrapper.methodToJava(function (self) {
 *   const slider = self.sliderBuilder()
 *      .pos(10, 20)
 *      .size(100, 20)
 *      .message("volume")
 *      .steps(10)
 *      .initially(5)
 *      .action(JavaWrapper.methodToJava(function (s) {
 *        Chat.log(`now ${s.getValue()}`);
 *      }))
 *      .build();
 *
 *   slider.setValue(1); // move to the maximum; callback runs if this changes the value
 * }));
 * Hud.openScreen(screen);
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Screen and UI Elements")
public class SliderWidgetHelper extends ClickableWidgetHelper<SliderWidgetHelper, Slider> {

    /**
     * Wraps the supplied slider, using z-index zero for this helper.
     *
     * @param btn the slider to wrap
     */
    public SliderWidgetHelper(Slider btn) {
        super(btn);
    }

    /**
     * Wraps the supplied slider and sets this helper's drawing-order index.
     *
     * @param btn the slider to wrap
     * @param zIndex the drawing-order index for this helper
     */
    public SliderWidgetHelper(Slider btn, int zIndex) {
        super(btn, zIndex);
    }

    /**
     * Returns the normalized value from {@code 0.0} to {@code 1.0}, rounded to one of
     * the configured positions. For {@code n} positions, the increments are
     * {@code 1.0 / (n - 1)}.
     *
     * @return the current value of this slider.
     * @since 1.8.4
     */
    public double getValue() {
        return this.base.getValue();
    }

    /**
     * Sets the normalized value, clamping it to {@code [0.0, 1.0]} and rounding to a
     * configured position. The change callback runs only if the resulting value differs.
     *
     * @param value requested normalized value
     * @return self for chaining.
     * @since 1.8.4
     */
    public SliderWidgetHelper setValue(double value) {
        base.setValue(value);
        return this;
    }

    /**
     * Returns the configured number of discrete positions (including both endpoints).
     *
     * @return the set amount of steps of this slider.
     * @since 1.8.4
     */
    public int getSteps() {
        return base.getSteps();
    }

    /**
     * Sets the number of discrete positions without changing or re-rounding the
     * current value. Unlike the builder, this method does not clamp the count; use at
     * least two positions for normal slider operation.
     *
     * @param steps number of discrete positions
     * @return self for chaining.
     * @since 1.8.4
     */
    public SliderWidgetHelper setSteps(int steps) {
        base.setSteps(steps);
        return this;
    }

    /**
     * Returns a formatted diagnostic containing this slider's message, value, and step count.
     * The message is inserted without JSON escaping, and numeric formatting follows the default locale.
     *
     * @return the formatted diagnostic string
     */
    @Override
    public String toString() {
        return String.format("SliderWidgetHelper:{\"message\": \"%s\", \"value\": %f, \"steps\": %d}", base.getMessage().getString(), getValue(), getSteps());
    }

    /**
     * Builds sliders for the screen returned by {@link IScreen#sliderBuilder()}.
     * Defaults to two positions and position zero. Set {@link #steps(int)} before
     * {@link #initially(int)}, since the latter clamps against the current count.
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

        /**
         * Associates this builder with the screen that will receive widgets built from it.
         *
         * @param screen the screen to associate with this builder
         */
        public SliderBuilder(IScreen screen) {
            super(screen);
        }

        /**
         * Returns the configured number of discrete positions; defaults to two.
         *
         * @return the amount of steps of this slider.
         * @since 1.8.4
         */
        public int getSteps() {
            return steps;
        }

        /**
         * Sets the number of discrete positions, clamping values below two to two.
         * Does not re-clamp a previously configured starting position.
         *
         * @param steps requested number of positions; values below two become two
         * @return self for chaining.
         * @since 1.8.4
         */
        public SliderBuilder steps(int steps) {
            this.steps = Mth.clamp(steps, 2, Integer.MAX_VALUE);
            return this;
        }

        /**
         * Returns the builder's configured starting position as a zero-based step
         * number; it defaults to zero.
         *
         * @return the initial value of the slider.
         * @since 1.8.4
         */
        public int getValue() {
            return value;
        }

        /**
         * Sets the starting position as a zero-based step number, clamped to the range
         * from zero through {@code steps - 1}.
         * <p>
         * The clamp reads the step count as it stands at the moment of the call, so a
         * chain that sets the value before the steps is clamping against the default of
         * two and a value of five becomes one. Setting the steps first is what makes
         * this land where it looks like it should.
         * <p>
         * The widget value is a normalized fraction, so the step number is divided by
         * {@code steps - 1} (for example, step five of ten corresponds to {@code 5/9}).
         * <p>
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * screen.setOnInit(JavaWrapper.methodToJava(function (self) {
         *   const slider = self.sliderBuilder()
         *      .pos(10, 20)
         *      .size(100, 20)
         *      .message("volume")
         *      .steps(10)
         *      .initially(5)
         *      .build();
         *   // step five of ten is five ninths along, not halfway
         *   Chat.log(`built at ${slider.getValue()}`);
         * }));
         * Hud.openScreen(screen);
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
         * Returns the configured change callback, or {@code null} if none is set.
         *
         * @return the change listener of the slider.
         * @since 1.8.4
         */
        @Nullable
        public MethodWrapper<SliderWidgetHelper, IScreen, Object, ?> getAction() {
            return action;
        }

        /**
         * Sets the callback invoked with the built slider and its screen when the
         * slider value changes, whether by dragging or by {@link SliderWidgetHelper#setValue(double)}.
         * Passing {@code null} clears the callback.
         *
         * @param action the change listener for the slider
         * @return self for chaining.
         * @since 1.8.4
         */
        public SliderBuilder action(@Nullable MethodWrapper<SliderWidgetHelper, IScreen, Object, ?> action) {
            this.action = action;
            return this;
        }

        /**
         * Constructs a slider from the builder's configured geometry, message, step count,
         * and initial step converted to a normalized fraction. The configured action receives
         * this helper and its associated screen; exceptions from the action are logged.
         * The returned helper uses the configured z-index; {@code build()} applies the other
         * builder properties and adds the widget to the associated screen.
         *
         * @return the helper wrapping the constructed slider
         */
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
