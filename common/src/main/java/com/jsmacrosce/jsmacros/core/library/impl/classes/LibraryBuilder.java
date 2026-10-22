package com.jsmacrosce.jsmacros.core.library.impl.classes;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.extensions.Extension;
import com.jsmacrosce.jsmacros.core.extensions.LanguageExtension;
import com.jsmacrosce.jsmacros.core.language.BaseLanguage;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;
import com.jsmacrosce.jsmacros.core.library.*;

import java.util.ArrayList;
import java.util.List;

/**
 * a {@link ClassBuilder} that assembles a library rather than an ordinary class: a script uses it
 * to write a new global variable for itself in Java at run time.<br>
 * A script gets one from {@code Reflection.createLibraryBuilder} and it works the way a
 * {@code ClassBuilder} does, with one difference and one job of its own. The difference is the
 * constructor: {@link #addConstructor() addConstructor} here takes no arguments and works the
 * parameter list out from the {@code perExec} and language settings, because the shape of the
 * constructor is what decides how the library is built and what it is handed. The job is
 * {@link #finishBuildAndFreeze() finishBuildAndFreeze}, which registers the finished class so
 * that scripts get the global, rather than only handing the class back.<br>
 * The superclass is chosen here from the same two settings, so the generated class extends
 * {@link BaseLibrary}, or one of the per exec and per language forms of it, which is what makes
 * the registration work. The name given is the name of the global rather than the name of the
 * class, and it is also written onto the class as its {@link Library} value, so the class is
 * generated under that name inside JsMacros' own package. See
 * {@code Reflection.getClassFromClassBuilderResult} for looking the class up by that name
 * afterwards.<br>
 * A language named that does not exist, or that is not a language extension, is refused here with
 * an {@link IllegalArgumentException} rather than being ignored, so a typo in a language name
 * fails the build instead of quietly producing a library no script can reach.<br>
 * One thing to know about the result: the globals a run is given are bound when the run starts,
 * so a library registered part way through a script is not a global in that script. It is there
 * for the runs after it.
 * example:
 * <pre>
 * // a library named `Greetings`, built afresh for every script run
 * const builder = Reflection.createLibraryBuilder("Greetings", true);
 *
 * builder.addField("private String greeting;");
 *
 * // the constructor takes no arguments here: perExec decides that it is
 * // handed the run's context, and the body has to pass that on
 * builder.addConstructor()
 *   .makePublic()
 *   .body('super($1); this.greeting = "hello";');
 *
 * builder.addMethod("public String greet(String name) { return this.greeting + \", \" + name; }");
 *
 * // nothing exists until this, and the class is frozen by it
 * builder.finishBuildAndFreeze();
 *
 * // `Greetings` is bound when a run starts, so it is the runs after this one
 * // that can reach it
 * print("built. run this again and Greetings will be here");
 * </pre>
 * @author Wagyourtail
 * @since 1.6.5
 */
public class LibraryBuilder extends ClassBuilder<BaseLibrary> {
    final Core<?, ?> runner;
    final boolean languages;
    final boolean perExec;
    boolean hasConstructorSet = false;

    public LibraryBuilder(Core<?, ?> runner, String name, boolean perExec, String... allowedLangs) throws NotFoundException, CannotCompileException {
        super(name, (Class<BaseLibrary>) (perExec ? (allowedLangs.length > 0 ? PerExecLanguageLibrary.class : PerExecLibrary.class) : (allowedLangs.length > 0 ?
                PerLanguageLibrary.class : BaseLibrary.class)));
        this.runner = runner;
        AnnotationBuilder b = this.addAnnotation(Library.class).putString("value", name);
        List<Class<?>> allowed = new ArrayList<>();
        for (int i = 0; i < allowedLangs.length; i++) {
            Extension ext = runner.extensions.getExtensionForName(allowedLangs[i]);
            if (ext instanceof LanguageExtension l) {
                allowed.add(l.getLanguage(runner).getClass());
            } else {
                throw new IllegalArgumentException("Language not found: " + allowedLangs[i]);
            }
        }
        AnnotationBuilder.AnnotationArrayBuilder ab = b.putArray("allowedLanguages");
        for (Class<?> c : allowed) {
            ab.putClass(c);
        }
        ab.finish();
        b.finish();
        this.perExec = perExec;
        languages = allowedLangs.length > 0;
    }

