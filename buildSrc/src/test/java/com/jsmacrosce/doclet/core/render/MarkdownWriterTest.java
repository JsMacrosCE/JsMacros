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
        var constructor = new MemberDoc(MemberKind.CONSTRUCTOR, "Result", "Result_constructor", List.of(), List.of(),
            null, null, null, null, List.of("public"), empty);
        var classes = List.of(
            clz("FExample", ClassGroup.Library, "Example", null, empty, List.of()),
            clz("FExample.Result", ClassGroup.Library, null, null, links, List.of(constructor)),
            clz("FExample.Result.Details", ClassGroup.Library, null, null, empty, List.of()),
            clz("OptionsHelper", ClassGroup.Class, null, "Helpers", empty, List.of()),
            clz("OptionsHelper.Controls", ClassGroup.Class, null, null, empty, List.of()),
            clz("Other", ClassGroup.Class, null, null, empty, List.of())
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
        String result = Files.readString(out.resolve("libraries/example/FExample.Result.md"));
        contains(result, "example.FExample.Result");
        contains(result, "Nested type declared in [Example](FExample); not a separate global library.");
        contains(result, "## Constructors");
        contains(result, "[Original's $source](https://example.org/source)");
        excludes(result, "Accessible in scripts via the global");
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
}
