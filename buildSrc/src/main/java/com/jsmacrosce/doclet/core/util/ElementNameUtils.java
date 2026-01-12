package com.jsmacrosce.doclet.core.util;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

public final class ElementNameUtils {
    private ElementNameUtils() {
    }

    public static String getPackageName(TypeElement type) {
        Element current = type;
        while (current.getKind() != ElementKind.PACKAGE) {
            current = current.getEnclosingElement();
        }
        return ((PackageElement) current).getQualifiedName().toString();
    }

    public static String getDisplayClassName(TypeElement type) {
        StringBuilder name = new StringBuilder(type.getSimpleName());
        Element current = type.getEnclosingElement();
        while (current.getKind() == ElementKind.INTERFACE || current.getKind() == ElementKind.CLASS) {
            name.insert(0, current.getSimpleName() + ".");
            current = current.getEnclosingElement();
        }
        return name.toString();
    }

    public static String getQualifiedName(TypeElement type) {
        String pkg = getPackageName(type);
        String name = getDisplayClassName(type);
        return pkg.isEmpty() ? name : pkg + "." + name;
    }

    public static String memberId(Element member) {
        StringBuilder s = new StringBuilder();
        switch (member.getKind()) {
            case ENUM_CONSTANT, FIELD -> s.append(member.getSimpleName());
            case CONSTRUCTOR, METHOD -> {
                if (member.getKind() == ElementKind.METHOD) {
                    s.append(member.getSimpleName());
                } else {
                    s.append("constructor");
                }
                for (VariableElement parameter : ((ExecutableElement) member).getParameters()) {
                    s.append("-").append(getTypeMirrorName(parameter.asType()));
                }
                s.append("-");
            }
            default -> throw new UnsupportedOperationException(String.valueOf(member.getKind()));
        }
        return s.toString();
    }

    private static String getTypeMirrorName(TypeMirror type) {
        return switch (type.getKind()) {
            case BOOLEAN -> "boolean";
            case BYTE -> "byte";
            case SHORT -> "short";
            case INT -> "int";
            case LONG -> "long";
            case CHAR -> "char";
            case FLOAT -> "float";
            case DOUBLE -> "double";
            case VOID, NONE -> "void";
            case NULL -> "null";
            case ARRAY -> getTypeMirrorName(((ArrayType) type).getComponentType()) + "[]";
            case DECLARED -> getDisplayClassName((TypeElement) ((DeclaredType) type).asElement());
            case TYPEVAR -> ((TypeVariable) type).asElement().getSimpleName().toString();
            case WILDCARD -> "?";
            default -> type.toString();
        };
    }
}
