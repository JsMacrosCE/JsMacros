package com.jsmacrosce.doclet.core.util;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The type parameters a class written inside another one carries. A class sees the type
 * parameters of the class it is written inside, unless it declares ones with the same names,
 * and the definitions write such a class under one name of its own, so the parameters have to
 * be carried along with it.
 */
public final class NestedTypeParams {
    /**
     * A type parameter a class carries, and where it comes from: the level it was declared on,
     * counted from the class the carrying class is written inside, and its place among the
     * parameters of that level.
     */
    public record Captured(String name, TypeParameterElement element, int level, int index) {
    }

    private NestedTypeParams() {
    }

    /**
     * The classes the given class is written inside, as far as the type parameters reach. A
     * class written inside a static one does not see the parameters of the classes above it,
     * so the chain stops there.
     */
    public static List<TypeElement> captureChain(TypeElement type) {
        List<TypeElement> chain = new ArrayList<>();
        TypeElement child = type;
        Element parent = child.getEnclosingElement();
        while (parent instanceof TypeElement parentType) {
            if (child.getModifiers().contains(Modifier.STATIC)) {
                break;
            }
            chain.add(parentType);
            child = parentType;
            parent = parentType.getEnclosingElement();
        }
        return chain;
    }

    /**
     * The parameters of the classes the given class is written inside that are in scope in it.
     * The parameters come back from the outermost class inwards, and a class that declares a
     * parameter of its own hides the one of the same name it would otherwise carry.
     */
    public static List<Captured> capturedParams(TypeElement type) {
        List<TypeElement> chain = captureChain(type);
        if (chain.isEmpty()) {
            return List.of();
        }
        Set<String> own = paramNames(type);
        List<Captured> captured = new ArrayList<>();
        // The chain counts from the class the type is written inside, so it is walked from the
        // outermost class inwards, and every level hides the levels above it.
        for (int level = chain.size() - 1; level >= 0; level--) {
            TypeElement element = chain.get(level);
            Set<String> names = paramNames(element);
            captured.removeIf(param -> names.contains(param.name()));
            int index = 0;
            for (TypeParameterElement param : element.getTypeParameters()) {
                captured.add(new Captured(param.getSimpleName().toString(), param, level, index++));
            }
        }
        captured.removeIf(param -> own.contains(param.name()));
        return captured;
    }

    private static Set<String> paramNames(TypeElement type) {
        Set<String> names = new LinkedHashSet<>();
        for (TypeParameterElement param : type.getTypeParameters()) {
            names.add(param.getSimpleName().toString());
        }
        return names;
    }
}
