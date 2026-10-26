package com.jsmacrosce;

import com.jsmacrosce.jsmacros.util.ColorMath;

/** Checks packed-ARGB packing, unpacking, range validation and the render alpha policy. */
public final class ColorMathTest {
    private static int checks;

    public static void main(String[] args) {
        componentsAndGetters();
        packedConversion();
        alphaReplacement();
        renderAlphaPolicy();
        validation();
        System.out.println("ColorMath: " + checks + " checks passed");
    }

    private static void componentsAndGetters() {
        int halfBlack = ColorMath.argb(0x80, 0, 0, 0);
        check(halfBlack == 0x80000000, "argb packs alpha into the high byte");
        check(ColorMath.alpha(halfBlack) == 0x80, "alpha reads the high byte");
        check(ColorMath.red(halfBlack) == 0, "red of black is zero");
        check(ColorMath.green(halfBlack) == 0, "green of black is zero");
        check(ColorMath.blue(halfBlack) == 0, "blue of black is zero");

        int white = ColorMath.rgb(0xFF, 0xFF, 0xFF);
        check(white == 0xFFFFFFFF, "rgb produces opaque white");
        check(ColorMath.alpha(white) == 0xFF, "rgb sets opaque alpha");
        check(ColorMath.red(white) == 0xFF, "red of white is 255");
        check(ColorMath.green(white) == 0xFF, "green of white is 255");
        check(ColorMath.blue(white) == 0xFF, "blue of white is 255");
    }

    private static void packedConversion() {
        check(ColorMath.fromPacked(0x80000000L) == ColorMath.argb(0x80, 0, 0, 0),
                "fromPacked accepts an unsigned high-bit literal");
        check(ColorMath.fromPacked(-2147483648L) == ColorMath.argb(0x80, 0, 0, 0),
                "fromPacked accepts the signed form of the same bits");
        check(ColorMath.fromPacked(0xFFFFFFFFL) == -1, "fromPacked accepts unsigned white");
        check(ColorMath.fromPacked(-1L) == -1, "fromPacked accepts signed white");
    }

    private static void alphaReplacement() {
        int color = ColorMath.rgb(0x12, 0x34, 0x56);
        int replaced = ColorMath.withAlpha(color, 0x40);
        check(ColorMath.alpha(replaced) == 0x40, "withAlpha replaces alpha");
        check(ColorMath.red(replaced) == 0x12 && ColorMath.green(replaced) == 0x34
                && ColorMath.blue(replaced) == 0x56, "withAlpha preserves RGB");
    }

    private static void renderAlphaPolicy() {
        check(ColorMath.fixAlpha(0x123456) == 0xFF123456,
                "fixAlpha makes bare RGB opaque");
        check(ColorMath.fixAlpha(0x40123456) == 0x40123456,
                "fixAlpha preserves an explicit alpha byte");
        check(ColorMath.fixAlpha(0) == 0,
                "fixAlpha leaves transparent black transparent");
    }

    private static void validation() {
        expectThrows(() -> ColorMath.argb(256, 0, 0, 0), "alpha above 255");
        expectThrows(() -> ColorMath.argb(0, -1, 0, 0), "red below 0");
        expectThrows(() -> ColorMath.argb(0, 0, 0, 256), "blue above 255");
        expectThrows(() -> ColorMath.withAlpha(0, 256), "withAlpha above 255");
        expectThrows(() -> ColorMath.fromPacked(0x100000000L), "packed value above 32 bits");
        expectThrows(() -> ColorMath.fromPacked(-2147483649L), "packed value below 32 bits");
    }

    private static void expectThrows(Runnable action, String message) {
        checks++;
        try {
            action.run();
            throw new AssertionError(message + ": expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
