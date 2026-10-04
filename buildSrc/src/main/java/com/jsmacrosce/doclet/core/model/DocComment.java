package com.jsmacrosce.doclet.core.model;

import com.jsmacrosce.doclet.DocletIgnore;

import java.util.List;

/**
 * The parsed documentation comment for a class or member.
 *
 * <p>{@code summary} contains the first sentence of the comment body.
 * {@code body} contains the full body (including the first sentence).
 * Both are structured as a {@link List} of {@link DocBodyNode} rather than
 * raw strings so that each renderer can format links, code spans, and HTML
 * in its own target-specific way without regex re-parsing.
 */
@DocletIgnore
public record DocComment(List<DocBodyNode> summary, List<DocBodyNode> body, List<DocTag> tags) {
}
