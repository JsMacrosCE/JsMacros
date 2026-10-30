package com.jsmacrosce.doclet.core.render;

import com.jsmacrosce.doclet.core.BasicTypeResolver;
import com.jsmacrosce.doclet.core.ClassGroup;
import com.jsmacrosce.doclet.core.model.*;

import java.util.List;

public final class TsRendererTest {
    public static void main(String[] args) {
        var comment = new DocComment(List.of(), List.of(
            new DocBodyNode.Link("#keyBind(String, boolean)", null),
            new DocBodyNode.Text(" "),
            new DocBodyNode.Link("FKeyBind#keyBind(String, boolean)", "hold (by name)"),
            new DocBodyNode.Text(" "),
            new DocBodyNode.Link("https://example.org/reference(foo)", null),
            new DocBodyNode.Text(" "),
            new DocBodyNode.Link("java.lang.String", null)
        ), List.of(
            new DocTag(DocTagKind.SEE, null, List.of(
                new DocBodyNode.Link("EventService#unregisterOnStop(boolean, Registrable[])", null))),
            new DocTag(DocTagKind.SEE, null, List.of(new DocBodyNode.Text("\"quoted reference (unchanged)\""))),
            new DocTag(DocTagKind.SEE, null, List.of(new DocBodyNode.Html("<a href=\"https://example.org\">reference</a>")))
        ));
        var members = List.of(
            method("pressKeyBind", "keyBind: Bind", null, comment),
            method("releaseKeyBind", "keyBind: Bind", null, comment),
            method("plain", null, null, comment),
            method("getClass", "name: C): JavaClass<C>;\ngetClass<C extends KnownClass>(name: C", "C extends string", comment)
        );
        var library = new ClassDoc("FKeyBind", "example.FKeyBind", "example", ClassKind.CLASS,
            ClassGroup.Library, "KeyBind", null, false, null, List.of(), List.of(), List.of(),
            List.of("public"), comment, members);
        String output = new TsRenderer(new BasicTypeResolver()).render(
            new DocletModel(List.of(new PackageDoc("example", List.of(library))), List.of()));

        contains(output, "@see {@link EventService.unregisterOnStop}");
        excludes(output, "{@link EventService.unregisterOnStop(boolean");
        contains(output, "{@link keyBind}");
        contains(output, "{@link KeyBind.keyBind hold (by name)}");
        contains(output, "{@link https://example.org/reference(foo)}");
        contains(output, "@see \"quoted reference (unchanged)\"");
        contains(output, "@see [reference](https://example.org)");
        contains(output, "function pressKeyBind(keyBind: Bind): void;");
        contains(output, "function releaseKeyBind(keyBind: Bind): void;");
        contains(output, "pressKeyBind(keyBind: Bind): void;");
        contains(output, "function plain(keyBind: string): void;");
        contains(output, "function getClass<C extends string>(name: C): JavaClass<C>;\n    function getClass<C extends KnownClass>(name: C): void;");
        contains(output, "getClass<C extends string>(name: C): JavaClass<C>;\ngetClass<C extends KnownClass>(name: C): void;");
        replacementTypeParameterRoles();
        eventCancellationInterfaces();
        System.out.println("TsRenderer: regression checks passed");
    }

    private static void eventCancellationInterfaces() {
        var comment = new DocComment(List.of(), List.of(), List.of());
        var dynamic = new ClassDoc("EventDynamic", "example.EventDynamic", "example", ClassKind.CLASS,
            ClassGroup.Event, "Dynamic", null, false, true, null, List.of(), List.of(), List.of(),
            List.of("public"), comment, List.of(), List.of(), false, false, false);
        var plain = new ClassDoc("EventPlain", "example.EventPlain", "example", ClassKind.CLASS,
            ClassGroup.Event, "Plain", null, false, false, null, List.of(), List.of(), List.of(),
            List.of("public"), comment, List.of(), List.of(), false, false, false);
        String output = new TsRenderer(new BasicTypeResolver()).render(
            new DocletModel(List.of(new PackageDoc("example", List.of(dynamic, plain))), List.of()));
        contains(output, "interface Dynamic extends BaseEvent, Cancellable {");
        contains(output, "interface Plain extends BaseEvent {");
    }

