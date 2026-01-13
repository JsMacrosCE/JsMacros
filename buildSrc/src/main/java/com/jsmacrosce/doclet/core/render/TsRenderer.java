package com.jsmacrosce.doclet.core.render;

import com.jsmacrosce.doclet.core.BasicTypeResolver;
import com.jsmacrosce.doclet.core.Renderer;
import com.jsmacrosce.doclet.core.TargetLanguage;
import com.jsmacrosce.doclet.core.TypeResolver;
import com.jsmacrosce.doclet.core.model.ClassDoc;
import com.jsmacrosce.doclet.core.model.ClassKind;
import com.jsmacrosce.doclet.core.model.DeclaredTypeDoc;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;


public class TsRenderer implements Renderer {
    private static final Pattern HTML_LINK =
        Pattern.compile("<a\\s[^>]*?href=\"([^\"]*)\"[^>]*?>(.*?)</a>", Pattern.DOTALL);
    private static final Pattern LINK_TAG =
        Pattern.compile("\\{@link\\s+([^}]+)}");
    private static final Set<String> TS_RESERVED_WORDS = Set.of(
        "break", "case", "catch", "class", "const", "continue", "debugger", "default",
        "delete", "do", "else", "enum", "export", "extends", "false", "finally", "for",
        "function", "if", "import", "in", "instanceof", "new", "null", "return", "super",
        "switch", "this", "throw", "true", "try", "typeof", "var", "void", "while", "with"
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
    private final TypeResolver typeResolver;

    public TsRenderer(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Override
    public String render(DocletModel model) {
        Map<String, String> tsAliases = buildTypeScriptAliases(model);
        applyTypeScriptAliases(tsAliases);

        List<ClassDoc> events = collectByGroup(model, "Event");
        List<ClassDoc> libraries = collectByGroup(model, "Library");

        StringBuilder out = new StringBuilder();
        appendGlobalDeclarations(out);
        appendEvents(out, events);
        appendEventFilterers(out, events);
        appendEventTypeMap(out, events);
        appendLibraries(out, libraries);

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

    private void appendEvents(StringBuilder out, List<ClassDoc> events) {
        out.append("declare namespace Events {\n\n");
        indent(out, 1).append("interface BaseEvent extends JavaObject {\n\n");
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
        DocComment libraryComment = sanitizeLibraryComment(library.docComment());
        appendDocComment(out, libraryComment, List.of(), false, null, library, indent);
        indent(out, indent).append("declare namespace ").append(library.alias()).append(" {\n");
        for (MemberDoc member : library.members()) {
            if (member.kind() != MemberKind.METHOD || hasModifier(member, "static")) {
                continue;
            }
            appendDocComment(out, member.docComment(), member.params(), true, member, library, indent + 1);
            indent(out, indent + 1).append("function ").append(member.name());
            appendTypeParams(out, member.typeParams(), member.replaceTypeParams(), false);
            out.append("(");
            appendParams(out, member.params());
            out.append(")");
            out.append(": ").append(formatReturn(member, library));
            out.append(";\n");
        }
        indent(out, indent).append("}\n");
    }

    private void renderEventInterface(StringBuilder out, ClassDoc event, int indent) {
        appendDocComment(out, event.docComment(), List.of(), false, null, event, indent);
        indent(out, indent).append("interface ").append(event.alias()).append(" extends BaseEvent");
        if (event.eventCancellable()) {
            out.append(", Cancellable");
        }
        out.append(" {\n");
        for (MemberDoc member : event.members()) {
            if (member.kind() == MemberKind.FIELD && !hasModifier(member, "static")) {
                renderField(out, member, indent + 1, false, event);
            } else if (member.kind() == MemberKind.METHOD && !hasModifier(member, "static")) {
                renderMethod(out, member, indent + 1, false, event);
            }
        }
        indent(out, indent).append("}\n");
    }

    private String renderPackages(DocletModel model) {
        PackageNode root = buildTree(model);
        PackageNode current = root;
        String name = root.name;
        while (current.classes.isEmpty() && current.children.size() == 1) {
            PackageNode child = current.children.values().iterator().next();
            if (isReservedWord(child.name)) {
                break;
            }
            name = name + "." + child.name;
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
        return root;
    }

    private void renderPackageNode(StringBuilder out, PackageNode node, int indent) {
        for (PackageNode child : node.children.values()) {
            PackageNode current = child;
            String mergedName = child.name;
            while (current.classes.isEmpty() && current.children.size() == 1) {
                PackageNode onlyChild = current.children.values().iterator().next();
                if (isReservedWord(onlyChild.name)) {
                    break;
                }
                mergedName = mergedName + "." + onlyChild.name;
                current = onlyChild;
            }
            String namespaceName = escapePackageName(mergedName);
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
        if (clz.kind() == ClassKind.INTERFACE) {
            appendDocComment(out, clz.docComment(), List.of(), false, null, clz, indent);
            indent(out, indent).append("export abstract class ").append(tsSafeName(clz.name()))
                .append(" extends java.lang.Interface {\n");
            appendClassHeader(out, clz, indent + 1);
            renderStaticMembers(out, clz, indent + 1, clz);
            indent(out, indent).append("}\n");

            indent(out, indent).append("export interface ").append(tsSafeName(clz.name()));
            appendTypeParams(out, clz.typeParams(), null, false);
            appendImplements(out, clz);
            out.append(" {\n");
            renderInstanceMembers(out, clz, indent + 1, clz);
            indent(out, indent).append("}\n");
            return;
        }

        // Don't generate interface for Library classes (they're rendered as namespaces)
        if (!clz.implementsTypes().isEmpty() && !"Library".equals(clz.group())) {
            appendDocComment(out, clz.docComment(), List.of(), false, null, clz, indent);
            indent(out, indent).append("export interface ").append(tsSafeName(clz.name()));
            appendTypeParams(out, clz.typeParams(), null, false);
            appendImplements(out, clz);
            out.append(" {}\n");
        } else {
            appendDocComment(out, clz.docComment(), List.of(), false, null, clz, indent);
        }

        boolean hasConstructor = clz.members().stream().anyMatch(member -> member.kind() == MemberKind.CONSTRUCTOR);
        indent(out, indent).append("export ").append(hasConstructor ? "class " : "abstract class ")
            .append(tsSafeName(clz.name()));
        appendTypeParams(out, clz.typeParams(), null, false);
        appendExtends(out, clz);
        out.append(" {\n");
        appendClassHeader(out, clz, indent + 1);
        renderStaticMembers(out, clz, indent + 1, clz);
        renderConstructors(out, clz, indent + 1);
        renderInstanceMembers(out, clz, indent + 1, clz);
        indent(out, indent).append("}\n");
    }

    private void renderField(StringBuilder out, MemberDoc member, int indent, boolean includeStatic, ClassDoc owner) {
        appendDocComment(out, member.docComment(), List.of(), false, member, owner, indent);
        indent(out, indent);
        if (includeStatic && hasModifier(member, "static")) {
            out.append("static ");
        }
        if (hasModifier(member, "final")) {
            out.append("readonly ");
        }
        out.append(member.name()).append(": ").append(formatReturn(member, owner)).append(";\n");
    }

    private void renderMethod(StringBuilder out, MemberDoc member, int indent, boolean includeStatic, ClassDoc owner) {
        appendDocComment(out, member.docComment(), member.params(), true, member, owner, indent);
        indent(out, indent);
        if (includeStatic && hasModifier(member, "static")) {
            out.append("static ");
        }
        out.append(member.name());
        appendTypeParams(out, member.typeParams(), member.replaceTypeParams(), false);
        out.append("(");
        if (member.replaceParams() != null && !member.replaceParams().isBlank()) {
            String replaced = member.replaceParams();
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

    private void renderStaticMembers(StringBuilder out, ClassDoc clz, int indent, ClassDoc owner) {
        for (MemberDoc member : clz.members()) {
            if (!hasModifier(member, "static")) {
                continue;
            }
            if (member.kind() == MemberKind.FIELD) {
                renderField(out, member, indent, true, owner);
            } else if (member.kind() == MemberKind.METHOD) {
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

    private void renderInstanceMembers(StringBuilder out, ClassDoc clz, int indent, ClassDoc owner) {
        for (MemberDoc member : clz.members()) {
            if (hasModifier(member, "static")) {
                continue;
            }
            if (member.kind() == MemberKind.FIELD) {
                renderField(out, member, indent, false, owner);
            } else if (member.kind() == MemberKind.METHOD) {
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
        if (clz.typeParams() != null && !clz.typeParams().isEmpty()) {
            out.append("<");
            boolean first = true;
            for (TypeRef param : clz.typeParams()) {
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
            clz.typeParams(),
            false,
            false,
            null,
            false
        );
        return typeResolver.format(classType, TargetLanguage.TYPESCRIPT, false);
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

    private List<ClassDoc> collectByGroup(DocletModel model, String group) {
        List<ClassDoc> result = new ArrayList<>();
        for (PackageDoc pkg : model.packages()) {
            for (ClassDoc clz : pkg.classes()) {
                if (group.equals(clz.group())) {
                    result.add(clz);
                }
            }
        }
        result.sort(Comparator.comparing(ClassDoc::alias, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private Map<String, String> buildTypeScriptAliases(DocletModel model) {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("com.jsmacrosce.jsmacros.core.event.BaseEvent", "Events.BaseEvent");
        for (PackageDoc pkg : model.packages()) {
            for (ClassDoc clz : pkg.classes()) {
                if ("Event".equals(clz.group()) && clz.alias() != null) {
                    aliases.put(clz.qualifiedName(), "Events." + clz.alias());
                } else if ("Library".equals(clz.group()) && clz.alias() != null) {
                    aliases.put(clz.qualifiedName(), "typeof " + clz.alias());
                }
            }
        }
        return aliases;
    }

    private void applyTypeScriptAliases(Map<String, String> aliases) {
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
        int params = clz.typeParams().size();
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
        if (clz.extendsTypes().isEmpty()) {
            extendsType = "java.lang.Object";
        } else {
            extendsType = formatHeritageType(clz.extendsTypes().get(0), true);
        }
        out.append(" extends ").append(extendsType);
    }

    private void appendImplements(StringBuilder out, ClassDoc clz) {
        if (clz.implementsTypes().isEmpty()) {
            return;
        }
        out.append(" extends ");
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
        String formatted = typeResolver.format(type, TargetLanguage.TYPESCRIPT, false);
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
        if (owner == null || member.docComment() == null || member.kind() != MemberKind.METHOD) {
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
        String desc = formatDocText(comment.description().isBlank() ? comment.summary() : comment.description());
        List<String> lines = new ArrayList<>();
        if (!desc.isBlank()) {
            lines.add(desc);
        }

        Map<String, String> paramDocs = new HashMap<>();
        for (DocTag tag : comment.tags()) {
            if (tag.kind() == DocTagKind.PARAM && tag.name() != null) {
                paramDocs.put(tag.name(), formatDocText(tag.text()));
            }
        }
        for (ParamDoc param : params) {
            String text = paramDocs.get(param.name());
            if (text != null && !text.isBlank()) {
                lines.add("@param " + escapeParamName(param.name()) + " " + text);
            }
        }

        if (includeReturn) {
            String returnText = formatDocText(getTagText(comment, DocTagKind.RETURN));
            if (!returnText.isBlank()) {
                if (returnText.startsWith("{")) {
                    returnText = "{*} " + returnText;
                }
                lines.add("@return " + returnText);
            } else if (shouldReturnThis(member, owner)) {
                lines.add("@return self");
            }
        }

        String since = formatDocText(getTagText(comment, DocTagKind.SINCE));
        if (!since.isBlank()) {
            lines.add("@since " + since);
        }
        String deprecated = formatDocText(getTagText(comment, DocTagKind.DEPRECATED));
        if (hasDeprecatedTag(comment)) {
            lines.add(deprecated.isBlank() ? "@deprecated" : "@deprecated " + deprecated);
        }
        for (String see : getTagTexts(comment, DocTagKind.SEE)) {
            String formatted = formatDocText(see);
            if (!formatted.isBlank()) {
                lines.add("@see " + formatted);
            }
        }
        for (DocTag tag : comment.tags()) {
            if (tag.kind() == DocTagKind.TEMPLATE && tag.name() != null) {
                String text = formatDocText(tag.text());
                if (!text.isBlank()) {
                    lines.add("@template " + tag.name() + " " + text);
                }
            }
        }
        for (DocTag tag : comment.tags()) {
            if (tag.kind() == DocTagKind.OTHER && tag.text() != null) {
                String text = formatDocText(tag.text());
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

    private boolean hasDeprecatedTag(DocComment comment) {
        if (comment == null) {
            return false;
        }
        return comment.tags().stream().anyMatch(tag -> tag.kind() == DocTagKind.DEPRECATED);
    }

    private String getTagText(DocComment comment, DocTagKind kind) {
        if (comment == null) {
            return "";
        }
        for (DocTag tag : comment.tags()) {
            if (tag.kind() == kind) {
                return tag.text();
            }
        }
        return "";
    }

    private List<String> getTagTexts(DocComment comment, DocTagKind kind) {
        if (comment == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (DocTag tag : comment.tags()) {
            if (tag.kind() == kind) {
                values.add(tag.text());
            }
        }
        return values;
    }

    private String formatDocText(String text) {
        if (text == null) {
            return "";
        }
        String formatted = text.trim();
        if (formatted.isEmpty()) {
            return "";
        }
        formatted = formatted.replaceAll("\n <p>", "\n")
            .replaceAll("</?pre>", "```");
        // Convert HTML links to Markdown, cleaning up newlines in link text
        formatted = convertHtmlLinks(formatted);
        formatted = formatted.replace("&lt;", "<").replace("&gt;", ">");
        formatted = convertLinkTags(formatted);
        String trimmed = formatted.trim();
        if (looksLikeSignature(trimmed)) {
            formatted = convertSignature(trimmed);
        }
        return formatted.trim();
    }
    
    private String convertHtmlLinks(String text) {
        java.util.regex.Matcher matcher = HTML_LINK.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String url = matcher.group(1);
            String linkText = matcher.group(2);
            String replacement = "[" + linkText + "](" + url + ")";
            matcher.appendReplacement(buffer, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String convertLinkTags(String text) {
        java.util.regex.Matcher matcher = LINK_TAG.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String sig = matcher.group(1).trim();
            String mapped = mapSimpleLinkSignature(sig);
            String replacement = mapped != null ? mapped : "{@link " + convertSignature(sig) + "}";
            matcher.appendReplacement(buffer, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private boolean looksLikeSignature(String text) {
        if (text.startsWith("#")) {
            return true;
        }
        return text.matches("^xyz\\.wagyourtail\\.[^#]+\\w$")
            || text.matches("^\\w+\\.(?:\\w+\\.)+[\\w$_]+$");
    }

    private String convertSignature(String sig) {
        if (sig.matches("^xyz\\.wagyourtail\\.[^#]+\\w$")) {
            return sig.replaceFirst("^.+\\.(?=[^.]+$)", "");
        }
        if (sig.matches("^\\w+\\.(?:\\w+\\.)+[\\w$_]+$")) {
            return "Packages." + sig;
        }
        if (sig.startsWith("#")) {
            return sig.substring(1);
        }
        return sig
            .replaceFirst("^(?:xyz\\.wagyourtail\\.jsmacros\\.(?:client\\.api|core)\\.library\\.impl\\.)?F([A-Z]\\w+)#", "$1.")
            .replaceFirst("#", ".");
    }

    private String mapSimpleLinkSignature(String sig) {
        if (JAVA_NUMBER_TYPES.containsKey(sig)) {
            return JAVA_NUMBER_TYPES.get(sig);
        }
        return switch (sig) {
            case "java.lang.String" -> "string";
            case "java.lang.Boolean" -> "boolean";
            default -> null;
        };
    }

    private DocComment sanitizeLibraryComment(DocComment comment) {
        if (comment == null) {
            return null;
        }
        String summary = stripLibraryBoilerplate(comment.summary());
        String description = stripLibraryBoilerplate(comment.description());
        if (summary.equals(comment.summary()) && description.equals(comment.description())) {
            return comment;
        }
        return new DocComment(summary, description, comment.tags());
    }

    private String stripLibraryBoilerplate(String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }
        return text.replaceAll("(?m)^\\s*An instance of this class is passed to scripts as the `\\w+` variable\\.?\\s*$", "")
            .trim();
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