    /**
     * constructor, if perExec run every context, if per language run once for each lang;
     * params are context and language class.
     * if not per exec, param will be skipped.
     * ie:
     * BaseLibrary: no params
     * PerExecLibrary: context
     * PerExecLanguageLibrary: context, language
     * PerLanguageLibrary: language
     * <p>
     * Don't do other constructors...
     * <br>
     * This takes no arguments on purpose, since the parameter list is not the script's to write:
     * it is worked out from whether the library is per exec and whether it was given any
     * languages, and it has to match the superclass this builder chose or the library cannot be
     * built. The result is made public here, so a body given to it does not have to say so.<br>
     * A body is still the script's to write, and it has to pass the parameters on, so a per exec
     * library's body starts with a {@code super($1)} and a per exec, per language one with a
     * {@code super($1, $2)}. A body that skips that is a class that does not compile, which is
     * reported when {@link #finishBuildAndFreeze() finishBuildAndFreeze} is called.<br>
     * The argument-less call on {@link ClassBuilder} is not reachable from here, since this
     * overload shadows it with no parameters.
     * example:
     * <pre>
     * // a per exec library is handed the run's context, so the body takes it
     * // and passes it on
     * builder.addConstructor()
     *   .body('super($1); this.calls = 0;');
     *
     * // a once-only library is handed nothing, so its body takes nothing
     * const onceOnly = Reflection.createLibraryBuilder("Counter", false);
     * onceOnly.addConstructor()
     *   .body('super(); this.calls = 0;');
     * </pre>
     *
     * @return a builder for the constructor, which is public and has a body still to be given
     * @throws NotFoundException if a parameter class could not be resolved
     */
    public ConstructorBuilder addConstructor() throws NotFoundException {
        hasConstructorSet = true;
        List<Class<?>> params = new ArrayList<>();
        if (perExec) {
            params.add(BaseScriptContext.class);
        }
        if (languages) {
            params.add(BaseLanguage.class);
        }
        ConstructorBuilder cb = addConstructor(params.toArray(new Class<?>[0]));
        cb.makePublic();
        return cb;
    }

    /**
     * builds the class, freezes it, and registers it so scripts get a global for it.<br>
     * This is where a library stops being an assembly of pieces and becomes something that can
     * be instantiated, and it does two things over a {@code ClassBuilder}'s own
     * {@code finishBuildAndFreeze}: it generates the constructor when the script has not written
     * one, and it hands the finished class to the {@link LibraryRegistry} rather than only
     * returning it. The generated constructor is the plain one that passes the parameters on to
     * the superclass, so a library that only needs the default is written without a body at all.
     * <br>
     * A class is frozen by this, so it can only be built once, and building it twice fails on
     * the freeze rather than producing a second class. The registration is what makes the
     * library real: a class that is never registered is a class nothing can reach. What the
     * registration cannot do is appear in the run that made it, since a run's globals are bound
     * when it starts, so the global is there for the runs after this one.
     * example:
     * <pre>
     * // this is the call that registers it, and it can only be made once
     * const built = builder.finishBuildAndFreeze();
     * print(`built ${built.getName()}`);
     *
     * // the class is in JsMacros' own package under the global's name, so it
     * // can be looked up afterwards
     * const same = Reflection.getClassFromClassBuilderResult("Greetings");
     * print(same === built);
     * </pre>
     *
     * @return the generated library class, which is also now registered
     * @throws CannotCompileException if the assembled class does not compile
     * @throws NotFoundException      if a class named while building could not be resolved
     */
    @Override
    public Class<? extends BaseLibrary> finishBuildAndFreeze() throws CannotCompileException, NotFoundException {
        if (!hasConstructorSet) {
            ConstructorBuilder cb = addConstructor();
            StringBuilder body = new StringBuilder("{super(");
            for (int i = 0; i < cb.params.length; i++) {
                if (i > 0) {
                    body.append(", ");
                }
                body.append("$").append(i);
            }
            body.append(");}");
            cb.body(body.toString());
        }
        Class<? extends BaseLibrary> clazz = super.finishBuildAndFreeze();
        runner.libraryRegistry.addLibrary(clazz);
        return clazz;
    }

}
