package com.jsmacrosce.jsmacros.core.library.impl;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.core.library.Library;
import com.jsmacrosce.jsmacros.util.ColorMath;

/**
 * Creates and inspects the packed ARGB colors used by the render APIs.
 * <p>
 * Components are bytes from 0 to 255. {@code Color.argb} and {@code Color.rgb} build a color
 * from its components, while {@code Color.fromPacked} converts a packed 32-bit value such as
 * {@code 0x80000000} without requiring JavaScript's signed-integer conversion.
 * <p>
 * Render elements treat a zero alpha byte with nonzero RGB as "alpha omitted" and make the color
 * opaque. Use alpha {@code 1} for a nearly transparent color; transparent black
 * ({@code argb(0, 0, 0, 0)}) remains fully transparent.
 * <pre>
 * const draw = Hud.createDraw2D();
 * draw.addRect(10, 10, 210, 60, Color.argb(0x80, 0, 0, 0));
 * draw.addText("hello", 18, 22, Color.rgb(0xFF, 0xFF, 0xFF), true);
 * draw.register();
 *
 * // The same colors can be written from an existing packed value:
 * const halfBlack = Color.fromPacked(0x80000000);
 * const white = Color.fromPacked(0xFFFFFFFF);
 * </pre>
 *
 * @since 2.0.0
 */
@Library("Color")
@SuppressWarnings("unused")
public class FColor extends BaseLibrary {

    /**
     * Associates this color library with the core that owns it.
     *
     * @param runner core associated with this library
     */
    public FColor(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * Packs alpha, red, green, and blue components into a signed 32-bit ARGB color.
     *
     * @param alpha alpha component, 0-255
     * @param red red component, 0-255
     * @param green green component, 0-255
     * @param blue blue component, 0-255
     * @return the packed color
     * @throws IllegalArgumentException if a component is outside 0-255
     * @since 2.0.0
     */
    public int argb(int alpha, int red, int green, int blue) {
        return ColorMath.argb(alpha, red, green, blue);
    }

    /**
     * Packs an opaque color from red, green, and blue components.
     *
     * @param red red component, 0-255
     * @param green green component, 0-255
     * @param blue blue component, 0-255
     * @return the packed opaque color
     * @throws IllegalArgumentException if a component is outside 0-255
     * @since 2.0.0
     */
    public int rgb(int red, int green, int blue) {
        return ColorMath.rgb(red, green, blue);
    }

    /**
     * Converts a packed 32-bit ARGB value from either its unsigned hex form or its signed form.
     * {@code fromPacked(0x80000000)} and {@code fromPacked(-2147483648)} return the same color.
     * <p>
     * This is the escape hatch for values above {@code Integer.MAX_VALUE}, which JavaScript reads
     * as positive numbers but the render APIs accept only as signed Java {@code int}s.
     *
     * @param packed packed ARGB value in the range {@code Integer.MIN_VALUE} through
     *               {@code 0xFFFFFFFF}
     * @return the corresponding signed 32-bit color
     * @throws IllegalArgumentException if the value is outside the 32-bit ARGB range
     * @since 2.0.0
     */
    public int fromPacked(long packed) {
        return ColorMath.fromPacked(packed);
    }

    /**
     * Returns the alpha component of a packed color.
     *
     * @param color packed color
     * @return alpha component, 0-255
     * @since 2.0.0
     */
    public int alpha(int color) {
        return ColorMath.alpha(color);
    }

    /**
     * Returns the red component of a packed color.
     *
     * @param color packed color
     * @return red component, 0-255
     * @since 2.0.0
     */
    public int red(int color) {
        return ColorMath.red(color);
    }

    /**
     * Returns the green component of a packed color.
     *
     * @param color packed color
     * @return green component, 0-255
     * @since 2.0.0
     */
    public int green(int color) {
        return ColorMath.green(color);
    }

    /**
     * Returns the blue component of a packed color.
     *
     * @param color packed color
     * @return blue component, 0-255
     * @since 2.0.0
     */
    public int blue(int color) {
        return ColorMath.blue(color);
    }

    /**
     * Replaces the alpha component of a packed color while preserving its RGB value.
     *
     * @param color packed color whose RGB is kept
     * @param alpha new alpha component, 0-255
     * @return the repacked color
     * @throws IllegalArgumentException if {@code alpha} is outside 0-255
     * @since 2.0.0
     */
    public int withAlpha(int color, int alpha) {
        return ColorMath.withAlpha(color, alpha);
    }
}
