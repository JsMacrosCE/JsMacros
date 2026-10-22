package com.jsmacrosce.jsmacros.graal.library.impl;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.language.BaseLanguage;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;
import com.jsmacrosce.jsmacros.core.library.IFWrapper;
import com.jsmacrosce.jsmacros.core.library.Library;
import com.jsmacrosce.jsmacros.core.library.PerExecLanguageLibrary;
import com.jsmacrosce.jsmacros.graal.language.impl.GraalLanguageDefinition;
import com.jsmacrosce.jsmacros.graal.language.impl.GraalScriptContext;
import com.jsmacrosce.jsmacros.graal.language.impl.WrappedThread;

/**
 * Wraps script callbacks for use as Java {@link MethodWrapper}s through the {@code JavaWrapper} library.
 * <p>
 * Graal contexts are entered by one thread at a time. Calls through these wrappers are coordinated
 * by a priority queue; asynchronous wrappers execute callbacks on the script thread pool.
 *
 * @author Wagyourtail
 * @since 1.2.5, re-named from {@code consumer} in 1.4.0
 */
@Library(value = "JavaWrapper", languages = GraalLanguageDefinition.class)
@SuppressWarnings("unused")
public class FWrapper extends PerExecLanguageLibrary<Context, GraalScriptContext> implements IFWrapper<Value> {

    /**
     * Initializes this per-execution Java wrapper library for a Graal script context and language.
     * The supplied context, language class, and the context's runner are retained by the library;
     * construction does not create a language instance or start callback execution.
     *
     * @param ctx non-null script context associated with this library
     * @param language language implementation class associated with the context
     */
    public FWrapper(GraalScriptContext ctx, Class<? extends BaseLanguage<Context, GraalScriptContext>> language) {
        super(ctx, language);
    }

    /**
     * Wraps a callable for Java use; each wrapper call executes the script function synchronously and returns its result.
     *
     * @param c executable script function
     * @param <A> type of the first callback argument
     * @param <B> type of the second callback argument
     * @param <R> callback result type
     * @return wrapper that invokes {@code c} and returns its result
     * @since 1.3.2
     */
    @Override
    @DocletReplaceParams("c: (arg0: A, arg1: B) => R | void")
    public <A, B, R> MethodWrapper<A, B, R, GraalScriptContext> methodToJava(Value c) {
        return new JSMethodWrapper<>(c, true, 5);
    }

    /**
     * Wraps a callable for Java use; each wrapper call schedules the script function on the script thread pool.
     *
     * @param c executable script function
     * @param <A> type of the first callback argument
     * @param <B> type of the second callback argument
     * @param <R> callback result type
     * @return wrapper that schedules {@code c}; callback failures are logged, not returned to the caller
     * @since 1.3.2
     */
    @Override
    @DocletReplaceParams("c: (arg0: A, arg1: B) => R | void")
    public <A, B, R> MethodWrapper<A, B, R, GraalScriptContext> methodToJavaAsync(Value c) {
        return new JSMethodWrapper<>(c, false, 5);
    }

    /**
     * Wraps a callable for asynchronous execution with an explicit queue priority. The other asynchronous overloads use priority 5.
     *
     * @param priority queue priority used to order callback execution
     * @param c executable script function
     * @param <A> type of the first callback argument
     * @param <B> type of the second callback argument
     * @param <R> callback result type
     * @return wrapper that schedules {@code c}; callback failures are logged, not returned to the caller
     */
    @Override
    public <A, B, R> MethodWrapper<A, B, R, ?> methodToJavaAsync(int priority, Value c) {
        return new JSMethodWrapper<>(c, false, priority);
    }

    /** Yields the current script task while the supplied sleep operation runs, then rejoins the queue at its existing priority.
     * @throws InterruptedException if the task is interrupted while waiting to resume
     */
    @Override
    public void deferCurrentTask() throws InterruptedException {
        ctx.wrapSleep(() -> {
        });
    }

    /** Yields the current script task while the supplied sleep operation runs, then rejoins the queue with an adjusted priority.
     * @param priorityAdjust amount added to the current task priority
     * @throws InterruptedException if the task is interrupted while waiting to resume
     */
    @Override
    public void deferCurrentTask(int priorityAdjust) throws InterruptedException {
        ctx.wrapSleep(priorityAdjust, () -> {
        });
    }

    /**
     * @return the current script task's queue priority
     */
    @Override
    public int getCurrentPriority() {
        assert ctx.tasks.peek() != null;
        return ctx.tasks.peek().priority;
    }

    /** Closes the current script context, including its Graal context.
     *
     * @since 1.2.2
     */
    @Override
    public void stop() {
        ctx.closeContext();
    }

