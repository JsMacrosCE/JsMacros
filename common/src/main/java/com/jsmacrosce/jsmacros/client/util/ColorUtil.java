package com.jsmacrosce.jsmacros.client.util;

/** Utilities for packed ARGB color values.
 * <pre>
 * const ColorUtil = Java.type("com.jsmacrosce.jsmacros.client.util.ColorUtil");
 * const opaque = ColorUtil.fixAlpha(0x336699);
 * </pre>
 */
public class ColorUtil {
    /**
     * Adds fully opaque alpha when the value has nonzero RGB and a zero alpha byte.
     *
     * @param color packed ARGB color; a zero alpha byte is treated as omitted only when RGB is nonzero
     * @return the input unchanged, or with its alpha byte set to {@code 0xFF}
     */
    public static int fixAlpha(int color) {
        int alpha = color & 0xFF000000;
        int rgb = color & 0xFFFFFF;
        return alpha == 0 && rgb > 0 ? color | 0xFF000000 : color;
    }
}
