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
    /**
     * Whether the class decides cancellation per instance instead of from its {@code @Event}
     * metadata. An event that overrides {@code cancellable()} can be cancellable for one
     * dispatch and not another; the website must not reduce that to a boolean flag.
     */
    boolean eventCancellableDynamic,
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
    List<TypeRef> capturedTypeParams,
    /**
     * Whether the class is not part of the API a script is given. Such a class is declared
     * only because a class of the API builds on it, and it is marked as what it is.
     */
    boolean internal,
    /**
     * Whether the types the class builds on cannot be written together, because two of them
     * answer to the same name with a different shape. Java allows that; the type system of the
     * definitions does not.
     */
    boolean heritageConflict,
    /**
     * Whether the static side of the class cannot be written, because a static member answers
     * to the same name on the type it builds on with a different shape.
     */
    boolean staticConflict
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
        this(name, qualifiedName, packageName, kind, group, alias, category, eventCancellable, false,
            eventFilterer, typeParams, extendsTypes, implementsTypes, modifiers, docComment, members,
            List.of(), false, false, false);
    }
}
