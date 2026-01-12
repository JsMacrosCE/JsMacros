package com.jsmacrosce.doclet.core;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.util.DocTrees;
import com.jsmacrosce.doclet.core.model.DocComment;
import com.jsmacrosce.doclet.core.model.DocTag;
import com.jsmacrosce.doclet.core.model.DocTagKind;

import javax.lang.model.element.Element;
import java.util.ArrayList;
import java.util.List;

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
                return new DocComment("", "", List.of(new DocTag(DocTagKind.DEPRECATED, null, "")));
            }
            return new DocComment("", "", List.of());
        }

        String summary = renderText(tree.getFirstSentence());
        String description = renderText(tree.getFullBody());
        List<DocTag> tags = new ArrayList<>();

        for (DocTree tag : tree.getBlockTags()) {
            switch (tag.getKind()) {
                case PARAM -> {
                    ParamTree param = (ParamTree) tag;
                    DocTagKind kind = param.isTypeParameter() ? DocTagKind.TEMPLATE : DocTagKind.PARAM;
                    tags.add(new DocTag(kind, param.getName().getName().toString(), renderText(param.getDescription())));
                }
                case RETURN -> {
                    ReturnTree ret = (ReturnTree) tag;
                    tags.add(new DocTag(DocTagKind.RETURN, null, renderText(ret.getDescription())));
                }
                case SINCE -> {
                    SinceTree since = (SinceTree) tag;
                    tags.add(new DocTag(DocTagKind.SINCE, null, renderText(since.getBody())));
                }
                case DEPRECATED -> {
                    DeprecatedTree dep = (DeprecatedTree) tag;
                    tags.add(new DocTag(DocTagKind.DEPRECATED, null, renderText(dep.getBody())));
                }
                case SEE -> {
                    SeeTree see = (SeeTree) tag;
                    for (DocTree ref : see.getReference()) {
                        if (ref.getKind() == DocTree.Kind.REFERENCE) {
                            String signature = ((ReferenceTree) ref).getSignature();
                            tags.add(new DocTag(DocTagKind.SEE, null, signature));
                        } else {
                            tags.add(new DocTag(DocTagKind.SEE, null, ref.toString()));
                        }
                    }
                }
                default -> tags.add(new DocTag(DocTagKind.OTHER, null, tag.toString()));
            }
        }

        if (isDeprecated && tags.stream().noneMatch(tag -> tag.kind() == DocTagKind.DEPRECATED)) {
            tags.add(new DocTag(DocTagKind.DEPRECATED, null, ""));
        }

        return new DocComment(summary, description, tags);
    }

    private String renderText(List<? extends DocTree> trees) {
        StringBuilder builder = new StringBuilder();
        for (DocTree tree : trees) {
            switch (tree.getKind()) {
                case TEXT -> builder.append(((TextTree) tree).getBody());
                case CODE -> builder.append("`").append(((LiteralTree) tree).getBody()).append("`");
                case LINK, LINK_PLAIN -> {
                    String signature = ((LinkTree) tree).getReference().getSignature();
                    builder.append("{@link ").append(signature).append("}");
                }
                default -> builder.append(tree.toString());
            }
        }
        return builder.toString().trim();
    }
}
