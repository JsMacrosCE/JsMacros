package com.jsmacrosce.doclet.core.render;

import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.core.*;
import com.jsmacrosce.doclet.core.model.ClassDoc;
import com.jsmacrosce.doclet.core.model.ClassKind;
import com.jsmacrosce.doclet.core.model.DeclaredTypeDoc;
import com.jsmacrosce.doclet.core.model.DocBodyNode;
import com.jsmacrosce.doclet.core.model.DocComment;
import com.jsmacrosce.doclet.core.model.DocTag;
import com.jsmacrosce.doclet.core.model.DocTagKind;
import com.jsmacrosce.doclet.core.model.DocletModel;
import com.jsmacrosce.doclet.core.model.MemberDoc;
import com.jsmacrosce.doclet.core.model.MemberKind;
import com.jsmacrosce.doclet.core.model.PackageDoc;
import com.jsmacrosce.doclet.core.model.ParamDoc;
import com.jsmacrosce.doclet.core.model.TypeKind;
import com.jsmacrosce.doclet.core.model.TypeRef;
import com.jsmacrosce.doclet.core.util.DocBodyRenderer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


@DocletIgnore
public class TsRenderer implements Renderer {
    private static final Set<String> TS_RESERVED_WORDS = Set.of(
        "break", "case", "catch", "class", "const", "continue", "debugger", "default",
        "delete", "do", "else", "enum", "export", "extends", "false", "finally", "for",
        "function", "if", "import", "in", "instanceof", "new", "null", "return", "super",
        "switch", "this", "throw", "true", "try", "typeof", "var", "void", "while", "with"
    );
    private final TypeResolver typeResolver;

    private Set<String> referencedTypes = Set.of();
    /** The types the definitions write as interfaces, which a class implements rather than extends. */
    private Set<String> interfaceTypes = Set.of();

