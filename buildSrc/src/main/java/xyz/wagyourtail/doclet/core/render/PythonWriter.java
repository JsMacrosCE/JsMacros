package xyz.wagyourtail.doclet.core.render;

import xyz.wagyourtail.FileHandler;
import xyz.wagyourtail.doclet.core.model.ClassDoc;
import xyz.wagyourtail.doclet.core.model.DocComment;
import xyz.wagyourtail.doclet.core.model.DocletModel;
import xyz.wagyourtail.doclet.core.model.MemberDoc;
import xyz.wagyourtail.doclet.core.model.MemberKind;
import xyz.wagyourtail.doclet.core.model.PackageDoc;
import xyz.wagyourtail.doclet.core.model.ParamDoc;
import xyz.wagyourtail.doclet.core.model.TypeRef;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class PythonWriter {
    private static final Set<String> PYTHON_KEYWORDS = new LinkedHashSet<>(Arrays.asList(
        "False", "await", "else", "import", "pass", "None", "break", "except",
        "in", "raise", "True", "class", "finally", "is", "return", "and",
        "continue", "for", "lambda", "try", "as", "def", "from", "nonlocal",
        "while", "assert", "del", "global", "not", "with", "async", "elif",
        "if", "or", "yield"
    ));

    public PythonWriter() {}

    public void write(DocletModel model, File outDir, String version) throws IOException {
        Map<String, String> classNameByQualified = new LinkedHashMap<>();
        for (PackageDoc pkg : model.packages()) {
            for (ClassDoc clz : pkg.classes()) {
                classNameByQualified.put(clz.qualifiedName(), pythonClassName(clz));
            }
        }

        for (PackageDoc pkg : model.packages()) {
            for (ClassDoc clz : pkg.classes()) {
                String fileName = pythonClassName(clz);
                File out = new File(outDir, fileName + ".py");
                new FileHandler(out).write(renderClass(clz, classNameByQualified));
            }
        }

        Map<String, List<ClassDoc>> grouped = groupClasses(model);
        List<String> groupOrder = List.of("libraries", "events", "helpers", "mixins", "rest");
        List<String> groupModules = new ArrayList<>();
        for (String group : groupOrder) {
            List<ClassDoc> classes = grouped.get(group);
            if (classes == null || classes.isEmpty()) {
                continue;
            }
            groupModules.add(group);
            new FileHandler(new File(outDir, group + ".py"))
                .write(renderGroupModule(group, classes));
        }

        StringBuilder init = new StringBuilder();
        for (String module : groupModules) {
            init.append("from .").append(module).append(" import *\n");
        }
        new FileHandler(new File(outDir, "__init__.py")).write(init.toString());

        if (version != null) {
            new FileHandler(new File(outDir, "setup.py"))
                .write(renderSetupPy(version));
        }
    }

    private String pythonClassName(ClassDoc clz) {
        return clz.name().replace('.', '_');
    }

    private String renderClass(ClassDoc clz, Map<String, String> classNameByQualified) {
        PythonTypeContext ctx = new PythonTypeContext(classNameByQualified, pythonClassName(clz));
        StringBuilder body = new StringBuilder();

        body.append(renderClassLine(clz, ctx));
        appendDocstring(body, 1, clz.docComment(), List.of(), false);

        for (MemberDoc member : clz.members()) {
            if (member.kind() == MemberKind.FIELD) {
                body.append("    ")
                    .append(getVarName(member.name()))
                    .append(": ")
                    .append(ctx.formatType(member.returnType(), false))
                    .append("\n");
            }
        }

        for (MemberDoc member : clz.members()) {
            if (member.kind() == MemberKind.CONSTRUCTOR) {
                body.append(renderConstructor(member, ctx));
            } else if (member.kind() == MemberKind.METHOD) {
                body.append(renderMethod(member, ctx));
            }
        }

        body.append("    pass\n\n");

        StringBuilder builder = new StringBuilder();
        builder.append(ctx.renderImports());
        builder.append(body);
        return builder.toString();
    }

    private String renderConstructor(MemberDoc member, PythonTypeContext ctx) {
        StringBuilder builder = new StringBuilder();
        ctx.setOverloadUsed(true);
        builder.append("    @overload\n");
        builder.append("    def __init__(self");
        appendParams(builder, member.params(), ctx);
        builder.append(") -> None:\n");
        appendDocstring(builder, 2, member.docComment(), member.params(), false);
        builder.append("        pass\n\n");
        return builder.toString();
    }

    private String renderMethod(MemberDoc member, PythonTypeContext ctx) {
        StringBuilder builder = new StringBuilder();
        ctx.setOverloadUsed(true);
        builder.append("    @overload\n");
        builder.append("    def ").append(getVarName(member.name())).append("(self");
        appendParams(builder, member.params(), ctx);
        builder.append(") -> ").append(ctx.formatType(member.returnType(), false)).append(":\n");
        appendDocstring(builder, 2, member.docComment(), member.params(), true);
        builder.append("        pass\n\n");
        return builder.toString();
    }

    private void appendParams(StringBuilder builder, List<ParamDoc> params, PythonTypeContext ctx) {
        for (ParamDoc param : params) {
            builder.append(", ").append(getVarName(param.name())).append(": ")
                .append(ctx.formatType(param.type(), false));
        }
    }

    private void appendDocstring(
        StringBuilder builder,
        int indent,
        DocComment comment,
        List<ParamDoc> params,
        boolean includeReturn
    ) {
        String desc = formatDescription(comment);
        String since = getTagText(comment, "SINCE");
        Map<String, String> tagDocs = new LinkedHashMap<>();
        if (comment != null) {
            comment.tags().stream()
                .filter(tag -> tag.kind().name().equals("PARAM") && tag.name() != null)
                .forEach(tag -> tagDocs.put(tag.name(), tag.text()));
        }
        Map<String, String> paramDocs = new LinkedHashMap<>();
        if (params != null) {
            for (ParamDoc param : params) {
                if (tagDocs.containsKey(param.name())) {
                    paramDocs.put(param.name(), tagDocs.get(param.name()));
                }
            }
        }
        String ret = includeReturn ? getTagText(comment, "RETURN") : "";

        if (desc.isEmpty() && since.isEmpty() && paramDocs.isEmpty() && ret.isEmpty()) {
            return;
        }

        String pad = "    ".repeat(indent);
        builder.append(pad).append("\"\"\"");
        if (!desc.isEmpty()) {
            builder.append(escape(desc));
        }
        if (!since.isEmpty()) {
            if (!desc.isEmpty()) {
                builder.append("\n");
            }
            builder.append("Since: ").append(escape(since));
        }
        if (!paramDocs.isEmpty()) {
            builder.append("\n\n").append(pad).append("Args:\n");
            for (Map.Entry<String, String> entry : paramDocs.entrySet()) {
                builder.append(pad).append("    ")
                    .append(getVarName(entry.getKey()))
                    .append(": ")
                    .append(escape(entry.getValue()))
                    .append("\n");
            }
            builder.setLength(builder.length() - 1);
        }
        if (!ret.isEmpty()) {
            builder.append("\n\n").append(pad).append("Returns:\n");
            builder.append(pad).append("    ").append(escape(ret));
        }
        builder.append("\n").append(pad).append("\"\"\"\n");
    }

    private String renderClassLine(ClassDoc clz, PythonTypeContext ctx) {
        StringBuilder builder = new StringBuilder("\nclass ");
        builder.append(pythonClassName(clz));

        List<String> bases = new ArrayList<>();
        for (TypeRef iface : clz.implementsTypes()) {
            String name = ctx.formatType(iface, true);
            if (!name.isBlank()) {
                bases.add(name);
            }
        }
        if (!clz.typeParams().isEmpty()) {
            ctx.setGenericUsed(true);
            StringBuilder typeParams = new StringBuilder("Generic[");
            for (TypeRef param : clz.typeParams()) {
                String name = ctx.formatType(param, true);
                typeParams.append(name).append(", ");
            }
            typeParams.setLength(typeParams.length() - 2);
            typeParams.append("]");
            bases.add(typeParams.toString());
        }
        for (TypeRef extend : clz.extendsTypes()) {
            String name = ctx.formatType(extend, true);
            if (!name.isBlank()) {
                bases.add(name);
            }
        }

        if (!bases.isEmpty()) {
            builder.append("(");
            for (String base : bases) {
                builder.append(base).append(", ");
            }
            builder.setLength(builder.length() - 2);
            builder.append(")");
        }
        builder.append(":\n");
        return builder.toString();
    }

    private String renderGroupModule(String group, List<ClassDoc> classes) {
        StringBuilder builder = new StringBuilder();
        builder.append("from typing import TypeVar\n\n");
        builder.append("from .EventContainer import EventContainer\n");
        builder.append("from .BaseEvent import BaseEvent\n");

        for (ClassDoc clz : classes) {
            String name = pythonClassName(clz);
            builder.append("from .").append(name).append(" import ").append(name).append("\n");
        }

        builder.append("\nFile = TypeVar(\"java.io.File\")\n\n");
        if ("libraries".equals(group)) {
            builder.append("\n\n");
            for (ClassDoc clz : classes) {
                String name = pythonClassName(clz);
                String alias = clz.alias() == null || clz.alias().isEmpty() ? name : clz.alias();
                builder.append(alias).append(" = ").append(name).append("()\n");
            }
            builder.append("context = EventContainer()\n");
            builder.append("file = File()\n");
            builder.append("event = BaseEvent()\n");
        }

        return builder.toString();
    }

    private Map<String, List<ClassDoc>> groupClasses(DocletModel model) {
        Map<String, List<ClassDoc>> grouped = new LinkedHashMap<>();
        for (PackageDoc pkg : model.packages()) {
            for (ClassDoc clz : pkg.classes()) {
                String name = clz.name();
                String key;
                if ("Library".equalsIgnoreCase(clz.group())) {
                    key = "libraries";
                } else if ("Event".equalsIgnoreCase(clz.group())) {
                    key = "events";
                } else if (name.contains("Helper")) {
                    key = "helpers";
                } else if (name.contains("Mixin")) {
                    key = "mixins";
                } else {
                    key = "rest";
                }
                grouped.computeIfAbsent(key, value -> new ArrayList<>()).add(clz);
            }
        }
        return grouped;
    }

    private String renderSetupPy(String version) {
        String cleaned = version;
        if (cleaned.contains("-")) {
            cleaned = cleaned.split("-", 2)[0];
        }
        StringBuilder sb = new StringBuilder();
        sb.append("""
            from setuptools import setup, find_packages
            from os import path
            import os
            import time

            this_directory = path.abspath(path.dirname(__file__))
            with open(path.join(this_directory, 'README.md'), encoding='utf-8') as f:
                long_description = f.read()


            VERSION = '""");
        sb.append(cleaned);
        sb.append("""
            '
            if "-" in VERSION: VERSION = VERSION.split("-")[0]
            VERSION += "." + str(time.time()).split(".")[0][3:]
            DESCRIPTION = 'A package to let your IDE know what JsMacros can do'

            def package_files(directory):
                paths = []
                for (path, directories, filenames) in os.walk(directory):
                    for filename in filenames:
                        paths.append(os.path.join('..', path, filename))
                return paths

            extra_files = package_files('JsMacrosAC')

            # Setting up
            setup(
                name="JsMacrosAC",
                version=VERSION,
                author="Hasenzahn1",
                author_email="<motzer10@gmx.de>",
                description=DESCRIPTION,
                long_description_content_type="text/markdown",
                long_description=long_description,
                packages=["JsMacrosAC"],
                package_data = {"": extra_files},
                install_requires=[],
                keywords=['python', 'JsMacros', 'Autocomplete', 'Doc'],
                classifiers=[
                    "Intended Audience :: Developers",
                    "Programming Language :: Python :: 3",
                    "Operating System :: Unix",
                    "Operating System :: MacOS :: MacOS X",
                    "Operating System :: Microsoft :: Windows",
                ]
            )""");
        return sb.toString();
    }

    private String formatDescription(DocComment comment) {
        if (comment == null) {
            return "";
        }
        String text = comment.description();
        return text == null ? "" : text.trim();
    }

    private String getTagText(DocComment comment, String kind) {
        if (comment == null) {
            return "";
        }
        return comment.tags().stream()
            .filter(tag -> tag.kind().name().equals(kind))
            .map(tag -> tag.text())
            .findFirst()
            .orElse("");
    }

    private String escape(String text) {
        return text.replace("\"\"\"", "''");
    }

    private String getVarName(String name) {
        return PYTHON_KEYWORDS.contains(name) ? name + "_" : name;
    }

    private static class PythonTypeContext {
        private static final Map<String, String> TYPE_ALIASES = Map.ofEntries(
            Map.entry("java.lang.Object", "object"),
            Map.entry("java.lang.String", "str"),
            Map.entry("java.lang.Integer", "int"),
            Map.entry("java.lang.Boolean", "bool"),
            Map.entry("java.lang.Double", "float"),
            Map.entry("java.lang.Float", "float"),
            Map.entry("java.lang.Long", "float"),
            Map.entry("java.lang.Short", "float"),
            Map.entry("java.lang.Byte", "float"),
            Map.entry("java.lang.Character", "str"),
            Map.entry("java.lang.annotation.Annotation", ""),
            Map.entry("java.lang.Enum", "")
        );
        private static final Set<String> UNWANTED_CLASS = Set.of(
            "java.lang.Object",
            "java.lang.annotation.Annotation",
            "java.lang.Enum",
            "java.util.Collection"
        );
        private static final Map<String, String> WITH_ARG = Map.of(
            "java.util.Set", "Set",
            "java.util.List", "List",
            "java.util.Map", "Mapping",
            "java.util.Collection", "List"
        );
        private static final List<String> EXTERNAL_PREFIXES = List.of(
            "net.", "com.", "io.", "org.", "java.", "javax.", "jdk."
        );

        private final Map<String, String> classNameByQualified;
        private final String currentClassName;
        private final Set<String> internalImports = new TreeSet<>();
        private final Map<String, String> externalTypeVars = new TreeMap<>();
        private final Set<String> typeVars = new TreeSet<>();
        private boolean overloadUsed;
        private boolean importList;
        private boolean importTypeVar;
        private boolean importAny;
        private boolean importMapping;
        private boolean importSet;
        private boolean importGeneric;

        private PythonTypeContext(Map<String, String> classNameByQualified, String currentClassName) {
            this.classNameByQualified = classNameByQualified;
            this.currentClassName = currentClassName;
        }

        String formatType(TypeRef type, boolean classContext) {
            if (type == null) {
                importAny = true;
                return "Any";
            }
            return switch (type.kind()) {
                case PRIMITIVE -> switch (type.name()) {
                    case "boolean" -> "bool";
                    case "int" -> "int";
                    case "char" -> "str";
                    case "byte", "short", "long", "float", "double" -> "float";
                    default -> "int";
                };
                case VOID -> "None";
                case ARRAY -> {
                    importList = true;
                    String component = formatType(type.typeArgs().get(0), false);
                    yield "List[" + component + "]";
                }
                case TYPEVAR -> {
                    importTypeVar = true;
                    typeVars.add(type.name());
                    yield type.name();
                }
                case WILDCARD -> {
                    importAny = true;
                    yield "Any";
                }
                case DECLARED -> formatDeclared(type, classContext);
                default -> "Any";
            };
        }

        private String formatDeclared(TypeRef type, boolean classContext) {
            String qualified = type.qualifiedName();
            if (classContext && UNWANTED_CLASS.contains(qualified)) {
                return "";
            }
            if (TYPE_ALIASES.containsKey(qualified)) {
                return TYPE_ALIASES.get(qualified);
            }
            if (WITH_ARG.containsKey(qualified)) {
                String base = WITH_ARG.get(qualified);
                if ("List".equals(base)) {
                    importList = true;
                } else if ("Mapping".equals(base)) {
                    importMapping = true;
                } else if ("Set".equals(base)) {
                    importSet = true;
                }
                if (type.typeArgs().isEmpty()) {
                    importAny = true;
                    return base + "[Any]";
                }
                StringBuilder builder = new StringBuilder(base);
                builder.append("[");
                for (TypeRef arg : type.typeArgs()) {
                    builder.append(formatType(arg, false)).append(", ");
                }
                builder.setLength(builder.length() - 2);
                builder.append("]");
                return builder.toString();
            }

            String className = type.name().replace('.', '_');
            if (className.equals(currentClassName)) {
                return "\"" + className + "\"";
            }
            if (classNameByQualified.containsKey(qualified)) {
                String importName = classNameByQualified.get(qualified);
                if (!importName.equals(currentClassName)) {
                    internalImports.add(importName);
                }
                if (!type.typeArgs().isEmpty()) {
                    StringBuilder builder = new StringBuilder(importName);
                    builder.append("[");
                    for (TypeRef arg : type.typeArgs()) {
                        builder.append(formatType(arg, false)).append(", ");
                    }
                    builder.setLength(builder.length() - 2);
                    builder.append("]");
                    return builder.toString();
                }
                return importName;
            }

            if (isExternalType(qualified)) {
                importTypeVar = true;
                String typeVarName = sanitizeTypeVarName(qualified);
                externalTypeVars.put(className, typeVarName);
                return className;
            }

            if (!type.typeArgs().isEmpty()) {
                StringBuilder builder = new StringBuilder(className);
                builder.append("[");
                for (TypeRef arg : type.typeArgs()) {
                    builder.append(formatType(arg, false)).append(", ");
                }
                builder.setLength(builder.length() - 2);
                builder.append("]");
                return builder.toString();
            }
            return className;
        }

        private boolean isExternalType(String qualifiedName) {
            for (String prefix : EXTERNAL_PREFIXES) {
                if (qualifiedName.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }

        private String sanitizeTypeVarName(String qualifiedName) {
            return qualifiedName.replace("<", "_")
                .replace(">", "_")
                .replace("?", "")
                .replace(".", "_");
        }

        String renderImports() {
            StringBuilder builder = new StringBuilder();
            if (overloadUsed) {
                builder.append("from typing import overload\n");
            }
            if (importList) {
                builder.append("from typing import List\n");
            }
            if (importTypeVar) {
                builder.append("from typing import TypeVar\n");
            }
            if (importAny) {
                builder.append("from typing import Any\n");
            }
            if (importMapping) {
                builder.append("from typing import Mapping\n");
            }
            if (importSet) {
                builder.append("from typing import Set\n");
            }
            if (importGeneric) {
                builder.append("from typing import Generic\n");
            }
            for (String imp : internalImports) {
                builder.append("from .").append(imp).append(" import ").append(imp).append("\n");
            }
            builder.append("\n");

            for (String typeVar : typeVars) {
                builder.append(typeVar).append(" = TypeVar(\"").append(typeVar).append("\")\n");
            }
            for (Map.Entry<String, String> entry : externalTypeVars.entrySet()) {
                String className = entry.getKey();
                String typeVarName = entry.getValue();
                if ("T".equals(typeVarName) || "U".equals(typeVarName) || "R".equals(typeVarName)) {
                    builder.append(typeVarName).append(" = TypeVar(\"").append(typeVarName).append("\")\n");
                } else {
                    builder.append(typeVarName).append(" = TypeVar(\"").append(typeVarName).append("\")\n");
                    builder.append(className).append(" = ").append(typeVarName).append("\n\n");
                }
            }

            return builder.toString();
        }

        void setOverloadUsed(boolean overloadUsed) {
            this.overloadUsed = overloadUsed;
        }

        void setGenericUsed(boolean genericUsed) {
            this.importGeneric = genericUsed;
        }
    }
}
