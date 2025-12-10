package xyz.wagyourtail.doclet.mddoclet.parsers;

import com.sun.source.doctree.*;
import com.sun.source.util.DocTreePath;
import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.Pair;
import xyz.wagyourtail.doclet.DocletIgnore;
import xyz.wagyourtail.doclet.mddoclet.Group;
import xyz.wagyourtail.doclet.mddoclet.Main;
import xyz.wagyourtail.doclet.mddoclet.options.Links;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassParser {
    private final Group group;
    private final String alias;
    public TypeElement type;

    public ClassParser(TypeElement type, Group group, String alias) {
        this.type = type;
        this.group = group;
        this.alias = alias;
    }

    /**
     * @return class name with $ for inner class
     */
    private static String getClassName(TypeElement type) {
        StringBuilder s = new StringBuilder(type.getSimpleName());
        Element t2 = type.getEnclosingElement();
        while (t2.getKind() == ElementKind.INTERFACE || t2.getKind() == ElementKind.CLASS) {
            s.insert(0, t2.getSimpleName() + "$");
            t2 = t2.getEnclosingElement();
        }
        return s.toString();
    }

    /**
     * @return package name with . separators
     */
    private static String getPackage(TypeElement type) {
        Element t2 = type;
        while (t2.getKind() != ElementKind.PACKAGE) t2 = t2.getEnclosingElement();

        return ((PackageElement) t2).getQualifiedName().toString();
    }

    public String getPathPart() {
        return getPackage(type).replaceAll("\\.", "/") + "/" + getClassName(type).replaceAll("\\$", ".");
    }

    /**
     * nothing much
     *
     * @return up dir string
     */
    private String getUpDir(int extra) {
        StringBuilder s = new StringBuilder();
        for (String ignored : getPackage(type).split("\\.")) {
            s.append("../");
        }
        s.append("../".repeat(Math.max(0, extra)));
        return s.toString();
    }

    public String generateMarkdown() {
        StringBuilder builder = new StringBuilder();

        builder.append("# ").append(getClassName(type)).append("\n\n");
        builder.append(String.format("**Full Class Name:** `%s.%s`", getPackage(type), getClassName(type))).append("\n\n");
        StringBuilder desc = getDescription(type.getEnclosingElement());
        builder.append(desc.isEmpty() ? "TODO: No description supplied" : desc);
        if (group == Group.Library) {
            // TODO: Don't use class name, get actual var name
            builder.append(String.format("Accessible in scripts via the global `%s` variable.", getClassName(type)));
        }
        builder.append("\n\n");

        // Constructors
        if (group != Group.Library) {
            List<? extends Element> constructors = type.getEnclosedElements().stream().filter(e -> e.getKind() == ElementKind.CONSTRUCTOR).toList();

            if (!constructors.isEmpty()) {
                builder.append("## Constructors\n\n");
            }

            constructors.forEach(el -> {
                if (!el.getModifiers().contains(Modifier.PUBLIC)) {
                    return;
                }
//                if (firstFlag.get()) {
//                    firstFlag.set(false);
//                    builder.append(new XMLBuilder("h3", true, true).append("Constructors"));
//                    XMLBuilder con = new XMLBuilder("div").setClass("constructorDoc");
//                    builder.append(con);
//                    constructors.set(con);
//                }
//                constructors.get().append(parseConstructor(el));

                builder.append(parseConstructor((ExecutableElement) el));
            });
        }

        // Methods
        builder.append("## Methods\n\n");

        return builder.toString();
    }

//    private XMLBuilder parseClass() {
//        XMLBuilder builder = new XMLBuilder("main").setClass("classDoc");
//        XMLBuilder subClasses;
//        builder.append(subClasses = new XMLBuilder("div").setId("subClasses"));
//        for (Element subClass : Main.elements.stream().filter(e -> {
//            if (e.getKind().isClass() || e.getKind().isInterface()) {
//                return Main.types.isAssignable(e.asType(), Main.types.getDeclaredType(type)) && !e.equals(type);
//            }
//            return false;
//        }).collect(Collectors.toList())) {
//            subClasses.append(parseType(subClass.asType()), " ");
//        }
//        XMLBuilder cname;
//        builder.append(cname = new XMLBuilder("h2", true, true).setClass("classTitle").append((getPackage(type)), ".", getClassName(type)));
//
//        List<? extends TypeParameterElement> params = type.getTypeParameters();
//        if (params != null && !params.isEmpty()) {
//            cname.append("<");
//            for (TypeParameterElement param : params) {
//                cname.append(parseType(param.asType()), ", ");
//            }
//            cname.pop();
//            cname.append(">");
//        }
//
//        builder.append(createFlags(type, false));
//        TypeMirror sup = type.getSuperclass();
//        List<? extends TypeMirror> ifaces = type.getInterfaces();
//        XMLBuilder ext;
//        builder.append(ext = new XMLBuilder("h4", true, true).addStringOption("class", "classExtends"));
//        if (sup != null && !sup.toString().equals("java.lang.Object") && !sup.getKind().equals(TypeKind.NONE)) {
//            ext.append("extends ", parseType(sup));
//        }
//        if (!ifaces.isEmpty()) {
//            ext.append(" implements ");
//            for (TypeMirror iface : ifaces) {
//                ext.append(parseType(iface), " ");
//            }
//        }
//
//        builder.append(getSince(type));
//        builder.append(getDescription(type));
//
//        AtomicBoolean firstFlag = new AtomicBoolean(true);
//        AtomicReference<XMLBuilder> constructors = new AtomicReference<>();
//        //CONSTRUCTORS
//        if (!group.equals("Library")) {
//            type.getEnclosedElements().stream().filter(e -> e.getKind() == ElementKind.CONSTRUCTOR).forEach(el -> {
//                if (!el.getModifiers().contains(Modifier.PUBLIC)) {
//                    return;
//                }
//                if (firstFlag.get()) {
//                    firstFlag.set(false);
//                    builder.append(new XMLBuilder("h3", true, true).append("Constructors"));
//                    XMLBuilder con = new XMLBuilder("div").setClass("constructorDoc");
//                    builder.append(con);
//                    constructors.set(con);
//                }
//                constructors.get().append(parseConstructor((ExecutableElement) el));
//            });
//        }
//
//        XMLBuilder shorts;
//        builder.append(shorts = new XMLBuilder("div").setClass("shortFieldMethods"));
//
//        AtomicReference<XMLBuilder> fieldShorts = new AtomicReference<>();
//        AtomicReference<XMLBuilder> fields = new AtomicReference<>();
//
//        firstFlag.set(true);
//        type.getEnclosedElements().stream().filter(e -> e.getKind() == ElementKind.FIELD || e.getKind() == ElementKind.ENUM_CONSTANT).forEach(el -> {
//            if (!el.getModifiers().contains(Modifier.PUBLIC)) {
//                return;
//            }
//            if (firstFlag.get()) {
//                firstFlag.set(false);
//
//                builder.append(new XMLBuilder("h3", true, true).append("Fields"));
//
//                XMLBuilder f = new XMLBuilder("div").setClass("fieldDoc");
//                builder.append(f);
//                XMLBuilder fs = new XMLBuilder("div").setClass("fieldShorts").append(new XMLBuilder("h4").append("Fields"));
//                shorts.append(fs);
//
//                fields.set(f);
//                fieldShorts.set(fs);
//            }
//
//            fields.get().append(parseField(el));
//            fieldShorts.get().append(
//                    new XMLBuilder("div").setClass("shortField shortClassItem").append(
//                            new XMLBuilder("a", true, true).addStringOption("href", getURL(el).getKey()).append(memberName(el)),
//                            createFlags(el, true)
//                    )
//            );
//        });
//
//        AtomicReference<XMLBuilder> methodShorts = new AtomicReference<>();
//        AtomicReference<XMLBuilder> methods = new AtomicReference<>();
//
//        firstFlag.set(true);
//        type.getEnclosedElements().stream().filter(e -> e.getKind() == ElementKind.METHOD).forEach(el -> {
//            if (!el.getModifiers().contains(Modifier.PUBLIC)) {
//                return;
//            }
//            if (firstFlag.get()) {
//                firstFlag.set(false);
//
//                builder.append(new XMLBuilder("h3", true, true).append("Methods"));
//
//                XMLBuilder m = new XMLBuilder("div").setClass("methodDoc");
//                builder.append(m);
//                XMLBuilder ms = new XMLBuilder("div").setClass("methodShorts").append(new XMLBuilder("h4").append("Methods"));
//                shorts.append(ms);
//
//                methods.set(m);
//                methodShorts.set(ms);
//            }
//            methods.get().append(parseMethod((ExecutableElement) el));
//            methodShorts.get().append(
//                    new XMLBuilder("div").setClass("shortMethod shortClassItem").append(
//                            new XMLBuilder("a", true, true).addStringOption("href", getURL(el).getKey()).append(memberName(el)),
//                            createFlags(el, true)
//                    )
//            );
//        });
//
//        return builder;
//    }
//
    private StringBuilder parseConstructor(ExecutableElement element) {
        StringBuilder builder = new StringBuilder();

        builder.append(String.format("### `new %s(%s)`",
                    getClassName((TypeElement) element.getEnclosingElement()),
                    createTitleParams(element)))
                .append("\n\n");

        builder.append(createFlags(element));
        builder.append(getSince(element));
        builder.append(getDescription(element));

//        StringBuilder paramTable = createParamTable(element);
//        if (paramTable != null) {
//            builder.append(paramTable);
//        }


//        XMLBuilder constructor = new XMLBuilder("div").setClass("constructor classItem").setId(memberId(element));
//        constructor.append(new XMLBuilder("h4").setClass("constructorTitle classItemTitle").append(
//                "new ", getClassName((TypeElement) element.getEnclosingElement()), "(",
//                createTitleParams(element).setClass("constructorParams"),
//                ")"
//        ));
//        constructor.append(createFlags(element, false));
//        constructor.append(getSince(element));
//
//        constructor.append(new XMLBuilder("div").setClass("constructorDesc classItemDesc")
//                .append(getDescription(element)));
//
//        XMLBuilder paramTable = createParamTable(element);
//        if (paramTable != null) {
//            constructor.append(paramTable);
//        }

        return builder;
    }
//
//    private XMLBuilder parseMethod(ExecutableElement element) {
//        XMLBuilder method = new XMLBuilder("div").setClass("method classItem").setId(memberId(element));
//        XMLBuilder methodTitle;
//        method.append(methodTitle = new XMLBuilder("h4", true).setClass("methodTitle classItemTitle").append(
//                ".", element.getSimpleName()
//        ));
//
//        List<? extends TypeParameterElement> params = element.getTypeParameters();
//        if (params.size() > 0) {
//            methodTitle.append("<");
//            for (TypeParameterElement param : params) {
//                methodTitle.append(parseType(param.asType()), ", ");
//            }
//            methodTitle.pop();
//            methodTitle.append(">");
//        }
//
//        methodTitle.append("(",
//                createTitleParams(element).setClass("methodParams"),
//                ")"
//        );
//        method.append(createFlags(element, false));
//        method.append(getSince(element));
//
//        method.append(new XMLBuilder("div").setClass("methodDesc classItemDesc").append(getDescription(element)));
//
//        XMLBuilder paramTable = createParamTable(element);
//        if (paramTable != null) {
//            method.append(paramTable);
//        }
//
//        method.append(new XMLBuilder("div").setClass("methodReturn classItemType").append(
//                new XMLBuilder("h5", true, true).setClass("methodReturnTitle classItemTypeTitle").append(
//                        "Returns: ", parseType(element.getReturnType())
//                ),
//                getReturnDescription(element).setClass("methodReturnDesc classItemTypeDesc")
//        ));
//
//        return method;
//    }
//
//    private XMLBuilder getReturnDescription(ExecutableElement element) {
//        DocCommentTree dct = Main.treeUtils.getDocCommentTree(element);
//        if (dct == null) {
//            return new XMLBuilder("p");
//        }
//        ReturnTree t = (ReturnTree) dct.getBlockTags().stream().filter(e -> e.getKind() == DocTree.Kind.RETURN).findFirst().orElse(null);
//        if (t == null) {
//            return new XMLBuilder("p");
//        }
//        return createDescription(element, t.getDescription());
//    }

    private StringBuilder createTitleParams(ExecutableElement element) {
        StringBuilder builder = new StringBuilder();

        for (VariableElement parameter : element.getParameters()) {
            builder.append(parameter.getSimpleName().toString());
            builder.append(", ");
        }
        if (!element.getParameters().isEmpty()) {
            builder.delete(builder.length() - ", ".length(), builder.length());
        }

        return builder;
    }

//    private XMLBuilder createParamTable(ExecutableElement element) {
//        List<? extends VariableElement> params = element.getParameters();
//        if (params == null || params.isEmpty()) {
//            return null;
//        }
//        XMLBuilder body;
//        XMLBuilder table = new XMLBuilder("table").setClass("paramTable").append(
//                new XMLBuilder("thead").append(
//                        new XMLBuilder("th", true, true).append("Parameter"),
//                        new XMLBuilder("th", true, true).append("Type"),
//                        new XMLBuilder("th", true, true).append("Description")
//                ),
//                body = new XMLBuilder("tbody")
//        );
//        Map<String, XMLBuilder> paramDescMap = getParamDescriptions(element);
//        for (VariableElement param : params) {
//            body.append(new XMLBuilder("tr").append(
//                    new XMLBuilder("td", true, true).append(param.getSimpleName()),
//                    new XMLBuilder("td", true, true).append(parseType(param.asType())),
//                    new XMLBuilder("td", true, true).append(paramDescMap.get(param.getSimpleName().toString()))
//            ));
//        }
//        return table;
//    }
//
//    private XMLBuilder parseField(Element element) {
//        XMLBuilder field = new XMLBuilder("div").setClass("field classItem").setId(memberId(element));
//        field.append(new XMLBuilder("h4", true).setClass("classItemTitle").append(
//                ".", memberName(element)
//        ));
//        field.append(createFlags(element, false));
//        field.append(getSince(element));
//
//        field.append(new XMLBuilder("div").setClass("fieldDesc classItemDesc").append(getDescription(element)));
//
//        field.append(new XMLBuilder("div").setClass("fieldReturn classItemType").append(
//                new XMLBuilder("h5", true, true).setClass("fieldTypeTitle classItemTypeTitle").append(
//                        "Type: ", parseType(element.asType())
//                )
//        ));
//
//        return field;
//    }
//
//    public Map<String, XMLBuilder> getParamDescriptions(ExecutableElement element) {
//        Map<String, XMLBuilder> paramMap = new HashMap<>();
//        DocCommentTree comment = Main.treeUtils.getDocCommentTree(element);
//        if (comment == null) {
//            return paramMap;
//        }
//        comment.getBlockTags().stream().filter(e -> e.getKind() == DocTree.Kind.PARAM).forEach(e -> paramMap.put(((ParamTree) e).getName().getName().toString(), createDescription(element, ((ParamTree) e).getDescription())));
//        return paramMap;
//    }

    private StringBuilder getSince(Element element) {
        DocCommentTree tree = Main.treeUtils.getDocCommentTree(element);
        SinceTree since = tree == null ? null : (SinceTree) tree.getBlockTags().stream().filter(e -> e.getKind().equals(DocTree.Kind.SINCE)).findFirst().orElse(null);
        if (since == null) {
            return new StringBuilder();
        }

        return new StringBuilder(String.format("**Since:** %s", since.getBody())).append("\n\n");
    }

    private StringBuilder getDescription(Element element) {
        DocCommentTree tree = Main.treeUtils.getDocCommentTree(element);

        StringBuilder builder = createDescription(element, tree == null ? List.of() : tree.getFullBody());
        if (builder.isEmpty()) {
            return builder;
        }

        return builder.append("\n\n");
    }

    private StringBuilder createDescription(Element el, List<? extends DocTree> inlinedoc) {
        StringBuilder builder = new StringBuilder();
        for (DocTree docTree : inlinedoc) {
            switch (docTree.getKind()) {
                case LINK, LINK_PLAIN -> {
                    Element ele = Main.treeUtils.getElement(new DocTreePath(new DocTreePath(Main.treeUtils.getPath(el), Main.treeUtils.getDocCommentTree(el)), ((LinkTree) docTree).getReference()));
                    if (ele != null) {
                        StringBuilder linkBuilder = new StringBuilder();
                        Pair<String, Boolean> url = getURL(ele);

                        //  = new StringBuilder("a", true).addStringOption("href", url.getKey())

                        linkBuilder.append(String.format("[%s](%s)", "", url.getKey()));

                        if (List.of(ElementKind.INTERFACE, ElementKind.CLASS, ElementKind.ANNOTATION_TYPE, ElementKind.ENUM).contains(ele.getKind())) {
                            linkBuilder.append(getClassName((TypeElement) ele));
                        } else {
//                            linkBuilder.append(getClassName((TypeElement) ele.getEnclosingElement()), "#", ele.toString());
                        }

//                        if (url.getValue()) {
//                            linkBuilder.addStringOption("target", "_blank");
//                        }
//                        if (linkBuilder.options.get("href").equals("\"\"")) {
//                            linkBuilder.setClass("type deadType");
//                        } else {
//                            linkBuilder.setClass("type");
//                        }

                        builder.append(linkBuilder);
                    } else {
                        builder.append(((LinkTree) docTree).getReference().getSignature());
                    }
                }
                case CODE ->
                        builder.append(((LiteralTree) docTree).getBody());
                default -> builder.append(docTree);
            }
        }
        return builder;
    }
//
//    private XMLBuilder parseType(TypeMirror type) {
//        XMLBuilder builder = new XMLBuilder("div", true).setClass("typeParameter");
//        XMLBuilder typeLink;
//        switch (type.getKind()) {
//            case BOOLEAN, BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE, VOID, NONE -> {
//                //isPrimitive
//                builder.append(typeLink = new XMLBuilder("p", true).append(type));
//                typeLink.setClass("type primitiveType");
//            }
//            case ARRAY -> {
//                return parseType(((ArrayType) type).getComponentType()).append("[]");
//            }
//            case DECLARED -> {
//                Pair<String, Boolean> url = getURL(((DeclaredType) type).asElement());
//                builder.append(typeLink = new XMLBuilder("a", true).addStringOption("href", url.getKey()).append(getClassName((TypeElement) ((DeclaredType) type).asElement())));
//
//                if (url.getValue()) {
//                    typeLink.addStringOption("target", "_blank");
//                }
//                if (typeLink.options.get("href").equals("\"\"")) {
//                    typeLink.setClass("type deadType");
//                } else {
//                    typeLink.setClass("type");
//                }
//
//                List<? extends TypeMirror> params = ((DeclaredType) type).getTypeArguments();
//                if (params != null && !params.isEmpty()) {
//                    builder.append("<");
//                    for (TypeMirror param : params) {
//                        if (param instanceof TypeVariable typeVariable && typeVariable.getUpperBound().equals(type)) {
//                            builder.append(typeLink = new XMLBuilder("p", true));
//                            typeLink.setClass("type primitiveType");
//                            typeLink.append(typeVariable.asElement().getSimpleName());
//                            builder.append(", ");
//                        } else {
//                            builder.append(parseType(param), ", ");
//                        }
//                    }
//                    builder.pop();
//                    builder.append(">");
//                }
//            }
//            case TYPEVAR -> {
//                builder.append(typeLink = new XMLBuilder("p", true));
//                typeLink.setClass("type primitiveType");
//                typeLink.append(((TypeVariable) type).asElement().getSimpleName());
//                TypeMirror ext = ((TypeVariable) type).getUpperBound();
//                if (!ext.toString().equals("java.lang.Object")) {
//                    typeLink.append(
//                            new XMLBuilder("p").setClass("classExtends").append("<b> extends </b>"),
//                            parseType(ext)
//                    );
//                }
//            }
//            case WILDCARD -> {
//                builder.append(typeLink = new XMLBuilder("p", true));
//                typeLink.setClass("type primitiveType");
//                typeLink.append("?");
//            }
//        }
//        return builder;
//    }

    /**
     *
     * @param type The element to get the url to
     * @return a pair containing:
     *         1. the resolved URL, or "" if the element has no URL
     *         2. true if the URL should open in a new tab, false otherwise
     */
    private Pair<String, Boolean> getURL(Element type) {
        if (type.asType().getKind().isPrimitive()) {
            return new Pair<>("", false);
        }

        Element clazz = type;
        while (!(clazz instanceof TypeElement)) {
            clazz = clazz.getEnclosingElement();
        }

        if (!clazz.equals(this.type)) {
            String pkg = getPackage((TypeElement) clazz);
            if (Main.internalClasses.containsKey(clazz)) {
                StringBuilder s = new StringBuilder(getUpDir(0));
                s.append(Main.internalClasses.get(clazz).getPathPart()).append(".html");
                if (type != clazz) {
                    s.append("#").append(memberId(type));
                }
                return new Pair<>(s.toString(), false);
            } else if (Links.externalPackages.containsKey(pkg)) {
                return new Pair<>(Links.externalPackages.get(pkg) + getClassName((TypeElement) clazz) + ".html", true);
            } else if (pkg.startsWith("com.mojang") || pkg.startsWith("net.minecraft")) {
                return new Pair<>(Main.mappingViewerURL + pkg.replaceAll("\\.", "/") + "/" + getClassName((TypeElement) clazz), true);
            } else {
                return new Pair<>("", false);
            }
        } else {
            StringBuilder s = new StringBuilder();
            s.append("#");
            if (type != clazz) {
                s.append(memberId(type));
            }
            return new Pair<>(s.toString(), false);
        }
    }

    private static String memberId(Element member) {
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
            case TYPE_PARAMETER -> {
            }
            default -> throw new UnsupportedOperationException(String.valueOf(member.getKind()));
        }

        return s.toString();
    }

    private static String memberName(Element member) {
        StringBuilder s = new StringBuilder();
        switch (member.getKind()) {
            case ENUM_CONSTANT, FIELD -> s.append(member.getSimpleName());
            case METHOD -> {
                s.append(member.getSimpleName()).append("(");
                for (VariableElement parameter : ((ExecutableElement) member).getParameters()) {
                    s.append(parameter.getSimpleName()).append(", ");
                }
                if (((ExecutableElement) member).getParameters().size() > 0) {
                    s.setLength(s.length() - 2);
                }
                s.append(")");
            }
            default -> throw new UnsupportedOperationException(String.valueOf(member.getKind()));
        }
        return s.toString();
    }

    private static String getTypeMirrorName(TypeMirror type) {
        switch (type.getKind()) {
            case BOOLEAN -> {
                return "boolean";
            }
            case BYTE -> {
                return "byte";
            }
            case SHORT -> {
                return "short";
            }
            case INT -> {
                return "int";
            }
            case LONG -> {
                return "long";
            }
            case CHAR -> {
                return "char";
            }
            case FLOAT -> {
                return "float";
            }
            case DOUBLE -> {
                return "double";
            }
            case VOID, NONE -> {
                return "void";
            }
            case NULL -> {
                return "null";
            }
            case ARRAY -> {
                return getTypeMirrorName(((ArrayType) type).getComponentType()) + "[]";
            }
            case DECLARED -> {
                return getClassName((TypeElement) ((DeclaredType) type).asElement());
            }
            case TYPEVAR -> {
                return ((TypeVariable) type).asElement().getSimpleName().toString();
            }
            case WILDCARD -> {
                return "?";
            }
            default -> throw new UnsupportedOperationException(String.valueOf(type.getKind()));
        }
    }

    private static StringBuilder createFlags(Element member) {
        StringBuilder builder = new StringBuilder("");
        ArrayList<String> flags = new ArrayList<>();

        for (Modifier modifier : member.getModifiers()) {
            switch (modifier) {
                case ABSTRACT -> {
                    if (member.getKind() != ElementKind.INTERFACE && member.getEnclosingElement().getKind() != ElementKind.INTERFACE) {
                        flags.add("Abstract");
                    }
                }
                case STATIC -> flags.add("Static");
                case FINAL -> flags.add("Final");
                default -> {
                }
            }
        }
        if (member.getKind() == ElementKind.ENUM || member.getKind() == ElementKind.ENUM_CONSTANT) {
            flags.add("Enum");
        }
        if (member.getKind() == ElementKind.INTERFACE) {
            flags.add("Interface");
        }
        if (member.getAnnotation(Deprecated.class) != null) {
            flags.add("Deprecated");
        }

        for (String flag : flags) {
            builder.append(flag).append(", ");
        }
        if (!flags.isEmpty()) {
            builder.delete(builder.length() - ", ".length(), builder.length());
        }

        if (!builder.isEmpty()) {
            builder = new StringBuilder("<").append(builder).append(">");
        }

        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClassParser that)) {
            return false;
        }
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

}