    public TsRenderer(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Override
    public String render(DocletModel model) {
        Map<String, String> tsAliases = buildTypeScriptAliases(model);
        applyTypeScriptAliases(tsAliases);
        referencedTypes = new HashSet<>();
        for (ClassDoc clz : model.referencedTypes()) {
            referencedTypes.add(clz.qualifiedName());
        }

        List<ClassDoc> events = collectByGroup(model, ClassGroup.Event);
        List<ClassDoc> libraries = collectByGroup(model, ClassGroup.Library);

        if (typeResolver instanceof BasicTypeResolver basicTypeResolver) {
            basicTypeResolver.setUndeclaredTypes(model.undeclaredTypes());
        }
        interfaceTypes = new HashSet<>();
        for (ClassDoc clz : events) {
            interfaceTypes.add(clz.qualifiedName());
        }
        for (ClassDoc clz : model.referencedTypes()) {
            if (clz.kind() == ClassKind.INTERFACE) {
                interfaceTypes.add(clz.qualifiedName());
            }
        }

        StringBuilder out = new StringBuilder();
        appendGlobalDeclarations(out);
        appendEvents(out, events);
        appendEventFilterers(out, events);
        appendEventTypeMap(out, events);
        appendLibraries(out, libraries);
        appendScriptNamespaces(out, model);

        out.append("\n\ndeclare ").append(renderPackages(model)).append("\n");
        appendAliasTypes(out, model);
        appendEnumTypes(out, model.declaredTypes());

        return out.toString();
    }

    private void appendGlobalDeclarations(StringBuilder out) {
        out.append("/**\n");
        out.append(" * The global context\n");
        out.append(" * If you're trying to access the context in {@link JsMacros.on},\n");
        out.append(" * use the second param of callback\n");
        out.append(" */\n");
        out.append("declare const context: EventContainer;\n");
        out.append("/**\n");
        out.append(" * Assert and convert event type:\n");
        out.append(" * ```js\n");
        out.append(" * JsMacros.assertEvent(event, 'Service')\n");
        out.append(" * ```\n");
        out.append(" * If the type doesn't convert, that means the event type doesn't have any properties\n");
        out.append(" */\n");
        out.append("declare const event: Events.BaseEvent;\n");
        out.append("declare const file: Packages.java.io.File;\n\n");
    }

    private void appendScriptNamespaces(StringBuilder out, DocletModel model) {
        for (var namespace : model.scriptNamespaces()) {
            appendDocComment(out, namespace.docComment(), List.of(), false, null, null, 0);
            out.append("declare namespace ").append(namespace.name()).append(" {\n");
            for (var export : namespace.exports()) {
                TypeRef target = export.target();
                // Use the full Java type here so another short name cannot change what this refers to.
                String canonical = "Packages." + target.qualifiedName().substring(0,
                        target.qualifiedName().length() - target.name().length()) + target.name().replace('.', '$');
                String args = export.typeParams().isEmpty() ? "" : "<" + export.typeParams().stream()
                        .map(TypeRef::name).collect(java.util.stream.Collectors.joining(", ")) + ">";
                String defaults = export.typeParams().isEmpty() ? "" : "<" + export.typeParams().stream()
                        .map(p -> formatTypeParam(p, true)).collect(java.util.stream.Collectors.joining(", ")) + ">";
                out.append("    const ").append(export.name()).append(": ");
                if (export.interfaceType()) {
                    // Java lets scripts check against interfaces and use their static members.
                    // Describe those uses for TypeScript without suggesting interfaces can be constructed.
                    String anyArgs = export.typeParams().isEmpty() ? "" : "<" + export.typeParams().stream()
                            .map(p -> "any").collect(java.util.stream.Collectors.joining(", ")) + ">";
                    String instance = canonical + anyArgs;
                    out.append("{ readonly class: JavaClass<").append(instance).append(">; ")
                            .append("[Symbol.hasInstance](value: unknown): value is ").append(instance).append(";\n");
                    ClassDoc owner = model.packages().stream().flatMap(p -> p.classes().stream())
                            .filter(c -> c.qualifiedName().equals(target.qualifiedName())).findFirst().orElseThrow();
                    Set<String> merged = mergedMemberNames(owner);
                    for (MemberDoc member : owner.members()) {
                        if (!hasModifier(member, "static")) continue;
                        if (member.kind() == MemberKind.FIELD) renderField(out, member, 2, false, owner, merged);
                        if (member.kind() == MemberKind.METHOD) renderMethod(out, member, 2, false, owner);
                    }
                    out.append("    }");
                } else {
                    out.append("typeof ").append(canonical);
                }
                // Declare both uses of the name: instanceof/new in code, and type annotations in editors.
                out.append(";\n    type ").append(export.name()).append(defaults)
                        .append(" = ").append(canonical).append(args).append(";\n");
            }
            out.append("}\n\n");
        }
    }

    private void appendEvents(StringBuilder out, List<ClassDoc> events) {
        out.append("declare namespace Events {\n\n");
        // The type every event builds on is written as a class, because the event classes
        // build on it and inherit what it carries.
        indent(out, 1).append("class BaseEvent extends Packages.java.lang.Object {\n\n");
        indent(out, 2).append("getEventName(): string;\n\n");
        indent(out, 1).append("}\n\n");
        indent(out, 1).append("interface Cancellable {\n\n");
        indent(out, 2).append("cancel(): void;\n\n");
        indent(out, 1).append("}\n");

        for (ClassDoc event : events) {
            out.append("\n");
            renderEventInterface(out, event, 1);
        }
        out.append("\n}\n");
    }

    private void appendEventFilterers(StringBuilder out, List<ClassDoc> events) {
        out.append("\ninterface EventFilterers {\n");
        for (ClassDoc event : events) {
            if (event.eventFilterer() == null || event.eventFilterer().isBlank()) {
                continue;
            }
            indent(out, 1).append(event.alias()).append(": ").append(event.eventFilterer()).append(";\n");
        }
        out.append("}\n");
    }

    private void appendEventTypeMap(StringBuilder out, List<ClassDoc> events) {
        out.append("\ninterface Events {\n");
        for (ClassDoc event : events) {
            indent(out, 1).append(event.alias()).append(": Events.").append(event.alias()).append(";\n");
        }
        out.append("}\n");
    }

    private void appendLibraries(StringBuilder out, List<ClassDoc> libraries) {
        for (ClassDoc library : libraries) {
            out.append("\n");
            renderLibraryNamespace(out, library, 0);
        }
    }

    private void renderLibraryNamespace(StringBuilder out, ClassDoc library, int indent) {
        appendDocComment(out, library.docComment(), List.of(), false, null, library, indent);
        indent(out, indent).append("declare namespace ").append(library.alias()).append(" {\n");
        for (MemberDoc member : library.members()) {
            if (member.kind() != MemberKind.METHOD || hasModifier(member, "static")) {
                continue;
            }
            renderMethod(out, member, indent + 1, false, library, true);
        }
        indent(out, indent).append("}\n");
    }

    private void renderEventInterface(StringBuilder out, ClassDoc event, int indent) {
        appendDocComment(out, event.docComment(), List.of(), false, null, event, indent);
        indent(out, indent).append("interface ").append(event.alias());
        appendTypeParams(out, typeParams(event), null, true);
        out.append(" extends BaseEvent");
        if (event.eventCancellable()) {
            out.append(", Cancellable");
        }
        out.append(" {\n");
        Set<String> merged = mergedMemberNames(event);
        for (MemberDoc member : event.members()) {
            if (member.kind() == MemberKind.FIELD && !hasModifier(member, "static")) {
                renderField(out, member, indent + 1, false, event, merged);
            } else if (member.kind() == MemberKind.METHOD && !hasModifier(member, "static")) {
                if (merged.contains(member.name())) {
                    continue;
                }
                renderMethod(out, member, indent + 1, false, event);
            }
        }
        indent(out, indent).append("}\n");
    }

    /**
     * The names of members that are both a field and a method of the same class. TypeScript
     * cannot declare both under one name, so such a pair is presented as a single member whose
     * type carries both readings.
     */
    private Set<String> mergedMemberNames(ClassDoc clz) {
        Set<String> fields = new HashSet<>();
        Set<String> merged = new HashSet<>();
        for (MemberDoc member : clz.members()) {
            if (member.kind() == MemberKind.FIELD) {
                fields.add(member.name());
            } else if (member.kind() == MemberKind.METHOD && fields.contains(member.name())) {
                merged.add(member.name());
            }
        }
        return merged;
    }

    private MemberDoc mergedMethod(ClassDoc clz, String name) {
        for (MemberDoc member : clz.members()) {
            if (member.kind() == MemberKind.METHOD && member.name().equals(name)) {
                return member;
            }
        }
        return null;
    }

    private String renderPackages(DocletModel model) {
        PackageNode root = buildTree(model);
        PackageNode current = root;
        StringBuilder name = new StringBuilder(root.name);
        while (current.classes.isEmpty() && current.children.size() == 1) {
            PackageNode child = current.children.values().iterator().next();
            if (isReservedWord(child.name)) {
                break;
            }
            name.append(".").append(child.name);
            current = child;
        }
        StringBuilder out = new StringBuilder();
        out.append("namespace ").append(name).append(" {\n");
        renderPackageNode(out, current, 1);
        out.append("}");
        return out.toString();
    }

    private PackageNode buildTree(DocletModel model) {
        PackageNode root = new PackageNode("Packages");
        for (PackageDoc pkg : model.packages()) {
            for (ClassDoc clz : pkg.classes()) {
                root.addClass(pkg.name(), clz);
            }
        }
        // A type the documented API mentions but does not document still has to exist, or a
        // reference to it resolves to nothing.
        for (ClassDoc clz : model.referencedTypes()) {
            root.addClass(clz.packageName(), clz);
        }
        return root;
    }

    private void renderPackageNode(StringBuilder out, PackageNode node, int indent) {
        for (PackageNode child : node.children.values()) {
            PackageNode current = child;
            StringBuilder mergedName = new StringBuilder(child.name);
            while (current.classes.isEmpty() && current.children.size() == 1) {
                PackageNode onlyChild = current.children.values().iterator().next();
                if (isReservedWord(onlyChild.name)) {
                    break;
                }
                mergedName.append(".").append(onlyChild.name);
                current = onlyChild;
            }
            String namespaceName = escapePackageName(mergedName.toString());
            indent(out, indent).append("export namespace ").append(namespaceName).append(" {\n");
            renderPackageNode(out, current, indent + 1);
            indent(out, indent).append("}\n");
            if (isReservedWord(child.name)) {
                indent(out, indent).append("export { ").append(namespaceName).append(" as ")
                    .append(child.name).append(" };\n");
            }
        }
        for (ClassDoc clz : node.classes) {
            renderClass(out, clz, indent);
        }
    }

    private void renderClass(StringBuilder out, ClassDoc clz, int indent) {
        Set<String> merged = mergedMemberNames(clz);
        if (isReferencedType(clz)) {
            renderReferencedClass(out, clz, indent, merged);
            return;
        }
        if (clz.kind() == ClassKind.INTERFACE) {
            appendDocComment(out, clz.docComment(), List.of(), false, null, clz, indent);
            appendShapeIgnore(out, clz, indent);
            indent(out, indent).append("export abstract class ").append(tsSafeName(clz.name()));
            appendTypeParams(out, typeParams(clz), null, true);
            indent(out, indent).append(" extends java.lang.Interface {\n");
            appendClassHeader(out, clz, indent + 1);
            renderStaticMembers(out, clz, indent + 1, clz, merged);
            indent(out, indent).append("}\n");

            appendHeritageIgnore(out, clz, indent);
            indent(out, indent).append("export interface ").append(tsSafeName(clz.name()));
            appendTypeParams(out, typeParams(clz), null, true);
            appendImplements(out, clz);
            out.append(" {\n");
            renderInstanceMembers(out, clz, indent + 1, clz, merged);
            indent(out, indent).append("}\n");
            return;
        }

        // Don't generate interface for Library classes (they're rendered as namespaces)
        if (!clz.implementsTypes().isEmpty() && clz.group() != ClassGroup.Library) {
            appendDocComment(out, clz.docComment(), List.of(), false, null, clz, indent);
            appendHeritageIgnore(out, clz, indent);
            indent(out, indent).append("export interface ").append(tsSafeName(clz.name()));
            appendTypeParams(out, typeParams(clz), null, true);
            appendImplements(out, clz);
            out.append(" {}\n");
        } else {
            appendDocComment(out, clz.docComment(), List.of(), false, null, clz, indent);
        }

        boolean hasConstructor = clz.members().stream().anyMatch(member -> member.kind() == MemberKind.CONSTRUCTOR);
        appendShapeIgnore(out, clz, indent);
        indent(out, indent).append("export ").append(hasConstructor ? "class " : "abstract class ")
            .append(tsSafeName(clz.name()));
        appendTypeParams(out, typeParams(clz), null, true);
        appendExtends(out, clz);
        out.append(" {\n");
        appendClassHeader(out, clz, indent + 1);
        renderStaticMembers(out, clz, indent + 1, clz, merged);
        renderConstructors(out, clz, indent + 1);
        renderInstanceMembers(out, clz, indent + 1, clz, merged);
        indent(out, indent).append("}\n");
    }

    /**
     * A type the documented API mentions but does not document is declared on its own: an
     * interface stays an interface and a class stays a class. The two halves the documented
     * classes are given do not fit a type that is only there to be referred to.
     */
    private void renderReferencedClass(StringBuilder out, ClassDoc clz, int indent, Set<String> merged) {
        appendInternalNotice(out, clz, indent);
        if (clz.kind() == ClassKind.INTERFACE) {
            appendHeritageIgnore(out, clz, indent);
            appendShapeIgnore(out, clz, indent);
            indent(out, indent).append("export interface ").append(tsSafeName(clz.name()));
            appendTypeParams(out, typeParams(clz), null, true);
            appendImplements(out, clz);
            out.append(" {\n");
            renderInstanceMembers(out, clz, indent + 1, clz, merged);
            indent(out, indent).append("}\n");
            return;
        }
        appendShapeIgnore(out, clz, indent);
        indent(out, indent).append("export class ").append(tsSafeName(clz.name()));
        appendTypeParams(out, typeParams(clz), null, true);
        appendExtends(out, clz);
        out.append(" {\n");
        renderStaticMembers(out, clz, indent + 1, clz, merged);
        renderInstanceMembers(out, clz, indent + 1, clz, merged);
        indent(out, indent).append("}\n");
    }

    /**
     * Marks a type that is not part of the API a script is given. It is declared only because
     * a class of the API builds on it, and the marker keeps it out of the API of a script.
     */
    private void appendInternalNotice(StringBuilder out, ClassDoc clz, int indent) {
        if (!clz.internal()) {
            return;
        }
        indent(out, indent).append("/**\n");
        indent(out, indent).append(" * Not part of the API a script is given. It is here because a class of\n");
        indent(out, indent).append(" * the API builds on it, and that class cannot be written without it.\n");
        indent(out, indent).append(" * @internal\n");
        indent(out, indent).append(" */\n");
    }

    private boolean isReferencedType(ClassDoc clz) {
        return referencedTypes.contains(clz.qualifiedName());
    }

    private void renderField(
        StringBuilder out,
        MemberDoc member,
        int indent,
        boolean includeStatic,
        ClassDoc owner,
        Set<String> merged
    ) {
        MemberDoc method = merged.contains(member.name()) ? mergedMethod(owner, member.name()) : null;
        appendDocComment(out, member.docComment(), List.of(), false, member, owner, indent);
        if (method != null) {
            appendDocComment(out, method.docComment(), List.of(), false, method, owner, indent);
        }
        appendShapeIgnore(out, member, indent);
        indent(out, indent);
        if (includeStatic && hasModifier(member, "static")) {
            out.append("static ");
        }
        if (member.isReadOnly()) {
            out.append("readonly ");
        }
        out.append(member.name()).append(": ");
        out.append(method == null
            ? formatReturn(member, owner)
            : mergedMemberType(member, method, owner));
        out.append(";\n");
    }

    /**
     * Marks the declaration of a type whose types it builds on cannot be written together, so
     * that the mismatch they cause is not reported against the definitions.
     */
    private void appendHeritageIgnore(StringBuilder out, ClassDoc clz, int indent) {
        if (clz.heritageConflict()) {
            indent(out, indent).append("// @ts-ignore\n");
        }
    }

    /**
     * Marks the declaration of a type whose shape the definitions cannot write, so that the
     * mismatch the shape causes is not reported against the definitions.
     */
    private void appendShapeIgnore(StringBuilder out, ClassDoc clz, int indent) {
        if (hasStaticShapeConflict(clz)) {
            indent(out, indent).append("// @ts-ignore\n");
        }
    }

    /**
     * Whether the static side of the type cannot be written the way the definitions write it,
     * because a static member answers to the same name on the type it builds on with a
     * different shape.
     */
    private boolean hasStaticShapeConflict(ClassDoc clz) {
        return clz.staticConflict();
    }

    /**
     * Marks a member whose shape the definitions cannot write, so that the mismatch the shape
     * causes is not reported against the definitions. The member is still there, with its type.
     */
    private void appendShapeIgnore(StringBuilder out, MemberDoc member, int indent) {
        if (member.shapeConflict()) {
            indent(out, indent).append("// @ts-ignore\n");
        }
    }

    /**
     * The type of a member that is both a field and a method. Reading it gives the field's
     * value and calling it gives the method's result, which is what the runtime does with such
     * a pair, so the type carries both readings.
     */
    private String mergedMemberType(MemberDoc field, MemberDoc method, ClassDoc owner) {
        StringBuilder builder = new StringBuilder(formatType(field.returnType(), false, true));
        builder.append(" & ((");
        List<ParamDoc> params = method.params();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            ParamDoc param = params.get(i);
            builder.append(escapeParamName(param.name())).append(": ")
                .append(formatType(param.type(), true, true));
        }
        builder.append(") => ").append(formatReturn(method, owner)).append(")");
        return builder.toString();
    }

