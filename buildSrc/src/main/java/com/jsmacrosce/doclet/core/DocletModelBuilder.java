package com.jsmacrosce.doclet.core;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.DocletReadOnly;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.doclet.DocletReplaceTypeParams;
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
import com.jsmacrosce.doclet.core.model.ScriptNamespaceDoc;
import com.jsmacrosce.doclet.core.model.TypeRef;
import com.jsmacrosce.doclet.core.util.DocBodyRenderer;
import com.jsmacrosce.doclet.core.util.ElementNameUtils;
import com.jsmacrosce.doclet.core.util.NestedTypeParams;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import jdk.javadoc.doclet.DocletEnvironment;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeKind;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

@DocletIgnore
public class DocletModelBuilder {
    private final TypeResolver typeResolver;
    private final DocCommentParser docCommentParser;
    private final List<DeclaredTypeDoc> declaredTypes = new ArrayList<>();
    private DocTrees docTrees;
    private Elements elementUtils;
    private Types typeUtils;
    private Set<ExecutableElement> objectMethods = Set.of();
    private Set<String> objectMethodNames = Set.of();
    private static final Pattern NULL_INITIALIZER = Pattern.compile("=\\s*null\\s*;?\\s*$");

    /**
     * The types the shipped TypeScript headers declare. The generated definitions must not
     * declare them again, or the two would collide and the definitions would not compile.
     */
    private static final Set<String> HEADER_DECLARED_TYPES = Set.of(
        "java.io.File",
        "java.io.Serializable",
        "java.lang.Array",
        "java.lang.Class",
        "java.lang.Comparable",
        "java.lang.Interface",
        "java.lang.Iterable",
        "java.lang.Object",
        "java.lang.StackTraceElement",
        "java.lang.Throwable",
        "java.net.URI",
        "java.net.URL",
        "java.util.Collection",
        "java.util.List",
        "java.util.Map",
        "java.util.Set"
    );

    public DocletModelBuilder(
        TypeResolver typeResolver,
        DocCommentParser docCommentParser
    ) {
        this.typeResolver = typeResolver;
        this.docCommentParser = docCommentParser;
    }

    public DocletModel build(DocletEnvironment environment) {
        this.docTrees = environment.getDocTrees();
        this.elementUtils = environment.getElementUtils();
        this.typeUtils = environment.getTypeUtils();
        initObjectMethods();

        Map<String, List<ClassDoc>> packages = new HashMap<>();
        Set<String> documented = new HashSet<>();
        List<ScriptNamespaceDoc> namespaces = new ArrayList<>();
        Set<TypeElement> catalogs = new HashSet<>();
        // These classes list the types scripts can use; they are not themselves objects scripts create.
        // Read their entries separately instead of documenting them as ordinary classes.
        for (ScriptNamespaceReader.Namespace namespace : ScriptNamespaceReader.read(environment.getIncludedElements())) {
            catalogs.add(namespace.catalog());
            List<ScriptNamespaceDoc.Export> exports = new ArrayList<>();
            for (ScriptNamespaceReader.Export export : namespace.exports()) {
                // Class<GenericHelper> does not tell us about GenericHelper<T>. Read the class's
                // own parameters, and its enclosing class's parameters, so the typings keep them.
                List<TypeRef> params = new ArrayList<>();
                for (NestedTypeParams.Captured captured : NestedTypeParams.capturedParams(export.target())) {
                    params.add(typeResolver.resolve(captured.element().asType()));
                }
                for (TypeParameterElement param : export.target().getTypeParameters()) {
                    params.add(typeResolver.resolve(param.asType()));
                }
                exports.add(new ScriptNamespaceDoc.Export(export.name(),
                        typeResolver.resolve(export.target().asType()), params, export.target().getKind().isInterface()));
            }
            namespaces.add(new ScriptNamespaceDoc(namespace.name(),
                    docCommentParser.parse(namespace.catalog()), exports));
        }
        Map<String, List<TypeMirror>> mixinInterfaces = collectMixinInterfaces(environment);

        for (Element element : environment.getIncludedElements()) {
            if (!(element instanceof TypeElement type)) {
                addDeclaredType(element);
                continue;
            }

            if (catalogs.contains(type) || hasDocletIgnore(type)) {
                continue;
            }

            addDeclaredType(type);

            String packageName = ElementNameUtils.getPackageName(type);
            String displayName = ElementNameUtils.getDisplayClassName(type);
            String qualifiedName = ElementNameUtils.getQualifiedName(type);

            ClassGroup group = ClassGroup.Class;
            String alias = null;
            boolean eventCancellable = false;
            String eventFilterer = null;
            String category = null;
            AnnotationMirror library = findAnnotation(type, "Library");
            AnnotationMirror event = findAnnotation(type, "Event");
            if (library != null) {
                group = ClassGroup.Library;
                alias = String.valueOf(getAnnotationValue(library, "value"));
            } else if (event != null) {
                group = ClassGroup.Event;
                alias = String.valueOf(getAnnotationValue(event, "value"));
                Object cancellableValue = getAnnotationValue(event, "cancellable");
                eventCancellable = Boolean.TRUE.equals(cancellableValue);
                Object filtererValue = getAnnotationValue(event, "filterer");
                if (filtererValue instanceof DeclaredType declared) {
                    String name = declared.asElement().getSimpleName().toString();
                    if (!"EventFilterer".equals(name)) {
                        eventFilterer = name;
                    }
                } else if (filtererValue instanceof TypeMirror mirror && mirror.getKind() == TypeKind.DECLARED) {
                    String name = ((DeclaredType) mirror).asElement().getSimpleName().toString();
                    if (!"EventFilterer".equals(name)) {
                        eventFilterer = name;
                    }
                }
            } else if (hasEnclosingAnnotation(type, "Library")) {
                group = ClassGroup.Library;
            } else if (hasEnclosingAnnotation(type, "Event")) {
                group = ClassGroup.Event;
            }
            DocletCategory categoryAnnotation = type.getAnnotation(DocletCategory.class);
            if (categoryAnnotation != null) {
                category = categoryAnnotation.value();
            }

            ClassKind kind = switch (type.getKind()) {
                case INTERFACE -> ClassKind.INTERFACE;
                case ENUM -> ClassKind.ENUM;
                case ANNOTATION_TYPE -> ClassKind.ANNOTATION;
                default -> ClassKind.CLASS;
            };

            List<TypeRef> typeParams = new ArrayList<>();
            for (TypeParameterElement param : type.getTypeParameters()) {
                typeParams.add(typeResolver.resolve(param.asType()));
            }

            List<TypeRef> capturedTypeParams = new ArrayList<>();
            for (NestedTypeParams.Captured captured : NestedTypeParams.capturedParams(type)) {
                capturedTypeParams.add(typeResolver.resolve(captured.element().asType()));
            }

            List<TypeRef> extendsTypes = new ArrayList<>();
            List<TypeMirror> heritage = new ArrayList<>();
            if (type.getSuperclass() != null && type.getSuperclass().getKind() != javax.lang.model.type.TypeKind.NONE) {
                String sup = type.getSuperclass().toString();
                if (!"java.lang.Object".equals(sup)) {
                    extendsTypes.add(typeResolver.resolve(type.getSuperclass()));
                    heritage.add(type.getSuperclass());
                }
            }
            List<TypeRef> implementsTypes = new ArrayList<>();
            Set<String> implemented = new HashSet<>();
            for (var iface : type.getInterfaces()) {
                TypeRef resolved = typeResolver.resolve(iface);
                if (implemented.add(resolved.qualifiedName())) {
                    implementsTypes.add(resolved);
                    heritage.add(iface);
                }
            }
            for (TypeRef supertype : extendsTypes) {
                heritageTypes.add(supertype.qualifiedName());
            }

            Set<TypeElement> superMcTypes = new LinkedHashSet<>();
            Set<TypeElement> superTypes = new LinkedHashSet<>();
            collectSuperTypes(type, superTypes, superMcTypes, new HashSet<>());

            boolean directExtendsMc = isMinecraftType(type.getSuperclass());
            List<TypeMirror> mixins = mixinInterfaces.get(qualifiedName);
            if (mixins != null) {
                for (TypeMirror iface : mixins) {
                    TypeRef resolved = typeResolver.resolve(iface);
                    if (implemented.add(resolved.qualifiedName())) {
                        implementsTypes.add(resolved);
                        heritage.add(iface);
                    }
                }
            }
            if (directExtendsMc && !superMcTypes.isEmpty()) {
                for (TypeElement mcType : superMcTypes) {
                    String mcQualified = ElementNameUtils.getQualifiedName(mcType);
                    List<TypeMirror> extraMixins = mixinInterfaces.get(mcQualified);
                    if (extraMixins == null) {
                        continue;
                    }
                    for (TypeMirror iface : extraMixins) {
                        TypeRef resolved = typeResolver.resolve(iface);
                        if (implemented.add(resolved.qualifiedName())) {
                            implementsTypes.add(resolved);
                            heritage.add(iface);
                        }
                        if (iface.getKind() == TypeKind.DECLARED) {
                            superTypes.add((TypeElement) ((DeclaredType) iface).asElement());
                        }
                    }
                }
            }
            for (TypeRef supertype : implementsTypes) {
                heritageTypes.add(supertype.qualifiedName());
            }

            List<String> modifiers = new ArrayList<>();
            for (Modifier modifier : type.getModifiers()) {
                modifiers.add(modifier.toString());
            }

            DocComment classComment = docCommentParser.parse(type);
            List<MemberDoc> members = new ArrayList<>();
            List<ExecutableElement> instanceMethods = new ArrayList<>();

            Set<String> eventSkipNames = new HashSet<>();
            if (group == ClassGroup.Event) {
                Map<String, List<ExecutableElement>> eventMethodsByName = new HashMap<>();
                for (Element enclosed : type.getEnclosedElements()) {
                    if (enclosed.getKind() != ElementKind.METHOD) {
                        continue;
                    }
                    if (!enclosed.getModifiers().contains(Modifier.PUBLIC)) {
                        continue;
                    }
                    ExecutableElement method = (ExecutableElement) enclosed;
                    eventMethodsByName.computeIfAbsent(method.getSimpleName().toString(), key -> new ArrayList<>())
                        .add(method);
                }
                for (Map.Entry<String, List<ExecutableElement>> entry : eventMethodsByName.entrySet()) {
                    String name = entry.getKey();
                    if (!objectMethodNames.contains(name)) {
                        continue;
                    }
                    boolean allObjectMethods = true;
                    for (ExecutableElement method : entry.getValue()) {
                        if (!isObjectMethod(method, type)) {
                            allObjectMethods = false;
                            break;
                        }
                    }
                    if (allObjectMethods) {
                        eventSkipNames.add(name);
                    }
                }
            }

            for (Element enclosed : type.getEnclosedElements()) {
                if (hasDocletIgnore(enclosed)) {
                    continue;
                }
                addDeclaredType(enclosed);
                if (!enclosed.getModifiers().contains(Modifier.PUBLIC)) {
                    continue;
                }
                if (enclosed.getKind() == ElementKind.FIELD || enclosed.getKind() == ElementKind.ENUM_CONSTANT) {
                    members.add(buildField(enclosed));
                } else if (enclosed.getKind() == ElementKind.METHOD) {
                    ExecutableElement method = (ExecutableElement) enclosed;
                    String methodName = method.getSimpleName().toString();
                    if (group == ClassGroup.Event && eventSkipNames.contains(methodName)) {
                        continue;
                    }
                    if (group != ClassGroup.Event && isObjectMethod(method, type)) {
                        continue;
                    }
                    if (isObfuscated(method, type, superMcTypes)) {
                        continue;
                    }
                    members.add(buildMethod(method));
                    if (!method.getModifiers().contains(Modifier.STATIC)) {
                        instanceMethods.add(method);
                    }
                } else if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
                    members.add(buildConstructor((ExecutableElement) enclosed));
                }
            }

            if (group != ClassGroup.Event) {
                addSuperMethods(type, superTypes, superMcTypes, instanceMethods, members);
            }

            markShapeConflicts(type, members, false, false);
            markSelfReturns(type, members);
            members.sort(Comparator.comparing(MemberDoc::name, String.CASE_INSENSITIVE_ORDER));

            ClassDoc classDoc = new ClassDoc(
                displayName,
                qualifiedName,
                packageName,
                kind,
                group,
                alias,
                category,
                eventCancellable,
                eventFilterer,
                typeParams,
                extendsTypes,
                implementsTypes,
                modifiers,
                classComment,
                members,
                capturedTypeParams,
                false,
                heritageConflicts(heritage) || interfaceConflict,
                staticConflict
            );

            packages.computeIfAbsent(packageName, key -> new ArrayList<>()).add(classDoc);
            documented.add(qualifiedName);
        }

