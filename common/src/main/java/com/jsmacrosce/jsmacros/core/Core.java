package com.jsmacrosce.jsmacros.core;

import org.slf4j.Logger;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.SynchronizedWeakHashSet;
import com.jsmacrosce.jsmacros.core.config.BaseProfile;
import com.jsmacrosce.jsmacros.core.config.ConfigManager;
import com.jsmacrosce.jsmacros.core.config.CoreConfigV2;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.BaseEventRegistry;
import com.jsmacrosce.jsmacros.core.extensions.ExtensionLoader;
import com.jsmacrosce.jsmacros.core.extensions.LanguageExtension;
import com.jsmacrosce.jsmacros.core.helper.ClassWrapperTree;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;
import com.jsmacrosce.jsmacros.core.language.BaseWrappedException;
import com.jsmacrosce.jsmacros.core.language.EventContainer;
import com.jsmacrosce.jsmacros.core.library.LibraryRegistry;
import com.jsmacrosce.jsmacros.core.library.ScriptTypeRegistry;
import com.jsmacrosce.jsmacros.core.service.ServiceManager;
import com.jsmacrosce.jsmacros.core.threads.JsMacrosThreadPool;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Connects a runtime's profile, configuration, event and library registries, extensions, services, and script execution facilities.
 * <pre>
 * const Client = Java.type('com.jsmacrosce.jsmacros.client.JsMacrosClient');
 * const core = Client.clientCore;
 * Chat.log(core.profile.getCurrentProfileName());
 * </pre>
 * @param <T> profile implementation type
 * @param <U> event registry factory result type
 */
public class Core<T extends BaseProfile, U extends BaseEventRegistry> {
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private final Set<BaseScriptContext<?>> contexts = new SynchronizedWeakHashSet<>();

    /** Maps Java object types to helper wrappers; initially, unmatched objects use an {@code Object} fallback returning {@code null}. */
    public final ClassWrapperTree<Object, BaseHelper<?>> helperRegistry = new ClassWrapperTree<>(Object.class, a -> null);
    /** Registry of scripting libraries associated with this core. */
    public final LibraryRegistry libraryRegistry = new LibraryRegistry(this);
    /** Share the list of Java types, but let each script create its own language-specific values. */
    @DocletIgnore
    public final ScriptTypeRegistry scriptTypeRegistry = new ScriptTypeRegistry();
    /** Event registry supplied by the event-registry factory. */
    public final BaseEventRegistry eventRegistry;

    /** Extension loader associated with this core; extensions are not necessarily loaded during construction. */
    public final ExtensionLoader extensions;

    /** Profile instance returned by the profile factory. */
    public final T profile;
    /** Configuration manager associated with this core. */
    public final ConfigManager config;
    /** Service manager associated with this core. */
    public final ServiceManager services;

    /** Worker pool used for scripts associated with this core. */
    public final JsMacrosThreadPool threadPool = new JsMacrosThreadPool();

    /**
     * Creates the event registry, loads configuration, creates the profile and managers, then calls profile initialization with the configured default name and loads configured services, starting enabled entries.
     * The event-registry factory runs first and receives this partially initialized core; the profile factory runs after configuration setup but before the extension loader and service manager are assigned.
     * @param eventRegistryFunction factory that creates the event registry from this core
     * @param profileFunction factory that creates the profile from this core and logger
     * @param configFolder configuration directory
     * @param macroFolder script directory
     * @param logger diagnostics logger
     */
    public Core(Function<Core<T, U>, U> eventRegistryFunction, BiFunction<Core<T, U>, Logger, T> profileFunction, File configFolder, File macroFolder, Logger logger) {
        eventRegistry = eventRegistryFunction.apply(this);
        config = new ConfigManager(this, configFolder, macroFolder, logger);
        profile = profileFunction.apply(this, logger);

        extensions = new ExtensionLoader(this);
        this.services = new ServiceManager(this);
        profile.init(config.getOptions(CoreConfigV2.class).defaultProfile);
        services.load();
    }

    /**
     * Adds the event container's context to the weakly tracked set.
     *
     * @param container event container whose context is tracked
     */
    public void addContext(EventContainer<?> container) {
        contexts.add(container.getCtx());
    }

    /**
     * Returns the tracked script contexts.
     *
     * @return the live set, which holds contexts weakly and does not keep them alive
     */
    public Set<BaseScriptContext<?>> getContexts() {
        return contexts;
    }

