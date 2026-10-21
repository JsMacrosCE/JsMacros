package com.jsmacrosce.doclet.core.render;

import com.jsmacrosce.doclet.core.ClassGroup;
import com.jsmacrosce.doclet.core.model.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Presentation checks: retain the model and URLs, but distinguish globals from supporting types. */
public final class MarkdownWriterTest {
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        var empty = new DocComment(List.of(), List.of(), List.of());
        var links = new DocComment(List.of(), List.of(
            new DocBodyNode.Text("Based on "),
            new DocBodyNode.Html("<a target=\"_blank\" href=\"https://example.org/source\">"),
            new DocBodyNode.Text("Original's $source"),
            new DocBodyNode.Html("</a>")), List.of());
        var memberSince = new DocComment(List.of(), List.of(), List.of(
            new DocTag(DocTagKind.SINCE, null, List.of(new DocBodyNode.Text("member-release")))));
        var classSince = new DocComment(List.of(), List.of(), List.of(
            new DocTag(DocTagKind.SINCE, null, List.of(new DocBodyNode.Text("1.9.0")))));
        var constructor = new MemberDoc(MemberKind.CONSTRUCTOR, "Result", "Result_constructor", List.of(), List.of(),
            null, null, null, null, List.of("public"), false, memberSince);
        var stringType = new TypeRef(TypeKind.DECLARED, "String", "java.lang.String", List.of(), false, false, null, false);
        var stringArray = new TypeRef(TypeKind.ARRAY, "String", "java.lang.String", List.of(stringType), true, false, null, false);
        var variadic = new MemberDoc(MemberKind.METHOD, "select", "select-String[]-",
            List.of(new ParamDoc("types", stringArray, true, "")), List.of(), stringType,
            null, null, null, List.of("public"), false, memberSince);
        var classes = List.of(
            clz("FExample", ClassGroup.Library, "Example", null, empty, List.of(variadic)),
            clz("FExample.Result", ClassGroup.Library, null, null,
                new DocComment(List.of(), links.body(), classSince.tags()), List.of(constructor)),
            clz("FExample.Result.Details", ClassGroup.Library, null, null, empty, List.of()),
            clz("OptionsHelper", ClassGroup.Class, null, "Helpers", empty, List.of()),
            clz("OptionsHelper.Controls", ClassGroup.Class, null, null, empty, List.of()),
            clz("Other", ClassGroup.Class, null, null, empty, List.of()),
            clz("Versioned", ClassGroup.Class, null, null, classSince, List.of())
        );
        new MarkdownWriter().write(new DocletModel(List.of(new PackageDoc("example", classes)), List.of()),
            out.toFile(), "fixture", "26.1.2");
        String libraries = Files.readString(out.resolve("libraries.md"));
        contains(libraries, "- [Example](./libraries/example/FExample.md)\n  - [Result](./libraries/example/FExample.Result.md)\n    - [Details]");
        excludes(libraries, "Uncategorized");
        excludes(libraries, "example.FExample");
        String index = Files.readString(out.resolve("classes.md"));
        contains(index, "- [OptionsHelper](./classes/example/OptionsHelper.md)\n  - [Controls]");
        contains(index, "### Other classes");
        excludes(index, "Uncategorized");
        excludes(index, "example.OptionsHelper");
        String library = Files.readString(out.resolve("libraries/example/FExample.md"));
        contains(library, "Accessible in scripts via the global `Example` variable.");
        contains(library, "## Nested types");
        contains(library, "[Result](FExample.Result)");
        // A parameter that takes a number of arguments is written with an ellipsis, so that
        // the signature reads as the call that it is.
        contains(plain(library), "select(String... types): String");
        excludes(plain(library), "select(String[] types)");
        String result = Files.readString(out.resolve("libraries/example/FExample.Result.md"));
        contains(result, "example.FExample.Result");
        contains(result, "Nested type declared in [Example](FExample); not a separate global library.");
        contains(result, "## Constructors");
        contains(result, "**Since:** 1.9.0");
        contains(result, "Since:</strong> member-release");
        if (count(result, "1.9.0") != 1) throw new AssertionError("Class @since rendered more than once: " + result);
        contains(result, "[Original's $source](https://example.org/source)");
        excludes(result, "Accessible in scripts via the global");
        String unversioned = Files.readString(out.resolve("classes/example/Other.md"));
        excludes(unversioned, "Since:");
        excludes(unversioned, "**Since:**");
        String nested = Files.readString(out.resolve("classes/example/Versioned.md"));
        contains(nested, "**Since:** 1.9.0");
        excludes(result, "&lt;a");
        String sidebar = Files.readString(out.resolve("sidebar-data.json"));
        contains(sidebar, "\"name\": \"Example\"");
        contains(sidebar, "\"name\": \"Result\"");
        contains(sidebar, "\"text\": \"Details\"");
        contains(sidebar, "\"text\": \"Controls\"");
        excludes(sidebar, "\"text\": \"OptionsHelper.Controls\"");
        contains(Files.readString(out.resolve("index.md")), "[Libraries](./libraries.md) (1)");
        System.out.println("MarkdownWriter: nested indexes, labels, constructors, global notices and anchor rendering passed");
    }

    private static ClassDoc clz(String name, ClassGroup group, String alias, String category,
                                DocComment comment, List<MemberDoc> members) {
        return new ClassDoc(name, "example." + name, "example", ClassKind.CLASS, group, alias, category,
            false, null, List.of(), List.of(), List.of(), List.of("public"), comment, members);
    }

    private static void contains(String text, String expected) {
        if (!text.contains(expected)) throw new AssertionError("Missing: " + expected + "\nOutput:\n" + text);
    }

    private static void excludes(String text, String unexpected) {
        if (text.contains(unexpected)) throw new AssertionError("Unexpected: " + unexpected);
    }

    private static int count(String text, String value) {
        return (text.length() - text.replace(value, "").length()) / value.length();
    }

    /** The text of a document without the markup that carries the presentation. */
    private static String plain(String text) {
        return text.replaceAll("<[^>]*>", "")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&");
    }
}
