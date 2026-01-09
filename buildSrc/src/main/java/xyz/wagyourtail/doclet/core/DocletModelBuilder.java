package xyz.wagyourtail.doclet.core;

import xyz.wagyourtail.doclet.DocletDeclareType;
import xyz.wagyourtail.doclet.DocletIgnore;
import xyz.wagyourtail.doclet.DocletReplaceParams;
import xyz.wagyourtail.doclet.DocletReplaceReturn;
import xyz.wagyourtail.doclet.DocletReplaceTypeParams;
import xyz.wagyourtail.doclet.core.model.ClassDoc;
import xyz.wagyourtail.doclet.core.model.ClassKind;
import xyz.wagyourtail.doclet.core.model.DeclaredTypeDoc;
import xyz.wagyourtail.doclet.core.model.DocComment;
import xyz.wagyourtail.doclet.core.model.DocTag;
import xyz.wagyourtail.doclet.core.model.DocTagKind;
import xyz.wagyourtail.doclet.core.model.DocletModel;
import xyz.wagyourtail.doclet.core.model.MemberDoc;
import xyz.wagyourtail.doclet.core.model.MemberKind;
import xyz.wagyourtail.doclet.core.model.PackageDoc;
import xyz.wagyourtail.doclet.core.model.ParamDoc;
import xyz.wagyourtail.doclet.core.model.TypeRef;
import xyz.wagyourtail.doclet.core.util.ElementNameUtils;

import com.sun.source.util.DocTrees;
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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeKind;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;

public class DocletModelBuilder {
    private final TypeResolver typeResolver;
    private final DocCommentParser docCommentParser;
    private final List<DeclaredTypeDoc> declaredTypes = new ArrayList<>();
    private DocTrees docTrees;
    private Elements elementUtils;
    private Set<ExecutableElement> objectMethods = Set.of();
    private Set<String> objectMethodNames = Set.of();

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
        initObjectMethods();

        Map<String, List<ClassDoc>> packages = new HashMap<>();
        Map<String, List<TypeMirror>> mixinInterfaces = collectMixinInterfaces(environment);

