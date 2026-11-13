package com.jsmacrosce.jsmacros.core.library;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;
import com.jsmacrosce.jsmacros.core.library.impl.classes.LibraryBuilder;
import com.jsmacrosce.jsmacros.test.stubs.CoreInstanceCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Checks the constructor that a generated library receives when the builder script does not
 * supply one. The builder must forward its own arguments to the superclass; the generated
 * class is unusable otherwise.
 */
class GeneratedLibraryBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void forwardsConstructorArgumentsForSharedLibrary() throws Exception {
        Core<?, ?> core = CoreInstanceCreator.createIsolatedCore(tempDir.toFile());
        LibraryBuilder builder = new LibraryBuilder(core, "GeneratedSharedProbe", false);

        Class<? extends BaseLibrary> type = builder.finishBuildAndFreeze();
        BaseLibrary library = type.getConstructor(Core.class).newInstance(core);

        assertSame(core, library.runner);
    }

    @Test
    void forwardsConstructorArgumentsForPerExecutionLibrary() throws Exception {
        Core<?, ?> core = CoreInstanceCreator.createIsolatedCore(tempDir.toFile());
        LibraryBuilder builder = new LibraryBuilder(core, "GeneratedPerExecProbe", true);

        Class<? extends BaseLibrary> type = builder.finishBuildAndFreeze();

        assertNotNull(type.getConstructor(BaseScriptContext.class));
    }
}
