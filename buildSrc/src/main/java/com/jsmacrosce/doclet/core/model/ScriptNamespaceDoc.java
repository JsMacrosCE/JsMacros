package com.jsmacrosce.doclet.core.model;

import java.util.List;

public record ScriptNamespaceDoc(String name, DocComment docComment, List<Export> exports) {
    public record Export(String name, TypeRef target, List<TypeRef> typeParams, boolean interfaceType) {
    }
}
