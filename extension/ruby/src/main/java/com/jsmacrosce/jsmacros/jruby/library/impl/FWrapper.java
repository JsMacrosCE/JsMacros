package com.jsmacrosce.jsmacros.jruby.library.impl;

import org.jruby.RubyMethod;
import org.jruby.embed.ScriptingContainer;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.language.BaseLanguage;
import com.jsmacrosce.jsmacros.core.library.IFWrapper;
import com.jsmacrosce.jsmacros.core.library.Library;
import com.jsmacrosce.jsmacros.core.library.PerExecLanguageLibrary;
import com.jsmacrosce.jsmacros.jruby.language.impl.JRubyLanguageDefinition;
import com.jsmacrosce.jsmacros.jruby.language.impl.JRubyScriptContext;

@Library(value = "JavaWrapper", languages = JRubyLanguageDefinition.class)
public class FWrapper extends PerExecLanguageLibrary<ScriptingContainer, JRubyScriptContext> implements IFWrapper<RubyMethod> {

    public FWrapper(JRubyScriptContext context, Class<? extends BaseLanguage<ScriptingContainer, JRubyScriptContext>> language) {
        super(context, language);
    }

    @Override
    public <A, B, R> MethodWrapper<A, B, R, ?> methodToJava(RubyMethod c) {
        return new RubyMethodWrapper<>(c, true, ctx);
    }

    @Override
    public <A, B, R> MethodWrapper<A, B, R, ?> methodToJavaAsync(RubyMethod c) {
        return new RubyMethodWrapper<>(c, false, ctx);
    }

    @Override
    public void stop() {
        ctx.closeContext();
    }

    private static class RubyMethodWrapper<T, U, R> extends MethodWrapper<T, U, R, JRubyScriptContext> {
        private final RubyMethod fn;
        private final boolean await;

        RubyMethodWrapper(RubyMethod fn, boolean await, JRubyScriptContext ctx) {
            super(ctx);
            this.fn = fn;
            this.await = await;
        }

        private Object callFn(Object... params) {
            ThreadContext threadContext = ctx.getContext().getProvider().getRuntime().getCurrentContext();
            threadContext.pushNewScope(threadContext.getCurrentStaticScope());
            try {
                IRubyObject[] rubyObjects = JavaUtil.convertJavaArrayToRuby(threadContext.runtime, params);
                return fn.call(threadContext, rubyObjects, threadContext.getFrameBlock()).toJava(Object.class);
            } finally {
                threadContext.popScope();
            }
        }

        private void innerAccept(Object... params) {
            if (await) {
                innerApply(params);
                return;
            }

            Thread t = new Thread(() -> {
                ctx.bindThread(Thread.currentThread());
                try {
                    callFn(params);
                } catch (Throwable ex) {
                    ctx.runner.profile.logError(ex);
                } finally {
                    ctx.unbindThread(Thread.currentThread());
                    ctx.runner.profile.joinedThreadStack.remove(Thread.currentThread());
                    ctx.releaseBoundEventIfPresent(Thread.currentThread());
                }
            }, "JRuby-JavaWrapper");
            t.setDaemon(true);
            t.start();
        }

        @SuppressWarnings("unchecked")
        private <R2> R2 innerApply(Object... params) {
            if (ctx.getBoundThreads().contains(Thread.currentThread())) {
                return (R2) callFn(params);
            }

            try {
                ctx.bindThread(Thread.currentThread());
                if (ctx.runner.profile.checkJoinedThreadStack()) {
                    ctx.runner.profile.joinedThreadStack.add(Thread.currentThread());
                }
                return (R2) callFn(params);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            } finally {
                ctx.releaseBoundEventIfPresent(Thread.currentThread());
                ctx.unbindThread(Thread.currentThread());
                ctx.runner.profile.joinedThreadStack.remove(Thread.currentThread());
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
            return (boolean) innerApply(t);
        }

        @Override
        public boolean test(T t, U u) {
            return (boolean) innerApply(t, u);
        }

        @Override
        public void run() {
            innerAccept();
        }

        @Override
        public int compare(T o1, T o2) {
            Object result = innerApply(o1, o2);
            if (!(result instanceof Number)) {
                throw new ClassCastException("Ruby comparator must return a numeric value, got: " + result);
            }
            return ((Number) result).intValue();
        }

        @Override
        public R get() {
            return innerApply();
        }
    }

}
