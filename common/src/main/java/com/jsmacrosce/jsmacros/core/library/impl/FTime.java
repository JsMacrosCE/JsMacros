package com.jsmacrosce.jsmacros.core.library.impl;

import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;
import com.jsmacrosce.jsmacros.core.library.Library;
import com.jsmacrosce.jsmacros.core.library.PerExecLibrary;

/**
 * Provides wall-clock time utilities to scripts as {@code Time}.
 * <p>
 * {@link #time()} returns system time in milliseconds since the Unix epoch; {@link #sleep(long)}
 * waits in milliseconds rather than game ticks. In the Graal context, sleeping yields execution
 * to other script threads while the calling script waits.
 * <pre>
 * const started = Time.time();
 * Time.sleep(1000);
 * print(`that took about ${Time.time() - started} ms`);
 * </pre>
 * @author Wagyourtail
 */
@Library("Time")
@SuppressWarnings("unused")
public class FTime extends PerExecLibrary {

    /**
     * Initializes this time library with its script context.
     * @param context script context associated with this library
     */
    public FTime(BaseScriptContext<?> context) {
        super(context);
    }

    /**
     * @return milliseconds since the Unix epoch, from the system wall clock
     */
    public long time() {
        return System.currentTimeMillis();
    }

    /**
     * Waits for the specified wall-clock duration; this is not a tick-based delay. In the Graal
     * context, the language engine is yielded to other script threads while waiting.
     * Closing the script context can interrupt a sleeping bound thread.
     *
     * @param millis how long to sleep for, in milliseconds
     * @throws InterruptedException if interrupted while waiting
     */
    public void sleep(long millis) throws InterruptedException {
        ctx.wrapSleep(() -> Thread.sleep(millis));
    }

}
