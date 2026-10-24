package com.jsmacrosce.jsmacros.jruby.language.impl;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.language.BaseLanguage;
import com.jsmacrosce.jsmacros.core.language.EventContainer;
import com.jsmacrosce.jsmacros.jruby.client.JRubyExtension;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class JRubyLanguageDefinition extends BaseLanguage<ScriptingContainer, JRubyScriptContext> {
    public JRubyLanguageDefinition(JRubyExtension extension, Core<?, ?> runner) {
        super(extension, runner);
    }

    private void runInstance(EventContainer<JRubyScriptContext> ctx, BaseEvent event, ScriptletRunner scriptlet, @Nullable Path cwd) throws Exception {
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        ctx.getCtx().setContext(instance);

        if (cwd != null) {
            instance.setCurrentDirectory(cwd.toString());
        }

        retrieveLibs(ctx.getCtx()).forEach((name, lib) -> {
            // "Time" is a built-in Ruby class; expose jsmacros' Time library under FTime instead.
            String bindName = "Time".equals(name) ? "FTime" : name;
            instance.put(bindName, lib);
        });
        instance.put("event", event);
        instance.put("file", ctx.getCtx().getFile());
        instance.put("context", ctx);

        scriptlet.run(instance);
    }

    @Override
    protected void exec(EventContainer<JRubyScriptContext> ctx, ScriptTrigger macro, BaseEvent event) throws Exception {
        File file = ctx.getCtx().getFile();
        runInstance(ctx, event, instance -> {
            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                instance.runScriptlet(reader, file.getAbsolutePath());
            }
        }, parentPathOf(file));
    }

    @Override
    protected void exec(EventContainer<JRubyScriptContext> ctx, String lang, String script, BaseEvent event) throws Exception {
        File file = ctx.getCtx().getFile();
        runInstance(ctx, event, instance -> {
            if (file != null) {
                instance.runScriptlet(new StringReader(script), file.getAbsolutePath());
            } else {
                instance.runScriptlet(script);
            }
        }, parentPathOf(file));
    }

    @Override
    public JRubyScriptContext createContext(BaseEvent event, File path) {
        return new JRubyScriptContext(runner, event, path);
    }

    private static @Nullable Path parentPathOf(@Nullable File f) {
        if (f == null) return null;
        File parent = f.getParentFile();
        return parent != null ? parent.toPath() : null;
    }

    private interface ScriptletRunner {
        void run(ScriptingContainer instance) throws Exception;
    }
}
