package com.jsmacrosce.jsmacros.client.access;

import net.minecraft.network.chat.Component;

public interface IChatHud {

    void jsmacros_addMessageBypass(Component message);

    void jsmacros_addMessageAtIndexBypass(Component message, int index, int time);

}
