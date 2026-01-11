package com.jsmacrosce.jsmacros.client.api.event.filterer;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.event.impl.EventSendPacket;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.EventFilterer;

/**
 * @author aMelonRind
 * @since 1.9.1
 */
@SuppressWarnings("unused")
public class FiltererSendPacket implements EventFilterer {
    @Nullable
    @DocletReplaceReturn("PacketName | null")
    public String type;

    @Override
    public boolean canFilter(String event) {
        return "SendPacket".equals(event);
    }

    @Override
    public boolean test(BaseEvent event) {
        return (event instanceof EventSendPacket e) && (type == null || e.type.equals(type));
    }

    @DocletReplaceParams("type: PacketName | null")
    public FiltererSendPacket setType(@Nullable String type) {
        this.type = type;
        return this;
    }

}
