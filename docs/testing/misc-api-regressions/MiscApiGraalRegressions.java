import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.UnaryOperator;

/**
 * Exercises real Graal JavaScript interop with separately loaded mod and extension
 * classes, using the production {@link IFilter} interface and host-loader bridge.
 *
 * <p>The suite reproduces the missing Graal {@code Value} failure with parent-only
 * host lookup, then checks the bridge's class identities, legacy aliases, missing
 * class behavior, adapter creation, callbacks, and interface overload selection.
 * The callback-adapter fixture is read from the client regression macro.</p>
 *
 * <p>Run through {@link MiscApiGraalClassLoaderRegressions} to maintain loader
 * isolation. These checks do not execute Minecraft scanning, Mixins, or client
 * thread handoff.</p>
 */
public class MiscApiGraalRegressions {
    /**
     * Matches the scanner builder's string/interface overload shape without
     * implementing scanning or scanner thread policy. Each interface overload
     * invokes its callback so overload selection alone cannot satisfy the test.
     */
    public static class Overloads {
        /** Number of interface-overload calls whose callbacks returned true. */
        public int callbackCalls;
        /** Number of string-overload calls, which must remain zero in callback tests. */
        public int stringCalls;

        /**
         * Invokes the block callback with a non-null fixture value.
         *
         * @param filter callback selected by Graal overload resolution
         * @return this fixture
         * @throws AssertionError if the callback returns false
         */
        public Overloads withBlockFilter(IFilter<String> filter) {
            if (!filter.apply("block fixture")) throw new AssertionError("block callback returned false");
            callbackCalls++;
            return this;
        }

        /**
         * Invokes the state callback with a non-null fixture value.
         *
         * @param filter callback selected by Graal overload resolution
         * @return this fixture
         * @throws AssertionError if the callback returns false
         */
        public Overloads withStateFilter(IFilter<String> filter) {
            if (!filter.apply("state fixture")) throw new AssertionError("state callback returned false");
            callbackCalls++;
            return this;
        }

        /**
         * Records selection of the competing block string overload without method lookup.
         *
         * @param method unused method-name fixture
         * @return this fixture
         */
        public Overloads withBlockFilter(String method) { stringCalls++; return this; }
        /**
         * Records selection of the competing state string overload without method lookup.
         *
         * @param method unused method-name fixture
         * @return this fixture
         */
        public Overloads withStateFilter(String method) { stringCalls++; return this; }
    }

