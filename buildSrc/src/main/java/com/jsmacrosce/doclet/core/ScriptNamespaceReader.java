package com.jsmacrosce.doclet.core;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import java.util.*;

/** Finds the short type names to include in documentation, using the same lists that scripts use. */
public final class ScriptNamespaceReader {
    private ScriptNamespaceReader() {
    }

    public record Export(String name, TypeElement target) {
    }

    public record Namespace(String name, TypeElement catalog, List<Export> exports) {
    }

    public static List<Namespace> read(Set<? extends Element> elements) {
        Map<String, Namespace> namespaces = new TreeMap<>();
        Set<String> globals = new HashSet<>(Set.of("context", "event", "file"));
        // Find all library names first so conflicts are caught regardless of which class is read first.
        for (Element element : elements) {
            if (!(element instanceof TypeElement type)) continue;
            for (AnnotationMirror annotation : type.getAnnotationMirrors()) {
                if (annotation.getAnnotationType().asElement().getSimpleName().contentEquals("Library")) {
                    globals.add(value(annotation));
                }
            }
        }
        for (Element element : elements) {
            if (!(element instanceof TypeElement type)) continue;
            // Match the actual runtime annotation, not an unrelated annotation with the same name.
            AnnotationMirror annotation = type.getAnnotationMirrors().stream()
                    .filter(a -> a.getAnnotationType().toString().equals("com.jsmacrosce.jsmacros.core.library.ScriptNamespace"))
                    .findFirst().orElse(null);
            if (annotation == null) continue;
            String name = value(annotation);
            if (!name.matches("[A-Za-z_][A-Za-z0-9_]*") || globals.contains(name) || namespaces.containsKey(name)) {
                throw new IllegalArgumentException("Invalid or conflicting script namespace: " + name);
            }
            List<Export> exports = new ArrayList<>();
            Set<String> names = new HashSet<>();
            for (Element field : type.getEnclosedElements()) {
                if (field.getKind() != ElementKind.FIELD || !field.getModifiers().contains(Modifier.PUBLIC)) continue;
                // Read the type from its source declaration so generating docs does not run game code.
                if (!field.getModifiers().containsAll(Set.of(Modifier.STATIC, Modifier.FINAL))
                        || !(field.asType() instanceof DeclaredType declared)
                        || !((TypeElement) declared.asElement()).getQualifiedName().contentEquals("java.lang.Class")
                        || declared.getTypeArguments().size() != 1
                        || !(declared.getTypeArguments().getFirst() instanceof DeclaredType target)) {
                    throw new IllegalArgumentException("Expected public static final Class<T> export: " + field);
                }
                String memberName = field.getSimpleName().toString();
                TypeElement targetType = (TypeElement) target.asElement();
                if (!memberName.matches("[A-Za-z_][A-Za-z0-9_]*") || !names.add(memberName)
                        || !targetType.getModifiers().contains(Modifier.PUBLIC)) {
                    throw new IllegalArgumentException("Invalid script type export: " + field);
                }
                exports.add(new Export(memberName, targetType));
            }
            exports.sort(Comparator.comparing(Export::name));
            if (exports.isEmpty()) throw new IllegalArgumentException("Empty script namespace: " + name);
            namespaces.put(name, new Namespace(name, type, List.copyOf(exports)));
        }
        return List.copyOf(namespaces.values());
    }

    private static String value(AnnotationMirror annotation) {
        return annotation.getElementValues().entrySet().stream()
                .filter(e -> e.getKey().getSimpleName().contentEquals("value"))
                .map(e -> String.valueOf(e.getValue().getValue())).findFirst().orElse("");
    }
}
