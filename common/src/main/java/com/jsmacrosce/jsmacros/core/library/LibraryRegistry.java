package com.jsmacrosce.jsmacros.core.library;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.language.BaseLanguage;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registers library classes and supplies applicable instances as globals for script runs.
 * Instances are shared, created per language, per execution, or per execution and language,
 * according to the library superclass.
 */
public class LibraryRegistry {
    private final Core<?, ?> runner;

    /**
     * Libraries instantiated once when registered and then shared, keyed by their
     * {@link Library} annotation.
     */
    public final Map<Library, BaseLibrary> libraries = new LinkedHashMap<>();
    /**
     * Per-execution library classes; each applicable class is instantiated for a script run
     * using a {@link com.jsmacrosce.jsmacros.core.language.BaseScriptContext BaseScriptContext}.
     */
    public final Map<Library, Class<? extends PerExecLibrary>> perExec = new LinkedHashMap<>();
    /**
     * Per-language instances, keyed by the language class used at registration. A language class
     * also receives entries registered for its superclasses.
     */
    public final Map<Class<? extends BaseLanguage<?, ?>>, Map<Library, PerLanguageLibrary>> perLanguage = new LinkedHashMap<>();
    /**
     * Per-execution, per-language library classes, keyed by their registered language class.
     * Applicable classes are instantiated for each run with its context and the language class.
     */
    public final Map<Class<? extends BaseLanguage<?, ?>>, Map<Library, Class<? extends PerExecLanguageLibrary<?, ?>>>> perExecLanguage = new LinkedHashMap<>();

    /**
     * Stores the core used to instantiate shared and per-language libraries during registration.
     *
     * @param runner core used for subsequent library instantiation
     */
    public LibraryRegistry(Core<?, ?> runner) {
        this.runner = runner;
    }

    /**
     * Returns applicable shared and per-execution libraries. Per-execution instances are
     * constructed by this call; shared instances are reused.
     *
     * @param language the language the script is being run in
     * @param context  the run context used to construct per-execution libraries
     * @return libraries keyed by their {@link Library} global names
     * @throws RuntimeException if a per exec library's constructor is missing, is not public, or
     *                          throws
     */
    public Map<String, BaseLibrary> getLibraries(BaseLanguage<?, ?> language, BaseScriptContext<?> context) {
        Map<String, BaseLibrary> libs = new LinkedHashMap<>();
        libs.putAll(getOnceLibraries(language));
        libs.putAll(getPerExecLibraries(language, context));
        return libs;
    }

    /**
     * Returns shared libraries registered for all languages or this exact language, plus
     * per-language instances registered for this language or one of its superclasses.
     *
     * @param language the language the libraries are being asked for
     * @return libraries keyed by their {@link Library} global names
     */
    public Map<String, BaseLibrary> getOnceLibraries(BaseLanguage<?, ?> language) {
        Map<String, BaseLibrary> libs = new LinkedHashMap<>();

        for (Map.Entry<Library, BaseLibrary> lib : libraries.entrySet()) {
            if (lib.getKey().languages().length == 0 || Arrays.stream(lib.getKey().languages()).anyMatch(e -> e.equals(language.getClass()))) {
                libs.put(lib.getKey().value(), lib.getValue());
            }
        }

        for (Map.Entry<Class<? extends BaseLanguage<?, ?>>, Map<Library, PerLanguageLibrary>> languageEntry : perLanguage.entrySet()) {
            if (languageEntry.getKey().isAssignableFrom(language.getClass())) {
                for (Map.Entry<Library, PerLanguageLibrary> lib : languageEntry.getValue().entrySet()) {
                    libs.put(lib.getKey().value(), lib.getValue());
                }
            }
        }

        return libs;
    }