    /**
     * Runs the negative control and bridge/adapter assertions in closeable Graal contexts.
     *
     * @param args client regression macro path as the first argument
     * @throws Exception if fixture loading, reflection, or Graal evaluation fails
     * @throws AssertionError if loader isolation or a regression assertion fails
     */
    public static void main(String[] args) throws Exception {
        String macro = Files.readString(Path.of(args[0]));
        String startMarker = "  var IFilter = type('client.api.classes.worldscanner.filter.api.IFilter');";
        int start = macro.indexOf(startMarker);
        int end = macro.indexOf("  ['Block', 'State'].forEach", start);
        if (start == -1 || end <= start) throw new AssertionError("scanner callback fixture boundaries not found");
        ClassLoader modLoader = IFilter.class.getClassLoader();
        ClassLoader extensionLoader = MiscApiGraalRegressions.class.getClassLoader();
        if (modLoader == extensionLoader || org.graalvm.polyglot.Value.class.getClassLoader() != extensionLoader) {
            throw new AssertionError("Test invalid: mod and Graal classes must use separate loaders");
        }
        ClassLoader reflectionLoader = new ClassLoader(modLoader) {};
        // Prove the old parent-only host loader reproduces the user's failure.
        try (Context context = context(reflectionLoader)) {
            try {
                context.eval("js", """
                        Java.extend(Java.type('com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter'));
                        """);
                throw new AssertionError("Parent-only loader unexpectedly supports Java.extend");
            } catch (PolyglotException expected) {
                if (!expected.getMessage().contains("org/graalvm/polyglot/Value")) throw expected;
            }
        }
        System.out.println("PASS split-loader negative control: parent-only host lookup reproduces missing Graal Value");
        Class<?> loaderType = extensionLoader.loadClass("com.jsmacrosce.jsmacros.graal.language.impl.GraalHostClassLoader");
        var constructor = loaderType.getDeclaredConstructor(ClassLoader.class, ClassLoader.class, UnaryOperator.class);
        constructor.setAccessible(true);
        UnaryOperator<String> redirect = name -> name.replace("xyz.wagyourtail.", "com.jsmacrosce.");
        ClassLoader bridge = (ClassLoader) constructor.newInstance(reflectionLoader, extensionLoader, redirect);
        if (bridge.loadClass("org.graalvm.polyglot.Value") != org.graalvm.polyglot.Value.class ||
                bridge.loadClass(IFilter.class.getName()) != IFilter.class ||
                bridge.loadClass("xyz.wagyourtail.jsmacros.client.api.classes.worldscanner.filter.api.IFilter") != IFilter.class) {
            throw new AssertionError("Bridge must preserve Graal/mod class identity and legacy aliases");
        }
        try {
            bridge.loadClass("missing.misc.api.RegressionType");
            throw new AssertionError("unknown class accepted");
        } catch (ClassNotFoundException expected) {
            // No arbitrary bytecode creation or permissive recovery for unknown classes.
        }
        try (Context context = context(bridge)) {
            context.eval("js", """
                    var SupplierAdapter = Java.extend(Java.type('java.util.function.Supplier'));
                    var supplier = new SupplierAdapter({ get: function () { return 'supplier-ok'; } });
                    if (supplier.get() !== 'supplier-ok') throw new Error('bootstrap interface adapter failed');
                    """);
            // Extension-only types must also be available through Java.type without copying bytecode.
            if (context.eval("js", "Java.type('org.graalvm.polyglot.Value').class").asHostObject() != org.graalvm.polyglot.Value.class) {
                throw new AssertionError("host lookup returned a different Polyglot Value class");
            }
            try {
                context.eval("js", """
                        var BadFilter = Java.type('com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter');
                        new BadFilter({ apply: function () { return true; } });
                        """);
                throw new AssertionError("direct interface construction unexpectedly succeeded");
            } catch (PolyglotException expected) {
                if (!expected.getMessage().contains("Message not supported")) throw expected;
            }
            context.eval("js", """
                    function type(name) { return Java.type('com.jsmacrosce.jsmacros.' + name); }
                    """ + macro.substring(start, end));
            IFilter<?> callback = context.getBindings("js").getMember("callback").as(IFilter.class);
            if (!Boolean.TRUE.equals(callback.apply(null))) throw new AssertionError("Java-to-JS callback failed");
            Overloads overloads = new Overloads();
            context.getBindings("js").putMember("overloads", overloads);
            context.eval("js", "overloads.withBlockFilter(callback); overloads.withStateFilter(callback);");
            if (overloads.callbackCalls != 2 || overloads.stringCalls != 0) {
                throw new AssertionError("callback did not select both IFilter overloads");
            }
            System.out.println("PASS split-loader Graal: host bridge preserves identities/aliases; Java.extend and both IFilter overloads work");
        }
    }

    /**
     * Creates a JavaScript context with the production host-access settings used
     * by these interop checks and an explicitly selected host loader.
     *
     * @param hostLoader loader used for Java host lookup and generated adapters
     * @return a new context that the caller must close
     */
    private static Context context(ClassLoader hostLoader) {
        // Same host permissions as GraalLanguageDefinition; interpreter mode needs no compiler module.
        return Context.newBuilder("js")
                .allowAllAccess(true)
                .allowHostClassLookup(name -> true)
                .hostClassLoader(hostLoader)
                .option("engine.WarnInterpreterOnly", "false")
                .build();
    }
}
