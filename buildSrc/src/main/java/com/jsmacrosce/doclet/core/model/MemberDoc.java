package com.jsmacrosce.doclet.core.model;

import com.jsmacrosce.doclet.DocletIgnore;

import java.util.List;

@DocletIgnore
public record MemberDoc(
    MemberKind kind,
    String name,
    String anchorId,
    List<ParamDoc> params,
    List<TypeRef> typeParams,
    TypeRef returnType,
    String replaceParams,
    String replaceReturn,
    String replaceTypeParams,
    List<String> modifiers,
    boolean scriptReadOnly,
    DocComment docComment,
    /**
     * Whether the member cannot be written the way the definitions write it, because the type
     * it belongs to answers to a member of the same name on the type it builds on with a
     * different shape. Java allows that; the type system of the definitions does not.
     */
    boolean shapeConflict
) {
    public MemberDoc(
        MemberKind kind,
        String name,
        String anchorId,
        List<ParamDoc> params,
        List<TypeRef> typeParams,
        TypeRef returnType,
        String replaceParams,
        String replaceReturn,
        String replaceTypeParams,
        List<String> modifiers,
        boolean scriptReadOnly,
        DocComment docComment
    ) {
        this(kind, name, anchorId, params, typeParams, returnType, replaceParams, replaceReturn,
            replaceTypeParams, modifiers, scriptReadOnly, docComment, false);
    }

    public boolean isReadOnly() {
        return kind == MemberKind.FIELD && (scriptReadOnly || modifiers.contains("final"));
    }
}
