package com.jsmacrosce.doclet.core;

import com.jsmacrosce.doclet.DocletIgnore;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.DocTreePath;
import com.jsmacrosce.doclet.core.model.DocBodyNode;
import com.jsmacrosce.doclet.core.model.DocComment;
import com.jsmacrosce.doclet.core.model.DocTag;
import com.jsmacrosce.doclet.core.model.DocTagKind;
import com.jsmacrosce.doclet.core.model.TypeKind;
import com.jsmacrosce.doclet.core.model.TypeRef;
import com.jsmacrosce.doclet.core.util.ElementNameUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@DocletIgnore
public class BasicDocCommentParser implements DocCommentParser {
    private final DocTrees docTrees;

    public BasicDocCommentParser(DocTrees docTrees) {
        this.docTrees = docTrees;
    }

    @Override
    public DocComment parse(Element element) {
        DocCommentTree tree = docTrees.getDocCommentTree(element);
        boolean isDeprecated = element.getAnnotation(Deprecated.class) != null;
        if (tree == null) {
            if (isDeprecated) {
                return new DocComment(
                    List.of(),
                    List.of(),
                    List.of(new DocTag(DocTagKind.DEPRECATED, null, List.of()))
                );
            }
            return new DocComment(List.of(), List.of(), List.of());
        }

        List<DocBodyNode> summary = parseNodes(tree.getFirstSentence(), element);
        List<DocBodyNode> body = parseNodes(tree.getFullBody(), element);
        List<DocTag> tags = new ArrayList<>();

        for (DocTree tag : tree.getBlockTags()) {
            switch (tag.getKind()) {
                case PARAM -> {
                    ParamTree param = (ParamTree) tag;
                    DocTagKind kind = param.isTypeParameter() ? DocTagKind.TEMPLATE : DocTagKind.PARAM;
                    tags.add(new DocTag(kind, param.getName().getName().toString(), parseNodes(param.getDescription(), element)));
                }
                case RETURN -> {
                    ReturnTree ret = (ReturnTree) tag;
                    tags.add(new DocTag(DocTagKind.RETURN, null, parseNodes(ret.getDescription(), element)));
                }
                case SINCE -> {
                    SinceTree since = (SinceTree) tag;
                    tags.add(new DocTag(DocTagKind.SINCE, null, parseNodes(since.getBody(), element)));
                }
                case DEPRECATED -> {
                    DeprecatedTree dep = (DeprecatedTree) tag;
                    tags.add(new DocTag(DocTagKind.DEPRECATED, null, parseNodes(dep.getBody(), element)));
                }
                case SEE -> {
                    SeeTree see = (SeeTree) tag;
                    for (DocTree ref : see.getReference()) {
                        if (ref.getKind() == DocTree.Kind.REFERENCE) {
                            String signature = ((ReferenceTree) ref).getSignature();
                            // @see signatures are already just reference strings — wrap as a Link node
                            // with no label so renderers can format them consistently with {@link}.
                            tags.add(new DocTag(DocTagKind.SEE, null, List.of(new DocBodyNode.Link(signature, null))));
                        } else {
                            tags.add(new DocTag(DocTagKind.SEE, null, List.of(new DocBodyNode.Html(ref.toString()))));
                        }
                    }
                }
                default -> tags.add(new DocTag(DocTagKind.OTHER, null, List.of(new DocBodyNode.Html(tag.toString()))));
            }
        }

        if (isDeprecated && tags.stream().noneMatch(tag -> tag.kind() == DocTagKind.DEPRECATED)) {
            tags.add(new DocTag(DocTagKind.DEPRECATED, null, List.of()));
        }

        return new DocComment(summary, body, tags);
    }

    /**
     * Converts a list of {@link DocTree} nodes from the javadoc AST into a
     * structured {@link List} of {@link DocBodyNode} values.
     *
     * <p>The javadoc AST is preserved rather than collapsed to a raw string so
     * that each renderer can format links, code spans, and HTML in its own
     * target-specific way without regex re-parsing.
     */
    private List<DocBodyNode> parseNodes(List<? extends DocTree> trees, Element context) {
        List<DocBodyNode> nodes = new ArrayList<>();
        for (DocTree tree : trees) {
            switch (tree.getKind()) {
                case TEXT -> nodes.add(new DocBodyNode.Text(((TextTree) tree).getBody()));
                case CODE, LITERAL -> {
                    // {@code ...} and {@literal ...} both produce inline code spans.
                    // LiteralTree.getBody() returns an ErroneousTree or TextTree; toString() gives
                    // the raw text, but getBody().toString() is cleaner.
                    nodes.add(new DocBodyNode.Code(((LiteralTree) tree).getBody().toString()));
                }
                case LINK, LINK_PLAIN -> {
                    LinkTree link = (LinkTree) tree;
                    String signature = link.getReference().getSignature();
                    // Flatten label nodes to plain text (no nested links possible in a label).
                    String label = flattenToText(link.getLabel());
                    nodes.add(new DocBodyNode.Link(signature, label.isBlank() ? null : label, resolveTargetType(link, context)));
                }
                default -> nodes.add(new DocBodyNode.Html(tree.toString()));
            }
        }
        return dedentExamples(nodes);
    }

