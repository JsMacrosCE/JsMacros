package com.jsmacrosce.jsmacros.client.access;

import com.jsmacrosce.doclet.DocletIgnore;
@DocletIgnore
public interface IResourcePackManager {
    void jsmacros_disableServerPacks(boolean disable);

    boolean jsmacros_isServerPacksDisabled();

}
