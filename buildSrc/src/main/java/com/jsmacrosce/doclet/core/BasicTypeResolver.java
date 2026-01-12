package com.jsmacrosce.doclet.core;

import com.jsmacrosce.doclet.core.model.TypeKind;
import com.jsmacrosce.doclet.core.model.TypeRef;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BasicTypeResolver implements TypeResolver {
    private static final Set<String> JAVA_ALIASES = Set.of(
        "java.lang.Array",
        "java.lang.Class",
        "java.util.Collection",
        "java.util.List",
        "java.util.Map",
        "java.util.Set"
    );
    private static final Map<String, String> JAVA_NUMBER_TYPES = Map.of(
        "java.lang.Integer", "int",
        "java.lang.Float", "float",
        "java.lang.Long", "long",
        "java.lang.Short", "short",
        "java.lang.Character", "char",
        "java.lang.Byte", "byte",
        "java.lang.Double", "double",
        "java.lang.Number", "number"
    );
    private static final Map<String, String> FUNCTIONAL_INTERFACES = Map.of(
        "java.util.function.Consumer", "MethodWrapper<$0>",
        "java.util.function.BiConsumer", "MethodWrapper<$0, $1>",
        "java.util.function.Function", "MethodWrapper<$0, any, $1>",
        "java.util.function.BiFunction", "MethodWrapper<$0, $1, $2>",
        "java.util.function.Predicate", "MethodWrapper<$0, any, boolean>",
        "java.util.function.BiPredicate", "MethodWrapper<$0, $1, boolean>",
        "java.util.function.Supplier", "MethodWrapper<any, any, $0>",
        "java.util.Comparator", "MethodWrapper<$0, $0, int>",
        "java.lang.Runnable", "MethodWrapper"
    );

    private Map<String, String> typeScriptAliases = Map.of();
    private boolean pythonAliasEnabled = true;
    private final Set<TypeMirror> resolving = Collections.newSetFromMap(new IdentityHashMap<>());

    public void setTypeScriptAliases(Map<String, String> typeScriptAliases) {
        this.typeScriptAliases = typeScriptAliases == null ? Map.of() : typeScriptAliases;
    }

    public void setPythonAliasEnabled(boolean pythonAliasEnabled) {
        this.pythonAliasEnabled = pythonAliasEnabled;
    }

    @Override
    public TypeRef resolve(TypeMirror type) {
        if (type == null) {
            return new TypeRef(TypeKind.UNKNOWN, "unknown", "unknown", List.of(), false, false, null, false);
        }
        if (!resolving.add(type)) {
            return new TypeRef(TypeKind.UNKNOWN, type.toString(), type.toString(), List.of(), false, false, null, false);
        }
        try {
            return switch (type.getKind()) {
            case BOOLEAN, BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE ->
                new TypeRef(TypeKind.PRIMITIVE, type.toString(), type.toString(), List.of(), false, false, null, false);
            case VOID, NONE -> new TypeRef(TypeKind.VOID, "void", "void", List.of(), false, false, null, false);
            case ARRAY -> {
                ArrayType arrayType = (ArrayType) type;
                TypeRef component = resolve(arrayType.getComponentType());
                yield new TypeRef(TypeKind.ARRAY, component.name(), component.qualifiedName(), List.of(component), true, false, null, false);
            }
            case DECLARED -> resolveDeclared((DeclaredType) type);
            case TYPEVAR -> resolveTypeVar((TypeVariable) type);
            case WILDCARD -> resolveWildcard((WildcardType) type);
            case INTERSECTION -> resolveIntersection((IntersectionType) type);
            case UNION -> resolveUnion((UnionType) type);
            default -> new TypeRef(TypeKind.UNKNOWN, type.toString(), type.toString(), List.of(), false, false, null, false);
        };
        } finally {
            resolving.remove(type);
        }
    }

    private TypeRef resolveDeclared(DeclaredType type) {
        Element el = type.asElement();
        String qualifiedName = getQualifiedName(el);
        String displayName = getDisplayName(el);
        List<TypeRef> params = new ArrayList<>();
        for (TypeMirror arg : type.getTypeArguments()) {
            params.add(resolve(arg));
        }
        return new TypeRef(TypeKind.DECLARED, displayName, qualifiedName, params, false, false, null, false);
    }

    private TypeRef resolveTypeVar(TypeVariable type) {
        String name = type.asElement().getSimpleName().toString();
        TypeMirror bound = type.getUpperBound();
        TypeRef bounds = bound == null || bound.toString().equals("java.lang.Object") ? null : resolve(bound);
        return new TypeRef(TypeKind.TYPEVAR, name, name, List.of(), false, false, bounds, false);
    }

    private TypeRef resolveWildcard(WildcardType type) {
        TypeMirror bound = type.getExtendsBound();
        if (bound == null) {
            bound = type.getSuperBound();
        }
        TypeRef bounds = bound == null ? null : resolve(bound);
        return new TypeRef(TypeKind.WILDCARD, "?", "?", List.of(), false, false, bounds, false);
    }

    private TypeRef resolveIntersection(IntersectionType type) {
        List<TypeRef> bounds = new ArrayList<>();
        for (TypeMirror bound : type.getBounds()) {
            bounds.add(resolve(bound));
        }
        return new TypeRef(TypeKind.INTERSECTION, "&", "&", bounds, false, false, null, false);
    }

    private TypeRef resolveUnion(UnionType type) {
        List<TypeRef> bounds = new ArrayList<>();
        for (TypeMirror bound : type.getAlternatives()) {
            bounds.add(resolve(bound));
        }
        return new TypeRef(TypeKind.UNION, "|", "|", bounds, false, false, null, false);
    }

    private String getQualifiedName(Element element) {
        Element current = element;
        StringBuilder name = new StringBuilder();
        while (current != null && current.getKind() != ElementKind.PACKAGE) {
            if (current instanceof TypeElement typeElement) {
                if (!name.isEmpty()) {
                    name.insert(0, ".");
                }
                name.insert(0, typeElement.getSimpleName());
            }
            current = current.getEnclosingElement();
        }
        if (current instanceof PackageElement pkg) {
            String pkgName = pkg.getQualifiedName().toString();
            if (!pkgName.isEmpty()) {
                name.insert(0, pkgName + ".");
            }
        }
        return name.toString();
    }

    private String getDisplayName(Element element) {
        StringBuilder name = new StringBuilder();
        Element current = element;
        while (current != null && current.getKind() != ElementKind.PACKAGE) {
            if (current instanceof TypeElement typeElement) {
                if (!name.isEmpty()) {
                    name.insert(0, ".");
                }
                name.insert(0, typeElement.getSimpleName());
            }
            current = current.getEnclosingElement();
        }
        return name.toString();
    }

    @Override
    public String format(TypeRef type, TargetLanguage target) {
        return format(type, target, false);
    }

    @Override
    public String format(TypeRef type, TargetLanguage target, boolean paramType) {
        if (type == null) {
            return "any";
        }
        return switch (target) {
            case TYPESCRIPT -> formatTypeScript(type, paramType);
            case PYTHON -> formatPython(type);
            case MARKDOWN, HTML -> formatJavaLike(type);
        };
    }

    private String formatTypeScript(TypeRef type, boolean paramType) {
        return switch (type.kind()) {
            case PRIMITIVE -> switch (type.name()) {
                case "boolean" -> "boolean";
                case "byte", "short", "int", "long", "float", "double", "char" ->
                    paramType ? type.name() : "number";
                default -> "number";
            };
            case VOID -> "void";
            case ARRAY -> {
                String component = formatTypeScript(type.typeArgs().get(0), paramType);
                yield paramType ? component + "[]" : "JavaArray<" + component + ">";
            }
            case DECLARED -> {
                String alias = typeScriptAliases.get(type.qualifiedName());
                if (alias != null) {
                    yield alias;
                }

                if (type.qualifiedName().startsWith("net.minecraft.")) {
                    yield maskMinecraftType(type, paramType);
                }

                if (paramType && FUNCTIONAL_INTERFACES.containsKey(type.qualifiedName())) {
                    yield formatFunctionalInterface(type, paramType);
                }

                String base = mapDeclaredTypeScript(type, paramType);
                if (!type.typeArgs().isEmpty()) {
                    base = base + "<" + joinTypes(type.typeArgs(), TargetLanguage.TYPESCRIPT, paramType) + ">";
                }
                yield base;
            }
            case TYPEVAR -> type.name();
            case WILDCARD -> "any";
            case INTERSECTION -> "(" + joinTypes(type.typeArgs(), TargetLanguage.TYPESCRIPT, paramType, " & ") + ")";
            case UNION -> "(" + joinTypes(type.typeArgs(), TargetLanguage.TYPESCRIPT, paramType, " | ") + ")";
            default -> "any";
        };
    }

    private String mapDeclaredTypeScript(TypeRef type, boolean paramType) {
        String qualifiedName = type.qualifiedName();
        if ("com.jsmacrosce.jsmacros.core.event.BaseEvent".equals(qualifiedName)) {
            return "Events.BaseEvent";
        }
        if (JAVA_NUMBER_TYPES.containsKey(qualifiedName)) {
            return paramType ? JAVA_NUMBER_TYPES.get(qualifiedName) : "number";
        }
        return switch (qualifiedName) {
            case "java.lang.String" -> "string";
            case "java.lang.Boolean" -> "boolean";
            case "java.lang.Object" -> "any";
            default -> {
                if (JAVA_ALIASES.contains(qualifiedName)) {
                    String base = "Java" + simpleName(qualifiedName);
                    if (paramType && base.equals("JavaClass")) {
                        base = "JavaClassArg";
                    }
                    yield base;
                }
                yield "Packages." + toTsQualified(type);
            }
        };
    }

    private String toTsQualified(TypeRef type) {
        String qualifiedName = type.qualifiedName();
        String name = type.name();
        if (qualifiedName.endsWith(name)) {
            String pkg = qualifiedName.substring(0, qualifiedName.length() - name.length());
            return pkg + name.replace('.', '$');
        }
        return qualifiedName.replace('.', '$');
    }

    private String formatPython(TypeRef type) {
        return switch (type.kind()) {
            case PRIMITIVE -> switch (type.name()) {
                case "boolean" -> "bool";
                case "byte", "short", "int", "long", "char" -> "int";
                case "float", "double" -> "float";
                default -> "int";
            };
            case VOID -> "None";
            case ARRAY -> "list[" + formatPython(type.typeArgs().get(0)) + "]";
            case DECLARED -> {
                String base = mapDeclaredPython(type.qualifiedName());
                if (!type.typeArgs().isEmpty()) {
                    base = base + "[" + joinTypes(type.typeArgs(), TargetLanguage.PYTHON) + "]";
                }
                yield base;
            }
            case TYPEVAR -> type.name();
            case WILDCARD -> "object";
            case INTERSECTION, UNION -> "object";
            default -> "object";
        };
    }

    private String mapDeclaredPython(String qualifiedName) {
        if (!pythonAliasEnabled) {
            return simpleName(qualifiedName);
        }
        return switch (qualifiedName) {
            case "java.lang.String" -> "str";
            case "java.lang.Boolean" -> "bool";
            case "java.lang.Integer", "java.lang.Long", "java.lang.Short", "java.lang.Byte" -> "int";
            case "java.lang.Float", "java.lang.Double" -> "float";
            case "java.lang.Object" -> "object";
            default -> simpleName(qualifiedName);
        };
    }

    private String formatJavaLike(TypeRef type) {
        return switch (type.kind()) {
            case PRIMITIVE, VOID, TYPEVAR -> type.name();
            case ARRAY -> formatJavaLike(type.typeArgs().get(0)) + "[]";
            case DECLARED -> {
                String base = simpleName(type.qualifiedName());
                if (!type.typeArgs().isEmpty()) {
                    base = base + "<" + joinTypes(type.typeArgs(), TargetLanguage.MARKDOWN) + ">";
                }
                yield base;
            }
            case WILDCARD -> "?";
            case INTERSECTION -> "(" + joinTypes(type.typeArgs(), TargetLanguage.MARKDOWN, false, " & ") + ")";
            case UNION -> "(" + joinTypes(type.typeArgs(), TargetLanguage.MARKDOWN, false, " | ") + ")";
            default -> type.name();
        };
    }

    private String joinTypes(List<TypeRef> types, TargetLanguage target) {
        return joinTypes(types, target, false);
    }

    private String joinTypes(List<TypeRef> types, TargetLanguage target, boolean paramType) {
        return joinTypes(types, target, paramType, ", ");
    }

    private String joinTypes(List<TypeRef> types, TargetLanguage target, boolean paramType, String separator) {
        StringBuilder builder = new StringBuilder();
        for (TypeRef ref : types) {
            builder.append(format(ref, target, paramType)).append(separator);
        }
        if (!types.isEmpty()) {
            builder.setLength(builder.length() - separator.length());
        }
        return builder.toString();
    }

    private String simpleName(String qualifiedName) {
        int idx = qualifiedName.lastIndexOf('.');
        return idx == -1 ? qualifiedName : qualifiedName.substring(idx + 1);
    }

    private String formatFunctionalInterface(TypeRef type, boolean paramType) {
        String template = FUNCTIONAL_INTERFACES.get(type.qualifiedName());
        if (template == null) {
            return formatTypeScript(type, paramType);
        }
        String res = template;
        if (!type.typeArgs().isEmpty()) {
            int size = type.typeArgs().size();
            for (int i = 0; i < size; i++) {
                res = res.replace("$" + i, formatTypeScript(type.typeArgs().get(i), true));
            }
        }
        return res;
    }

    private String maskMinecraftType(TypeRef type, boolean paramType) {
        StringBuilder raw = new StringBuilder(type.qualifiedName());
        if (!type.typeArgs().isEmpty()) {
            raw.append("<").append(joinTypes(type.typeArgs(), TargetLanguage.TYPESCRIPT, paramType)).append(">");
        }
        String cleaned = raw.toString()
            .replace("/* ", "")
            .replace(" */ any", "")
            .replace(" */", "");
        return "/* " + cleaned + " */ any";
    }
}
