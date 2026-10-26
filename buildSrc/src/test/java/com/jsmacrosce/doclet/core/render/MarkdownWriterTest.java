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
        // A member that is only marked with @Deprecated has no @deprecated text.
        var deprecatedWithoutText = new DocComment(List.of(), List.of(), List.of(
            new DocTag(DocTagKind.DEPRECATED, null, List.of())));
        var deprecatedWithText = new DocComment(List.of(), List.of(), List.of(
            new DocTag(DocTagKind.DEPRECATED, null, List.of(new DocBodyNode.Text("use other() instead")))));
        var constructor = new MemberDoc(MemberKind.CONSTRUCTOR, "Result", "Result_constructor", List.of(), List.of(),
            null, null, null, null, List.of("public"), false, memberSince);
        var stringType = new TypeRef(TypeKind.DECLARED, "String", "java.lang.String", List.of(), false, false, null, false);
        var stringArray = new TypeRef(TypeKind.ARRAY, "String", "java.lang.String", List.of(stringType), true, false, null, false);
        var variadic = new MemberDoc(MemberKind.METHOD, "select", "select-String[]-",
            List.of(new ParamDoc("types", stringArray, true, "")), List.of(), stringType,
            null, null, null, List.of("public"), false, memberSince);
        var deprecatedOnly = new MemberDoc(MemberKind.METHOD, "legacy", "legacy-",
            List.of(), List.of(), stringType,
            null, null, null, List.of("public"), false, deprecatedWithoutText);
        var deprecatedExplained = new MemberDoc(MemberKind.METHOD, "older", "older-",
            List.of(), List.of(), stringType,
            null, null, null, List.of("public"), false, deprecatedWithText);
        var summaryComment = new DocComment(
            List.of(new DocBodyNode.Text("A short helper.")),
            List.of(new DocBodyNode.Text("A short helper.")),
            List.of());
        var classes = List.of(
            clz("FExample", ClassGroup.Library, "Example", null, empty, List.of(variadic)),
            clz("FExample.Result", ClassGroup.Library, null, null,
                new DocComment(List.of(), links.body(), classSince.tags()), List.of(constructor)),
            clz("FExample.Result.Details", ClassGroup.Library, null, null, empty, List.of()),
            clz("OptionsHelper", ClassGroup.Class, null, "Helpers", empty, List.of()),
            clz("OptionsHelper.Controls", ClassGroup.Class, null, null, empty, List.of()),
            clz("SummaryHelper", ClassGroup.Class, null, "Helpers", summaryComment, List.of()),
            clz("FiltererThing", ClassGroup.Class, null, "Event Filterers", empty, List.of()),
            eventClz("EventThing", "Thing", "FiltererThing", true, empty),
            clz("Other", ClassGroup.Class, null, null, empty, List.of()),
            clz("Versioned", ClassGroup.Class, null, null, classSince, List.of()),
            clz("Retired", ClassGroup.Class, null, null, empty, List.of(deprecatedOnly, deprecatedExplained))
        );
        var exampleTypes = new ScriptNamespaceDoc(
            "ExampleTypes",
            new DocComment(
                List.of(new DocBodyNode.Text("Names for examples.")),
                List.of(new DocBodyNode.Text("Names for examples.")),
                List.of()),
            List.of(new ScriptNamespaceDoc.Export(
                "Helper",
                new TypeRef(TypeKind.DECLARED, "SummaryHelper", "example.SummaryHelper", List.of(), false, false, null, false),
                List.of(),
                false))
        );
        new MarkdownWriter().write(new DocletModel(List.of(new PackageDoc("example", classes)), List.of(),
                List.of(), java.util.Set.of(), List.of(exampleTypes)),
            out.toFile(), "fixture", "26.1.2");
        String libraries = Files.readString(out.resolve("libraries.md"));
        contains(libraries, "- [Example](./libraries/example/FExample.md)\n  - [Result](./libraries/example/FExample.Result.md)\n    - [Details]");
        excludes(libraries, "Uncategorized");
        excludes(libraries, "example.FExample");
        String index = Files.readString(out.resolve("classes.md"));
        contains(index, "- [OptionsHelper](./classes/example/OptionsHelper.md)\n  - [Controls]");
        contains(index, "- [SummaryHelper](./classes/example/SummaryHelper.md) — A short helper.");
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
        // A member that only carries the @Deprecated annotation is marked by the badge alone.
        // A "Deprecated:" label with nothing after it would be noise.
        String retired = Files.readString(out.resolve("classes/example/Retired.md"));
        contains(retired, "<Badge type=\"danger\" text=\"deprecated\" />");
        contains(retired, "<strong>Deprecated:</strong> use other() instead");
        if (count(retired, "<strong>Deprecated:</strong>") != 1) {
            throw new AssertionError("Deprecated label rendered more than once: " + retired);
        }
        String legacyLine = retired.lines().filter(l -> l.contains("syntax-token-method\">legacy<")).findFirst()
            .orElseThrow(() -> new AssertionError("No legacy member rendered: " + retired));
        excludes(legacyLine, "Deprecated:");
        String sidebar = Files.readString(out.resolve("sidebar-data.json"));
        contains(sidebar, "\"name\": \"Example\"");
        contains(sidebar, "\"name\": \"Result\"");
        contains(sidebar, "\"text\": \"Details\"");
        contains(sidebar, "\"text\": \"Controls\"");
        contains(sidebar, "\"types\"");
        contains(sidebar, "\"text\": \"ExampleTypes\"");
        excludes(sidebar, "\"text\": \"OptionsHelper.Controls\"");
        contains(Files.readString(out.resolve("index.md")), "[Libraries](./libraries.md) (1)");
        String typeIndex = Files.readString(out.resolve("types/index.md"));
        contains(typeIndex, "[ExampleTypes](./ExampleTypes.md)");
        contains(typeIndex, "Names for examples.");
        String namespacePage = Files.readString(out.resolve("types/ExampleTypes.md"));
        contains(namespacePage, "| `ExampleTypes.Helper` | [SummaryHelper]");
        contains(namespacePage, "| A short helper. |");
        String event = Files.readString(out.resolve("events/example/EventThing.md"));
        contains(event, "**Cancellable:** yes");
        contains(event, "**Filterer:** [FiltererThing]");
        contains(event, "JsMacros.createEventFilterer(\"Thing\")");
        System.out.println("MarkdownWriter: nested indexes, labels, summaries, type namespaces, event metadata, constructors, global notices and anchor rendering passed");
    }

    private static ClassDoc clz(String name, ClassGroup group, String alias, String category,
                                DocComment comment, List<MemberDoc> members) {
        return new ClassDoc(name, "example." + name, "example", ClassKind.CLASS, group, alias, category,
            false, null, List.of(), List.of(), List.of(), List.of("public"), comment, members);
    }

    private static ClassDoc eventClz(String name, String alias, String filterer, boolean cancellable,
                                     DocComment comment) {
        return new ClassDoc(name, "example." + name, "example", ClassKind.CLASS, ClassGroup.Event, alias,
            "Events and Event Handling", cancellable, filterer, List.of(), List.of(), List.of(),
            List.of("public"), comment, List.of(), List.of(), false, false, false);
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
