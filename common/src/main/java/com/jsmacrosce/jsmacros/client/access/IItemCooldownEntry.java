package com.jsmacrosce.jsmacros.client.access;

import com.jsmacrosce.doclet.DocletIgnore;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@DocletIgnore
public interface IItemCooldownEntry {

    int jsmacros_getStartTick();

    int jsmacros_getEndTick();

}
