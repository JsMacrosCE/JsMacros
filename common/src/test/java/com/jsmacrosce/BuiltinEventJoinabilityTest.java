package com.jsmacrosce;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Guards the Events guide's statement that every joinable built-in event is currently also
 * cancellable. Scans compiled event classes with ASM so the check does not load Minecraft classes
 * or start a client.
 *
 * <p>If this check starts failing, update the "Joinable events" section in
 * {@code docs/vitepress/content/guide/concepts/events.md} first, then decide whether this check
 * should change or be removed.
 */
public final class BuiltinEventJoinabilityTest {
    private static final String EVENT_ANNOTATION = "Lcom/jsmacrosce/jsmacros/core/event/Event;";

    public static void main(String[] args) throws IOException {
        List<String> failures = new ArrayList<>();
        int events = 0;
        int cancellableEvents = 0;

        for (String directory : args) {
            Path root = Path.of(directory);
            if (!Files.isDirectory(root)) continue;

            try (var files = Files.walk(root)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".class")).toList()) {
                    ClassNode clazz = new ClassNode();
                    new ClassReader(Files.readAllBytes(file)).accept(clazz, 0);
                    AnnotationNode event = annotation(clazz);
                    if (event == null) continue;

                    events++;
                    boolean cancellable = Boolean.TRUE.equals(value(event, "cancellable"));
                    if (cancellable) cancellableEvents++;
                    if (Boolean.TRUE.equals(value(event, "joinable")) && !cancellable) {
                        failures.add(clazz.name.replace('/', '.'));
                    }
                }
            }
        }

        if (events == 0) {
            throw new AssertionError("No @Event classes found; pass the compiled main class directories");
        }
        if (cancellableEvents == 0) {
            throw new AssertionError("No cancellable @Event classes found; the test cannot read event metadata");
        }

        if (!failures.isEmpty()) {
            failures.sort(String::compareTo);
            System.err.println("The Events guide says no built-in event is joinable unless it is cancellable.");
            System.err.println("Update the 'Joinable events' section in docs/vitepress/content/guide/concepts/events.md,");
            System.err.println("then update or remove this check.");
            failures.forEach(name -> System.err.println("  joinable but not cancellable: " + name));
            throw new AssertionError(failures.size() + " built-in event(s) break the documented joinability rule");
        }

        System.out.println("BuiltinEventJoinability: " + events
            + " event classes checked; all joinable events are cancellable");
    }

    private static AnnotationNode annotation(ClassNode clazz) {
        AnnotationNode visible = find(clazz.visibleAnnotations);
        return visible != null ? visible : find(clazz.invisibleAnnotations);
    }

    private static AnnotationNode find(List<AnnotationNode> annotations) {
        if (annotations == null) return null;
        for (AnnotationNode annotation : annotations) {
            if (EVENT_ANNOTATION.equals(annotation.desc)) return annotation;
        }
        return null;
    }

    private static Object value(AnnotationNode annotation, String key) {
        if (annotation.values == null) return null;
        for (int i = 0; i < annotation.values.size(); i += 2) {
            if (key.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        }
        return null;
    }
}
