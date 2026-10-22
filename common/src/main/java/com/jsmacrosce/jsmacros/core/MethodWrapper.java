package com.jsmacrosce.jsmacros.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;

import java.util.Comparator;
import java.util.function.*;

/**
 * Implements several Java functional interfaces in one callback wrapper, optionally associated with a script context.
 * Script functions are commonly exposed through {@link com.jsmacrosce.jsmacros.core.library.IFWrapper}.
 *
 * @param <T> the first input type
 * @param <U> the second input type
 * @param <R> the result type
 * @param <C> the associated script-context type
 */
public abstract class MethodWrapper<T, U, R, C extends BaseScriptContext<?>> implements Consumer<T>, BiConsumer<T, U>, Function<T, R>, BiFunction<T, U, R>, Predicate<T>, BiPredicate<T, U>, Runnable, Supplier<R>, Comparator<T> {

    private final Object syncObject;

    /**
     * The associated script context, or {@code null} when this wrapper was created without one. A non-null reference
     * keeps that context reachable for as long as this wrapper remains reachable.
     */
    protected final C ctx;

    protected MethodWrapper() {
        ctx = null;
        syncObject = null;
    }

    /**
     * Associates this wrapper with a script context, marks it as having had a method wrapper invoked, and captures its synchronization object.
     * @param containingContext the context associated with this wrapper; must not be {@code null}
     */
    public MethodWrapper(C containingContext) {
        ctx = containingContext;
        ctx.hasMethodWrapperBeenInvoked = true;
        syncObject = ctx.getSyncObject();
    }

    /** Returns the script context associated with this wrapper, or {@code null} for a contextless wrapper.
     * @return the associated context, or {@code null} if none was set
     */
    @Nullable
    public C getCtx() {
        return ctx;
    }

    /** Accepts one input value.
     * @param t the input value
     */
    @Override
    public abstract void accept(T t);

    /** Accepts two input values.
     * @param t the first input value
     * @param u the second input value
     */
    @Override
    public abstract void accept(T t, U u);

    /** Applies this callback to one input value and returns its result.
     * @param t the input value
     * @return the callback result, which may be {@code null}
     */
    @Override
    public abstract R apply(T t);

    /** Applies this callback to two input values and returns its result.
     * @param t the first input value
     * @param u the second input value
     * @return the callback result, which may be {@code null}
     */
    @Override
    public abstract R apply(T t, U u);

    /** Tests one input value and returns the callback's boolean result.
     * @param t the input value
     * @return the callback's boolean result
     */
    @Override
    public abstract boolean test(T t);

    /** Tests two input values and returns the callback's boolean result.
     * @param t the first input value
     * @param u the second input value
     * @return the callback's boolean result
     */
    @Override
    public abstract boolean test(T t, U u);

    /**
     * Returns whether callbacks in this wrapper must not join their creating script context.
     * Override for script languages that do not support joining.
     */
    @DocletIgnore
    @Deprecated
    public boolean preventSameScriptJoin() {
        return !ctx.isMultiThreaded();
    }

    /**
     * Returns the thread to use for an event callback instead of the thread captured at registration, or {@code null} to use the captured thread.
     * @return the alternative callback thread, or {@code null} to use the captured thread
     */
    public Thread overrideThread() {
        return null;
    }

    /**
     * Composes this wrapper with {@code after}. Function and supplier calls pass this wrapper's result
     * to {@code after}; consumer and runnable calls invoke this wrapper and then run {@code after} if it
     * is a {@code MethodWrapper}. Predicate calls pass their boolean result to {@code after} if it is a
     * {@code MethodWrapper}; otherwise they return this wrapper's predicate result unchanged. Comparator
     * calls retain this wrapper's comparison result and run {@code after} only if it is a {@code MethodWrapper}.
     *
     * @param after function applied to results, or a {@link MethodWrapper} for callback-style calls
     * @param <V> the result type produced by {@code after}
     * @return a wrapper that composes this wrapper with {@code after}
     */
    @NotNull
    @Override
    public <V> MethodWrapper<T, U, V, C> andThen(@NotNull Function<? super R, ? extends V> after) {
        return new AndThenMethodWrapper<>(this, after);
    }

