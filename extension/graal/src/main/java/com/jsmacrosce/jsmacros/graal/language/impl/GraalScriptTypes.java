package com.jsmacrosce.jsmacros.graal.language.impl;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;
import java.util.Map;

/** Makes the registered type groups available to JavaScript and Python scripts. */
final class GraalScriptTypes {
    private GraalScriptTypes() {
    }

    static void install(Context context, String language, Map<String, Map<String, Class<?>>> namespaces) {
        if (namespaces.isEmpty()) return;
        // Scripts need usable types, not Java reflection objects: they should be able to check
        // instances, call constructors, and use static methods just as with Java.type/java.type.
        // Only JavaScript and Python are handled here; other languages keep their existing globals.
        String lookupSource = switch (language) {
            case "js" -> "Java.type";
            case "python" -> "__import__('java').type";
            default -> null;
        };
        if (lookupSource == null) return;
        Value bindings = context.getBindings(language);
        // Check all names first so a conflict cannot hide an existing global or add only some groups.
        for (String name : namespaces.keySet()) {
            if (bindings.hasMember(name)) {
                throw new IllegalArgumentException("Script type namespace conflicts with a global: " + name);
            }
        }
        // Keep the original lookup function even if a script later reassigns Java or java.
        Value lookup = context.eval(language, lookupSource);
        namespaces.forEach((name, types) -> bindings.putMember(name, new Namespace(types, lookup)));
    }

    private static final class Namespace implements ProxyObject {
        private final Map<String, Class<?>> types;
        // Reuse lookups within this script, but never pass its values to another script context.
        private final Map<String, Value> resolved = new HashMap<>();
        private final Value lookup;

        private Namespace(Map<String, Class<?>> types, Value lookup) {
            this.types = types;
            this.lookup = lookup;
        }

        @Override
        public synchronized Object getMember(String key) {
            // A Java caller might retain this object after the script closes. Reject that use,
            // even when the requested type was already looked up.
            lookup.canExecute();
            Class<?> type = types.get(key);
            if (type == null) return null;
            // Look up a type only when needed. getName() gives nested classes the '$' name Java expects.
            return resolved.computeIfAbsent(key, ignored -> lookup.execute(type.getName()));
        }

        @Override
        public Object getMemberKeys() {
            return types.keySet().toArray(new String[0]);
        }

        @Override
        public boolean hasMember(String key) {
            return types.containsKey(key);
        }

        @Override
        public void putMember(String key, Value value) {
            throw new UnsupportedOperationException("Script type namespaces are read-only");
        }

        @Override
        public boolean removeMember(String key) {
            throw new UnsupportedOperationException("Script type namespaces are read-only");
        }
    }
}
