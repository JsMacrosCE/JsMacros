package com.jsmacrosce.jsmacros.graal.language.impl;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import com.jsmacrosce.PrioryFiFoTaskQueue;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;

import java.io.File;

/** Holds the runtime context and task queue for a Graal script. Scripts obtain it from the {@code context} global:
 * <pre>
 * const ctx = context.getCtx();
 * Chat.log(ctx.isMultiThreaded());
 * </pre>
 */
public class GraalScriptContext extends BaseScriptContext<Context> {
    /** Live per-context queue of wrapped threads, ordered using their stored priorities. */
    public final PrioryFiFoTaskQueue<WrappedThread> tasks = new PrioryFiFoTaskQueue<>(GraalScriptContext::getThreadPriority);

    /** Initializes the context with its owner and optional triggering event and script file.
     * @param runner owning core
     * @param event triggering event, or {@code null} if none
     * @param file script file, or {@code null} if none
     */
    public GraalScriptContext(Core<?, ?> runner, BaseEvent event, File file) {
        super(runner, event, file);
    }

    /** Releases bound event locks, interrupts bound threads and unregisters this context through base cleanup,
     * then calls {@code close(true)} on the assigned Graal runtime if present. */
    @Override
    public void closeContext() {
        super.closeContext();
        Context ctx = getContext();
        if (ctx != null) {
            ctx.close(true);
        }
    }

    @Override
    protected boolean isLanguageCancellation(Throwable failure) {
        return failure instanceof PolyglotException && ((PolyglotException) failure).isCancelled();
    }

    /** Selects a queue priority from a wrapped thread's stored priority.
     * @param thread wrapped thread whose queue priority to inspect
     * @return the negated stored priority; negating {@code Integer.MIN_VALUE} leaves it unchanged
     */
    public static int getThreadPriority(Object thread) {
        return -((WrappedThread) thread).priority;
    }

    /** Reports that this context does not advertise concurrent execution on multiple threads.
     * @return {@code false} because this context does not advertise concurrent execution
     */
    @Override
    public boolean isMultiThreaded() {
        return false;
    }

    /** Assigns and binds the main thread through the base context, then enqueues it with stored priority 5.
     * This does not start the thread or change its Java thread priority.
     * @param t thread to assign and bind
     */
    @Override
    public void setMainThread(Thread t) {
        super.setMainThread(t);
        WrappedThread w = new WrappedThread(t, 5);
        tasks.add(w);
    }

    /** Runs a potentially blocking operation through the task-queue scheduling path without changing its priority.
     * @param sleep operation that may block and throw {@code InterruptedException}
     */
    @Override
    public void wrapSleep(SleepRunnable sleep) throws InterruptedException {
        wrapSleep(0, sleep);
    }

    /** Leaves the Graal runtime, polls the queue head and signals the next entry, if present, before running the operation.
     * After the operation, a new wrapper for the calling thread is enqueued with the polled entry's stored priority
     * plus {@code changePriority}, using Java integer arithmetic. It waits if the queue head then belongs to another
     * thread. Runtime re-entry is attempted in the outer {@code finally} block after a successful leave.
     * This requires an assigned, entered runtime and a suitable queue head; the method does not verify that the
     * polled entry belongs to the caller. If the operation or wait is interrupted, this method does not remove the
     * newly enqueued entry.
     * @param changePriority adjustment added to the polled entry's stored priority for the new entry
     * @param sleep operation to run while the runtime is left; it may block and throw {@code InterruptedException}
     */
    public void wrapSleep(int changePriority, SleepRunnable sleep) throws InterruptedException {
        getContext().leave();
        try {
            assert tasks.peek() != null;
            // remove self from queue
            WrappedThread current = tasks.poll();
            WrappedThread next = tasks.peek();
            if (next != null) {
                next.notifyReady();
            }

            try {
                sleep.run();
            } finally {
                // put self at back of the queue
                WrappedThread wt = new WrappedThread(Thread.currentThread(), current.priority + changePriority);
                tasks.add(wt);

                // wait to be at the front of the queue again
                assert tasks.peek() != null;
                if (tasks.peek().thread != Thread.currentThread()) {
                    wt.waitUntilReady();
                }
            }
        } finally {
            getContext().enter();
        }
    }

}
