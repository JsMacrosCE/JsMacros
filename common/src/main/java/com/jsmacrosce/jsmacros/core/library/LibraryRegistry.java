package com.jsmacrosce.jsmacros.core.library;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.language.BaseLanguage;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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
     * The library class that registered each script-global name, used to report a name that more
     * than one library provides.
     */
    private final Map<String, Class<? extends BaseLibrary>> globalOwners = new LinkedHashMap<>();

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
            if (!languageEntry.getKey().isAssignableFrom(language.getClass())) {
                continue;
            }
            for (Map.Entry<Library, Class<? extends PerExecLanguageLibrary<?, ?>>> lib : languageEntry.getValue().entrySet()) {
                try {
                    libs.put(lib.getKey().value(), instantiatePerExecLanguage(lib.getValue(), context, language.getClass()));
                } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                         InvocationTargetException e) {
                    throw new RuntimeException("Failed to instantiate library, ", e);
                }
            }
        }

        return libs;
    }

    /**
     * Constructs a per-execution, per-language library for one run.
     * <p>
     * The constructor is looked up by the run's context class first. A generated library class
     * is defined by the core's class loader, so it declares the context's base type rather than
     * the concrete type; a constructor whose first parameter accepts the context is used when no
     * exact match exists.
     *
     * @param clazz        the library class to construct
     * @param context      the run context to construct it with
     * @param languageClass the language class to construct it with
     * @return the constructed library
     * @throws NoSuchMethodException    if the class has no usable constructor
     * @throws InstantiationException   if the class cannot be instantiated
     * @throws IllegalAccessException   if the constructor is not accessible
     * @throws InvocationTargetException if the constructor throws
     */
    private static <T extends PerExecLanguageLibrary<?, ?>> T instantiatePerExecLanguage(Class<T> clazz, BaseScriptContext<?> context, Class<?> languageClass) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        try {
            return clazz.getConstructor(context.getClass(), Class.class).newInstance(context, languageClass);
        } catch (NoSuchMethodException e) {
            for (Constructor<?> candidate : clazz.getConstructors()) {
                Class<?>[] parameters = candidate.getParameterTypes();
                if (parameters.length == 2 && parameters[0].isAssignableFrom(context.getClass()) && parameters[1] == Class.class) {
                    return clazz.cast(candidate.newInstance(context, languageClass));
                }
            }
            throw e;
        }
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
        addLibrary(clazz, List.of());
    }

    /**
     * Registers a library class that is restricted to the given languages.
     * <p>
     * The languages are supplied separately rather than only through the {@link Library}
     * annotation because a generated library class is defined by the core's class loader, while
     * the language it is restricted to may live in an extension's loader. A class reference in
     * an annotation is resolved against the loader that defined the annotated class, so such a
     * reference could not be read back. The supplied languages take precedence over the ones in
     * the annotation.
     *
     * @param clazz     the library class to register
     * @param languages the languages the library applies to; empty means the annotation decides
     * @throws RuntimeException if the class lacks a {@link Library} annotation or immediate
     *                          construction fails
     */
    public synchronized void addLibrary(Class<? extends BaseLibrary> clazz, List<Class<? extends BaseLanguage<?, ?>>> languages) {
        if (!clazz.isAnnotationPresent(Library.class)) {
            throw new RuntimeException("Tried to add library that doesn't have a proper library annotation");
        }
        Library ann = clazz.getAnnotation(Library.class);
        List<Class<? extends BaseLanguage<?, ?>>> applicable = languages.isEmpty() ? Arrays.asList(ann.languages()) : languages;

        Class<? extends BaseLibrary> previous = globalOwners.putIfAbsent(ann.value(), clazz);
        if (previous != null && previous != clazz) {
            runner.profile.LOGGER.warn("Library global '{}' is provided by both {} and {}; the last one registered is used",
                    ann.value(), previous.getName(), clazz.getName());
        }

        if (PerExecLibrary.class.isAssignableFrom(clazz)) {
            perExec.put(ann, clazz.asSubclass(PerExecLibrary.class));
        } else if (PerExecLanguageLibrary.class.isAssignableFrom(clazz)) {
            for (Class<? extends BaseLanguage<?, ?>> lang : applicable) {
                perExecLanguage.computeIfAbsent(lang, k -> new LinkedHashMap<>())
                        .put(ann, (Class<? extends PerExecLanguageLibrary<?, ?>>) clazz);
            }
        } else if (PerLanguageLibrary.class.isAssignableFrom(clazz)) {
            for (Class<? extends BaseLanguage<?, ?>> lang : applicable) {
                perLanguage.computeIfAbsent(lang, k -> new LinkedHashMap<>());
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
    }

}
