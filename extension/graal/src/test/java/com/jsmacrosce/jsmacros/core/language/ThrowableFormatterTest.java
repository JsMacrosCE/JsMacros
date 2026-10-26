package com.jsmacrosce.jsmacros.core.language;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThrowableFormatterTest {

    @Test
    void formatsTheCauseChainWithSimpleNames() {
        RuntimeException exception = new RuntimeException("outer", new IllegalArgumentException("inner"));

        String message = ThrowableFormatter.format(exception);

        assertTrue(message.startsWith("RuntimeException: outer"));
        assertTrue(message.contains("\n  caused by IllegalArgumentException: inner"));
    }

    @Test
    void omitsTheColonForExceptionsWithoutAMessage() {
        String message = ThrowableFormatter.format(new NullPointerException());

        assertTrue(message.startsWith("NullPointerException"));
        assertFalse(message.contains(": null"));
    }

    @Test
    void includesTheFifthCause() {
        Throwable cause = new IllegalStateException("cause5");
        for (int i = 4; i >= 1; i--) {
            cause = new RuntimeException("cause" + i, cause);
        }

        String message = ThrowableFormatter.format(new RuntimeException("root", cause));

        assertTrue(message.contains("caused by IllegalStateException: cause5"));
    }

    @Test
    void stopsAtACauseCycle() {
        RuntimeException first = new RuntimeException("first");
        RuntimeException second = new RuntimeException("second", first);
        first.initCause(second);

        String message = ThrowableFormatter.format(first);

        assertTrue(message.startsWith("RuntimeException: first"));
        assertTrue(message.contains("caused by RuntimeException: second"));
        assertFalse(message.contains("caused by RuntimeException: first"));
    }
}