    /**
     * Takes the indentation that the comment's own marker gave the lines of an example off, so
     * that the example reads the way it was written.
     *
     * <p>Every line of a javadoc comment carries the marker of the comment and the space that
     * follows it, and an example keeps that space on each of its lines. The example would read
     * as though it were shifted, so the space is taken off the lines that carry it.
     */
    private List<DocBodyNode> dedentExamples(List<DocBodyNode> nodes) {
        List<DocBodyNode> result = new ArrayList<>(nodes);
        for (int index = 0; index < result.size(); index++) {
            if (!(result.get(index) instanceof DocBodyNode.Html html) || !html.raw().startsWith("<pre")) {
                continue;
            }
            int end = index + 1;
            while (end < result.size() && !isEndOfExample(result.get(end))) {
                end++;
            }
            dedentExample(result, index + 1, end);
            index = end;
        }
        return result;
    }

    private boolean isEndOfExample(DocBodyNode node) {
        return node instanceof DocBodyNode.Html html && html.raw().startsWith("</pre");
    }

    /**
     * Takes the common indentation off the lines of one example.
     *
     * <p>The example is written as a number of pieces: the text between the tags, the code
     * spans, and the tags themselves. The lines are found in the text they make up, and the
     * space that marks them is taken off where the text starts them.
     */
    private void dedentExample(List<DocBodyNode> nodes, int from, int to) {
        StringBuilder text = new StringBuilder();
        List<int[]> pieces = new ArrayList<>();
        for (int index = from; index < to; index++) {
            DocBodyNode node = nodes.get(index);
            String value = switch (node) {
                case DocBodyNode.Text(var body) -> body;
                case DocBodyNode.Code(var body) -> body;
                case DocBodyNode.Html(var body) -> body;
                default -> "";
            };
            pieces.add(new int[] { index, text.length(), text.length() + value.length() });
            text.append(value);
        }
        // The lines of the example, and the space each of them is marked with. The first line
        // is written on the line of the marker itself, so it carries no space of its own.
        int common = Integer.MAX_VALUE;
        List<int[]> lines = new ArrayList<>();
        int start = 0;
        int number = 0;
        while (start <= text.length()) {
            int end = text.indexOf("\n", start);
            if (end < 0) {
                end = text.length();
            }
            int spaces = 0;
            while (start + spaces < end && text.charAt(start + spaces) == ' ') {
                spaces++;
            }
            if (number > 0 && !text.substring(start + spaces, end).isBlank()) {
                lines.add(new int[] { start, spaces });
                common = Math.min(common, spaces);
            }
            number++;
            if (end == text.length()) {
                break;
            }
            start = end + 1;
        }
        if (common == Integer.MAX_VALUE || common == 0) {
            return;
        }
        // The lines each piece of the example starts, with the space each of them is marked
        // with.
        Map<Integer, List<int[]>> byPiece = new LinkedHashMap<>();
        for (int[] line : lines) {
            for (int[] piece : pieces) {
                if (line[0] < piece[1] || line[0] >= piece[2]) {
                    continue;
                }
                byPiece.computeIfAbsent(piece[0], key -> new ArrayList<>())
                    .add(new int[] { line[0] - piece[1], line[1] });
                break;
            }
        }
        // The space is taken off from the last line backwards, so that the offsets of the lines
        // before it stay the same.
        for (Map.Entry<Integer, List<int[]>> entry : byPiece.entrySet()) {
            int index = entry.getKey();
            if (!(nodes.get(index) instanceof DocBodyNode.Text value)) {
                continue;
            }
            String body = value.value();
            List<int[]> offsets = entry.getValue();
            offsets.sort((left, right) -> Integer.compare(right[0], left[0]));
            for (int[] offset : offsets) {
                int spaces = Math.min(common, offset[1]);
                body = body.substring(0, offset[0]) + body.substring(offset[0] + spaces);
            }
            nodes.set(index, new DocBodyNode.Text(body));
        }
    }

    private TypeRef resolveTargetType(LinkTree link, Element context) {
        var sourcePath = docTrees.getPath(context);
        if (sourcePath == null) return null;
        Element target = docTrees.getElement(new DocTreePath(
            new DocTreePath(sourcePath, docTrees.getDocCommentTree(context)), link.getReference()));
        if (target instanceof TypeParameterElement) return null;
        while (target != null && !(target instanceof TypeElement)) {
            target = target.getEnclosingElement();
        }
        if (!(target instanceof TypeElement type)) return null;
        return new TypeRef(TypeKind.DECLARED, ElementNameUtils.getDisplayClassName(type),
            ElementNameUtils.getQualifiedName(type), List.of(), false, false, null, false);
    }

    /**
     * Flattens a list of {@link DocTree} nodes to a plain-text string.
     * Used only for extracting the label portion of a {@code {@link}} tag
     * (which cannot itself contain nested links).
     */
    private String flattenToText(List<? extends DocTree> trees) {
        StringBuilder sb = new StringBuilder();
        for (DocTree tree : trees) {
            switch (tree.getKind()) {
                case TEXT -> sb.append(((TextTree) tree).getBody());
                case CODE, LITERAL -> sb.append(((LiteralTree) tree).getBody());
                default -> sb.append(tree.toString());
            }
        }
        return sb.toString().trim();
    }
}
