package com.jsmacrosce;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

// Static binding checks only, not a replacement for applying Mixins in a running client.
public final class MixinBindingsTest {
    private static final Map<String, ClassNode> classes = new HashMap<>();
    private static final List<String> failures = new ArrayList<>();
    private static int checks;
    private static int mixins;

    public static void main(String[] args) throws IOException {
        for (String directory : args) {
            if (!Files.isDirectory(Path.of(directory))) continue;
            try (var files = Files.walk(Path.of(directory))) {
                for (Path file : files.filter(path -> path.toString().endsWith(".class")).toList()) {
                    ClassNode mixin = new ClassNode();
                    new ClassReader(Files.readAllBytes(file)).accept(mixin, 0);
                    AnnotationNode annotation = annotation(mixin.visibleAnnotations, mixin.invisibleAnnotations, "Mixin");
                    if (annotation == null) continue;
                    mixins++;
                    List<String> owners = new ArrayList<>();
                    for (Object type : list(value(annotation, "value"))) owners.add(((Type) type).getInternalName());
                    for (Object type : list(value(annotation, "targets"))) owners.add(type.toString().replace('.', '/'));
                    for (String owner : owners) inspect(mixin, read(owner));
                }
            }
        }
        failures.forEach(System.err::println);
        if (!failures.isEmpty()) throw new AssertionError(failures.size() + " unresolved Mixin bindings");
        if (mixins == 0 || checks == 0) throw new AssertionError("No Mixin bindings inspected");
        System.out.println("MixinBindings: " + mixins + " mixins, " + checks + " exact-version static binding checks passed");
    }

    private static void inspect(ClassNode mixin, ClassNode target) throws IOException {
        for (FieldNode field : mixin.fields) {
            AnnotationNode shadow = annotation(field.visibleAnnotations, field.invisibleAnnotations, "Shadow");
            if (shadow != null) {
                boolean found = field(target, field.name, field.desc);
                for (Object alias : list(value(shadow, "aliases"))) found |= field(target, alias.toString(), field.desc);
                check(found, mixin.name + "." + field.name + " shadow " + target.name + ":" + field.desc);
            }
        }
        for (MethodNode method : mixin.methods) {
            AnnotationNode shadow = annotation(method.visibleAnnotations, method.invisibleAnnotations, "Shadow");
            if (shadow != null) {
                String prefix = Objects.toString(value(shadow, "prefix"), "shadow$");
                String name = method.name.startsWith(prefix) ? method.name.substring(prefix.length()) : method.name;
                boolean found = !methods(target, name + method.desc, true).isEmpty();
                for (Object alias : list(value(shadow, "aliases"))) found |= !methods(target, alias + method.desc, true).isEmpty();
                check(found, mixin.name + "." + method.name + " shadow " + target.name + "." + name + method.desc);
            }
            AnnotationNode accessor = annotation(method.visibleAnnotations, method.invisibleAnnotations, "Accessor");
            if (accessor != null) {
                String name = Objects.toString(value(accessor, "value"), "");
                if (name.isEmpty()) name = infer(method.name, "get", "set", "is");
                Type[] args = Type.getArgumentTypes(method.desc);
                String desc = (args.length == 0 ? Type.getReturnType(method.desc) : args[0]).getDescriptor();
                check(field(target, name, desc), mixin.name + "." + method.name + " accessor " + target.name + "." + name + ":" + desc);
            }
            AnnotationNode invoker = annotation(method.visibleAnnotations, method.invisibleAnnotations, "Invoker");
            if (invoker != null) {
                String name = Objects.toString(value(invoker, "value"), "");
                if (name.isEmpty()) name = infer(method.name, "invoke", "call");
                check(!methods(target, name + method.desc, true).isEmpty(), mixin.name + "." + method.name + " invoker " + target.name + "." + name + method.desc);
            }
            for (AnnotationNode injection : annotations(method.visibleAnnotations, method.invisibleAnnotations)) {
                if (value(injection, "method") == null) continue;
                List<MethodNode> selected = new ArrayList<>();
                for (Object selector : list(value(injection, "method"))) {
                    List<MethodNode> matches = methods(target, selector.toString(), false);
                    check(!matches.isEmpty(), mixin.name + "." + method.name + " injection " + target.name + "." + selector);
                    selected.addAll(matches);
                }
                for (Object atValue : list(value(injection, "at"))) {
                    AnnotationNode at = (AnnotationNode) atValue;
                    String member = Objects.toString(value(at, "target"), "");
                    if (member.isEmpty() || member.startsWith("[")) continue;
                    check(selected.stream().flatMap(m -> Arrays.stream(m.instructions.toArray())).anyMatch(insn -> matches(insn, member)),
                            mixin.name + "." + method.name + " @At target " + member);
                }
                if (injection.desc.endsWith("/Inject;")) {
                    Type[] handler = Type.getArgumentTypes(method.desc);
                    int callback = 0;
                    while (callback < handler.length && !handler[callback].getClassName().startsWith("org.spongepowered.asm.mixin.injection.callback.CallbackInfo")) callback++;
                    int captured = callback;
                    check(selected.stream().anyMatch(m -> {
                        Type[] parameters = Type.getArgumentTypes(m.desc);
                        return captured <= parameters.length && Arrays.equals(Arrays.copyOf(handler, captured), Arrays.copyOf(parameters, captured));
                    }), mixin.name + "." + method.name + " injected argument prefix " + method.desc);
                }
            }
        }
    }

