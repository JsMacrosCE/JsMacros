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
 * Builds a library class and registers it for use as a script global. The superclass and
 * constructor parameters depend on whether the library is per execution and whether languages
 * were specified. {@link #finishBuildAndFreeze()} performs the registration; see
 * {@link #addConstructor()} for its generated constructor contract.
 * <pre>
 * const builder = Reflection.createLibraryBuilder("Greetings", true);
 * builder.addConstructor().body("super($1);");
 * builder.finishBuildAndFreeze();
 * </pre>
 * @author Wagyourtail
 * @since 1.6.5
 */
public class LibraryBuilder extends ClassBuilder<BaseLibrary> {
    final Core<?, ?> runner;
    final boolean languages;
    final boolean perExec;
    boolean hasConstructorSet = false;

    /**
     * Configures a generated library class, selecting its superclass and recording its
     * {@link Library} alias and resolved language types. Registration happens in
     * {@link #finishBuildAndFreeze()}, not during construction. A requested extension that is
     * missing or is not a {@link LanguageExtension} causes {@link IllegalArgumentException}.
     *
     * @param runner core providing the extensions used for language resolution
     * @param name generated class name and script-global library alias
     * @param perExec whether the library is per execution rather than shared
     * @param allowedLangs language extension names to allow; an empty array imposes no explicit language restriction
     */
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
     * Adds the library constructor with parameters determined by the builder configuration:
     * context when per-execution, followed by language class when languages were specified.
     * The constructor is public. Its body must invoke the matching superclass constructor;
     * when omitted, {@link #finishBuildAndFreeze()} supplies a body that forwards all parameters.
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
     * Builds and freezes the class, generating a constructor that forwards its parameters if
     * none was added, then registers the class with the library registry.
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
