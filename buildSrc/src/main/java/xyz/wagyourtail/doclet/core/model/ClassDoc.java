package xyz.wagyourtail.doclet.core.model;

import java.util.List;

public record ClassDoc(
    String name,
    String qualifiedName,
    String packageName,
    ClassKind kind,
    String group,
    String alias,
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
