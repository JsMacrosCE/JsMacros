package com.jsmacrosce.jsmacros.graal;

import com.google.common.collect.Sets;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.SourceSection;
import org.graalvm.polyglot.Value;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.extensions.LanguageExtension;
import com.jsmacrosce.jsmacros.core.extensions.LibraryExtension;
import com.jsmacrosce.jsmacros.core.language.BaseLanguage;
import com.jsmacrosce.jsmacros.core.language.BaseWrappedException;
import com.jsmacrosce.jsmacros.core.language.ThrowableFormatter;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.graal.language.impl.GraalLanguageDefinition;
import com.jsmacrosce.jsmacros.graal.language.impl.GuestExceptionSimplifier;
import com.jsmacrosce.jsmacros.graal.library.impl.FWrapper;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/** Provides Graal language support and the {@code JavaWrapper} library through the extension loader; it is not a script global.
 * <pre>
 * const extension = context.getCtx().runner.extensions.getExtensionForName("graal");
 * if (extension !== null) {
 *   Chat.log(extension.getExtensionName() + ": " + extension.defaultFileExtension());
 * }
 * </pre>
 */
public class GraalExtension implements LanguageExtension, LibraryExtension {
    private Core<?, ?> runner;
    private static GraalLanguageDefinition languageDefinition;

    /** Returns the loader registration identifier {@code "graal"}.
     * @return this extension's registration identifier
     */
    @Override
    public String getExtensionName() {
        return "graal";
    }

    /** Stores the core and registers this extension's {@code graal} configuration options.
     * The runner is stored before registration; a registration failure is wrapped in {@code RuntimeException} and does not undo that assignment.
     * @param runner core to store and use for configuration registration
     */
    @Override
    public void init(Core<?, ?> runner) {
        this.runner = runner;

        try {
            runner.config.addOptions("graal", GraalConfig.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Returns this language extension's selection priority.
     * @return priority value {@code 1000}; higher priorities are normally preferred, but this does not guarantee selection
     */
    @Override
    public int getPriority() {
        return 1000;
    }

    /** Tests a file using the initialized runner's Graal engine and language detection; a detected language gets a name-preferred match when its basename contains {@code "graal"}.
     * An {@code IOException} from language detection is printed and results in {@code NOT_MATCH}.
     * @param fname candidate file to test
     * @return {@code MATCH_WITH_NAME} for a detected language with {@code "graal"} in its basename, {@code MATCH} for another detected language, or {@code NOT_MATCH} when the engine lacks the detected language or detection throws {@code IOException}
     */
    @Override
    public ExtMatch extensionMatch(File fname) {
        try {
            if (GraalLanguageDefinition.getOrCreateEngine(runner).getLanguages().containsKey(Source.findLanguage(fname))) {
                if (fname.getName().contains(getExtensionName())) {
                    return ExtMatch.MATCH_WITH_NAME;
                }
                return ExtMatch.MATCH;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ExtMatch.NOT_MATCH;
    }

    /** Returns the default file suffix, without a leading period.
     * @return {@code "js"}
     */
    @Override
    public String defaultFileExtension() {
        return "js";
    }

    /** Returns the cached Graal language definition, creating it on the first call.
     * The definition is stored statically across extension instances and uses the core supplied when it is created;
     * a call finding the cache populated ignores its core argument. Creation is unsynchronized, so concurrent calls
     * can construct competing definitions. During creation, the calling thread's context class loader is changed to
     * this extension's loader and restored on the normal path, not in a failure-protected block.
     * @param runner core used if the shared definition has not yet been created
     * @return the cached Graal language definition
     */
    @Override
    public BaseLanguage<?, ?> getLanguage(Core<?, ?> runner) {
        if (languageDefinition == null) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(GraalExtension.class.getClassLoader());
            languageDefinition = new GraalLanguageDefinition(this, runner);
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        return languageDefinition;
    }

    /** Returns this extension's library classes for loader registration.
     * Each call returns a fresh mutable set containing {@code FWrapper.class}.
     * @return fresh set of contributed library classes
     */
    @Override
    public Set<Class<? extends BaseLibrary>> getLibraries() {
        return Sets.newHashSet(FWrapper.class);
    }

    /** Builds a diagnostic wrapper for a Graal polyglot exception; other throwable types are not recognized.
     * Host exceptions use their host class name and optional message; other polyglot exceptions use
     * {@code GuestExceptionSimplifier}, falling back to {@code "UnknownGuestException"} if its message is null.
     * The wrapper retains the original throwable and may include a source position and linked guest/host frames;
     * the frame chain stops at the first host {@code org.graalvm.polyglot.Context.eval} frame.
     * @param ex throwable to inspect
     * @return diagnostic wrapper for a {@code PolyglotException}, or {@code null} otherwise
     */
    @Override
    public BaseWrappedException<?> wrapException(Throwable ex) {
        if (ex instanceof PolyglotException) {
            Iterator<PolyglotException.StackFrame> frames = ((PolyglotException) ex).getPolyglotStackTrace().iterator();
            String message;
            if (((PolyglotException) ex).isHostException()) {
                message = ThrowableFormatter.format(((PolyglotException) ex).asHostException());
            } else {
                message = GuestExceptionSimplifier.simplifyException(ex);
                if (message == null) {
                    message = "UnknownGuestException";
                }
            }
            return new BaseWrappedException<>(ex, message, wrapLocation(((PolyglotException) ex).getSourceLocation()), frames.hasNext() ? internalWrap(frames.next(), frames) : null);
        }
        return null;
    }

    /** Tests whether the object is a Graal {@code Value} instance.
     * This type check does not establish that the value originated in guest code.
     * @param o value to inspect
     * @return {@code true} for a {@code Value} instance; {@code false} otherwise
     */
    @Override
    public boolean isGuestObject(Object o) {
        return o instanceof Value;
    }

    private BaseWrappedException<?> internalWrap(PolyglotException.StackFrame current, Iterator<PolyglotException.StackFrame> frames) {
        if (current == null) {
            return null;
        }
        if (current.isGuestFrame()) {
            return new BaseWrappedException<>(current, " at " + current.getRootName(), wrapLocation(current.getSourceLocation()), frames.hasNext() ? internalWrap(frames.next(), frames) : null);
        }
        if (current.toHostFrame().getClassName().equals("org.graalvm.polyglot.Context") && current.toHostFrame().getMethodName().equals("eval")) {
            return null;
        }
        return BaseWrappedException.wrapHostElement(current.toHostFrame(), frames.hasNext() ? internalWrap(frames.next(), frames) : null);
    }

    private BaseWrappedException.SourceLocation wrapLocation(SourceSection pos) {
        BaseWrappedException.SourceLocation loc = null;
        if (pos != null) {
            if (pos.getSource().getPath() != null) {
                loc = new BaseWrappedException.GuestLocation(new File(pos.getSource().getPath()), pos.getCharIndex(), pos.getCharEndIndex(), pos.getStartLine(), pos.getStartColumn());
            } else {
                loc = new BaseWrappedException.HostLocation(String.format("%s %d:%d", pos.getSource().getName(), pos.getStartLine(), pos.getStartColumn()));
            }
        }
        return loc;
    }

}
