package com.jsmacrosce;

import java.util.Map;

/**
 * Fluent builder for Markdown documents with VitePress-specific constructs.
 *
 * <p>Block-level methods automatically manage blank lines between elements.
 * Consecutive bullet items (and items that immediately follow a
 * {@link #boldHeader}) are not separated by blank lines; a blank line is
 * inserted before any other block element that follows a list.
 *
 * <p>Inline helpers ({@link #codeSpan} and {@link #link}) are static so they
 * can be used in contexts where only a {@code String} is needed, such as
 * inside {@code MarkdownWriter} signatures.
 */
public class MarkdownBuilder {

    /** Tracks what the last emitted element was, to decide separators. */
    private enum State {
        /** Nothing has been emitted yet. */
        START,
        /** Last emitted element was a regular block (heading, paragraph, …). */
        BLOCK,
        /**
         * Last emitted element was a list item or a bold-header (a bold label
         * immediately followed by list items, e.g. {@code **Parameters:**}).
         * Consecutive list elements never receive a blank-line separator.
         */
        LIST
    }

    private final StringBuilder sb = new StringBuilder();
    private State state = State.START;

    // -----------------------------------------------------------------------
    // Inline helpers (static — no state, return String)
    // -----------------------------------------------------------------------

    /**
     * Wraps {@code text} in backtick fences, choosing a fence length that does
     * not conflict with any backtick run already inside {@code text}.
     *
     * @param text the raw text content; must not be {@code null}
     * @return a markdown inline code span
     */
    public static String codeSpan(String text) {
        if (text == null) {
            return "";
        }
        int maxTicks = 0;
        int current = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '`') {
                current++;
                if (current > maxTicks) {
                    maxTicks = current;
                }
            } else {
                current = 0;
            }
        }
        String fence = "`".repeat(maxTicks + 1);
        return fence + text + fence;
    }

    /**
     * Returns a markdown inline link: {@code [text](url)}.
     *
     * @param text the link label
     * @param url  the link target
     * @return a markdown link string
     */
    public static String link(String text, String url) {
        return "[" + text + "](" + url + ")";
    }

    // -----------------------------------------------------------------------
    // Block-level methods
    // -----------------------------------------------------------------------

    /**
     * Emits a YAML frontmatter block at the current position.
     *
     * <p>Entries are written in insertion order. Values are emitted verbatim
     * (no quoting is added by this method).
     *
     * @param entries key-value pairs for the frontmatter
     * @return {@code this}
     */
    public MarkdownBuilder frontmatter(Map<String, String> entries) {
        beginBlock();
        sb.append("---\n");
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        sb.append("---\n");
        state = State.BLOCK;
        return this;
    }

    /**
     * Emits {@code # … ## … ###  …} depending on {@code level}.
     *
     * @param level ATX heading level (1–6)
     * @param text  heading text (not escaped by this method)
     * @return {@code this}
     */
    public MarkdownBuilder heading(int level, String text) {
        beginBlock();
        sb.append("#".repeat(level)).append(" ").append(text).append("\n");
        state = State.BLOCK;
        return this;
    }

    /**
     * Emits a heading with a VitePress custom anchor: {@code ## text {#anchor}}.
     *
     * @param level  ATX heading level (1–6)
     * @param text   heading text
     * @param anchor anchor identifier (without the leading {@code #})
     * @return {@code this}
     */
    public MarkdownBuilder heading(int level, String text, String anchor) {
        beginBlock();
        sb.append("#".repeat(level)).append(" ").append(text)
          .append(" {#").append(anchor).append("}\n");
        state = State.BLOCK;
        return this;
    }

    /**
     * Emits a paragraph block.  The text is appended verbatim followed by a
     * single newline; the blank-line separator before the paragraph is managed
     * automatically.
     *
     * @param text paragraph content (may include inline markdown)
     * @return {@code this}
     */
    public MarkdownBuilder paragraph(String text) {
        if (text == null || text.isEmpty()) {
            return this;
        }
        beginBlock();
        sb.append(text).append("\n");
        state = State.BLOCK;
        return this;
    }

    /**
     * Emits a bold "label" immediately followed by list items.
     *
     * <p>Example output: {@code **Parameters:**\n}.  No blank line is inserted
     * between this header and subsequent {@link #bulletItem} calls.
     *
     * @param label the bold label (without the {@code **} delimiters)
     * @return {@code this}
     */
    public MarkdownBuilder boldHeader(String label) {
        beginBlock();
        sb.append("**").append(label).append("**\n");
        state = State.LIST;
        return this;
    }

    /**
     * Emits a list item ({@code - text\n}).
     *
     * <p>Consecutive bullet items are never separated by blank lines.  A blank
     * line is inserted before the first bullet item when it follows a
     * non-list block (i.e. when the preceding state is {@link State#BLOCK}).
     *
     * @param text list-item content (may include inline markdown)
     * @return {@code this}
     */
    public MarkdownBuilder bulletItem(String text) {
        if (state == State.BLOCK) {
            sb.append("\n");
        }
        sb.append("- ").append(text).append("\n");
        state = State.LIST;
        return this;
    }

    /**
     * Appends {@code text} verbatim without inserting any separator or
     * modifying the current state.  Use this for pre-formatted content whose
     * structural role is best managed by the caller.
     *
     * @param text raw text to append
     * @return {@code this}
     */
    public MarkdownBuilder raw(String text) {
        sb.append(text);
        return this;
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    @Override
    public String toString() {
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Inserts a blank line before the upcoming block element when needed.
     *
     * <p>A blank line is inserted in two situations:
     * <ul>
     *   <li>When the previous element was a regular block ({@link State#BLOCK})
     *       — each block element ends with {@code \n}, so appending one more
     *       {@code \n} creates the required {@code \n\n} separator.</li>
     *   <li>When the previous element was a list item or bold-header
     *       ({@link State#LIST}) — same logic applies.</li>
     * </ul>
     * In the initial {@link State#START} state nothing is emitted.
     */
    private void beginBlock() {
        if (state != State.START) {
            sb.append("\n");
        }
    }
}
