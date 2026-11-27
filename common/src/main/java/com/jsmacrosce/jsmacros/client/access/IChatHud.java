package com.jsmacrosce.jsmacros.client.access;

import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@DocletIgnore
public interface IChatHud {

    void jsmacros_addMessageBypass(Component message);

    void jsmacros_addMessageAtIndexBypass(Component message, int index, int time);

}
