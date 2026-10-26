package com.jsmacrosce.jsmacros.util;

import com.jsmacrosce.doclet.DocletIgnore;

/**
 * Internal packed-ARGB helpers shared by the script-facing {@code Color} library and the render
 * components. Scripts should use {@code Color} instead.
 * <p>
 * Java stores a packed color in a signed {@code int}. A value with the high bit set, such as
 * {@code 0x80000000}, is therefore negative when read as a Java number even though its bits are a
 * valid ARGB color. These helpers keep that bit-level behavior in one place.
 * <p>
 * {@link #fixAlpha(int)} preserves the render API's existing interpretation of a zero alpha byte
 * with nonzero RGB as "alpha omitted". It is an input-compatibility rule, not a general color
 * operation, so it is not exposed through the script-facing library.
 */
@DocletIgnore
public final class ColorMath {
    private ColorMath() {
    }

    /**
     * Packs alpha, red, green, and blue components into a signed 32-bit ARGB value.
     *
     * @param alpha alpha component, 0-255
     * @param red red component, 0-255
     * @param green green component, 0-255
     * @param blue blue component, 0-255
     * @return the packed color
     * @throws IllegalArgumentException if a component is outside 0-255
     */
    public static int argb(int alpha, int red, int green, int blue) {
        requireByte("alpha", alpha);
        requireByte("red", red);
        requireByte("green", green);
        requireByte("blue", blue);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * Packs an opaque color. This is equivalent to {@code argb(0xFF, red, green, blue)}.
     *
     * @param red red component, 0-255
     * @param green green component, 0-255
     * @param blue blue component, 0-255
     * @return the packed opaque color
     * @throws IllegalArgumentException if a component is outside 0-255
     */
    public static int rgb(int red, int green, int blue) {
        return argb(0xFF, red, green, blue);
    }

    /**
     * Converts a 32-bit ARGB bit pattern from either its unsigned or signed numeric form.
     * Both {@code 0x80000000} and {@code -2147483648} produce the same color.
     *
     * @param packed packed ARGB value in the range {@code Integer.MIN_VALUE} through
     *               {@code 0xFFFFFFFF}
     * @return the corresponding signed 32-bit color
     * @throws IllegalArgumentException if the value is outside the 32-bit ARGB range
     */
    public static int fromPacked(long packed) {
        if (packed < Integer.MIN_VALUE || packed > 0xFFFFFFFFL) {
            throw new IllegalArgumentException(
                    "Packed ARGB must be between -2147483648 and 4294967295: " + packed);
        }
        return (int) packed;
    }

    /**
     * Returns the alpha component of a packed color.
     *
     * @param color packed color
     * @return alpha component, 0-255
     */
    public static int alpha(int color) {
        return (color >>> 24) & 0xFF;
    }

    /**
     * Returns the red component of a packed color.
     *
     * @param color packed color
     * @return red component, 0-255
     */
    public static int red(int color) {
        return (color >>> 16) & 0xFF;
    }

    /**
     * Returns the green component of a packed color.
     *
     * @param color packed color
     * @return green component, 0-255
     */
    public static int green(int color) {
        return (color >>> 8) & 0xFF;
    }

    /**
     * Returns the blue component of a packed color.
     *
     * @param color packed color
     * @return blue component, 0-255
     */
    public static int blue(int color) {
        return color & 0xFF;
    }

    /**
     * Replaces the alpha component of a packed color while preserving its RGB value.
     *
     * @param color packed color whose RGB is kept
     * @param alpha new alpha component, 0-255
     * @return the repacked color
     * @throws IllegalArgumentException if {@code alpha} is outside 0-255
     */
    public static int withAlpha(int color, int alpha) {
        requireByte("alpha", alpha);
        return (color & 0xFFFFFF) | (alpha << 24);
    }

    /**
     * Adds fully opaque alpha when the value has nonzero RGB and a zero alpha byte. This keeps the
     * render API's historical treatment of a bare {@code 0xRRGGBB} value as opaque while leaving
     * transparent black ({@code 0}) transparent.
     *
     * @param color packed ARGB color; a zero alpha byte is treated as omitted only when RGB is nonzero
     * @return the input unchanged, or with its alpha byte set to {@code 0xFF}
     */
    public static int fixAlpha(int color) {
        int alpha = color & 0xFF000000;
        int rgb = color & 0xFFFFFF;
        return alpha == 0 && rgb > 0 ? color | 0xFF000000 : color;
    }

    private static void requireByte(String name, int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(name + " must be between 0 and 255: " + value);
        }
    }
}
