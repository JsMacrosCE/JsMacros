package com.jsmacrosce.doclet.core.model;

import java.util.List;

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
    DocComment docComment
) {
}
