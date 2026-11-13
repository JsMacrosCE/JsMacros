package com.jsmacrosce.jsmacros.core.library;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.language.BaseLanguage;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;
import com.jsmacrosce.jsmacros.core.language.EventContainer;
import com.jsmacrosce.jsmacros.test.stubs.CoreInstanceCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that languages passed to {@link LibraryRegistry#addLibrary(Class, List)} restrict
 * shared and per-execution libraries, which are stored without a language key. Generated
 * libraries are registered through that overload, so ignoring the list would expose them to
 * every language despite the requested restriction.
 */
class LibraryRegistryRestrictionTest {

    @TempDir
    Path tempDir;

    @Test
    void sharedLibraryUsesExplicitLanguageRestriction() {
        Core<?, ?> core = CoreInstanceCreator.createIsolatedCore(tempDir.toFile());
        ProbeLanguageA allowed = new ProbeLanguageA(core);
        ProbeLanguageB other = new ProbeLanguageB(core);

        core.libraryRegistry.addLibrary(SharedProbe.class, List.of(ProbeLanguageA.class));

        assertTrue(core.libraryRegistry.getOnceLibraries(allowed).containsKey("ExplicitSharedProbe"));
        assertFalse(core.libraryRegistry.getOnceLibraries(other).containsKey("ExplicitSharedProbe"));
    }

    @Test
    void perExecutionLibraryUsesExplicitLanguageRestriction() {
        Core<?, ?> core = CoreInstanceCreator.createIsolatedCore(tempDir.toFile());
        ProbeLanguageA allowed = new ProbeLanguageA(core);
        ProbeLanguageB other = new ProbeLanguageB(core);
        BaseScriptContext<Object> context = new BaseScriptContext<>(core, null, null) {
            @Override
            public boolean isMultiThreaded() {
                return false;
            }
        };

        core.libraryRegistry.addLibrary(PerExecProbe.class, List.of(ProbeLanguageA.class));

        assertTrue(core.libraryRegistry.getPerExecLibraries(allowed, context).containsKey("ExplicitPerExecProbe"));
        assertFalse(core.libraryRegistry.getPerExecLibraries(other, context).containsKey("ExplicitPerExecProbe"));
    }

    @Test
    void explicitLanguageListOverridesARestrictiveAnnotation() {
        Core<?, ?> core = CoreInstanceCreator.createIsolatedCore(tempDir.toFile());
        ProbeLanguageA allowed = new ProbeLanguageA(core);
        ProbeLanguageB annotated = new ProbeLanguageB(core);

        core.libraryRegistry.addLibrary(AnnotatedProbe.class, List.of(ProbeLanguageA.class));

        assertTrue(core.libraryRegistry.getOnceLibraries(allowed).containsKey("ExplicitOverrideProbe"));
        assertFalse(core.libraryRegistry.getOnceLibraries(annotated).containsKey("ExplicitOverrideProbe"));
    }

    @Library("ExplicitSharedProbe")
    public static final class SharedProbe extends BaseLibrary {
        public SharedProbe(Core<?, ?> core) {
            super(core);
        }
    }

    @Library(value = "ExplicitOverrideProbe", languages = ProbeLanguageB.class)
    public static final class AnnotatedProbe extends BaseLibrary {
        public AnnotatedProbe(Core<?, ?> core) {
            super(core);
        }
    }

    @Test
    void batchRegistrationRestoresEarlierStateWhenOneLibraryFails() {
        Core<?, ?> core = CoreInstanceCreator.createIsolatedCore(tempDir.toFile());
        ProbeLanguageA language = new ProbeLanguageA(core);
        core.libraryRegistry.addLibrary(BaselineProbe.class);

        assertThrows(RuntimeException.class,
                () -> core.libraryRegistry.addLibraries(List.of(BatchGoodProbe.class, BatchBadProbe.class)));

        assertTrue(core.libraryRegistry.getOnceLibraries(language).containsKey("BaselineProbe"),
                "libraries registered before the batch must survive");
        assertFalse(core.libraryRegistry.getOnceLibraries(language).containsKey("BatchGoodProbe"),
                "the successful part of the failed batch must be rolled back");
    }

    @Library("BaselineProbe")
    public static final class BaselineProbe extends BaseLibrary {
        public BaselineProbe(Core<?, ?> core) {
            super(core);
        }
    }

    @Library("BatchGoodProbe")
    public static final class BatchGoodProbe extends BaseLibrary {
        public BatchGoodProbe(Core<?, ?> core) {
            super(core);
        }
    }

    @Library("BatchBadProbe")
    public static final class BatchBadProbe extends BaseLibrary {
        public BatchBadProbe(Core<?, ?> core) {
            super(core);
            throw new IllegalStateException("batch member failed on purpose");
        }
    }

    @Library("ExplicitPerExecProbe")
    public static final class PerExecProbe extends PerExecLibrary {
        public PerExecProbe(BaseScriptContext<?> context) {
            super(context);
        }
    }

    private abstract static class ProbeLanguage extends BaseLanguage<Object, BaseScriptContext<Object>> {
        ProbeLanguage(Core<?, ?> core) {
            super(null, core);
        }

        @Override
        protected void exec(EventContainer<BaseScriptContext<Object>> ctx, ScriptTrigger macro, BaseEvent event) {
        }

        @Override
        protected void exec(EventContainer<BaseScriptContext<Object>> ctx, String lang, String script, BaseEvent event) {
        }

        @Override
        public BaseScriptContext<Object> createContext(BaseEvent event, File file) {
            return null;
        }
    }

    private static final class ProbeLanguageA extends ProbeLanguage {
        ProbeLanguageA(Core<?, ?> core) {
            super(core);
        }
    }

    private static final class ProbeLanguageB extends ProbeLanguage {
        ProbeLanguageB(Core<?, ?> core) {
            super(core);
        }
    }
}
