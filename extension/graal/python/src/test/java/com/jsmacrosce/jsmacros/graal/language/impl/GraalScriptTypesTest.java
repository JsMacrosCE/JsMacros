package com.jsmacrosce.jsmacros.graal.language.impl;

import com.jsmacrosce.jsmacros.core.library.ScriptNamespace;
import com.jsmacrosce.jsmacros.core.library.ScriptTypeRegistry;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GraalScriptTypesTest {
    @ScriptNamespace("TestTypes")
    public static final class Catalog {
        public static final Class<java.util.ArrayList> ArrayList = java.util.ArrayList.class;
        public static final Class<java.util.List> List = java.util.List.class;
        public static final Class<Integer> Integer = Integer.class;
    }

    @Test
    public void pythonGetsActualTypesWithInheritanceConstructorsAndStaticMembers() {
        ScriptTypeRegistry registry = new ScriptTypeRegistry();
        registry.register(Catalog.class);
        try (Context context = Context.newBuilder("python").allowAllAccess(true)
                .option("engine.WarnInterpreterOnly", "false").build()) {
            GraalScriptTypes.install(context, "python", registry.snapshot());
            context.eval("python", """
                    items = TestTypes.ArrayList()
                    items.add('hello')
                    """
                    );
            assertTrue(context.eval("python", "isinstance(items, TestTypes.ArrayList)").asBoolean(), "concrete type");
            assertTrue(context.eval("python", "isinstance(items, TestTypes.List)").asBoolean(), "interface type");
            // GraalPython regards null as assignable to host reference types. Preserve native behavior.
            assertTrue(context.eval("python", "isinstance(None, TestTypes.ArrayList) == isinstance(None, __import__('java').type('java.util.ArrayList'))").asBoolean());
            assertEquals(42, context.eval("python", "TestTypes.Integer.parseInt('42')").asInt());
            assertTrue(context.eval("python", "TestTypes.ArrayList is __import__('java').type('java.util.ArrayList')").asBoolean());
            assertThrows(Exception.class, () -> context.eval("python", "TestTypes.ArrayList = None"));
            assertTrue(context.eval("python", "isinstance(TestTypes.ArrayList(), TestTypes.List)").asBoolean());
        }
    }
}