    private void renderMethod(StringBuilder out, MemberDoc member, int indent, boolean includeStatic, ClassDoc owner) {
        renderMethod(out, member, indent, includeStatic, owner, false);
    }

    private void renderMethod(StringBuilder out, MemberDoc member, int indent, boolean includeStatic, ClassDoc owner, boolean namespace) {
        appendDocComment(out, member.docComment(), member.params(), true, member, owner, indent);
        appendShapeIgnore(out, member, indent);
        indent(out, indent);
        if (namespace) {
            out.append("function ");
        }
        if (includeStatic && hasModifier(member, "static")) {
            out.append("static ");
        }
        out.append(member.name());
        appendTypeParams(out, member.typeParams(), member.replaceTypeParams(), false);
        out.append("(");
        if (member.replaceParams() != null && !member.replaceParams().isBlank()) {
            String replaced = member.replaceParams();
            if (namespace) {
                // Replacement text can include additional overloads (e.g. Reflection.getClass).
                // Each declaration inside a namespace needs its own function keyword.
                replaced = replaced.replaceAll(
                    "(?m)^[\\t ]*(?=" + java.util.regex.Pattern.quote(member.name()) + "(?:<|\\())",
                    "    ".repeat(Math.max(0, indent)) + "function "
                );
            }
            // Check if replaceParams contains complete declaration(s) with overloads
            if (replaced.contains(");")) {
                // It's a complete declaration with overloads, output as-is
                out.append(replaced);
                if (!replaced.endsWith("\n")) {
                    out.append("\n");
                }
                return;
            }
            // Otherwise it's just parameter replacement
            out.append(replaced);
        } else {
            appendParams(out, member.params());
        }
        out.append(")");
        out.append(": ").append(formatReturn(member, owner));
        out.append(";\n");
    }