    /**
     * Returns a wrapper with inverted predicate results and negated comparator results; other operations are delegated unchanged.
     * @return the negated wrapper
     */
    @NotNull
    @Override
    public MethodWrapper<T, U, R, C> negate() {
        return new NegateMethodWrapper<>(this);
    }

    private static class AndThenMethodWrapper<T, U, R, V, C extends BaseScriptContext<?>> extends MethodWrapper<T, U, V, C> {
        private final MethodWrapper<T, U, R, C> self;
        private final Function<? super R, ? extends V> after;

        AndThenMethodWrapper(MethodWrapper<T, U, R, C> self, Function<? super R, ? extends V> after) {
            super(self.ctx);
            this.self = self;
            this.after = after;
        }

        @Override
        public int compare(T o1, T o2) {
            int retVal = self.compare(o1, o2);
            if (after instanceof MethodWrapper) {
                ((MethodWrapper<?, ?, ?, ?>) after).run();
            }
            return retVal;
        }

        @Override
        public void accept(T t) {
            self.accept(t);
            if (after instanceof MethodWrapper) {
                ((MethodWrapper<?, ?, ?, ?>) after).run();
            }
        }

        @Override
        public void accept(T t, U u) {
            self.accept(t, u);
            if (after instanceof MethodWrapper) {
                ((MethodWrapper<?, ?, ?, ?>) after).run();
            }
        }

        @Override
        public V apply(T t) {
            return after.apply(self.apply(t));
        }

        @Override
        public V apply(T t, U u) {
            return after.apply(self.apply(t, u));
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean test(T t) {
            boolean result = self.test(t);
            if (after instanceof MethodWrapper) {
                return ((MethodWrapper<Boolean, ?, ?, ?>) after).test(result);
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean test(T arg0, U arg1) {
            boolean result = self.test(arg0, arg1);
            if (after instanceof MethodWrapper) {
                return ((MethodWrapper<Boolean, ?, ?, ?>) after).test(result);
            }
            return result;
        }

        @Override
        public boolean preventSameScriptJoin() {
            boolean afterPrevent = false;
            if (after instanceof MethodWrapper) {
                afterPrevent = ((MethodWrapper<?, ?, ?, ?>) after).preventSameScriptJoin();
            }
            return self.preventSameScriptJoin() || afterPrevent;
        }

        @Override
        public void run() {
            self.run();
            if (after instanceof MethodWrapper) {
                ((MethodWrapper<?, ?, ?, ?>) after).run();
            }
        }

        @Override
        public V get() {
            return after.apply(self.get());
        }

    }

    private static class NegateMethodWrapper<T, U, R, C extends BaseScriptContext<?>> extends MethodWrapper<T, U, R, C> {
        private final MethodWrapper<T, U, R, C> self;

        NegateMethodWrapper(MethodWrapper<T, U, R, C> self) {
            super(self.ctx);
            this.self = self;
        }

        @Override
        public int compare(T o1, T o2) {
            return -self.compare(o1, o2);
        }

        @Override
        public void accept(T t) {
            self.accept(t);
        }

        @Override
        public void accept(T t, U u) {
            self.accept(t, u);
        }

        @Override
        public R apply(T t) {
            return self.apply(t);
        }

        @Override
        public R apply(T t, U u) {
            return self.apply(t, u);
        }

        @Override
        public boolean test(T t) {
            return !self.test(t);
        }

        @Override
        public boolean test(T t, U u) {
            return !self.test(t, u);
        }

        @Override
        public boolean preventSameScriptJoin() {
            return self.preventSameScriptJoin();
        }

        @Override
        public void run() {
            self.run();
        }

        @Override
        public R get() {
            return self.get();
        }

    }

}
