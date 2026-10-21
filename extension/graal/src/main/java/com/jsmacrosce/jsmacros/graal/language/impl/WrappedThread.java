package com.jsmacrosce.jsmacros.graal.language.impl;

import java.util.concurrent.locks.LockSupport;

/**
 * Associates a Java thread with its Graal task-queue priority and a one-way readiness signal.
 * <pre>
 * const WrappedThread = Java.type("com.jsmacrosce.jsmacros.graal.language.impl.WrappedThread");
 * const Thread = Java.type("java.lang.Thread");
 * const task = new WrappedThread(Thread.currentThread(), 5);
 * task.notifyReady();
 * task.waitUntilReady(); // already signaled: returns without parking
 * </pre>
 * Do not block a script waiting for a notifier that has not been scheduled.
 */
public class WrappedThread {
    /** The Java thread represented by this task; this reference is not copied or started. */
    public final Thread thread;
    /** Graal task-queue priority value, not a {@code Thread.setPriority} value. */
    public final int priority;
    private volatile boolean ready = false;

    /**
     * Stores the thread and queue priority; the readiness signal starts unset.
     * @param thread Java thread represented by this task
     * @param priority Graal task-queue priority, not Java thread priority
     */
    public WrappedThread(Thread thread, int priority) {
        this.thread = thread;
        this.priority = priority;
    }

    /**
     * Waits for readiness on the represented thread; another caller receives an {@code AssertionError}.
     * Parks while not ready. After each park, an interrupt clears the current thread's interrupt status
     * and causes {@code InterruptedException}; an already-ready call returns without checking it.
     */
    public void waitUntilReady() throws InterruptedException {
        if (Thread.currentThread() != thread) throw new AssertionError("not the same thread");
        while (!ready) {
            LockSupport.park();
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }

    /** Sets readiness and unparks the represented thread; readiness is one-way. */
    public void notifyReady() {
        ready = true;
        LockSupport.unpark(thread);
    }

}
