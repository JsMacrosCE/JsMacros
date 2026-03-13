package com.jsmacrosce.doclet.core.model;

import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.core.ClassGroup;

import java.util.List;

@DocletIgnore
public record ClassDoc(
    String name,
    String qualifiedName,
    String packageName,
    ClassKind kind,
    ClassGroup group,
    String alias,
    String category,
    boolean eventCancellable,
    String eventFilterer,
    List<TypeRef> typeParams,
    List<TypeRef> extendsTypes,
    List<TypeRef> implementsTypes,
    List<String> modifiers,
    DocComment docComment,
    List<MemberDoc> members
) {
}
