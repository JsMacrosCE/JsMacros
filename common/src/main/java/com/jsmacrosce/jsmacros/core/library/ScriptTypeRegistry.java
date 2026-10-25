package com.jsmacrosce.jsmacros.core.library;

import com.jsmacrosce.doclet.DocletIgnore;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Keeps track of the Java types scripts can access through short names such as {@code EntityHelpers.Horse}. */
@DocletIgnore
public final class ScriptTypeRegistry {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private final Map<String, Map<String, Class<?>>> namespaces = new LinkedHashMap<>();

    /** Adds a group of script types after checking every entry. Registering the same entries again is allowed. */
    public synchronized void register(Class<?> catalog) {
        ScriptNamespace annotation = catalog.getAnnotation(ScriptNamespace.class);
        if (annotation == null || !IDENTIFIER.matcher(annotation.value()).matches()) {
            throw new IllegalArgumentException("Missing or invalid @ScriptNamespace on " + catalog.getName());
        }
        Map<String, Class<?>> types = new LinkedHashMap<>();
        // Expose only the types listed in this class, in a predictable order.
        Field[] fields = catalog.getDeclaredFields();
        Arrays.sort(fields, java.util.Comparator.comparing(Field::getName));
        for (Field field : fields) {
            if (!Modifier.isPublic(field.getModifiers()) || field.isSynthetic()) continue;
            if (!Modifier.isStatic(field.getModifiers()) || !Modifier.isFinal(field.getModifiers())
                    || field.getType() != Class.class || !IDENTIFIER.matcher(field.getName()).matches()) {
                throw new IllegalArgumentException("Expected public static final Class field: " + field);
            }
            try {
                Class<?> type = (Class<?>) field.get(null);
                // Documentation reads the field's declared type. Check that its value matches,
                // so an unsafe cast cannot make scripts and their documentation disagree.
                if (type == null || !Modifier.isPublic(type.getModifiers())
                        || !(field.getGenericType() instanceof ParameterizedType signature)
                        || !signature.getActualTypeArguments()[0].equals(type)) {
                    throw new IllegalArgumentException("Exported type must be public and match Class<T>: " + field);
                }
                types.put(field.getName(), type);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Inaccessible script type catalog: " + catalog.getName(), e);
            }
        }
        if (types.isEmpty()) throw new IllegalArgumentException("Empty script type catalog: " + catalog.getName());
        // A bad entry must not leave a half-registered group or replace a working one.
        Map<String, Class<?>> previous = namespaces.get(annotation.value());
        if (previous != null && !previous.equals(types)) {
            throw new IllegalArgumentException("Script namespace already registered: " + annotation.value());
        }
        namespaces.put(annotation.value(), Collections.unmodifiableMap(types));
    }

    /** Returns a read-only copy of the registered groups for language adapters to expose to scripts. */
    public synchronized Map<String, Map<String, Class<?>>> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(namespaces));
    }
}