        List<PackageDoc> packageDocs = new ArrayList<>();
        for (Map.Entry<String, List<ClassDoc>> entry : packages.entrySet()) {
            entry.getValue().sort(Comparator.comparing(ClassDoc::qualifiedName, String.CASE_INSENSITIVE_ORDER));
            packageDocs.add(new PackageDoc(entry.getKey(), entry.getValue()));
        }
        packageDocs.sort(Comparator.comparing(PackageDoc::name, String.CASE_INSENSITIVE_ORDER));

        // Each short name must lead to a documented class that Python can import and the site can link to.
        for (var namespace : namespaces) {
            for (var export : namespace.exports()) {
                if (!documented.contains(export.target().qualifiedName())) {
                    throw new IllegalArgumentException("Script export must target a documented public API type: "
                            + namespace.name() + "." + export.name() + " -> " + export.target().qualifiedName());
                }
            }
        }
        List<ClassDoc> referenced = buildReferencedTypes(documented);
        return new DocletModel(packageDocs, declaredTypes, referenced, undeclaredTypes, namespaces);
    }

    /**
     * The types the documented API mentions that are not documented themselves. A reference to
     * one of them has to resolve in the generated definitions, so each one gets a declaration
     * of its own. The set grows while it is built, because a declared type mentions others.
     */
    private final Set<String> undeclaredTypes = new HashSet<>();

    /**
     * The types the documented classes build on. A type of these that is not part of the API
     * still has to be declared, or the class that builds on it cannot be written.
     */
    private final Set<String> heritageTypes = new HashSet<>();

    private List<ClassDoc> buildReferencedTypes(Set<String> documented) {
        List<ClassDoc> referenced = new ArrayList<>();
        if (!(typeResolver instanceof BasicTypeResolver resolver)) {
            return referenced;
        }
        Set<String> done = new HashSet<>();
        boolean grew = true;
        while (grew) {
            grew = false;
            for (TypeElement element : new ArrayList<>(resolver.referencedTypes().values())) {
                String qualified = ElementNameUtils.getQualifiedName(element);
                if (!done.add(qualified)) {
                    continue;
                }
                grew = true;
                if (documented.contains(qualified)
                    || HEADER_DECLARED_TYPES.contains(qualified)
                    || isMinecraftType(element.asType())) {
                    continue;
                }
                if (hasDocletIgnore(element)) {
                    if (!heritageTypes.contains(qualified)) {
                        // The type is not part of the documented API, so it is not declared, and
                        // a reference to it has to say that rather than point at nothing.
                        undeclaredTypes.add(qualified);
                        continue;
                    }
                    // The type is not part of the API, but a class of the API builds on it, so
                    // it is declared all the same, and marked as what it is.
                    referenced.add(buildReferencedClass(element, true));
                    continue;
                }
                referenced.add(buildReferencedClass(element, false));
            }
        }
        referenced.sort(Comparator.comparing(ClassDoc::qualifiedName, String.CASE_INSENSITIVE_ORDER));
        return referenced;
    }

    /**
     * Marks the members whose shape cannot be written the way the definitions write them: a
     * member that answers to the same name on the type it builds on, with a different shape.
     * Java allows that, the type system of the definitions does not.
     * <p>
     * The members of a name are written as one set of calls, so a member of the type it builds
     * on is answered as long as one of them fits it. Only a name that one of them does not fit
     * is a shape the definitions cannot write.
     */
    /** Whether the static side of the class being built cannot be written. */
    private boolean staticConflict = false;

    /** Whether the interface the class is written as cannot be written. */
    private boolean interfaceConflict = false;

    private void markShapeConflicts(
        TypeElement type,
        List<MemberDoc> members,
        boolean compareResults,
        boolean ownClass
    ) {
        Map<String, List<Element>> classSide = classSideMembers(type);
        Map<String, List<Element>> interfaceSide = interfaceSideMembers(type);
        if (classSide.isEmpty() && interfaceSide.isEmpty()) {
            return;
        }
        Map<String, List<Integer>> own = new LinkedHashMap<>();
        for (int index = 0; index < members.size(); index++) {
            own.computeIfAbsent(members.get(index).name(), key -> new ArrayList<>()).add(index);
        }
        Set<Integer> conflicting = new LinkedHashSet<>();
        boolean interfaceConflict = false;
        for (Map.Entry<String, List<Element>> entry : classSide.entrySet()) {
            conflicting.addAll(unanswered(type, entry, own, members));
        }
        for (Map.Entry<String, List<Element>> entry : interfaceSide.entrySet()) {
            Set<Integer> unanswered = unanswered(type, entry, own, members);
            if (unanswered.isEmpty()) {
                continue;
            }
            // A class that builds on an interface is written as an interface of its own, and
            // that interface is what answers to the interface it builds on.
            interfaceConflict = true;
            if (ownClass) {
                // A class that is written on its own answers to the interface itself.
                conflicting.addAll(unanswered);
            }
        }
        // A narrower result stands in for the wider one only when the type that gives it can
        // stand in for the type that wider one is. A type that answers a name wrongly cannot,
        // so the narrower results of such a type cannot be written either.
        if (compareResults && !conflicting.isEmpty()) {
            for (int index = 0; index < members.size(); index++) {
                if (conflicting.contains(index)) {
                    continue;
                }
                MemberDoc member = members.get(index);
                List<Element> candidates = classSide.get(member.name());
                if (candidates == null) {
                    continue;
                }
                for (Element candidate : candidates) {
                    if (narrowerThan(member, candidate)) {
                        conflicting.add(index);
                        break;
                    }
                }
            }
        }
        boolean staticConflict = false;
        for (int index : conflicting) {
            members.set(index, withShapeConflict(members.get(index)));
            MemberDoc member = members.get(index);
            if (member.modifiers().contains("static") || isValueAgainstCall(member, classSide.get(member.name()))) {
                // The static side of the type is written apart from the rest of it, and a name
                // that is a call on one side and a value on the other is written apart as well.
                staticConflict = true;
            }
        }
        this.staticConflict = staticConflict;
        this.interfaceConflict = interfaceConflict;
    }

    /**
     * The members of the class that answer to a name of the type it builds on that none of them
     * fits. The members of a name are written as one set of calls, so a member of the type it
     * builds on is answered as long as one of them fits it.
     */
    private Set<Integer> unanswered(
        TypeElement type,
        Map.Entry<String, List<Element>> entry,
        Map<String, List<Integer>> own,
        List<MemberDoc> members
    ) {
        List<Integer> indexes = own.get(entry.getKey());
        if (indexes == null) {
            return Set.of();
        }
        for (Element candidate : entry.getValue()) {
            boolean covered = false;
            for (int index : indexes) {
                if (fits(members.get(index), candidate)) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                return new LinkedHashSet<>(indexes);
            }
        }
        return Set.of();
    }

    /** Whether the member is a call where the type it builds on has a value of that name. */
    private boolean isValueAgainstCall(MemberDoc member, List<Element> candidates) {
        if (candidates == null) {
            return false;
        }
        for (Element candidate : candidates) {
            if ((member.kind() == MemberKind.METHOD) != (candidate.getKind() == ElementKind.METHOD)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the member answers for the member of the type it builds on: the definitions
     * write the two under one name, so the member has to be able to stand in for it.
     */
    private boolean fits(MemberDoc member, Element candidate) {
        boolean candidateIsMethod = candidate.getKind() == ElementKind.METHOD;
        if (member.kind() == MemberKind.METHOD != candidateIsMethod) {
            // One of them is a call and the other is a value, and a name cannot be both.
            return false;
        }
        if (!candidateIsMethod) {
            // A value of a narrower type stands in for one of a wider type.
            if (paramTypeKey(((VariableElement) candidate).asType())
                .equals(paramTypeKey(member.returnType()))) {
                return true;
            }
            return isDeclared(member.returnType()) && isDeclaredType(((VariableElement) candidate).asType());
        }
        ExecutableElement method = (ExecutableElement) candidate;
        // The static side of a type is written apart from the rest of it.
        boolean candidateIsStatic = method.getModifiers().contains(Modifier.STATIC);
        if (candidateIsStatic != member.modifiers().contains("static")) {
            return false;
        }
        List<? extends VariableElement> parameters = method.getParameters();
        if (member.params().size() > parameters.size()) {
            // A call that takes fewer arguments stands in for one that takes more.
            return false;
        }
        for (int index = 0; index < member.params().size(); index++) {
            if (!paramsMatch(parameters.get(index).asType(), member.params().get(index).type())) {
                return false;
            }
        }
        // The result has to stand in for the result of the member it builds on.
        return resultFits(member.returnType(), method.getReturnType());
    }

    /**
     * Whether the member gives a narrower result than the member of the type it builds on.
     */
    private boolean narrowerThan(MemberDoc member, Element candidate) {
        if (member.kind() != MemberKind.METHOD || candidate.getKind() != ElementKind.METHOD) {
            return false;
        }
        ExecutableElement method = (ExecutableElement) candidate;
        if (method.getModifiers().contains(Modifier.STATIC) != member.modifiers().contains("static")) {
            return false;
        }
        List<? extends VariableElement> parameters = method.getParameters();
        if (member.params().size() > parameters.size()) {
            return false;
        }
        for (int index = 0; index < member.params().size(); index++) {
            if (!paramTypeKey(parameters.get(index).asType())
                .equals(paramTypeKey(member.params().get(index).type()))) {
                return false;
            }
        }
        if (returnTypeKey(method.getReturnType()).equals(returnTypeKey(member.returnType()))) {
            return false;
        }
        return isDeclared(member.returnType()) && isDeclaredType(method.getReturnType())
            && resultFits(member.returnType(), method.getReturnType());
    }

    /**
     * Whether the result of a member stands in for the result of the member it builds on. A
     * narrower one does; one that is not a type of its own does not.
     */
    private boolean resultFits(TypeRef memberResult, TypeMirror candidateResult) {
        if (isAny(memberResult) || isAnyType(candidateResult)) {
            // A type the definitions do not know answers to every type.
            return true;
        }
        if (returnTypeKey(candidateResult).equals(returnTypeKey(memberResult))) {
            // The same type with other arguments: the arguments have to fit as well.
            return candidateResult.getKind() == TypeKind.DECLARED
                && argumentsFit(memberResult, candidateResult);
        }
        // A narrower result stands in for a wider one.
        if (memberResult == null || memberResult.kind() != com.jsmacrosce.doclet.core.model.TypeKind.DECLARED) {
            return false;
        }
        if (candidateResult == null || candidateResult.getKind() != TypeKind.DECLARED) {
            return false;
        }
        TypeElement derived = elementUtils.getTypeElement(memberResult.qualifiedName());
        if (derived == null) {
            return false;
        }
        return typeUtils != null && typeUtils.isAssignable(derived.asType(), candidateResult);
    }

    /** Whether the arguments of a type answer to the arguments of the type it stands in for. */
    private boolean argumentsFit(TypeRef memberResult, TypeMirror candidateResult) {
        List<TypeRef> memberArgs = memberResult.typeArgs();
        List<? extends TypeMirror> candidateArgs = ((DeclaredType) candidateResult).getTypeArguments();
        if (memberArgs.size() != candidateArgs.size()) {
            return false;
        }
        for (int index = 0; index < memberArgs.size(); index++) {
            TypeRef memberArg = memberArgs.get(index);
            TypeRef candidateArg = typeResolver.resolve(candidateArgs.get(index));
            String written = typeResolver.format(memberArg, TargetLanguage.TYPESCRIPT, false);
            String inherited = typeResolver.format(candidateArg, TargetLanguage.TYPESCRIPT, false);
            if (written.equals(inherited)) {
                continue;
            }
            if (isAny(memberArg) || isAny(candidateArg)) {
                // A type the definitions do not know answers to every type.
                continue;
            }
            if (memberArg.kind() == com.jsmacrosce.doclet.core.model.TypeKind.TYPEVAR
                || candidateArg.kind() == com.jsmacrosce.doclet.core.model.TypeKind.TYPEVAR) {
                // A type parameter answers to every type.
                continue;
            }
            if (memberArg.kind() != com.jsmacrosce.doclet.core.model.TypeKind.DECLARED
                || candidateArg.kind() != com.jsmacrosce.doclet.core.model.TypeKind.DECLARED) {
                return false;
            }
            TypeElement memberElement = elementUtils.getTypeElement(memberArg.qualifiedName());
            TypeElement candidateElement = elementUtils.getTypeElement(candidateArg.qualifiedName());
            if (memberElement == null || candidateElement == null) {
                return false;
            }
            if (typeUtils == null
                || !typeUtils.isAssignable(memberElement.asType(), candidateElement.asType())) {
                return false;
            }
        }
        return true;
    }

    private boolean isDeclared(TypeRef type) {
        return type != null && type.kind() == com.jsmacrosce.doclet.core.model.TypeKind.DECLARED;
    }

    private boolean isDeclaredType(TypeMirror type) {
        return type != null && type.getKind() == TypeKind.DECLARED;
    }

    private MemberDoc withShapeConflict(MemberDoc member) {
        return new MemberDoc(
            member.kind(),
            member.name(),
            member.anchorId(),
            member.params(),
            member.typeParams(),
            member.returnType(),
            member.replaceParams(),
            member.replaceReturn(),
            member.replaceTypeParams(),
            member.modifiers(),
            member.scriptReadOnly(),
            member.docComment(),
            true,
            member.selfReturn()
        );
    }

    /**
     * Marks the members that hand back the type they belong to, the way the member they build
     * on does. A member that returns the type of the class it is declared in is written as
     * returning that type, so a class that builds on it has to be written the same way.
     */
    private void markSelfReturns(TypeElement type, List<MemberDoc> members) {
        Map<String, List<Element>> inherited = inheritedMembers(type);
        if (inherited.isEmpty()) {
            return;
        }
        for (int index = 0; index < members.size(); index++) {
            MemberDoc member = members.get(index);
            if (member.kind() != MemberKind.METHOD) {
                continue;
            }
            List<Element> candidates = inherited.get(member.name());
            if (candidates == null) {
                continue;
            }
            for (Element candidate : candidates) {
                if (returnsItself(candidate) && returnsSameType(member, candidate)) {
                    members.set(index, withSelfReturn(member));
                    break;
                }
            }
        }
    }

    private MemberDoc withSelfReturn(MemberDoc member) {
        return new MemberDoc(
            member.kind(),
            member.name(),
            member.anchorId(),
            member.params(),
            member.typeParams(),
            member.returnType(),
            member.replaceParams(),
            member.replaceReturn(),
            member.replaceTypeParams(),
            member.modifiers(),
            member.scriptReadOnly(),
            member.docComment(),
            member.shapeConflict(),
            true
        );
    }

    /**
     * Whether the member hands back the type it is declared in, and says so. The definitions
     * write such a member as returning the type it belongs to.
     */
    private boolean returnsItself(Element candidate) {
        if (candidate.getKind() != ElementKind.METHOD) {
            return false;
        }
        ExecutableElement method = (ExecutableElement) candidate;
        String owner = ElementNameUtils.getQualifiedName((TypeElement) method.getEnclosingElement());
        String returned = declaredTypeName(method.getReturnType());
        if (owner == null || !owner.equals(returned)) {
            return false;
        }
        DocComment comment = docCommentParser.parse(method);
        for (DocTag tag : comment.tags()) {
            if (tag.kind() != DocTagKind.RETURN) {
                continue;
            }
            String text = DocBodyRenderer.toPlainText(
                tag.body(),
                link -> link.label() != null ? link.label() : link.signature()
            ).trim();
            return text.equals("self") || text.startsWith("self ");
        }
        return false;
    }

    /** Whether the member returns the same type as the member it builds on. */
    private boolean returnsSameType(MemberDoc member, Element candidate) {
        TypeRef returnType = member.returnType();
        if (returnType == null || returnType.kind() != com.jsmacrosce.doclet.core.model.TypeKind.DECLARED) {
            return false;
        }
        String returned = declaredTypeName(((ExecutableElement) candidate).getReturnType());
        return returned != null && returned.equals(returnType.qualifiedName());
    }

    private String declaredTypeName(TypeMirror type) {
        if (type == null || type.getKind() != TypeKind.DECLARED) {
            return null;
        }
        Element element = ((DeclaredType) type).asElement();
        if (!(element instanceof TypeElement typeElement)) {
            return null;
        }
        return ElementNameUtils.getQualifiedName(typeElement);
    }

    /**
     * Whether the member answers for the member of the type it builds on: the definitions
     * write the two under one name, so the member has to be able to stand in for it.
     */
    private boolean fits(MemberDoc member, Element candidate, boolean compareResults) {
        boolean candidateIsMethod = candidate.getKind() == ElementKind.METHOD;
        if (member.kind() == MemberKind.METHOD != candidateIsMethod) {
            // One of them is a call and the other is a value, and a name cannot be both.
            return false;
        }
        if (!candidateIsMethod) {
            // A value of a narrower type stands in for one of a wider type.
            if (paramTypeKey(((VariableElement) candidate).asType())
                .equals(paramTypeKey(member.returnType()))) {
                return true;
            }
            return isDeclared(member.returnType()) && isDeclaredType(((VariableElement) candidate).asType());
        }
        ExecutableElement method = (ExecutableElement) candidate;
        // The static side of a type is written apart from the rest of it.
        boolean candidateIsStatic = method.getModifiers().contains(Modifier.STATIC);
        if (candidateIsStatic != member.modifiers().contains("static")) {
            return false;
        }
        List<? extends VariableElement> parameters = method.getParameters();
        if (member.params().size() > parameters.size()) {
            // A call that takes fewer arguments stands in for one that takes more.
            return false;
        }
        for (int index = 0; index < member.params().size(); index++) {
            if (!paramTypeKey(parameters.get(index).asType())
                .equals(paramTypeKey(member.params().get(index).type()))) {
                return false;
            }
        }
        if (compareResults && !returnTypeKey(method.getReturnType()).equals(returnTypeKey(member.returnType()))) {
            // A narrower result stands in for a wider one only when it answers to it, and it
            // answers to it only when the wider one is a class it builds on.
            return !isInterface(method.getReturnType());
        }
        return true;
    }

    /**
     * Whether the two types of a parameter answer to each other. A type the definitions do not
     * know answers to every type, so a parameter of such a type answers to any other.
     */
    private boolean paramsMatch(TypeMirror candidate, TypeRef member) {
        if (isAnyType(candidate) || isAny(member)) {
            return true;
        }
        return paramTypeKey(candidate).equals(paramTypeKey(member));
    }

    /** Whether the type is one the definitions do not know, which is nothing but any. */
    private boolean isAny(TypeRef type) {
        if (type == null) {
            return true;
        }
        if (type.kind() == com.jsmacrosce.doclet.core.model.TypeKind.UNKNOWN) {
            return true;
        }
        return type.qualifiedName().startsWith("net.minecraft.");
    }

    private boolean isAnyType(TypeMirror type) {
        if (type == null || type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        Element element = ((DeclaredType) type).asElement();
        if (!(element instanceof TypeElement typeElement)) {
            return false;
        }
        return ElementNameUtils.getQualifiedName(typeElement).startsWith("net.minecraft.");
    }

    /** Whether the type is an interface, which a narrower result has to answer to. */
    private boolean isInterface(TypeMirror type) {
        if (type == null || type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        Element element = ((DeclaredType) type).asElement();
        return element instanceof TypeElement typeElement && typeElement.getKind() == ElementKind.INTERFACE;
    }

    private String returnTypeKey(TypeMirror type) {
        if (type == null) {
            return "";
        }
        return switch (type.getKind()) {
            case DECLARED -> declaredTypeName(type);
            case ARRAY -> returnTypeKey(((javax.lang.model.type.ArrayType) type).getComponentType()) + "[]";
            case TYPEVAR -> type.toString();
            default -> type.toString();
        };
    }

    private String returnTypeKey(TypeRef type) {
        if (type == null) {
            return "";
        }
        return switch (type.kind()) {
            case DECLARED -> type.qualifiedName();
            case ARRAY -> returnTypeKey(type.typeArgs().isEmpty() ? null : type.typeArgs().getFirst()) + "[]";
            case TYPEVAR -> type.name();
            default -> type.qualifiedName();
        };
    }

    private String paramTypeKey(javax.lang.model.type.TypeMirror type) {
        if (type == null) {
            return "";
        }
        return switch (type.getKind()) {
            case DECLARED -> ElementNameUtils.getQualifiedName(
                ((DeclaredType) type).asElement() instanceof TypeElement element ? element : null);
            case ARRAY -> paramTypeKey(((javax.lang.model.type.ArrayType) type).getComponentType()) + "[]";
            case TYPEVAR -> type.toString();
            default -> type.toString();
        };
    }

    private String paramTypeKey(TypeRef type) {
        if (type == null) {
            return "";
        }
        return switch (type.kind()) {
            case DECLARED -> type.qualifiedName();
            case ARRAY -> paramTypeKey(type.typeArgs().isEmpty() ? null : type.typeArgs().getFirst()) + "[]";
            case TYPEVAR -> type.name();
            default -> type.qualifiedName();
        };
    }

    /**
     * Whether the types a class builds on cannot be written together, because two of them
     * answer to the same name with a different shape. Java allows that; the type system of the
     * definitions does not.
     */
    private boolean heritageConflicts(List<TypeMirror> heritage) {
        for (int first = 0; first < heritage.size(); first++) {
            for (int second = first + 1; second < heritage.size(); second++) {
                Map<String, List<Element>> firstMembers = membersOf(heritage.get(first));
                Map<String, List<Element>> secondMembers = membersOf(heritage.get(second));
                for (Map.Entry<String, List<Element>> entry : firstMembers.entrySet()) {
                    List<Element> others = secondMembers.get(entry.getKey());
                    if (others == null) {
                        continue;
                    }
                    for (Element one : entry.getValue()) {
                        for (Element other : others) {
                            if (membersConflict(one, other)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /** The members a type answers to, its own and the ones it takes from the types it builds on. */
    private Map<String, List<Element>> membersOf(TypeMirror type) {
        Map<String, List<Element>> members = new HashMap<>();
        if (type == null || type.getKind() != TypeKind.DECLARED) {
            return members;
        }
        Element element = ((DeclaredType) type).asElement();
        if (!(element instanceof TypeElement typeElement)) {
            return members;
        }
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (!enclosed.getModifiers().contains(Modifier.PUBLIC)) {
                continue;
            }
            if (enclosed.getKind() == ElementKind.METHOD
                || enclosed.getKind() == ElementKind.FIELD
                || enclosed.getKind() == ElementKind.ENUM_CONSTANT) {
                members.computeIfAbsent(enclosed.getSimpleName().toString(), key -> new ArrayList<>())
                    .add(enclosed);
            }
        }
        for (Map.Entry<String, List<Element>> entry : inheritedMembers(typeElement).entrySet()) {
            members.computeIfAbsent(entry.getKey(), key -> new ArrayList<>()).addAll(entry.getValue());
        }
        return members;
    }

    /** Whether two members cannot be written under one name. */
    private boolean membersConflict(Element one, Element other) {
        boolean oneIsMethod = one.getKind() == ElementKind.METHOD;
        boolean otherIsMethod = other.getKind() == ElementKind.METHOD;
        if (oneIsMethod != otherIsMethod) {
            // One of them is a call and the other is a value, and a name cannot be both.
            return true;
        }
        if (!oneIsMethod) {
            return !paramTypeKey(((VariableElement) one).asType())
                .equals(paramTypeKey(((VariableElement) other).asType()));
        }
        ExecutableElement first = (ExecutableElement) one;
        ExecutableElement second = (ExecutableElement) other;
        if (first.getParameters().size() != second.getParameters().size()) {
            return true;
        }
        for (int index = 0; index < first.getParameters().size(); index++) {
            if (!paramTypeKey(first.getParameters().get(index).asType())
                .equals(paramTypeKey(second.getParameters().get(index).asType()))) {
                return true;
            }
        }
        return !returnTypeKey(first.getReturnType()).equals(returnTypeKey(second.getReturnType()));
    }

    /** The public members of the types the given type builds on, by name. */
    private Map<String, List<Element>> inheritedMembers(TypeElement type) {
        return classSideMembers(type);
    }

    /**
     * The public members of the class the given type builds on, by name. The members it takes
     * from the interfaces that class builds on are part of them.
     */
    private Map<String, List<Element>> classSideMembers(TypeElement type) {
        Map<String, List<Element>> inherited = new HashMap<>();
        Set<TypeElement> visited = new HashSet<>();
        Deque<TypeMirror> pending = new ArrayDeque<>();
        TypeMirror superclass = type.getSuperclass();
        if (superclass != null && superclass.getKind() == javax.lang.model.type.TypeKind.DECLARED) {
            pending.add(superclass);
        }
        while (!pending.isEmpty()) {
            TypeMirror mirror = pending.poll();
            if (mirror.getKind() != javax.lang.model.type.TypeKind.DECLARED) {
                continue;
            }
            Element element = ((DeclaredType) mirror).asElement();
            if (!(element instanceof TypeElement superType) || !visited.add(superType)) {
                continue;
            }
            collectMembers(superType, inherited);
            TypeMirror superSuper = superType.getSuperclass();
            if (superSuper != null && superSuper.getKind() == javax.lang.model.type.TypeKind.DECLARED) {
                pending.add(superSuper);
            }
            pending.addAll(superType.getInterfaces());
        }
        return inherited;
    }

    /**
     * The public members of the interfaces the given type builds on, by name. A class does not
     * take the members of a class through an interface, so only the interfaces are followed.
     */
    private Map<String, List<Element>> interfaceSideMembers(TypeElement type) {
        Map<String, List<Element>> inherited = new HashMap<>();
        Set<TypeElement> visited = new HashSet<>();
        Deque<TypeMirror> pending = new ArrayDeque<>(type.getInterfaces());
        while (!pending.isEmpty()) {
            TypeMirror mirror = pending.poll();
            if (mirror.getKind() != javax.lang.model.type.TypeKind.DECLARED) {
                continue;
            }
            Element element = ((DeclaredType) mirror).asElement();
            if (!(element instanceof TypeElement superType) || !visited.add(superType)) {
                continue;
            }
            collectMembers(superType, inherited);
            pending.addAll(superType.getInterfaces());
        }
        return inherited;
    }

    private void collectMembers(TypeElement type, Map<String, List<Element>> inherited) {
        for (Element enclosed : type.getEnclosedElements()) {
            if (!enclosed.getModifiers().contains(Modifier.PUBLIC)) {
                continue;
            }
            if (enclosed.getKind() == ElementKind.METHOD
                || enclosed.getKind() == ElementKind.FIELD
                || enclosed.getKind() == ElementKind.ENUM_CONSTANT) {
                inherited.computeIfAbsent(enclosed.getSimpleName().toString(), key -> new ArrayList<>())
                    .add(enclosed);
            }
        }
    }

    private ClassDoc buildReferencedClass(TypeElement type, boolean internal) {
        ClassKind kind = switch (type.getKind()) {
            case INTERFACE -> ClassKind.INTERFACE;
            case ENUM -> ClassKind.ENUM;
            case ANNOTATION_TYPE -> ClassKind.ANNOTATION;
            default -> ClassKind.CLASS;
        };

        List<TypeRef> typeParams = new ArrayList<>();
        for (TypeParameterElement param : type.getTypeParameters()) {
            typeParams.add(typeResolver.resolve(param.asType()));
        }

        List<TypeRef> capturedTypeParams = new ArrayList<>();
        for (NestedTypeParams.Captured captured : NestedTypeParams.capturedParams(type)) {
            capturedTypeParams.add(typeResolver.resolve(captured.element().asType()));
        }

        List<TypeRef> extendsTypes = new ArrayList<>();
        List<TypeMirror> heritage = new ArrayList<>();
        if (type.getSuperclass() != null && type.getSuperclass().getKind() != javax.lang.model.type.TypeKind.NONE) {
            String sup = type.getSuperclass().toString();
            if (!"java.lang.Object".equals(sup)) {
                extendsTypes.add(typeResolver.resolve(type.getSuperclass()));
                heritage.add(type.getSuperclass());
            }
        }
        List<TypeRef> implementsTypes = new ArrayList<>();
        Set<String> implemented = new HashSet<>();
        for (var iface : type.getInterfaces()) {
            TypeRef resolved = typeResolver.resolve(iface);
            if (implemented.add(resolved.qualifiedName())) {
                implementsTypes.add(resolved);
                heritage.add(iface);
            }
        }

        List<String> modifiers = new ArrayList<>();
        for (Modifier modifier : type.getModifiers()) {
            modifiers.add(modifier.toString());
        }

        List<MemberDoc> members = new ArrayList<>();
        for (Element enclosed : type.getEnclosedElements()) {
            if (!enclosed.getModifiers().contains(Modifier.PUBLIC)) {
                continue;
            }
            if (enclosed.getKind() == ElementKind.FIELD || enclosed.getKind() == ElementKind.ENUM_CONSTANT) {
                members.add(buildField(enclosed));
            } else if (enclosed.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) enclosed;
                if (isObjectMethod(method, type)) {
                    continue;
                }
                members.add(buildMethod(method));
            } else if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
                members.add(buildConstructor((ExecutableElement) enclosed));
            }
        }

        markShapeConflicts(type, members, true, true);
        markSelfReturns(type, members);

        return new ClassDoc(
            ElementNameUtils.getDisplayClassName(type),
            ElementNameUtils.getQualifiedName(type),
            ElementNameUtils.getPackageName(type),
            kind,
            ClassGroup.Class,
            null,
            null,
            false,
            null,
            typeParams,
            extendsTypes,
            implementsTypes,
            modifiers,
            docCommentParser.parse(type),
            members,
            capturedTypeParams,
            internal,
            heritageConflicts(heritage) || interfaceConflict,
            staticConflict
        );
    }

    private void initObjectMethods() {
        TypeElement objectElement = elementUtils.getTypeElement("java.lang.Object");
        if (objectElement == null) {
            objectMethods = Set.of();
            objectMethodNames = Set.of();
            return;
        }
        Set<ExecutableElement> methods = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (Element element : objectElement.getEnclosedElements()) {
            if (element.getKind() != ElementKind.METHOD) {
                continue;
            }
            if (!element.getModifiers().contains(Modifier.PUBLIC) || element.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            ExecutableElement method = (ExecutableElement) element;
            methods.add(method);
            names.add(method.getSimpleName().toString());
        }
        objectMethods = methods;
        objectMethodNames = names;
    }

    private boolean isObjectMethod(ExecutableElement method, TypeElement owner) {
        if (!objectMethodNames.contains(method.getSimpleName().toString())) {
            return false;
        }
        if (docTrees != null && docTrees.getDocCommentTree(method) != null) {
            return false;
        }
        for (ExecutableElement objectMethod : objectMethods) {
            if (elementUtils.overrides(method, objectMethod, owner)) {
                return true;
            }
        }
        return false;
    }

    private void collectSuperTypes(
        TypeElement type,
        Set<TypeElement> superTypes,
        Set<TypeElement> superMcTypes,
        Set<TypeElement> visited
    ) {
        if (!visited.add(type)) {
            return;
        }
        if (!type.getKind().isInterface()) {
            TypeMirror superType = type.getSuperclass();
            if (superType != null && superType.getKind() == TypeKind.DECLARED) {
                TypeElement superElement = (TypeElement) ((DeclaredType) superType).asElement();
                if (isMinecraftType(superType)) {
                    superMcTypes.add(superElement);
                } else {
                    superTypes.add(superElement);
                }
                collectSuperTypes(superElement, superTypes, superMcTypes, visited);
            }
        }
        for (TypeMirror iface : type.getInterfaces()) {
            if (iface.getKind() != TypeKind.DECLARED) {
                continue;
            }
            TypeElement ifaceElement = (TypeElement) ((DeclaredType) iface).asElement();
            if (isMinecraftType(iface)) {
                superMcTypes.add(ifaceElement);
            } else {
                superTypes.add(ifaceElement);
            }
            collectSuperTypes(ifaceElement, superTypes, superMcTypes, visited);
        }
    }

    private boolean isMinecraftType(TypeMirror type) {
        if (type == null || type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        Element element = ((DeclaredType) type).asElement();
        if (!(element instanceof TypeElement typeElement)) {
            return false;
        }
        // The game keeps a few types in the root package of its own, next to the ones in the
        // packages below it, so the name of the type is what tells them apart.
        String qualified = ElementNameUtils.getQualifiedName(typeElement);
        return qualified.startsWith("net.minecraft.");
    }

    /**
     * An override of a Minecraft method keeps that method's name, so the name decides whether the
     * override is worth documenting. A mapped name such as {@code getSelectedEntities()} is a real
     * call a script can make and belongs in the reference; only an intermediary-style name
     * ({@code method_1234}) carries no meaning outside the obfuscated game and is dropped.
     */
    private boolean isObfuscated(ExecutableElement method, TypeElement owner, Set<TypeElement> superMcTypes) {
        if (method.getAnnotation(Override.class) == null) {
            return false;
        }
        for (TypeElement mcType : superMcTypes) {
            for (Element element : mcType.getEnclosedElements()) {
                if (element.getKind() != ElementKind.METHOD) {
                    continue;
                }
                if (elementUtils.overrides(method, (ExecutableElement) element, owner)) {
                    return isObfuscatedName(method.getSimpleName().toString());
                }
            }
        }
        return false;
    }

    /** Matches the intermediary names Mojang's official mappings and Fabric's intermediary use. */
    private static boolean isObfuscatedName(String name) {
        return name.matches("(?:method|field|comp|lambda|class|record)_\\d+");
    }

    private void addSuperMethods(
        TypeElement owner,
        Set<TypeElement> superTypes,
        Set<TypeElement> superMcTypes,
        List<ExecutableElement> instanceMethods,
        List<MemberDoc> members
    ) {
        if (instanceMethods.isEmpty() || superTypes.isEmpty()) {
            return;
        }
        Set<String> methodNames = new HashSet<>();
        for (ExecutableElement method : instanceMethods) {
            methodNames.add(method.getSimpleName().toString());
        }
        if (methodNames.isEmpty()) {
            return;
        }

        List<ExecutableElement> superMethods = new ArrayList<>();
        for (TypeElement superType : superTypes) {
            for (Element element : superType.getEnclosedElements()) {
                if (element.getKind() != ElementKind.METHOD) {
                    continue;
                }
                if (!element.getModifiers().contains(Modifier.PUBLIC)
                    || element.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }
                ExecutableElement method = (ExecutableElement) element;
                if (!methodNames.contains(method.getSimpleName().toString())) {
                    continue;
                }

                boolean overridden = false;
                for (ExecutableElement existing : instanceMethods) {
                    if (existing.getSimpleName().contentEquals(method.getSimpleName())
                        && elementUtils.overrides(existing, method, owner)) {
                        overridden = true;
                        break;
                    }
                }
                if (overridden) {
                    continue;
                }
                for (ExecutableElement existing : superMethods) {
                    if (existing.getSimpleName().contentEquals(method.getSimpleName())
                        && elementUtils.overrides(existing, method, owner)) {
                        overridden = true;
                        break;
                    }
                }
                if (overridden) {
                    continue;
                }
                if (hasDocletIgnore(element)) {
                    continue;
                }
                if (isObfuscated(method, superType, superMcTypes)) {
                    continue;
                }
                superMethods.add(method);
            }
        }

        for (ExecutableElement method : superMethods) {
            members.add(buildMethod(method, asSeenIn(owner, method)));
        }
    }

    /**
     * The type of a member of the type the given type builds on, as the type that builds on it
     * sees it: the type parameters of the type it builds on are the ones it was given.
     */
    private ExecutableType asSeenIn(TypeElement owner, ExecutableElement method) {
        if (typeUtils == null) {
            return null;
        }
        Element ownerOfMethod = method.getEnclosingElement();
        DeclaredType seen = null;
        TypeMirror superclass = owner.getSuperclass();
        if (superclass instanceof DeclaredType declared
            && declared.asElement().equals(ownerOfMethod)) {
            seen = declared;
        }
        if (seen == null) {
            for (TypeMirror iface : owner.getInterfaces()) {
                if (iface instanceof DeclaredType declared && declared.asElement().equals(ownerOfMethod)) {
                    seen = declared;
                    break;
                }
            }
        }
        if (seen == null) {
            return null;
        }
        return (ExecutableType) typeUtils.asMemberOf(seen, method);
    }

    private Map<String, List<TypeMirror>> collectMixinInterfaces(DocletEnvironment environment) {
        Map<String, List<TypeMirror>> mixinInterfaces = new HashMap<>();
        for (Element element : environment.getIncludedElements()) {
            if (!(element instanceof TypeElement type)) {
                continue;
            }
            AnnotationMirror mixin = findAnnotation(type, "Mixin");
            if (mixin == null) {
                continue;
            }
            List<? extends TypeMirror> interfaces = type.getInterfaces();
            if (interfaces.isEmpty()) {
                continue;
            }

            Object targetsObj = getAnnotationValue(mixin, "value");
            if (!(targetsObj instanceof List<?> targets) || targets.isEmpty()) {
                continue;
            }

            List<TypeMirror> mixinIfaces = new ArrayList<>();
            for (TypeMirror iface : interfaces) {
                if (iface.getKind() != TypeKind.DECLARED) {
                    continue;
                }
                TypeElement ifaceElement = (TypeElement) ((DeclaredType) iface).asElement();
                if (hasDocletIgnore(ifaceElement)) {
                    continue;
                }
                mixinIfaces.add(iface);
            }
            if (mixinIfaces.isEmpty()) {
                continue;
            }

            for (Object targetObj : targets) {
                if (!(targetObj instanceof AnnotationValue av)) {
                    continue;
                }
                Object value = av.getValue();
                if (!(value instanceof TypeMirror mirror) || mirror.getKind() != TypeKind.DECLARED) {
                    continue;
                }
                TypeElement targetType = (TypeElement) ((DeclaredType) mirror).asElement();
                String qualifiedName = ElementNameUtils.getQualifiedName(targetType);
                mixinInterfaces.computeIfAbsent(qualifiedName, key -> new ArrayList<>())
                    .addAll(mixinIfaces);
            }
        }
        return mixinInterfaces;
    }

    private void addDeclaredType(Element element) {
        DocletDeclareType declare = element.getAnnotation(DocletDeclareType.class);
        if (declare != null) {
            declaredTypes.add(new DeclaredTypeDoc(declare.name(), declare.type()));
        }
    }

    private boolean hasDocletIgnore(Element element) {
        Element current = element;
        while (current != null) {
            if (current.getAnnotation(DocletIgnore.class) != null) {
                return true;
            }
            current = current.getEnclosingElement();
        }
        return false;
    }

    private MemberDoc buildField(Element element) {
        DocComment comment = docCommentParser.parse(element);
        List<String> modifiers = new ArrayList<>();
        for (Modifier modifier : element.getModifiers()) {
            modifiers.add(modifier.toString());
        }

        DocletReplaceReturn replaceReturn = element.getAnnotation(DocletReplaceReturn.class);
        String replaceReturnValue = replaceReturn == null ? null : replaceReturn.value();
        TypeRef typeRef = typeResolver.resolve(element.asType());
        if (isNullable(element) || isNullInitialized(element)) {
            typeRef = typeRef.withNullable(true);
        }

        return new MemberDoc(
            MemberKind.FIELD,
            element.getSimpleName().toString(),
            ElementNameUtils.memberId(element),
            List.of(),
            List.of(),
            typeRef,
            null,
            replaceReturnValue,
            null,
            modifiers,
            element.getAnnotation(DocletReadOnly.class) != null,
            comment
        );
    }

    private MemberDoc buildConstructor(ExecutableElement element) {
        return buildExecutable(element, MemberKind.CONSTRUCTOR);
    }

    private MemberDoc buildMethod(ExecutableElement element) {
        return buildExecutable(element, MemberKind.METHOD);
    }

    private MemberDoc buildMethod(ExecutableElement element, ExecutableType asSeen) {
        return buildExecutable(element, MemberKind.METHOD, asSeen);
    }

    private MemberDoc buildExecutable(ExecutableElement element, MemberKind kind) {
        return buildExecutable(element, kind, null);
    }

    private MemberDoc buildExecutable(ExecutableElement element, MemberKind kind, ExecutableType asSeen) {
        DocComment comment = docCommentParser.parse(element);
        Map<String, String> paramDocs = new HashMap<>();
        for (DocTag tag : comment.tags()) {
            if (tag.kind() == DocTagKind.PARAM && tag.name() != null) {
                paramDocs.put(tag.name(), DocBodyRenderer.toRawText(tag.body()));
            }
        }

        List<ParamDoc> params = new ArrayList<>();
        if (kind == MemberKind.CONSTRUCTOR) {
            ParamDoc enclosing = enclosingInstanceParam(element);
            if (enclosing != null) {
                params.add(enclosing);
            }
        }
        List<? extends VariableElement> rawParams = element.getParameters();
        VariableElement varArgParam = element.isVarArgs() && !rawParams.isEmpty()
            ? rawParams.get(rawParams.size() - 1)
            : null;
        List<? extends TypeMirror> paramTypes = asSeen == null ? null : asSeen.getParameterTypes();
        for (int paramIndex = 0; paramIndex < rawParams.size(); paramIndex++) {
            VariableElement param = rawParams.get(paramIndex);
            boolean isVarArgs = param.equals(varArgParam);
            TypeMirror declaredType = paramTypes == null ? param.asType() : paramTypes.get(paramIndex);
            TypeRef paramType = typeResolver.resolve(declaredType);
            if (isNullable(param)) {
                paramType = paramType.withNullable(true);
            }
            params.add(new ParamDoc(
                param.getSimpleName().toString(),
                paramType,
                isVarArgs,
                paramDocs.getOrDefault(param.getSimpleName().toString(), "")
            ));
        }

        List<TypeRef> typeParams = new ArrayList<>();
        for (TypeParameterElement param : element.getTypeParameters()) {
            typeParams.add(typeResolver.resolve(param.asType()));
        }

        List<String> modifiers = new ArrayList<>();
        for (Modifier modifier : element.getModifiers()) {
            modifiers.add(modifier.toString());
        }

        DocletReplaceParams replaceParams = element.getAnnotation(DocletReplaceParams.class);
        DocletReplaceReturn replaceReturn = element.getAnnotation(DocletReplaceReturn.class);
        DocletReplaceTypeParams replaceTypeParams = element.getAnnotation(DocletReplaceTypeParams.class);

        String replaceParamsValue = replaceParams == null ? null : replaceParams.value();
        String replaceReturnValue = replaceReturn == null ? null : replaceReturn.value();
        String replaceTypeParamsValue = replaceTypeParams == null ? null : replaceTypeParams.value();

        String name = kind == MemberKind.CONSTRUCTOR
            ? ElementNameUtils.getDisplayClassName((TypeElement) element.getEnclosingElement())
            : element.getSimpleName().toString();

        TypeMirror declaredReturn = kind == MemberKind.CONSTRUCTOR
            ? element.getEnclosingElement().asType()
            : asSeen == null ? element.getReturnType() : asSeen.getReturnType();
        TypeRef returnType = typeResolver.resolve(declaredReturn);
        if (kind == MemberKind.METHOD && isNullable(element)) {
            returnType = returnType.withNullable(true);
        }

        return new MemberDoc(
            kind,
            name,
            ElementNameUtils.memberId(element),
            params,
            typeParams,
            returnType,
            replaceParamsValue,
            replaceReturnValue,
            replaceTypeParamsValue,
            modifiers,
            false,
            comment
        );
    }

    /**
     * The constructor of a non-static inner class takes the enclosing instance as its first
     * argument. That argument is not in the declaration, so it has to be added: without it the
     * constructor reads as one that can be called with nothing, and the class reads as one that
     * stands on its own.
     */
    private ParamDoc enclosingInstanceParam(ExecutableElement constructor) {
        if (!(constructor.getEnclosingElement() instanceof TypeElement declaring)) {
            return null;
        }
        if (declaring.getModifiers().contains(Modifier.STATIC)) {
            return null;
        }
        if (!(declaring.getEnclosingElement() instanceof TypeElement outer)) {
            return null;
        }
        TypeRef type = typeResolver.resolve(outer.asType());
        String name = outer.getSimpleName().toString();
        return new ParamDoc(
            Character.toLowerCase(name.charAt(0)) + name.substring(1),
            type,
            false,
            "the " + name + " this is built inside"
        );
    }

    private AnnotationMirror findAnnotation(TypeElement type, String name) {
        for (AnnotationMirror mirror : type.getAnnotationMirrors()) {
            String simpleName = mirror.getAnnotationType().asElement().getSimpleName().toString();
            if (simpleName.equals(name)) {
                return mirror;
            }
        }
        return null;
    }

    private boolean hasEnclosingAnnotation(TypeElement type, String annotationName) {
        TypeElement enclosing = enclosingType(type.getEnclosingElement());
        while (enclosing != null) {
            if (findAnnotation(enclosing, annotationName) != null) {
                return true;
            }
            enclosing = enclosingType(enclosing.getEnclosingElement());
        }
        return false;
    }

    private TypeElement enclosingType(Element element) {
        if (element instanceof TypeElement typeElement) {
            return typeElement;
        }
        return null;
    }

    private Object getAnnotationValue(AnnotationMirror annotation, String key) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> el : annotation.getElementValues().entrySet()) {
            if (el.getKey().getSimpleName().toString().equals(key)) {
                return el.getValue().getValue();
            }
        }
        return null;
    }

    private boolean isNullable(Element element) {
        return element.getAnnotationMirrors().stream()
            .anyMatch(a -> a.getAnnotationType().asElement().getSimpleName().contentEquals("Nullable"));
    }

    /**
     * A field that starts out as {@code null} holds {@code null} until something fills it in, so
     * its type has to admit that. The initializer is the evidence; the declared type alone does
     * not say whether the field can be null.
     */
    private boolean isNullInitialized(Element element) {
        if (!(docTrees instanceof Trees trees)) {
            return false;
        }
        TreePath path = trees.getPath(element);
        if (path == null) {
            return false;
        }
        CompilationUnitTree unit = path.getCompilationUnit();
        long start = trees.getSourcePositions().getStartPosition(unit, path.getLeaf());
        if (start < 0) {
            return false;
        }
        String source;
        try {
            source = unit.getSourceFile().getCharContent(true).toString();
        } catch (IOException e) {
            return false;
        }
        String declaration = fieldDeclaration(source, (int) start);
        return declaration != null && NULL_INITIALIZER.matcher(declaration).find();
    }

    /**
     * Reads the field declaration that starts at {@code start} and ends at its own semicolon.
     * Returns null when the declaration cannot be read.
     */
    private static String fieldDeclaration(String source, int start) {
        StringBuilder out = new StringBuilder();
        int depth = 0;
        int index = start;
        while (index < source.length()) {
            char c = source.charAt(index);
            if (c == '"' || c == '\'') {
                int end = skipLiteral(source, index);
                out.append(source, index, end);
                index = end;
                continue;
            }
            if (c == '/' && index + 1 < source.length() && source.charAt(index + 1) == '/') {
                int lineEnd = source.indexOf('\n', index);
                index = lineEnd < 0 ? source.length() : lineEnd;
                continue;
            }
            if (c == '/' && index + 1 < source.length() && source.charAt(index + 1) == '*') {
                int commentEnd = source.indexOf("*/", index + 2);
                index = commentEnd < 0 ? source.length() : commentEnd + 2;
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            } else if (c == ';' && depth == 0) {
                return out.append(c).toString();
            }
            out.append(c);
            index++;
        }
        return null;
    }

    /** Returns the index just past the string or character literal that starts at {@code start}. */
    private static int skipLiteral(String source, int start) {
        char quote = source.charAt(start);
        int index = start + 1;
        while (index < source.length()) {
            char c = source.charAt(index);
            if (c == '\\') {
                index += 2;
                continue;
            }
            if (c == quote) {
                return index + 1;
            }
            if (c == '\n') {
                return index;
            }
            index++;
        }
        return source.length();
    }
}
