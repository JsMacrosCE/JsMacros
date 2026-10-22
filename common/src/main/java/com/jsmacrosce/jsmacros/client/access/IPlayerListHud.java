package com.jsmacrosce.jsmacros.client.access;

import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.network.chat.Component;

@DocletIgnore
public interface IPlayerListHud {
    Component jsmacros_getHeader();

    Component jsmacros_getFooter();

}