    private void renderConstructor(StringBuilder out, MemberDoc member, int indent, ClassDoc owner) {
        appendDocComment(out, member.docComment(), member.params(), false, member, owner, indent);
        indent(out, indent).append("constructor");
        appendTypeParams(out, member.typeParams(), member.replaceTypeParams(), false);
        out.append("(");
        if (member.replaceParams() != null && !member.replaceParams().isBlank()) {
            out.append(member.replaceParams());
        } else {
            appendParams(out, member.params());
        }
        out.append(");\n");
    }

    private void renderStaticMembers(StringBuilder out, ClassDoc clz, int indent, ClassDoc owner, Set<String> merged) {
        for (MemberDoc member : clz.members()) {
            if (!hasModifier(member, "static")) {
                continue;
            }
            if (member.kind() == MemberKind.FIELD) {
                renderField(out, member, indent, true, owner, merged);
            } else if (member.kind() == MemberKind.METHOD) {
                if (merged.contains(member.name())) {
                    continue;
                }
                renderMethod(out, member, indent, true, owner);
            }
        }
    }

    private void renderConstructors(StringBuilder out, ClassDoc clz, int indent) {
        for (MemberDoc member : clz.members()) {
            if (member.kind() == MemberKind.CONSTRUCTOR) {
                renderConstructor(out, member, indent, clz);
            }
        }
    }

