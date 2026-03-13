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
import com.jsmacrosce.doclet.core.model.DocBodyNode;
import com.jsmacrosce.doclet.core.model.DocComment;
import com.jsmacrosce.doclet.core.model.DocTag;
import com.jsmacrosce.doclet.core.model.DocTagKind;

import javax.lang.model.element.Element;
import java.util.ArrayList;
import java.util.List;

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

        List<DocBodyNode> summary = parseNodes(tree.getFirstSentence());
        List<DocBodyNode> body = parseNodes(tree.getFullBody());
        List<DocTag> tags = new ArrayList<>();

        for (DocTree tag : tree.getBlockTags()) {
            switch (tag.getKind()) {
                case PARAM -> {
                    ParamTree param = (ParamTree) tag;
                    DocTagKind kind = param.isTypeParameter() ? DocTagKind.TEMPLATE : DocTagKind.PARAM;
                    tags.add(new DocTag(kind, param.getName().getName().toString(), parseNodes(param.getDescription())));
                }
                case RETURN -> {
                    ReturnTree ret = (ReturnTree) tag;
                    tags.add(new DocTag(DocTagKind.RETURN, null, parseNodes(ret.getDescription())));
                }
                case SINCE -> {
                    SinceTree since = (SinceTree) tag;
                    tags.add(new DocTag(DocTagKind.SINCE, null, parseNodes(since.getBody())));
                }
                case DEPRECATED -> {
                    DeprecatedTree dep = (DeprecatedTree) tag;
                    tags.add(new DocTag(DocTagKind.DEPRECATED, null, parseNodes(dep.getBody())));
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
    private List<DocBodyNode> parseNodes(List<? extends DocTree> trees) {
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
                    nodes.add(new DocBodyNode.Link(signature, label.isBlank() ? null : label));
                }
                default -> nodes.add(new DocBodyNode.Html(tree.toString()));
            }
        }
        return nodes;
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
