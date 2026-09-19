package com.jsmacrosce.jsmacros.jruby.language.impl;

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
    public synchronized void closeContext() {
        super.closeContext();
        ScriptingContainer ctx = getContext();
        if (ctx != null) {
            ctx.terminate();
        }
    }

    @Override
    public boolean isMultiThreaded() {
        return true;
    }

}