    private void renderInstanceMembers(StringBuilder out, ClassDoc clz, int indent, ClassDoc owner, Set<String> merged) {
        for (MemberDoc member : clz.members()) {
            if (hasModifier(member, "static")) {
                continue;
            }
            if (member.kind() == MemberKind.FIELD) {
                renderField(out, member, indent, false, owner, merged);
            } else if (member.kind() == MemberKind.METHOD) {
                if (merged.contains(member.name())) {
                    continue;
                }
                renderMethod(out, member, indent, false, owner);
            }
        }
    }

    private void appendParams(StringBuilder out, List<ParamDoc> params) {
        boolean first = true;
        for (ParamDoc param : params) {
            if (!first) {
                out.append(", ");
            }
            first = false;
            if (param.varArgs()) {
                out.append("...");
            }
            String name = escapeParamName(param.name());
            out.append(name).append(": ");
            if (param.varArgs()) {
                String baseType = formatType(param.type(), true, false);
                if (baseType.endsWith("[]")) {
                    baseType = baseType.substring(0, baseType.length() - "[]".length());
                }
                if (param.type().nullable()) {
                    baseType = baseType + " | null";
                }
                out.append("JavaVarArgs<").append(baseType).append(">");
            } else {
                out.append(formatType(param.type(), true, true));
            }
        }
    }

    private void appendTypeParams(
        StringBuilder out,
        List<TypeRef> typeParams,
        String replaceTypeParams,
        boolean defaultToAny
    ) {
        if (replaceTypeParams != null && !replaceTypeParams.isBlank()) {
            out.append("<").append(replaceTypeParams).append(">");
            return;
        }
        if (typeParams == null || typeParams.isEmpty()) {
            return;
        }
        out.append("<");
        boolean first = true;
        for (TypeRef param : typeParams) {
            if (!first) {
                out.append(", ");
            }
            first = false;
            out.append(formatTypeParam(param, defaultToAny));
        }
        out.append(">");
    }

    private String formatTypeParam(TypeRef param, boolean defaultToAny) {
        StringBuilder out = new StringBuilder(param.name());
        String ext = param.bounds() == null ? "any" : formatType(param.bounds(), false, false);
        if (!ext.endsWith("any")) {
            out.append(" extends ").append(ext);
            if (defaultToAny) {
                out.append(" = any");
            }
        } else if (ext.startsWith("/* net.minecraft.")) {
            out.append(" = ").append(ext);
        } else if (defaultToAny) {
            out.append(" = any");
        }
        return out.toString();
    }

    private String formatReturn(MemberDoc member, ClassDoc owner) {
        if (member.replaceReturn() != null && !member.replaceReturn().isBlank()) {
            return member.replaceReturn();
        }
        if (shouldReturnThis(member, owner)) {
            return "this";
        }
        return formatType(member.returnType(), false, true);
    }

