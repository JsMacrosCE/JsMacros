package com.jsmacrosce.jsmacros.graal.language.impl;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.extensions.Extension;
import com.jsmacrosce.jsmacros.core.language.BaseLanguage;
import com.jsmacrosce.jsmacros.core.language.EventContainer;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.graal.GraalConfig;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class GraalLanguageDefinition extends BaseLanguage<Context, GraalScriptContext> {
    private static volatile Engine engine = null;
    public static final boolean isJsInstalled;

    static {
        // Create a temporary engine just to check available languages
        try (Engine tempEngine = Engine.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build()) {
            isJsInstalled = tempEngine.getLanguages().containsKey("js");
        }
    }

    public GraalLanguageDefinition(Extension extension, Core<?, ?> runner) {
        super(extension, runner);
        engine = getOrCreateEngine(runner);
    }

    public static Engine getOrCreateEngine(Core<?, ?> runner) {
        Engine e = engine;
        if (e != null) return e;

        synchronized (GraalLanguageDefinition.class) {
            if (engine != null) return engine;

            Engine.Builder b = Engine.newBuilder()
                    .option("engine.WarnInterpreterOnly", "false");

            final GraalConfig conf = runner.config.getOptions(GraalConfig.class);
            for (Map.Entry<String, String> e2 : conf.extraEngineOptions.entrySet()) {
                try {
                    b.option(e2.getKey(), e2.getValue());
                    runner.profile.LOGGER.info("Added GraalVM engine option from config: {} = {}", e2.getKey(), e2.getValue());
                } catch (IllegalArgumentException ex) {
                    runner.profile.logError(new RuntimeException("Invalid GraalVM option: " + e2.getKey() + " = " + e2.getValue(), ex));
                }
            }

            return engine = b.build();
        }
    }

    protected Context buildContext(File currentDir, String lang, Map<String, String> extraLangOptions, Map<String, Object> globals, Map<String, BaseLibrary> libs) throws IOException {

        Builder build = Context.newBuilder()
                .engine(engine)
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .allowExperimentalOptions(true);

        for (Map.Entry<String, String> e : extraLangOptions.entrySet()) {
            try {
                build.option(e.getKey(), e.getValue());
            } catch (IllegalArgumentException ex) {
                runner.profile.logError(new RuntimeException("Invalid GraalVM option: " + e.getKey() + " = " + e.getValue(), ex));
            }
        }

        if (currentDir == null) {
            currentDir = runner.config.macroFolder;
        }
        build.currentWorkingDirectory(currentDir.toPath().toAbsolutePath());

        if (isJsInstalled && lang.equals("js")) {
            build.option("js.commonjs-require", "true");
            build.option("js.commonjs-require-cwd", currentDir.getCanonicalPath());
        }

        final Context con = build.build();

        // Set Bindings
        final Value binds = con.getBindings(lang);

        if (globals != null) {
            globals.forEach(binds::putMember);
        }

        libs.forEach(binds::putMember);

        return con;
    }

    @Override
    protected void exec(EventContainer<GraalScriptContext> ctx, ScriptTrigger macro, BaseEvent event) throws Exception {
        Map<String, Object> globals = new HashMap<>();

        globals.put("event", event);
        globals.put("file", ctx.getCtx().getFile());
        globals.put("context", ctx);

        final GraalConfig conf = runner.config.getOptions(GraalConfig.class);
        if (conf.extraEngineOptions == null) {
            conf.extraEngineOptions = new LinkedHashMap<>();
        }

        if (conf.extraLangOptions == null) {
            conf.extraLangOptions = new LinkedHashMap<>();
        }

        Map<String, BaseLibrary> lib = retrieveLibs(ctx.getCtx());
        String lang = Source.findLanguage(ctx.getCtx().getFile());
        if (!engine.getLanguages().containsKey(lang)) {
            if (isJsInstalled) {
                lang = "js";
            } else {
                lang = engine.getLanguages().keySet().stream().findFirst().orElseThrow(() -> new RuntimeException("No GraalVM languages installed!"));
            }
        }
        final Context con = buildContext(ctx.getCtx().getContainedFolder(), lang, conf.extraLangOptions.getOrDefault(lang, new LinkedHashMap<>()), globals, lib);
        ctx.getCtx().setContext(con);
        con.enter();
        try {
            assert ctx.getCtx().getFile() != null;
            con.eval(Source.newBuilder(lang, ctx.getCtx().getFile()).build());
        } finally {
            try {
                con.leave();
                ctx.getCtx().tasks.poll();
                WrappedThread next = ctx.getCtx().tasks.peek();
                if (next != null) {
                    next.notifyReady();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void exec(EventContainer<GraalScriptContext> ctx, String lang, String script, BaseEvent event) throws Exception {
        Map<String, Object> globals = new HashMap<>();

        globals.put("event", event);
        globals.put("file", ctx.getCtx().getFile());
        globals.put("context", ctx);

        final GraalConfig conf = runner.config.getOptions(GraalConfig.class);
        if (conf.extraEngineOptions == null) {
            conf.extraEngineOptions = new LinkedHashMap<>();
        }

        if (conf.extraLangOptions == null) {
            conf.extraLangOptions = new LinkedHashMap<>();
        }

        Map<String, BaseLibrary> lib = retrieveLibs(ctx.getCtx());
        lang = Source.findLanguage(new File(lang.startsWith(".") ? lang : "." + lang));
        if (!engine.getLanguages().containsKey(lang)) {
            if (isJsInstalled) {
                lang = "js";
            } else {
                lang = engine.getLanguages().keySet().stream().findFirst().orElseThrow(() -> new RuntimeException("No GraalVM languages installed!"));
            }
        }
        final Context con = buildContext(ctx.getCtx().getContainedFolder(), lang, conf.extraLangOptions.getOrDefault(lang, new LinkedHashMap<>()), globals, lib);
        ctx.getCtx().setContext(con);
        con.enter();
        try {
            if (ctx.getCtx().getFile() != null) {
                con.eval(Source.newBuilder(lang, ctx.getCtx().getFile()).content(script).build());
            } else {
                con.eval(lang, script);
            }
        } catch (Throwable e) {
            throw e;
        } finally {
            try {
                con.leave();
                ctx.getCtx().tasks.poll();
                WrappedThread next = ctx.getCtx().tasks.peek();
                if (next != null) {
                    next.notifyReady();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public GraalScriptContext createContext(BaseEvent event, File file) {
        return new GraalScriptContext(runner, event, file);
    }

}
