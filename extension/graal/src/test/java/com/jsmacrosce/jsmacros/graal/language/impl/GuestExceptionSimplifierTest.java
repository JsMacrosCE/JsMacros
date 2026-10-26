package com.jsmacrosce.jsmacros.graal.language.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuestExceptionSimplifierTest {

    public static final class Overloads {
        public void draw(int x1, int y1, int x2, int y2, int color) {
        }

        public void draw(int x1, int y1, int x2, int y2, int color, int alpha) {
        }
    }

    public static final class CompatibleWidening {
        public void draw(double value) {
        }
    }

    public static final class StringParameter {
        public void draw(String text) {
        }
    }

    @Test
    void includesParameterNamesAndHighlightsTheMismatchedArgument() {
        String message = GuestExceptionSimplifier.formatOverloadError(
                Overloads.class,
                "draw",
                new Object[]{10, 10, 210, 60, 2_147_483_648D}
        );
        String plain = stripFormatting(message);

        assertTrue(plain.contains("(int x1, int y1, int x2, int y2, int color)"));
        assertTrue(plain.contains("(int x1, int y1, int x2, int y2, int color, int alpha)"));
        assertTrue(plain.contains(
                "(x1: Integer = 10, y1: Integer = 10, x2: Integer = 210, y2: Integer = 60, "
                        + "color: Double = 2.147483648E9)"
        ));
        assertTrue(plain.contains("Mismatched arguments:"));
        assertTrue(plain.contains("color: expected int, got Double = 2.147483648E9"));
        assertTrue(message.contains("\u00A7ecolor: Double = 2.147483648E9\u00A7r"));
    }

    @Test
    void fallsBackToPositionalNamesWhenNoOverloadMatchesTheArgumentCount() {
        String message = GuestExceptionSimplifier.formatOverloadError(
                Overloads.class,
                "draw",
                new Object[]{10}
        );

        assertTrue(stripFormatting(message).contains("(arg0: Integer = 10)"));
        assertFalse(message.contains("Mismatched arguments:"));
    }

    @Test
    void doesNotFlagConversionsThatMayStillBeValid() {
        String message = GuestExceptionSimplifier.formatOverloadError(
                CompatibleWidening.class,
                "draw",
                new Object[]{10}
        );

        assertFalse(message.contains("Mismatched arguments:"));
        assertFalse(message.contains("\u00A7e"));
    }

    @Test
    void reportsMismatchesForNonNumericExpectedTypes() {
        String message = GuestExceptionSimplifier.formatOverloadError(
                StringParameter.class,
                "draw",
                new Object[]{10}
        );

        assertTrue(stripFormatting(message).contains("text: expected String, got Integer = 10"));
    }

    private static String stripFormatting(String message) {
        return message.replaceAll("\u00A7.", "");
    }
}
