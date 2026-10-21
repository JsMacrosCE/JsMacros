package com.jsmacrosce.jsmacros.core.threads;

import java.util.ArrayDeque;
import java.util.function.Consumer;

/**
 * Runs tasks on one-shot daemon workers, each of which executes one task and exits. When workers
 * are removed, a background task is notified and adds a free worker if the pool is below its target;
 * this does not guarantee that the target is reached.
 * <pre>
 * const pool = context.getCtx().runner.threadPool;
 * pool.runTask(JavaWrapper.methodToJava(function() { Chat.log("task"); }));
 * </pre>
 */
public class JsMacrosThreadPool {
    private final ArrayDeque<PoolThread> freeThreads = new ArrayDeque<>();

    /** Target number of prestarted free workers; this is not a limit on concurrently running tasks. */
    public final int maxFreeThreads;

    /** Uses {@code availableProcessors() * 3} as the free-worker target and starts replenishment. */
    public JsMacrosThreadPool() {
        this(Runtime.getRuntime().availableProcessors() * 3);
    }

    /**
     * Starts the initial workers and replenishment for the supplied free-worker target.
     * @param maxFreeThreads target number of prestarted free workers, not a concurrent-task limit
     */
    public JsMacrosThreadPool(int maxFreeThreads) {
        this.maxFreeThreads = maxFreeThreads;
        for (int i = 0; i < maxFreeThreads; i++) {
            PoolThread t = new PoolThread();
            t.start();
            freeThreads.addLast(t);
        }
        runTask(() -> {
            while (true) {
                synchronized (freeThreads) {
                    try {
                        freeThreads.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if (freeThreads.size() < maxFreeThreads) {
                        PoolThread t = new PoolThread();
                        t.start();
                        freeThreads.addLast(t);
                    }
                }
            }
        });
    }

    /** Assigns a task to a free worker, or starts a new worker if none is free.
     * @param task non-null task to execute
     * @return the worker selected for the task; this does not wait for task completion
     */
    public Thread runTask(Runnable task) {
        PoolThread t;
        synchronized (freeThreads) {
            if (freeThreads.isEmpty()) {
                // this shouldn't happen, I guess if called too fast...
                t = new PoolThread();
                t.runTask(task);
                t.start();
            } else {
                t = freeThreads.removeLast();
                freeThreads.notify();
                t.runTask(task);
            }
        }
        return t;
    }

    /** Invokes the hook on the calling thread before assigning the task, as in {@code runTask(Runnable)}.
     * If the hook throws after a free worker is removed from the pool, that worker is not restored;
     * for a newly created worker, the hook throws before task assignment and the worker is not started.
     * @param task non-null task to execute
     * @param beforeRunTask non-null hook receiving the selected worker before task assignment
     * @return the worker selected for the task; this does not wait for task completion
     */
    public Thread runTask(Runnable task, Consumer<Thread> beforeRunTask) {
        PoolThread t;
        synchronized (freeThreads) {
            if (freeThreads.isEmpty()) {
                // this shouldn't happen, I guess if called too fast...
                t = new PoolThread();
                beforeRunTask.accept(t);
                t.runTask(task);
                t.start();
            } else {
                t = freeThreads.removeLast();
                freeThreads.notify();
                beforeRunTask.accept(t);
                t.runTask(task);
            }
        }
        return t;
    }

    /** One-shot daemon worker; assign a task and then start it, rather than reusing it.
     * <pre>
     * const Worker = Java.type("com.jsmacrosce.jsmacros.core.threads.JsMacrosThreadPool$PoolThread");
     * const worker = new Worker();
     * worker.runTask(JavaWrapper.methodToJava(function() { Chat.log("task"); }));
     * worker.start();
     * </pre>
     */
    public static class PoolThread extends Thread {
        private Runnable task;

        /** Creates an unstarted daemon worker named {@code JsMacros Pool Thread}. */
        public PoolThread() {
            super("JsMacros Pool Thread");
            setDaemon(true);
        }

        /** Assigns the task, copies the caller's context class loader, and signals this worker; does not start it.
         * A pending task can be replaced; tasks are not queued or looped.
         * @param task non-null task to execute
         */
        public void runTask(Runnable task) {
            synchronized (this) {
                this.setContextClassLoader(Thread.currentThread().getContextClassLoader());
                this.task = task;
                notify();
            }
        }

        /** Waits for an assigned task, executes it once, and exits; any thrown {@code Throwable} is ignored. */
        @Override
        public void run() {
            try {
                synchronized (this) {
                    while (task == null) {
                        wait();
                    }
                }
                task.run();
            } catch (Throwable ignored) {
            }
        }
    }
}
