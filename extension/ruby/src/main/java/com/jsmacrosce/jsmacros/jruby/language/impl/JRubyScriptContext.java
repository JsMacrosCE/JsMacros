package com.jsmacrosce.jsmacros.jruby.language.impl;

import org.jruby.RubyThread;
import org.jruby.embed.ScriptingContainer;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;

import java.io.File;

public class JRubyScriptContext extends BaseScriptContext<ScriptingContainer> {
    public JRubyScriptContext(Core<?, ?> runner, BaseEvent event, File file) {
        super(runner, event, file);
    }

    @Override
    protected void doSubclassClose() {
        ScriptingContainer ctx = getContext();
        if (ctx == null) return;
        // Ruby's Thread.new spawns JRuby-runtime-owned threads that aren't
        // tracked in BaseScriptContext.threads, so closeContext's interrupt
        // wave misses them. Interrupt them here so closing the context
        // actually stops user code spawned via Thread.new.
        try {
            for (RubyThread rt : ctx.getRuntime().getThreadService().getActiveRubyThreads()) {
                Thread nativeThread = rt.getNativeThread();
                if (nativeThread != null && nativeThread != Thread.currentThread()) {
                    nativeThread.interrupt();
                }
            }
        } catch (Throwable ex) {
            runner.profile.logError(ex);
        }
        ctx.terminate();
    }

    @Override
    public boolean isMultiThreaded() {
        return true;
    }

}
