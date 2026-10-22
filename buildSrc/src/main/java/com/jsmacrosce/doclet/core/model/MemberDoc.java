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
    boolean shapeConflict,
    /**
     * Whether the member hands back the type it belongs to, the way the member it builds on
     * does. A class that builds on such a type has to hand the type back the same way.
     */
    boolean selfReturn
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
            replaceTypeParams, modifiers, scriptReadOnly, docComment, false, false);
    }

    public boolean isReadOnly() {
        return kind == MemberKind.FIELD && (scriptReadOnly || modifiers.contains("final"));
    }
}