    /**
     * Constructs and returns applicable per-execution libraries. Construction failures are
     * wrapped in a {@link RuntimeException}.
     *
     * @param language the language the libraries are being asked for
     * @param context  the run context used to construct libraries
     * @return libraries keyed by their {@link Library} global names
     * @throws RuntimeException if a registered library's constructor is missing, is not public,
     *                          or throws
     */
    public Map<String, BaseLibrary> getPerExecLibraries(BaseLanguage<?, ?> language, BaseScriptContext<?> context) {
        Map<String, BaseLibrary> libs = new LinkedHashMap<>();

        for (Map.Entry<Library, Class<? extends PerExecLibrary>> lib : perExec.entrySet()) {
            if (lib.getKey().languages().length == 0 || Arrays.stream(lib.getKey().languages()).anyMatch(e -> e.equals(language.getClass()))) {
                try {
                    libs.put(lib.getKey().value(), lib.getValue().getConstructor(BaseScriptContext.class).newInstance(context));
                } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                         InvocationTargetException e) {
                    throw new RuntimeException("Failed to instantiate library, ", e);
                }
            }
        }

        for (Map.Entry<Class<? extends BaseLanguage<?, ?>>, Map<Library, Class<? extends PerExecLanguageLibrary<?, ?>>>> languageEntry : perExecLanguage.entrySet()) {
            if (languageEntry.getKey().isAssignableFrom(language.getClass())) {
                for (Map.Entry<Library, Class<? extends PerExecLanguageLibrary<?, ?>>> lib : languageEntry.getValue().entrySet()) {
                    if (Arrays.stream(lib.getKey().languages()).anyMatch(e -> e.equals(language.getClass()))) {
                        try {
                            libs.put(lib.getKey().value(), lib.getValue().getConstructor(context.getClass(), Class.class).newInstance(context, language.getClass()));
                        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                                 InvocationTargetException e) {
                            throw new RuntimeException("Failed to instantiate library, ", e);
                        }
                    }
                }
            }
        }

        return libs;
    }

    /**
     * Registers a library class, instantiating shared and per-language kinds immediately and
     * storing per-execution kinds for later construction. The superclass determines the kind.
     * Registration requires a {@link Library} annotation; per-language construction uses a
     * {@link Core} and the registered language class, while shared construction uses a {@link Core}.
     * This method is synchronized.
     *
     * @param clazz the library class to register
     * @throws RuntimeException if the class lacks a {@link Library} annotation or immediate
     *                          construction fails
     */
    public synchronized void addLibrary(Class<? extends BaseLibrary> clazz) {
        if (clazz.isAnnotationPresent(Library.class)) {
            Library ann = clazz.getAnnotation(Library.class);
            if (PerExecLibrary.class.isAssignableFrom(clazz)) {
                perExec.put(ann, clazz.asSubclass(PerExecLibrary.class));
            } else if (PerExecLanguageLibrary.class.isAssignableFrom(clazz)) {
                for (Class<? extends BaseLanguage<?, ?>> lang : ann.languages()) {
                    if (!perExecLanguage.containsKey(lang)) {
                        perExecLanguage.put(lang, new LinkedHashMap<>());
                    }
                    perExecLanguage.get(lang).put(ann, (Class<? extends PerExecLanguageLibrary<?, ?>>) clazz);
                }
            } else if (PerLanguageLibrary.class.isAssignableFrom(clazz)) {
                for (Class<? extends BaseLanguage<?, ?>> lang : ann.languages()) {
                    if (!perLanguage.containsKey(lang)) {
                        perLanguage.put(lang, new LinkedHashMap<>());
                    }
                    try {
                        perLanguage.get(lang).put(ann, clazz.asSubclass(PerLanguageLibrary.class).getConstructor(Core.class, Class.class).newInstance(runner, lang));
                    } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                             InvocationTargetException e) {
                        throw new RuntimeException("Failed to instantiate library, ", e);
                    }
                }
            } else {
                try {
                    libraries.put(ann, clazz.getConstructor(Core.class).newInstance(runner));
                } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                    throw new RuntimeException("Failed to instantiate library, ", e);
                }
            }
        } else {
            throw new RuntimeException("Tried to add library that doesn't have a proper library annotation");
        }
    }

}