    private static void replacementTypeParameterRoles() {
        var roleComment = new DocComment(List.of(), List.of(), List.of(
            new DocTag(DocTagKind.TEMPLATE, "T", List.of(new DocBodyNode.Text("represents the selected Java class")))
        ));
        var blankTemplateComment = new DocComment(List.of(), List.of(), List.of(
            new DocTag(DocTagKind.TEMPLATE, "T", List.of())
        ));
        var orderedRoleComment = new DocComment(List.of(), List.of(new DocBodyNode.Text("Description before tags.")), List.of(
            new DocTag(DocTagKind.PARAM, "name", List.of(new DocBodyNode.Text("class name"))),
            new DocTag(DocTagKind.RETURN, null, List.of(new DocBodyNode.Text("the resolved class"))),
            new DocTag(DocTagKind.SINCE, null, List.of(new DocBodyNode.Text("1.2.3"))),
            new DocTag(DocTagKind.TEMPLATE, "T", List.of(new DocBodyNode.Text("represents the selected Java class")))
        ));
        var generic = new TypeRef(TypeKind.TYPEVAR, "T", "T", List.of(), false, false, null, false);
        var methods = List.of(
            method("simpleReplacement", "name: C", "C extends string", "JavaClass<C>", generic, roleComment),
            method("whitespaceSimpleReplacement", "name: C", " \tC \t", "JavaClass<C>", generic, roleComment),
            method("whitespaceMatchingReplacement", "name: T", " \tT \t extends \t java.lang.String \t", "JavaClass<T>", generic, roleComment),
            method("qualifiedBoundReplacement", "name: C", " C   extends   java.lang.String ", "JavaClass<C>", generic, roleComment),
            method("orderedRoleReplacement", "name: C", "C extends string", "JavaClass<C>", generic, orderedRoleComment),
            method("matchingReplacement", "name: T", "T extends string", "JavaClass<T>", generic, roleComment),
            method("ordinary", null, null, generic, roleComment),
            method("complexReplacement", "name: C", "C extends string, U extends object", "JavaClass<C>", generic, roleComment),
            method("unrecognizedReplacement", "name: C", "C extends keyof string", "JavaClass<C>", generic, roleComment),
            method("blankWithRole", "name: C", " ", "JavaClass<C>", generic, roleComment),
            method("blankNoTags", "name: C", " ", "JavaClass<C>", generic, new DocComment(List.of(), List.of(), List.of())),
            method("blankTemplateBody", "name: C", "C extends string", "JavaClass<C>", generic, blankTemplateComment),
            method("noTemplateTag", null, null, null, generic, new DocComment(List.of(), List.of(), List.of()))
        );
        String output = new TsRenderer(new BasicTypeResolver()).render(new DocletModel(
            List.of(new PackageDoc("example", List.of(new ClassDoc("FTypes", "example.FTypes", "example",
                ClassKind.CLASS, ClassGroup.Library, "Types", null, false, null, List.of(), List.of(), List.of(),
                List.of("public"), new DocComment(List.of(), List.of(), List.of()), methods)))), List.of()));

        String simple = memberScope(output, "simpleReplacement");
        contains(simple, "Java type parameter T: represents the selected Java class");
        excludes(simple, "@template T");
        contains(simple, "function simpleReplacement<C extends string>(name: C): JavaClass<C>;");
        String whitespaceSimple = memberScope(output, "whitespaceSimpleReplacement");
        contains(whitespaceSimple, "Java type parameter T: represents the selected Java class");
        excludes(whitespaceSimple, "@template T");
        contains(whitespaceSimple, "function whitespaceSimpleReplacement< \tC \t>(name: C): JavaClass<C>;");
        String whitespaceMatching = memberScope(output, "whitespaceMatchingReplacement");
        contains(whitespaceMatching, "@template T represents the selected Java class");
        excludes(whitespaceMatching, "Java type parameter T:");
        contains(whitespaceMatching, "function whitespaceMatchingReplacement< \tT \t extends \t java.lang.String \t>(name: T): JavaClass<T>;");
        String qualifiedBound = memberScope(output, "qualifiedBoundReplacement");
        contains(qualifiedBound, "Java type parameter T: represents the selected Java class");
        excludes(qualifiedBound, "@template T");
        contains(qualifiedBound, "function qualifiedBoundReplacement< C   extends   java.lang.String >(name: C): JavaClass<C>;");
        String orderedRole = memberScope(output, "orderedRoleReplacement");
        assertOrdered(orderedRole, " * Description before tags.",
            " * Java type parameter T: represents the selected Java class", " * @param name class name",
            " * @return the resolved class", " * @since 1.2.3",
            "function orderedRoleReplacement<C extends string>(name: C): JavaClass<C>;");
        excludes(orderedRole, "@since 1.2.3 Java type parameter T:");
        excludes(orderedRole, "@param name class name Java type parameter T:");
        assertTemplate(memberScope(output, "matchingReplacement"), "matchingReplacement<T extends string>(name: T): JavaClass<T>;", true);
        assertTemplate(memberScope(output, "ordinary"), "ordinary<T>(name: string): Packages.example.JavaClass;", true);
        assertTemplate(memberScope(output, "complexReplacement"), "complexReplacement<C extends string, U extends object>(name: C): JavaClass<C>;", true);
        assertTemplate(memberScope(output, "unrecognizedReplacement"), "unrecognizedReplacement<C extends keyof string>(name: C): JavaClass<C>;", true);
        String blankWithRole = memberScope(output, "blankWithRole");
        contains(blankWithRole, "@template T represents the selected Java class");
        contains(blankWithRole, "blankWithRole<T>(name: C): JavaClass<C>;");
        assertNoRoleOrTemplate(memberScope(output, "blankNoTags"), "blankNoTags<T>(name: C): JavaClass<C>;");
        assertNoRoleOrTemplate(memberScope(output, "blankTemplateBody"), "blankTemplateBody<C extends string>(name: C): JavaClass<C>;");
        assertNoRoleOrTemplate(memberScope(output, "noTemplateTag"), "noTemplateTag<T>(name: string): Packages.example.JavaClass;");
    }

