package com.jsmacrosce.doclet.core.util;

import com.jsmacrosce.doclet.core.model.DocBodyNode;

import java.util.List;

/** Dependency-free regression checks, run by {@code testDocBodyRenderer}. */
public final class DocBodyRendererTest {
    public static void main(String[] args) {
        var javascript = example("<pre>", "const draw = Hud.createDraw2D();");
        expect("default JavaScript fence", "```js\nconst draw = Hud.createDraw2D();  \n```",
            DocBodyRenderer.toMarkdown(javascript, link -> link.signature()));
        expect("default JavaScript HTML", "<pre><code>const draw = Hud.createDraw2D();</code></pre>",
            DocBodyRenderer.toHtml(javascript, link -> link.signature()));

        var typescript = example("<pre class=\"language-typescript\">", "const block: BlockHelper = value;");
        expect("TypeScript fence", "```ts\nconst block: BlockHelper = value;  \n```",
            DocBodyRenderer.toMarkdown(typescript, link -> link.signature()));
        expect("TypeScript HTML and trimmed code lines",
            "<pre><code class=\"language-typescript\">const block: BlockHelper = value;</code></pre>",
            DocBodyRenderer.toHtml(typescript, link -> link.signature()));
        expect("plain text strips language markup", "const block: BlockHelper = value;",
            DocBodyRenderer.toPlainText(typescript, link -> link.signature()));

        var alias = example("<pre class='example language-ts'>", "const block = value as BlockHelper;");
        expect("short language alias and single-quoted classes",
            "<pre><code class=\"language-typescript\">const block = value as BlockHelper;</code></pre>",
            DocBodyRenderer.toHtml(alias, link -> link.signature()));
        var uppercase = example("<PRE CLASS=\"language-TYPESCRIPT\">", "const typed: number = 1;");
        expect("case-insensitive HTML tags and language", "```ts\nconst typed: number = 1;  \n```",
            DocBodyRenderer.toMarkdown(uppercase, link -> link.signature()));

        var comparison = example("<pre class=\"language-typescript\">", "if (left < right && ready) {\n  run();\n}");
        expect("plain text preserves operators and indentation", "if (left < right && ready) {\n  run();\n}",
            DocBodyRenderer.toPlainText(comparison, link -> link.signature()));
        expect("HTML escapes operators without stripping them",
            "<pre><code class=\"language-typescript\">if (left &lt; right &amp;&amp; ready) {\n  run();\n}</code></pre>",
            DocBodyRenderer.toHtml(comparison, link -> link.signature()));

        var literal = List.<DocBodyNode>of(new DocBodyNode.Code("<pre class=\"language-typescript\">"));
        expect("literal code is not treated as example markup",
            "`<pre class=\"language-typescript\">`",
            DocBodyRenderer.toMarkdown(literal, link -> link.signature()));
        var attributedLink = List.<DocBodyNode>of(new DocBodyNode.Html("<a target=\"_blank\" href=\"https://example.org\" title=\"Source\">"),
            new DocBodyNode.Text("Original's $source"), new DocBodyNode.Html("</a>"));
        expect("anchor attributes become Markdown links", "[Original's $source](https://example.org)",
            DocBodyRenderer.toMarkdown(attributedLink, link -> link.signature()));
        expect("anchor attributes become readable plain-text links", "[Original's $source](https://example.org)",
            DocBodyRenderer.toPlainText(attributedLink, link -> link.signature()));
        var singleQuoteLink = List.<DocBodyNode>of(new DocBodyNode.Html("<A HREF='https://example.org' TARGET='_blank'>Source</A>"));
        expect("single quotes and case-insensitive anchor attributes", "[Source](https://example.org)",
            DocBodyRenderer.toMarkdown(singleQuoteLink, link -> link.signature()));
        String codeLink = "<a target='_blank' href='https://example.org'>$code</a>";
        var literalAnchor = List.<DocBodyNode>of(new DocBodyNode.Text("  Literal: "), new DocBodyNode.Code(codeLink));
        expect("literal anchors remain code in Markdown", "Literal: `" + codeLink + "`",
            DocBodyRenderer.toMarkdown(literalAnchor, link -> link.signature()));
        expect("literal anchors remain code in plain text", "Literal: " + codeLink,
            DocBodyRenderer.toPlainText(literalAnchor, link -> link.signature()));
        System.out.println("DocBodyRenderer: 15 regression checks passed");
    }

    private static List<DocBodyNode> example(String opening, String code) {
        return List.of(new DocBodyNode.Html(opening), new DocBodyNode.Text("\n" + code + "\n"),
            new DocBodyNode.Html("</pre>"));
    }

    private static void expect(String name, String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(name + "\nExpected: " + expected + "\nActual: " + actual);
        }
    }
}
