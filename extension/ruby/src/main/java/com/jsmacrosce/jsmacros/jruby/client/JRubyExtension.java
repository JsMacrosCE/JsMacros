package com.jsmacrosce.jsmacros.jruby.client;

import com.google.common.collect.Sets;
import org.jruby.RubyException;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.extensions.LanguageExtension;
import com.jsmacrosce.jsmacros.core.extensions.LibraryExtension;
import com.jsmacrosce.jsmacros.core.language.BaseLanguage;
import com.jsmacrosce.jsmacros.core.language.BaseWrappedException;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.jruby.language.impl.JRubyLanguageDefinition;
import com.jsmacrosce.jsmacros.jruby.library.impl.FWrapper;

import java.io.File;
import java.util.Arrays;
import java.util.Set;

public class JRubyExtension implements LanguageExtension, LibraryExtension {

    private static JRubyLanguageDefinition languageDefinition;

    @Override
    public String getExtensionName() {
        return "jruby";
    }

    @Override
    public void init(Core<?, ?> runner) {
        Thread t = new Thread(() -> {
            ScriptingContainer instance = new ScriptingContainer();
            instance.runScriptlet("p \"Ruby Pre-Loaded\"");
            instance.terminate();
        }, "JRuby-Preload");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public ExtMatch extensionMatch(File file) {
        if (file.getName().endsWith(".rb")) {
            if (file.getName().contains(getExtensionName())) {
                return ExtMatch.MATCH_WITH_NAME;
            } else {
                return ExtMatch.MATCH;
            }
        }
        return ExtMatch.NOT_MATCH;
    }

    @Override
    public String defaultFileExtension() {
        return "rb";
    }

    @Override
    public synchronized BaseLanguage<?, ?> getLanguage(Core<?, ?> runner) {
        if (languageDefinition == null) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(JRubyExtension.class.getClassLoader());
            try {
                languageDefinition = new JRubyLanguageDefinition(this, runner);
            } finally {
                Thread.currentThread().setContextClassLoader(classLoader);
            }
        }
        return languageDefinition;
    }

    @Override
    public Set<Class<? extends BaseLibrary>> getLibraries() {
        return Sets.newHashSet(FWrapper.class);
    }

    @Override
    public BaseWrappedException<?> wrapException(Throwable ex) {
        if (!(ex instanceof EvalFailedException)) return null;
        Throwable cause = ex.getCause();
        if (cause instanceof RaiseException) {
            RubyException e = ((RaiseException) cause).getException();
            StackTraceElement[] frames = Arrays.stream(e.getBacktraceElements())
                    .map(RubyStackTraceElement::asStackTraceElement)
                    .toArray(StackTraceElement[]::new);
            return new BaseWrappedException<>(e, e.getMessageAsJavaString(), null, buildTrace(frames));
        }
        return new BaseWrappedException<>(cause, cause.getClass().getName() + ": " + cause.getMessage(), null, buildTrace(cause.getStackTrace()));
    }

    private BaseWrappedException<StackTraceElement> buildTrace(StackTraceElement[] frames) {
        BaseWrappedException<StackTraceElement> head = null;
        for (int i = frames.length - 1; i >= 0; i--) {
            StackTraceElement frame = frames[i];
            String cls = frame.getClassName();
            if ("org.jruby.embed.internal.EmbedEvalUnitImpl".equals(cls)) {
                // upstream ran here — discard everything we've accumulated above it in the chain
                head = null;
                continue;
            }
            if (cls.startsWith("org.jruby")) continue;
            BaseWrappedException.SourceLocation loc;
            if ("RUBY".equals(cls)) {
                String fileName = frame.getFileName();
                loc = new BaseWrappedException.GuestLocation(
                        fileName != null ? new File(fileName) : null,
                        -1, -1, frame.getLineNumber(), -1);
            } else {
                loc = new BaseWrappedException.HostLocation(cls + " " + frame.getLineNumber());
            }
            head = new BaseWrappedException<>(frame, frame.getMethodName(), loc, head);
        }
        return head;
    }

    @Override
    public boolean isGuestObject(Object o) {
        return o instanceof IRubyObject;
    }

}
