package com.jsmacrosce.jsmacros.core.library.impl;

import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;
import com.jsmacrosce.jsmacros.core.library.Library;
import com.jsmacrosce.jsmacros.core.library.PerExecLibrary;

/**
 * Functions for getting and using raw java classes, methods and functions.
 * <p>
 * An instance of this class is passed to scripts as the {@code Time} variable.
 * <br>
 * This is the wall clock rather than anything the game measures, and it is the right thing for
 * timing how long something took or spacing work out. It is not the game's tick clock: nothing
 * here advances with ticks, is affected by the game being paused, or reports anything about the
 * world. For a delay measured in ticks, use a listener on the tick event instead.
 * <br>
 * There are only two functions, and the sleep is the one with the subtlety to it: it does more
 * than pause the script, it lets the other script threads run while it waits.
 * example:
 * <pre>
 * // the wall clock, in milliseconds since the epoch
 * const started = Time.time();
 *
 * // sleeping lets the other script threads run rather than freezing everything,
 * // so a second of it costs about a second and not a stall
 * Time.sleep(1000);
 *
 * print(`that took about ${Time.time() - started} ms`);
 *
 * // a listener is the shape to use for anything measured in ticks, since this
 * // clock is not the game's
 * const listener = JsMacros.on("Tick", JavaWrapper.methodToJava(function () {
 *   Chat.log("a tick went by");
 * }));
 * // later
 * JsMacros.off(listener);
 * </pre>
 * @author Wagyourtail
 */
@Library("Time")
@SuppressWarnings("unused")
public class FTime extends PerExecLibrary {

    public FTime(BaseScriptContext<?> context) {
        super(context);
    }

    /**
     * @return current time in MS.
     */
    public long time() {
        return System.currentTimeMillis();
    }

    /**
     * Sleeps the current thread for the specified time in MS.
     * <br>
     * The sleep goes through the script context rather than straight to the thread, and that is
     * what makes it worth using over a bare sleep. A language that allows only one thread into the
     * engine at a time, which is JavaScript here, hands the engine over to the next script thread
     * for the duration and takes it back afterwards, and this thread goes to the back of the
     * script priority queue while it waits rather than keeping its place at the front. So the
     * scripts that were waiting run during the sleep, and this one resumes afterwards.<br>
     * It still blocks the script it is called from, and it is still the wall clock rather than the
     * game's tick clock, so a sleep is not a way to wait a number of ticks.
     * example:
     * <pre>
     * // while this one waits, the other script threads get to run
     * const started = Time.time();
     * Time.sleep(2000);
     * print(`slept for about ${Time.time() - started} ms`);
     *
     * // which is what makes this the right way to space a repeated job out
     * let runs = 0;
     * while (runs !== 3) {
     *   print(`run ${runs + 1}`);
     *   runs += 1;
     *   if (runs !== 3) {
     *     Time.sleep(1000);
     *   }
     * }
     * </pre>
     *
     * @param millis how long to sleep for, in milliseconds
     * @throws InterruptedException if the thread is interrupted while waiting, which also happens
     *         when the script context is closed underneath it
     */
    public void sleep(long millis) throws InterruptedException {
        ctx.wrapSleep(() -> Thread.sleep(millis));
    }

}
