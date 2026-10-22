package com.jsmacrosce.jsmacros.core.language;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

import java.util.ArrayList;
import java.util.List;

/** Tracks a joined event's lock and callbacks queued until its release.
 * @param <T> associated script-context type
 * @since 1.4.0
 */
@DocletCategory("Events and Event Handling")
public class EventContainer<T extends BaseScriptContext<?>> {
    private final T ctx;
    private Thread lockThread;
    private boolean locked = true;

    private final List<Runnable> then = new ArrayList<>();

    /** Stores the supplied context reference and starts locked with no lock thread or queued callbacks.
     * A {@code null} context is accepted, but the first {@link #releaseLock()} call then fails with {@link NullPointerException}.
     * @param ctx context associated with this container; may be {@code null}
     */
    public EventContainer(T ctx) {
        this.ctx = ctx;
    }

    /** Returns whether this container's event lock is still held.
     * @return {@code true} until the first release
     */
    public synchronized boolean isLocked() {
        return locked;
    }

    /** Sets the lock thread; while locked and when a context is present, registers this container in its event map.
     * A non-null thread can be assigned only while no non-null thread has been assigned; assigning {@code null} leaves it assignable.
     * @param lockThread thread associated with the lock; may be {@code null}
     */
    public synchronized void setLockThread(Thread lockThread) {
        if (this.lockThread != null) {
            throw new AssertionError("Cannot change lock thread of context container once assigned!");
        }
        this.lockThread = lockThread;
        if (locked && ctx != null)
            ctx.events.put(lockThread, (EventContainer) this);
    }

    /** Returns the associated script context, or {@code null} if none was supplied.
     * @return associated context, possibly {@code null}
     */
    public T getCtx() {
        return ctx;
    }

    /** Returns the associated thread, or {@code null} before assignment.
     * @return lock thread
     */
    public Thread getLockThread() {
        return lockThread;
    }

    /** Waits once while locked, queueing a non-null callback; the wait may return spuriously.
     * If already released, runs the callback immediately. Callback failures are logged.
     *
     * @param then callback to run on release; when called from a thread bound to the context, a non-null
     *             callback must be a {@link MethodWrapper}. May be {@code null} while locked, but must be non-null if already released.
     * @throws InterruptedException if the waiting thread is interrupted
     * @since 1.4.0
     */
    public synchronized void awaitLock(Runnable then) throws InterruptedException {
        if (ctx != null && ctx.threads.contains(Thread.currentThread())) {
            if (then != null && !(then instanceof MethodWrapper)) {
                throw new AssertionError("For your safety, please use MethodWrapper in scripts.");
            }
        }
        if (locked) {
            if (then != null) {
                this.then.add(then);
            }
            this.wait();
        } else {
            try {
                then.run();
            } catch (Throwable t) {
                ctx.runner.profile.logError(t);
            }
        }
    }

    /**
     * Releases this lock once, runs queued callbacks, wakes waiters, and removes its thread association from the context.
     * If still locked, a null context causes {@link NullPointerException}; an already released lock is a no-op.
     * Callback failures are logged and do not prevent later callbacks from running.
     *
     * @since 1.4.0
     */
    public synchronized void releaseLock() {
        if (!locked) return;
        locked = false;
        ctx.runner.profile.joinedThreadStack.remove(lockThread);
        for (Runnable runnable : then) {
            try {
                runnable.run();
            } catch (Throwable t) {
                ctx.runner.profile.logError(t);
            }
        }
        then.clear();
        this.notifyAll();
        if (ctx == null) return;
        synchronized (ctx) {
            if (lockThread != null)
                ctx.events.remove(lockThread);
        }
    }

    /** Returns a diagnostic string containing the current lock state and lock-thread name; it is not a serialization format.
     * Thread names are not escaped. With no assigned thread, the name is {@code "null"}; for example, {@code ContextContainer:{"locked": true, "lockThread": "null"}}.
     * @return diagnostic text in the form {@code ContextContainer:{"locked": boolean, "lockThread": "name"}}
     */
    @Override
    public String toString() {
        return String.format("ContextContainer:{\"locked\": %s, \"lockThread\": \"%s\"}", locked, lockThread == null ? null : lockThread.getName());
    }

}