    private String formatType(TypeRef type, boolean paramType, boolean includeNull) {
        String formatted = typeResolver.format(type, TargetLanguage.TYPESCRIPT, paramType);
        if (includeNull && type.nullable()) {
            return formatted + " | null";
        }
        return formatted;
    }

    private void appendAliasTypes(StringBuilder out, DocletModel model) {
        List<ClassDoc> wagClasses = new ArrayList<>();
        for (PackageDoc pkg : model.packages()) {
            if (pkg.name().startsWith("com.jsmacrosce")) {
                wagClasses.addAll(pkg.classes());
            }
        }
        wagClasses.sort(Comparator.comparing(ClassDoc::name, String.CASE_INSENSITIVE_ORDER));
        Set<String> seen = new HashSet<>();
        Set<String> aliases = new TreeSet<>();
        for (ClassDoc clz : wagClasses) {
            if (!seen.add(clz.name())) {
                continue;
            }
            String aliasName = buildAliasName(clz, true);
            String aliasValue = formatClassType(clz);
            aliases.add("type " + aliasName + " = " + aliasValue + ";");
        }
        for (String alias : aliases) {
            out.append("\n").append(alias);
        }
    }

    private String buildAliasName(ClassDoc clz, boolean defaultToAny) {
        StringBuilder out = new StringBuilder(clz.name().replace(".", "$"));
        List<TypeRef> params = typeParams(clz);
        if (!params.isEmpty()) {
            out.append("<");
            boolean first = true;
            for (TypeRef param : params) {
                if (!first) {
                    out.append(", ");
                }
                first = false;
                out.append(formatTypeParam(param, defaultToAny));
            }
            out.append(">");
        }
        return out.toString();
    }

    private String formatClassType(ClassDoc clz) {
        TypeRef classType = new TypeRef(
            TypeKind.DECLARED,
            clz.name(),
            clz.qualifiedName(),
            typeParams(clz),
            false,
            false,
            null,
            false
        );
        return typeResolver.format(classType, TargetLanguage.TYPESCRIPT, false);
    }

    /**
     * The type parameters a class is written with: the ones it carries from the class it is
     * written inside, and the ones it declares itself. The ones it carries come first, the way
     * the class they come from is written inside the ones above it.
     */
    private List<TypeRef> typeParams(ClassDoc clz) {
        List<TypeRef> params = new ArrayList<>();
        if (clz.capturedTypeParams() != null) {
            params.addAll(clz.capturedTypeParams());
        }
        if (clz.typeParams() != null) {
            params.addAll(clz.typeParams());
        }
        return params;
    }

    private void appendEnumTypes(StringBuilder out, List<DeclaredTypeDoc> declaredTypes) {
        out.append(
            """

            // Enum types
            type Bit    = 1 | 0;
            type Trit   = 2 | Bit;
            type Dit    = 3 | Trit;
            type Pentit = 4 | Dit;
            type Hexit  = 5 | Pentit;
            type Septit = 6 | Hexit;
            type Octit  = 7 | Septit;

            type Side = Hexit;
            type HotbarSlot = Octit | 8;
            type HotbarSwapSlot = HotbarSlot | OffhandSlot;
            type ClickSlotButton = HotbarSwapSlot | 9 | 10;
            type OffhandSlot = 40;

            """
        );

        for (DeclaredTypeDoc decl : declaredTypes) {
            out.append("type ").append(decl.name()).append(" = ").append(decl.type());
            if (!decl.type().contains("\n")) {
                out.append(";");
            }
            out.append("\n");
        }
    }

