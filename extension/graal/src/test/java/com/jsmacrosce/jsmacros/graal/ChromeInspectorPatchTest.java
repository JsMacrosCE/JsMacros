package com.jsmacrosce.jsmacros.graal;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the build-time Chrome Inspector patch. The inspector is a third-party jar that
 * JsMacrosCE rewrites before embedding, so a Graal upgrade could silently restore the
 * stock dispatch method without failing compilation.
 */
class ChromeInspectorPatchTest {

    private static final String SESSION_CLASS =
            "com/oracle/truffle/tools/chromeinspector/server/InspectServerSession.class";
    private static final String REJECTION_MARKER = "JsMacrosCE";

    @Test
    void dispatchReadsAndRejectsThrowOnSideEffect() throws Exception {
        byte[] classBytes = readPatchedClass();
        boolean[] readsTwoArgumentOptBoolean = {false};
        boolean[] containsRejection = {false};

        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (!"doProcessCommand".equals(name)) {
                    return null;
                }
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String member,
                                                String memberDescriptor, boolean isInterface) {
                        if ("optBoolean".equals(member)
                                && "(Ljava/lang/String;Z)Z".equals(memberDescriptor)) {
                            readsTwoArgumentOptBoolean[0] = true;
                        }
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof String text && text.contains(REJECTION_MARKER)) {
                            containsRejection[0] = true;
                        }
                    }
                };
            }
        }, 0);

        assertTrue(readsTwoArgumentOptBoolean[0],
                "patched dispatch does not read throwOnSideEffect from the command params");
        assertTrue(containsRejection[0],
                "patched dispatch does not throw a JsMacrosCE rejection");
    }

    @Test
    void dispatchThrowsBeforeEvaluatingWhenThrowOnSideEffectIsRequested() throws Exception {
        Class<?> sessionClass = Class.forName(
                "com.oracle.truffle.tools.chromeinspector.server.InspectServerSession");
        Constructor<?> constructor = sessionClass.getDeclaredConstructors()[0];
        Object[] constructorArgs = new Object[constructor.getParameterCount()];
        constructorArgs[constructorArgs.length - 1] = new ReentrantReadWriteLock();
        constructor.setAccessible(true);
        Object session = constructor.newInstance(constructorArgs);

        Class<?> commandClass = Class.forName(
                "com.oracle.truffle.tools.chromeinspector.commands.Command");
        Constructor<?> commandConstructor = commandClass.getConstructor(String.class);
        Class<?> postProcessorClass = Class.forName(
                "com.oracle.truffle.tools.chromeinspector.server.InspectServerSession$CommandPostProcessor");
        Method dispatch = sessionClass.getDeclaredMethod(
                "doProcessCommand", commandClass, postProcessorClass);
        dispatch.setAccessible(true);

        String commandPrefix = "{\"id\":1,\"method\":\"Debugger.evaluateOnCallFrame\","
                + "\"params\":{\"callFrameId\":\"0\",\"expression\":\"1\"";
        assertGuardRejects(dispatch, session, commandConstructor.newInstance(
                commandPrefix + ",\"throwOnSideEffect\":true}}"));

        // The guard sits in the shared dispatch, so previews that use Runtime.evaluate
        // while the debugger is not suspended are rejected too.
        assertGuardRejects(dispatch, session, commandConstructor.newInstance(
                "{\"id\":2,\"method\":\"Runtime.evaluate\","
                        + "\"params\":{\"expression\":\"1\",\"throwOnSideEffect\":true}}"));
    }

    private static void assertGuardRejects(Method dispatch, Object session, Object command) {
        InvocationTargetException failure = assertThrows(InvocationTargetException.class,
                () -> dispatch.invoke(session, command, null));
        Throwable cause = failure.getCause();
        assertTrue(cause.getClass().getName().endsWith("CommandProcessException"),
                "expected the inspector to reject the request, got " + cause);
        assertTrue(String.valueOf(cause.getMessage()).contains(REJECTION_MARKER),
                "unexpected rejection message: " + cause.getMessage());
    }

    private static byte[] readPatchedClass() throws Exception {
        Path directory = Path.of("build", "patched-deps");
        List<Path> jars;
        try (Stream<Path> files = Files.list(directory)) {
            jars = files
                    .filter(file -> file.getFileName().toString().startsWith("chromeinspector-tool-"))
                    .filter(file -> file.getFileName().toString().endsWith(".jar"))
                    .toList();
        }
        assertEquals(1, jars.size(),
                "expected one patched inspector jar in " + directory.toAbsolutePath());

        try (ZipFile zip = new ZipFile(jars.get(0).toFile())) {
            ZipEntry entry = zip.getEntry(SESSION_CLASS);
            assertNotNull(entry, "patched jar is missing " + SESSION_CLASS);
            try (InputStream stream = zip.getInputStream(entry)) {
                return stream.readAllBytes();
            }
        }
    }
}
