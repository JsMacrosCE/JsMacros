package com.jsmacrosce.jsmacros.core.library;

import com.jsmacrosce.jsmacros.core.Core;

/**
 * the superclass of every JsMacros library, and the type a library is held as once it has been
 * registered.<br>
 * A library is a plain Java class that JsMacros instantiates and hands to a script as a global
 * variable, named by the {@link Library} annotation on it. {@code FS}, {@code JsMacros},
 * {@code Request} and the rest are all instances of this class, which means a script holds one of
 * these without ever naming the type.<br>
 * There is no behaviour here. The class exists so that the registry has one type to hold every
 * library as, and so that every library is handed the {@link Core} it was built for. Which
 * subclass a library extends is the part that matters, since that decides how long it lives and
 * what it is handed when it is made.<br>
 * A library extending {@link PerExecLibrary} is built once per script run and is given that
 * run's {@link com.jsmacrosce.jsmacros.core.language.BaseScriptContext BaseScriptContext}, so it
 * can keep per run state. That is what {@code FS} and {@code Reflection} are. One extending
 * {@link PerLanguageLibrary} is built once per guest language for the whole profile and is given
 * its language class, and one extending
 * {@link com.jsmacrosce.jsmacros.core.library.PerExecLanguageLibrary PerExecLanguageLibrary} is
 * the per exec form of that, so it is given both. A class extending none of the three is built
 * once per profile and is only given the {@link Core}, which is the case for {@code JsMacros} and
 * {@code Request} and the shape to use for a library that holds nothing per run.<br>
 * This is also the class a script generates at run time, since
 * {@code Reflection.createLibraryBuilder} assembles a subclass of one of these. Nothing here is
 * meant to be subclassed from a script directly; that is what the builder is for.
 * example:
 * <pre>
 * // there is no runnable example here, because this class is abstract and
 * // a script never names it. what a script actually holds is an instance of
 * // one of the subclasses, reached as the global its Library annotation
 * // names, and there is no constructor call to write:
 * //   FS  Request  JsMacros  JavaWrapper  Reflection  Time
 * //
 * // so `Request` is an instance of a subclass of this class, and the type of
 * // that global is what the shipped typings give it. reaching for the type
 * // itself is not a thing a script does, and `Reflection.createLibraryBuilder`
 * // is the way a subclass gets made at run time rather than by extending this
 * // one directly.
 * </pre>
 */
public abstract class BaseLibrary {
    /**
     * the {@link Core} this library was built for, handed to it in the constructor and the way
     * back to the profile, the config, the event registry and the {@link LibraryRegistry}.<br>
     * It is a public field rather than a getter, and it is not private to the library, so a
     * library that reaches for anything on the core does so through here. A script can read it off
     * any library it has been given, which is a way into the rest of the mod that is not meant to
     * be part of the API and is not documented anywhere else.
     */
    public Core<?, ?> runner;

    public BaseLibrary(Core<?, ?> runner) {
        this.runner = runner;
    }

}
