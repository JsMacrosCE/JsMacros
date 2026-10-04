package com.jsmacrosce.doclet.core.model;

import com.jsmacrosce.doclet.DocletIgnore;

import java.util.List;

/**
 * A single block tag from a javadoc comment (e.g. {@code @param}, {@code @return},
 * {@code @since}).
 *
 * <p>{@code name} is the tag parameter name where applicable (e.g. the parameter
 * name for {@code @param} tags, or the type-parameter name for {@code @template}
 * tags), and is {@code null} for tags that don't have one.
 *
 * <p>{@code body} holds the tag's description as a structured
 * {@link List} of {@link DocBodyNode} nodes rather than a raw string, for the
 * same reason as {@link DocComment}: each renderer formats links, code spans,
 * and HTML in its own way without regex re-parsing.
 */
@DocletIgnore
public record DocTag(DocTagKind kind, String name, List<DocBodyNode> body) {
}
