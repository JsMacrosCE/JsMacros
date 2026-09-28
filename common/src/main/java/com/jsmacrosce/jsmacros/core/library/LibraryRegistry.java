package com.jsmacrosce.jsmacros.core.library;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.language.BaseLanguage;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * the list of libraries JsMacros knows about, and the place that decides which of them a given
 * script run gets.<br>
 * A library is added as a class rather than an instance, and which of the four maps it lands in is
 * worked out here from what it extends, so the choice of superclass is what registers it. Each
 * kind is then handed out differently: the plain ones are built once and shared, the per exec ones
 * are built afresh for every script run, and the per language ones are built once per guest
 * language.<br>
 * This is internal infrastructure. A script never names it and there is no library that hands one
 * over on purpose; the only route to it is to follow the {@code runner} field on any library,
 * which is not a documented way in and is not part of the API. What a script does see is the
 * result: the globals it was handed at the start of its run.
 * example:
 * <pre>
 * // nothing here is called from a script. what a script gets is the libraries
 * // themselves, already built, as globals named by their Library annotation:
 * //   FS  Request  JsMacros  JavaWrapper  Reflection  Time
 * //
 * // so the registry is what put `Request` there, and the way to tell what is
 * // available is to look at what the run was given rather than at the registry.
 * // a library whose constructor is missing or throws does not turn up as a
 * // global at all, and neither does any of the others: the whole lookup fails
 * // rather than skipping it, so the run it was for never gets as far as being
 * // started and dies on the way there.
 * </pre>
 */
public class LibraryRegistry {
    private final Core<?, ?> runner;

    /**
     * the libraries that are built once per profile and shared by every script, keyed by their
     * {@link Library} annotation rather than by the global variable name. The value is the
     * already built instance, so it is the same object for the whole profile.
     */
    public final Map<Library, BaseLibrary> libraries = new LinkedHashMap<>();
    /**
     * the per exec libraries, held as classes rather than as instances, since one is built for
     * each script run. A class lands here because it extended {@link PerExecLibrary}, and the
     * constructor it is built with is the one taking a
     * {@link com.jsmacrosce.jsmacros.core.language.BaseScriptContext BaseScriptContext}.
     */
    public final Map<Library, Class<? extends PerExecLibrary>> perExec = new LinkedHashMap<>();
    /**
     * the per language libraries, built once per guest language. The outer key is the language
     * class, so a library registered for two languages is built once for each of them and each
     * build only goes to scripts of that language.
     */
    public final Map<Class<? extends BaseLanguage<?, ?>>, Map<Library, PerLanguageLibrary>> perLanguage = new LinkedHashMap<>();
    /**
     * the per exec, per language libraries, which are the per exec form of the map above and are
     * likewise held as classes since one is built per script run. They are built with the run's
     * context and the language class, in that order.
     */
    public final Map<Class<? extends BaseLanguage<?, ?>>, Map<Library, Class<? extends PerExecLanguageLibrary<?, ?>>>> perExecLanguage = new LinkedHashMap<>();

    public LibraryRegistry(Core<?, ?> runner) {
        this.runner = runner;
    }

    /**
     * everything a script run is given: the once-only libraries first, then the per exec ones,
     * both narrowed down to those that apply to this language. The per exec libraries are built by
     * this call, so a script gets a fresh set of them on every run, and this map is what is bound
     * into the guest language as the globals.
     *
     * @param language the language the script is being run in
     * @param context  the run, handed to each per exec library as it is built
     * @return a map from the global variable name, which is the {@link Library} value, to the
     * built library
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
     * the libraries that are built once and shared: the ones registered against no language at
     * all, the ones registered against this one, and the per language ones whose language this
     * one is a kind of.<br>
     * A library registered for no language in particular is the one that reaches every guest
     * language, which is where the core libraries a script cannot do without come from. The per
     * exec libraries are not here; they are built per run and come from
     * {@link #getPerExecLibraries(com.jsmacrosce.jsmacros.core.language.BaseLanguage, com.jsmacrosce.jsmacros.core.language.BaseScriptContext) getPerExecLibraries}.
     *
     * @param language the language the libraries are being asked for
     * @return a map from the global variable name to the already built library
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
     * the libraries that are built fresh for this run. A new instance of every applicable
     * {@link PerExecLibrary} is made through its single argument context constructor, and a new
     * one of every applicable
     * {@link com.jsmacrosce.jsmacros.core.library.PerExecLanguageLibrary PerExecLanguageLibrary}
     * through its two argument context and language constructor.<br>
     * Those constructors are reached reflectively, so a class that has no such constructor, has a
     * private one, or throws while building is not a library that gets quietly skipped: the whole
     * lookup fails with a {@link RuntimeException} saying the library could not be instantiated,
     * carrying the original failure as its cause.
     *
     * @param language the language the libraries are being asked for
     * @param context  the run, handed to each library as it is built
     * @return a map from the global variable name to the newly built library
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
     * registers a library class, building it now if this is a kind that is built on registration.
     * The kind is worked out from what the class extends rather than from what was asked for: a
     * {@link PerExecLibrary} or a
     * {@link com.jsmacrosce.jsmacros.core.library.PerExecLanguageLibrary PerExecLanguageLibrary}
     * is only filed away and built later, per run; a {@link PerLanguageLibrary} is built once for
     * each of the languages it was registered for; and anything else is built once and shared.<br>
     * The class has to carry a {@link Library} annotation, since that is where the global variable
     * name comes from, and that annotation is the whole of what identifies it. A class without one
     * is not a library, and saying so is all that happens: a {@link RuntimeException} is thrown and
     * nothing is registered.<br>
     * The kinds that are built here are built through their constructors reflectively, with a
     * {@link Core} and a language class for the per language one, so a class missing that
     * constructor is not skipped either but fails the registration outright.<br>
     * This is what {@code Reflection.createLibraryBuilder} calls when a script finishes building a
     * library, and what the profile calls for each of the built in ones. It is synchronized,
     * because a library can be registered while scripts are running.
     *
     * @param clazz the library class to register
     * @throws RuntimeException if the class carries no {@link Library} annotation, or if one of
     *                          the constructors needed to build it is missing, is not public, or
     *                          throws
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
