package com.jsmacrosce.jsmacros.graal.language.impl;

import com.jsmacrosce.jsmacros.core.library.Library;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
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

    public static final class IntParameter {
        public void draw(int value) {
        }
    }

    public static final class VarargsOverloads {
        public void draw(String message, Object... args) {
        }

        public void entities(double distance, String... types) {
        }

        public void onlyVarargs(String... types) {
        }
    }

    public static final class ConstructorOverloads {
        public ConstructorOverloads(int x, String text) {
        }
    }

    @Library("World")
    public static final class WorldLibrary {
        public void draw(int distance) {
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
    void doesNotFlagIntegralDoublesThatFitTheTargetType() {
        String message = GuestExceptionSimplifier.formatOverloadError(
                IntParameter.class,
                "draw",
                new Object[]{10.0D}
        );

        assertFalse(message.contains("Mismatched arguments:"));
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

    @Test
    void reportsVarargsMinimumInsteadOfTheArrayParameterCount() {
        String plain = stripFormatting(GuestExceptionSimplifier.formatMethodArityError(
                VarargsOverloads.class,
                "draw",
                0
        ));

        assertTrue(plain.contains("(String message, Object... args) - at least 1 argument"));
        assertTrue(plain.contains("Given: 0 arguments."));
    }

    @Test
    void reportsVarargsMinimumForAFixedLeadingParameter() {
        String plain = stripFormatting(GuestExceptionSimplifier.formatMethodArityError(
                VarargsOverloads.class,
                "entities",
                0
        ));

        assertTrue(plain.contains("(double distance, String... types) - at least 1 argument"));
        assertTrue(plain.contains("Given: 0 arguments."));
    }

    @Test
    void reportsAllFixedArityCandidates() {
        String plain = stripFormatting(GuestExceptionSimplifier.formatMethodArityError(
                Overloads.class,
                "draw",
                4
        ));

        assertTrue(plain.contains("(int x1, int y1, int x2, int y2, int color) - 5 arguments"));
        assertTrue(plain.contains("(int x1, int y1, int x2, int y2, int color, int alpha) - 6 arguments"));
        assertTrue(plain.contains("Given: 4 arguments."));
    }

    @Test
    void reportsBadTypesInTheVarargsTail() {
        String plain = stripFormatting(GuestExceptionSimplifier.formatOverloadError(
                VarargsOverloads.class,
                "entities",
                new Object[]{10, 2}
        ));

        assertTrue(plain.contains("(double distance, String... types)"));
        assertTrue(plain.contains("types: expected String, got Integer = 2"));
    }

    @Test
    void reportsConstructorOverloadsWithNewSyntax() {
        String plain = stripFormatting(GuestExceptionSimplifier.formatConstructorOverloadError(
                ConstructorOverloads.class,
                new Object[]{"x", 2}
        ));

        assertTrue(plain.startsWith("OverloadError on constructor ConstructorOverloads"), plain);
        assertTrue(plain.contains("new ConstructorOverloads(int x, String text)"), plain);
        assertTrue(plain.contains("x: expected int, got String"), plain);
        assertTrue(plain.contains("text: expected String, got Integer = 2"), plain);
    }

    @Test
    void reportsConstructorArityWithNewSyntax() {
        String plain = stripFormatting(GuestExceptionSimplifier.formatConstructorArityError(
                ConstructorOverloads.class,
                0
        ));

        assertTrue(plain.startsWith("ArityError on constructor ConstructorOverloads"), plain);
        assertTrue(plain.contains("new ConstructorOverloads(int x, String text) - 2 arguments"), plain);
        assertTrue(plain.contains("Given: 0 arguments."), plain);
    }

    @Test
    void usesTheScriptFacingLibraryName() {
        String plain = stripFormatting(GuestExceptionSimplifier.formatOverloadError(
                WorldLibrary.class,
                "draw",
                new Object[]{"x"}
        ));

        assertTrue(plain.startsWith("OverloadError on method World.draw"));
        assertTrue(plain.contains("distance: expected int, got String"));
    }

    @Test
    void recognizesConstructorArityErrorsFromGraalMessages() throws Exception {
        ArityException cause = ArityException.create(2, 2, 0);
        String message = GuestExceptionSimplifier.simplifyArityError(
                "TypeError: instantiate on " + ConstructorOverloads.class.getName()
                        + " failed due to: " + cause.getMessage(),
                cause
        );

        String plain = stripFormatting(message);
        assertTrue(plain.startsWith("ArityError on constructor ConstructorOverloads"));
        assertTrue(plain.contains("new ConstructorOverloads(int x, String text) - 2 arguments"));
    }

    @Test
    void recognizesConstructorTypeErrorsFromGraalMessages() throws Exception {
        UnsupportedTypeException cause = UnsupportedTypeException.create(new Object[]{"x", 2});
        String message = GuestExceptionSimplifier.simplifyOverloadError(
                "TypeError: instantiate on " + ConstructorOverloads.class.getName()
                        + " failed due to: " + cause.getMessage(),
                cause
        );

        String plain = stripFormatting(message);
        assertTrue(plain.startsWith("OverloadError on constructor ConstructorOverloads"));
        assertTrue(plain.contains("x: expected int, got String"));
    }

    @Test
    void recognizesVarargsTypeErrorsFromGraalMessages() throws Exception {
        UnsupportedTypeException cause = UnsupportedTypeException.create(new Object[]{10, 2});
        String message = GuestExceptionSimplifier.simplifyOverloadError(
                "TypeError: invokeMember (entities) on " + VarargsOverloads.class.getName()
                        + " failed due to: " + cause.getMessage(),
                cause
        );

        String plain = stripFormatting(message);
        assertTrue(plain.startsWith("OverloadError on method VarargsOverloads.entities"));
        assertTrue(plain.contains("types: expected String, got Integer = 2"));
    }

    private static String stripFormatting(String message) {
        return message.replaceAll("\u00A7.", "");
    }
}
