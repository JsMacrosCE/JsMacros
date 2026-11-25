package com.jsmacrosce.jsmacros.graal.language.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the Graal context recognizes its runtime's cancellation shapes. Closing a context
 * during execution uses Truffle's package-private {@code CancelExecution}, so only its message is
 * available to identify it; the same cancellation can also arrive as a polyglot exception.
 */
class GraalScriptContextCancellationTest {
    private final GraalScriptContext context = new GraalScriptContext(null, null, null);

    @Test
    void recognizesTruffleExecutionCancellation() {
        assertTrue(context.isLanguageCancellation(new ExecutionCancelled()));
    }

    @Test
    void ignoresUnrelatedThreadDeath() {
        assertFalse(context.isLanguageCancellation(new ThreadDeath()));
    }

    /** Stands in for Truffle's package-private {@code PolyglotEngineImpl.CancelExecution}. */
    private static final class ExecutionCancelled extends ThreadDeath {
        @Override
        public String getMessage() {
            return "Context execution was cancelled.";
        }
    }
}