    private List<ClassDoc> collectByGroup(DocletModel model, ClassGroup group) {
        List<ClassDoc> result = new ArrayList<>();
        for (PackageDoc pkg : model.packages()) {
            for (ClassDoc clz : pkg.classes()) {
                if (clz.group() == group && clz.alias() != null) {
                    result.add(clz);
                }
            }
        }
        result.sort(
            Comparator.comparing(ClassDoc::alias, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(ClassDoc::name, String.CASE_INSENSITIVE_ORDER)
        );
        return result;
    }

    private Map<String, String> buildTypeScriptAliases(DocletModel model) {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("com.jsmacrosce.jsmacros.core.event.BaseEvent", "Events.BaseEvent");
        for (PackageDoc pkg : model.packages()) {
            for (ClassDoc clz : pkg.classes()) {
                if (clz.group() == ClassGroup.Event && clz.alias() != null) {
                    aliases.put(clz.qualifiedName(), "Events." + clz.alias());
                } else if (clz.group() == ClassGroup.Library && clz.alias() != null) {
                    aliases.put(clz.qualifiedName(), "typeof " + clz.alias());
                }
            }
        }
        return aliases;
    }

    private void applyTypeScriptAliases(Map<String, String> aliases) {
        // TODO: This ignores all other aliases?
        if (typeResolver instanceof BasicTypeResolver basicTypeResolver) {
            basicTypeResolver.setTypeScriptAliases(aliases);
        }
    }

    private StringBuilder indent(StringBuilder out, int indent) {
        out.append("    ".repeat(Math.max(0, indent)));
        return out;
    }

    private boolean hasModifier(MemberDoc member, String modifier) {
        return member.modifiers().contains(modifier);
    }

    private String tsSafeName(String name) {
        return name.replace('.', '$');
    }

    private String escapeParamName(String name) {
        if (isReservedWord(name)) {
            return "_" + name;
        }
        return name;
    }

    private String escapePackageName(String name) {
        if (isReservedWord(name)) {
            return "_" + name;
        }
        return name;
    }

    private boolean isReservedWord(String name) {
        return TS_RESERVED_WORDS.contains(name);
    }

    private void appendClassHeader(StringBuilder out, ClassDoc clz, int indent) {
        indent(out, indent).append("static readonly class: JavaClass<")
            .append(buildClassHeaderType(clz)).append(">;\n");
        indent(out, indent).append("/** @deprecated */ static prototype: undefined;\n");
    }

    private String buildClassHeaderType(ClassDoc clz) {
        StringBuilder builder = new StringBuilder(tsSafeName(clz.name()));
        int params = typeParams(clz).size();
        if (params > 0) {
            builder.append("<");
            builder.append("any, ".repeat(params));
            builder.setLength(builder.length() - 2);
            builder.append(">");
        }
        return builder.toString();
    }

    private void appendExtends(StringBuilder out, ClassDoc clz) {
        String extendsType;
        boolean isInterface = false;
        if (clz.extendsTypes().isEmpty()) {
            extendsType = "java.lang.Object";
        } else {
            TypeRef supertype = clz.extendsTypes().getFirst();
            extendsType = formatHeritageType(supertype, true);
            isInterface = interfaceTypes.contains(supertype.qualifiedName());
        }
        out.append(isInterface ? " implements " : " extends ").append(extendsType);
    }

    private void appendImplements(StringBuilder out, ClassDoc clz) {
        appendHeritage(out, clz, " extends ");
    }

    private void appendHeritage(StringBuilder out, ClassDoc clz, String keyword) {
        if (clz.implementsTypes().isEmpty()) {
            return;
        }
        out.append(keyword);
        boolean first = true;
        for (TypeRef type : clz.implementsTypes()) {
            if (!first) {
                out.append(", ");
            }
            first = false;
            out.append(formatHeritageType(type, false));
        }
    }

    private String formatHeritageType(TypeRef type, boolean isExtends) {
        String formatted = typeResolver.formatHeritage(type);
        if (isExtends) {
            if (formatted.equals("any") || formatted.equals("JavaObject") || formatted.equals("Object")
                || formatted.equals("void")) {
                return "java.lang.Object";
            }
            if (formatted.startsWith("/* net.minecraft.") && formatted.endsWith("any")) {
                return formatted.substring(0, formatted.length() - "any".length()) + "java.lang.Object";
            }
            return formatted;
        }
        if (formatted.startsWith("/* net.minecraft.") && formatted.endsWith("any")) {
            return formatted.substring(0, formatted.length() - "any".length()) + "JavaObject";
        }
        return formatted;
    }

    private boolean shouldReturnThis(MemberDoc member, ClassDoc owner) {
        if (owner == null || member.kind() != MemberKind.METHOD) {
            return false;
        }
        if (member.selfReturn()) {
            // The member it builds on hands the type back, so this one does too.
            return true;
        }
        if (member.docComment() == null) {
            return false;
        }
        String returnText = getTagText(member.docComment(), DocTagKind.RETURN);
        if (returnText.isBlank()) {
            return false;
        }
        String trimmed = returnText.trim();
        if (!trimmed.equals("self") && !trimmed.startsWith("self ")) {
            return false;
        }
        TypeRef returnType = member.returnType();
        return returnType != null && owner.qualifiedName().equals(returnType.qualifiedName());
    }

    private void appendDocComment(
        StringBuilder out,
        DocComment comment,
        List<ParamDoc> params,
        boolean includeReturn,
        MemberDoc member,
        ClassDoc owner,
        int indent
    ) {
        if (comment == null) {
            return;
        }

        // Render the body via DocBodyRenderer — links become {@link ConvertedSig}.
        List<DocBodyNode> bodyNodes = comment.body().isEmpty() ? comment.summary() : comment.body();
        String desc = DocBodyRenderer.toPlainText(bodyNodes, this::resolveLinkForJsDoc);

        List<String> lines = new ArrayList<>();
        if (!desc.isBlank()) {
            lines.add(desc);
        }

        Set<DocTag> mismatchedTemplateTags = new HashSet<>();
        for (DocTag tag : comment.tags()) {
            if (tag.kind() == DocTagKind.TEMPLATE && tag.name() != null) {
                String replacementName = simpleReplacementTypeParamName(
                    member == null ? null : member.replaceTypeParams()
                );
                if (replacementName != null && !replacementName.equals(tag.name())) {
                    String text = DocBodyRenderer.toPlainText(tag.body(), this::resolveLinkForJsDoc);
                    if (!text.isBlank()) {
                        lines.add("Java type parameter " + tag.name() + ": " + text);
                        mismatchedTemplateTags.add(tag);
                    }
                }
            }
        }

        Map<String, String> paramDocs = new HashMap<>();
        for (DocTag tag : comment.tags()) {
            if (tag.kind() == DocTagKind.PARAM && tag.name() != null) {
                paramDocs.put(tag.name(), DocBodyRenderer.toPlainText(tag.body(), this::resolveLinkForJsDoc));
            }
        }
        for (ParamDoc param : params) {
            String text = paramDocs.get(param.name());
            if (text != null && !text.isBlank()) {
                lines.add("@param " + escapeParamName(param.name()) + " " + text);
            }
        }

        if (includeReturn) {
            String returnText = getTagText(comment, DocTagKind.RETURN);
            if (!returnText.isBlank()) {
                if (returnText.startsWith("{")) {
                    returnText = "{*} " + returnText;
                }
                lines.add("@return " + returnText);
            } else if (shouldReturnThis(member, owner)) {
                lines.add("@return self");
            }
        }

        String since = getTagText(comment, DocTagKind.SINCE);
        if (!since.isBlank()) {
            lines.add("@since " + since);
        }
        String deprecated = getTagText(comment, DocTagKind.DEPRECATED);
        if (hasDeprecatedTag(comment)) {
            lines.add(deprecated.isBlank() ? "@deprecated" : "@deprecated " + deprecated);
        }
        for (DocTag tag : comment.tags()) {
            if (tag.kind() == DocTagKind.SEE) {
                String formatted = DocBodyRenderer.toPlainText(tag.body(), this::resolveLinkForJsDoc);
                if (!formatted.isBlank()) {
                    lines.add("@see " + formatted);
                }
            }
        }
        for (DocTag tag : comment.tags()) {
            if (tag.kind() == DocTagKind.TEMPLATE && tag.name() != null && !mismatchedTemplateTags.contains(tag)) {
                String text = DocBodyRenderer.toPlainText(tag.body(), this::resolveLinkForJsDoc);
                if (!text.isBlank()) {
                    lines.add("@template " + tag.name() + " " + text);
                }
            }
        }
        for (DocTag tag : comment.tags()) {
            if (tag.kind() == DocTagKind.OTHER) {
                String text = DocBodyRenderer.toPlainText(tag.body(), this::resolveLinkForJsDoc);
                if (!text.isBlank()) {
                    lines.add(text);
                }
            }
        }

        if (lines.isEmpty()) {
            return;
        }
        indent(out, indent).append("/**\n");
        for (String line : lines) {
            // Split on newlines to handle multi-line comments
            String[] subLines = line.split("\n");
            for (String subLine : subLines) {
                indent(out, indent).append(" * ").append(subLine).append("\n");
            }
        }
        indent(out, indent).append(" */\n");
    }

    private String simpleReplacementTypeParamName(String replacement) {
        if (replacement == null || replacement.isBlank()) {
            return null;
        }
        // Limit reconciliation to one identifier and an optional simple qualified-identifier bound.
        String identifier = "[A-Za-z_$][A-Za-z0-9_$]*";
        String qualifiedIdentifier = identifier + "(?:\\." + identifier + ")*";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
            "\\s*(" + identifier + ")(?:\\s+extends\\s+" + qualifiedIdentifier + ")?\\s*"
        ).matcher(replacement);
        return matcher.matches() ? matcher.group(1) : null;
    }

