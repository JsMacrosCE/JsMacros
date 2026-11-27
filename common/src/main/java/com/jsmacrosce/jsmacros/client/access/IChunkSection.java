package com.jsmacrosce.jsmacros.client.access;

import com.jsmacrosce.doclet.DocletIgnore;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@DocletIgnore
public interface IChunkSection {

    short jsmacros_getNonEmptyBlockCount();

    short jsmacros_getRandomTickableBlockCount();

    short jsmacros_getNonEmptyFluidCount();

}