        for (Element element : environment.getIncludedElements()) {
            if (!(element instanceof TypeElement type)) {
                addDeclaredType(element);
                continue;
            }

            if (type.getAnnotation(DocletIgnore.class) != null) {
                continue;
            }

            addDeclaredType(type);

            String packageName = ElementNameUtils.getPackageName(type);
            String displayName = ElementNameUtils.getDisplayClassName(type);
            String qualifiedName = ElementNameUtils.getQualifiedName(type);

            String group = "Class";
            String alias = null;
            boolean eventCancellable = false;
            String eventFilterer = null;
            AnnotationMirror library = findAnnotation(type, "Library");
            AnnotationMirror event = findAnnotation(type, "Event");
            if (library != null) {
                group = "Library";
                alias = String.valueOf(getAnnotationValue(library, "value"));
            } else if (event != null) {
                group = "Event";
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

            List<TypeRef> extendsTypes = new ArrayList<>();
            if (type.getSuperclass() != null && type.getSuperclass().getKind() != javax.lang.model.type.TypeKind.NONE) {
                String sup = type.getSuperclass().toString();
                if (!"java.lang.Object".equals(sup)) {
                    extendsTypes.add(typeResolver.resolve(type.getSuperclass()));
                }
            }
            List<TypeRef> implementsTypes = new ArrayList<>();
            Set<String> implemented = new HashSet<>();
            for (var iface : type.getInterfaces()) {
                TypeRef resolved = typeResolver.resolve(iface);
                if (implemented.add(resolved.qualifiedName())) {
                    implementsTypes.add(resolved);
                }
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
                        }
                        if (iface.getKind() == TypeKind.DECLARED) {
                            superTypes.add((TypeElement) ((DeclaredType) iface).asElement());
                        }
                    }
                }
            }

            List<String> modifiers = new ArrayList<>();
            for (Modifier modifier : type.getModifiers()) {
                modifiers.add(modifier.toString());
            }

            DocComment classComment = docCommentParser.parse(type);
            List<MemberDoc> members = new ArrayList<>();
            List<ExecutableElement> instanceMethods = new ArrayList<>();

            Set<String> eventSkipNames = new HashSet<>();
            if ("Event".equals(group)) {
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
                if (enclosed.getAnnotation(DocletIgnore.class) != null) {
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
                    if ("Event".equals(group) && eventSkipNames.contains(methodName)) {
                        continue;
                    }
                    if (!"Event".equals(group) && isObjectMethod(method, type)) {
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

            if (!"Event".equals(group)) {
                addSuperMethods(type, superTypes, superMcTypes, instanceMethods, members);
            }

            members.sort(Comparator.comparing(MemberDoc::name, String.CASE_INSENSITIVE_ORDER));

            ClassDoc classDoc = new ClassDoc(
                displayName,
                qualifiedName,
                packageName,
                kind,
                group,
                alias,
                eventCancellable,
                eventFilterer,
                typeParams,
                extendsTypes,
                implementsTypes,
                modifiers,
                classComment,
                members
            );

            packages.computeIfAbsent(packageName, key -> new ArrayList<>()).add(classDoc);
        }

        List<PackageDoc> packageDocs = new ArrayList<>();
        for (Map.Entry<String, List<ClassDoc>> entry : packages.entrySet()) {
            entry.getValue().sort(Comparator.comparing(ClassDoc::qualifiedName, String.CASE_INSENSITIVE_ORDER));
            packageDocs.add(new PackageDoc(entry.getKey(), entry.getValue()));
        }
        packageDocs.sort(Comparator.comparing(PackageDoc::name, String.CASE_INSENSITIVE_ORDER));

        return new DocletModel(packageDocs, declaredTypes);
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
        String pkg = ElementNameUtils.getPackageName(typeElement);
        return pkg != null && pkg.startsWith("net.minecraft.");
    }

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
                    return true;
                }
            }
        }
        return false;
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
                if (element.getAnnotation(DocletIgnore.class) != null) {
                    continue;
                }
                if (isObfuscated(method, superType, superMcTypes)) {
                    continue;
                }
                superMethods.add(method);
            }
        }

        for (ExecutableElement method : superMethods) {
            members.add(buildMethod(method));
        }
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
                if (ifaceElement.getAnnotation(DocletIgnore.class) != null) {
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

    private MemberDoc buildField(Element element) {
        DocComment comment = docCommentParser.parse(element);
        List<String> modifiers = new ArrayList<>();
        for (Modifier modifier : element.getModifiers()) {
            modifiers.add(modifier.toString());
        }

        DocletReplaceReturn replaceReturn = element.getAnnotation(DocletReplaceReturn.class);
        String replaceReturnValue = replaceReturn == null ? null : replaceReturn.value();
        TypeRef typeRef = typeResolver.resolve(element.asType());
        if (isNullable(element)) {
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
            comment
        );
    }

    private MemberDoc buildConstructor(ExecutableElement element) {
        return buildExecutable(element, MemberKind.CONSTRUCTOR);
    }

    private MemberDoc buildMethod(ExecutableElement element) {
        return buildExecutable(element, MemberKind.METHOD);
    }

    private MemberDoc buildExecutable(ExecutableElement element, MemberKind kind) {
        DocComment comment = docCommentParser.parse(element);
        Map<String, String> paramDocs = new HashMap<>();
        for (DocTag tag : comment.tags()) {
            if (tag.kind() == DocTagKind.PARAM && tag.name() != null) {
                paramDocs.put(tag.name(), tag.text());
            }
        }

        List<ParamDoc> params = new ArrayList<>();
        List<? extends VariableElement> rawParams = element.getParameters();
        VariableElement varArgParam = element.isVarArgs() && !rawParams.isEmpty()
            ? rawParams.get(rawParams.size() - 1)
            : null;
        for (VariableElement param : rawParams) {
            boolean isVarArgs = param.equals(varArgParam);
            TypeRef paramType = typeResolver.resolve(param.asType());
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

        TypeRef returnType = typeResolver.resolve(
            kind == MemberKind.CONSTRUCTOR ? element.getEnclosingElement().asType() : element.getReturnType()
        );
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
            comment
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
}
