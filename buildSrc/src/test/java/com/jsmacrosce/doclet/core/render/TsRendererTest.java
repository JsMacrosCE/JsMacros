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
        System.out.println("TsRenderer: 13 regression checks passed");
    }

    private static MemberDoc method(String name, String params, String typeParams, DocComment comment) {
        var stringType = new TypeRef(TypeKind.DECLARED, "String", "java.lang.String", List.of(), false, false, null, false);
        var voidType = new TypeRef(TypeKind.VOID, "void", "void", List.of(), false, false, null, false);
        return new MemberDoc(MemberKind.METHOD, name, name, List.of(new ParamDoc("keyBind", stringType, false, "")),
            List.of(), voidType, params, null, typeParams, List.of("public"), false, comment);
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