    private class JSMethodWrapper<T, U, R> extends MethodWrapper<T, U, R, GraalScriptContext> {
        private final Value fn;
        public final boolean await;
        public final int priority;

        JSMethodWrapper(Value fn, boolean await, int priority) {
            super(FWrapper.this.ctx);
            if (!fn.canExecute()) {
                throw new AssertionError("c is not executable");
            }
            this.fn = fn;
            this.await = await;
            this.priority = priority;
        }

        private void innerAccept(Object... args) {
            if (await) {
                innerApply(args);
                return;
            }

            if (ctx.isContextClosed()) {
                throw new BaseScriptContext.ScriptAssertionError("Context closed");
            }

            runner.threadPool.runTask(() -> {
                try {
                    ctx.bindThread(Thread.currentThread());
                    if (!ctx.isMultiThreaded()) {
                        WrappedThread wt = new WrappedThread(Thread.currentThread(), priority);
                        ctx.tasks.add(wt);

                        // wait to be at the front of the queue again
                        assert ctx.tasks.peek() != null;
                        if (ctx.tasks.peek().thread != Thread.currentThread()) {
                            wt.waitUntilReady();
                        }

                        if (ctx.isContextClosed()) {
                            ctx.unbindThread(Thread.currentThread());
                            assert ctx.tasks.peek() != null;
                            ctx.tasks.poll();
                            WrappedThread next = ctx.tasks.peek();
                            if (next != null) {
                                next.notifyReady();
                            }
                            throw new BaseScriptContext.ScriptAssertionError("Context closed");
                        }
                    }

                    ctx.getContext().enter();
                    try {
                        fn.executeVoid(args);
                    } catch (Throwable ex) {
                        runner.profile.logError(ex);
                    } finally {
                        ctx.getContext().leave();
                        ctx.releaseBoundEventIfPresent(Thread.currentThread());
                        runner.profile.joinedThreadStack.remove(Thread.currentThread());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } finally {
                    ctx.unbindThread(Thread.currentThread());
                    if (!ctx.isMultiThreaded()) {
                        ctx.tasks.poll();
                        WrappedThread next = ctx.tasks.peek();
                        if (next != null) {
                            next.notifyReady();
                        }
                    }
                }
            });
        }

        private <R2> R2 innerApply(Object... args) {
            if (ctx.isContextClosed()) {
                throw new BaseScriptContext.ScriptAssertionError("Context closed");
            }

            if (ctx.getBoundThreads().contains(Thread.currentThread())) {
                return (R2) fn.execute(args).as(Object.class);
            }

            try {
                ctx.bindThread(Thread.currentThread());
                if (!ctx.isMultiThreaded()) {
                    WrappedThread wt = new WrappedThread(Thread.currentThread(), priority);
                    ctx.tasks.add(wt);

                    // wait to be at the front of the queue again
                    WrappedThread peek = ctx.tasks.peek();
                    if (peek.thread != Thread.currentThread()) {
                        wt.waitUntilReady();
                    }

                    if (ctx.isContextClosed()) {
                        ctx.unbindThread(Thread.currentThread());
                        ctx.tasks.poll();
                        WrappedThread next = ctx.tasks.peek();
                        if (next != null) {
                            next.notifyReady();
                        }
                        throw new BaseScriptContext.ScriptAssertionError("Context closed");
                    }
                }

                ctx.getContext().enter();
                try {
                    if (await && runner.profile.checkJoinedThreadStack()) {
                        runner.profile.joinedThreadStack.add(Thread.currentThread());
                    }
                    return (R2) fn.execute(args).as(Object.class);
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                } finally {
                    ctx.getContext().leave();
                    ctx.releaseBoundEventIfPresent(Thread.currentThread());
                    runner.profile.joinedThreadStack.remove(Thread.currentThread());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                ctx.unbindThread(Thread.currentThread());
                if (!ctx.isMultiThreaded()) {
                    ctx.tasks.poll();
                    WrappedThread next = ctx.tasks.peek();
                    if (next != null) {
                        next.notifyReady();
                    }
                }
            }
        }

        @Override
        public void accept(T t) {
            innerAccept(t);
        }

        @Override
        public void accept(T t, U u) {
            innerAccept(t, u);
        }

        @Override
        public R apply(T t) {
            return innerApply(t);
        }

        @Override
        public R apply(T t, U u) {
            return innerApply(t, u);
        }

        @Override
        public boolean test(T t) {
            return innerApply(t);
        }

        @Override
        public boolean test(T t, U u) {
            return innerApply(t, u);
        }

        @Override
        public void run() {
            innerAccept();
        }

        @Override
        public int compare(T o1, T o2) {
            return innerApply(o1, o2);
        }

        @Override
        public R get() {
            return innerApply();
        }

    }

}
