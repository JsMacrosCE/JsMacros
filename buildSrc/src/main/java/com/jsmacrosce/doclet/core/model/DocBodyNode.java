package com.jsmacrosce.doclet.core.model;

/**
 * A single node in a structured javadoc body.
 *
 * <p>The javadoc AST is preserved through the model boundary as a
 * {@code List<DocBodyNode>} rather than being eagerly collapsed to a raw string.
 * Each renderer then pattern-matches on node types to produce its own
 * format-specific output without regex re-parsing.
 *
 * <p>Permitted subtypes:
 * <ul>
 *   <li>{@link Text}  — a plain-text run</li>
 *   <li>{@link Code}  — an inline code span ({@code {@code ...}} or {@code {@literal ...}})</li>
 *   <li>{@link Link}  — a {@code {@link}} or {@code {@linkplain}} reference</li>
 *   <li>{@link Html}  — raw HTML (block or inline) that doesn't fit the above</li>
 * </ul>
 */
public sealed interface DocBodyNode
        permits DocBodyNode.Text, DocBodyNode.Code, DocBodyNode.Link, DocBodyNode.Html {

    /**
     * A plain-text run originating from a {@code DocTree.Kind.TEXT} node.
     *
     * @param value the raw text content (may contain newlines, spaces, etc.)
     */
    record Text(String value) implements DocBodyNode {}

    /**
     * An inline code span originating from a {@code {@code}} or {@code {@literal}} tag.
     * The {@code value} is the unescaped content; renderers wrap it as appropriate
     * (e.g. {@code `value`} in Markdown, {@code <code>value</code>} in HTML).
     *
     * @param value the literal content of the code span, unescaped
     */
    record Code(String value) implements DocBodyNode {}

    /**
     * A {@code {@link}} or {@code {@linkplain}} reference.
     *
     * <p>{@code signature} is the raw javadoc reference string exactly as written
     * (e.g. {@code "FInput#keyDown(int, int)"} or {@code "java.util.List"}).
     * Each renderer resolves this string in its own way.
     *
     * <p>{@code label} is the explicit display label written after the signature, or
     * {@code null} when no label was provided (renderers should then derive a label
     * from the signature itself).
     *
     * @param signature the raw javadoc reference (class, class#member, or #member)
     * @param label     explicit display label, or {@code null} if none
     */
    record Link(String signature, String label) implements DocBodyNode {}

    /**
     * Raw HTML content that does not fit the other node types.
     *
     * <p>This covers HTML block tags ({@code <p>}, {@code <br>}, {@code <pre>},
     * {@code <ul>}, {@code <li>}), inline tags ({@code <a href="...">...</a>},
     * HTML entities), and any javadoc tree node whose {@code toString()} is used
     * as a fallback.
     *
     * <p>Renderers may pass this through as-is, convert it to their own
     * formatting, or strip it entirely.
     *
     * @param raw the raw HTML string
     */
    record Html(String raw) implements DocBodyNode {}
}