    private static boolean matches(AbstractInsnNode insn, String target) {
        if (insn instanceof MethodInsnNode call) return target.equals("L" + call.owner + ";" + call.name + call.desc);
        if (insn instanceof FieldInsnNode field) return target.equals("L" + field.owner + ";" + field.name + ":" + field.desc);
        return false;
    }

    private static String infer(String method, String... prefixes) {
        for (String prefix : prefixes) {
            if (method.startsWith(prefix) && method.length() > prefix.length()) {
                String name = method.substring(prefix.length());
                return Character.toLowerCase(name.charAt(0)) + name.substring(1);
            }
        }
        return method;
    }

    private static boolean field(ClassNode owner, String name, String desc) throws IOException {
        if (owner.fields.stream().anyMatch(f -> f.name.equals(name) && f.desc.equals(desc))) return true;
        return owner.superName != null && field(read(owner.superName), name, desc);
    }

    private static List<MethodNode> methods(ClassNode owner, String selector, boolean inherited) throws IOException {
        if (selector.startsWith("L") && selector.contains(";")) selector = selector.substring(selector.indexOf(';') + 1);
        int descriptor = selector.indexOf('(');
        String name = descriptor < 0 ? selector : selector.substring(0, descriptor);
        String desc = descriptor < 0 ? null : selector.substring(descriptor);
        List<MethodNode> found = owner.methods.stream().filter(m -> m.name.equals(name) && (desc == null || m.desc.equals(desc))).toList();
        if (!found.isEmpty() || !inherited) return found;
        if (owner.superName != null) {
            found = methods(read(owner.superName), selector, true);
            if (!found.isEmpty()) return found;
        }
        for (String iface : owner.interfaces) {
            found = methods(read(iface), selector, true);
            if (!found.isEmpty()) return found;
        }
        return List.of();
    }

    private static ClassNode read(String name) throws IOException {
        if (classes.containsKey(name)) return classes.get(name);
        try (var input = MixinBindingsTest.class.getResourceAsStream("/" + name + ".class")) {
            if (input == null) throw new IOException("Missing exact-classpath class " + name);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            classes.put(name, node);
            return node;
        }
    }

    private static AnnotationNode annotation(List<AnnotationNode> visible, List<AnnotationNode> invisible, String simple) {
        return annotations(visible, invisible).stream().filter(a -> a.desc.endsWith("/" + simple + ";")).findFirst().orElse(null);
    }

    private static List<AnnotationNode> annotations(List<AnnotationNode> visible, List<AnnotationNode> invisible) {
        List<AnnotationNode> result = new ArrayList<>();
        if (visible != null) result.addAll(visible);
        if (invisible != null) result.addAll(invisible);
        return result;
    }

    private static Object value(AnnotationNode annotation, String key) {
        if (annotation.values == null) return null;
        for (int i = 0; i < annotation.values.size(); i += 2) if (annotation.values.get(i).equals(key)) return annotation.values.get(i + 1);
        return null;
    }

    private static List<?> list(Object value) {
        return value == null ? List.of() : value instanceof List<?> values ? values : List.of(value);
    }

    private static void check(boolean success, String message) {
        checks++;
        if (!success) failures.add(message);
    }
}