    /**
     * Resolves a {@link DocBodyNode.Link} to the JSDoc {@code {@link}} format used
     * in TypeScript declaration files.  Simple Java types map to their TS aliases;
     * Java argument lists select overloads in Javadoc, but are not part of a JSDoc
     * namepath. Strip them only here, leaving explicit labels and other renderers intact.
     * All other signatures are converted via {@link DocBodyRenderer#convertSignature}.
     */
    private String resolveLinkForJsDoc(DocBodyNode.Link link) {
        String sig = link.signature();
        int arguments = sig.indexOf('(');
        if (arguments >= 0 && sig.endsWith(")") && !sig.contains("://")) {
            sig = sig.substring(0, arguments);
        }
        String mapped = DocBodyRenderer.mapSimpleLinkSignature(sig);
        if (mapped != null) {
            return mapped;
        }
        String converted = DocBodyRenderer.convertSignature(sig);
        String label = link.label();
        if (label != null && !label.isBlank() && !label.equals(converted)) {
            return "{@link " + converted + " " + label + "}";
        }
        return "{@link " + converted + "}";
    }

    /**
     * Returns the rendered plain-text of the first tag of {@code kind},
     * or an empty string when absent.
     */
    private String getTagText(DocComment comment, DocTagKind kind) {
        if (comment == null) {
            return "";
        }
        for (DocTag tag : comment.tags()) {
            if (tag.kind() == kind) {
                return DocBodyRenderer.toPlainText(tag.body(), this::resolveLinkForJsDoc);
            }
        }
        return "";
    }

    private boolean hasDeprecatedTag(DocComment comment) {
        if (comment == null) {
            return false;
        }
        return comment.tags().stream().anyMatch(tag -> tag.kind() == DocTagKind.DEPRECATED);
    }

    private static class PackageNode {
        private final String name;
        private final Map<String, PackageNode> children = new TreeMap<>();
        private final List<ClassDoc> classes = new ArrayList<>();

        private PackageNode(String name) {
            this.name = name;
        }

        private void addClass(String packageName, ClassDoc clz) {
            if (packageName == null || packageName.isEmpty()) {
                classes.add(clz);
                return;
            }
            String[] parts = packageName.split("\\.");
            PackageNode current = this;
            for (String part : parts) {
                current = current.children.computeIfAbsent(part, PackageNode::new);
            }
            current.classes.add(clz);
        }
    }
}