    private static void assertTemplate(String scope, String declaration, boolean expected) {
        contains(scope, declaration);
        if (expected) contains(scope, "@template T represents the selected Java class");
        else excludes(scope, "@template T");
    }

    private static void assertNoRoleOrTemplate(String scope, String declaration) {
        contains(scope, declaration);
        excludes(scope, "@template");
        excludes(scope, "Java type parameter T:");
    }

    private static void assertOrdered(String scope, String... fragments) {
        int previous = -1;
        for (String fragment : fragments) {
            int current = scope.indexOf(fragment);
            if (current < 0 || current <= previous) {
                throw new AssertionError("Expected ordered fragment: " + fragment + "\nScope:\n" + scope);
            }
            previous = current;
        }
    }

    private static String memberScope(String output, String name) {
        int namespace = output.indexOf("declare namespace Types {");
        int end = output.indexOf("\n}\n", namespace);
        int declaration = output.indexOf("function " + name, namespace);
        if (namespace < 0 || declaration < 0 || declaration >= end) {
            throw new AssertionError("Missing namespace declaration for " + name);
        }
        int docStart = output.lastIndexOf("    /**", declaration);
        int previousMember = output.lastIndexOf(";\n", declaration);
        if (docStart < 0 || docStart < previousMember) docStart = declaration;
        int declarationEnd = output.indexOf(";\n", declaration) + 1;
        if (docStart < namespace || declarationEnd <= declaration) {
            throw new AssertionError("Incomplete namespace member scope for " + name);
        }
        return output.substring(docStart, declarationEnd);
    }

    private static MemberDoc method(String name, String params, String typeParams, DocComment comment) {
        var stringType = new TypeRef(TypeKind.DECLARED, "String", "java.lang.String", List.of(), false, false, null, false);
        var voidType = new TypeRef(TypeKind.VOID, "void", "void", List.of(), false, false, null, false);
        return new MemberDoc(MemberKind.METHOD, name, name, List.of(new ParamDoc("keyBind", stringType, false, "")),
            List.of(), voidType, params, null, typeParams, List.of("public"), false, comment);
    }

    private static MemberDoc method(String name, String params, String typeParams, TypeRef typeParam, DocComment comment) {
        return method(name, params, typeParams, null, typeParam, comment);
    }

    private static MemberDoc method(String name, String params, String typeParams, String returnOverride, TypeRef typeParam, DocComment comment) {
        var stringType = new TypeRef(TypeKind.DECLARED, "String", "java.lang.String", List.of(), false, false, null, false);
        var returnType = new TypeRef(TypeKind.DECLARED, "JavaClass", "example.JavaClass", List.of(), false, false, null, false);
        return new MemberDoc(MemberKind.METHOD, name, name, List.of(new ParamDoc("name", stringType, false, "")),
            List.of(typeParam), returnType, params, returnOverride, typeParams, List.of("public"), false, comment);
    }

    private static void contains(String output, String expected) {
        if (!output.contains(expected)) {
            throw new AssertionError("Missing: " + expected + "\nOutput:\n" + output);
        }
    }

    private static void excludes(String output, String unexpected) {
        if (output.contains(unexpected)) {
            throw new AssertionError("Unexpected: " + unexpected);
        }
    }
}