    /**
     * Starts the trigger's script for an event and returns its event container.
     *
     * @param macro script trigger to execute
     * @param event event supplied to the script
     * @return the event container for the script execution
     */
    public EventContainer<?> exec(ScriptTrigger macro, BaseEvent event) {
        return exec(macro, event, null, null);
    }

    /**
     * Starts the trigger's script and returns its event container, with optional completion and failure callbacks.
     *
     * @param macro script trigger to execute
     * @param event event supplied to the script
     * @param then callback run after successful script execution
     * @param catcher callback given an execution failure
     * @return the event container for the script execution
     */
    public EventContainer<?> exec(ScriptTrigger macro, BaseEvent event, Runnable then,
                                  Consumer<Throwable> catcher) {

        final File file;
        if (macro.scriptFile.isAbsolute()) {
            file = macro.scriptFile.toFile();
        } else {
            file = this.config.macroFolder.toPath().resolve(macro.scriptFile).toFile();
        }
        LanguageExtension l = extensions.getExtensionForFile(file);
        if (l == null) {
            l = extensions.getHighestPriorityExtension();
        }
        return l.getLanguage(this).trigger(macro, event, then, catcher);
    }

    /**
     * Starts source execution, selecting its language from {@code fakeFile}'s extension when supplied, or from {@code lang} otherwise.
     * @param lang language identifier or extension hint
     * @param script source text
     * @param fakeFile optional file used to select a language extension and as the script file; may be {@code null}
     * @param event event supplied to the script
     * @param then callback run after successful script execution
     * @param catcher callback given an execution failure
     * @return the event container for the script execution
     * @since 1.7.0
     */
    public EventContainer<?> exec(String lang, String script, File fakeFile, BaseEvent event, Runnable then, Consumer<Throwable> catcher) {
        LanguageExtension l = extensions.getExtensionForFile(fakeFile != null ? fakeFile : new File(lang.startsWith(".") ? lang : "." + lang));
        assert l != null;
        return l.getLanguage(this).trigger(lang, script, fakeFile, event, then, catcher);
    }

    /**
     * Wraps an exception with the first language-specific wrapper available, or as a host-stack exception otherwise.
     *
     * @param ex exception to wrap
     * @return the wrapped exception, or {@code null} when {@code ex} is {@code null}
     */
    public BaseWrappedException<?> wrapException(Throwable ex) {
        if (ex == null) {
            return null;
        }
        for (LanguageExtension lang : extensions.getAllLanguageExtensions()) {
            BaseWrappedException<?> e = lang.wrapException(ex);
            if (e != null) {
                return e;
            }
        }
        Iterator<StackTraceElement> elements = Arrays.stream(ex.getStackTrace()).iterator();
        String message = ex.getClass().getSimpleName();
        String intMessage = ex.getMessage();
        if (intMessage != null) {
            message += ": " + intMessage;
        }
        return new BaseWrappedException<>(ex, message, null, elements.hasNext() ? wrapHostInternal(elements.next(), elements) : null);
    }

    private BaseWrappedException<StackTraceElement> wrapHostInternal(StackTraceElement e, Iterator<StackTraceElement> elements) {
        return BaseWrappedException.wrapHostElement(e, elements.hasNext() ? wrapHostInternal(elements.next(), elements) : null);
    }

    /**
     * Registers a helper wrapper for objects of the specified Java type; the wrapper is constructed when the registered mapping is used. A missing or inaccessible matching constructor causes {@code RuntimeException} during registration. Failures invoking the constructor are wrapped in {@code RuntimeException} when the mapping is used.
     * @param <E> input Java object type
     * @param <R> helper type for {@code E}
     * @param type exact constructor parameter type accepted by the wrapper
     * @param wrapper helper class with an accessible constructor taking {@code type}
     */
    public <E, R extends BaseHelper<E>> void registerHelper(Class<E> type, Class<R> wrapper) {
        try {
            MethodHandle mh = lookup.findConstructor(wrapper, MethodType.methodType(void.class, type));
            MethodHandle exact = MethodHandles.explicitCastArguments(mh, MethodType.methodType(BaseHelper.class, Object.class));
            helperRegistry.registerType(type, t -> {
                try {
                    return (BaseHelper<?>) exact.invokeExact(t);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
