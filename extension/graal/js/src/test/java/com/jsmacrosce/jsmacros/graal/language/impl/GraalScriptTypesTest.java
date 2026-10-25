package com.jsmacrosce.jsmacros.graal.language.impl;

import com.jsmacrosce.jsmacros.core.library.ScriptNamespace;
import com.jsmacrosce.jsmacros.core.library.ScriptTypeRegistry;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;
import org.graalvm.polyglot.proxy.ProxyObject;

import static org.junit.jupiter.api.Assertions.*;

public class GraalScriptTypesTest {
    private static boolean initialized;

    public static class Lazy {
        static { initialized = true; }
    }
    public interface Animal {
        int MARKER = 7;
        static String family() { return "animal"; }
    }

    public static class Horse implements Animal {
        public int getVariant() { return 257; }
        public static String label() { return "horse"; }
    }

    @ScriptNamespace("TestTypes")
    public static final class Catalog {
        public static final Class<Horse> Horse = Horse.class;
        public static final Class<Animal> Animal = Animal.class;
        public static final Class<String> String = String.class;
        public static final Class<Lazy> Lazy = Lazy.class;
    }

    @ScriptNamespace("TestTypes")
    public static final class ConflictingCatalog {
        public static final Class<Integer> Integer = Integer.class;
    }

    @ScriptNamespace("Bad")
    public static final class InvalidCatalog {
        public static Class<String> Mutable = String.class;
    }

    @ScriptNamespace("Bad")
    public static final class MismatchedCatalog {
        @SuppressWarnings({"rawtypes", "unchecked"})
        public static final Class<String> Wrong = (Class) Integer.class;
    }

    @ScriptNamespace("Bad")
    public static final class RawCatalog {
        public static final Class Raw = String.class;
    }

    @ScriptNamespace("Bad")
    public static final class EmptyCatalog {
    }

    private Context context() {
        return Context.newBuilder("js").allowAllAccess(true)
                .option("engine.WarnInterpreterOnly", "false").build();
    }

    private ScriptTypeRegistry registry() {
        ScriptTypeRegistry registry = new ScriptTypeRegistry();
        registry.register(Catalog.class);
        return registry;
    }

    @Test
    public void realHostSymbolsSupportInheritanceStaticsConstructorsAndNull() {
        try (Context context = context()) {
            GraalScriptTypes.install(context, "js", registry().snapshot());
            assertTrue(context.eval("js", """
                    const horse = new TestTypes.Horse();
                    horse instanceof TestTypes.Horse && horse instanceof TestTypes.Animal
                        && !(null instanceof TestTypes.Horse)
                        && !({} instanceof TestTypes.Horse)
                        && horse.getVariant() === 257 && TestTypes.Horse.label() === 'horse'
                    """
                    ).asBoolean());
            assertTrue(context.eval("js", "TestTypes.Horse === Java.type('" + Horse.class.getName() + "')").asBoolean());
            assertTrue(context.eval("js", "TestTypes.Horse.class.isInstance(horse)").asBoolean());
            assertTrue(context.eval("js", "TestTypes.Horse === TestTypes.Horse").asBoolean());
            assertEquals("animal", context.eval("js", "TestTypes.Animal.family()").asString());
            assertEquals(7, context.eval("js", "TestTypes.Animal.MARKER").asInt());
        }
    }

    @Test
    public void namespacesAreEnumerableAndMembersCannotBeChanged() {
        try (Context context = context()) {
            GraalScriptTypes.install(context, "js", registry().snapshot());
            assertEquals("Animal,Horse,Lazy,String", context.eval("js", "Object.keys(TestTypes).sort().join(',')").asString());
            context.eval("js", "TestTypes.Horse = null; TestTypes.NewType = null; delete TestTypes.Horse");
            assertTrue(context.eval("js", "typeof TestTypes.Horse.label === 'function' && TestTypes.NewType === undefined").asBoolean());
            assertThrows(Exception.class, () -> context.eval("js", "'use strict'; TestTypes.Horse = null"));
            assertTrue(context.eval("js", "TestTypes.Missing === undefined").asBoolean());
        }
    }

    @Test
    public void symbolsBelongToEachContextAndSurviveOtherContextClosure() {
        ScriptTypeRegistry registry = registry();
        try (Context second = context()) {
            try (Context first = context()) {
                GraalScriptTypes.install(first, "js", registry.snapshot());
                assertEquals("horse", first.eval("js", "TestTypes.Horse.label()").asString());
            }
            GraalScriptTypes.install(second, "js", registry.snapshot());
            assertTrue(second.eval("js", "new TestTypes.Horse() instanceof TestTypes.Animal").asBoolean());
        }
    }

    @Test
    public void conflictingGlobalsAreRejectedBeforeInstallingAnyNamespace() {
        try (Context context = context()) {
            context.getBindings("js").putMember("TestTypes", 42);
            assertThrows(IllegalArgumentException.class, () -> GraalScriptTypes.install(context, "js", registry().snapshot()));
            assertEquals(42, context.getBindings("js").getMember("TestTypes").asInt());
        }
    }

    @Test
    public void registryIsExplicitImmutableAndRejectsMalformedOrConflictingCatalogs() {
        ScriptTypeRegistry registry = registry();
        registry.register(Catalog.class);
        assertEquals(1, registry.snapshot().size());
        assertEquals(4, registry.snapshot().get("TestTypes").size());
        assertThrows(UnsupportedOperationException.class, () -> registry.snapshot().clear());
        assertThrows(UnsupportedOperationException.class, () -> registry.snapshot().get("TestTypes").clear());
        assertThrows(IllegalArgumentException.class, () -> registry.register(ConflictingCatalog.class));
        assertThrows(IllegalArgumentException.class, () -> registry.register(InvalidCatalog.class));
        assertThrows(IllegalArgumentException.class, () -> registry.register(MismatchedCatalog.class));
        assertThrows(IllegalArgumentException.class, () -> registry.register(RawCatalog.class));
        assertThrows(IllegalArgumentException.class, () -> registry.register(EmptyCatalog.class));
        assertThrows(IllegalArgumentException.class, () -> registry.register(Horse.class));
        assertEquals(1, registry.snapshot().size());
    }

    @Test
    public void installingDoesNotInitializeTargetsAndEscapedProxyRejectsClosedContext() {
        initialized = false;
        ProxyObject escaped;
        try (Context context = context()) {
            GraalScriptTypes.install(context, "js", registry().snapshot());
            assertFalse(initialized);
            context.eval("js", "TestTypes.Horse.label()");
            escaped = context.getBindings("js").getMember("TestTypes").asProxyObject();
        }
        assertThrows(IllegalStateException.class, () -> escaped.getMember("Horse"));
        assertThrows(IllegalStateException.class, () -> escaped.getMember("Lazy"));
    }
}
