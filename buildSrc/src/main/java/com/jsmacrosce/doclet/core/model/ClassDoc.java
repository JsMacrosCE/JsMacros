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
    List<MemberDoc> members,
    /**
     * The type parameters the class carries from the class it is written inside, which are in
     * scope in it. A class that is not written inside another one carries none.
     */
    List<TypeRef> capturedTypeParams
) {
    public ClassDoc(
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
        this(name, qualifiedName, packageName, kind, group, alias, category, eventCancellable,
            eventFilterer, typeParams, extendsTypes, implementsTypes, modifiers, docComment, members,
            List.of());
    }
}
