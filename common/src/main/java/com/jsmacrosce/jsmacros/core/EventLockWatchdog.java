package com.jsmacrosce.jsmacros.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.core.event.BaseListener;
import com.jsmacrosce.jsmacros.core.event.IEventListener;
import com.jsmacrosce.jsmacros.core.language.EventContainer;

/** Monitors an event-container lock and closes its script context if the lock remains held at the timed check.
 * <pre>
 * const Watchdog = Java.type("com.jsmacrosce.jsmacros.core.EventLockWatchdog");
 * const run = JsMacros.runScript("js", "const result = 1 + 1;");
 * Watchdog.startWatchdog(run, null, 1000);
 * </pre>
 */
public class EventLockWatchdog {

    /** Starts a lock monitor using the container's context and runner.
     * After the delay, the monitor checks whether the lock is held; if so, it disables a {@link BaseListener} listener, closes the context, and logs a {@link WatchdogException}.
     * A release observed at the check avoids those actions; a release after the check may still race with them. Other listener implementations are not disabled.
     * @param lock non-null container with a non-null context and initialized runner thread pool
     * @param listener nullable listener; only a {@link BaseListener} is disabled when the timed check finds the lock held
     * @param maxTime nonnegative delay before the timed check, in milliseconds
     */
    public static void startWatchdog(@NotNull EventContainer<?> lock, @Nullable IEventListener listener, long maxTime) {
        lock.getCtx().runner.threadPool.runTask(() -> {
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(maxTime);
                    synchronized (lock) {
                        if (!lock.isLocked()) {
                            return;
                        }
                    }
                    if (listener instanceof BaseListener baseListener) {
                        baseListener.off();
                    }
                    lock.getCtx().closeContext();
                    lock.getCtx().runner.profile.logError(new WatchdogException(String.format("Script \n\"%s\"\n joined longer than allowed time of %d ms.", listener, maxTime)));
                } catch (InterruptedException ignored) {
                }
            });
            Thread u = new Thread(() -> {
                try {
                    lock.awaitLock(() -> {
                        synchronized (lock) {
                            t.interrupt();
                        }
                    });
                } catch (InterruptedException ignored) {
                }
            });
            t.setPriority(Thread.NORM_PRIORITY - 1);
            u.setPriority(Thread.NORM_PRIORITY - 1);
            t.start();
            u.start();
        });
    }

    /** Diagnostic runtime exception used when a joined-script lock is still held at the watchdog's timed check. */
    public static class WatchdogException extends RuntimeException {
        /** Constructs a watchdog diagnostic with the supplied message, which may be {@code null}.
         * @param message diagnostic message
         */
        public WatchdogException(String message) {
            super(message);
        }

    }

}
