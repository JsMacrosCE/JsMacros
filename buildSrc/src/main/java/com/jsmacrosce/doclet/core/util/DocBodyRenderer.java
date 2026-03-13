package com.jsmacrosce.doclet.core.util;

import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.core.model.DocBodyNode;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Utility class for rendering a {@code List<DocBodyNode>} to a target-format string.
 *
 * <p>Each renderer calls the appropriate static method and supplies a
 * {@code linkResolver} lambda that converts a {@link DocBodyNode.Link} node into
 * its rendered form (e.g. a Markdown hyperlink, a JSDoc {@code {@link}} token,
 * or plain text).
 *
 * <p>Shared signature-conversion helpers ({@link #looksLikeSignature},
 * {@link #convertSignature}, {@link #mapSimpleLinkSignature}) are provided here
 * so they are not duplicated across renderers.
 */
@DocletIgnore
public final class DocBodyRenderer {

    // Java boxed number types that map to their primitive names in display text.
    public static final Map<String, String> JAVA_NUMBER_TYPES = Map.of(
        "java.lang.Integer", "int",
        "java.lang.Float", "float",
        "java.lang.Long", "long",
        "java.lang.Short", "short",
        "java.lang.Character", "char",
        "java.lang.Byte", "byte",
        "java.lang.Double", "double",
        "java.lang.Number", "number"
    );

    private static final Pattern HTML_LINK =
        Pattern.compile("<a (?:[\\n.])*?href=\"([^\"]*)\"(?:[\\n.])*?>(.*?)</a>", Pattern.DOTALL);

    private DocBodyRenderer() {}

    // -------------------------------------------------------------------------
    // Public rendering entry points
    // -------------------------------------------------------------------------

    /**
     * Renders nodes to a Markdown string.
     *
     * <p>HTML structural tags ({@code <p>}, {@code <br>}, {@code <pre>}) are
     * converted to Markdown equivalents. Remaining HTML angle brackets are
     * escaped to {@code &lt;}/{@code &gt;} so they render as literal text
     * inside HTML blocks embedded in Markdown. {@code <a href>} tags in
     * {@link DocBodyNode.Html} nodes are converted to Markdown links.
     *
     * @param nodes        the body node list to render
     * @param linkResolver called for each {@link DocBodyNode.Link} node; should
     *                     return a Markdown link string (e.g. {@code "[label](url)"})
     *                     or a plain fallback label when the link cannot be resolved
     */
    public static String toMarkdown(List<DocBodyNode> nodes, Function<DocBodyNode.Link, String> linkResolver) {
        if (nodes == null || nodes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (DocBodyNode node : nodes) {
            switch (node) {
                case DocBodyNode.Text(var value) -> sb.append(value);
                case DocBodyNode.Code(var value) -> sb.append("`").append(value).append("`");
                case DocBodyNode.Link link -> sb.append(linkResolver.apply(link));
                case DocBodyNode.Html(var raw) -> sb.append(processHtmlForMarkdown(raw));
            }
        }
        String result = sb.toString().trim();
        if (result.isEmpty()) {
            return "";
        }

        // Post-process the accumulated string:
        // 1. Convert <a href="…">…</a> that survived Html nodes to Markdown links.
        result = HTML_LINK.matcher(result).replaceAll("[$2]($1)");
        // 2. Escape remaining HTML angle brackets so they render as literal text
        //    when embedded inside HTML blocks in the VitePress Markdown output.
        result = result.replace("<", "&lt;").replace(">", "&gt;");
        // 3. Normalise line endings after paragraph tags.
        result = result.replaceAll("(?<=[.,:;>]) ?\n", "  \n");

        return result.trim();
    }

    /**
     * Renders nodes to a plain-text string suitable for JSDoc comments or
     * Python docstrings.
     *
     * <p>HTML structural tags are stripped or converted to whitespace. Angle
     * brackets in remaining HTML are left as-is (JSDoc and Python docstrings
     * are not HTML contexts). {@code <a href>} tags are converted to
     * {@code [text](url)} Markdown notation (common in JSDoc-flavored comments).
     *
     * @param nodes        the body node list to render
     * @param linkResolver called for each {@link DocBodyNode.Link} node; should
     *                     return the plain-text or inline representation of the link
     */
    public static String toPlainText(List<DocBodyNode> nodes, Function<DocBodyNode.Link, String> linkResolver) {
        if (nodes == null || nodes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (DocBodyNode node : nodes) {
            switch (node) {
                case DocBodyNode.Text(var value) -> sb.append(value);
                case DocBodyNode.Code(var value) -> sb.append(value);
                case DocBodyNode.Link link -> sb.append(linkResolver.apply(link));
                case DocBodyNode.Html(var raw) -> sb.append(processHtmlForPlainText(raw));
            }
        }

        String result = sb.toString().trim();
        if (result.isEmpty()) {
            return "";
        }

        // Convert surviving <a href> links to Markdown notation (JSDoc-friendly).
        result = HTML_LINK.matcher(result).replaceAll("[$2]($1)");
        return result.trim();
    }

    /**
     * Renders nodes to an inline HTML string, suitable for embedding inside HTML
     * elements such as {@code <p>}, {@code <li>}, or {@code <td>}.
     *
     * <p>Links are rendered as {@code <a href="url">label</a>} elements.
     * Code spans are rendered as {@code <code>value</code>}.
     * {@link DocBodyNode.Html} nodes are passed through as-is (their HTML is already
     * valid in an HTML context).
     * Angle brackets in plain text are HTML-escaped so they don't break the
     * surrounding HTML structure.
     *
     * <p>Use this method whenever the output will be placed inside a raw HTML block
     * rather than a Markdown paragraph — Markdown link syntax ({@code [text](url)})
     * does not render inside raw HTML in VitePress/Vue.
     *
     * @param nodes        the body node list to render
     * @param linkResolver called for each {@link DocBodyNode.Link} node; should
     *                     return an {@code <a href="url">label</a>} string, or a
     *                     plain text/code fallback when the link cannot be resolved
     */
    public static String toHtml(List<DocBodyNode> nodes, Function<DocBodyNode.Link, String> linkResolver) {
        if (nodes == null || nodes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (DocBodyNode node : nodes) {
            switch (node) {
                case DocBodyNode.Text(var value) ->
                    // Escape angle brackets in plain text so they don't break surrounding HTML.
                    sb.append(value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
                case DocBodyNode.Code(var value) ->
                    sb.append("<code>").append(value.replace("<", "&lt;").replace(">", "&gt;")).append("</code>");
                case DocBodyNode.Link link -> sb.append(linkResolver.apply(link));
                case DocBodyNode.Html(var raw) -> sb.append(processHtmlForInline(raw));
            }
        }
        return sb.toString().trim();
    }

    /**
     * Renders nodes to a raw text string with no formatting applied.
     * Links are rendered as their label (or signature if no label), code
     * spans are rendered as their plain value, and HTML nodes are included
     * verbatim. Useful for plain-string fields that don't need rendering
     * (e.g. {@code ParamDoc.description} populated by the model builder).
     *
     * @param nodes the body node list to render
     */
    public static String toRawText(List<DocBodyNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (DocBodyNode node : nodes) {
            switch (node) {
                case DocBodyNode.Text(var value) -> sb.append(value);
                case DocBodyNode.Code(var value) -> sb.append(value);
                case DocBodyNode.Link(var sig, var label) -> sb.append(label != null ? label : sig);
                case DocBodyNode.Html(var raw) -> sb.append(raw);
            }
        }
        return sb.toString().trim();
    }

    // -------------------------------------------------------------------------
    // Shared signature-conversion helpers (used by renderer link-resolver lambdas)
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when {@code text} looks like a bare javadoc
     * reference signature that should be formatted as a code span rather than
     * plain prose (e.g. {@code "#someMethod"} or a fully-qualified class name).
     */
    public static boolean looksLikeSignature(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        if (text.startsWith("#")) {
            return true;
        }
        return text.matches("^com\\.jsmacrosce\\.[^#]+\\w$")
            || text.matches("^\\w+\\.(?:\\w+\\.)+[\\w$_]+$");
    }

    /**
     * Converts a bare javadoc reference signature to a display-friendly string.
     *
     * <ul>
     *   <li>Fully-qualified JsMacros class names are stripped to their simple name
     *       (e.g. {@code com.jsmacrosce.foo.Bar} → {@code Bar}).</li>
     *   <li>Other fully-qualified names are prefixed with {@code Packages.}.</li>
     *   <li>{@code #member} signatures have the leading {@code #} removed.</li>
     *   <li>{@code FooLib#method} patterns are converted to {@code FooLib.method}.</li>
     * </ul>
     */
    public static String convertSignature(String sig) {
        if (sig == null) {
            return "";
        }
        if (sig.matches("^com\\.jsmacrosce\\.[^#]+\\w$")) {
            return sig.replaceFirst("^.+\\.(?=[^.]+$)", "");
        }
        if (sig.matches("^\\w+\\.(?:\\w+\\.)+[\\w$_]+$")) {
            return "Packages." + sig;
        }
        if (sig.startsWith("#")) {
            return sig.substring(1);
        }
        return sig
            .replaceFirst("^(?:com\\.jsmacrosce\\.jsmacros\\.(?:client\\.api|core)\\.library\\.impl\\.)?F([A-Z]\\w+)#", "$1.")
            .replaceFirst("#", ".");
    }

    /**
     * Maps a javadoc link signature that refers to a simple Java type (boxed
     * number, {@code String}, {@code Boolean}) to its display alias.
     * Returns {@code null} when the signature is not a recognised simple type.
     */
    public static String mapSimpleLinkSignature(String sig) {
        if (sig == null) {
            return null;
        }
        if (JAVA_NUMBER_TYPES.containsKey(sig)) {
            return JAVA_NUMBER_TYPES.get(sig);
        }
        return switch (sig) {
            case "java.lang.String" -> "string";
            case "java.lang.Boolean" -> "boolean";
            default -> null;
        };
    }

    // -------------------------------------------------------------------------
    // Private HTML processing helpers
    // -------------------------------------------------------------------------

    /**
     * Converts HTML structural markup inside a raw Html node to Markdown-friendly
     * equivalents, preserving {@code <a href>} for the outer post-processing step.
     */
    private static String processHtmlForMarkdown(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw;
        // <br> / <br/> → newline
        s = s.replaceAll("<br ?/?>", "\n");
        // <p> continuation → newline (strip the tag itself)
        s = s.replaceAll("\n ?<p>", "\n").replaceAll("^<p>", "");
        // <pre>...</pre> → fenced code block
        s = s.replaceAll("</?pre>", "```");
        // Leave <a href> intact — the outer step converts it to a Markdown link.
        return s;
    }

    /**
     * Passes HTML from a raw Html node through for inline HTML contexts.
     * Structural block tags ({@code <p>}, {@code <br>}) are converted to
     * spacing equivalents; everything else (including {@code <a href>}) is
     * preserved as-is since it is already valid HTML.
     */
    private static String processHtmlForInline(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw;
        s = s.replaceAll("<br ?/?>", " ");
        // <p> continuation → space (keep content flowing inline)
        s = s.replaceAll("\n ?<p>", " ").replaceAll("^<p>", "");
        // <pre> in inline HTML context: just strip the delimiters
        s = s.replaceAll("</?pre>", "");
        return s;
    }

    /**
     * Strips or converts HTML structural markup inside a raw Html node to
     * plain-text equivalents. {@code <a href>} tags are left for the outer
     * post-processing step.
     */
    private static String processHtmlForPlainText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw;
        s = s.replaceAll("<br ?/?>", "\n");
        s = s.replaceAll("\n ?<p>", "\n").replaceAll("^<p>", "");
        // Strip <pre> delimiters in plain text context.
        s = s.replaceAll("</?pre>", "");
        // Leave <a href> for the outer Markdown link conversion step.
        return s;
    }
}
